package com.tokoku.demos.web.d92;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * D9.2 (slide 68): secret dari folder yang di-mount (config tree), tidak ada di repo maupun di image.
 * Lihat scripts/d9.2-configtree.ps1. Nilai tidak pernah dicetak utuh.
 */
@RestController
class ConfigTreeController {

	private final String password;

	ConfigTreeController(@Value("${demo.db-password}") String password) {
		this.password = password;
	}

	@GetMapping("/d92/secret")
	Map<String, Object> secret() {
		boolean present = !password.startsWith("<");
		return Map.of("terbaca", present, "panjang", present ? password.length() : 0,
				"awalan", present ? password.charAt(0) + "***" : "-");
	}

}
