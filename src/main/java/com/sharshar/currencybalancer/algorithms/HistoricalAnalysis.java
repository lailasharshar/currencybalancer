package com.sharshar.currencybalancer.algorithms;

import com.sharshar.currencybalancer.beans.HoldingRatio;
import com.sharshar.currencybalancer.beans.OwnedAsset;
import com.sharshar.currencybalancer.beans.PriceData;
import com.sharshar.currencybalancer.binance.BinanceAccountServices;
import com.sharshar.currencybalancer.repository.PriceDataSQLRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created to try to find the best strategy to balance different currency holdings
 *
 * Created by lsharshar on 6/21/2018.
 */
@Service
public class HistoricalAnalysis {

	Logger logger = LogManager.getLogger();

	private static final long DAY_MICROSECONDS = 1000L * 60 * 60 * 24;

	@Autowired
	PriceDataSQLRepository priceDataSQLRepository;

	@Autowired
	CurrencyBalancer cb;

	@Autowired
	BinanceAccountServices accountServices;

	public class AnalysisResult {
		private List<OwnedAsset> initialOwnedAssets;
		private List<OwnedAsset> finalOwnedAssets;
		private List<HoldingRatio> holdingRatios;
		private int daysBetween;
		private Date startDate;
		private Date endDate;
		private double amountBitcoin;
		private double amountUsd;
		private double initialAmountBitcoin;

		public double getInitialAmountBitcoin() {
			return initialAmountBitcoin;
		}

		public AnalysisResult setInitialAmountBitcoin(double initialAmountBitcoin) {
			this.initialAmountBitcoin = initialAmountBitcoin;
			return this;
		}

		public List<OwnedAsset> getInitialOwnedAssets() {
			return initialOwnedAssets;
		}

		public AnalysisResult setInitialOwnedAssets(List<OwnedAsset> initialOwnedAssets) {
			this.initialOwnedAssets = initialOwnedAssets;
			return this;
		}

		public List<HoldingRatio> getHoldingRatios() {
			return holdingRatios;
		}

		public AnalysisResult setHoldingRatios(List<HoldingRatio> holdingRatios) {
			this.holdingRatios = holdingRatios;
			return this;
		}

		public int getDaysBetween() {
			return daysBetween;
		}

		public AnalysisResult setDaysBetween(int daysBetween) {
			this.daysBetween = daysBetween;
			return this;
		}

		public Date getStartDate() {
			return startDate;
		}

		public AnalysisResult setStartDate(Date startDate) {
			this.startDate = startDate;
			return this;
		}

		public Date getEndDate() {
			return endDate;
		}

		public AnalysisResult setEndDate(Date endDate) {
			this.endDate = endDate;
			return this;
		}

		public double getAmountBitcoin() {
			return amountBitcoin;
		}

		public AnalysisResult setAmountBitcoin(double amountBitcoin) {
			this.amountBitcoin = amountBitcoin;
			return this;
		}

		public double getAmountUsd() {
			return amountUsd;
		}

		public AnalysisResult setAmountUsd(double amountUsd) {
			this.amountUsd = amountUsd;
			return this;
		}

		public List<OwnedAsset> getFinalOwnedAssets() {
			return finalOwnedAssets;
		}

		public AnalysisResult setFinalOwnedAssets(List<OwnedAsset> finalOwnedAssets) {
			this.finalOwnedAssets = finalOwnedAssets;
			return this;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("---------------------------------------------------------------------------------------------\n")
				.append("Dates: ").append(startDate).append(" - ").append(endDate).append("\n")
				.append("Frequency: ").append(daysBetween).append(" days\n")
				.append("Holding Ratios: \n")
					;

			for (HoldingRatio ratio : holdingRatios) {
				builder.append("    ").append(ratio.getTicker()).append(": ").append(String.format("%.4f", ratio.getPercent() * 100)).append("\n");
			}
			builder.append("Final owned assets:\n");
			for (OwnedAsset asset : finalOwnedAssets) {
				builder.append("    ").append(asset.getAsset()).append(": ").append(String.format("%.4f", asset.getFree() + asset.getLocked())).append("\n");
			}
			builder.append("Initial Bitcoin: ").append(String.format("%.4f", initialAmountBitcoin)).append("\n");
			builder.append("Total Bitcoin: ").append(String.format("%.4f", amountBitcoin)).append(" ($").append(amountUsd).append(")\n");
			builder.append("Profit: ").append(String.format("%.4f", amountBitcoin - initialAmountBitcoin)).append("\n");
			return builder.toString();
		}
	}

