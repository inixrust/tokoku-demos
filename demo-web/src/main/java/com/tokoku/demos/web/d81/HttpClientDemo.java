package com.tokoku.demos.web.d81;

import java.time.Duration;
import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * D8.1 (slide 51): RestClient.create() buatan sendiri vs builder yang di-inject Spring, ke downstream lambat (30 s).
 * <pre>
 * Measure-Command { curl.exe -s "localhost:8090/d81/builder" }   # gagal ±2 s (spring.http.clients.read-timeout)
 * Measure-Command { curl.exe -s "localhost:8090/d81/create" }    # menggantung 30 s: tanpa timeout
 * </pre>
 */
@RestController
@RequestMapping("/d81")
class HttpClientDemo {

	private final RestClient injected;

	private final RestClient homemade = RestClient.create("http://localhost:8090");

	HttpClientDemo(RestClient.Builder builder) {
		this.injected = builder.baseUrl("http://localhost:8090").build();
	}

	@GetMapping("/slow")
	String slow(@RequestParam(defaultValue = "30") long seconds) throws InterruptedException {
		Thread.sleep(Duration.ofSeconds(seconds));
		return "akhirnya";
	}

	@GetMapping("/builder")
	String viaBuilder() {
		return call(injected);
	}

	@GetMapping("/create")
	String viaCreate() {
		return call(homemade);
	}

	private static String call(RestClient client) {
		Instant start = Instant.now();
		try {
			String body = client.get().uri("/d81/slow").retrieve().body(String.class);
			return "OK setelah " + Duration.between(start, Instant.now()).toMillis() + " ms: " + body;
		}
		catch (RuntimeException ex) {
			return "GAGAL setelah " + Duration.between(start, Instant.now()).toMillis() + " ms: "
					+ ex.getClass().getSimpleName();
		}
	}

}
