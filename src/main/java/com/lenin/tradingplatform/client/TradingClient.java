package com.lenin.tradingplatform.client;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;

import com.lenin.tradingplatform.TradingProcess;
import com.lenin.tradingplatform.client.BtceApi;
import com.lenin.tradingplatform.data.entities.AccountFunds;
import com.lenin.tradingplatform.data.entities.Order;
import com.lenin.tradingplatform.data.entities.Trade;
import com.lenin.tradingplatform.data.entities.TradingSession;
import com.lenin.tradingplatform.data.entities.User;
import com.lenin.tradingplatform.data.repositories.OrderRepository;
import com.lenin.tradingplatform.data.repositories.TradeRepository;
import com.lenin.tradingplatform.data.repositories.TradingSessionRepository;
import com.lenin.tradingplatform.data.repositories.UserRepository;

public class TradingClient {
	
	public static Double orderFee = 0.000;
	
	private TradingSession tradingSession;
	private UserRepository userRepository;
	private OrderRepository orderRepository;
	private TradingSessionRepository tradingSessionRepository;
	private TradeRepository tradeRepository;
	private TradingProcess tradingProcess;
	
	private BtceApi btceApi;
	
	public TradingClient(TradingProcess tradingProcess, TradingSession tradingSession, UserRepository userRepository, TradingSessionRepository tradingSessionRepository, 
			OrderRepository orderRepository,
			TradeRepository tradeRepository) {
		
		this.tradingProcess = tradingProcess;
		this.tradingSession = tradingSession;
		this.userRepository = userRepository;
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
		
		Double brokerFeeFactor = 1 - btceApi.getOrderFee();
		
		RequestResponse response = new RequestResponse();
		
		if(tradingSession.getLive()) {
			
			JSONObject cancelOrderResult = btceApi.cancelOrder(order);
		
			if(cancelOrderResult == null) {
				response.setSuccess(-3);
				response.setMessage("Could not get order cancellation result.");
				return response;
			}
			
			try {
				
				Integer success = cancelOrderResult.getInt("success");
				response.setSuccess(success);
				
				if(success == 1) {
					
					if(order.getIsReversed()) {
						
						Order reverseOrder = orderRepository.findByReversedOrder(order);
						compensateCancelledOrder(reverseOrder, tradingSession);
							
						orderRepository.delete(reverseOrder);
						orderRepository.delete(order);
					
					} else {
						
						compensateCancelledOrder(order, tradingSession);
						
						if(order.getFilledAmount() > 0) {
							Double fillRatio = order.getFilledAmount()/order.getBrokerAmount();
							order.setBrokerAmount(order.getFilledAmount());
							order.setFinalAmount(order.getBrokerAmount()*brokerFeeFactor);
							order.setAmount(order.getAmount()*fillRatio);
							orderRepository.save(order);
						} else {
							orderRepository.delete(order);
						}
						
					}
					
					tradingSessionRepository.save(tradingSession);
					
					System.out.println("Order cancelled successfully.");
					
				} else {
					
					System.out.println("Order cancellation request failed: "+success);
					Iterator<String> keys = cancelOrderResult.keys();
					
					while(keys.hasNext()) {
						String key = keys.next();
						System.out.println(key+" : "+cancelOrderResult.get(key));
					}
					
					response.setMessage("Cancel order was unsuccessful.");
					response.setSuccess(-4);
					
					
				}
				
			} catch(JSONException e) {
				
				e.printStackTrace();
				response.setSuccess(-5);
				response.setMessage(e.getMessage());
				
			}
			
		} else {
			
			if(order.getIsReversed()) {
				
				Order reverseOrder = orderRepository.findByReversedOrder(order);
				compensateCancelledOrder(reverseOrder, tradingSession);
					
				orderRepository.delete(reverseOrder);
				orderRepository.delete(order);
			
			} else {
				
				compensateCancelledOrder(order, tradingSession);
				
				if(order.getFilledAmount() > 0) {
					Double fillRatio = order.getFilledAmount()/order.getBrokerAmount();
					order.setBrokerAmount(order.getFilledAmount());
					order.setFinalAmount(order.getBrokerAmount()*brokerFeeFactor);
					order.setAmount(order.getAmount()*fillRatio);
					orderRepository.save(order);
				} else {
					orderRepository.delete(order);
				}
				
			}
			
			tradingSessionRepository.save(tradingSession);
			
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
		
		Double serviceFeeFactor = 1-TradingClient.orderFee;
		order.setBrokerAmount(order.getAmount()*serviceFeeFactor);
		
		if(tradingSession.getLive()) {
			
			JSONObject tradeResult = btceApi.trade(order, serviceFeeFactor);
			
			if(tradeResult == null) {
				response.setSuccess(-3);
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
					
					String errorMessage = tradeResult.getString("error");
					
					response.setMessage("Order was unsuccessful: "+errorMessage);
					response.setSuccess(-4);
					
				}
				
			} catch(JSONException e) {
				
				e.printStackTrace();
				response.setSuccess(-5);
				response.setMessage(e.getMessage());
				
			}
			
		} else {
			
			Double random = Math.random();
			String orderId = ""+random;
			
			order.setOrderId(orderId);
			order.setReceived(order.getBrokerAmount()*Math.random());
			//order.setReceived(order.getBrokerAmount());
			order.setRemains(order.getBrokerAmount()-order.getReceived());
			
			if(order.getRemains() == 0) {
				order.setFilledAmount(order.getBrokerAmount());
			} else if(order.getReceived() > 0) {
				order.setFilledAmount(order.getReceived());
			}
			
			/*
			Trade trade = new Trade();
			trade.setLive(false);
			trade.setOrderId(order.getOrderId());
			trade.setAmount(order.getReceived());
			trade.setTime(unixTime);
			
			tradeRepository.save(trade);
			*/
			
			executeOrder(order);
			
			response.setSuccess(1);
			
		}
		
		return response;
		
	}
	
	
	private void executeOrder(Order order) {
		
		Double brokerFeeFactor = 1-btceApi.getOrderFee();
		Double serviceFeeFactor = 1-TradingClient.orderFee;
		
		Double totalFeeFactor = serviceFeeFactor * brokerFeeFactor;
		
		order.setFinalAmount(order.getBrokerAmount() * brokerFeeFactor);
		
		if(order.getType().equals("buy")) {
			
			Double leftCurrencyVal = order.getAmount() * order.getRate();
			Double rightCurrencyVal = order.getFilledAmount() * brokerFeeFactor;
			
			tradingSession.setFundsLeft(tradingSession.getFundsLeft() - leftCurrencyVal);
			tradingSession.setFundsRight(tradingSession.getFundsRight() + rightCurrencyVal);
			
		} else if(order.getType().equals("sell")) {
			
			Double leftCurrencyVal = (order.getFilledAmount() * brokerFeeFactor) * order.getRate();
			Double rightCurrencyVal = order.getAmount();
			
			tradingSession.setFundsLeft(tradingSession.getFundsLeft() + leftCurrencyVal);
			tradingSession.setFundsRight(tradingSession.getFundsRight() - rightCurrencyVal);
			
		}


		Order reversedOrder = order.getReversedOrder();
		System.out.println("exec tx (reverse="+(reversedOrder != null)+", fill="+order.getFilledAmount()+"/broker="+order.getBrokerAmount()+")");
		
		if(reversedOrder != null) {
			
			Trade trade = new Trade();
			trade.setAmount(order.getFilledAmount());
			Double tradeProfit = order.calcProfit(trade, brokerFeeFactor);
			tradingSession.setProfitLeft(tradingSession.getProfitLeft() + tradeProfit);
			
			User user = userRepository.findByUsername(tradingSession.getUsername());
			
			if(tradingProcess != null) {
				
				String orderMode = order.getMode();
				if(orderMode != null && !orderMode.equals("manual") && user.getLive() == true) {
					tradingProcess.processProfit(user, tradeProfit, "left", tradingSession);
				}
				
			}
			
			reversedOrder.setIsReversed(true);
			reversedOrder.setFilledAmount(order.getFilledAmount());
			
			if(!order.getIsFilled()) {
				orderRepository.save(reversedOrder);
				orderRepository.save(order);
			} else {
				orderRepository.delete(reversedOrder);
				//orderRepository.delete(order);
			}
			
		} else {
			
			orderRepository.save(order);
			
			/*
			if(order.getSave()) {
				
			}
			*/
			
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
		reverseOrder.setMode(order.getMode());
		
		return reverseOrder;
		
	}
	

	private void compensateCancelledOrder(Order order, TradingSession tradingSession) {
		
		Double brokerFeeFactor = 1 - btceApi.getOrderFee();
		
		if(order.getType().equals("buy")) {
			
			Double leftReturnUnits = (order.getBrokerAmount() - order.getFilledAmount()) * brokerFeeFactor;
			Double leftCurrencyReturn = leftReturnUnits * order.getRate();
			
			tradingSession.setFundsLeft(tradingSession.getFundsLeft() + leftCurrencyReturn);
			
		} else if(order.getType().equals("sell")) {
			
			Double rightCurrencyReturn = (order.getBrokerAmount() - order.getFilledAmount()) * brokerFeeFactor;
			
			tradingSession.setFundsRight(tradingSession.getFundsRight() + rightCurrencyReturn);
			
		}
		
	}
	
	
}
