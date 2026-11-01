<#
.SYNOPSIS
  D7.1 (slide 58): dual write + crash -> event hilang; outbox + crash -> event tetap terkirim setelah restart.
  Butuh profil db + kafka TokoKu dan demo-jpa sudah di-build.
#>
$ErrorActionPreference = 'Continue'
Import-Module (Join-Path $PSScriptRoot 'demo-jpa.psm1') -Force
Stop-DemoJpa

function Post([string]$path) {
    try { Invoke-RestMethod -Method Post "http://localhost:8095$path" -TimeoutSec 10 } catch { '(proses mati: koneksi terputus)' }
}

Write-Host '==> a. Dual write: commit order, CRASH sebelum publish ke Kafka' -ForegroundColor Cyan
Start-DemoJpa | Out-Null
$before = Invoke-RestMethod http://localhost:8095/d71/check
Write-Host "    sebelum: $($before.orders) order, $($before.events) event"
Post '/d71/dual?crash=true' | Out-Null
Start-Sleep 2
Start-DemoJpa | Out-Null
$after = Invoke-RestMethod http://localhost:8095/d71/check
Write-Host "    sesudah restart: $($after.orders) order, $($after.events) event, order tanpa event: $($after.ordersTanpaEvent -join ', ')" -ForegroundColor Yellow
Stop-DemoJpa

Write-Host '==> b. Outbox: commit order + outbox, CRASH, lalu restart dengan relay aktif' -ForegroundColor Cyan
Start-DemoJpa | Out-Null
Post '/d71/outbox?crash=true' | Out-Null
Start-Sleep 2
Start-DemoJpa -AppArgs @('--demo.relay-enabled=true') | Out-Null
Start-Sleep 3
$final = Invoke-RestMethod http://localhost:8095/d71/check
Write-Host "    sesudah restart + relay: $($final.orders) order, $($final.events) event, order tanpa event: $($final.ordersTanpaEvent -join ', ')" -ForegroundColor Green
Write-Host '    Order dari kasus (a) tetap tanpa event: dual write tidak bisa dipulihkan. Kasus (b) terkirim.'
Stop-DemoJpa
