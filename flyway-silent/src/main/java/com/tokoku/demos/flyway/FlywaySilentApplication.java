package com.tokoku.demos.flyway;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;

/** D3.2: start, cek apakah migrasi V1 benar-benar dijalankan, lalu keluar. Tidak ada error di kedua kasus. */
@SpringBootApplication
public class FlywaySilentApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlywaySilentApplication.class, args).close();
	}

	@Bean
	CommandLineRunner check(JdbcClient jdbc) {
		return args -> {
			boolean migrated = jdbc.sql("""
					select count(*) from information_schema.tables
					where table_schema = 'flywaydemo' and table_name = 'demo_item'""").query(Long.class).single() > 0;
			System.out.println();
			System.out.println(migrated ? ">>> Migrasi JALAN: tabel flywaydemo.demo_item ada."
					: ">>> Migrasi TIDAK jalan: tabel flywaydemo.demo_item tidak ada (tanpa error, tanpa warning).");
			System.out.println();
		};
	}

}
