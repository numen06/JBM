param(
    [string]$BaseUrl = 'http://127.0.0.1:5173/v3/api',
    [string]$Username = 'admin',
    [string]$Password = $env:JBM_LOCAL_ADMIN_PASSWORD,
    [string]$ClientId = 'jbmSeedDevAppKey00000001',
    [string]$CenterHealthUrl = 'http://127.0.0.1:7777/actuator/health',
    [int]$TimeoutSeconds = 90
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($Password)) { $Password = 'Admin@123' }
$BaseUrl = $BaseUrl.TrimEnd('/')

function Assert-JbmSuccess([object]$Response, [string]$Name) {
    if ($null -eq $Response -or $Response.success -ne $true) {
        $message = if ($Response.message) { $Response.message } else { 'empty response' }
        throw "$Name failed: $message"
    }
    return $Response.result
}

function New-PkcePair {
    $bytes = New-Object byte[] 48
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $verifier = [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
    $sha = [System.Security.Cryptography.SHA256]::Create()
    $challenge = [Convert]::ToBase64String(
        $sha.ComputeHash([Text.Encoding]::ASCII.GetBytes($verifier))
    ).TrimEnd('=').Replace('+', '-').Replace('/', '_')
    return @{ Verifier = $verifier; Challenge = $challenge }
}

function New-AccessToken {
    $pkce = New-PkcePair
    $redirect = 'http://127.0.0.1:5173/login/callback'
    $login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/oauth2/doLogin" `
        -ContentType 'application/x-www-form-urlencoded' -Body @{
            response_type = 'code'
            client_id = $ClientId
            redirect_uri = $redirect
            state = 'local-seed-verification'
            username = $Username
            password = $Password
            scope = 'all'
            loginType = 'PASSWORD'
            vcode = '9999'
            code_challenge = $pkce.Challenge
            code_challenge_method = 'S256'
        }
    $callback = [uri](Assert-JbmSuccess $login 'OAuth login')
    $query = [System.Web.HttpUtility]::ParseQueryString($callback.Query)
    $authorizationCode = $query['code']
    if ([string]::IsNullOrWhiteSpace($authorizationCode)) { throw 'OAuth login returned no authorization code' }

    $tokenResponse = Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/oauth2/token" `
        -ContentType 'application/x-www-form-urlencoded' -Body @{
            grant_type = 'authorization_code'
            code = $authorizationCode
            client_id = $ClientId
            redirect_uri = $redirect
            code_verifier = $pkce.Verifier
        }
    $token = Assert-JbmSuccess $tokenResponse 'OAuth token exchange'
    $accessToken = if ($token.access_token) { $token.access_token } else { $token.accessToken }
    if ([string]::IsNullOrWhiteSpace($accessToken)) { throw 'OAuth token exchange returned no access token' }
    return $accessToken
}

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
do {
    try {
        $health = Invoke-RestMethod -Method Get -Uri $CenterHealthUrl
        if ($health.status -eq 'UP') { break }
    }
    catch {
        if ((Get-Date) -ge $deadline) { throw 'Center service did not become available before timeout' }
    }
    Start-Sleep -Seconds 3
} while ((Get-Date) -lt $deadline)

do {
    try {
        $accessToken = New-AccessToken
        break
    }
    catch {
        if ((Get-Date) -ge $deadline) { throw }
        Start-Sleep -Seconds 3
    }
} while ($true)

$headers = @{ Authorization = "Bearer $accessToken" }
$user = Assert-JbmSuccess (
    Invoke-RestMethod -Method Get -Uri "$BaseUrl/center/current/user" -Headers $headers
) 'Current user'
$orgs = Assert-JbmSuccess (
    Invoke-RestMethod -Method Post -Uri "$BaseUrl/center/baseOrg/root" -Headers $headers `
        -ContentType 'application/json' -Body '{}'
) 'Organization seed'
$dictPage = Assert-JbmSuccess (
    Invoke-RestMethod -Method Post -Uri "$BaseUrl/center/baseDic/root/pageList" -Headers $headers `
        -ContentType 'application/json' -Body '{"pageForm":{"currPage":1,"pageSize":100},"baseDic":{}}'
) 'Dictionary seed'
$apps = Assert-JbmSuccess (
    Invoke-RestMethod -Method Get -Uri "$BaseUrl/center/app?pageForm.currPage=1&pageForm.pageSize=10" -Headers $headers
) 'Application seed'
$roles = Assert-JbmSuccess (
    Invoke-RestMethod -Method Get -Uri "$BaseUrl/center/role?pageForm.currPage=1&pageForm.pageSize=10" -Headers $headers
) 'Role seed'
$routes = Assert-JbmSuccess (
    Invoke-RestMethod -Method Get -Uri "$BaseUrl/center/gateway/routes?pageForm.currPage=1&pageForm.pageSize=20" -Headers $headers
) 'Gateway route seed'

$orgCount = @($orgs).Count
$dictCount = @($dictPage.contents).Count
$appCount = @($apps.contents).Count
$roleCount = @($roles.contents).Count
$routeCount = @($routes.contents).Count
$expectedDictCodes = @('sys_status', 'yes_no', 'org_type', 'job_status')
$actualDictCodes = @($dictPage.contents | ForEach-Object { $_.code })
$missingDictCodes = @($expectedDictCodes | Where-Object { $_ -notin $actualDictCodes })
$seedApp = @($apps.contents | Where-Object { $_.id -eq 1000 -or $_.appId -eq 1000 }) | Select-Object -First 1

if ($user.userName -ne 'admin') { throw "Unexpected current user: $($user.userName)" }
if ($user.companyId -ne 1) { throw 'Admin is not assigned to the default organization' }
if ($orgCount -lt 1) { throw 'Default organization is missing' }
if ($missingDictCodes.Count -gt 0) { throw "Dictionary groups are incomplete: $($missingDictCodes -join ', ')" }
if ($appCount -lt 1) { throw 'Seed OAuth application is missing' }
if ($null -eq $seedApp -or $seedApp.orgId -ne 1) { throw 'Seed OAuth application is not assigned to the default organization' }
if ($roleCount -lt 2) { throw 'Built-in roles are incomplete' }
if ($routeCount -lt 1) { throw 'Gateway routes are missing' }

[pscustomobject]@{
    Login = 'PASS'
    User = $user.userName
    Organizations = $orgCount
    DictionaryGroups = $dictCount
    Applications = $appCount
    Roles = $roleCount
    GatewayRoutes = $routeCount
} | Format-List
