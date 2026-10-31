package com.tokoku.demos.chain;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * Satu "hop" dalam rantai synchronous. Dijalankan tiga kali (A -> B -> C) oleh scripts/d8.2-chain.ps1 dan
 * scripts/d10.1-retry-storm.ps1.
 * <ul>
 * <li>{@code hop.downstream}: URL hop berikutnya, atau {@code fail} = dependency di bawahnya selalu gagal.</li>
 * <li>{@code hop.delay}: kerja lokal per request (D8.2: 200 ms).</li>
 * <li>{@code hop.attempts}: jumlah attempt ke downstream (D10.1: 3 di setiap layer).</li>
 * </ul>
 * GET /calls = jumlah call yang diterima hop ini dan jumlah call ke dependency yang gagal.
 */
@SpringBootApplication
@RestController
public class DemoChainApplication {

	private static final Logger log = LoggerFactory.getLogger(DemoChainApplication.class);

	private final AtomicLong received = new AtomicLong();

	private final AtomicLong dependencyCalls = new AtomicLong();

	private final RestClient client;

	private final String name;

	private final String downstream;

	private final Duration delay;

	private final int attempts;

	DemoChainApplication(RestClient.Builder builder, @Value("${hop.name}") String name,
			@Value("${hop.downstream}") String downstream, @Value("${hop.delay:0ms}") Duration delay,
			@Value("${hop.attempts:1}") int attempts) {
		this.client = builder.build();
		this.name = name;
		this.downstream = downstream;
		this.delay = delay;
		this.attempts = attempts;
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoChainApplication.class, args);
	}

	@GetMapping("/hop")
	ResponseEntity<String> hop() throws InterruptedException {
		received.incrementAndGet();
		Thread.sleep(delay);
		for (int attempt = 1; attempt <= attempts; attempt++) {
			try {
				String below = callDownstream();
				return ResponseEntity.ok(name + " -> " + below);
			}
			catch (RuntimeException ex) {
				log.info("{} attempt {}/{} gagal: {}", name, attempt, attempts, ex.getClass().getSimpleName());
			}
		}
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(name + " gagal setelah " + attempts + " attempt");
	}

	private String callDownstream() {
		if ("none".equals(downstream)) {
			return "(daun)";
		}
		if ("fail".equals(downstream)) {
			dependencyCalls.incrementAndGet();
			throw new IllegalStateException("dependency mati");
		}
		return client.get().uri(downstream + "/hop").retrieve().body(String.class);
	}

	@GetMapping("/calls")
	Map<String, Object> calls() {
		return Map.of("hop", name, "diterima", received.get(), "callKeDependency", dependencyCalls.get());
	}

	@PostMapping("/calls/reset")
	void reset() {
		received.set(0);
		dependencyCalls.set(0);
	}

}
