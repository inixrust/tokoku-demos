package com.tokoku.demos.jpa.orders;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

	/** D6.1 perbaikan: fetch plan eksplisit untuk use case ini. */
	@EntityGraph(attributePaths = "lines")
	Optional<PurchaseOrder> findWithLinesById(Long id);

	/** D15.1: JOIN FETCH collection + Pageable (lihat OrderDemoController untuk perilaku Hibernate 7). */
	@Query(value = "select o from PurchaseOrder o join fetch o.lines", countQuery = "select count(o) from PurchaseOrder o")
	Page<PurchaseOrder> findAllWithLines(Pageable pageable);

	@Query(value = "select distinct o from PurchaseOrder o left join fetch o.lines order by o.id",
			countQuery = "select count(o) from PurchaseOrder o")
	Page<PurchaseOrder> findAllWithLinesLeftJoin(Pageable pageable);

	@Query(value = "select o from PurchaseOrder o join fetch o.lines l where l.quantity >= 2",
			countQuery = "select count(o) from PurchaseOrder o")
	Page<PurchaseOrder> findAllWithFilteredLines(Pageable pageable);

}
