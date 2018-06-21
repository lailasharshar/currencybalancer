package com.sharshar.currencybalancer.repository;

import com.sharshar.currencybalancer.beans.OrderHistory;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Used to hold order history information
 *
 * Created by lsharshar on 6/20/2018.
 */
public interface OrderHistoryRepository extends CrudRepository<OrderHistory, Long> {
	List<OrderHistory> findByStatus(String status);
}
