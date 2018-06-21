package com.sharshar.currencybalancer.repository;

import com.sharshar.currencybalancer.beans.PriceData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Used to load historical data on tickers from the database
 *
 * Created by lsharshar on 5/14/2018.
 */
@Transactional
public interface PriceDataSQLRepository extends CrudRepository<PriceData, Long> {
	public List<PriceData> findByTickerAndUpdateTimeGreaterThanAndUpdateTimeLessThan(String ticker, Date d1, Date d2);
	public List<PriceData> findByUpdateTimeGreaterThanAndUpdateTimeLessThan(Date d1, Date d2);
	public List<PriceData> findTop500ByTicker(String ticker);
}
