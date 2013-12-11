package com.lenin.tradingplatform;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.lenin.tradingplatform.client.BtceApi;
import com.lenin.tradingplatform.client.MtgoxApi;
import com.lenin.tradingplatform.data.entities.Rate;
import com.lenin.tradingplatform.data.repositories.OrderRepository;
import com.lenin.tradingplatform.data.repositories.RateRepository;
import com.lenin.tradingplatform.data.repositories.TradeRepository;
import com.lenin.tradingplatform.data.repositories.TradingSessionRepository;
import com.lenin.tradingplatform.data.repositories.UserRepository;

@Service
public class RatesUpdateProcess {
	
	private Map<String, Long> rateUpdateCounters = new HashMap<String, Long>();
	
	private Map<String, Long> lastRateUpdateTimes = new HashMap<String, Long>();
	
	
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
	
	
	public void update() {
		
		try {
			
			long start = System.currentTimeMillis();
			
			System.out.println("Started trade updates "+(new Date()));
			
			System.out.println("Updating rates");
			updateRates();
			
			long finish = System.currentTimeMillis();
		
			long time = finish-start;
		
			System.out.println("["+(new Date())+"] Update pass complete in "+time/1000+" s");
		
		} catch(Exception e) {
			
			System.out.println(e.getMessage());
			StackTraceElement[] trace = e.getStackTrace();
			for(int i=0; i<trace.length; i++) {
				System.out.println(trace[i].toString());
			}
			
			e.printStackTrace();
			
		}
		
	}
	
	
	private void updateRates() {
		
		List<String> btcePairs = new ArrayList<String>();
		btcePairs.add("ltc_usd");
		btcePairs.add("ltc_btc");
		btcePairs.add("btc_usd");
		
		List<String> mtgoxPairs = new ArrayList<String>();
		mtgoxPairs.add("btc_usd");
		
		List<Rate> btceRates = getRates("btce", btcePairs);
		List<Rate> mtgoxRates = getRates("mtgox", mtgoxPairs);
		
		if(btceRates != null && btceRates.size() > 0) {
			rateRepository.save(btceRates);
			Long lastRateUpdateTime = btceRates.get(btceRates.size()-1).getTime();
			lastRateUpdateTimes.put("btce", lastRateUpdateTime);
			generateAverages("btce", btcePairs);
		}
		
		if(mtgoxRates != null && mtgoxRates.size() > 0) {
			rateRepository.save(mtgoxRates);
			Long lastRateUpdateTime = mtgoxRates.get(mtgoxRates.size()-1).getTime();
			lastRateUpdateTimes.put("mtgox", lastRateUpdateTime);
			generateAverages("mtgox", mtgoxPairs);
		}
		
		
	}
	
	
	private void generateAverages(String service, List<String> pairs) {
		
		if(lastRateUpdateTimes.get(service+"1min") == null) {
			lastRateUpdateTimes.put(service+"1min", lastRateUpdateTimes.get(service));
			lastRateUpdateTimes.put(service+"10min", lastRateUpdateTimes.get(service));
			lastRateUpdateTimes.put(service+"30min", lastRateUpdateTimes.get(service));
			lastRateUpdateTimes.put(service+"4h", lastRateUpdateTimes.get(service));
			lastRateUpdateTimes.put(service+"6h", lastRateUpdateTimes.get(service));
		}
		
		if(rateUpdateCounters.get(service) == null) {
			rateUpdateCounters.put(service, 0L);
		}
		
		Long rateUpdateCounter = rateUpdateCounters.get(service);
		
		if(rateUpdateCounter > 0) {
		
			if(rateUpdateCounter % 4 == 0) {
				createAverageRates(service, pairs, "1min");
				lastRateUpdateTimes.put(service+"1min", lastRateUpdateTimes.get(service));
			}
		
			if(rateUpdateCounter % 40 == 0) {
				createAverageRates(service, pairs, "10min");
				lastRateUpdateTimes.put(service+"10min", lastRateUpdateTimes.get(service));
			}
		
			if(rateUpdateCounter % 120 == 0) {
				createAverageRates(service, pairs, "30min");
				lastRateUpdateTimes.put(service+"30min", lastRateUpdateTimes.get(service));
			}
		
			if(rateUpdateCounter % 960 == 0) {
				createAverageRates(service, pairs, "4h");
				lastRateUpdateTimes.put(service+"4h", lastRateUpdateTimes.get(service));
			}
		
			if(rateUpdateCounter % 1440 == 0) {
				createAverageRates(service, pairs, "6h");
				lastRateUpdateTimes.put(service+"6h", lastRateUpdateTimes.get(service));
				rateUpdateCounters.put(service, 0L);
			}
			
		}
		
		rateUpdateCounter = rateUpdateCounter + 1;
		rateUpdateCounters.put(service, rateUpdateCounter);
		
		
	}
	
