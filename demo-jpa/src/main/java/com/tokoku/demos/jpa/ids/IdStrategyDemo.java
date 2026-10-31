package com.tokoku.demos.jpa.ids;

import java.util.Map;
import java.util.function.IntFunction;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * D6.2 (slide 43, 47): insert 1.000 baris dengan IDENTITY vs SEQUENCE (+ hibernate.jdbc.batch_size=50).
 * <pre>
 * curl.exe -s -X POST "localhost:8095/d62/identity?n=1000"   # 1000 statement, 0 batch: IDENTITY mematikan batching
 * curl.exe -s -X POST "localhost:8095/d62/sequence?n=1000"   # ±20 batch insert + 20 nextval
 * </pre>
 * Lihat juga log "Session Metrics": "... executing N JDBC batches".
 */
@RestController
class IdStrategyDemo {

	private final EntityManager entityManager;

	private final TransactionTemplate transaction;

	private final Statistics statistics;

	IdStrategyDemo(EntityManager entityManager, TransactionTemplate transaction, EntityManagerFactory emf) {
		this.entityManager = entityManager;
		this.transaction = transaction;
		this.statistics = emf.unwrap(SessionFactory.class).getStatistics();
	}

	@PostMapping("/d62/identity")
	Map<String, Object> identity(@RequestParam(defaultValue = "1000") int n) {
		return insert(n, i -> new IdentityItem("item-" + i));
	}

	@PostMapping("/d62/sequence")
	Map<String, Object> sequence(@RequestParam(defaultValue = "1000") int n) {
		return insert(n, i -> new SeqItem("item-" + i));
	}

	private Map<String, Object> insert(int n, IntFunction<Object> factory) {
		statistics.clear();
		long start = System.nanoTime();
		transaction.executeWithoutResult(status -> {
			for (int i = 0; i < n; i++) {
				entityManager.persist(factory.apply(i));
			}
		});
		return Map.of("rows", n, "ms", (System.nanoTime() - start) / 1_000_000,
				"preparedStatements", statistics.getPrepareStatementCount(),
				"entityInserts", statistics.getEntityInsertCount());
	}

	@Entity
	@Table(name = "identity_item")
	static class IdentityItem {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		Long id;

		String name;

		protected IdentityItem() {
		}

		IdentityItem(String name) {
			this.name = name;
		}

	}

	@Entity
	@Table(name = "seq_item")
	static class SeqItem {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_item_seq")
		@SequenceGenerator(name = "seq_item_seq", sequenceName = "seq_item_seq", allocationSize = 50)
		Long id;

		String name;

		protected SeqItem() {
		}

		SeqItem(String name) {
			this.name = name;
		}

	}

}
