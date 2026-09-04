param([switch]$NoBuild)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location -LiteralPath $projectRoot
$localDir = Join-Path $projectRoot '.local'
New-Item -ItemType Directory -Force -Path $localDir, (Join-Path $projectRoot 'logs') | Out-Null
function New-Secret([int]$Length=36) {
    $bytes = New-Object byte[] $Length
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    return [Convert]::ToBase64String($bytes)
}
$configPath = Join-Path $localDir 'settings.json'
if (-not (Test-Path -LiteralPath $configPath)) {
    $settings = [ordered]@{
        MUSIC_JWT_SECRET = New-Secret
        MUSIC_INTERNAL_SECRET = New-Secret
        MUSIC_DB_PASSWORD = New-Secret 18
        MUSIC_ADMIN_EMAIL = 'admin@cadence.local'
        MUSIC_ADMIN_PASSWORD = New-Secret 18
        MUSIC_EMAIL_ENABLED = 'false'
    }
    $settings | ConvertTo-Json | Set-Content -LiteralPath $configPath -Encoding UTF8
    "Admin email: $($settings.MUSIC_ADMIN_EMAIL)`r`nAdmin password: $($settings.MUSIC_ADMIN_PASSWORD)`r`n`r`nKeep this file private. These credentials were generated on your computer." | Set-Content -LiteralPath (Join-Path $localDir 'ADMIN-LOGIN.txt') -Encoding UTF8
}
$settings = Get-Content -LiteralPath $configPath -Raw | ConvertFrom-Json
foreach ($setting in $settings.PSObject.Properties) {
    if ($setting.Name -notmatch '^MUSIC_[A-Z_]+$') { throw 'Unexpected setting name.' }
    [Environment]::SetEnvironmentVariable($setting.Name, [string]$setting.Value, 'Process')
}
$javaExe = (Get-Command java -ErrorAction Stop).Source
$services = @(
    @{Name='discovery-server';Port=8761},
    @{Name='user-service';Port=8081},
    @{Name='song-service';Port=8082},
    @{Name='notification-service';Port=8083},
    @{Name='admin-service';Port=8090}
)
foreach ($service in $services) {
    $listener = Get-NetTCPConnection -LocalPort $service.Port -State Listen -ErrorAction SilentlyContinue
    if ($listener) { throw "Port $($service.Port) is already in use. Run Stop.cmd for this project first, or close the conflicting app. No process was stopped." }
}
if (-not $NoBuild) {
    Write-Host 'Building all services. The first build downloads dependencies and can take several minutes.'
    & (Join-Path $projectRoot 'mvnw.cmd') -B package
    if ($LASTEXITCODE -ne 0) { throw 'Build failed. Read the error above; services were not started.' }
}
$processes = @()
foreach ($service in $services) {
    $jar = Join-Path $projectRoot "$($service.Name)/target/$($service.Name)-1.0.0.jar"
    if (-not (Test-Path -LiteralPath $jar)) { throw "Missing $jar. Run Start.cmd without -NoBuild." }
    $stdout = Join-Path $projectRoot "logs/$($service.Name).log"
    $stderr = Join-Path $projectRoot "logs/$($service.Name).error.log"
    $proc = Start-Process -FilePath $javaExe -ArgumentList @('-Xms64m','-Xmx320m','-jar',('"'+$jar+'"')) -WorkingDirectory $projectRoot -WindowStyle Hidden -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
    $processes += @{Id=$proc.Id;Name=$service.Name;Jar=$jar;Started=$proc.StartTime.ToUniversalTime().ToString("o")}
    ConvertTo-Json -InputObject @($processes) | Set-Content -LiteralPath (Join-Path $localDir 'processes.json') -Encoding UTF8
    $ready = $false
    for ($attempt=0;$attempt -lt 90;$attempt++) {
        if ($proc.HasExited) { throw "$($service.Name) stopped. Open $stderr and $stdout. Run Stop.cmd before trying again." }
        try { $health=Invoke-RestMethod -Uri "http://127.0.0.1:$($service.Port)/actuator/health" -TimeoutSec 5; if($health.status -eq 'UP') {$ready=$true;break} } catch {}
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw "$($service.Name) did not become ready. See logs, then run Stop.cmd." }
    Write-Host "$($service.Name) is ready."
}
Write-Host 'Waiting briefly for service discovery...'
Start-Sleep -Seconds 15
Write-Host ''
Write-Host 'Open http://localhost:8090 in your browser.' -ForegroundColor Green
Write-Host 'Admin credentials: .local/ADMIN-LOGIN.txt'
Write-Host 'Register a normal account from the sign-in dialog.'
Write-Host 'Leave the background services running. Use Stop.cmd to stop them.'

