package com.tokoku.demos.web.d51;

import jakarta.validation.constraints.Size;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * D5.1 (slide 36-37): constraint pada parameter dengan dan tanpa {@code @Validated} di class.
 * <pre>
 * curl.exe -s -i "localhost:8090/d51/validated/search?q=ko"  # 500: ConstraintViolationException (jalur AOP)
 * curl.exe -s -i "localhost:8090/d51/plain/search?q=ko"      # 400 problem+json: HandlerMethodValidationException
 * </pre>
 */
final class ValidationControllers {

	private ValidationControllers() {
	}

	@Validated
	@RestController
	@RequestMapping("/d51/validated")
	static class WithValidated {

		@GetMapping("/search")
		String search(@RequestParam @Size(min = 3) String q) {
			return "hasil untuk " + q;
		}

	}

	@RestController
	@RequestMapping("/d51/plain")
	static class Plain {

		@GetMapping("/search")
		String search(@RequestParam @Size(min = 3) String q) {
			return "hasil untuk " + q;
		}

	}

}
