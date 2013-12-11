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


public abstract class TradingClient {
	
	public static Double orderFee = 0.000;
	
	protected TradingSession tradingSession;
	protected MongoTemplate mongoTemplate;
	
	protected ExchangeApi exchangeApi;
	
	public TradingClient(TradingSession tradingSession,
			MongoTemplate mongoTemplate) {
		
		this.tradingSession = tradingSession;
		this.mongoTemplate = mongoTemplate;
		
	}
	
	
	public ExchangeApi getExchangeApi() {
		return exchangeApi;
	}

	public void setExchangeApi(ExchangeApi exchangeApi) {
		this.exchangeApi = exchangeApi;
	}

	public TradingSession getTradingSession() {
		return tradingSession;
	}


	public void setTradingSession(TradingSession tradingSession) {
		this.tradingSession = tradingSession;
	}

	
	public MongoTemplate getMongoTemplate() {
		return mongoTemplate;
	}


	public void setMongoTemplate(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	
	public RequestResponse cancelOrder(Order order) {
		
		Double brokerFeeFactor = 1 - exchangeApi.getOrderFee();
		
		RequestResponse response = new RequestResponse();
			
		Integer cancelledOrdersNum = 0;
		List<Order> reverseOrders = null;
		JSONObject cancelOrderResult = null;
			
		Boolean cancelFailed = false;
			
		if(order.getIsReversed()) {
				
			Query orderQuery = new Query(Criteria.where("reversedOrder").is(order));
			reverseOrders = mongoTemplate.find(orderQuery, Order.class);
			//reverseOrders = orderRepository.findByReversedOrder(order);
				
			if(reverseOrders.size() == 0) {
					
				mongoTemplate.remove(order);
				response.setSuccess(1);
				return response;
				
			} else {
					
				for(Order reverseOrder : reverseOrders) {
						
					if(reverseOrder.getIsFilled() == false) {
							
						cancelOrderResult = exchangeApi.cancelOrder(reverseOrder);
							
						if(cancelOrderResult != null) {
							compensateCancelledOrder(reverseOrder, tradingSession);
							mongoTemplate.remove(reverseOrder);
							cancelledOrdersNum++;
						} else {
							cancelFailed = true;
						}
						
					} else {
							
						mongoTemplate.remove(reverseOrder);
						
					}
						
				}
					
			}
				
		} else {
				
			cancelOrderResult = exchangeApi.cancelOrder(order);
				
		}
			
		if(cancelFailed) {
				
			response.setSuccess(-3);
			response.setMessage("Could not get order cancellation result.");
			return response;
			
		} else {
				
			if((reverseOrders != null && reverseOrders.size() > 0) && cancelledOrdersNum == 0) {
				
				mongoTemplate.remove(order);
				response.setSuccess(1);
				return response;
					
			}
				
		}
			
		try {
				
			response = processCancellationResult(cancelOrderResult, order);
				
			if(response.getSuccess() == 1) {
					
				if(reverseOrders != null && reverseOrders.size() > 0) {
						
					mongoTemplate.remove(order);
					
				} else {
						
					compensateCancelledOrder(order, tradingSession);
						
					if(order.getFilledAmount() > 0) {
						Double fillRatio = order.getFilledAmount()/order.getBrokerAmount();
						order.setBrokerAmount(order.getFilledAmount());
						order.setFinalAmount(order.getBrokerAmount()*brokerFeeFactor);
						order.setAmount(order.getAmount()*fillRatio);
						mongoTemplate.save(order);
					} else {
						mongoTemplate.remove(order);
					}
						
				}
					
				System.out.println("Order cancelled successfully.");
					
			} else {
					
				System.out.println("Order cancellation request failed");
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
		
		return response;
		
	}
	
	
	protected abstract RequestResponse processCancellationResult(JSONObject cancelOrderResult, Order order);
	
	
	public RequestResponse trade(Order order) {
		
		RequestResponse response = new RequestResponse();
		
		order.setTradingSession(tradingSession);
		order.setLive(tradingSession.getLive());
		
		Double serviceFeeFactor = 1-MtgoxTradingClient.orderFee;
		order.setBrokerAmount(order.getAmount()*serviceFeeFactor);
			
		JSONObject tradeResult = exchangeApi.trade(order, serviceFeeFactor);
				
		if(tradeResult == null) {
			response.setSuccess(-3);
			response.setMessage("Could not get trade result.");
			return response;
		}
				
		response = processTradeResult(tradeResult, order);
		
		return response;
		
	}
	
	
	protected abstract RequestResponse processTradeResult(JSONObject tradeResult, Order order);
	
	
	protected void executeOrder(Order order) {
		
		
		Double brokerFeeFactor = 1.0 - 0.002;
		
		if(exchangeApi != null) {
			brokerFeeFactor = 1-exchangeApi.getOrderFee();
		}
		
		Double serviceFeeFactor = 1-TradingClient.orderFee;
		
		order.setFinalAmount(order.getBrokerAmount() * brokerFeeFactor);
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		if(order.getType().equals("buy")) {
			
			Double leftCurrencyVal = order.getAmount() * order.getRate();
			Double rightCurrencyVal = order.getFilledAmount() * brokerFeeFactor;
			
			tradingSession.setFundsLeft(tradingSession.getFundsLeft() - leftCurrencyVal);
			tradingSession.setFundsRight(tradingSession.getFundsRight() + rightCurrencyVal);
			
			mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
					new Update().set("fundsLeft", tradingSession.getFundsLeft()), TradingSession.class);
			mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
					new Update().set("fundsRight", tradingSession.getFundsRight()), TradingSession.class);
			
		} else if(order.getType().equals("sell")) {
			
			Double leftCurrencyVal = (order.getFilledAmount() * brokerFeeFactor) * order.getRate();
			Double rightCurrencyVal = order.getAmount();
			
			tradingSession.setFundsLeft(tradingSession.getFundsLeft() + leftCurrencyVal);
			tradingSession.setFundsRight(tradingSession.getFundsRight() - rightCurrencyVal);
			
			mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
					new Update().set("fundsLeft", tradingSession.getFundsLeft()), TradingSession.class);
			mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
					new Update().set("fundsRight", tradingSession.getFundsRight()), TradingSession.class);
			
			
		}


