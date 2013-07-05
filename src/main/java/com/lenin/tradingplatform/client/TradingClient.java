package com.lenin.tradingplatform.client;

import java.util.Iterator;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.lenin.tradingplatform.client.BtceApi;
import com.lenin.tradingplatform.data.entities.Order;
import com.lenin.tradingplatform.data.entities.Trade;
import com.lenin.tradingplatform.data.entities.TradingSession;
import com.lenin.tradingplatform.data.repositories.OrderRepository;
import com.lenin.tradingplatform.data.repositories.TradeRepository;
import com.lenin.tradingplatform.data.repositories.TradingSessionRepository;

public class TradingClient {
	
	public static Double orderFee = 0.002;
	
	private TradingSession tradingSession;
	private OrderRepository orderRepository;
	private TradingSessionRepository tradingSessionRepository;
	private TradeRepository tradeRepository;
	
	private BtceApi btceApi;
	
	public TradingClient(TradingSession tradingSession, TradingSessionRepository tradingSessionRepository, 
			OrderRepository orderRepository,
			TradeRepository tradeRepository) {
		
		this.tradingSession = tradingSession;
		this.orderRepository = orderRepository;
		this.tradingSessionRepository = tradingSessionRepository;
		this.tradeRepository = tradeRepository;
		
	}
	
	
	public BtceApi getBtceApi() {
		return btceApi;
	}

	public void setBtceApi(BtceApi btceApi) {
		this.btceApi = btceApi;
	}


	public TradingSession getTradingSession() {
		return tradingSession;
	}


	public void setTradingSession(TradingSession tradingSession) {
		this.tradingSession = tradingSession;
	}


	public OrderRepository getOrderRepository() {
		return orderRepository;
	}


	public void setOrderRepository(OrderRepository orderRepository) {
		this.orderRepository = orderRepository;
	}


	public TradingSessionRepository getTradingSessionRepository() {
		return tradingSessionRepository;
	}


	public void setTradingSessionRepository(
			TradingSessionRepository tradingSessionRepository) {
		this.tradingSessionRepository = tradingSessionRepository;
	}


	public TradeRepository getTradeRepository() {
		return tradeRepository;
	}


	public void setTradeRepository(TradeRepository tradeRepository) {
		this.tradeRepository = tradeRepository;
	}



	public RequestResponse cancelOrder(Order order) {
		
		if(btceApi == null) {
			System.err.println("BtceApi undefined. Cannot execute cancelOrder command.");
			return null;
		}
		
		RequestResponse response = new RequestResponse();
		
		if(tradingSession.getLive()) {
			
			JSONObject cancelOrderResult = btceApi.cancelOrder(order);
		
			if(cancelOrderResult == null) {
				response.setSuccess(0);
				response.setMessage("Could not get order cancellation result.");
				return response;
			}
			
			try {
				
				Integer success = cancelOrderResult.getInt("success");
				response.setSuccess(success);
				
				if(success == 1) {
					
					if(order.getIsReversed() || order.getFilledAmount() == 0) {
						orderRepository.delete(order);
					} else {
						order.setBrokerAmount(order.getFilledAmount());
						order.setAmount(order.getFilledAmount());
						orderRepository.save(order);
					}
					
					System.out.println("Order cancelled successfully.");
					
				} else {
					
					System.out.println("Order cancellation request failed: "+success);
					Iterator<String> keys = cancelOrderResult.keys();
					
					while(keys.hasNext()) {
						String key = keys.next();
						System.out.println(key+" : "+cancelOrderResult.get(key));
					}
					
				}
				
			} catch(JSONException e) {
				
				e.printStackTrace();
				response.setSuccess(0);
				response.setMessage(e.getMessage());
				
			}
			
		} else {
			
			if(order.getIsReversed()) {
				orderRepository.delete(order);
			} else {
				order.setBrokerAmount(order.getFilledAmount());
				order.setAmount(order.getFilledAmount());
				orderRepository.save(order);
			}
			
			response.setSuccess(1);
			
		}
		
		return response;
		
	}
	
