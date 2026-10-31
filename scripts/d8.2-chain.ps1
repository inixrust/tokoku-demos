<#
.SYNOPSIS
  D8.2 (slide 12): rantai synchronous 3 hop, masing-masing 200 ms. Latency menjumlah; satu hop mati = semua gagal.
#>
$ErrorActionPreference = 'Continue'
Import-Module (Join-Path $PSScriptRoot 'chain.psm1') -Force
Stop-Hops
Start-Hop A 8101 'http://localhost:8102' '200ms'
Start-Hop B 8102 'http://localhost:8103' '200ms'
Start-Hop C 8103 'none' '200ms'
Wait-Hops 8101, 8102, 8103
Invoke-RestMethod http://localhost:8101/hop | Out-Null   # warm-up

Write-Host "a. A -> B -> C sehat : $(Invoke-Timed http://localhost:8101/hop)   (~600 ms = 3 x 200 ms)" -ForegroundColor Cyan
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match 'hop.name=C' } |
    ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
Start-Sleep 1
Write-Host "b. C mati           : $(Invoke-Timed http://localhost:8101/hop)   (A dan B ikut gagal)" -ForegroundColor Yellow
Stop-Hops
