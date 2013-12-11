package com.lenin.tradingplatform;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.lenin.tradingplatform.client.BtceApi;
import com.lenin.tradingplatform.client.TradingClient;
import com.lenin.tradingplatform.data.entities.AutoTradingOptions;
import com.lenin.tradingplatform.data.entities.Order;
import com.lenin.tradingplatform.data.entities.TradingSession;
import com.lenin.tradingplatform.data.repositories.OrderRepository;
import com.lenin.tradingplatform.data.repositories.TradingSessionRepository;

public class AutoTrader {
	
	private TradingClient client;
	private TradingSession tradingSession;
	
	private MongoTemplate mongoTemplate;
	
	public AutoTrader(TradingClient client) {
		
		this.client = client;
		
		tradingSession = client.getTradingSession();
		
		mongoTemplate = client.getMongoTemplate();
		
	}
	
	
	public void autoTrade(Boolean newTrades) {
		
		AutoTradingOptions options = tradingSession.getAutoTradingOptions();
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		Boolean manualSettings = options.getManualSettings();
		if(manualSettings == false) {
			setTradeDuration(options.getAutoDuration());
			setTradeFrequency(options.getAutoFrequency());
		}
		
		Double highestSell = highestSell();
		if(highestSell == null) {
			highestSell = tradingSession.getOldRate();
		}
		
		Double lowestBuy = lowestBuy();
		if(lowestBuy == null) {
			lowestBuy = tradingSession.getOldRate();
		}
		
		List<Order> reversibleBuys = getReversibleBuys();
		List<Order> reversibleSells = getReversibleSells();
		
		for(Order reversibleBuy : reversibleBuys) {
			client.reverseTrade(reversibleBuy, false);
		}
		
		for(Order reversibleSell : reversibleSells) {
			client.reverseTrade(reversibleSell, false);
		}
		
		System.out.println("autotrading..."+reversibleBuys.size());
		
		if(newTrades == false) {
			return;
		}
		
		if(reversibleBuys.size() == 0 && reversibleSells.size() == 0) {
			
			Double sellThreshold = tradingSession.getRate().getSell()*(options.getSellThreshold()/100.0);
			Double buyThreshold = tradingSession.getRate().getBuy()*(options.getBuyThreshold()/100.0);
			
			Double sellRate = tradingSession.getRate().getSell();
			Double buyRate = tradingSession.getRate().getBuy();
			
			Double sellRateChange = sellRate - highestSell;
			Double buyRateChange = buyRate - lowestBuy;
			
			System.out.println("NEW SELL? "+sellRateChange +" >= "+sellThreshold+" (src = "+sellRate+" - "+highestSell+")");
			System.out.println("NEW BUY? "+buyRateChange +" <="+ -buyThreshold+" (brc = "+buyRate+" - "+lowestBuy+")");
			
			if(sellRateChange >= sellThreshold) {
				
				Double tradeChunk = options.getSellChunk();
				
				if(tradingSession.getFundsRight() >= tradeChunk && tradingSession.getRate().getSell() > options.getSellFloor()) {
				
					Order sellOrder = 
							BtceApi.createOrder(tradingSession.getCurrencyRight()+"_"+tradingSession.getCurrencyLeft(), 
									tradeChunk, client.actualTradeRate("sell"), "sell");
				
					sellOrder.setMode("auto");
					sellOrder.setSave(true);
					
					client.trade(sellOrder);
					
					tradingSession.setOldRate(tradingSession.getRate().getLast());
					
					mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
							new Update().set("oldRate", tradingSession.getOldRate()), TradingSession.class);
					
					//tradingSessionRepository.save(tradingSession);
				
				}
			
			} else if(buyRateChange <= -buyThreshold) {
				
				Double tradeChunk = options.getBuyChunk();
				
				if(tradingSession.getRate().getBuy() < options.getBuyCeiling() &&
						tradingSession.getFundsLeft() >= (tradeChunk * client.actualTradeRate("buy"))) {
			
					Order buyOrder = 
							BtceApi.createOrder(tradingSession.getCurrencyRight()+"_"+tradingSession.getCurrencyLeft(), 
									tradeChunk, client.actualTradeRate("buy"), "buy");
					
					buyOrder.setMode("auto");
					buyOrder.setSave(true);
				
					client.trade(buyOrder);
					tradingSession.setOldRate(tradingSession.getRate().getLast());
					
					mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
							new Update().set("oldRate", tradingSession.getOldRate()), TradingSession.class);
					
					//tradingSessionRepository.save(tradingSession);
				
				}
				
			}
			
		}
		
	}
	
	
	
	private void setTradeFrequency(Double value) {
		
		AutoTradingOptions options = tradingSession.getAutoTradingOptions();
		Double autoDuration = options.getAutoDuration();
		Double autoFrequency = options.getAutoFrequency();
		Boolean manualSettings = options.getManualSettings();
		
		if(value > 80) {
			if(autoDuration < 20) {
				value = 80 + (-0.05+(Math.random()*0.1));
			}
		}
		
		if(value < 20) {
			if(autoDuration > 80) {
				value = 20 + (-0.05+(Math.random()*0.1));
			}
		}
		
		Double threshold = 10-((value/100)*9);
		if((""+threshold).length() > 5) {
			threshold = new Double((threshold+"").substring(0, 5));
		}
		
		options.setAutoFrequency(value);
		
		if(manualSettings == false) {
			
			options.setBuyThreshold(threshold);
			options.setSellThreshold(threshold);
			
		}
		
	}
	
	private void setTradeDuration(Double value) {
		
		AutoTradingOptions options = tradingSession.getAutoTradingOptions();
		Double autoDuration = options.getAutoDuration();
		Double autoFrequency = options.getAutoFrequency();
		Boolean manualSettings = options.getManualSettings();
		Double fundsLeft = tradingSession.getFundsLeft();
		Double fundsRight = tradingSession.getFundsRight();
		Double sellThreshold = options.getSellThreshold();
		Double buyThreshold = options.getBuyThreshold();
		
		if(fundsLeft <= 0 || fundsRight <= 0) {
			return;
		}
		
		if(value > 80) {
			if(autoFrequency < 20) {
				setTradeFrequency(20.0);
			}
		}
		
		if(value < 20) {
			if(autoFrequency > 80) {
				setTradeFrequency(80.0);
			}
		}
		
		Double leftTotal = fundsLeft;
		Double rightTotal = fundsRight * tradingSession.getRate().getBuy();
		Double total = leftTotal + rightTotal;
		Double leftPart = leftTotal/total;
		Double rightPart = rightTotal/total;
		
		Double valueLeft = 10+(leftPart * value);
		Double valueRight = 10+(rightPart * value);
		
		Double sellCount = valueRight/sellThreshold;
		if(sellCount < 1) {
			sellCount = 1.0;
		}
		
		Double buyCount = valueLeft/buyThreshold;
		if(buyCount < 1) {
			buyCount = 1.0;
		}
		
		Double maxCount = buyCount > sellCount ? buyCount : sellCount;
		Double stepCount = (sellCount + buyCount)/2;
		
		Double sellChunk = fundsRight / stepCount;
		Double buyChunk = fundsLeft / (stepCount * tradingSession.getRate().getBuy());
		
		options.setAutoDuration(value);
		
		if(manualSettings == false) {
			
			options.setBuyChunk(buyChunk);
			options.setSellChunk(sellChunk);
		
		}
		
	};

	
	public Double lowestBuy() {
		
		Query orderQuery = new Query(Criteria.where("tradingSession").is(tradingSession).andOperator(
				Criteria.where("type").is("buy"),
				Criteria.where("mode").is("auto")
			));
		
		List<Order> orders = mongoTemplate.find(orderQuery, Order.class);
		
		//List<Order> orders = orderRepository.findByTradingSessionAndTypeAndMode(tradingSession, "buy", "auto");
		
		Double lowest = null;
		
		for(Order order : orders) {
			
			Double difference = Math.abs(order.getRate()-tradingSession.getRate().getSell());
			Double sellThreshold = tradingSession.getRate().getSell()*(tradingSession.getAutoTradingOptions().getBuyThreshold()/100.0);
			Boolean withinRange = difference < sellThreshold;
			
			if(withinRange || order.getIsFilled()) {
				if(lowest == null) {
					lowest = order.getRate();
				} else {
					if(order.getRate() < lowest) {
						lowest = order.getRate();
					}
				}
			}
			
		}
		
		return lowest;
		
	}
	
	
	public Double highestSell() {
		
		Query orderQuery = new Query(Criteria.where("tradingSession").is(tradingSession).andOperator(
				Criteria.where("type").is("sell"),
				Criteria.where("mode").is("auto")
			));
		
		List<Order> orders = mongoTemplate.find(orderQuery, Order.class);
		
		//List<Order> orders = orderRepository.findByTradingSessionAndTypeAndMode(tradingSession, "sell", "auto");
		
		Double highest = null;
		
		for(Order order : orders) {
			
			Double difference = Math.abs(order.getRate()-tradingSession.getRate().getBuy());
			Double buyThreshold = tradingSession.getRate().getBuy()*(tradingSession.getAutoTradingOptions().getSellThreshold()/100.0);
			Boolean withinRange = difference < buyThreshold;
			
			if(withinRange || order.getIsFilled()) {
				if(highest == null) {
					highest = order.getRate();
				} else {
					if(order.getRate() > highest) {
						highest = order.getRate();
					}
				}
			}
		}
		
		return highest;
		
	}
	
	
	private List<Order> getReversibleSells() {
   		
		Query orderQuery = new Query(Criteria.where("tradingSession").is(tradingSession).andOperator(
				Criteria.where("type").is("sell"),
				Criteria.where("mode").is("auto")
			));
		
		List<Order> orders = mongoTemplate.find(orderQuery, Order.class);
		//List<Order> orders = orderRepository.findByTradingSessionAndTypeAndMode(tradingSession, "sell", "auto");
		System.out.println(orders);
		
		Double calculatedBuyAmount = 0.0;
		
		List<Order> reversibleOrders = new ArrayList<Order>();
		
		for(Order order : orders) {
			
			Double rateVal = order.getRate();
			Double amountVal = order.getAmount();
			
			Double sellThreshold = order.getRate()*(tradingSession.getAutoTradingOptions().getSellThreshold()/100.0);
			System.out.println(tradingSession.getRate().getBuy()+" <= "+(rateVal - sellThreshold));
			
			Boolean isFilled = order.getIsFilled();
			
			if(isFilled && tradingSession.getRate().getBuy() <= (rateVal - sellThreshold)) {
					
				Double actualBuyRate = client.actualTradeRate("buy");
				
				Double newBuyAmount = 
					calculatedBuyAmount + amountVal;
				
				if(tradingSession.getFundsLeft() > (newBuyAmount * actualBuyRate)) {
					calculatedBuyAmount = newBuyAmount;
					reversibleOrders.add(order);
					System.out.println("buy "+amountVal+" for "+(amountVal * actualBuyRate));
				} else {
					System.out.println("OUT OF USD!");
					break;
				}
				
			}
				
		}
			
		return reversibleOrders;
			
	};
		
	
	private List<Order> getReversibleBuys() {
   		
		Query orderQuery = new Query(Criteria.where("tradingSession").is(tradingSession).andOperator(
				Criteria.where("type").is("buy"),
				Criteria.where("mode").is("auto")
			));
		
		List<Order> orders = mongoTemplate.find(orderQuery, Order.class);
		//List<Order> orders = orderRepository.findByTradingSessionAndTypeAndMode(tradingSession, "buy", "auto");
		System.out.println(orders);
		
		Double calculatedSellAmount = 0.0;
		
		List<Order> reversibleOrders = new ArrayList<Order>();
		
		for(Order order : orders) {
			
			Double rateVal = order.getRate();
			Double amountVal = order.getAmount();
			
			Boolean isFilled = order.getIsFilled();
			
			Double buyThreshold = order.getRate()*(tradingSession.getAutoTradingOptions().getBuyThreshold()/100.0);
			
			System.out.println(tradingSession.getRate().getSell() +" >= "+ (rateVal + buyThreshold));
					
			if(isFilled && tradingSession.getRate().getSell() >= (rateVal + buyThreshold)) {
				
				Double actualSellRate = client.actualTradeRate("sell");
				Double newSellAmount = calculatedSellAmount + amountVal;
				
				if(tradingSession.getFundsRight() > newSellAmount) {
					calculatedSellAmount = newSellAmount;
					reversibleOrders.add(order);
					System.out.println("sell "+amountVal+" for "+(amountVal * actualSellRate));
				} else {
					System.out.println("OUT OF LTC!");
					break;
				}
					
			}
				
			
		}
		
		return reversibleOrders;
		
	}
	
	

}