	public RequestResponse trade(Order order) {
		
		if(btceApi == null) {
			System.err.println("BtceApi undefined. Cannot execute trade command.");
			return null;
		}
		
		RequestResponse response = new RequestResponse();
		
		order.setTradingSession(tradingSession);
		order.setLive(tradingSession.getLive());
		
		Double feeFactor = 1-TradingClient.orderFee;
		order.setBrokerAmount(order.getAmount()*(feeFactor-0.001));
		
		if(tradingSession.getLive()) {
			
			JSONObject tradeResult = btceApi.trade(order, feeFactor);
			
			if(tradeResult == null) {
				response.setSuccess(0);
				response.setMessage("Could not get trade result.");
				return response;
			}
			
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
					
				}
				
			} catch(JSONException e) {
				
				e.printStackTrace();
				response.setSuccess(0);
				response.setMessage(e.getMessage());
				
			}
			
		} else {
			
			Long unixTime = System.currentTimeMillis() / 1000L;
			String orderId = ""+unixTime;
			
			order.setOrderId(orderId);
			order.setReceived(order.getBrokerAmount()); //*Math.random());
			order.setRemains(order.getBrokerAmount()-order.getReceived());
			
			if(order.getRemains() == 0) {
				order.setFilledAmount(order.getBrokerAmount());
			}
			
			Trade trade = new Trade();
			trade.setLive(false);
			trade.setOrderId(order.getOrderId());
			trade.setAmount(order.getReceived());
			trade.setTime(unixTime);
			
			tradeRepository.save(trade);
			
			executeOrder(order);
			
			response.setSuccess(1);
			
		}
		
		return response;
		
	}
	
	
	private void executeOrder(Order order) {
		
		Double brokerFeeFactor = 1-btceApi.getOrderFee();
		
		order.setFinalAmount(order.getBrokerAmount()*brokerFeeFactor);
		
		if(order.getType().equals("buy")) {
			
			Double usdVal = order.getAmount() * order.getRate();
			
			tradingSession.setFundsLeft(tradingSession.getFundsLeft() - usdVal);
			tradingSession.setFundsRight(tradingSession.getFundsRight() + order.getFinalAmount());
			
		} else if(order.getType().equals("sell")) {
			
			Double usdVal = order.getFinalAmount() * order.getRate();
			
			tradingSession.setFundsLeft(tradingSession.getFundsLeft() + usdVal);
			tradingSession.setFundsRight(tradingSession.getFundsRight() - order.getAmount());
			
		}
		
		Order reversedOrder = order.getReversedOrder();
		System.out.println("exec tx (reverse="+(reversedOrder != null)+", fill="+order.getFilledAmount()+"/broker="+order.getBrokerAmount()+")");
		
		if(reversedOrder != null) {
			
			Trade trade = new Trade();
			trade.setAmount(order.getFilledAmount());
			Double tradeRevenue = order.calcTradeRevenue(trade);
			
			tradingSession.setProfitLeft(tradingSession.getProfitLeft() + tradeRevenue);
			reversedOrder.setIsReversed(true);
			reversedOrder.setFilledAmount(order.getFilledAmount());
			
			if(order.getFilledAmount() < order.getBrokerAmount()) {
				orderRepository.save(reversedOrder);
				orderRepository.save(order);
			} else {
				orderRepository.delete(reversedOrder);
				//orderRepository.delete(order);
			}
			
		} else {
			
			if(order.getSave()) {
				orderRepository.save(order);
			}
			
		}
		
		tradingSessionRepository.save(tradingSession);
		
	}
	
	
	public Double actualTradeRate(String type) {
		
		if(type == "buy") {
			return tradingSession.getRate().getBuy() - tradingSession.getAutoTradingOptions().getRateBuffer();
		} else if(type == "sell") {
			return tradingSession.getRate().getSell() + tradingSession.getAutoTradingOptions().getRateBuffer();
		} else {
			return null;
		}
		
	}


	public RequestResponse reverseTrade(Order order, Boolean save) {
		
		System.out.println("Reverting order.");
		
		Order reverseOrder = createReverseOrder(order);
		reverseOrder.setSave(save);
		
		RequestResponse tradeResult = trade(reverseOrder);
		
		return tradeResult;
		
	};
	
	
	public Order createReverseOrder(Order order) {
		
		String reverseType = null;
		
		if(order.getType().equals("sell")) {
			reverseType = "buy";
		} else if(order.getType().equals("buy")) {
			reverseType = "sell";
		}
		
		//System.out.println("REVERSED TYPE TO "+reverseType);
		
		Order reverseOrder = 
			BtceApi.createOrder(tradingSession.getCurrencyRight()+"_"+tradingSession.getCurrencyLeft(), order.getAmount(), actualTradeRate(reverseType), reverseType);
		
		reverseOrder.setReversedOrder(order);
		
		return reverseOrder;
		
	}
	
	
}
