package com.sharshar.currencybalancer.algorithms;

import com.sharshar.currencybalancer.beans.HoldingRatio;
import com.sharshar.currencybalancer.beans.OwnedAsset;
import com.sharshar.currencybalancer.beans.PriceData;
import com.sharshar.currencybalancer.binance.BinanceAccountServices;
import com.sharshar.currencybalancer.repository.PriceDataHoldingRepository;
import com.sharshar.currencybalancer.services.BalancerServices;
import com.sharshar.currencybalancer.utils.ScratchException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Used to re-balance holdings based on values defined in the database
 *
 * Created by lsharshar on 5/19/2018.
 */
@Service
public class CurrencyBalancer {
	Logger logger = LogManager.getLogger();

	@Autowired
	BalancerServices services;

	@Autowired
	BinanceAccountServices accountServices;

	// The desired ratios
	private List<HoldingRatio> desiredHoldingRatios;

	// The currently owned assets
	private List<OwnedAsset> ownedAssets;

	// The current price of all the tickers on Binance
	private List<PriceData> currentPriceData;

	public CurrencyBalancer() { }

	public List<HoldingRatio> getDesiredHoldingRatios() {
		return desiredHoldingRatios;
	}

	public List<OwnedAsset> getOwnedAssets() {
		return ownedAssets;
	}

	@PostConstruct
	public void load() throws ScratchException {
		desiredHoldingRatios = services.getDesiredRatios();
		if (desiredHoldingRatios == null) {
			throw new ScratchException("Cannot load holding ratios");
		}
		if (desiredHoldingRatios.isEmpty()) {
			throw new ScratchException("Now holding ratios found");
		}
		double total = desiredHoldingRatios.stream().mapToDouble(HoldingRatio::getPercent).sum();
		if (Math.abs(1.0 - total) > 0.0001) {
			throw new ScratchException("Holdings should add up to 1");
		}
		ownedAssets = services.getOwnedAssets();
		if (ownedAssets == null || ownedAssets.isEmpty()) {
			throw new ScratchException("There appears to be no owned assets");
		}
		currentPriceData = accountServices.getData();
	}

	/**
	 * Adjustments are in the asset. For example, if you have 100 NEO and you want to sell half, this will return
	 * -50
	 *
	 * @param ownedAsset - The owned asset
	 * @param desiredRatio - The desired ratio you want to have this asset at
	 * @param currentPrice - The current price of the asset (in BTC)
	 * @param totalAmount - The total amount in BTC of owned assets (to determine current ratio)
	 * @return the amount to sell (negative) or buy (positive)
	 */
	public double getAdjustment(OwnedAsset ownedAsset, double desiredRatio, double currentPrice, double totalAmount) {
		double totalOwnedValue = (ownedAsset.getFree() + ownedAsset.getLocked()) * currentPrice;
		double totalDesiredValue = desiredRatio * totalAmount;
		double totalAdjustedValue = totalDesiredValue - totalOwnedValue;
		return totalAdjustedValue/currentPrice;
	}

	/**
	 * Retrieve the total value of all assets owned. If the base currency is anything other than bitcoin, get the
	 * value in bitcoin first, then convert to the desired base currency. If you want USD, since most exchanges
	 * don't deal with real dollars, use tether instead.
	 *
	 * @param baseCurrency - The base currency to return the value of.
	 * @return the amount in the base currency
	 */
	public double getTotalValue(String baseCurrency) {
		if (!baseCurrency.equalsIgnoreCase("BTC")) {
			// First convert to BTC, then convert back
			double totalValueInBTC = getTotalValue("BTC");
			// find the conversion from that currency from bitcoin
			if (baseCurrency == "USD") {
				baseCurrency = "USDT";
			}
			final String correctedBaseCurrency = baseCurrency;
			PriceData pd = currentPriceData.stream()
					.filter(c -> c.getTicker().equalsIgnoreCase("BTC" + correctedBaseCurrency))
					.findFirst().orElse(null);
			if (pd == null) {
				logger.error("Cannot find mapping for " + baseCurrency);
				return 0.0;
			}
			return totalValueInBTC * pd.getPrice();
		}

		// reload price data, just to be current
		this.currentPriceData = accountServices.getData();

		double totalValue = 0;
		for (OwnedAsset asset : ownedAssets) {
			totalValue += getValueOwned(asset, baseCurrency);
		}
		return totalValue;
	}

