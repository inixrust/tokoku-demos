# TokoKu Demos

Demo instruktur untuk pelatihan **Spring Boot 4 & Java 21: Microservices dan Performance Tuning**. Repo ini terpisah
dari `tokoku-lab` agar repo lab tetap bersih. Letakkan kedua repo bersebelahan:

```
Spring from 0 to 1/
├─ tokoku-lab/     # sistem TokoKu + lab
└─ tokoku-demos/   # repo ini
```

Build semua: `.\mvnw.cmd package -DskipTests`. Infra (bila dibutuhkan) dari tokoku-lab:
`docker compose -f ..\tokoku-lab\infra\compose.yaml --profile db --profile kafka up -d`.
Skrip PowerShell: `powershell -ExecutionPolicy Bypass -File .\scripts\<skrip>.ps1` bila policy menolak.
wrk2/wrk: `..\tokoku-lab\loadtest\wrk2\run.ps1`.

## Isi

| Folder | Isi | Port |
|---|---|---|
| `java21/` | Program single-file (`java X.java`), tanpa Spring | — |
| `demo-web/` | Boot tanpa database | 8090 |
| `demo-jpa/` | Boot + PostgreSQL (schema `demo`) + Kafka | 8095 (8096 untuk instance kedua) |
| `demo-chain/` | Satu "hop", dijalankan 3× (A → B → C) | 8101–8103 |
| `flyway-silent/` | Flyway tanpa starter vs dengan starter | — |
| `scripts/` | Skrip per demo, termasuk demo yang memakai sistem TokoKu | — |

## Indeks demo

Demo yang isinya sama dengan langkah lab sudah dipindah ke lab (rencana §7): D3.3 → L3.1, D7.3 → L7.1, D11.2 → L11.1,
D13.1 & D13.3 → L13.1, D21.2 → L21.1.

### Hari 1

| ID | Slide | Cara menjalankan | Momen kunci (terverifikasi saat build) |
|---|---|---|---|
| D0 | 3 | tokoku-lab tag `final`, mode perf → `scripts\d0-tour.ps1` | Satu trace: HTTP order → HTTP inventory → Kafka (inventory + notification) → Kafka (order) |
| D2.1 | 16 | `java java21\PaymentResults.java`, lalu buka komentar `Refunded` | Compile error *the switch expression does not cover all possible input values* |
| D2.2 | 17 | `java java21\VirtualVsPlatform.java io` / `cpu` | io: pool 200 = 50,6 s vs VT = 1,0 s · cpu: sama (1,5 s) |
| D2.3 | 18 | lihat header `java21\Pinning.java` (JDK 21 vs 25, JFR) | JDK 21: 50 event `jdk.VirtualThreadPinned`, 2,7 s (parallelism=2) · JDK 25: 0 event, 0,1 s |
| D3.1 | 21–22 | `scripts\d3.1-starters.ps1` | `starter-web` → `starter-webmvc`, `flyway-core` → `starter-flyway`, pasangan `-test` per starter |
| D3.2 | 21 | `scripts\d3.2-flyway-silent.ps1` | Hanya `flyway-core`: migrasi tidak jalan, tanpa error · + starter: *Successfully applied 1 migration* |

