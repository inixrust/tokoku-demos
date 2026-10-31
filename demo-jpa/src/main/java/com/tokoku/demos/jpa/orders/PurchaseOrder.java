package com.tokoku.demos.jpa.orders;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;

@Entity
public class PurchaseOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "purchase_order_seq")
	@SequenceGenerator(name = "purchase_order_seq", sequenceName = "purchase_order_seq", allocationSize = 50)
	private Long id;

	private String customer;

	private Instant createdAt;

	@OneToMany(mappedBy = "order")
	private List<OrderLine> lines = new ArrayList<>();

	protected PurchaseOrder() {
	}

	public Long getId() {
		return id;
	}

	public String getCustomer() {
		return customer;
	}

	public List<OrderLine> getLines() {
		return lines;
	}

}
