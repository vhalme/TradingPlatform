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
import com.lenin.tradingplatform.client.BtceTradingClient;
import com.lenin.tradingplatform.client.ExchangeApi;
import com.lenin.tradingplatform.client.MtgoxTradingClient;
import com.lenin.tradingplatform.client.TestTradingClient;
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
public class TradingSessionProcess extends ExchangeApiProcess {
	
	private Map<String, Map<String, Rate>> rateMaps = new HashMap<String, Map<String, Rate>>();
	
	@Autowired
	private TradingSessionRepository tradingSessionRepository;
	
	@Autowired
	private TradeRepository tradeRepository;
	
	@Autowired
	private OrderRepository orderRepository;
	
	@Autowired
	private RateRepository rateRepository;
	
	@Autowired
	private UserRepository userRepository;
	
	private Settings settings;
	
	
	public void init() {
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		List<Settings> settingsResult = mongoOps.findAll(Settings.class);
		
		if(settingsResult.size() > 0) {	
			
			settings = settingsResult.get(0);
			
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
			
			Map<String, Rate> rateMapBtce = TradingClient.createRateMap("btce", mongoTemplate);
			Map<String, Rate> rateMapMtgox = TradingClient.createRateMap("mtgox", mongoTemplate);
			rateMaps.put("btce", rateMapBtce);
			rateMaps.put("mtgox", rateMapMtgox);
			
			System.out.println("Updating trading sessions");
			updateUsers();
			
			long finish = System.currentTimeMillis();
		
			long time = finish-start;
		
			System.out.println("["+(new Date())+"] Update pass complete in "+time/1000+" s");
		
		} catch(Exception e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	
	private BtceApi createBtceApi(User user, MongoOperations mongoOps) {
		
		AccountFunds account = user.getAccountFunds();
		
		PropertyMap servicePropertyMap = account.getServiceProperties().get("btce");
		Map<String, Object> serviceProperties = servicePropertyMap.getProperties();
		
		String accountKey = (String)serviceProperties.get("apiKey");
		String accountSecret = (String)serviceProperties.get("apiSecret");
		
		BtceApi btceApi = new BtceApi(accountKey, accountSecret, mongoOps);
		btceApi.setOrderFee(0.002);
		
		return btceApi;
		
	}
	
	
	private void updateUsers() throws Exception {
		
		Long tradeUpdateStart = System.currentTimeMillis();
		
		List<User> users = userRepository.findAll();
		
		for(User user : users) {
			
			Long userUpdateStart = System.currentTimeMillis();
			
			System.out.println("Updating trading sessions for "+user.getUsername());
			
			if(user.getDeleted() == true) {
				continue;
			}
			
			updateTradingSessions(user);
			
			Long userUpdateTime = (System.currentTimeMillis() - userUpdateStart)/1000L;
			System.out.println("Trading sessions update for "+user.getUsername()+" completed in "+userUpdateTime+" s.");
			
		}
		
		Long tradeUpdateTime = (System.currentTimeMillis() - tradeUpdateStart)/1000L;
		System.out.println("Update trading sessions cycle completed in "+tradeUpdateTime+" s.");
		
	}
	
	
	private void updateTradingSessions(User user) {
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		System.out.println("Updating tradingSessions for "+user.getUsername());
		
		Long sessionsUpdateStart = System.currentTimeMillis();
		
		List<TradingSession> tradingSessions = user.getTradingSessions();
		
		for(int i=0; i<tradingSessions.size(); i++) {
		
			TradingSession tradingSession = tradingSessions.get(i);
			//System.out.println(tradingSession.getId()+": "+tradingSession.getLive());
			
			String maLong = tradingSession.getAutoTradingOptions().getMaLong();
			String maShort = tradingSession.getAutoTradingOptions().getMaShort();
			
			if(maLong == null) {
				maLong = "ema1h";
			}
			
			if(maShort == null) {
				maShort = "ema4h";
			}
			
			Long fromTime = (System.currentTimeMillis() / 1000) - (2 * 60);
			
			Query searchRates = new Query(Criteria.where("pair").is(tradingSession.getPair()).andOperator(
					Criteria.where("service").is(tradingSession.getService()),
					Criteria.where("time").gt(fromTime)
				)).with(new Sort(Direction.ASC, "time"));
			
			List<Rate> rates = mongoOps.find(searchRates, Rate.class);
		
			//List<Rate> rates = rateRepository.findByPairAndTimeGreaterThanOrderByTimeAsc(tradingSession.getPair(), fromTime);
			
			System.out.println("Going through "+rates.size()+" rates");
			Map<String, Double> lastMovingAverages = null;
			for(Rate rate : rates) {
				Map<String, Double> movingAverages = rate.getMovingAverages();
				if(movingAverages.get(maShort) != null) {
					lastMovingAverages = movingAverages;
				}
			}
			
			if(tradingSession.getLive() == true) {
				
				Rate tickerQuote = rateMaps.get(tradingSession.getService()).get(tradingSession.getPair());
				
				if(tickerQuote != null) {
					
					if(lastMovingAverages != null) {
						tickerQuote.setMovingAverages(lastMovingAverages);
					}
					
					tradingSession.setRate(tickerQuote);
					
					Map<String, Boolean> tradeOk = user.getTradeOk();
					Boolean serviceTradeOk = tradeOk.get(tradingSession.getService());
					
					tradingSession.setTradeOk(serviceTradeOk);
					
					mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
							new Update().set("rate", tickerQuote), TradingSession.class);
					
					mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
							new Update().set("tradeOk", serviceTradeOk), TradingSession.class);
					
				}
			
			} else {
				
				Rate rate = tradingSession.getRate();
				rate.setTime(System.currentTimeMillis()/1000L);
				
				tradingSession.setRate(rate);
				mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
						new Update().set("rate", rate), TradingSession.class);
				
			}
			
			
			if(tradingSession.getPreviousRate() == null || tradingSession.getPreviousRate().getMovingAverages() == null ||
					(tradingSession.getLive() == true && tradingSession.getPreviousRate().getMovingAverages().get(maShort) == null)) {
				
				tradingSession.setPreviousRate(tradingSession.getRate());
				mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
						new Update().set("previousRate", tradingSession.getRate()), TradingSession.class);
			}
			
