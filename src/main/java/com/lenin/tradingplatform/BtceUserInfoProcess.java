package com.lenin.tradingplatform;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.lenin.tradingplatform.client.BtceApi;
import com.lenin.tradingplatform.client.MtgoxApi;
import com.lenin.tradingplatform.data.entities.AccountFunds;
import com.lenin.tradingplatform.data.entities.ErrorMessage;
import com.lenin.tradingplatform.data.entities.TradingSession;
import com.lenin.tradingplatform.data.entities.User;
import com.lenin.tradingplatform.data.repositories.OrderRepository;
import com.lenin.tradingplatform.data.repositories.UserRepository;

@Service
public class BtceUserInfoProcess extends UserInfoProcess {
	
	
	private String service = "btce";
	
	
	protected List<ErrorMessage> updateUserInfo(User user) throws Exception {
		
		List<ErrorMessage> userErrors = new ArrayList<ErrorMessage>();
		
		Long userInfoUpdateStart = System.currentTimeMillis();
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		BtceApi btceApi = apiFactory.createBtceApi(user);
		
		JSONObject userInfoResult = null; //btceApi.getAccountInfo();
		JSONObject userInfo = null; //userInfoResult.getJSONObject("return");
		JSONObject userFunds = null; //userInfo.getJSONObject("funds");
		
		userInfoResult = btceApi.getAccountInfo();
		
		if(userInfoResult != null) {
			
			if(userInfoResult.getInt("success") == 1) {
				
				userInfo = userInfoResult.getJSONObject("return");
				userFunds = userInfo.getJSONObject("funds");
				JSONObject apiRights = userInfo.getJSONObject("rights");
				
				if(apiRights.getInt("trade") == 0) {
					ErrorMessage errorMessage = new ErrorMessage();
					errorMessage.setMessage("api key dont have trade permission");
					errorMessage.setCode(0);
					userErrors.add(errorMessage);
				}

				List<TradingSession> tradingSessions = user.getTradingSessions();
				
				for(TradingSession tradingSession : tradingSessions) {
				
					if(tradingSession.getService().equals("btce") && tradingSession.getLive() == true) {
					
						String currencyLeft = tradingSession.getCurrencyLeft();
						String currencyRight = tradingSession.getCurrencyRight();
					
						Double accountFundsLeft = userFunds.getDouble(currencyLeft);
						Double accountFundsRight = userFunds.getDouble(currencyRight);
						Double sessionFundsLeft = tradingSession.getFundsLeft();
						Double sessionFundsRight = tradingSession.getFundsRight();
					
						System.out.println("Session "+tradingSession.getPair()+": "+currencyLeft+"> "+accountFundsLeft+" - "+sessionFundsLeft);
						System.out.println("Session "+tradingSession.getPair()+": "+currencyRight+"> "+accountFundsRight+" - "+sessionFundsRight);
						userFunds.put(currencyLeft, accountFundsLeft - sessionFundsLeft);
						userFunds.put(currencyRight, accountFundsRight - sessionFundsRight);
						
					
					}
				
				}
			
				Double userFundsBtc = userFunds.getDouble("btc");
				Double userFundsLtc = userFunds.getDouble("ltc");
				Double userFundsUsd = userFunds.getDouble("usd");
			
				System.out.println("Final funds: "+userFundsBtc+"/"+userFundsLtc+"/"+userFundsUsd);
				
				AccountFunds account = user.getAccountFunds(); 
				Map<String, Double> userBtceFunds = account.getActiveFunds().get("btce");
				userBtceFunds.put("btc", userFundsBtc);
				userBtceFunds.put("ltc", userFundsLtc);
				userBtceFunds.put("usd", userFundsUsd);
				
				mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(account.getId()))),
						new Update().set("activeFunds.btce.btc", userFundsBtc), AccountFunds.class);
				mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(account.getId()))),
						new Update().set("activeFunds.btce.ltc", userFundsLtc), AccountFunds.class);
				mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(account.getId()))),
						new Update().set("activeFunds.btce.usd", userFundsUsd), AccountFunds.class);
				
				//mongoOps.save(account);
			
			} else {
				
				String error = userInfoResult.getString("error");
				
				if(!error.startsWith("invalid nonce parameter")) {
					
					System.out.println("There was an error: "+error);
					
					ErrorMessage errorMessage = new ErrorMessage();
					errorMessage.setMessage(error);
					errorMessage.setCode(0);
					userErrors.add(errorMessage);
					
				} else {
					
					System.out.println("Info update unsuccessful: "+error);
					
				}
				
				
			}
			
			
		}
		
		Long userInfoUpdateTime = (System.currentTimeMillis() - userInfoUpdateStart)/1000L;
		System.out.println("Info update for "+user.getUsername()+" completed in "+userInfoUpdateTime+" s.");
		
		return userErrors;
		
	}
	
	
	protected String getService() {
		return service;
	}
	
}
