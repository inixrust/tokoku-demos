<#
.SYNOPSIS
  D7.4 (slide 58): EXPLAIN (ANALYZE) untuk urutan kolom composite index, pada data perf TokoKu (seed-perf.ps1).
.DESCRIPTION
  Di dalam satu transaction yang di-ROLLBACK (DDL PostgreSQL transactional): data asli tidak berubah.
  a. index (status, customer_id)                   -> leftmost column tidak cocok dengan pola query
  b. index (customer_id, status, created_at desc)  -> equality dulu, ORDER BY terakhir: Index Scan, tanpa Sort
#>
$ErrorActionPreference = 'Continue'
Import-Module (Join-Path $PSScriptRoot 'tokoku.psm1') -Force
$compose = Get-LabPath 'infra\compose.yaml'
$query = "select id, status, created_at from orders.orders where customer_id = '7f2c1a4e-0b1d-4c55-9a6e-a11ce0000001' and status = 'PENDING' order by created_at desc limit 20"

function Explain([string]$title, [string]$index) {
    $sql = @"
begin;
drop index if exists orders.idx_orders_cust_status_created;
create index demo_idx on orders.orders $index;
analyze orders.orders;
explain (analyze, costs off, timing off) $query;
rollback;
"@
    Write-Host "==> $title  $index" -ForegroundColor Cyan
    $sql | docker compose -f $compose exec -T postgres psql -q -U postgres -d tokoku | Select-String 'Scan|Sort|Execution|rows=' |
        ForEach-Object { '    ' + $_.Line.Trim() }
}

Explain 'a.' '(status, customer_id)'
Explain 'b.' '(customer_id, status, created_at desc)'
