package com.lenin.tradingplatform.client;

import java.util.List;

import org.codehaus.jettison.json.JSONObject;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.lenin.tradingplatform.data.entities.Order;
import com.lenin.tradingplatform.data.entities.TradingSession;


public class TestTradingClient extends TradingClient {
	
	
	public TestTradingClient(TradingSession tradingSession,
			MongoTemplate mongoTemplate) {
		
		super(tradingSession, mongoTemplate);
		
	}
	
	@Override
	public RequestResponse cancelOrder(Order order) {
		
		RequestResponse response = new RequestResponse();
			
		if(order.getIsReversed()) {
				
			Query orderQuery = new Query(Criteria.where("reversedOrder").is(order));
			List<Order> reverseOrders = mongoTemplate.find(orderQuery, Order.class);
			//List<Order> reverseOrders = orderRepository.findByReversedOrder(order);
				
			for(Order reverseOrder : reverseOrders) {
				compensateCancelledOrder(reverseOrder, tradingSession);
				mongoTemplate.remove(reverseOrder);
			}
				
			mongoTemplate.remove(order);
				
		} else {
				
			compensateCancelledOrder(order, tradingSession);
				
			if(order.getFilledAmount() > 0) {
				Double fillRatio = order.getFilledAmount()/order.getBrokerAmount();
				order.setBrokerAmount(order.getFilledAmount());
				order.setFinalAmount(order.getBrokerAmount()*0.002);
				order.setAmount(order.getAmount()*fillRatio);
				mongoTemplate.save(order);
			} else {
				mongoTemplate.remove(order);
			}
				
		}
			
		response.setSuccess(1);
		
		return response;
		
	}
	
	
	protected RequestResponse processCancellationResult(JSONObject cancelOrderResult, Order order) {
		return null;
	}
	
	@Override
	public RequestResponse trade(Order order) {
		
		RequestResponse response = new RequestResponse();
		
		order.setTradingSession(tradingSession);
		order.setLive(tradingSession.getLive());
		order.setService("test");
		
		Double serviceFeeFactor = 1-MtgoxTradingClient.orderFee;
		order.setBrokerAmount(order.getAmount()*serviceFeeFactor);
			
		Double random = Math.random();
		String orderId = ""+random;
			
		order.setOrderId(orderId);
		order.setReceived(order.getBrokerAmount()*Math.random());
		order.setRemains(order.getBrokerAmount()-order.getReceived());
			
		if(order.getRemains() == 0) {
			order.setFilledAmount(order.getBrokerAmount());
		} else if(order.getReceived() > 0) {
			order.setFilledAmount(order.getReceived());
		}
			
			
		executeOrder(order);
			
		response.setSuccess(1);
		
		return response;
		
	}
	
	
	protected RequestResponse processTradeResult(JSONObject tradeResult, Order order) {
		return null;
	}
	
	
}
