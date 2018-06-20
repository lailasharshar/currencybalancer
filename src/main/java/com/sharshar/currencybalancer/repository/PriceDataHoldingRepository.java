package com.sharshar.currencybalancer.repository;

import com.sharshar.currencybalancer.beans.HoldingRatio;
import org.springframework.data.repository.CrudRepository;


/**
 * Used to load the holding ratio information from the db
 *
 * Created by lsharshar on 5/14/2018.
 */
public interface PriceDataHoldingRepository extends CrudRepository<HoldingRatio, Long> {
}
