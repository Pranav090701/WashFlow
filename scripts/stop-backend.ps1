$ErrorActionPreference = "Stop"

$pidDir = Join-Path $PSScriptRoot ".pids"

if (!(Test-Path $pidDir)) {
    Write-Host "No script-started backend processes found."
    exit 0
}

$pidFiles = Get-ChildItem -Path $pidDir -Filter "*.pid" -File
if (!$pidFiles) {
    Write-Host "No script-started backend processes found."
    exit 0
}

foreach ($pidFile in $pidFiles) {
    $serviceName = [System.IO.Path]::GetFileNameWithoutExtension($pidFile.Name)
    $processId = Get-Content $pidFile.FullName -ErrorAction SilentlyContinue

    if (!$processId) {
        Remove-Item -LiteralPath $pidFile.FullName -Force
        continue
    }

    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if ($process) {
        Write-Host "Stopping $serviceName (PID $processId)..." -ForegroundColor Cyan
        & taskkill.exe /PID $processId /T /F | Out-Null
    } else {
        Write-Host "$serviceName is not running. Removing stale PID file." -ForegroundColor Yellow
    }

    Remove-Item -LiteralPath $pidFile.FullName -Force
}

Write-Host "Backend services stopped." -ForegroundColor Green
