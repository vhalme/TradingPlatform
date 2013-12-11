package com.lenin.tradingplatform;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.lenin.tradingplatform.client.BtceApi;
import com.lenin.tradingplatform.client.MtgoxApi;
import com.lenin.tradingplatform.client.TradingClient;
import com.lenin.tradingplatform.data.entities.AccountFunds;
import com.lenin.tradingplatform.data.entities.AutoTradingOptions;
import com.lenin.tradingplatform.data.entities.ErrorMessage;
import com.lenin.tradingplatform.data.entities.Order;
import com.lenin.tradingplatform.data.entities.PropertyMap;
import com.lenin.tradingplatform.data.entities.Rate;
import com.lenin.tradingplatform.data.entities.Settings;
import com.lenin.tradingplatform.data.entities.Trade;
import com.lenin.tradingplatform.data.entities.TradingSession;
import com.lenin.tradingplatform.data.entities.User;
import com.lenin.tradingplatform.data.repositories.OrderRepository;
import com.lenin.tradingplatform.data.repositories.RateRepository;
import com.lenin.tradingplatform.data.repositories.TradeRepository;
import com.lenin.tradingplatform.data.repositories.TradingSessionRepository;
import com.lenin.tradingplatform.data.repositories.UserRepository;

@Service
public class BtceOrderProcess extends OrderProcess {
	
	@Autowired
	private TradeRepository tradeRepository;
	
