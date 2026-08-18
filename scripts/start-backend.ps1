param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$pidDir = Join-Path $PSScriptRoot ".pids"
$logDir = Join-Path $repoRoot "logs"

$services = @(
    "auth-service",
    "customer-service",
    "washer-service",
    "booking-service",
    "payment-service",
    "notification-service",
    "api-gateway"
)

New-Item -ItemType Directory -Force -Path $pidDir | Out-Null
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

function Get-MavenCommand($servicePath) {
    $mavenWrapper = Join-Path $servicePath "mvnw.cmd"
    if (Test-Path $mavenWrapper) {
        return $mavenWrapper
    }

    $mavenCommand = Get-Command "mvn.cmd" -ErrorAction SilentlyContinue
    if ($mavenCommand) {
        return $mavenCommand.Source
    }

    $mavenCommand = Get-Command "mvn" -ErrorAction SilentlyContinue
    if ($mavenCommand) {
        return $mavenCommand.Source
    }

    throw "Maven was not found on PATH. Install Maven or add mvn.cmd to PATH."
}

function Test-ServiceAlreadyStarted($serviceName) {
    $pidFile = Join-Path $pidDir "$serviceName.pid"
    if (!(Test-Path $pidFile)) {
        return $false
    }

    $processId = Get-Content $pidFile -ErrorAction SilentlyContinue
    if (!$processId) {
        Remove-Item -LiteralPath $pidFile -Force
        return $false
    }

    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if ($process) {
        return $true
    }

    Remove-Item -LiteralPath $pidFile -Force
    return $false
}

foreach ($serviceName in $services) {
    $servicePath = Join-Path $repoRoot $serviceName
    if (!(Test-Path $servicePath)) {
        Write-Warning "$serviceName folder not found. Skipping."
        continue
    }

    if (Test-ServiceAlreadyStarted $serviceName) {
        Write-Host "$serviceName is already running from scripts/.pids. Skipping." -ForegroundColor Yellow
        continue
    }

    $maven = Get-MavenCommand $servicePath
    $arguments = if ($SkipBuild) {
        "-DskipTests spring-boot:run"
    } else {
        "spring-boot:run"
    }

    $stdoutLog = Join-Path $logDir "$serviceName.out.log"
    $stderrLog = Join-Path $logDir "$serviceName.err.log"

    Write-Host "Starting $serviceName..." -ForegroundColor Cyan
    $process = Start-Process `
        -FilePath $maven `
        -ArgumentList $arguments `
        -WorkingDirectory $servicePath `
        -RedirectStandardOutput $stdoutLog `
        -RedirectStandardError $stderrLog `
        -WindowStyle Hidden `
        -PassThru

    Set-Content -Path (Join-Path $pidDir "$serviceName.pid") -Value $process.Id
    Write-Host "  PID $($process.Id), logs: logs/$serviceName.out.log and logs/$serviceName.err.log"
}

Write-Host ""
Write-Host "Backend startup triggered. Give Spring Boot a little time, then open the UI." -ForegroundColor Green
Write-Host "Stop all script-started services with: .\scripts\stop-backend.ps1"
