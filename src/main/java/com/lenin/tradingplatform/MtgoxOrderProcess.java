package com.lenin.tradingplatform;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.lenin.tradingplatform.client.MtgoxApi;
import com.lenin.tradingplatform.client.TradingClient;
import com.lenin.tradingplatform.data.entities.Order;
import com.lenin.tradingplatform.data.entities.Trade;
import com.lenin.tradingplatform.data.entities.TradingSession;
import com.lenin.tradingplatform.data.entities.User;
import com.lenin.tradingplatform.data.repositories.OrderRepository;
import com.lenin.tradingplatform.data.repositories.TradeRepository;

@Service
public class MtgoxOrderProcess extends OrderProcess {
	
	@Autowired
	private TradeRepository tradeRepository;
	
	@Autowired
	private OrderRepository orderRepository;

	
	protected void updateOrders(User user) {
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		System.out.println("Updating mtgox orders for "+user.getUsername());
		
		Long userOrderUpdateStart = System.currentTimeMillis();
		
		MtgoxApi mtgoxApi = apiFactory.createMtgoxApi(user);
		JSONObject openOrdersJson = mtgoxApi.getOpenOrders();
		
		if(openOrdersJson != null) {
			
			JSONArray openOrdersArray = null;
			
			try {
			
				openOrdersArray = openOrdersJson.getJSONArray("return");
			
				for(int i=0; i<openOrdersArray.length(); i++) {
					
					JSONObject openOrder = openOrdersArray.getJSONObject(i);
					
					String status = openOrder.getString("status");
					String orderId = openOrder.getString("oid");
				
					System.out.println("open order: "+status+"/"+orderId);
				
					Query orderQuery = new Query(Criteria.where("orderId").is(orderId));
					List<Order> ordersResult = mongoTemplate.find(orderQuery, Order.class);
				
					if(ordersResult.size() == 1) {
						System.out.println("Found order");
					}
				}
				
			} catch(Exception e) {
				e.printStackTrace();
			}
			
			Query tradingSessionQuery = new Query(Criteria.where("userId").is(user.getId()).and("service").is("mtgox"));
			List<TradingSession> tradingSessions = mongoTemplate.find(tradingSessionQuery, TradingSession.class);
			//List<TradingSession> tradingSessions = user.getTradingSessions();
			
			for(TradingSession tradingSession : tradingSessions) {
			
				List<Order> orders = orderRepository.findByTradingSession(tradingSession);
				List<Order> changedOrders = new ArrayList<Order>();
				
				for(Order order : orders) {
			
					if(!order.getIsFilled() && order.getIsReversed() == false) {
						
						Boolean orderFound = false;
						
						for(int i=0; i<openOrdersArray.length(); i++) {
							
							try {
							
								JSONObject openOrder = openOrdersArray.getJSONObject(i);
							
								if(openOrder.getString("oid").equals(order.getOrderId())) {
									orderFound = true;
									break;
								}
								
							} catch(Exception e) {
								e.printStackTrace();
							}
							
						}
						
						if(orderFound == false) {
							
							order.setFilledAmount(order.getBrokerAmount());
							order.setRemains(0.0);
							
							Double brokerFeeFactor = 1 - mtgoxApi.getOrderFee();
							
							if(order.getType().equals("buy")) {
								
								Double rightCurrencyVal = order.getBrokerAmount() * brokerFeeFactor;
								tradingSession.setFundsRight(tradingSession.getFundsRight() + rightCurrencyVal);
								mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
										new Update().set("fundsRight", tradingSession.getFundsRight()), TradingSession.class);
						
							} else if(order.getType().equals("sell")) {
						
								Double leftCurrencyVal = (order.getBrokerAmount() * brokerFeeFactor) * order.getRate();
								tradingSession.setFundsLeft(tradingSession.getFundsLeft() + leftCurrencyVal);
								mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
										new Update().set("fundsLeft", tradingSession.getFundsLeft()), TradingSession.class);
						
							}
							
							Order reversedOrder = order.getReversedOrder();
							
							if(reversedOrder != null) {
								
								String profitSide = "left";
								
								Trade trade = new Trade();
								trade.setAmount(order.getFilledAmount());
								
								Double profitLeft = tradingSession.getProfitLeft();
								Double tradeProfit = order.calcProfit(trade, brokerFeeFactor);
								tradingSession.setProfitLeft(profitLeft + tradeProfit);
								mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
										new Update().set("profitLeft", tradingSession.getProfitLeft()), TradingSession.class);
								
								String orderMode = reversedOrder.getMode();
								if(orderMode != null && !orderMode.equals("manual") && user.getLive() == true) {
									TradingClient.processProfit(user, tradeProfit, profitSide, tradingSession, mongoTemplate);
								}
								
							}
							
							changedOrders.add(order);
							
						}
				
				
					} else if(order.getIsReversed() == true) {
						
						List<Order> reverseOrders = orderRepository.findByReversedOrder(order);
						
						if(!order.getIsFilled()) {
							
							Double reverseFilledTotal = 0.0;
							
							for(Order reverseOrder : reverseOrders) {
								reverseFilledTotal += reverseOrder.getFilledAmount();
							}
							
							order.setFilledAmount(reverseFilledTotal);
							changedOrders.add(order);
						
							if(order.getIsFilled()) {
								changedOrders.remove(order);
								changedOrders.remove(reverseOrders);
								orderRepository.delete(order);
								for(Order reverseOrder : reverseOrders) {
									orderRepository.delete(reverseOrder);
								}
							}
						
						} else {
							
							orderRepository.delete(order);
							for(Order reverseOrder : reverseOrders) {
								orderRepository.delete(reverseOrder);
							}
							
						}
						
					}
			
					if(order.getLive() == false && !order.getIsFilled()) {
				
						Long unixTime = System.currentTimeMillis() / 1000L;
				
						Trade trade = new Trade();
						trade.setLive(false);
						trade.setRate(order.getRate());
						trade.setOrderId(order.getOrderId());
						
						BigDecimal bdBrokerAmount = new BigDecimal(""+order.getBrokerAmount());
						BigDecimal bdFilledAmount = new BigDecimal(""+order.getFilledAmount());
						trade.setAmount((bdBrokerAmount.subtract(bdFilledAmount)).doubleValue());
						trade.setTime(unixTime);
				
						tradeRepository.save(trade);
				
					}
			
				}
				
				orderRepository.save(changedOrders);
				
			}
		
		}
		
		Long userOrderUpdateTime = (System.currentTimeMillis() - userOrderUpdateStart) / 1000L;
		System.out.println("Orders update for "+user.getUsername()+" complete in "+userOrderUpdateTime+" s.");
		
	}
	
	
	protected String getService() {
		return "mtgox";
	}
	
	
}
