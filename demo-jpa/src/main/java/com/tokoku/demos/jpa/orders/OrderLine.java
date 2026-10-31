package com.tokoku.demos.jpa.orders;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class OrderLine {

	@Id
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id")
	private PurchaseOrder order;

	private String sku;

	private int quantity;

	protected OrderLine() {
	}

	public String getSku() {
		return sku;
	}

	public int getQuantity() {
		return quantity;
	}

}
