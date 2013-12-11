package com.lenin.tradingplatform;

import org.springframework.stereotype.Service;

@Service
public class TestOrderProcess extends BtceOrderProcess {
	
	@Override
	protected String getService() {
		return null;
	}
	
	
}
