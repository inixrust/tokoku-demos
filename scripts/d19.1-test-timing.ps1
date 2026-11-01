<#
.SYNOPSIS
  D19.1 (slide 96-100): waktu jalan slice test (@WebMvcTest) vs @SpringBootTest di order-service tokoku-lab.
.DESCRIPTION
  Prasyarat: tokoku-lab L19.1-solution atau sesudahnya; profil db + kafka menyala (OrderServiceApplicationTests
  memuat seluruh context).
#>
$ErrorActionPreference = 'Continue'
Import-Module (Join-Path $PSScriptRoot 'tokoku.psm1') -Force
$lab = Get-LabPath '.'

Push-Location $lab
try {
    foreach ($test in 'OrderControllerTest', 'OrderServiceApplicationTests') {
        $sw = [Diagnostics.Stopwatch]::StartNew()
        & .\mvnw.cmd -q -B -pl order-service test "-Dtest=$test" "-Dsurefire.failIfNoSpecifiedTests=false" | Out-Null
        $sw.Stop()
        $report = Get-ChildItem "order-service\target\surefire-reports\*$test.txt" | Get-Content | Select-String 'Tests run'
        Write-Host ("{0,-30} total Maven {1,5:N1} s | {2}" -f $test, $sw.Elapsed.TotalSeconds, ($report.Line -replace '^.*Time elapsed: ', 'waktu test: ')) -ForegroundColor Cyan
    }
} finally {
    Pop-Location
}
