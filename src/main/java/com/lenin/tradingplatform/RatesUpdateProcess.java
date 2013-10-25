package com.lenin.tradingplatform;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.lenin.tradingplatform.client.BtceApi;
import com.lenin.tradingplatform.data.entities.Rate;
import com.lenin.tradingplatform.data.repositories.OrderRepository;
import com.lenin.tradingplatform.data.repositories.RateRepository;
import com.lenin.tradingplatform.data.repositories.TradeRepository;
import com.lenin.tradingplatform.data.repositories.TradingSessionRepository;
import com.lenin.tradingplatform.data.repositories.UserRepository;

@Service
public class RatesUpdateProcess {
	
	Map<String, Rate> rateMap = new HashMap<String, Rate>();
	
	private Long rateUpdateCounter = 0L;
	
	private Long lastRateUpdateTime = 0L;
	private Long lastRateUpdateTime1min = 0L;
	private Long lastRateUpdateTime10min = 0L;
	private Long lastRateUpdateTime30min = 0L;
	private Long lastRateUpdateTime4h = 0L;
	private Long lastRateUpdateTime6h = 0L;
	
	
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
		
		if(lastRateUpdateTime1min == 0) {
			lastRateUpdateTime1min = lastRateUpdateTime;
			lastRateUpdateTime10min = lastRateUpdateTime;
			lastRateUpdateTime30min = lastRateUpdateTime;
			lastRateUpdateTime4h = lastRateUpdateTime;
			lastRateUpdateTime6h = lastRateUpdateTime;
		}
		
		rateMap = new HashMap<String, Rate>();
		rateMap.put("ltc_usd", rateLtcUsd);
		rateMap.put("btc_usd", rateBtcUsd);
		rateMap.put("ltc_btc", rateLtcBtc);
		
		
		if(rateUpdateCounter > 0) {
		
			if(rateUpdateCounter % 4 == 0) {
				createAverageRates("1min");
				lastRateUpdateTime1min = lastRateUpdateTime;
			}
		
			if(rateUpdateCounter % 40 == 0) {
				createAverageRates("10min");
				lastRateUpdateTime10min = lastRateUpdateTime;
			}
		
			if(rateUpdateCounter % 120 == 0) {
				createAverageRates("30min");
				lastRateUpdateTime30min = lastRateUpdateTime;
			}
		
			if(rateUpdateCounter % 960 == 0) {
				createAverageRates("4h");
				lastRateUpdateTime4h = lastRateUpdateTime;
			}
		
			if(rateUpdateCounter % 1440 == 0) {
				createAverageRates("6h");
				lastRateUpdateTime6h = lastRateUpdateTime;
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
		
		Long until = lastRateUpdateTime;
		
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
		
		Long from = lastRateUpdateTime1min - 1;
		
		if(setType.equals("10min")) {
			from = lastRateUpdateTime10min - 1;
		} else if(setType.equals("30min")) {
			from = lastRateUpdateTime30min - 1;
		} else if(setType.equals("4h")) {
			from = lastRateUpdateTime4h - 1;
		} else if(setType.equals("6h")) {
			from = lastRateUpdateTime6h - 1;
		}
		
		//List<Rate> rates = rateRepository.findByPairAndTimeGreaterThanOrderByTimeAsc(pair, lastRateUpdateTime - period);
		List<Rate> rates = rateRepository.findByPairAndSetTypeAndTimeBetweenOrderByTimeAsc(pair, "15s", from, until);
		
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
		
		avgRate.setMovingAverages(last.getMovingAverages());
		
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
			rate.setService("btce");
			
			setMovingAverages(rate);
			
			return rate;
			
		} catch(Exception e) {
			
			e.printStackTrace();
			return null;
			
		}
    	
	}
	
	
	private void setMovingAverages(Rate lastRate) {
		
		Long until = lastRate.getTime();
		Long from = until - (24*60*60);
		
		List<Rate> rates = rateRepository.findByPairAndSetTypeAndTimeBetweenOrderByTimeAsc(lastRate.getPair(), "15s", from, until);
		
		Map<String, Double> movingAverages = lastRate.getMovingAverages();
		
		Map<String, Long> fromTimes = new HashMap<String, Long>();
		fromTimes.put("1d", from);
		fromTimes.put("12h", until - (12*60*60));
		fromTimes.put("6h", until - (6*60*60));
		fromTimes.put("4h", until - (4*60*60));
		fromTimes.put("2h", until - (2*60*60));
		fromTimes.put("1h", until - (1*60*60));
		fromTimes.put("30min", until - (30*60));
		fromTimes.put("10min", until - (10*60));
		
		Set<String> timeKeys = fromTimes.keySet();
		
		Map<String, Integer> fromCounts = new HashMap<String, Integer>();
		
		for(Rate rate : rates) {
			
			for(String period : timeKeys) {
				addRateToCounts(rate, fromTimes, fromCounts, period);
			}
					
		}

		Map<String, Double> fromTotals = new HashMap<String, Double>();
		
		Map<String, Double> emaUpperSums = new HashMap<String, Double>();
		Map<String, Double> emaLowerSums = new HashMap<String, Double>();
		Map<String, Integer> emaIndexes = new HashMap<String, Integer>();
		

		for(Rate rate : rates) {
			
			for(String period : timeKeys) {
				addRateToSma(rate, fromTimes, fromTotals, fromCounts, period);
				addRateToEma(rate, fromTimes, emaUpperSums, emaLowerSums, emaIndexes, fromCounts.get(period), period);
			}
					
		}
		
		for(String period : timeKeys) {
			
			Double total = fromTotals.get(period);
			Integer count = fromCounts.get(period);
			
			if(total == null || count == null) {
				continue;
			}
			
			Double sma = total / count;
			
			Double emaUpperSum = emaUpperSums.get(period);
			Double emaLowerSum = emaLowerSums.get(period);
			Double ema = emaUpperSum / emaLowerSum;
			
			movingAverages.put("sma"+period, sma);
			movingAverages.put("ema"+period, ema);
			
			lastRate.setMovingAverages(movingAverages);
			
			System.out.println("SMA "+period+" = "+sma);
			System.out.println("EMA "+period+" = "+ema);
			
		}
		
		
	}
	
	
	private void addRateToCounts(Rate rate, Map<String, Long> fromTimes, Map<String, Integer> fromCounts, String period) {
		
		if(rate.getTime() >= fromTimes.get(period)) {
		
			Integer count = fromCounts.get(period);
			if(count == null) {
				count = 0;
			}
		
			count = count + 1;
		
			fromCounts.put(period, count);
		
		}
		
	}
	
