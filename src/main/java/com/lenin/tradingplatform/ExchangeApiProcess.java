package com.lenin.tradingplatform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.lenin.tradingplatform.client.ApiFactory;

public abstract class ExchangeApiProcess {
	
	@Autowired
	protected MongoTemplate mongoTemplate;
	
	@Autowired
	protected ApiFactory apiFactory;
	
	protected abstract String getService();
	

}
