package com.sharshar.currencybalancer.services;

import com.binance.api.client.domain.account.Order;
import com.sharshar.currencybalancer.beans.HoldingRatio;
import com.sharshar.currencybalancer.beans.OrderHistory;
import com.sharshar.currencybalancer.beans.OwnedAsset;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Test the loading and saving of the desired ratios
 *
 * Created by lsharshar on 6/20/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class BalancerServicesTest {
	private List<HoldingRatio> existingRatios;
	private List<HoldingRatio> newRatios;

	@Autowired
	private BalancerServices services;

	@Before
	public void loadRatios() {
		existingRatios = new ArrayList<>();
		existingRatios.add(new HoldingRatio().setTicker("ETH").setPercent(.25).setTableId(1));
		existingRatios.add(new HoldingRatio().setTicker("BTC").setPercent(.50).setTableId(2));
		existingRatios.add(new HoldingRatio().setTicker("BAT").setPercent(.05).setTableId(3));
		existingRatios.add(new HoldingRatio().setTicker("NEO").setPercent(.20).setTableId(4));

		newRatios = new ArrayList<>();
		newRatios.add(new HoldingRatio().setTicker("ETH").setPercent(2.0));
		newRatios.add(new HoldingRatio().setTicker("BTC").setPercent(5.0));
		newRatios.add(new HoldingRatio().setTicker("NEO").setPercent(2.0));
		newRatios.add(new HoldingRatio().setTicker("ABC").setPercent(1.0));
	}

	@Test
	public void testGetDesiredRatios() {
		List<HoldingRatio> ratios = services.getDesiredRatios();
		assertNotNull(ratios);
		assertTrue(ratios.size() > 0);
	}

	@Test
	public void testGetOwnedAssets() {
		List<OwnedAsset> ownedAssets = services.getOwnedAssets();
		assertNotNull(ownedAssets);
		assertTrue(ownedAssets.size() > 0);
	}

	@Test
	public void testLoadIds() {
		List<HoldingRatio> withIds = services.loadIds(newRatios, existingRatios);
		assertEquals(newRatios.stream().filter(c -> c.getTicker().equalsIgnoreCase("ETH")).findFirst().get().getTableId(), 1L);
		assertEquals(newRatios.stream().filter(c -> c.getTicker().equalsIgnoreCase("BTC")).findFirst().get().getTableId(), 2L);
		assertEquals(newRatios.stream().filter(c -> c.getTicker().equalsIgnoreCase("NEO")).findFirst().get().getTableId(), 4L);
		assertEquals(newRatios.stream().filter(c -> c.getTicker().equalsIgnoreCase("ABC")).findFirst().get().getTableId(), 0L);
	}

	@Test
	public void testCorrectRatios() {
		List<HoldingRatio> correctedRatios = services.correctRatios(newRatios);
		assertEquals(newRatios.stream().filter(c -> c.getTicker().equalsIgnoreCase("ETH")).findFirst().get().getPercent(), 0.2, 0.00001);
		assertEquals(newRatios.stream().filter(c -> c.getTicker().equalsIgnoreCase("BTC")).findFirst().get().getPercent(), 0.5, 0.00001);
		assertEquals(newRatios.stream().filter(c -> c.getTicker().equalsIgnoreCase("NEO")).findFirst().get().getPercent(), 0.2, 0.00001);
		assertEquals(newRatios.stream().filter(c -> c.getTicker().equalsIgnoreCase("ABC")).findFirst().get().getPercent(), 0.1, 0.00001);
	}

	@Test
	public void testFindItemsToDelete() {
		List<HoldingRatio> toDelete = services.findItemsToDelete(newRatios, existingRatios);
		assertNotNull(toDelete);
		assertEquals(1, toDelete.size());
		assertEquals("BAT", toDelete.get(0).getTicker());
	}

	@Test
	@Ignore // We tested once, no reason bo blow away data and since it's the most obvious from the REST interface, ignore it.
	public void testSaveNewRatios() {
		List<HoldingRatio> savedRatios = services.saveNewRatios(newRatios);
	}

	@Test
	public void testGetOrderStatus() throws Exception {
		List<OrderHistory> histories = services.getNewOrders();
		List<Order> statuses = services.getOrderStatuses(histories);
		assertEquals(histories.size(), statuses.size());
		services.updateNewOrdersWithStatus();
		histories = services.getNewOrders();
		assertEquals(0, histories.size());
	}
}
