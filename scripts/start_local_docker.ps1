param(
    [switch]$SkipPackage,
    [switch]$SkipImageBuild,
    [switch]$SkipVerify
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $repoRoot 'compose.local.yml'

Push-Location $repoRoot
try {
    docker compose -f $composeFile config --quiet
    if ($LASTEXITCODE -ne 0) { throw 'compose.local.yml validation failed' }

    if (-not $SkipPackage) {
        Push-Location (Join-Path $repoRoot 'jbm-admin-vue')
        try {
            npm run build
            if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed' }
            npm test
            if ($LASTEXITCODE -ne 0) { throw 'Frontend tests failed' }
        }
        finally {
            Pop-Location
        }
    }

    if (-not $SkipImageBuild) {
        docker compose -f $composeFile build
        if ($LASTEXITCODE -ne 0) { throw 'Docker image build failed' }
    }

    docker compose -f $composeFile up -d
    if ($LASTEXITCODE -ne 0) { throw 'Docker Compose startup failed' }
    docker compose -f $composeFile ps

    if (-not $SkipVerify) {
        & (Join-Path $PSScriptRoot 'verify_local_seed.ps1')
        if ($LASTEXITCODE -ne 0) { throw 'Local seed verification failed' }
    }
}
finally {
    Pop-Location
}
