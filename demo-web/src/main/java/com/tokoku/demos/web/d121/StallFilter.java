package com.tokoku.demos.web.d121;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * D12.1 (slide 106-108): aplikasi "stall" 2 s setiap 10 s (seperti GC pause). Semua request ke /d121/** yang
 * datang di jendela stall menunggu sampai jendela berakhir.
 * <pre>
 * .\loadtest\wrk2\run.ps1 -Closed -Duration 60s -Connections 100 -Url http://host.docker.internal:8090/d121/fast        # wrk (closed)
 * .\loadtest\wrk2\run.ps1 -Rate 200 -Duration 60s -Connections 100 -Url http://host.docker.internal:8090/d121/fast      # wrk2 -R
 * </pre>
 * Closed model: selama stall, load generator ikut berhenti mengirim, sehingga hanya beberapa request yang tercatat lambat.
 * Open model: request terus datang dan mengantre, sehingga p99.9 mendekati 2 s. Rata-rata menipu.
 */
@Component
class StallFilter extends OncePerRequestFilter {

	private static final long PERIOD_MS = 10_000;

	private static final long STALL_MS = 2_000;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getRequestURI().startsWith("/d121/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		long inPeriod = System.currentTimeMillis() % PERIOD_MS;
		if (inPeriod < STALL_MS) {
			try {
				Thread.sleep(STALL_MS - inPeriod);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
		chain.doFilter(request, response);
	}

	@RestController
	static class FastController {

		@GetMapping("/d121/fast")
		String fast() {
			return "ok";
		}

	}

}
