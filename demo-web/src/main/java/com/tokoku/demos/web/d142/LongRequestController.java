package com.tokoku.demos.web.d142;

import java.time.Duration;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * D14.2 (slide 125-126): request panjang saat shutdown. Lihat scripts/d14.2-graceful.ps1.
 * Graceful shutdown (default Boot): request yang sedang berjalan diselesaikan, request baru ditolak.
 */
@RestController
class LongRequestController {

	@GetMapping("/d142/long")
	String longRequest(@RequestParam(defaultValue = "20") long seconds) throws InterruptedException {
		Thread.sleep(Duration.ofSeconds(seconds));
		return "selesai setelah " + seconds + " s";
	}

}
