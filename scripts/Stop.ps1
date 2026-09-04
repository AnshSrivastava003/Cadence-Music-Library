$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$manifest=Join-Path $projectRoot '.local/processes.json'
if(-not(Test-Path -LiteralPath $manifest)){Write-Host 'No recorded project processes.';exit}
$records=Get-Content -LiteralPath $manifest -Raw | ConvertFrom-Json
foreach($record in $records){
    $process=Get-Process -Id ([int]$record.Id) -ErrorAction SilentlyContinue
    if($process -and $process.ProcessName -match '^java(w)?$' -and $record.Started -and [Math]::Abs(($process.StartTime.ToUniversalTime() - ([datetime]$record.Started).ToUniversalTime()).TotalMilliseconds) -lt 10){
        Stop-Process -Id ([int]$record.Id)
        Write-Host "Stopped $($record.Name)."
    } elseif($process){Write-Host "Skipped PID $($record.Id): it no longer belongs to this project."}
}
Remove-Item -LiteralPath $manifest
Write-Host 'Done. Your database files remain in data/.'

