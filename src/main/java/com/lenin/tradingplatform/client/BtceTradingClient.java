package com.lenin.tradingplatform.client;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.lenin.tradingplatform.data.entities.AccountFunds;
import com.lenin.tradingplatform.data.entities.Order;
import com.lenin.tradingplatform.data.entities.Rate;
import com.lenin.tradingplatform.data.entities.Settings;
import com.lenin.tradingplatform.data.entities.Trade;
import com.lenin.tradingplatform.data.entities.TradingSession;
import com.lenin.tradingplatform.data.entities.User;


public class BtceTradingClient extends TradingClient {
	
	public BtceTradingClient(TradingSession tradingSession,
			MongoTemplate mongoTemplate) {
		
		super(tradingSession, mongoTemplate);
		
	}
	
	
	protected RequestResponse processCancellationResult(JSONObject cancelOrderResult, Order order) {
		
		RequestResponse response = new RequestResponse();
		
		try {
		
			Integer success = cancelOrderResult.getInt("success");
			response.setSuccess(success);
		
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return response;
		
	}

	
	
	protected RequestResponse processTradeResult(JSONObject tradeResult, Order order) {
		
		RequestResponse response = new RequestResponse();
		response.setSuccess(0);
		
		try {
			
			Integer success = tradeResult.getInt("success");
			response.setSuccess(success);
			
			if(success == 1) {
				
				JSONObject resultData = tradeResult.getJSONObject("return");
				String orderId = resultData.getString("order_id");
				Double received = resultData.getDouble("received");
				Double remains = resultData.getDouble("remains");
				
				order.setOrderId(orderId);
				order.setReceived(received);
				order.setRemains(remains);
				
				if(order.getRemains() == 0) {
					order.setFilledAmount(order.getBrokerAmount());
				} else if(order.getReceived() > 0) {
					order.setFilledAmount(order.getReceived());
				}
				
				System.out.println("Trade request posted successfully.");
				executeOrder(order);
				
			} else {
				
				BtceApi._nonce = System.currentTimeMillis() / 10000L;
				
				System.out.println("Trade request failed: "+success);
				Iterator<String> keys = tradeResult.keys();
				
				while(keys.hasNext()) {
					String key = keys.next();
					System.out.println(key+" : "+tradeResult.get(key));
				}
				
				String errorMessage = tradeResult.getString("error");
				
				response.setMessage("Order was unsuccessful: "+errorMessage);
				response.setData(errorMessage);
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
