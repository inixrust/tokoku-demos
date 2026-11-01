package com.tokoku.demos.web.d161;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * D16.1 (slide 138-139): tiga call downstream masing-masing 300 ms.
 * <pre>
 * curl.exe -s localhost:8090/d161/sequential   # ±900 ms
 * curl.exe -s localhost:8090/d161/parallel     # ±300 ms (virtual thread per call)
 * </pre>
 */
@RestController
@RequestMapping("/d161")
class FanOutController {

	private static final List<String> PARTS = List.of("product", "stock", "review");

	private final RestClient client;

	FanOutController(RestClient.Builder builder) {
		this.client = builder.baseUrl("http://localhost:8090").build();
	}

	@GetMapping("/backend")
	String backend(@RequestParam String part) throws InterruptedException {
		Thread.sleep(Duration.ofMillis(300));
		return part + ":ok";
	}

	@GetMapping("/sequential")
	Map<String, Object> sequential() {
		long start = System.nanoTime();
		List<String> results = PARTS.stream().map(this::fetch).toList();
		return Map.of("results", results, "ms", (System.nanoTime() - start) / 1_000_000);
	}

	@GetMapping("/parallel")
	Map<String, Object> parallel() {
		long start = System.nanoTime();
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			List<CompletableFuture<String>> futures = PARTS.stream()
				.map(part -> CompletableFuture.supplyAsync(() -> fetch(part), executor))
				.toList();
			List<String> results = futures.stream().map(CompletableFuture::join).toList();
			return Map.of("results", results, "ms", (System.nanoTime() - start) / 1_000_000);
		}
	}

	private String fetch(String part) {
		return client.get().uri("/d161/backend?part={part}", part).retrieve().body(String.class);
	}

}