	/**
	 * Get the value owned in the base currency of the asset. Unlike totalValue, We assume the asset/base currency
	 * pair exist
	 *
	 * @param ownedAsset - the owned asset
	 * @param baseCurrency - the base currency to use to compare
	 * @return the total value of the amount owned in the base currency
	 */
	public double getValueOwned(OwnedAsset ownedAsset, String baseCurrency) {
		// If this is the base currency, just return the number owned
		if (ownedAsset.getAsset().equalsIgnoreCase(baseCurrency)) {
			double value = ownedAsset.getFree() + ownedAsset.getLocked();
			logger.info(ownedAsset.getAsset() + baseCurrency + " - qty: " + ownedAsset.getFree() + "/"
					+ ownedAsset.getLocked() + " @ 1.0 = " + value);
			return value;
		}
		PriceData data = this.currentPriceData.stream()
				.filter(c -> c.getTicker().equalsIgnoreCase(ownedAsset.getAsset() + baseCurrency))
				.findFirst().orElse(null);
		if (data == null) {
			logger.error("Can't find price for: " + ownedAsset.getAsset() + " in " + baseCurrency);
			return 0.0;
		}
		double value = (ownedAsset.getFree() + ownedAsset.getLocked()) * data.getPrice();
		logger.info(ownedAsset.getAsset() + baseCurrency + " - qty: " + ownedAsset.getFree() + "/"
				+ ownedAsset.getLocked() + " @ " + data.getPrice() + " = " + value);
		return value;
	}

	/**
	 * Return a map of the adjustments to make in the owned currencies based on the desired holding ratios. The
	 * adjustments will be units of that currency.
	 *
	 * @return the map of adjustments that need to be made to achieve the desired ratio
	 */
	public Map<String, Double> getAdjustments() {
		String baseCurrency = "BTC";
		double totalValueOwned = getTotalValue(baseCurrency);
		Map<String, Double> adjustments = new HashMap<>();
		for (OwnedAsset asset : ownedAssets) {
			double desiredRatio = 0.0;
			double currentPrice = 0.0;
			HoldingRatio ratio = getHoldingRatio(asset.getAsset());
			if (ratio == null) {
				continue;
			}
			desiredRatio = ratio.getPercent();
			PriceData pd = getPriceData(asset.getAsset(), baseCurrency);
			if (pd == null) {
				if (asset.getAsset().equalsIgnoreCase(baseCurrency)) {
					currentPrice = 1.0;
				} else {
					continue;
				}
			} else {
				currentPrice = pd.getPrice();
			}

			double adjustment = getAdjustment(asset, desiredRatio, currentPrice, totalValueOwned);
			if (!ratio.canDoFraction()) {
				adjustment = Math.round(adjustment);
			}
			if (Math.abs(adjustment) > 0.01) {
				adjustments.put(asset.getAsset(), adjustment);
			}

		}
		return adjustments;
	}

	private HoldingRatio getHoldingRatio(String ticker) {
		return desiredHoldingRatios.stream()
				.filter(c -> c.getTicker().equalsIgnoreCase(ticker))
				.findFirst().orElse(null);
	}

	private PriceData getPriceData(String ticker, String baseCurrency) {
		return currentPriceData.stream()
				.filter(c -> c.getTicker().equalsIgnoreCase(ticker + baseCurrency))
				.findFirst().orElse(null);
	}

	private OwnedAsset getOwnedAsset(String ticker) {
		return ownedAssets.stream()
				.filter(c -> c.getAsset().equalsIgnoreCase(ticker))
				.findFirst().orElse(null);
	}

	public double getDriftPercent() {
		double totalPercentDifference = 0;
		Map<String, Double> adjustments = getAdjustments();
		for (OwnedAsset asset : ownedAssets) {
			Double adjustment = adjustments.get(asset.getAsset());
			if (adjustment != null) {
				HoldingRatio ratio = getHoldingRatio(asset.getAsset());
				// Negative and positive shouldn't count against each other
				double thisPercentDifference = Math.abs(adjustment) / (asset.getFree() + asset.getLocked());
				// Weight this relative to all desired holdings
				totalPercentDifference += thisPercentDifference * ratio.getPercent();
			}
		}
		return totalPercentDifference;
	}

	public boolean ifMaxDriftExceededOnAnyCurrency(double driftPercent) {
		Map<String, Double> adjustments = getAdjustments();
		for (OwnedAsset asset : ownedAssets) {
			Double adjustment = adjustments.get(asset.getAsset());
			if (adjustment != null) {
				// Negative and positive shouldn't count against each other
				double thisPercentDifference = Math.abs(adjustment) / (asset.getFree() + asset.getLocked());
				if (thisPercentDifference > driftPercent) {
					return true;
				}
			}
		}
		return false;
	}
}
