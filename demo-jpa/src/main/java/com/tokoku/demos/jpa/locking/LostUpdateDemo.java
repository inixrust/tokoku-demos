package com.tokoku.demos.jpa.locking;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * D6.3 (slide 43): dua update paralel (masing-masing +100) pada saldo 1000, read-modify-write dengan jeda 300 ms.
 * <pre>
 * curl.exe -s -X POST "localhost:8095/d63/race?versioned=false"  # saldo akhir 1100: satu update HILANG (lost update)
 * curl.exe -s -X POST "localhost:8095/d63/race?versioned=true"   # satu sukses, satu OptimisticLockException -> 409
 * </pre>
 */
@RestController
class LostUpdateDemo {

	private final EntityManager entityManager;

	private final TransactionTemplate transaction;

	LostUpdateDemo(EntityManager entityManager, TransactionTemplate transaction) {
		this.entityManager = entityManager;
		this.transaction = transaction;
	}

	@PostMapping("/d63/race")
	Map<String, Object> race(@RequestParam boolean versioned) {
		Class<?> type = versioned ? Account.class : PlainAccount.class;
		transaction.executeWithoutResult(s -> entityManager
			.createNativeQuery("update " + (versioned ? "account" : "plain_account") + " set balance = 1000 where id = 1")
			.executeUpdate());
		List<String> outcomes = new ArrayList<>();
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var first = CompletableFuture.supplyAsync(() -> addHundred(type), executor);
			var second = CompletableFuture.supplyAsync(() -> addHundred(type), executor);
			outcomes.add(first.join());
			outcomes.add(second.join());
		}
		Object balance = transaction.execute(s -> entityManager
			.createNativeQuery("select balance from " + (versioned ? "account" : "plain_account") + " where id = 1")
			.getSingleResult());
		return Map.of("awal", 1000, "duaUpdate", "+100 dan +100", "saldoAkhir", balance, "hasil", outcomes);
	}

	private String addHundred(Class<?> type) {
		try {
			transaction.executeWithoutResult(status -> {
				Object account = entityManager.find(type, 1L);
				sleep();
				if (account instanceof Account a) {
					a.balance = a.balance.add(BigDecimal.valueOf(100));
				}
				else if (account instanceof PlainAccount p) {
					p.balance = p.balance.add(BigDecimal.valueOf(100));
				}
			});
			return "200 OK";
		}
		catch (ObjectOptimisticLockingFailureException ex) {
			return "409 Conflict (OptimisticLockException)";
		}
	}

	private static void sleep() {
		try {
			Thread.sleep(Duration.ofMillis(300));
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	@Entity
	@Table(name = "account")
	static class Account {

		@Id
		Long id;

		BigDecimal balance;

		@Version
		long version;

	}

	@Entity
	@Table(name = "plain_account")
	static class PlainAccount {

		@Id
		Long id;

		BigDecimal balance;

	}

}
