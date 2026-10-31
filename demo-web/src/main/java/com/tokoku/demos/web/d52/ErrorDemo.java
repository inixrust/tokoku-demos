package com.tokoku.demos.web.d52;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * D5.2 (slide 39): exception yang tidak tertangani.
 * <pre>
 * java -jar demo-web.jar --spring.profiles.active=leaky   # default error + include-message/stacktrace: SQL dan class bocor
 * java -jar demo-web.jar                                   # fallback handler: pesan generik + traceId, detail hanya di log
 * curl.exe -s "localhost:8090/d52/boom"
 * </pre>
 * Handler yang sama juga membuat D5.1 (jalur plain) menjadi problem+json.
 */
final class ErrorDemo {

	private ErrorDemo() {
	}

	@RestController
	static class BoomController {

		@GetMapping("/d52/boom")
		String boom() {
			throw new IllegalStateException(
					"SELECT * FROM customer WHERE pan='4111111111111111' failed: connection to db-prod.internal:5432 refused");
		}

	}

	@Profile("!leaky")
	@RestControllerAdvice
	static class SafeHandler extends ResponseEntityExceptionHandler {

		private static final Logger log = LoggerFactory.getLogger(SafeHandler.class);

		@ExceptionHandler(Exception.class)
		ProblemDetail unexpected(Exception ex, HttpServletRequest request) {
			String traceId = UUID.randomUUID().toString().replace("-", "");
			log.error("Unhandled error {} {} traceId={}", request.getMethod(), request.getRequestURI(), traceId, ex);
			ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
					"Unexpected error. Contact support with the traceId.");
			pd.setProperty("errorCode", "INTERNAL_ERROR");
			pd.setProperty("traceId", traceId);
			return pd;
		}

	}

}
