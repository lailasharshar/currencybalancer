package com.sharshar.currencybalancer.beans;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.persistence.*;

/**
 * Holds the desired ratio of currencies
 *
 * Created by lsharshar on 5/27/2018.
 */
@Entity
@Table(name="holding_ratio")
public class HoldingRatio {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long tableId;
	private String ticker;
	private double percent;
	private short fraction;

	public long getTableId() {
		return tableId;
	}

	public HoldingRatio setTableId(long tableId) {
		this.tableId = tableId;
		return this;
	}

	public String getTicker() {
		return ticker;
	}

	public HoldingRatio setTicker(String ticker) {
		this.ticker = ticker;
		return this;
	}

	public double getPercent() {
		return percent;
	}

	public HoldingRatio setPercent(double percent) {
		this.percent = percent;
		return this;
	}

	public boolean canDoFraction() {
		return this.fraction > 0;
	}

	public HoldingRatio setFraction(boolean canDoFractional) {
		this.fraction = (short) (canDoFractional ? 1 : 0);
		return this;
	}

	@Override
	public String toString() {
		return (new ToStringBuilder(this,
				ToStringStyle.SHORT_PREFIX_STYLE)
				.append("Id: ").append(getTableId())
				.append(" Ticker: ").append(getTicker())
				.append(" Percent: ").append(getPercent())).toString();
	}
}
