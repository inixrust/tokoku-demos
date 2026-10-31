<#
.SYNOPSIS
  D3.2 (slide 21): hanya flyway-core di Boot 4 -> migrasi diam-diam tidak jalan; + spring-boot-starter-flyway -> jalan.
  Butuh profil db TokoKu.
#>
$ErrorActionPreference = 'Continue'
$root = Resolve-Path (Join-Path $PSScriptRoot '..')
$compose = Join-Path $root '..\tokoku-lab\infra\compose.yaml'
$jar = Join-Path $root 'flyway-silent\target\flyway-silent-1.0.0-SNAPSHOT.jar'

function Run-Case([string]$title, [string[]]$mavenArgs) {
    docker compose -f $compose exec -T postgres psql -q -U postgres -d tokoku -c 'drop schema if exists flywaydemo cascade' *> $null
    Push-Location $root
    & .\mvnw.cmd -q -B -pl flyway-silent package -DskipTests @mavenArgs
    Pop-Location
    Write-Host "==> $title" -ForegroundColor Cyan
    java -jar $jar 2>&1 | Select-String '>>>|Successfully applied' | ForEach-Object { '    ' + $_.Line.Trim() }
}

Run-Case 'a. hanya flyway-core (pola Boot 3)' @()
Run-Case 'b. + spring-boot-starter-flyway (Boot 4)' @('-Pstarter')
