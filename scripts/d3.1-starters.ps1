<#
.SYNOPSIS
  D3.1 (slide 21-22): dependency Boot 3.5 vs Boot 4.1 untuk pilihan yang sama (Web, JPA, Flyway, Validation, PostgreSQL).
.DESCRIPTION
  Boot 4.1: di-generate langsung dari start.spring.io. Boot 3.5: pom referensi di scripts/d3.1 (start.spring.io
  tidak lagi melayani Boot < 4.0). Momen kunci: starter per teknologi + pasangan -test; flyway-core kini starter.
#>
$ErrorActionPreference = 'Continue'
$work = Join-Path $env:TEMP 'd31'
Remove-Item -Recurse -Force $work -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $work | Out-Null

$url = 'https://start.spring.io/starter.zip?type=maven-project&bootVersion=4.1.2&javaVersion=21&dependencies=web,data-jpa,flyway,validation,postgresql'
Invoke-WebRequest -UseBasicParsing $url -OutFile (Join-Path $work 'boot41.zip')
Expand-Archive (Join-Path $work 'boot41.zip') -DestinationPath (Join-Path $work 'boot41')

function Get-Deps([string]$pom) {
    ([xml](Get-Content $pom -Raw)).project.dependencies.dependency | ForEach-Object {
        $scope = if ($_.scope) { " ($($_.scope))" } else { '' }
        "$($_.artifactId)$scope"
    }
}

Write-Host '==> Boot 3.5' -ForegroundColor Cyan
Get-Deps (Join-Path $PSScriptRoot 'd3.1\boot-3.5-pom.xml') | ForEach-Object { "    $_" }
Write-Host '==> Boot 4.1.2 (start.spring.io)' -ForegroundColor Cyan
Get-Deps (Join-Path $work 'boot41\pom.xml') | ForEach-Object { "    $_" }