	@Autowired
	private OrderRepository orderRepository;
	
	
	private List<ErrorMessage> updateTrades(User user) throws Exception {
		
		List<ErrorMessage> userErrors = new ArrayList<ErrorMessage>();
		
		Long userTradesUpdateStart = System.currentTimeMillis();
		
		BtceApi btceApi = apiFactory.createBtceApi(user);
		
		Long lastTradeTime = user.getLastBtceTradeTime();
		
		if(lastTradeTime == 0) {
			lastTradeTime = System.currentTimeMillis()/1000L;
			user.setLastBtceTradeTime(lastTradeTime);
			mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(user.getId()))),
					new Update().set("lastBtceTradeTime", lastTradeTime), User.class);
			//userRepository.save(user);
		}
		
		
		Long resultsFrom = lastTradeTime + 1;
		System.out.println("Getting results from "+resultsFrom);
		
		JSONObject tradeListResult = btceApi.getTradeList(resultsFrom);
		
		if(tradeListResult != null) {
			
			if(tradeListResult.getInt("success") == 1) {
				
				JSONObject tradeListResultData = tradeListResult.getJSONObject("return");
				Iterator<String> tradeIds = tradeListResultData.keys();
				
				List<Trade> trades = new ArrayList<Trade>();
			
				while(tradeIds.hasNext()) {
				
					String tradeId = tradeIds.next();
					
					JSONObject tradeData = tradeListResultData.getJSONObject(tradeId);
				
					String orderId = tradeData.getString("order_id");
					String pair = tradeData.getString("pair");
					Double amount = tradeData.getDouble("amount");
					Double rate = tradeData.getDouble("rate");
					String type = tradeData.getString("type");
					Long time = tradeData.getLong("timestamp");
				
					//List<Trade> oldTrades = tradeRepository.findByOrderIdAndRateAndAmountAndTime(orderId, rate, amount, time);
					Trade oldTrade = tradeRepository.findByTradeId(tradeId);
					if(oldTrade == null) {
					
						Trade trade = new Trade();
						trade.setTradeId(tradeId);
						trade.setService("btce");
						trade.setAccountFunds(user.getAccountFunds());
						trade.setLive(true);
						trade.setOrderId(orderId);
						trade.setPair(pair);
						trade.setAmount(amount);
						trade.setRate(rate);
						trade.setType(type);
						trade.setTime(time);
						
						trades.add(trade);
					
					}
					
					if(time > lastTradeTime) {
						lastTradeTime = time;
					}
					
				}
				
				
				if(trades.size() > 0) {
					
					System.out.println("New trades: "+trades.size()+"; Last trade time: "+lastTradeTime);
					tradeRepository.save(trades);
					
					user.setLastBtceTradeTime(lastTradeTime);
					
					mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(user.getId()))),
							new Update().set("lastBtceTradeTime", lastTradeTime), User.class);
					
					
				}
			
			} else {
			
				String error = tradeListResult.getString("error");
				
				if(!error.startsWith("invalid nonce parameter")) {
					
					if(!error.equals("no trades")) {
					
						System.out.println("There was an error: "+error);
						
						ErrorMessage errorMessage = new ErrorMessage();
						errorMessage.setMessage(error);
						errorMessage.setCode(1);
						userErrors.add(errorMessage);
						
					}
					
				} else {
					
					System.out.println("Trades update unsuccessful: "+error);
					
				}	
				
			
			}
		
		
		}
			
		Long userTradesUpdateTime = (System.currentTimeMillis() - userTradesUpdateStart)/1000L;
		System.out.println("Trades update for "+user.getUsername()+" completed in "+userTradesUpdateTime+" s.");
		
		return userErrors;
		
	}
	
	
	protected void updateOrders(User user) {
		
		String service = "btce";
		
		if(user.getLive() == true) {

			try {
				
				updateTrades(user);
			
			} catch(Exception e) {
				e.printStackTrace();
			}
			
		} else {
			
			service = "test";
			
		}
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		System.out.println("Updating orders for "+user.getUsername());
		
		Long userOrderUpdateStart = System.currentTimeMillis();
		
		Query tradingSessionQuery = new Query(Criteria.where("userId").is(user.getId()).and("service").is(service));
		List<TradingSession> tradingSessions = mongoTemplate.find(tradingSessionQuery, TradingSession.class);
		
		for(TradingSession tradingSession : tradingSessions) {
			
			List<Order> orders = orderRepository.findByTradingSession(tradingSession);
			List<Order> changedOrders = new ArrayList<Order>();
			
			for(Order order : orders) {
		
				if(!order.getIsFilled() && order.getIsReversed() == false) {
			
					List<Trade> trades = tradeRepository.findByOrderId(order.getOrderId());
			
					Double ordersAmount = 0.0;
			
					if(trades.size() > 0) {
				
						for(Trade trade : trades) {
							ordersAmount += trade.getAmount();
						}
			
					}
			
					Double receivedAmount = order.getReceived();
					Double totalOrderAmount = receivedAmount + ordersAmount;
			
					if(totalOrderAmount > order.getFilledAmount()) {
				
						BtceApi btceApi = new BtceApi("", "", null);
						btceApi.setOrderFee(0.002);
				
						Double brokerFeeFactor = 1 - btceApi.getOrderFee();
						Double serviceFeeFactor = 1 - TradingClient.orderFee;
						Double totalFeeFactor = serviceFeeFactor * brokerFeeFactor;
				
						Double amountChange = totalOrderAmount-order.getFilledAmount();
				
						//TradingSession tradingSession = order.getTradingSession();
				
						if(order.getType().equals("buy")) {
					
							Double rightCurrencyVal = amountChange * brokerFeeFactor;
							tradingSession.setFundsRight(tradingSession.getFundsRight() + rightCurrencyVal);
							mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
									new Update().set("fundsRight", tradingSession.getFundsRight()), TradingSession.class);
					
						} else if(order.getType().equals("sell")) {
					
							Double leftCurrencyVal = (amountChange * brokerFeeFactor) * order.getRate();
							tradingSession.setFundsLeft(tradingSession.getFundsLeft() + leftCurrencyVal);
							mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
									new Update().set("fundsLeft", tradingSession.getFundsLeft()), TradingSession.class);
					
						}
				
						order.setFilledAmount(totalOrderAmount);
						
						if(order.getIsFilled() == true) {
							order.setRemains(0.0);
						} else {
							order.setRemains(order.getAmount() - totalOrderAmount);
						}
						
						changedOrders.add(order);
				
						Order reversedOrder = order.getReversedOrder();
				
						if(reversedOrder != null) {
							
							String profitSide = "left";
							
							Trade trade = new Trade();
							trade.setAmount(amountChange);
							
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
		
		Long userOrderUpdateTime = (System.currentTimeMillis() - userOrderUpdateStart) / 1000L;
		System.out.println("Orders update for "+user.getUsername()+" complete in "+userOrderUpdateTime+" s.");
		
	}
	
	protected String getService() {
		return "btce";
	}
	
	
}
