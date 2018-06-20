package com.sharshar.currencybalancer.controllers;

import com.sharshar.currencybalancer.beans.OwnedAsset;
import com.sharshar.currencybalancer.services.BalancerServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by lsharshar on 6/20/2018.
 */
@RestController
public class BalancerController {
	@Autowired
	private BalancerServices balancerServices;

	@GetMapping("/assets")
	public List<OwnedAsset> getOwnedAssets() {
		return balancerServices.getOwnedAssets();
	}
}
