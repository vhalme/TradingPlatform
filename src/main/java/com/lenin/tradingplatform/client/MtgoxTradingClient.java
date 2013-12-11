package com.lenin.tradingplatform.client;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.lenin.tradingplatform.data.entities.Order;
import com.lenin.tradingplatform.data.entities.TradingSession;


public class MtgoxTradingClient extends TradingClient {
	
	
	public MtgoxTradingClient(TradingSession tradingSession,
			MongoTemplate mongoTemplate) {
		
		super(tradingSession, mongoTemplate);
		
	}
	
	
	protected RequestResponse processCancellationResult(JSONObject cancelOrderResult, Order order) {
		
		RequestResponse response = new RequestResponse();
		response.setSuccess(0);
		
		try {
			
			String resultStr = cancelOrderResult.getString("result");
			if(resultStr.equals("success")) {
				response.setSuccess(1);
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return response;
		
	}
	
	
	protected RequestResponse processTradeResult(JSONObject tradeResult, Order order) {
		
		RequestResponse response = new RequestResponse();
		response.setSuccess(0);
		
		try {
			
			String result = tradeResult.getString("result");
			
			if(result.equals("success")) {
				order.setOrderId(tradeResult.getString("return"));
				executeOrder(order);
				response.setSuccess(1);
			} else {
				String error = tradeResult.getString("error");
				response.setData(error);
				response.setMessage("Order was unsuccessful: "+error);
				response.setSuccess(-4);
			}
		
		} catch(JSONException e) {
			
			e.printStackTrace();
			response.setSuccess(-5);
			response.setMessage(e.getMessage());
			response.setData(e.getMessage());
			
		}
		
		
		return response;
		
		
	}
	
	
}