	private List<Rate> getRates(String service, List<String> pairs) {
		
		List<Rate> rates = new ArrayList<Rate>();
		
		for(String pair : pairs) {
			
			Rate rate = getRate(service, pair);
			
			if(rate == null) {
				
				System.out.println("Couldn't create a "+pair+"@"+service+" rate");
				return null;
				
			} else {
				
				rates.add(rate);
				
			}
			
		}
		
		
		return rates;
		
		
	}
	
	
	private void createAverageRates(String service, List<String> pairs, String setType) {
		
		List<Rate> avgRates = new ArrayList<Rate>();
		for(String pair : pairs) {
			Rate avgRate = getAverageRate(service, pair, setType);
			if(avgRate != null) {
				avgRates.add(avgRate);
			}
		}
		
		if(avgRates.size() > 0) {
			rateRepository.save(avgRates);
		}
		
	}
	
	
	private Rate getAverageRate(String service, String pair, String setType) {
		
		Long until = lastRateUpdateTimes.get(service);
		
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
		
		Long from = lastRateUpdateTimes.get(service+"1min") - 1;
		
		if(setType.equals("10min")) {
			from = lastRateUpdateTimes.get(service+"10min") - 1;
		} else if(setType.equals("30min")) {
			from = lastRateUpdateTimes.get(service+"30min") - 1;
		} else if(setType.equals("4h")) {
			from = lastRateUpdateTimes.get(service+"4h") - 1;
		} else if(setType.equals("6h")) {
			from = lastRateUpdateTimes.get(service+"6h") - 1;
		}
		
		Query query = new Query(Criteria.where("pair").is(pair).andOperator(
				Criteria.where("service").is(service),
				Criteria.where("setType").is("15s"),
				Criteria.where("time").gt(from),
				Criteria.where("time").lte(until)
			)).with(new Sort(Sort.Direction.ASC, "time"));
		
		List<Rate> rates = mongoTemplate.find(query, Rate.class);
		
		//List<Rate> rates = rateRepository.findByPairAndTimeGreaterThanOrderByTimeAsc(pair, lastRateUpdateTime - period);
		//List<Rate> rates = rateRepository.findByPairAndSetTypeAndTimeBetweenOrderByTimeAsc(pair, "15s", from, until);
		
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
		avgRate.setTime(lastRateUpdateTimes.get(service));
		
		avgRate.setLast(avgLast);
		avgRate.setBuy(avgBuy);
		avgRate.setSell(avgSell);
		avgRate.setOpen(open);
		avgRate.setClose(close);
		avgRate.setHigh(high);
		avgRate.setLow(low);
		avgRate.setService(service);
		
		avgRate.setMovingAverages(last.getMovingAverages());
		
		return avgRate;
		
	}
	
	
	private Rate getRate(String service, String pair) {
		
		try {
			
			System.out.println("getting rate "+pair);
			
			if(service.equals("btce")) {
			
				BtceApi btceApi = new BtceApi("", "", null);
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
				rate.setService("btce");
			
				return rate;
				
			} else {
				
				System.out.println("create mtgox api");
				MtgoxApi mtgoxApi = new MtgoxApi("", "", null);
				
				System.out.println("getting rates from api");
				JSONObject ratesResult = mtgoxApi.getRates(pair);
				System.out.println("parsing json");
				JSONObject rateJson = ratesResult.getJSONObject("return");
				System.out.println("json parsed");
				
				Rate rate = new Rate();
				
				rate.setSetType("15s");
				rate.setPair(pair);
				rate.setLast(rateJson.getJSONObject("last").getDouble("value"));
				rate.setBuy(rateJson.getJSONObject("buy").getDouble("value"));
				rate.setSell(rateJson.getJSONObject("sell").getDouble("value"));
				rate.setHigh(rateJson.getJSONObject("high").getDouble("value"));
				rate.setLow(rateJson.getJSONObject("low").getDouble("value"));
				rate.setAverage(rateJson.getJSONObject("avg").getDouble("value"));
				rate.setOpen(rate.getLast());
				rate.setClose(rate.getLast());
				rate.setVolume(rateJson.getJSONObject("vol").getDouble("value"));
				rate.setCurrentVolume(rate.getVolume());
				rate.setTime(rateJson.getLong("now")/1000000);
				rate.setService("mtgox");
					
				System.out.println("got mtgox rate: "+rate.getSell()+"/"+rate.getBuy()+"/"+rate.getOpen()+"/"+rate.getClose()+"/"+rate.getLast());
				
				return rate;
				
			}
			
		} catch(Exception e) {
			
			e.printStackTrace();
			return null;
			
		}
    	
	}
	
	
}