	private void addRateToSma(Rate rate, Map<String, Long> fromTimes, Map<String, Double> fromTotals, Map<String, Integer> fromCounts, String period) {
		
		if(rate.getTime() >= fromTimes.get(period)) {
		
			Double total = fromTotals.get(period);
			if(total == null) {
				total = 0.0;
			}
		
			BigDecimal bdTotal = new BigDecimal(total);
			bdTotal = bdTotal.add(new BigDecimal(rate.getLast()));
			total = bdTotal.doubleValue();
		
			fromTotals.put(period, total);
		
		}
	
	}
	
	
	private void addRateToEma(Rate rate, Map<String, Long> fromTimes, Map<String, Double> emaUpperSums, Map<String, Double> emaLowerSums, Map<String, Integer> emaIndexes, Integer count, String period) {
		
		if(rate.getTime() >= fromTimes.get(period)) {
			
			Double alpha = 2.0/(count.doubleValue()+1.0);
			
			Integer index = emaIndexes.get(period);
			if(index == null) {
				index = 0;
			}
			
			Double upperSum = emaUpperSums.get(period);
			if(upperSum == null) {
				upperSum = 0.0;
			}
			
			Double upperSumIncr = rate.getLast()*Math.pow(1-alpha, (count-index));
			BigDecimal bdUpperSum = new BigDecimal(upperSum);
			bdUpperSum = bdUpperSum.add(new BigDecimal(upperSumIncr));
			upperSum = bdUpperSum.doubleValue();
		
			emaUpperSums.put(period, upperSum);
		
			Double lowerSum = emaLowerSums.get(period);
			if(lowerSum == null) {
				lowerSum = 0.0;
			}
			
			Double lowerSumIncr = Math.pow(1-alpha, (count-index));
			BigDecimal bdLowerSum = new BigDecimal(lowerSum);
			bdLowerSum = bdLowerSum.add(new BigDecimal(lowerSumIncr));
			lowerSum = bdLowerSum.doubleValue();
			
			emaLowerSums.put(period, lowerSum);
			
			emaIndexes.put(period, index+1);
		
		}
	
	}
	
}
