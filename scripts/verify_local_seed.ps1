param(
    [string]$BaseUrl = 'http://127.0.0.1:5173/v3/api',
    [string]$Username = 'admin',
    [string]$Password = $env:JBM_LOCAL_ADMIN_PASSWORD,
    [string]$OperatorPassword = $env:JBM_LOCAL_OPERATOR_PASSWORD,
    [string]$TenantPassword = $env:JBM_LOCAL_TENANT_PASSWORD,
    [string]$ClientId = 'jbmSeedDevAppKey00000001',
    [string]$CenterHealthUrl = 'http://127.0.0.1:7777/actuator/health',
    [string]$DocUrl = 'http://127.0.0.1:9999',
    [string]$BigscreenUrl = 'http://127.0.0.1:3314',
    [int]$TimeoutSeconds = 90
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($Password)) { $Password = 'Admin@123' }
if ([string]::IsNullOrWhiteSpace($OperatorPassword)) { $OperatorPassword = 'Operator@123' }
if ([string]::IsNullOrWhiteSpace($TenantPassword)) { $TenantPassword = 'Tenant@123' }
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

function New-AccessToken([string]$LoginUser = $Username, [string]$LoginPassword = $Password) {
    $pkce = New-PkcePair
    $redirect = 'http://127.0.0.1:5173/login/callback'
    $login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/oauth2/doLogin" `
        -ContentType 'application/x-www-form-urlencoded' -Body @{
            response_type = 'code'
            client_id = $ClientId
            redirect_uri = $redirect
            state = 'local-seed-verification'
            username = $LoginUser
            password = $LoginPassword
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

    do {
        try {
            $tokenResponse = Invoke-RestMethod -Method Post -Uri "$BaseUrl/auth/oauth2/token" `
                -ContentType 'application/x-www-form-urlencoded' -Body @{
                    grant_type = 'authorization_code'
                    code = $authorizationCode
                    client_id = $ClientId
                    redirect_uri = $redirect
                    code_verifier = $pkce.Verifier
                }
            break
        }
        catch {
            if ((Get-Date) -ge $deadline) { throw }
            Start-Sleep -Seconds 2
        }
    } while ($true)
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
$filterRules = Assert-JbmSuccess (
    Invoke-RestMethod -Method Get -Uri "$BaseUrl/logs/GatewayLogs/filterRules" -Headers $headers
) 'Gateway access-log integration'
$demoLog = Assert-JbmSuccess (
    Invoke-RestMethod -Method Post -Uri "$BaseUrl/logs/businessLog/demo?mode=simple" -Headers $headers `
        -ContentType 'application/json' -Body '{}'
) 'Business log creation'
$demoLines = Assert-JbmSuccess (
    Invoke-RestMethod -Method Get -Uri "$BaseUrl/logs/businessLog/get/$($demoLog.logId)" -Headers $headers
) 'Business log query'
$bigscreenPage = Assert-JbmSuccess (
    Invoke-RestMethod -Method Post -Uri "$BaseUrl/bigscreen/bigscreenView/pageList" -Headers $headers `
        -ContentType 'application/json' -Body '{"pageForm":{"currPage":1,"pageSize":10}}'
) 'Bigscreen query'
$gatewayLogPage = Assert-JbmSuccess (
    Invoke-RestMethod -Method Post -Uri "$BaseUrl/logs/GatewayLogs/findLogs" -Headers $headers `
        -ContentType 'application/json' -Body '{"pageForm":{"currPage":1,"pageSize":10}}'
) 'Gateway access-log query'

$smokeRoot = Join-Path $repoRoot ('temp\bigscreen-smoke-' + [guid]::NewGuid().ToString('N'))
$smokeZip = "$smokeRoot.zip"
$bigscreenPreview = $null
try {
    New-Item -ItemType Directory -Path $smokeRoot | Out-Null
    Set-Content -LiteralPath (Join-Path $smokeRoot 'index.html') `
        -Value '<html><body>JBM Bigscreen Python Smoke</body></html>' -Encoding UTF8
    Compress-Archive -Path (Join-Path $smokeRoot 'index.html') -DestinationPath $smokeZip
    $upload = curl.exe -sS -F "file=@$smokeZip" "$DocUrl/upload" | ConvertFrom-Json
    if ($LASTEXITCODE -ne 0) { throw 'Bigscreen package upload failed' }
    $docPath = Assert-JbmSuccess $upload 'Bigscreen package upload'
    $viewKey = 'python-smoke-' + [guid]::NewGuid().ToString('N')
    $view = Assert-JbmSuccess (
        Invoke-RestMethod -Method Post -Uri "$BigscreenUrl/bigscreenView/save" `
            -ContentType 'application/json' -Body (@{
                viewName = 'Python Smoke'
                viewUrl = $viewKey
                resourcePath = $docPath
            } | ConvertTo-Json)
    ) 'Bigscreen package deployment'
    $bigscreenPreview = Invoke-WebRequest -UseBasicParsing `
        -Uri "$BigscreenUrl/static/$viewKey/index.html"
    if ($bigscreenPreview.StatusCode -ne 200 -or $bigscreenPreview.Content -notmatch 'JBM Bigscreen Python Smoke') {
        throw 'Bigscreen static preview is invalid'
    }
}
finally {
    $tempRoot = [IO.Path]::GetFullPath((Join-Path $repoRoot 'temp'))
    $resolvedSmokeRoot = [IO.Path]::GetFullPath($smokeRoot)
    $resolvedSmokeZip = [IO.Path]::GetFullPath($smokeZip)
    if ($resolvedSmokeRoot.StartsWith($tempRoot) -and (Test-Path -LiteralPath $resolvedSmokeRoot)) {
        Remove-Item -LiteralPath $resolvedSmokeRoot -Recurse -Force
    }
    if ($resolvedSmokeZip.StartsWith($tempRoot) -and (Test-Path -LiteralPath $resolvedSmokeZip)) {
        Remove-Item -LiteralPath $resolvedSmokeZip -Force
    }
}

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
if (@($filterRules).Count -lt 4) { throw 'Built-in gateway access-log filter rules are incomplete' }
if ($gatewayLogPage.total -lt 1) { throw 'Gateway access logs were not persisted' }
if ([string]::IsNullOrWhiteSpace($demoLog.logId) -or @($demoLines).Count -lt 1) { throw 'Business log workflow is incomplete' }
if ($null -eq $bigscreenPage.contents) { throw 'Bigscreen page contract is invalid' }

$operatorToken = New-AccessToken 'platform_operator' $OperatorPassword
$tenantToken = New-AccessToken 'tenant_admin' $TenantPassword
$operatorHeaders = @{ Authorization = "Bearer $operatorToken" }
$tenantHeaders = @{ Authorization = "Bearer $tenantToken" }
$operatorUsers = Assert-JbmSuccess (
    Invoke-RestMethod -Method Get -Uri "$BaseUrl/center/user?pageForm.currPage=1&pageForm.pageSize=100" -Headers $operatorHeaders
) 'Platform operator user scope'
$tenantUsers = Assert-JbmSuccess (
    Invoke-RestMethod -Method Get -Uri "$BaseUrl/center/user?pageForm.currPage=1&pageForm.pageSize=100&companyId=1" -Headers $tenantHeaders
) 'Tenant user scope'
$tenantApps = Assert-JbmSuccess (
    Invoke-RestMethod -Method Get -Uri "$BaseUrl/center/app?pageForm.currPage=1&pageForm.pageSize=100&orgId=1" -Headers $tenantHeaders
) 'Tenant application scope'
$tenantDashboard = Assert-JbmSuccess (
    Invoke-RestMethod -Method Get -Uri "$BaseUrl/center/current/dashboard" -Headers $tenantHeaders
) 'Tenant dashboard scope'

if ($operatorUsers.total -lt 3) { throw 'Platform operator cannot see the platform user set' }
if ($tenantUsers.total -ne 1 -or $tenantUsers.contents[0].userName -ne 'tenant_admin') {
    throw 'Tenant user isolation failed'
}
if ($tenantApps.total -ne 1 -or $tenantApps.contents[0].orgId -ne 2000) {
    throw 'Tenant application isolation failed'
}
if ($tenantDashboard.identity.scope -ne 'tenant' -or $tenantDashboard.identity.tenantId -ne 2000) {
    throw 'Tenant dashboard identity is invalid'
}
try {
    Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/center/user/1" -Headers $tenantHeaders | Out-Null
    throw 'Cross-tenant user access was not rejected'
}
catch {
    if ($_.Exception.Response.StatusCode -ne 403) { throw }
}

[pscustomobject]@{
    Login = 'PASS'
    User = $user.userName
    Organizations = $orgCount
    DictionaryGroups = $dictCount
    Applications = $appCount
    Roles = $roleCount
    GatewayRoutes = $routeCount
    GatewayLogRules = @($filterRules).Count
    GatewayAccessLogs = $gatewayLogPage.total
    BusinessLog = $demoLog.logId
    Bigscreen = "PASS ($($bigscreenPreview.StatusCode))"
    PlatformOperator = "PASS ($($operatorUsers.total) users)"
    TenantIsolation = "PASS (tenant 2000)"
} | Format-List
