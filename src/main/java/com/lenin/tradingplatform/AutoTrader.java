package com.lenin.tradingplatform;

import java.util.ArrayList;
import java.util.List;

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
	
	private OrderRepository orderRepository;
	private TradingSessionRepository tradingSessionRepository;
	
	public AutoTrader(TradingClient client) {
		
		this.client = client;
		
		tradingSession = client.getTradingSession();
		
		orderRepository = client.getOrderRepository();
		tradingSessionRepository = client.getTradingSessionRepository();
		
	}
	
	
	public void autoTrade(Boolean newTrades) {
		
		AutoTradingOptions options = tradingSession.getAutoTradingOptions();
		
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
				
					tradingSessionRepository.save(tradingSession);
				
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
				
					tradingSessionRepository.save(tradingSession);
				
				}
				
			}
			
		}
		
	}
	
	
	


	
	public Double lowestBuy() {
		
		List<Order> orders = orderRepository.findByTradingSessionAndTypeAndMode(tradingSession, "buy", "auto");
		
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
		
		List<Order> orders = orderRepository.findByTradingSessionAndTypeAndMode(tradingSession, "sell", "auto");
		
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
   		
		List<Order> orders = orderRepository.findByTradingSessionAndTypeAndMode(tradingSession, "sell", "auto");
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
   		
		List<Order> orders = orderRepository.findByTradingSessionAndTypeAndMode(tradingSession, "buy", "auto");
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
