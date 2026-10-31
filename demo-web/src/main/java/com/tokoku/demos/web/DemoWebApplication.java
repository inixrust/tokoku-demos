package com.tokoku.demos.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Demo tanpa database: satu package per demo (d42, d51, ...). Lihat README di root tokoku-demos. */
@SpringBootApplication
public class DemoWebApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoWebApplication.class, args);
	}

}
