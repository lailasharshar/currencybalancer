package com.sharshar.currencybalancer.algorithms;

import com.sharshar.currencybalancer.beans.HoldingRatio;
import com.sharshar.currencybalancer.beans.OwnedAsset;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Test to see if historical analysis works correctly
 * Created by lsharshar on 6/21/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class HistoricalAnalysisTest {
	@Autowired
	private HistoricalAnalysis historicalAnalysis;

	@Autowired
	private CurrencyBalancer currencyBalancer;

	@Test
	public void testRandomGenerator() {
		List<Double> testValues = historicalAnalysis.generateRandomValues(10);
		assertEquals(10, testValues.size());
		double totalAmount = testValues.stream().mapToDouble(c -> c).sum();
		System.out.println(totalAmount);
		assertEquals(totalAmount, 1.0, 0.0001);
	}

	@Test
	public void testGetRandomRatios() {
		List<OwnedAsset> ownedAssets = currencyBalancer.getOwnedAssets();
		List<HoldingRatio> desiredRatios = currencyBalancer.getDesiredHoldingRatios();
		List<HoldingRatio> randomRatios = historicalAnalysis.getRandomRatios(ownedAssets, desiredRatios);
		assertEquals(randomRatios.size(), ownedAssets.size());
		double totalAmount = randomRatios.stream().mapToDouble(HoldingRatio::getPercent).sum();
		assertEquals(totalAmount, 1.0, 0.0001);
		System.out.println(randomRatios);
	}

	@Test
	public void doAnalysis() throws ParseException {
		int numberOfRandomTries = 1000;
		SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

		Date startDate = sdf.parse("03/01/2018 00:00:00");
		Date endDate = sdf.parse("05/15/2018 00:00:00");
		double bestResult = 0;

		// Open up a results file
		try (BufferedWriter s = Files.newBufferedWriter(Paths.get("c:/tmp/balancerresults.txt"))) {
			// Since each testMyHistorical
			for (int i=0; i<numberOfRandomTries; i++) {
				List<HistoricalAnalysis.AnalysisResult> results = historicalAnalysis.testMyHistorical(startDate, endDate, s);
				bestResult = results.stream().mapToDouble(c -> c.getAmountBitcoin()).max().orElse(0);
				s.write("****** Best result: " + String.format("%.4f", bestResult));
			}
		} catch (Exception ex) {
			System.out.println("----------------------------------------------------------------------\n****** Best result: " + String.format("%.4f", bestResult));
		}
		//results.forEach(c -> System.out.println(c));
	}
}
