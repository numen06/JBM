[CmdletBinding()]
param(
    [ValidatePattern('^\d+\.\d+\.\d+$')]
    [string]$Version = '7.3.9',
    [string]$Registry = 'registry.cn-shanghai.aliyuncs.com/okc',
    [string]$Builder = 'codex-multiarch',
    [switch]$SkipApp2Docker
)

$ErrorActionPreference = 'Stop'
$tag = "py-$Version"
$services = @(
    'jbm-python-cluster',
    'jbm-cluster-platform-center',
    'jbm-cluster-platform-auth',
    'jbm-cluster-platform-gateway',
    'jbm-cluster-platform-doc',
    'jbm-cluster-platform-push',
    'jbm-cluster-platform-job',
    'jbm-cluster-platform-logs',
    'jbm-cluster-platform-bigscreen'
)

if (-not $SkipApp2Docker) {
    & app2docker doctor
    if ($LASTEXITCODE -ne 0) { throw 'App2Docker doctor failed' }
    & app2docker build --source local --tag $tag --push
    if ($LASTEXITCODE -ne 0) { throw 'App2Docker build failed' }
}

& docker buildx inspect $Builder | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Buildx builder is unavailable: $Builder" }

$buildArgs = @(
    'build', '--builder', $Builder,
    '--platform', 'linux/amd64,linux/arm64',
    '--provenance=false', '--sbom=false',
    '--target', 'jbm-python-cluster',
    '--file', 'Dockerfile.app2docker',
    '--cache-from', "type=registry,ref=${Registry}/jbm-python-cluster:${tag}",
    '--push'
)
foreach ($service in $services) {
    $buildArgs += @('--tag', "${Registry}/${service}:${tag}")
}
$buildArgs += '.'

& docker buildx @buildArgs
if ($LASTEXITCODE -ne 0) { throw 'Multi-architecture image build failed' }

& docker buildx build --builder $Builder `
    --platform linux/amd64,linux/arm64 `
    --provenance=false --sbom=false `
    --target jbm-admin `
    --file Dockerfile.app2docker `
    --tag "${Registry}/jbm-admin:${tag}" `
    --push .
if ($LASTEXITCODE -ne 0) { throw 'JBM Admin multi-architecture build failed' }

& docker buildx imagetools inspect "${Registry}/jbm-python-cluster:${tag}"
if ($LASTEXITCODE -ne 0) { throw 'Published manifest verification failed' }
