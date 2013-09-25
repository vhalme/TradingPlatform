package com.lenin.tradingplatform;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.lenin.tradingplatform.client.BtceApi;
import com.lenin.tradingplatform.client.TradingClient;
import com.lenin.tradingplatform.data.entities.AccountFunds;
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
public class TradingProcess {
	
	Map<String, Rate> rateMap = new HashMap<String, Rate>();
	
	private Long rateUpdateCounter = 0L;
	private Long lastRateUpdateTime = 0L;
	
	private Long lastTradeTime = 0L;
	
	@Autowired
	private MongoTemplate mongoTemplate;
	
	@Autowired
	private RateRepository rateRepository;
	
	@Autowired
	private TradingSessionRepository tradingSessionRepository;
	
	@Autowired
	private TradeRepository tradeRepository;
	
	@Autowired
	private OrderRepository orderRepository;
	
	@Autowired
	private UserRepository userRepository;
	
	private Settings settings;
	
	
	public void init() {
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		List<Settings> settingsResult = mongoOps.findAll(Settings.class);
		
		if(settingsResult.size() > 0) {	
			
			settings = settingsResult.get(0);
			
			if(settings.getBtceApiKey() == null) {
				System.out.println("BTCE API key is not set. Exiting.");
				System.exit(2);
			}
		
		} else {
			
			settings = new Settings();
			mongoOps.save(settings);
			
			System.out.println("Created new settings entry. Supply BTCE API key and secret and start the process again.");
			System.exit(1);
			
		}
		
		
	}
	
	
	public void update() {
		
		try {
			
			long start = System.currentTimeMillis();
			
			System.out.println("Started trade updates "+(new Date()));
			
			System.out.println("Updating rates");
			updateRates();
		
			System.out.println("Updating trade history");
			updateTradeHistory();
			
			System.out.println("Updating orders");
			updateOrders();
		
			System.out.println("Updating trading sessions");
			updateTradingSessions();
			
			long finish = System.currentTimeMillis();
		
			long time = finish-start;
		
			System.out.println("["+(new Date())+"] Update pass complete in "+time/1000+" s");
		
		} catch(Exception e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	
	
	private void updateRates() throws Exception {
		
		Rate rateLtcUsd = getRate("ltc_usd");
		Rate rateBtcUsd = getRate("btc_usd");
		Rate rateLtcBtc = getRate("ltc_btc");
		
		List<Rate> rates = new ArrayList<Rate>();
		rates.add(rateLtcUsd);
		rates.add(rateBtcUsd);
		rates.add(rateLtcBtc);
		rateRepository.save(rates);
		
		lastRateUpdateTime = rateLtcUsd.getTime();
		
		rateMap = new HashMap<String, Rate>();
		rateMap.put("ltc_usd", rateLtcUsd);
		rateMap.put("btc_usd", rateBtcUsd);
		rateMap.put("ltc_btc", rateLtcBtc);
		
		
		if(rateUpdateCounter > 0) {
		
			if(rateUpdateCounter % 4 == 0) {
				createAverageRates("1min");
			}
		
			if(rateUpdateCounter % 40 == 0) {
				createAverageRates("10min");
			}
		
			if(rateUpdateCounter % 120 == 0) {
				createAverageRates("30min");
			}
		
			if(rateUpdateCounter % 960 == 0) {
				createAverageRates("4h");
			}
		
			if(rateUpdateCounter % 1440 == 0) {
				createAverageRates("6h");
				rateUpdateCounter = 0L;
			}
			
		}
		
		rateUpdateCounter++;
		
	}
	
	
	private void createAverageRates(String setType) {
		
		Rate avgLtcUsd = getAverageRate("ltc_usd", setType);
		Rate avgBtcUsd = getAverageRate("btc_usd", setType);
		Rate avgLtcBtc = getAverageRate("ltc_btc", setType);
		
		List<Rate> avgRates = new ArrayList<Rate>();
		avgRates.add(avgLtcUsd);
		avgRates.add(avgBtcUsd);
		avgRates.add(avgLtcBtc);
		
		rateRepository.save(avgRates);
		
	}
	
	
	private Rate getAverageRate(String pair, String setType) {
		
		Long period = 0L;
		
		if(setType.equals("1min")) {
			period = 60L;
		} else if(setType.equals("10min")) {
			period = 600L;
		} else if(setType.equals("30min")) {
			period = 1800L;
		} else if(setType.equals("4h")) {
			period = 14400L;
		} else if(setType.equals("6h")) {
			period = 21600L;
		}
		
		List<Rate> rates = rateRepository.findByPairAndTimeGreaterThanOrderByTimeAsc(pair, lastRateUpdateTime - period);
		
		Integer count = 0;
		Double totalLast = 0.0;
		Double totalBuy = 0.0;
		Double totalSell = 0.0;
		Double totalAverage = 0.0;
		
		Rate first = rates.get(0);
		Rate last = rates.get(rates.size()-1);
		Double open = first.getLast();
		Double close = last.getLast();
		Double high = first.getLast();
		Double low = first.getLast();
		
		for(Rate rate : rates) {
			
			if(rate.getLast() > high) {
				high = rate.getLast();
			}
			
			if(rate.getLast() < low) {
				low = rate.getLast();
			}
			
			totalLast += rate.getLast();
			totalBuy += rate.getBuy();
			totalSell += rate.getSell();
			totalAverage += rate.getAverage();
			
			count++;
		
		}
		
		Double avgLast = 0.0;
		Double avgBuy = 0.0;
		Double avgSell = 0.0;
		Double avgAverage = 0.0;
		
		if(count > 0) {
			avgLast = totalLast/count;
			avgBuy = totalBuy/count;
			avgSell = totalSell/count;
			avgAverage = totalAverage/count;
		}
		
		Rate avgRate = new Rate();
		avgRate.setSetType(setType);
		avgRate.setPair(pair);
		avgRate.setTime(lastRateUpdateTime);
		
		avgRate.setLast(avgLast);
		avgRate.setBuy(avgBuy);
		avgRate.setSell(avgSell);
		avgRate.setOpen(open);
		avgRate.setClose(close);
		avgRate.setHigh(high);
		avgRate.setLow(low);
		
		return avgRate;
		
	}
	
	
	private Rate getRate(String pair) {
		
		try {
			
			BtceApi btceApi = new BtceApi("", "");
			btceApi.setOrderFee(0.002);
			
			Rate rate = new Rate();
		
			JSONObject ratesResult = btceApi.getRates(pair);
			JSONObject rateJson = ratesResult.getJSONObject("ticker");
			
			rate.setSetType("15s");
			rate.setPair(pair);
			rate.setLast(rateJson.getDouble("last"));
			rate.setBuy(rateJson.getDouble("buy"));
			rate.setSell(rateJson.getDouble("sell"));
			rate.setHigh(rateJson.getDouble("high"));
			rate.setLow(rateJson.getDouble("low"));
			rate.setAverage(rateJson.getDouble("avg"));
			rate.setOpen(rate.getLast());
			rate.setClose(rate.getLast());
			rate.setVolume(rateJson.getDouble("vol"));
			rate.setCurrentVolume(rateJson.getDouble("vol_cur"));
			rate.setTime(rateJson.getLong("server_time"));
			
			return rate;
			
		} catch(Exception e) {
			
			e.printStackTrace();
			return null;
			
		}
    	
	}
	
	
	private void updateTradeHistory() throws Exception {
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		List<User> users = userRepository.findAll();
		
		for(User user : users) {
			
			AccountFunds account = user.getAccountFunds();
			
			PropertyMap servicePropertyMap = account.getServiceProperties().get("btce");
			Map<String, Object> serviceProperties = servicePropertyMap.getProperties();
			
			String accountKey = (String)serviceProperties.get("apiKey");
			String accountSecret = (String)serviceProperties.get("apiSecret");
			
			if(accountKey == null || accountKey.length() == 0) {
				continue;
			}
			
			Query searchTrades = new Query(Criteria.where("accountFunds").is(account));
			List<Trade> savedTrades = mongoOps.find(searchTrades, Trade.class);
			
			for(Trade trade : savedTrades) {
				if(trade.getLive() == true && trade.getTime() > lastTradeTime) {
					lastTradeTime = trade.getTime();
				}
			}
			
			BtceApi btceApi = new BtceApi(accountKey, accountSecret);
			btceApi.setOrderFee(0.002);
			
			if(user.getLive() == true) {
				
				JSONObject userInfoResult = null; //btceApi.getAccountInfo();
				JSONObject userInfo = null; //userInfoResult.getJSONObject("return");
				JSONObject userFunds = null; //userInfo.getJSONObject("funds");
				
				try {
					
					userInfoResult = btceApi.getAccountInfo();
					userInfo = userInfoResult.getJSONObject("return");
					userFunds = userInfo.getJSONObject("funds");
					
				} catch(Exception e) {
					e.printStackTrace();
					continue;
				}
				
				List<TradingSession> tradingSessions = user.getTradingSessions();
			
				for(TradingSession tradingSession : tradingSessions) {
				
					if(tradingSession.getService().equals("btce") && tradingSession.getLive() == true) {
					
						String currencyLeft = tradingSession.getCurrencyLeft();
						String currencyRight = tradingSession.getCurrencyRight();
					
						Double accountFundsLeft = userFunds.getDouble(currencyLeft);
						Double accountFundsRight = userFunds.getDouble(currencyRight);
						Double sessionFundsLeft = tradingSession.getFundsLeft();
						Double sessionFundsRight = tradingSession.getFundsRight();
					
						System.out.println("Session "+tradingSession.getPair()+": "+currencyLeft+"> "+accountFundsLeft+" - "+sessionFundsLeft);
						System.out.println("Session "+tradingSession.getPair()+": "+currencyRight+"> "+accountFundsRight+" - "+sessionFundsRight);
						userFunds.put(currencyLeft, accountFundsLeft - sessionFundsLeft);
						userFunds.put(currencyRight, accountFundsRight - sessionFundsRight);
					
					}
				
				}
			
				Double userFundsBtc = userFunds.getDouble("btc");
				Double userFundsLtc = userFunds.getDouble("ltc");
				Double userFundsUsd = userFunds.getDouble("usd");
			
				System.out.println("Final funds: "+userFundsBtc+"/"+userFundsLtc+"/"+userFundsUsd);
			
				Map<String, Double> userBtceFunds = account.getActiveFunds().get("btce");
				userBtceFunds.put("btc", userFundsBtc);
				userBtceFunds.put("ltc", userFundsLtc);
				userBtceFunds.put("usd", userFundsUsd);
			
				mongoOps.save(account);
			
			}
			
			JSONObject tradeListResult = btceApi.getTradeList(lastTradeTime+1);
			
			try {
			
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
					
						Trade trade = new Trade();
						trade.setService("btce");
						trade.setAccountFunds(account);
						trade.setLive(true);
						trade.setOrderId(orderId);
						trade.setPair(pair);
						trade.setAmount(amount);
						trade.setRate(rate);
						trade.setType(type);
						trade.setTime(time);
					
						trades.add(trade);
					
						if(time > lastTradeTime) {
							lastTradeTime = time;
						}
					
					}
				
					if(trades.size() > 0) {
						System.out.println("New trades: "+trades.size()+"; Last trade time: "+lastTradeTime);
						tradeRepository.save(trades);
					}
				
				} else {
				
					String error = tradeListResult.getString("error");
					if(!error.equals("no trades")) {
						BtceApi._nonce = System.currentTimeMillis() / 10000L;
						System.out.println("Trades update unsuccessful: "+error);
					}
				
				}
			
			} catch(Exception e) {
				e.printStackTrace();
			}
			
		}
		
		
	}
	
	
	private void updateOrders() throws Exception {
		
		Rate ltc_usd = rateMap.get("ltc_usd");
		Rate btc_usd = rateMap.get("btc_usd");
		Rate ltc_btc = rateMap.get("ltc_btc");
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		List<User> users = userRepository.findAll();
		
		//List<Order> orders = orderRepository.findAll();
		
		for(User user : users) {
			
			List<TradingSession> tradingSessions = user.getTradingSessions();
			
			for(TradingSession tradingSession : tradingSessions) {
			
				List<Order> orders = orderRepository.findByTradingSession(tradingSession);
				List<Order> changedOrders = new ArrayList<Order>();
				
				for(Order order : orders) {
			
					if(!order.getIsFilled() && order.getIsReversed() != true) {
				
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
					
							BtceApi btceApi = new BtceApi("", "");
							btceApi.setOrderFee(0.002);
					
							Double brokerFeeFactor = 1 - btceApi.getOrderFee();
							Double serviceFeeFactor = 1 - TradingClient.orderFee;
							Double totalFeeFactor = serviceFeeFactor * brokerFeeFactor;
					
							Double amountChange = totalOrderAmount-order.getFilledAmount();
					
							//TradingSession tradingSession = order.getTradingSession();
					
							if(order.getType().equals("buy")) {
						
								Double rightCurrencyVal = amountChange * brokerFeeFactor;
								tradingSession.setFundsRight(tradingSession.getFundsRight() + rightCurrencyVal);
						
							} else if(order.getType().equals("sell")) {
						
								Double leftCurrencyVal = (amountChange * brokerFeeFactor) * order.getRate();
								tradingSession.setFundsLeft(tradingSession.getFundsLeft() + leftCurrencyVal);
						
							}
					
							order.setFilledAmount(totalOrderAmount);
							changedOrders.add(order);
					
							Order reversedOrder = order.getReversedOrder();
					
							if(reversedOrder != null) {
								
								String profitSide = "left";
								
								reversedOrder.setFilledAmount(order.getFilledAmount());
								changedOrders.add(reversedOrder);
						
								Trade trade = new Trade();
								trade.setAmount(amountChange);
						
								Double profitLeft = tradingSession.getProfitLeft();
								Double tradeProfit = order.calcProfit(trade, brokerFeeFactor);
								tradingSession.setProfitLeft(profitLeft + tradeProfit);
								
								String orderMode = reversedOrder.getMode();
								if(orderMode != null && !orderMode.equals("manual")) {
									processProfit(user, tradeProfit, profitSide, tradingSession);
								}
								
								if(order.getIsFilled()) {
									changedOrders.remove(order);
									changedOrders.remove(reversedOrder);
									orderRepository.delete(order);
									orderRepository.delete(reversedOrder);
								}
						
							}
					
							tradingSessionRepository.save(tradingSession);
					
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
		
		
	}
	
	
	private void updateTradingSessions() throws Exception {
		
		List<User> users = userRepository.findAll();
		
		//System.out.println("Trade stats total: "+allTradingSession.size());
		
		for(User user : users) {
			
			List<TradingSession> tradingSessions = user.getTradingSessions();
			
			for(int i=0; i<tradingSessions.size(); i++) {
			
				TradingSession tradingSession = tradingSessions.get(i);
				//System.out.println(tradingSession.getId()+": "+tradingSession.getLive());
			
			
				if(tradingSession.getLive() == true) {
				
					Rate tickerQuote = rateMap.get(tradingSession.getPair());
					//System.out.println("ticker for "+tradingSession.getPair()+": "+tickerQuote.getLast());
				
					if(tickerQuote != null) {
						tradingSession.setRate(tickerQuote);
					}
				
				} else {
				
				
					Rate rate = tradingSession.getRate();
					rate.setTime(System.currentTimeMillis()/1000L);
				
					tradingSession.setRate(rate);
				
				}
			
			
				Boolean resetOldRate =
						( (tradingSession.getRate().getLast() - tradingSession.getOldRate() > tradingSession.getAutoTradingOptions().getBuyThreshold()) && 
							tradingSession.getRate().getLast() < tradingSession.getAutoTradingOptions().getBuyCeiling() ) ||
						( (tradingSession.getRate().getLast() - tradingSession.getOldRate() < - tradingSession.getAutoTradingOptions().getSellThreshold()) &&
							tradingSession.getRate().getLast() > tradingSession.getAutoTradingOptions().getSellFloor() );
			
				if(tradingSession.getOldRate() == 0.0 || resetOldRate) {
					tradingSession.setOldRate(tradingSession.getRate().getLast());
				}
			
				tradingSessionRepository.save(tradingSession);
			
				//System.out.println(tradingSession.getPair()+"("+tradingSession.getId()+"): "+tradingSession.getRate());
			
				if(tradingSession.getAutoTradingOptions().getTradeAuto() == true && tradingSession.getRate().getLast() != 0.0) {
				
					AccountFunds account = user.getAccountFunds();
					PropertyMap servicePropertyMap = account.getServiceProperties().get(tradingSession.getService());
					PropertyMap paymentPropertyMap = account.getServiceProperties().get("payment");
					Map<String, Object> serviceProperties = servicePropertyMap.getProperties();
					Map<String, Object> paymentProperties = paymentPropertyMap.getProperties();
					String accountKey = (String)serviceProperties.get("apiKey");
					String accountSecret = (String)serviceProperties.get("apiSecret");
					
					List<Map<String, Object>> periods = (List<Map<String, Object>>)paymentProperties.get("periods");
					Map<String, Object> currentPeriod = (Map<String, Object>)periods.get(1);
					
					String paymentMethod = (String)currentPeriod.get("method");
					String paymentCurrency = (String)currentPeriod.get("currency");
					
					Map<String, Map<String, Double>> serviceFees = settings.getServiceFees();
					Map<String, Double> monthlyFees = serviceFees.get("monthly");
					Map<String, Double> profitFees = serviceFees.get("profit");
					
					Boolean newTrades = false;
					
					Double currencyFunds = account.getReserves().get(paymentCurrency);
					BigDecimal bdCurrencyFunds = new BigDecimal(""+currencyFunds);
					
					BigDecimal bdRequiredFunds = new BigDecimal("0.0");
					if(paymentMethod != null && paymentMethod.length() > 0 && paymentCurrency != null && paymentCurrency.length() > 0) {
						
						if(paymentMethod.equals("monthly")) {
							bdRequiredFunds = new BigDecimal(""+monthlyFees.get(paymentCurrency));
						} else if(paymentMethod.equals("profit")) {
							bdRequiredFunds = new BigDecimal(""+profitFees.get(paymentCurrency));
						}
						
						BigDecimal bdMissingFunds = bdRequiredFunds.subtract(bdCurrencyFunds);
						Double missingFunds = bdMissingFunds.doubleValue();
						
						if(missingFunds <= 0) {
							newTrades = true;
						}
						
					}
					
					if(accountKey == null  || accountKey.length() == 0) {
						continue;
					}
					
					BtceApi btceApi = new BtceApi(accountKey, accountSecret);
					btceApi.setOrderFee(0.002);
				
					TradingClient tradingClient = new TradingClient(this, tradingSession, userRepository, tradingSessionRepository, orderRepository, tradeRepository);
					tradingClient.setBtceApi(btceApi);
				
					AutoTrader autoTrader = new AutoTrader(tradingClient);
					autoTrader.autoTrade(newTrades);
				
				}
				
			}
			
			
		}
		
		
	}
	
	
	public void processProfit(User user, Double tradeProfit, String profitSide, TradingSession tradingSession) {
		
		AccountFunds accountFunds = user.getAccountFunds();
		Map<String, Double> reserves = accountFunds.getReserves();
		
		Map<String, Object> paymentSettings = 
				accountFunds.getServiceProperties().get("payment").getProperties();
		
		List<Map<String, Object>> periods = (List<Map<String, Object>>)paymentSettings.get("periods");
		Map<String, Object> currentPeriod = periods.get(1);
		
		String paymentMethod = (String)currentPeriod.get("method");
		String paymentCurrency = (String)currentPeriod.get("currency");
		Map<String, String> periodProfits = (Map<String, String>)currentPeriod.get("sharedProfit");
		
		Double profitShare = tradeProfit * 0.1;
		
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
			
			Double currentFunds = reserves.get(paymentCurrency);
			BigDecimal bdCurrentFunds = new BigDecimal(""+currentFunds);
			
			bdCurrentFunds = bdCurrentFunds.subtract(new BigDecimal(""+profitShare));
			reserves.put(paymentCurrency, bdCurrentFunds.doubleValue());
			
			mongoTemplate.save(accountFunds);
		
		}
		
	}
	
	
	
}
