package com.sharshar.currencybalancer.controllers;

import com.sharshar.currencybalancer.beans.HoldingRatio;
import com.sharshar.currencybalancer.services.BalancerServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Used to change the values of the configuration
 *
 * Created by lsharshar on 6/20/2018.
 */
@RestController
public class ConfigureController {
	@Autowired
	BalancerServices balancerServices;

	@GetMapping("/currentConfig")
	public List<HoldingRatio> getRatios() {
		return balancerServices.getDesiredRatios();
	}

	@PutMapping("/currentConfig")
	public List<HoldingRatio> setRatios(@RequestBody List<HoldingRatio> newRatios) {
		if (newRatios == null) {
			return null;
		}
		return balancerServices.saveNewRatios(newRatios);
	}
}
