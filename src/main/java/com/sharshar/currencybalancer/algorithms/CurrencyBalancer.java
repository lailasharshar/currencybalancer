package com.sharshar.currencybalancer.algorithms;

import com.binance.api.client.domain.account.NewOrderResponse;
import com.sharshar.currencybalancer.beans.HoldingRatio;
import com.sharshar.currencybalancer.beans.OwnedAsset;
import com.sharshar.currencybalancer.beans.PriceData;
import com.sharshar.currencybalancer.binance.BinanceAccountServices;
import com.sharshar.currencybalancer.services.BalancerServices;
import com.sharshar.currencybalancer.utils.ScratchException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Used to re-balance holdings based on values defined in the database
 *
 * Created by lsharshar on 5/19/2018.
 */
@Service
public class CurrencyBalancer {

	private static Logger logger = LogManager.getLogger();

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

	public List<HoldingRatio> getDesiredHoldingRatios() {
		return desiredHoldingRatios;
	}

	public List<OwnedAsset> getOwnedAssets() {
		return ownedAssets;
	}

	public class CurrencyDrift {
		private String ticker;
		private double drift;

		public CurrencyDrift(String ticker, double drift) {
			this.ticker = ticker;
			this.drift = drift;
		}

		public String getTicker() {
			return ticker;
		}

		public double getDrift() {
			return drift;
		}
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
	public static double getAdjustment(OwnedAsset ownedAsset, double desiredRatio, double currentPrice, double totalAmount) {
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
		// reload price data, just to be current
		this.currentPriceData = accountServices.getData();
		return getTotalValue(this.ownedAssets, this.desiredHoldingRatios, this.currentPriceData, baseCurrency);
	}

