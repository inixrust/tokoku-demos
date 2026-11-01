package com.tokoku.demos.jpa.orders;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * D6.1 (slide 45) dan D15.1 (slide 128-129).
 * <pre>
 * D6.1  java -jar demo-jpa.jar --spring.profiles.active=osiv   ->  /d61/orders/1 OK, tetapi SQL items jalan di luar transaction
 *       java -jar demo-jpa.jar                                 ->  /d61/orders/1 500 LazyInitializationException
 *                                                                  /d61/orders/1/fixed OK (@EntityGraph)
 * D15.1 (perilaku Hibernate 7.4.13, berbeda dari slide yang berdasar Hibernate 6):
 *       /d151/join-fetch?page=1&amp;size=5                   ->  OK: Hibernate menulis ulang menjadi subquery dengan paging di SQL
 *                                                                  (tanpa ORDER BY: isi halaman tidak deterministik; inner join
 *                                                                  membuang order tanpa item). Tidak ada paging di memori.
 *       ...&amp;variant=left                                       ->  idem untuk left join fetch + distinct + order by
 *       ...&amp;variant=filtered                                   ->  SQL ERROR "missing FROM-clause entry" (filter pada collection
 *                                                                  yang di-fetch tidak bisa dipindah ke subquery)
 *       Pelajaran slide 128 tetap: fetch plan per use case, pola dua tahap (ID page + fetch) yang dapat diprediksi.
 * </pre>
 * Controller sengaja memakai repository dan entity langsung (tanpa transaction di service) agar efek OSIV terlihat.
 */
@RestController
class OrderDemoController {

	private final PurchaseOrderRepository orders;

	OrderDemoController(PurchaseOrderRepository orders) {
		this.orders = orders;
	}

	@GetMapping("/d61/orders/{id}")
	Map<String, Object> lazy(@PathVariable long id) {
		PurchaseOrder order = orders.findById(id).orElseThrow();
		return view(order); // akses lines di luar transaction repository
	}

	@GetMapping("/d61/orders/{id}/fixed")
	Map<String, Object> fixed(@PathVariable long id) {
		return view(orders.findWithLinesById(id).orElseThrow());
	}

	@GetMapping("/d151/join-fetch")
	Map<String, Object> joinFetch(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size,
			@RequestParam(defaultValue = "plain") String variant) {
		PageRequest request = PageRequest.of(page, size);
		Page<PurchaseOrder> result = switch (variant) {
			case "left" -> orders.findAllWithLinesLeftJoin(request);
			case "filtered" -> orders.findAllWithFilteredLines(request);
			default -> orders.findAllWithLines(request);
		};
		return Map.of("orders", result.map(OrderDemoController::view).getContent(), "total", result.getTotalElements());
	}

	private static Map<String, Object> view(PurchaseOrder order) {
		List<String> lines = order.getLines().stream().map(line -> line.getSku() + " x" + line.getQuantity()).toList();
		return Map.of("id", order.getId(), "customer", order.getCustomer(), "lines", lines);
	}

	@ExceptionHandler(RuntimeException.class)
	ProblemDetail error(RuntimeException ex) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
		pd.setTitle(ex.getClass().getSimpleName());
		return pd;
	}

}