	public List<AnalysisResult>  testMyHistorical(Date startDate, Date endDate, BufferedWriter writer) {
		// Use existing owned assets
		List<OwnedAsset> ownedAssets = cb.getOwnedAssets();
		// Remove ONT for now
		List<OwnedAsset> newAssets = new ArrayList<>(ownedAssets);
		OwnedAsset ont = ownedAssets.stream().filter(c -> c.getAsset().equalsIgnoreCase("ONT")).findFirst().orElse(null);
		newAssets.remove(ont);
		return testHistorical(newAssets, startDate, endDate, writer);
	}

	public List<PriceData> getInitialPrices(List<String> tickers, Date startDate) {
		List<PriceData> initialPrices = new ArrayList<>();
		List<PriceData> pds = priceDataSQLRepository.
				findByUpdateTimeGreaterThanAndUpdateTimeLessThan(startDate, new Date(startDate.getTime() + 3 * 60 * 60 * 1000));
		if (pds != null && !pds.isEmpty()) {
			for (String s : tickers) {
				PriceData pd = pds.stream().filter(c -> c.getTicker().equalsIgnoreCase(s)).findFirst().orElse(null);
				if (pd != null) {
					initialPrices.add(pd);
				}
			}
		}
		return initialPrices;
	}

	public List<AnalysisResult> testHistorical(List<OwnedAsset> ownedAssets, Date startDate, Date endDate, BufferedWriter writer) {
		// create random values for the dedired holding ratios
		List<AnalysisResult> results = new ArrayList<>();
		List<HoldingRatio> ratios = getRandomRatios(ownedAssets, cb.getDesiredHoldingRatios());
		List<PriceData> latestPriceData = accountServices.getData();
		PriceData usd = getPriceData(latestPriceData, "USDTBTC");
		List<String> initialTickers = ownedAssets.stream().map(c -> c.getAsset() + "BTC").collect(Collectors.toList());
		List<PriceData> initialPriceData = getInitialPrices(initialTickers, startDate);
		double usdprice = 0.0;
		if (usd != null) {
			usdprice = usd.getPrice();
		}

		for (int i=1; i<= 30; i++) {
			List<OwnedAsset> finalAssets = getFinalAssetsOwned(ratios, i, startDate, endDate, ownedAssets, "BTC");
			double totalBitcoins = CurrencyBalancer.getTotalValue(finalAssets, ratios, latestPriceData, "BTC");
			AnalysisResult result = new AnalysisResult()
					.setHoldingRatios(ratios)
					.setDaysBetween(i)
					.setStartDate(startDate)
					.setEndDate(endDate)
					.setFinalOwnedAssets(finalAssets)
					.setAmountBitcoin(totalBitcoins)
					.setAmountUsd(usdprice * totalBitcoins)
					.setInitialOwnedAssets(ownedAssets)
					.setInitialAmountBitcoin(CurrencyBalancer.getTotalValue(ownedAssets, ratios, initialPriceData, "BTC"));
			try {
				writer.write(result.toString());
			} catch (Exception ex) {
				System.out.println(result.toString());
			}
			results.add(result);
		}
		return results;
	}

	public List<HoldingRatio> getRandomRatios(List<OwnedAsset> ownedAssets, List<HoldingRatio> currentInfo) {
		List<HoldingRatio> ratios = new ArrayList<>();
		if (ownedAssets == null) {
			return ratios;
		}
		List<Double> randomValues = generateRandomValues(ownedAssets.size());
		for (int i=0; i< ownedAssets.size(); i++) {
			OwnedAsset asset = ownedAssets.get(i);
			HoldingRatio ratio = new HoldingRatio();
			ratio.setPercent(randomValues.get(i));
			// We don't have a good way to determine if we can trade fractional numbers, so see if we already know
			// otherwise, it's okay to assume we can't
			HoldingRatio existingOne = getHoldingRatio(currentInfo, asset.getAsset());
			if (existingOne != null) {
				ratio.setFraction(existingOne.canDoFraction());
			} else {
				ratio.setFraction(false);
			}
			ratio.setTicker(asset.getAsset());
			ratios.add(ratio);
		}
		return ratios;
	}

	public List<Double> generateRandomValues(int numberOfValues) {
		List<Double> initialRatios = new ArrayList<>();
		for (int i=0; i<numberOfValues; i++) {
			initialRatios.add(Math.random());
		}
		// Now correct to make sure they add up to 100%
		double totalAmount = initialRatios.stream().mapToDouble(c -> c).sum();

		List<Double> finalRatios = new ArrayList<>();
		for (int i=0; i<numberOfValues; i++) {
			finalRatios.add(initialRatios.get(i)/totalAmount);
		}
		return finalRatios;
	}


