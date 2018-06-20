package com.sharshar.currencybalancer.algorithms;

import com.binance.api.client.domain.account.Order;
import com.sharshar.currencybalancer.beans.HoldingRatio;
import com.sharshar.currencybalancer.beans.OwnedAsset;
import com.sharshar.currencybalancer.binance.BinanceAccountServices;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Used to test the code that derives the new balances to keep based on the desired ratios
 *
 * Created by lsharshar on 6/20/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class CurrencyBalancerTest {
	@Autowired
	private CurrencyBalancer balancer;

	@Autowired
	private BinanceAccountServices accountServices;

	@Test
	public void testBalancer() throws Exception {

		// Make sure everything loads correctly
		List<HoldingRatio> ratios = balancer.getDesiredHoldingRatios();
		assertNotNull(ratios);
		assertFalse(ratios.isEmpty());
		System.out.println(ratios);

		List<OwnedAsset> ownedAssets = balancer.getOwnedAssets();
		assertNotNull(ownedAssets);
		assertFalse(ownedAssets.isEmpty());
		System.out.println(ownedAssets);

		double totalValue = balancer.getTotalValue("USD");
		assertTrue(totalValue > 0);
		System.out.println("Total Value: " + totalValue);

		Map<String, Double> adjustments = balancer.getAdjustments();
		for (String val : adjustments.keySet()) {
			System.out.println("Adjustment: " + val + " = " + adjustments.get(val));
		}
		assertNotNull(adjustments);
		assertEquals(adjustments.size(), ratios.size());
		System.out.println("Drift: " + String.format("%.4f", (balancer.getDriftPercent() * 100)));
		assertTrue(balancer.ifMaxDriftExceededOnAnyCurrency(0.2));

		// Let's do the adjustments, then determine if the ratios are met.
		for (OwnedAsset asset : ownedAssets) {
			String assetName = asset.getAsset();
			double adjustment = adjustments.get(assetName);
			asset.setFree(asset.getFree() + adjustment);
		}
		ownedAssets = balancer.getOwnedAssets();
		System.out.println(ownedAssets);

		// Get the total value again since it reloads to current prices, after a slight
		// pause to make sure data has changed
		Thread.sleep(13000L);
		totalValue = balancer.getTotalValue("USD");
		assertTrue(totalValue > 0);
		System.out.println("Total Value: " + totalValue);
		System.out.println("\nAfter rebalancing:");
		adjustments = balancer.getAdjustments();
		for (String val : adjustments.keySet()) {
			System.out.println("Adjustment: " + val + " = " + adjustments.get(val));
		}
		System.out.println("Drift: " + String.format("%.6f", (balancer.getDriftPercent() * 100)));
		assertFalse(balancer.ifMaxDriftExceededOnAnyCurrency(0.2));
	}

	@Test
	public void testBuying() {
		try {
			accountServices.buyMarketTest("AAAAAA", 100);
			fail();
		} catch (Exception ex) {
			System.out.println("Good, exception thrown - " + ex.getMessage());
		}
		try {
			accountServices.buyMarketTest("BNBBTC", 100);
		} catch (Exception ex) {
			fail("Not goot, exception thrown - " + ex.getMessage());
		}
	}

	@Test
	public void testGetAllMyOrders() {
		List<Order> orders = accountServices.getAllMyOrders("NEOBTC");
		assertNotNull(orders);
		assertTrue(orders.size() > 0);
	}
}
