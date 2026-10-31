package com.tokoku.demos.jpa.pool;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * D14.1 (slide 122): virtual thread memindahkan bottleneck ke connection pool.
 * <pre>
 * java -jar demo-jpa.jar --spring.profiles.active=vt
 * .\loadtest\wrk2\run.ps1 -Rate 300 -Duration 30s -Connections 100 -Url http://host.docker.internal:8095/d141/query
 * curl.exe -s localhost:8095/actuator/metrics/hikaricp.connections.pending
 * </pre>
 * Query 100 ms, pool 10: kapasitas ±100 rps. Pada 300 rps, pending naik walau virtual thread "tak terbatas".
 * Menaikkan thread Tomcat (--server.tomcat.threads.max=400, tanpa profil vt) juga tidak membantu: batasnya pool.
 */
@RestController
class PoolBottleneckController {

	private final JdbcClient jdbc;

	PoolBottleneckController(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@GetMapping("/d141/query")
	String query() {
		jdbc.sql("select pg_sleep(0.1)").query().singleValue();
		return "ok";
	}

}
