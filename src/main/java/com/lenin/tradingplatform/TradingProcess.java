package com.lenin.tradingplatform;

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
		
		for(Rate rate : rates) {
			
			totalLast += rate.getLast();
			totalBuy += rate.getBuy();
			totalSell += rate.getSell();
			
			count++;
		
		}
		
		Double avgLast = 0.0;
		Double avgBuy = 0.0;
		Double avgSell = 0.0;
		
		if(count > 0) {
			avgLast = totalLast/count;
			avgBuy = totalBuy/count;
			avgSell = totalSell/count;
		}
		
		Rate avgRate = new Rate();
		avgRate.setSetType(setType);
		avgRate.setPair(pair);
		avgRate.setTime(lastRateUpdateTime);
		
		avgRate.setLast(avgLast);
		avgRate.setBuy(avgBuy);
		avgRate.setSell(avgSell);
		
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
			
				JSONObject userInfoResult = btceApi.getAccountInfo();
				JSONObject userInfo = userInfoResult.getJSONObject("return");
				JSONObject userFunds = userInfo.getJSONObject("funds");
			
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
		
		List<Order> orders = orderRepository.findAll();
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
					
					TradingSession tradingSession = order.getTradingSession();
					
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
						
						reversedOrder.setFilledAmount(order.getFilledAmount());
						changedOrders.add(reversedOrder);
						
						Trade trade = new Trade();
						trade.setAmount(amountChange);
						
						tradingSession.setProfitLeft(tradingSession.getProfitLeft() + (order.calcProfit(trade, brokerFeeFactor)));
						
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
				trade.setOrderId(order.getOrderId());
				trade.setAmount(order.getBrokerAmount()-order.getFilledAmount());
				trade.setTime(unixTime);
				
				tradeRepository.save(trade);
				
			}
			
		}
		
		orderRepository.save(changedOrders);
		
		
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
					PropertyMap servicePropertyMap = account.getServiceProperties().get("btce");
					Map<String, Object> serviceProperties = servicePropertyMap.getProperties();
					String accountKey = (String)serviceProperties.get("apiKey");
					String accountSecret = (String)serviceProperties.get("apiSecret");
					
					BtceApi btceApi = new BtceApi(accountKey, accountSecret);
					btceApi.setOrderFee(0.002);
				
					TradingClient tradingClient = new TradingClient(tradingSession, tradingSessionRepository, orderRepository, tradeRepository);
					tradingClient.setBtceApi(btceApi);
				
					AutoTrader autoTrader = new AutoTrader(tradingClient);
					autoTrader.autoTrade();
				
				}
				
			}
			
			
		}
		
		
	}
	
	
	
}
