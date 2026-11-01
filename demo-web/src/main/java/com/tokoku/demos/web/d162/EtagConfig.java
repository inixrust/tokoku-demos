package com.tokoku.demos.web.d162;

import java.math.BigDecimal;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * D16.2 (slide 140): ETag + If-None-Match.
 * <pre>
 * curl.exe -s -i localhost:8090/d162/products/1                          # 200 + header ETag: "0a1b..."
 * curl.exe -s -i -H 'If-None-Match: "0a1b..."' localhost:8090/d162/products/1   # 304 tanpa body
 * </pre>
 * ShallowEtagHeaderFilter menghemat bandwidth, bukan CPU server (response tetap dibuat lalu di-hash).
 */
@Configuration(proxyBeanMethods = false)
class EtagConfig {

	@Bean
	FilterRegistrationBean<ShallowEtagHeaderFilter> etagFilter() {
		FilterRegistrationBean<ShallowEtagHeaderFilter> registration = new FilterRegistrationBean<>(
				new ShallowEtagHeaderFilter());
		registration.addUrlPatterns("/d162/*");
		return registration;
	}

	@RestController
	static class ProductController {

		@GetMapping("/d162/products/{id}")
		Product product(@PathVariable long id) {
			return new Product(id, "Kopi Arabika Gayo 250 g", new BigDecimal("85000"));
		}

	}

	record Product(long id, String name, BigDecimal price) {
	}

}