> D3.1: start.spring.io kini hanya melayani Boot ≥ 4.0, sehingga pom Boot 3.5 disimpan di `scripts\d3.1\`.

### Hari 2

| ID | Slide | Cara menjalankan | Momen kunci |
|---|---|---|---|
| D4.1 | 33 | product-service (≥ `L5.1-solution`) → `scripts\d4.1-versioning.ps1` | v1/v2 beda bentuk; versi 3 dan tanpa header → 400 |
| D4.2 | 34 | demo-web: `/d42/page` vs `/d42/paged-model` | `Page` langsung: JSON internal + WARN *Serializing PageImpl instances as-is is not supported* |
| D5.1 | 36–37 | demo-web: `/d51/validated/search?q=ko` vs `/d51/plain/search?q=ko` | `@Validated` di class → 500 (ConstraintViolation) · tanpa → 400 problem+json |
| D5.2 | 39 | demo-web `/d52/boom`: profil `leaky` vs default | `leaky`: SQL, PAN, host, stack trace bocor · default: pesan generik + `traceId` |
| D6.1 | 45 | demo-jpa `/d61/orders/1`: profil `osiv` vs default; `/d61/orders/1/fixed` | OSIV: query lazy di luar transaction · tanpa OSIV: `LazyInitializationException` · fix: `@EntityGraph` |
| D6.2 | 43, 47 | demo-jpa `POST /d62/identity?n=1000` vs `/d62/sequence?n=1000` | IDENTITY: 1000 statement, 0 batch, 370 ms · SEQUENCE: 22 statement, 20 batch, 42 ms |
| D6.3 | 43 | demo-jpa `POST /d63/race?versioned=false` / `true` | tanpa `@Version`: saldo 1100 (lost update) · dengan: satu 409 |
| D8.1 | 63 | demo-web: `/d81/builder` vs `/d81/create` | builder Spring: gagal 2 s (timeout global) · `RestClient.create()`: menggantung 30 s |
| D8.2 | 12 | `scripts\d8.2-chain.ps1` | 3 hop × 200 ms = ±650 ms · hop C mati → A ikut gagal (502) |

### Hari 3

| ID | Slide | Cara menjalankan | Momen kunci |
|---|---|---|---|
| D7.1 | 52 | `scripts\d7.1-dual-write.ps1` | Dual write + crash: order tanpa event · outbox + crash: event terkirim setelah restart |
| D7.2 | 54 | `scripts\d7.2-skip-locked.ps1` | Dua relay mengambil batch berbeda (382–401 / 402–421 …), 500 event, 0 duplikat |
| D7.4 | 58 | data perf TokoKu → `scripts\d7.4-explain.ps1` | `(status, customer_id)`: bitmap scan + sort 3,5 ms · `(customer_id, status, created_at desc)`: index scan 0,04 ms |
| D20.1 | 70–71 | tokoku-lab ≥ `L8.1B-start`, Grafana **TokoKu - Golden Signals** | Latency (percentile), traffic, error, saturation per service |
| D9.1 | 66–67 | tokoku-lab branch `homework/L9.1` (lihat `docs/labs/L9.1.md`) | 2 instance di Eureka, round-robin; satu mati → 503 ±12 s lalu pulih |
| D9.2 | 68 | `scripts\d9.2-configtree.ps1` | Tanpa mount: tidak terbaca · dengan `/etc/config/db-password`: terbaca, tidak pernah dicetak utuh |
| D10.1 | 77 | `scripts\d10.1-retry-storm.ps1` | Retry 3 di setiap layer: **27 call** ke dependency · satu layer: 3 |

### Hari 4

| ID | Slide | Cara menjalankan | Momen kunci |
|---|---|---|---|
| D10.2 | 78, 81 | tokoku-lab ≥ `L10.1-solution` (mode dev) → `scripts\d10.2-breaker.ps1` + dashboard Resilience | CLOSED → OPEN (fail fast) → HALF_OPEN → CLOSED |
| D10.3 | 80 | order-service dengan `--tokoku.payments.gateway-delay=500ms` → `scripts\d10.3-idempotency.ps1` | Key sama → replay · payload beda → 422 · 5 paralel → 201 + 4× 409, satu payment |
| D11.1 | 86–88 | `scripts\d11-tokens.ps1` | Token alice (PKCE) vs `tokoku-loadtest`: `sub`, `aud`, `scope`, `exp` |
| D11.3 | 88 | `scripts\d11-tokens.ps1` (bagian akhir) | Token `aud=inventory-service` → 401 |
| D19.1 | 95–99 | `scripts\d19.1-test-timing.ps1` | `@WebMvcTest` vs `@SpringBootTest` untuk order-service |
| D12.1 | 105–107 | demo-web `/d121/fast` (stall 2 s / 10 s), perintah di `StallFilter.java` | wrk closed: p99 7,8 ms · wrk2 `-R200 -c100`: p99.9 2,0 s |

> D12.1: wrk2 butuh koneksi cukup (`-Connections 100`). Dengan 20 koneksi, p99 melonjak ke belasan detik karena
> antrean per koneksi (artefak alat, bukan aplikasi).

### Hari 5

| ID | Slide | Cara menjalankan | Momen kunci |
|---|---|---|---|
| D12.2 | 108 | `scripts\d12.2-diagnose.ps1` → `out\` | JFR: hot method `DiagnosticsController.hot` · thread dump JSON · heap dump 250 MiB: dominator `SESSION_SNAPSHOTS` |
| D13.2 | 117 | `scripts\d13.2-oom.ps1` | `-Xmx1g` di limit 1 GiB: OOMKilled, exit 137, tanpa pesan · `MaxRAMPercentage=70`: OutOfMemoryError bersih, exit 3 |
| D14.1 | 122 | demo-jpa profil `vt`, perintah di `PoolBottleneckController.java` | 300 rps ke query 100 ms, pool 10: tercapai 97 rps, pending 90 |
| D14.2 | 124–125 | `scripts\d14.2-graceful.ps1` | `stop -t 40`: request 20 s selesai · timeout Spring 5 s: terpotong · `stop` tanpa `-t`: SIGKILL (exit 137) |
| D15.1 | 127–128 | demo-jpa `/d151/join-fetch?page=1&size=5` (`&variant=left`, `&variant=filtered`) | **Hibernate 7.4: lihat catatan** |

> D14.2: dokumentasi Docker menyebut default `docker stop` 10 s; di Docker Engine 29.9 laptop instruktur terukur 3,3 s.
>
> D15.1: Hibernate 7.4 **tidak lagi** memotong halaman di memori untuk `JOIN FETCH` + `Pageable`. Query ditulis ulang
> menjadi subquery dengan paging di SQL, sehingga `fail_on_pagination_over_collection_fetch` tidak terpicu. Varian
> `filtered` (filter pada collection yang di-fetch) menghasilkan **SQL error** (`missing FROM-clause entry`). Pesan slide
> tetap: fetch plan per use case; pola dua tahap dapat diprediksi. Slide 127–128 perlu diperbarui.

### Hari 6

| ID | Slide | Cara menjalankan | Momen kunci |
|---|---|---|---|
| D16.1 | 137–138 | demo-web `/d161/sequential` vs `/d161/parallel` | 931 ms vs 307 ms |
| D16.2 | 139 | demo-web `/d162/products/1` + `If-None-Match` | 304 tanpa body |
| D17.1 | 142–143 | demo-web `/d171/expire/1` vs `/d171/refresh/1` (wrk2 100 rps); profil `refresh-no-loader` | expire: p99 500 ms · refresh: p99 3,9 ms · tanpa `CacheLoader`: *refreshAfterWrite requires a LoadingCache* |
| D20.2 | 150–152 | mode perf dengan observability (tag `final` / capstone) + beban → `scripts\d20.2-exemplar.ps1` | p99 → exemplar → trace (span terlama) → log dengan trace ID yang sama |
| D21.1 | 154 | tokoku-lab `L21.1-solution`, image `product-service:1.0` → `scripts\d21.1-liveness-db.ps1` | Liveness memeriksa DB: CrashLoopBackOff · benar: 0/1, 0 restart |
