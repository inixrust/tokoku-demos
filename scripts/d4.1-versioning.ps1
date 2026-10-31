<#
.SYNOPSIS
  D4.1 (slide 33): API versioning bawaan Boot 4 lewat header API-Version (product-service, L5.1-solution atau sesudahnya).
#>
$ErrorActionPreference = 'Continue'
foreach ($version in '1', '2', '3', $null) {
    $header = if ($version) { @('-H', "API-Version: $version") } else { @() }
    $label = if ($version) { "API-Version: $version" } else { 'tanpa header' }
    $body = curl.exe -s -w ' [HTTP %{http_code}]' @header http://localhost:8081/api/products/1
    Write-Host ("{0,-16} {1}" -f $label, $body)
}
