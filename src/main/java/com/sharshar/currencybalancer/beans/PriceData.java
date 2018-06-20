package com.sharshar.currencybalancer.beans;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.SecureRandom;
import java.util.Date;
import java.util.Random;

/**
 * Bean for price data
 *
 * Created by lsharshar on 3/6/2018.
 */
public class PriceData {

	private Logger logger = LogManager.getLogger();

	private String ticker;

	private Double price;

	//@Field(type = FieldType.Date, format = DateFormat.custom, pattern = "EEE MMM dd HH:mm:ss zzz yyyy")
	private Date updateTime;

	private short exchange;

	public PriceData() {
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
