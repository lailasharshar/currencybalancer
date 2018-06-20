package com.sharshar.currencybalancer.services;

import com.sharshar.currencybalancer.beans.HoldingRatio;
import com.sharshar.currencybalancer.beans.OwnedAsset;
import com.sharshar.currencybalancer.beans.PriceData;
import com.sharshar.currencybalancer.binance.BinanceAccountServices;
import com.sharshar.currencybalancer.repository.PriceDataHoldingRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Services to support balancing of currencies
 *
 * Created by lsharshar on 6/20/2018.
 */
@Service
public class BalancerServices {
	Logger logger = LogManager.getLogger();

	@Autowired
	private PriceDataHoldingRepository repository;

	@Autowired
	private BinanceAccountServices accountServices;

	public List<HoldingRatio> getDesiredRatios() {
		Iterable<HoldingRatio> ih = repository.findAll();
		List<HoldingRatio> ratios = new ArrayList<>();
		ih.forEach(ratios::add);
		return ratios;
	}

	public List<OwnedAsset> getOwnedAssets() {
		return accountServices.getBalancesWithValues();
	}

	public List<HoldingRatio> saveNewRatios(List<HoldingRatio> newDesiredRatios) {
		// Correct them so the percent adds up to 100%
		List<HoldingRatio> correctedRatios = correctRatios(newDesiredRatios);
		// Retrieve the existing ratios
		List<HoldingRatio> existingRatios = getDesiredRatios();
		// Update the correctedRatios with db ids if they have them
		correctedRatios = loadIds(correctedRatios, existingRatios);
		// Determine which items are not in the new list
		List<HoldingRatio> itemsToDelete = findItemsToDelete(newDesiredRatios, existingRatios);
		// Delete the ones to delete
		repository.deleteAll(itemsToDelete);
		// Add/update the database with the new list
		repository.saveAll(correctedRatios);
		// Retrieve the data from the database and pass it back to verify it's correct
		return getDesiredRatios();
	}

	public List<HoldingRatio> loadIds(List<HoldingRatio> newValues, List<HoldingRatio> existingValues) {
		for (HoldingRatio ratio : newValues) {
			HoldingRatio found = existingValues.stream()
					.filter(c -> c.getTicker().equalsIgnoreCase(ratio.getTicker())).findFirst().orElse(null);
			if (found != null) {
				ratio.setTableId(found.getTableId());
			}
		}
		return newValues;
	}

	public List<HoldingRatio> correctRatios(List<HoldingRatio> newDesiredRatios) {
		List<HoldingRatio> correctedRatios = new ArrayList<>();
		double total = newDesiredRatios.stream().mapToDouble(HoldingRatio::getPercent).sum();
		if (Math.abs(1.0 - total) > 0.00001) {
			// Doesn't total 100%, correct
			for (HoldingRatio ratio : newDesiredRatios) {
				ratio.setPercent(ratio.getPercent()/total);
				correctedRatios.add(ratio);
			}
		}
		return correctedRatios;
	}

	public List<HoldingRatio> findItemsToDelete(List<HoldingRatio> newValues, List<HoldingRatio> oldValues) {
		// First find ones to remove
		List<HoldingRatio> itemsToDelete = new ArrayList<>();
		for (HoldingRatio ratio : oldValues) {
			HoldingRatio found = newValues.stream()
					.filter(c -> c.getTicker().equalsIgnoreCase(ratio.getTicker())).findFirst().orElse(null);
			if (found == null) {
				itemsToDelete.add(ratio);
			}
		}
		return itemsToDelete;
	}
}
