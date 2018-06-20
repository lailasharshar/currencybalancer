package com.sharshar.currencybalancer.services;

import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.sharshar.currencybalancer.binance.BinanceAccountServices;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * I need to test buying and selling, but this involves real money, so keep this tests
 * separate and set them to @Ignore when you're done.
 *
 * Created by lsharshar on 6/20/2018.
 */

@RunWith(SpringRunner.class)
@SpringBootTest
public class DangerousTests {

	@Autowired
	BinanceAccountServices binanceAccountServices;

	@Test
	@Ignore
	public void testBuy() throws Exception {
		String ticker = "BNBBTC";
		NewOrderResponse response = binanceAccountServices.createMarketOrder(ticker, 20, OrderSide.BUY);
		Order order = binanceAccountServices.checkOrderStatus(ticker, response.getClientOrderId());
		System.out.println(order);
		Thread.sleep(3000L);
		order = binanceAccountServices.checkOrderStatus(ticker, response.getClientOrderId());
		System.out.println(order);
	}

	@Test
	@Ignore
	public void testSell() throws Exception {
		String ticker = "BNBBTC";
		NewOrderResponse response = binanceAccountServices
				.createMarketOrder(ticker, 10, OrderSide.SELL);
		Order order = binanceAccountServices.checkOrderStatus(ticker, response.getClientOrderId());
		System.out.println(order);
		Thread.sleep(3000L);
		order = binanceAccountServices.checkOrderStatus(ticker, response.getClientOrderId());
		System.out.println(order);
	}
}
