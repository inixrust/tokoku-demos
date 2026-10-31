package com.tokoku.demos.jpa.outbox;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * D7.1 (slide 52) dan D7.2 (slide 54). Lihat scripts/d7.1-dual-write.ps1 dan scripts/d7.2-skip-locked.ps1.
 * <ul>
 * <li>POST /d71/dual?crash=true: order di-commit, lalu proses "crash" sebelum publish ke Kafka. Event hilang.</li>
 * <li>POST /d71/outbox?crash=true: order + baris outbox di-commit bersama, lalu crash. Setelah restart dengan relay
 * aktif, event tetap terkirim.</li>
 * <li>POST /d72/burst?n=500 + dua instance relay: setiap instance mengambil batch berbeda (FOR UPDATE SKIP LOCKED).</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
class OutboxDemo {

	static final String TOPIC = "demo-orders";

	@Bean
	NewTopic demoOrdersTopic() {
		return TopicBuilder.name(TOPIC).partitions(3).replicas(1).build();
	}

	@RestController
	static class Endpoints {

		private final JdbcClient jdbc;

		private final TransactionTemplate transaction;

		private final KafkaTemplate<String, String> kafka;

		private final KafkaProperties kafkaProperties;

		Endpoints(JdbcClient jdbc, TransactionTemplate transaction, KafkaTemplate<String, String> kafka,
				KafkaProperties kafkaProperties) {
			this.jdbc = jdbc;
			this.transaction = transaction;
			this.kafka = kafka;
			this.kafkaProperties = kafkaProperties;
		}

		@PostMapping("/d71/dual")
		Map<String, Object> dualWrite(@RequestParam(defaultValue = "false") boolean crash) throws Exception {
			long id = transaction.execute(s -> insertOrder("dual write"));
			// Commit DB sudah terjadi. Publish ke Kafka adalah write KEDUA yang terpisah.
			if (crash) {
				crashNow("setelah commit DB, sebelum publish (order " + id + ")");
			}
			kafka.send(TOPIC, String.valueOf(id), "{\"orderId\":" + id + "}").get(5, TimeUnit.SECONDS);
			return Map.of("orderId", id, "published", true);
		}

		@PostMapping("/d71/outbox")
		Map<String, Object> outbox(@RequestParam(defaultValue = "false") boolean crash) {
			long id = transaction.execute(s -> {
				long orderId = insertOrder("outbox");
				jdbc.sql("insert into outbox (aggregate_id, payload) values (?, ?)")
					.params(String.valueOf(orderId), "{\"orderId\":" + orderId + "}")
					.update();
				return orderId;
			});
			if (crash) {
				crashNow("setelah commit order + outbox (order " + id + ")");
			}
			return Map.of("orderId", id, "outbox", "menunggu relay");
		}

		@PostMapping("/d72/burst")
		Map<String, Object> burst(@RequestParam(defaultValue = "500") int n) {
			jdbc.sql("insert into outbox (aggregate_id, payload) select 'burst-' || g, '{}' from generate_series(1, ?) g")
				.param(n)
				.update();
			return Map.of("inserted", n);
		}

		/** Ringkasan: order di DB vs event di Kafka (dibaca ulang dari awal topic). */
		@GetMapping("/d71/check")
		Map<String, Object> check() {
			List<Long> orders = jdbc.sql("select id from shop_order order by id").query(Long.class).list();
			Map<String, Integer> eventsPerKey = readAllKeys();
			List<Long> missing = orders.stream().filter(id -> !eventsPerKey.containsKey(String.valueOf(id))).toList();
			long duplicates = eventsPerKey.values().stream().filter(count -> count > 1).count();
			return Map.of("orders", orders.size(), "events", eventsPerKey.values().stream().mapToInt(i -> i).sum(),
					"ordersTanpaEvent", missing, "keyDuplikat", duplicates);
		}

		@GetMapping("/d72/stats")
		Map<String, Object> stats() {
			List<Map<String, Object>> perInstance = jdbc
				.sql("select coalesce(published_by, '-') as instance, count(*) as rows from outbox group by 1 order by 1")
				.query()
				.listOfRows();
			Map<String, Integer> keys = readAllKeys();
			long burstEvents = keys.entrySet().stream().filter(e -> e.getKey().startsWith("burst-")).mapToInt(Map.Entry::getValue).sum();
			long burstDuplicates = keys.entrySet().stream().filter(e -> e.getKey().startsWith("burst-") && e.getValue() > 1).count();
			return Map.of("outboxPerInstance", perInstance, "burstEvents", burstEvents, "burstKeyDuplikat", burstDuplicates);
		}

		private long insertOrder(String note) {
			return jdbc.sql("insert into shop_order (note) values (?) returning id").param(note).query(Long.class).single();
		}

		private Map<String, Integer> readAllKeys() {
			Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties());
			config.put(ConsumerConfig.GROUP_ID_CONFIG, "check-" + UUID.randomUUID());
			config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
			Map<String, Integer> counts = new HashMap<>();
			try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(config, new StringDeserializer(),
					new StringDeserializer())) {
				consumer.subscribe(List.of(TOPIC));
				int emptyPolls = 0;
				while (emptyPolls < 3) {
					var records = consumer.poll(Duration.ofSeconds(1));
					emptyPolls = records.isEmpty() ? emptyPolls + 1 : 0;
					for (ConsumerRecord<String, String> record : records) {
						counts.merge(record.key(), 1, Integer::sum);
					}
				}
			}
			return counts;
		}

		private static void crashNow(String where) {
			LoggerFactory.getLogger(OutboxDemo.class).error("SIMULASI CRASH {}", where);
			Runtime.getRuntime().halt(1); // tanpa shutdown hook: seperti proses dibunuh
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RelayConfig {

		@Bean
		@ConditionalOnProperty(name = "demo.relay-enabled", havingValue = "true")
		Relay relay(JdbcClient jdbc, KafkaTemplate<String, String> kafka, @Value("${demo.instance}") String instance) {
			return new Relay(jdbc, kafka, instance);
		}

	}

	/** Relay outbox; SKIP LOCKED: setiap instance mengambil batch berbeda tanpa saling menunggu. */
	static class Relay {

		private static final Logger log = LoggerFactory.getLogger(Relay.class);

		private final JdbcClient jdbc;

		private final KafkaTemplate<String, String> kafka;

		private final String instance;

		Relay(JdbcClient jdbc, KafkaTemplate<String, String> kafka, String instance) {
			this.jdbc = jdbc;
			this.kafka = kafka;
			this.instance = instance;
		}

		@Scheduled(fixedDelay = 200)
		@Transactional
		public void relay() throws Exception {
			List<Map<String, Object>> batch = jdbc.sql("""
					select id, aggregate_id, payload from outbox where published_at is null
					order by id limit 20 for update skip locked""").query().listOfRows();
			for (Map<String, Object> row : batch) {
				kafka.send(TOPIC, (String) row.get("aggregate_id"), (String) row.get("payload")).get(5, TimeUnit.SECONDS);
				jdbc.sql("update outbox set published_at = now(), published_by = ? where id = ?")
					.params(instance, row.get("id"))
					.update();
			}
			if (!batch.isEmpty()) {
				log.info("instance {} mengirim outbox id {}..{} ({} baris)", instance, batch.getFirst().get("id"),
						batch.getLast().get("id"), batch.size());
			}
		}

	}

}
