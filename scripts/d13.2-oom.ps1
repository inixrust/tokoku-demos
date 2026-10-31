<#
.SYNOPSIS
  D13.2: -Xmx = memory limit -> OOMKilled (exit 137) vs MaxRAMPercentage + ExitOnOutOfMemoryError (exit 3).
#>
$ErrorActionPreference = 'Continue'
$src = (Resolve-Path (Join-Path $PSScriptRoot '..\java21')).Path

function Run-Hog([string]$name, [string]$javaOpts) {
    Write-Host "==> $name  ($javaOpts)" -ForegroundColor Cyan
    docker rm -f d132 *> $null
    docker run --name d132 -m 1g --memory-swap 1g -v "${src}:/src:ro" eclipse-temurin:21-jdk `
        sh -c "java $javaOpts /src/MemoryHog.java" 2>&1 | Select-Object -Last 4
    $state = docker inspect d132 --format '{{.State.OOMKilled}} {{.State.ExitCode}}'
    Write-Host "    OOMKilled / ExitCode = $state" -ForegroundColor Yellow
    docker rm -f d132 *> $null
}

Run-Hog 'a. -Xmx = limit' '-Xmx1g'
Run-Hog 'b. heap relatif ke limit' '-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError'