		Order reversedOrder = order.getReversedOrder();
		System.out.println("exec tx "+order.getOrderId()+" (reverse="+(reversedOrder != null)+", fill="+order.getFilledAmount()+"/broker="+order.getBrokerAmount()+") @ "+order.getRate());
		
		if(reversedOrder != null) {
			
			Trade trade = new Trade();
			trade.setAmount(order.getFilledAmount());
			Double tradeProfit = order.calcProfit(trade, brokerFeeFactor);
			tradingSession.setProfitLeft(tradingSession.getProfitLeft() + tradeProfit);
			mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
					new Update().set("profitLeft", tradingSession.getProfitLeft()), TradingSession.class);
			
			Query userQuery = new Query(Criteria.where("username").is(tradingSession.getUsername()));
			List<User> usersResult = mongoTemplate.find(userQuery, User.class);
			User user = usersResult.get(0);
			
			String orderMode = order.getMode();
			if(orderMode != null && !orderMode.equals("manual") && user.getLive() == true) {
				TradingClient.processProfit(user, tradeProfit, "left", tradingSession, mongoTemplate);
			}
			
			reversedOrder.setIsReversed(true);
			reversedOrder.setFilledAmount(order.getFilledAmount());
			
			System.out.println("order "+order.getOrderId()+" reverts order "+reversedOrder.getOrderId()+" ("+reversedOrder.getAmount()+" @ "+reversedOrder.getRate()+")");
			