	public static double getTotalValue(List<OwnedAsset> assets, List<HoldingRatio> ratios, List<PriceData> currentPriceData, String baseCurrency) {
		if (!baseCurrency.equalsIgnoreCase("BTC")) {
			// First convert to BTC, then convert back
			double totalValueInBTC = getTotalValue(assets, ratios, currentPriceData, "BTC");
			if (baseCurrency.equalsIgnoreCase("USD")) {
				baseCurrency = "USDT";
			}
			// find the conversion from that currency from bitcoin
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


		double totalValue = 0;
		// Only do the ones we want to manage
		for (HoldingRatio ratio : ratios) {
			OwnedAsset asset = getOwnedAsset(ratio.getTicker(), assets);
			totalValue += getValueOwned(asset, currentPriceData, baseCurrency);
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
	public static double getValueOwned(OwnedAsset ownedAsset, List<PriceData> priceData, String baseCurrency) {
		// If this is the base currency, just return the number owned
		if (ownedAsset.getAsset().equalsIgnoreCase(baseCurrency)) {
			double value = ownedAsset.getFree() + ownedAsset.getLocked();
			//logger.info(ownedAsset.getAsset() + baseCurrency + " - qty: " + ownedAsset.getFree() + "/"
			//		+ ownedAsset.getLocked() + " @ 1.0 = " + value);
			return value;
		}
		PriceData data = priceData.stream()
				.filter(c -> c.getTicker().equalsIgnoreCase(ownedAsset.getAsset() + baseCurrency))
				.findFirst().orElse(null);
		if (data == null) {
			logger.error("Can't find price for: " + ownedAsset.getAsset() + " in " + baseCurrency);
			return 0.0;
		}
		double value = (ownedAsset.getFree() + ownedAsset.getLocked()) * data.getPrice();
		//logger.info(ownedAsset.getAsset() + baseCurrency + " - qty: " + ownedAsset.getFree() + "/"
		//		+ ownedAsset.getLocked() + " @ " + data.getPrice() + " = " + value);
		return value;
	}

	public Map<String, Double> getAdjustments() {
		return getAdjustments(ownedAssets, desiredHoldingRatios, currentPriceData);
	}

	/**
	 * Return a map of the adjustments to make in the owned currencies based on the desired holding ratios. The
	 * adjustments will be units of that currency.
	 *
	 * @return the map of adjustments that need to be made to achieve the desired ratio
	 */
	public static Map<String, Double> getAdjustments(List<OwnedAsset> ownedAssets, List<HoldingRatio> ratios, List<PriceData> priceData) {
		String baseCurrency = "BTC";
		double totalValueOwned = getTotalValue(ownedAssets, ratios, priceData, baseCurrency);
		Map<String, Double> adjustments = new HashMap<>();
		for (HoldingRatio ratio : ratios) {
			double desiredRatio = 0.0;
			double currentPrice = 0.0;
			OwnedAsset asset = getOwnedAsset(ratio.getTicker(), ownedAssets);
			if (ratio == null) {
				continue;
			}
			desiredRatio = ratio.getPercent();
			PriceData pd = getPriceData(priceData, asset.getAsset(), baseCurrency);
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

	public HoldingRatio getHoldingRatio(String ticker) {
		return desiredHoldingRatios.stream()
				.filter(c -> c.getTicker().equalsIgnoreCase(ticker))
				.findFirst().orElse(null);
	}

	public PriceData getPriceData(String ticker, String baseCurrency) {
		return getPriceData(currentPriceData, ticker, baseCurrency);
	}

	public static PriceData getPriceData(List<PriceData> priceData, String ticker, String baseCurrency) {
		return priceData.stream()
				.filter(c -> c.getTicker().equalsIgnoreCase(ticker + baseCurrency))
				.findFirst().orElse(null);
	}

	public OwnedAsset getOwnedAsset(String ticker) {
		return getOwnedAsset(ticker, ownedAssets);
	}

	public static OwnedAsset getOwnedAsset(String ticker, List<OwnedAsset> assets) {
		return assets.stream()
				.filter(c -> c.getAsset().equalsIgnoreCase(ticker))
				.findFirst().orElse(null);
	}

	public double getDriftPercent() {
		double totalPercentDifference = 0;
		Map<String, Double> adjustments = getAdjustments();
		for (HoldingRatio ratio : desiredHoldingRatios) {
			OwnedAsset asset = getOwnedAsset(ratio.getTicker());
			Double adjustment = adjustments.get(asset.getAsset());
			if (adjustment != null) {
				// Negative and positive shouldn't count against each other
				double thisPercentDifference = Math.abs(adjustment) / (asset.getFree() + asset.getLocked());
				// Weight this relative to all desired holdings
				totalPercentDifference += thisPercentDifference * ratio.getPercent();
			}
		}
		return totalPercentDifference;
	}

	public List<CurrencyDrift> getDrifts() {
		List<CurrencyDrift> drifts = new ArrayList<>();
		Map<String, Double> adjustments = getAdjustments();
		for (HoldingRatio ratio : desiredHoldingRatios) {
			OwnedAsset asset = getOwnedAsset(ratio.getTicker());
			Double adjustment = adjustments.get(asset.getAsset());
			if (adjustment != null) {
				double thisPercentDifference = adjustment / (asset.getFree() + asset.getLocked());
				drifts.add(new CurrencyDrift(asset.getAsset(), thisPercentDifference));
			}
		}
		return drifts;
	}

	public boolean ifMaxDriftExceededOnAnyCurrency(double driftPercent) {
		Map<String, Double> adjustments = getAdjustments();
		for (HoldingRatio ratio : desiredHoldingRatios) {
			OwnedAsset asset = getOwnedAsset(ratio.getTicker());
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

	public boolean shouldBalance(double minDrift, double minSingleDrift) {
		if (minDrift > 0 && getDriftPercent() > minDrift) {
			return true;
		}
		if (minSingleDrift > 0 && ifMaxDriftExceededOnAnyCurrency(minSingleDrift)) {
			return true;
		}
		return false;
	}

	public List<NewOrderResponse> balance() {
		List<NewOrderResponse> newOrders = new ArrayList<>();
		Map<String, Double> adjustments = getAdjustments();
		Map<String, Double> negativeAdjustments = new HashMap<>();
		Map<String, Double> positiveAdjustments = new HashMap<>();

		for (String ticker : adjustments.keySet()) {
			double amount = adjustments.get(ticker);
			if (amount != 0) {
				if (amount > 0) {
					positiveAdjustments.put(ticker, amount);
				} else {
					negativeAdjustments.put(ticker, amount);
				}
			}
		}

		// First sell
		newOrders.addAll(services.createOrders(negativeAdjustments));
		// Then buy
		newOrders.addAll(services.createOrders(positiveAdjustments));
		return newOrders;
	}
}
