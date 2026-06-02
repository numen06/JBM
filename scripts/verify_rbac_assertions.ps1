# RBAC + multi-credential verification (Gateway 6060)
$ErrorActionPreference = 'Continue'
$Gateway = 'http://127.0.0.1:6060'
$ClientId = 'demo'
$ClientSecret = 'demo123'
$Password = 'Admin@123'
$Headers = @{ tenantId = '0' }

function Get-Token($username) {
    $body = @{
        grant_type = 'password'
        client_id = $ClientId
        client_secret = $ClientSecret
        username = $username
        password = $Password
        scope = 'all'
        loginType = 'PASSWORD'
    }
    $r = Invoke-RestMethod -Uri "$Gateway/oauth2/token" -Method Post -Body $body -ContentType 'application/x-www-form-urlencoded'
    if ($r.code -ne 200 -and -not $r.success) { throw $r.message }
    return $r.result.access_token
}

function Get-Json($url, $token) {
    $h = @{ Authorization = "Bearer $token"; tenantId = '0' }
    $r = Invoke-RestMethod -Uri "$Gateway$url" -Headers $h -Method Get
    if ($r.code -ne 200 -and -not $r.success) { throw $r.message }
    return $r.result
}

$report = @()

# 1. Multi-credential
foreach ($case in @(
    @{ user = 'demo'; accounts = @('demo','13800138000','demo@jbm.local') },
    @{ user = 'viewer'; accounts = @('viewer','13900139000','viewer@jbm.local') },
    @{ user = 'admin'; accounts = @('admin') }
)) {
    $ids = @()
    foreach ($acc in $case.accounts) {
        try {
            $tok = Get-Token $acc
            $u = Get-Json '/current/user' $tok
            $ids += $u.userId
            $report += [pscustomobject]@{ Area='多凭证'; Case=$case.user; Check="登录 $acc"; Expected='成功且同 userId'; Actual="OK userId=$($u.userId)"; Pass=$true }
        } catch {
            $report += [pscustomobject]@{ Area='多凭证'; Case=$case.user; Check="登录 $acc"; Expected='成功'; Actual=$_.Exception.Message; Pass=$false }
        }
    }
    $uniq = $ids | Select-Object -Unique
    $pass = ($uniq.Count -eq 1 -and $ids.Count -eq $case.accounts.Count)
    $report += [pscustomobject]@{ Area='多凭证'; Case=$case.user; Check='userId 一致'; Expected='1 个 userId'; Actual="$($uniq -join ',')"; Pass=$pass }
}

# 2. Menus per user
$menuExpect = @{
    admin = @{ min = 10; must = @('/dashboard','/system/users') }
    demo = @{ min = 2; must = @('/dashboard','/system/dicts') }
    viewer = @{ min = 1; must = @('/system/users') }
}
foreach ($u in $menuExpect.Keys) {
    try {
        $tok = Get-Token $u
        $menus = Get-Json '/current/user/menus' $tok
        $paths = $menus | ForEach-Object { $_.path } | Where-Object { $_ }
        $exp = $menuExpect[$u]
        $hasMust = ($exp.must | ForEach-Object { $paths -contains $_ }) -notcontains $false
        $pass = ($paths.Count -ge $exp.min) -and $hasMust
        $report += [pscustomobject]@{ Area='菜单'; Case=$u; Check='侧栏路径'; Expected=($exp.must -join ','); Actual=($paths -join ' | '); Pass=$pass }
    } catch {
        $report += [pscustomobject]@{ Area='菜单'; Case=$u; Check='侧栏路径'; Expected='见上'; Actual=$_.Exception.Message; Pass=$false }
    }
}

# 3. Button authorities (ACTION_*)
$authExpect = @{
    admin = @{ must = @('ACTION_users_add','ACTION_dict_delete'); deny = @() }
    demo = @{ must = @('ACTION_dict_view','ACTION_dict_add'); deny = @('ACTION_dict_delete','ACTION_users_add') }
    viewer = @{ must = @('ACTION_users_view','ACTION_users_edit'); deny = @('ACTION_users_add','ACTION_users_delete') }
}
foreach ($u in $authExpect.Keys) {
    try {
        $tok = Get-Token $u
        $cu = Get-Json '/current/user' $tok
        $codes = @($cu.authorities | ForEach-Object { $_.authority })
        $exp = $authExpect[$u]
        $hasMust = ($exp.must | ForEach-Object { $codes -contains $_ }) -notcontains $false
        $noDeny = ($exp.deny | ForEach-Object { $codes -notcontains $_ }) -notcontains $false
        $pass = $hasMust -and $noDeny
        $report += [pscustomobject]@{ Area='按钮权限'; Case=$u; Check='ACTION_*'; Expected="有:$($exp.must -join ',') 无:$($exp.deny -join ',')"; Actual=($codes | Where-Object { $_ -like 'ACTION_*' } | Select-Object -First 12) -join ','; Pass=$pass }
    } catch {
        $report += [pscustomobject]@{ Area='按钮权限'; Case=$u; Check='ACTION_*'; Expected='见上'; Actual=$_.Exception.Message; Pass=$false }
    }
}

# 4. viewer user exists
try {
    $tok = Get-Token 'admin'
    $h = @{ Authorization = "Bearer $tok"; tenantId = '0' }
    $users = Invoke-RestMethod -Uri "$Gateway/user?keyword=viewer" -Headers $h
    $found = ($users.result | Where-Object { $_.userName -eq 'viewer' }).Count -gt 0
    $report += [pscustomobject]@{ Area='种子'; Case='viewer'; Check='用户存在'; Expected='true'; Actual=$found; Pass=$found }
} catch {
    $report += [pscustomobject]@{ Area='种子'; Case='viewer'; Check='用户存在'; Expected='true'; Actual=$_.Exception.Message; Pass=$false }
}

$report | Format-Table -AutoSize
$failed = @($report | Where-Object { -not $_.Pass }).Count
$total = $report.Count
Write-Host "`nSUMMARY: $($total - $failed)/$total passed, $failed failed"
if ($failed -gt 0) { exit 1 }
