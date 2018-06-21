package com.sharshar.currencybalancer.beans;

import javax.persistence.*;
import java.util.Date;

/**
 * Bean for price data
 *
 * Created by lsharshar on 3/6/2018.
 */
@Entity
@Table(name="pricedata")
public class PriceData {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long tableId;
	private Double price;
	private Date updateTime;
	private String ticker;
	private short exchange;

	public PriceData() {
	}

	public Long getTableId() {
		return tableId;
	}

	public PriceData setTableId(Long tableId) {
		this.tableId = tableId;
		return this;
	}

	public Double getPrice() {
		return price;
	}

	public PriceData setPrice(Double price) {
		this.price = price;
		return this;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public PriceData setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
		return this;
	}

	public String getTicker() {
		return ticker;
	}

	public PriceData setTicker(String ticker) {
		this.ticker = ticker;
		return this;
	}

	public short getExchange() {
		return exchange;
	}

	public PriceData setExchange(short exchange) {
		this.exchange = exchange;
		return this;
	}
}