			//if(!order.getIsFilled()) {
				mongoTemplate.save(reversedOrder);
				mongoTemplate.save(order);
			//} else {
				//mongoTemplate.remove(reversedOrder);
				//mongoTemplate.remove(order);
			//}
			
		} else {
			
			mongoTemplate.save(order);
			
			/*
			if(order.getSave()) {
				
			}
			*/
			
		}
		
		//tradingSessionRepository.save(tradingSession);
		
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
	
	
	public RequestResponse partialReverseTrade(Order order, Double amount, Boolean save) {
		
		System.out.println("Reverting order partially ("+amount+"/"+order.getAmount()+").");
		
		Order reverseOrder = createReverseOrder(order);
		reverseOrder.setAmount(amount);
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
	

	protected void compensateCancelledOrder(Order order, TradingSession tradingSession) {
		
		Double brokerFeeFactor = 1.0 - 0.002;
		
		if(exchangeApi != null) {
			brokerFeeFactor = 1 - exchangeApi.getOrderFee();
		}
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		if(order.getType().equals("buy")) {
			
			Double leftReturnUnits = (order.getBrokerAmount() - order.getFilledAmount()) * brokerFeeFactor;
			Double leftCurrencyReturn = leftReturnUnits * order.getRate();
			
			tradingSession.setFundsLeft(tradingSession.getFundsLeft() + leftCurrencyReturn);
			mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
					new Update().set("fundsLeft", tradingSession.getFundsLeft()), TradingSession.class);
			
			
		} else if(order.getType().equals("sell")) {
			
			Double rightCurrencyReturn = (order.getBrokerAmount() - order.getFilledAmount()) * brokerFeeFactor;
			
			tradingSession.setFundsRight(tradingSession.getFundsRight() + rightCurrencyReturn);
			mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
					new Update().set("fundsRight", tradingSession.getFundsRight()), TradingSession.class);
			
		}
		
	}
	
	
	public static void processProfit(User user, Double tradeProfit, String profitSide, TradingSession tradingSession, MongoTemplate mongoTemplate) {
		
		Map<String, Rate> rateMap = TradingClient.createRateMap(tradingSession.getService(), mongoTemplate);
		
		List<Settings> settingsResult = mongoTemplate.findAll(Settings.class);
		Settings settings = settingsResult.get(0);
		
		Map<String, Map<String, Double>> serviceFees = settings.getServiceFees();
		Map<String, Double> profitSharingFees = serviceFees.get("profit");
		Double share = profitSharingFees.get("share");
		
		AccountFunds accountFunds = user.getAccountFunds();
		Map<String, Double> reserves = accountFunds.getReserves();
		
		Map<String, Object> paymentSettings = 
				accountFunds.getServiceProperties().get("payment").getProperties();
		
		List<Map<String, Object>> periods = (List<Map<String, Object>>)paymentSettings.get("periods");
		Map<String, Object> currentPeriod = periods.get(1);
		
		String paymentMethod = (String)currentPeriod.get("method");
		String paymentCurrency = (String)currentPeriod.get("currency");
		Map<String, String> periodProfits = (Map<String, String>)currentPeriod.get("sharedProfit");
		
		Double profitShare = tradeProfit * share;
		
		if(paymentMethod.equals("profit")) {
			
			String profitCurrency = "";
			if(profitSide.equals("left")) {
				profitCurrency = tradingSession.getCurrencyLeft();
			} else {
				profitCurrency = tradingSession.getCurrencyRight();
			}
			
			if(!profitCurrency.equals(paymentCurrency)) {
				
				String exchangePair = null;
				Double exchangeFactor = 1.0;
				
				if(profitSide.equals("left")) {
					exchangePair = paymentCurrency+"_"+profitCurrency;
					Double rate = rateMap.get(exchangePair).getLast();
					exchangeFactor = 1.0/rate;
				} else {
					exchangePair = profitCurrency+"_"+paymentCurrency;
					Double rate = rateMap.get(exchangePair).getLast();
					exchangeFactor = rate;
				}
				
				profitShare = profitShare * exchangeFactor;
				
			}
			
			BigDecimal bdProfitShare = new BigDecimal(""+profitShare);
			BigDecimal bdExistingProfits = new BigDecimal(periodProfits.get(paymentCurrency));
			bdExistingProfits = bdExistingProfits.add(bdProfitShare);
			periodProfits.put(paymentCurrency, bdExistingProfits.toString());
			mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(accountFunds.getId()))),
					new Update().set("serviceProperties.paymentSettings.periods.current.sharedProfit."+paymentCurrency, 
							bdExistingProfits.toString()), AccountFunds.class);
			
			Double currentFunds = reserves.get(paymentCurrency);
			BigDecimal bdCurrentFunds = new BigDecimal(""+currentFunds);
			
			bdCurrentFunds = bdCurrentFunds.subtract(new BigDecimal(""+profitShare));
			reserves.put(paymentCurrency, bdCurrentFunds.doubleValue());
			mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(accountFunds.getId()))),
					new Update().set("reserves."+paymentCurrency, 
							bdCurrentFunds.doubleValue()), AccountFunds.class);
			
			Map<String, Double> totalProfits = settings.getTotalProfits();
			if(totalProfits == null) {
				totalProfits = new HashMap<String, Double>();
			}
			
			Double totalProfit = totalProfits.get(profitCurrency);
			if(totalProfit == null) {
				totalProfit = 0.0;
			}
			
			BigDecimal bdTotalProfit = new BigDecimal(totalProfit);
			bdTotalProfit = bdTotalProfit.add(new BigDecimal(tradeProfit));
			totalProfits.put(profitCurrency, bdTotalProfit.doubleValue());
			
			settings.setTotalProfits(totalProfits);
			
			//mongoTemplate.save(accountFunds);
			
			mongoTemplate.updateFirst(new Query(), new Update().set("totalProfits", totalProfits), Settings.class);
			//mongoTemplate.save(settings);
			
			
		}
		
	}

	
	public static Map<String, Rate> createRateMap(String service, MongoTemplate mongoTemplate) {
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		Query searchRates = new Query(Criteria.where("setType").is("15s").andOperator(
					Criteria.where("service").is(service)
				)).with(new Sort(Direction.DESC, "time")).limit(10);
		
		List<Rate> rates = mongoOps.find(searchRates, Rate.class);
		
		Map<String, Rate> rateMap = new HashMap<String, Rate>();
		for(Rate rate : rates) {
			rateMap.put(rate.getPair(), rate);
			System.out.println("Last rate: "+rate.getPair()+"="+rate.getLast()+", "+rate.getMovingAverages().size());
			if(service.equals("btce")) {
				if(rateMap.get("ltc_usd") != null && rateMap.get("ltc_btc") != null && rateMap.get("btc_usd") != null) {
					break;
				}
			} else {
				break;
			}
		}
		
		return rateMap;
		
	}
	
}
