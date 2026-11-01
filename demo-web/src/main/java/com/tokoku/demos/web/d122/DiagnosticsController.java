package com.tokoku.demos.web.d122;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * D12.2 (slide 109): bahan JFR (hot method, alokasi), thread dump JSON, dan heap dump (leak).
 * Lihat scripts/d12.2-diagnose.ps1.
 */
@RestController
@RequestMapping("/d122")
class DiagnosticsController {

	/** Leak yang disengaja: tumbuh terus, tidak pernah dibersihkan. Di heap dump: dominator teratas. */
	private static final List<byte[]> SESSION_SNAPSHOTS = new ArrayList<>();

	@PostMapping("/leak")
	synchronized Map<String, Object> leak(@RequestParam(defaultValue = "10") int mb) {
		for (int i = 0; i < mb; i++) {
			SESSION_SNAPSHOTS.add(new byte[1024 * 1024]);
		}
		return Map.of("totalMiB", SESSION_SNAPSHOTS.size());
	}

	/** Hot method untuk JFR: CPU-bound + alokasi string. */
	@GetMapping("/hot")
	Map<String, Object> hot(@RequestParam(defaultValue = "200000") int n) {
		StringBuilder sb = new StringBuilder();
		long hash = 0;
		for (int i = 0; i < n; i++) {
			String s = "order-" + i + "-" + (i * 31);
			hash += s.hashCode();
			if (i % 1000 == 0) {
				sb.setLength(0);
			}
			sb.append(s);
		}
		return Map.of("hash", hash);
	}

}
