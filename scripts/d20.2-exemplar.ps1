<#
.SYNOPSIS
  D20.2 (slide 150-152): metric -> exemplar -> trace -> log, untuk POST /api/orders yang paling lambat.
.DESCRIPTION
  Prasyarat: tokoku-lab mode perf dengan observability (tag "final" atau branch capstone/broken) + beban beberapa
  menit (mis. wrk2 POST /api/orders). Di Grafana: dashboard Golden Signals > panel p99 per endpoint > titik exemplar.
  Skrip ini melakukan langkah yang sama lewat API agar bisa diulang.
  Fallback bila exemplar kosong: Explore > Tempo > Search, service order-service, durasi > p99.
#>
$ErrorActionPreference = 'Continue'
$auth = @{ Authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes('admin:admin')) }
$grafana = 'http://localhost:3000/api/datasources/proxy/uid'
$now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()

$p99 = Invoke-RestMethod -Headers $auth ("$grafana/prometheus/api/v1/query?query=" + [uri]::EscapeDataString(
    'histogram_quantile(0.99, sum by (le) (rate(http_server_requests_milliseconds_bucket{service_name="order-service",uri="/api/orders",method="POST"}[5m])))'))
Write-Host ("1. Alert/dashboard: p99 POST /api/orders = {0:N0} ms" -f [double]$p99.data.result[0].value[1]) -ForegroundColor Cyan

$ex = Invoke-RestMethod -Headers $auth ("$grafana/prometheus/api/v1/query_exemplars?start=$($now - 900)&end=$now&query=" + [uri]::EscapeDataString(
    'http_server_requests_milliseconds_bucket{service_name="order-service",uri="/api/orders",method="POST"}'))
$slowest = $ex.data | ForEach-Object { $_.exemplars } | Sort-Object { [double]$_.value } -Descending | Select-Object -First 1
if (-not $slowest) { Write-Host 'Tidak ada exemplar: pakai fallback pencarian Tempo.' -ForegroundColor Yellow; return }
$traceId = $slowest.labels.trace_id
Write-Host ("2. Exemplar terlambat: {0:N0} ms, trace_id {1}" -f [double]$slowest.value, $traceId) -ForegroundColor Cyan

$trace = Invoke-RestMethod -Headers ($auth + @{ Accept = 'application/json' }) "$grafana/tempo/api/traces/$traceId"
Write-Host '3. Span di trace itu (terlama dulu):' -ForegroundColor Cyan
$trace.batches | ForEach-Object {
    $service = ($_.resource.attributes | Where-Object key -eq 'service.name').value.stringValue
    foreach ($span in $_.scopeSpans.spans) {
        [pscustomobject]@{ Service = $service; Span = $span.name; Ms = ([long]$span.endTimeUnixNano - [long]$span.startTimeUnixNano) / 1e6 }
    }
} | Sort-Object Ms -Descending | Select-Object -First 6 | ForEach-Object { '    {0,8:N1} ms  {1,-20} {2}' -f $_.Ms, $_.Service, $_.Span }

$logQuery = [uri]::EscapeDataString("{service_name=~`".+`"} | trace_id=`"$traceId`"")
$logs = Invoke-RestMethod -Headers $auth "$grafana/loki/loki/api/v1/query_range?query=$logQuery&limit=10&start=$($now - 900)000000000&end=$($now)000000000"
Write-Host '4. Log dengan trace_id yang sama:' -ForegroundColor Cyan
$logs.data.result | ForEach-Object { $svc = $_.stream.service_name; $_.values | ForEach-Object { '    {0,-20} {1}' -f $svc, ($_[1].Substring(0, [Math]::Min(110, $_[1].Length))) } } | Select-Object -First 8
