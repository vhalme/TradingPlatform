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
public class MtgoxUserInfoProcess extends UserInfoProcess {
	
	
	private String service = "mtgox";
	
	
	protected List<ErrorMessage> updateUserInfo(User user) throws Exception {
		
		List<ErrorMessage> userErrors = new ArrayList<ErrorMessage>();
		
		Long userInfoUpdateStart = System.currentTimeMillis();
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		MtgoxApi mtGoxApi = apiFactory.createMtgoxApi(user);
		
		JSONObject accountInfoResult = mtGoxApi.getAccountInfo();
		Map<String, Double> userFunds = new HashMap<String, Double>();
		
		if(accountInfoResult != null) {
			
			String result = accountInfoResult.getString("result");
			if(result != null && result.equals("success")) {
				
				accountInfoResult = accountInfoResult.getJSONObject("return");
				
				JSONArray rightsArray = accountInfoResult.getJSONArray("Rights");
				
				Boolean getInfoRight = false;
				Boolean tradeRight = false;
			
				for(int i=0; i<rightsArray.length(); i++) {
					
					String right = rightsArray.getString(i);
					if(right.equals("get_info")) {
						getInfoRight = true;
					} else if(right.equals("trade")) {
						tradeRight = true;
					}
				
				}
			
				if(getInfoRight == false) {
					ErrorMessage errorMessage = new ErrorMessage();
					errorMessage.setMessage("api key dont have info permission");
					errorMessage.setCode(0);
					userErrors.add(errorMessage);
				}
			
				if(tradeRight == false) {
					ErrorMessage errorMessage = new ErrorMessage();
					errorMessage.setMessage("api key dont have trade permission");
					errorMessage.setCode(0);
					userErrors.add(errorMessage);
				}
			
				if(getInfoRight == true) {
					
					Double tradeFee = accountInfoResult.getDouble("Trade_Fee");
					mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(user.getId()))),
							new Update().set("exchangeFees.mtgox", tradeFee*0.01), User.class);
					
					JSONObject walletsJson = accountInfoResult.getJSONObject("Wallets");
					
					if(walletsJson.get("USD") == null) {
						
						ErrorMessage errorMessage = new ErrorMessage();
						errorMessage.setMessage("missing usd wallet");
						errorMessage.setCode(0);
						userErrors.add(errorMessage);
					
					} else {

						Iterator<String> walletKeys = walletsJson.keys();
						
						while(walletKeys.hasNext()) {
							
							String walletKey = walletKeys.next();
							
							JSONObject walletJson = walletsJson.getJSONObject(walletKey);
							Double walletBalance = walletJson.getJSONObject("Balance").getDouble("value");
					
							if(walletKey.equals("USD")) {
								userFunds.put("usd", walletBalance);
							} else if(walletKey.equals("BTC")) {
								userFunds.put("btc", walletBalance);
							}
					
						}
				
						List<TradingSession> tradingSessions = user.getTradingSessions();
					
						for(TradingSession tradingSession : tradingSessions) {
						
							if(tradingSession.getService().equals("mtgox") && tradingSession.getLive() == true) {
							
								String currencyLeft = tradingSession.getCurrencyLeft();
								String currencyRight = tradingSession.getCurrencyRight();
								Double accountFundsLeft = userFunds.get(currencyLeft);
								Double accountFundsRight = userFunds.get(currencyRight);
								Double sessionFundsLeft = tradingSession.getFundsLeft();
								Double sessionFundsRight = tradingSession.getFundsRight();
							
								System.out.println("Session "+tradingSession.getPair()+": "+currencyLeft+"> "+accountFundsLeft+" - "+sessionFundsLeft);
								System.out.println("Session "+tradingSession.getPair()+": "+currencyRight+"> "+accountFundsRight+" - "+sessionFundsRight);
								userFunds.put(currencyLeft, accountFundsLeft - sessionFundsLeft);
								userFunds.put(currencyRight, accountFundsRight - sessionFundsRight);
							
							}
						
						}
					
						Double userFundsBtc = userFunds.get("btc");
						Double userFundsUsd = userFunds.get("usd");
					
						System.out.println("Final funds: "+userFundsBtc+"/"+userFundsUsd);
						
						AccountFunds account = user.getAccountFunds(); 
						Map<String, Double> userBtceFunds = account.getActiveFunds().get("btce");
						userBtceFunds.put("btc", userFundsBtc);
						userBtceFunds.put("usd", userFundsUsd);
					
						mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(account.getId()))),
								new Update().set("activeFunds.mtgox.btc", userFundsBtc), AccountFunds.class);
						mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(account.getId()))),
								new Update().set("activeFunds.mtgox.usd", userFundsUsd), AccountFunds.class);
					
				
					}
					
				}
				
			} else {
				
				String error = accountInfoResult.getString("error");
				
				System.out.println("There was an error: "+error);
				
				ErrorMessage errorMessage = new ErrorMessage();
				errorMessage.setMessage(error);
				errorMessage.setCode(0);
				userErrors.add(errorMessage);
				
			}
			
		} else {
				
			
			System.out.println("There was an error with mtgox");
			
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setMessage("mt gox error");
			errorMessage.setCode(0);
			userErrors.add(errorMessage);
			
		}
		
		
		Long userInfoUpdateTime = (System.currentTimeMillis() - userInfoUpdateStart)/1000L;
		System.out.println("Info update for "+user.getUsername()+" completed in "+userInfoUpdateTime+" s.");
		
		return userErrors;
		
	}
	
	
	protected String getService() {
		
		return service;
		
	}
	
	
}