			if(tradingSession.getLastMaTransactionRate() == 0) {
				tradingSession.setLastMaTransactionRate(tradingSession.getRate().getLast());
				mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
						new Update().set("lastMaTransactionRate", tradingSession.getLastMaTransactionRate()), TradingSession.class);
			}
			
			AutoTradingOptions options = tradingSession.getAutoTradingOptions();
			Double sellThreshold = tradingSession.getRate().getSell()*(options.getSellThreshold()/100.0);
			Double buyThreshold = tradingSession.getRate().getBuy()*(options.getBuyThreshold()/100.0);
			
			Boolean resetOldRate =
					( (tradingSession.getRate().getLast() - tradingSession.getOldRate() > buyThreshold) && 
						tradingSession.getRate().getLast() < tradingSession.getAutoTradingOptions().getBuyCeiling() ) ||
					( (tradingSession.getRate().getLast() - tradingSession.getOldRate() < - sellThreshold) &&
						tradingSession.getRate().getLast() > tradingSession.getAutoTradingOptions().getSellFloor() );
			
			if(tradingSession.getOldRate() == 0.0 || resetOldRate) {
				tradingSession.setOldRate(tradingSession.getRate().getLast());
				mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
						new Update().set("oldRate", tradingSession.getOldRate()), TradingSession.class);
			}
			
			
			tradingSession.setUserId(user.getId());
			tradingSession.setUsername(user.getUsername());
			
			mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
					new Update().set("userId", user.getId()), TradingSession.class);
			mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
					new Update().set("username", user.getUsername()), TradingSession.class);
			
			//tradingSessionRepository.save(tradingSession);
			
			//System.out.println(tradingSession.getPair()+"("+tradingSession.getId()+"): "+tradingSession.getRate());
		
			if(tradingSession.getAutoTradingOptions().getTradeAuto() == true && tradingSession.getRate().getLast() != 0.0) {
			
				AccountFunds account = user.getAccountFunds();
				PropertyMap servicePropertyMap = account.getServiceProperties().get(tradingSession.getService());
				PropertyMap paymentPropertyMap = account.getServiceProperties().get("payment");
				Map<String, Object> serviceProperties = servicePropertyMap.getProperties();
				Map<String, Object> paymentProperties = paymentPropertyMap.getProperties();
				
				List<Map<String, Object>> periods = (List<Map<String, Object>>)paymentProperties.get("periods");
				Map<String, Object> currentPeriod = (Map<String, Object>)periods.get(1);
				
				String paymentMethod = (String)currentPeriod.get("method");
				String paymentCurrency = (String)currentPeriod.get("currency");
				
				Map<String, Map<String, Double>> serviceFees = settings.getServiceFees();
				Map<String, Double> monthlyFees = serviceFees.get("monthly");
				Map<String, Double> profitFees = serviceFees.get("profit");
				
				Boolean newTrades = false;
				
				Double currencyFunds = account.getReserves().get(paymentCurrency);
				System.out.println("Payment method: "+paymentMethod+", payment currency: "+paymentCurrency+", currency funds: "+currencyFunds);
				if(currencyFunds == null) {
					continue;
				}
				
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
				
				ExchangeApi exchangeApi = null;
				TradingClient tradingClient = null;
				
				if(tradingSession.getService().equals("btce")) {
					exchangeApi = apiFactory.createBtceApi(user);
					tradingClient = new BtceTradingClient(tradingSession, mongoTemplate);
				} else if(tradingSession.getService().equals("mtgox")) {
					exchangeApi = apiFactory.createMtgoxApi(user);
					tradingClient = new MtgoxTradingClient(tradingSession, mongoTemplate);
				} else {
					tradingClient = new TestTradingClient(tradingSession, mongoTemplate);
				}
				
				tradingClient.setExchangeApi(exchangeApi);
				
				if(tradingSession.getAutoTradingOptions().getAutoTradingModel().equals("simpleDelta")) {
					AutoTrader autoTrader = new AutoTrader(tradingClient);
					autoTrader.autoTrade(newTrades);
				} else {
					MaAutoTrader autoTrader = new MaAutoTrader(tradingClient);
					autoTrader.autoTrade(newTrades);
				}
			}
			
			
			Rate previousRate = tradingSession.getRate();
			tradingSession.setPreviousRate(previousRate);
			mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
					new Update().set("previousRate", previousRate), TradingSession.class);
			
			//tradingSessionRepository.save(tradingSession);
				
			
			
		}
		
		Long sessionsUpdateTime = (System.currentTimeMillis() - sessionsUpdateStart) / 1000L;
		System.out.println("updateTradingSessions for "+user.getUsername()+" complete in "+sessionsUpdateTime+" s.");
		
	}
	
	protected String getService() {
		return null;
	}
	
}
