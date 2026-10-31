package com.tokoku.demos.web.d171;

import java.time.Duration;
import java.util.Map;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * D17.1 (slide 142-143): loader lambat 500 ms, TTL 5 s.
 * <pre>
 * .\loadtest\wrk2\run.ps1 -Rate 100 -Duration 60s -Url http://host.docker.internal:8090/d171/expire/1    # lonjakan p99 tiap 5 s
 * .\loadtest\wrk2\run.ps1 -Rate 100 -Duration 60s -Url http://host.docker.internal:8090/d171/refresh/1   # lonjakan hilang
 * </pre>
 * expireAfterWrite: request pertama setelah expired menunggu reload. refreshAfterWrite: nilai lama dikembalikan
 * seketika, reload asynchronous. Di Spring Cache, refreshAfterWrite di caffeine.spec butuh bean CacheLoader:
 * jalankan dengan --spring.profiles.active=refresh-no-loader untuk melihat aplikasi gagal start.
 */
@RestController
@RequestMapping("/d171")
class RefreshVsExpireController {

	private final LoadingCache<String, String> expiring = Caffeine.newBuilder()
		.maximumSize(1000)
		.expireAfterWrite(Duration.ofSeconds(5))
		.build(RefreshVsExpireController::slowLoad);

	private final LoadingCache<String, String> refreshing = Caffeine.newBuilder()
		.maximumSize(1000)
		.refreshAfterWrite(Duration.ofSeconds(5))
		.expireAfterWrite(Duration.ofMinutes(1))
		.build(RefreshVsExpireController::slowLoad);

	@GetMapping("/expire/{key}")
	Map<String, String> expire(@PathVariable String key) {
		return Map.of("value", expiring.get(key));
	}

	@GetMapping("/refresh/{key}")
	Map<String, String> refresh(@PathVariable String key) {
		return Map.of("value", refreshing.get(key));
	}

	private static String slowLoad(String key) throws InterruptedException {
		Thread.sleep(Duration.ofMillis(500));
		return "harga-" + key + "@" + System.currentTimeMillis();
	}

}
