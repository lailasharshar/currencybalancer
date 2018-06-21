package com.sharshar.currencybalancer.controllers;

import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.sharshar.currencybalancer.algorithms.CurrencyBalancer;
import com.sharshar.currencybalancer.beans.OwnedAsset;
import com.sharshar.currencybalancer.services.BalancerServices;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to inspect the assets, drift and open orders
 *
 * Created by lsharshar on 6/20/2018.
 */
@RestController
public class BalancerController {
	private Logger logger = LogManager.getLogger();

	@Autowired
	private BalancerServices balancerServices;

	@Autowired
	private CurrencyBalancer balancer;

	@GetMapping("/assets")
	public List<OwnedAsset> getOwnedAssets() {
		return balancerServices.getOwnedAssets();
	}

	@GetMapping("/open")
	public List<Order> getOpenOrders() {
		try {
			balancerServices.updateNewOrdersWithStatus();
			return balancerServices.getOrderStatuses(balancerServices.getNewOrders());
		} catch (Exception ex) {
			logger.error("Cannot find open orders", ex);
			return null;
		}
	}

	@GetMapping("/drift")
	public double getDrift() {
		return balancer.getDriftPercent();
	}

	@GetMapping("/alldrifts")
	public List<CurrencyBalancer.CurrencyDrift> getAllDrift() {
		return balancer.getDrifts();
	}

	@PostMapping("/balance")
	public List<NewOrderResponse> balance() {
		return new ArrayList<NewOrderResponse>();
		// Too scared to put this in yet
		//return balancer.balance();
	}
}