	private List<OwnedAsset> getFinalAssetsOwned(List<HoldingRatio> desiredRatios, int daysBetween,
												 Date startDate, Date endDate, List<OwnedAsset> startingAssets,
												 String baseCurrency) {
		Map<String, List<PriceData>> allPriceDataFromDb = new HashMap<>();
		for (HoldingRatio ratio : desiredRatios) {
			List<PriceData> data = null;
			if (ratio.getTicker().equalsIgnoreCase(baseCurrency)) {
				// It's itself, so no conversion. Assume 1 to 1 conversion, but get the data so it has data with time
				data = priceDataSQLRepository.findByTickerAndUpdateTimeGreaterThanAndUpdateTimeLessThan(
						ratio.getTicker() + "USDT", startDate, endDate);
				data.stream().forEach(c -> c.setPrice(1.0));
				data.stream().forEach(c -> c.setTicker("BTCBTC"));
			} else {
				data = priceDataSQLRepository.findByTickerAndUpdateTimeGreaterThanAndUpdateTimeLessThan(
						ratio.getTicker() + baseCurrency, startDate,endDate);
			}
			allPriceDataFromDb.put(ratio.getTicker(), data);
		}
		List<OwnedAsset> assets = new ArrayList<>();
		assets.addAll(startingAssets);
		Date newDate = startDate;
		List<PriceData> lastPrices = new ArrayList<>();
		double totalValue;
		while (newDate.getTime() < endDate.getTime()) {
			for (HoldingRatio ratio : desiredRatios) {
				PriceData val = getPrice(allPriceDataFromDb.get(ratio.getTicker()), ratio.getTicker(), newDate, baseCurrency);
				if (val != null) {
					PriceData pd = new PriceData().setUpdateTime(val.getUpdateTime()).setTicker(ratio.getTicker() + baseCurrency).setPrice(val.getPrice());
					lastPrices.add(pd);
				}
			}
			totalValue = CurrencyBalancer.getTotalValue(assets, desiredRatios, lastPrices, baseCurrency);
			logger.info("Total Value: " + totalValue);
			List<OwnedAsset> newAssets = new ArrayList<>();
			for (OwnedAsset asset : assets) {
				double desiredRatio = getRatio(desiredRatios, asset.getAsset());
				PriceData pd = getPriceData(lastPrices, asset.getAsset() + baseCurrency);
				// double totalAmountForThis = (asset.getFree() + asset.getLocked()) * pd.getPrice();
				double adjustment = CurrencyBalancer.getAdjustment(asset, desiredRatio, pd.getPrice(), totalValue);
				OwnedAsset a = new OwnedAsset()
						.setAsset(asset.getAsset())
						.setFree(asset.getFree() + asset.getLocked() + adjustment)
						.setLocked(0.0);
				newAssets.add(a);
			}
			newDate = new Date(newDate.getTime() + (daysBetween * DAY_MICROSECONDS));
			lastPrices.clear();
			assets = newAssets;
		}
		return assets;
	}

	public static PriceData getPriceData(List<PriceData> priceData, String ticker) {
		return priceData.stream()
				.filter(c -> c.getTicker().equalsIgnoreCase(ticker))
				.findFirst().orElse(null);
	}

	public static HoldingRatio getHoldingRatio(List<HoldingRatio> holdingRatios, String ticker) {
		return holdingRatios.stream()
				.filter(c -> c.getTicker().equalsIgnoreCase(ticker))
				.findFirst().orElse(null);
	}

	public double getRatio(List<HoldingRatio> ratios, String ticker) {
		HoldingRatio ratio = ratios.stream()
				.filter(c -> c.getTicker().equalsIgnoreCase(ticker))
				.findFirst().orElse(null);
		if (ratio == null) {
			return 0.0;
		}
		return ratio.getPercent();
	}

	private PriceData getPrice(List<PriceData> prices, String ticker, Date firstAfter, String baseCurr) {
		if (prices == null || prices.isEmpty() || ticker ==  null || ticker.isEmpty() || firstAfter == null) {
			return null;
		}
		PriceData result = prices.stream()
				.filter(c -> c.getTicker().equalsIgnoreCase(ticker + baseCurr))
				.filter(c -> c.getUpdateTime().getTime() >= firstAfter.getTime())
				.findFirst().orElse(null);
		return result;
	}
}
