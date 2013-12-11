package com.lenin.tradingplatform.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.lenin.tradingplatform.data.entities.AccountFunds;
import com.lenin.tradingplatform.data.entities.PropertyMap;
import com.lenin.tradingplatform.data.entities.User;

@Component
public class ApiFactory {

	@Autowired
	private MongoTemplate mongoTemplate;
	
	
	public BtceApi createBtceApi(User user) {
		
		AccountFunds account = user.getAccountFunds();
		
		PropertyMap servicePropertyMap = account.getServiceProperties().get("btce");
		Map<String, Object> serviceProperties = servicePropertyMap.getProperties();
		
		String accountKey = (String)serviceProperties.get("apiKey");
		String accountSecret = (String)serviceProperties.get("apiSecret");
		
		BtceApi btceApi = new BtceApi(accountKey, accountSecret, mongoTemplate);
		btceApi.setOrderFee(0.002);
		
		return btceApi;
		
	}
	
	
	public MtgoxApi createMtgoxApi(User user) {
		
		AccountFunds account = user.getAccountFunds();
		
		PropertyMap servicePropertyMap = account.getServiceProperties().get("mtgox");
		Map<String, Object> serviceProperties = servicePropertyMap.getProperties();
		
		String accountKey = (String)serviceProperties.get("apiKey");
		String accountSecret = (String)serviceProperties.get("apiSecret");
		
		MtgoxApi mtGoxApi = new MtgoxApi(accountKey, accountSecret, mongoTemplate);
		Double fee = user.getExchangeFees().get("mtgox");
		
		if(fee == null) {
			fee = 0.005;
		}
		
		mtGoxApi.setOrderFee(fee);
		
		return mtGoxApi;
		
	}
	
	
	public void setMongoTemplate(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}
	
	
}
