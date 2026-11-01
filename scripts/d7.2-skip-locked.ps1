<#
.SYNOPSIS
  D7.2 (slide 60): dua instance relay + FOR UPDATE SKIP LOCKED -> batch berbeda, tanpa duplikat.
#>
$ErrorActionPreference = 'Continue'
Import-Module (Join-Path $PSScriptRoot 'demo-jpa.psm1') -Force
Stop-DemoJpa

Start-DemoJpa -Port 8095 -AppArgs @('--demo.relay-enabled=true') | Out-Null
Start-DemoJpa -Port 8096 -AppArgs @('--demo.relay-enabled=true') | Out-Null
Write-Host '==> 500 baris outbox baru, dua relay berjalan bersamaan' -ForegroundColor Cyan
Invoke-RestMethod -Method Post 'http://localhost:8095/d72/burst?n=500' | Out-Null
Start-Sleep 8

foreach ($port in 8095, 8096) {
    Write-Host "--- log relay instance $port (3 batch terakhir)"
    Get-Content (Join-Path $env:TEMP "demo-jpa-$port.log") | Select-String 'mengirim outbox' | Select-Object -Last 3 |
        ForEach-Object { '    ' + ($_.Line -replace '^.*: instance', 'instance') }
}
$stats = Invoke-RestMethod http://localhost:8095/d72/stats
Write-Host '--- baris outbox per instance:'
$stats.outboxPerInstance | ForEach-Object { "    {0,-6} {1}" -f $_.instance, $_.rows }
Write-Host "--- event burst di Kafka: $($stats.burstEvents), key duplikat: $($stats.burstKeyDuplikat)" -ForegroundColor Green
Stop-DemoJpa
