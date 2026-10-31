<#
.SYNOPSIS
  D10.1 (slide 77): retry 3 attempt di SETIAP layer, dependency paling bawah selalu gagal.
  Satu request user -> 3 x 3 x 3 = 27 call ke dependency. Bandingkan: retry hanya di satu layer = 3 call.
  (Slide memakai gateway sebagai ilustrasi; TokoKu tidak punya gateway, jadi di sini tiga aplikasi A -> B -> C.)
#>
$ErrorActionPreference = 'Continue'
Import-Module (Join-Path $PSScriptRoot 'chain.psm1') -Force

function Run-Storm([string]$title, [int]$attemptsA, [int]$attemptsB, [int]$attemptsC) {
    Stop-Hops
    Start-Hop A 8101 'http://localhost:8102' '0ms' $attemptsA
    Start-Hop B 8102 'http://localhost:8103' '0ms' $attemptsB
    Start-Hop C 8103 'fail' '0ms' $attemptsC
    Wait-Hops 8101, 8102, 8103
    $result = Invoke-Timed http://localhost:8101/hop
    $a = Invoke-RestMethod http://localhost:8101/calls
    $b = Invoke-RestMethod http://localhost:8102/calls
    $c = Invoke-RestMethod http://localhost:8103/calls
    Write-Host $title -ForegroundColor Cyan
    Write-Host ("    1 request user: {0}; B menerima {1}, C menerima {2}, call ke dependency {3}" -f $result, $b.diterima, $c.diterima, $c.callKeDependency) -ForegroundColor Yellow
}

Run-Storm 'a. retry 3 attempt di setiap layer' 3 3 3
Run-Storm 'b. retry hanya di layer tepat di atas dependency' 1 1 3
Stop-Hops
