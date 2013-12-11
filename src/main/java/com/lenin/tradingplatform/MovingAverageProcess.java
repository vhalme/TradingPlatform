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
public class MovingAverageProcess {
	
	private Map<String, Long> lastAvgUpdateTimes = new HashMap<String, Long>();
	
	
	@Autowired
	private MongoTemplate mongoTemplate;
	
	@Autowired
	private RateRepository rateRepository;
	
	
	
	public void update() {
		
		try {
			
			long start = System.currentTimeMillis();
			
			System.out.println("Started moving average updates "+(new Date()));
			
			System.out.println("Updating averages");
			
			List<String> btcePairs = new ArrayList<String>();
			btcePairs.add("ltc_usd");
			btcePairs.add("ltc_btc");
			btcePairs.add("btc_usd");
			
			List<String> mtgoxPairs = new ArrayList<String>();
			mtgoxPairs.add("btc_usd");
			
			updateAverages("btce", btcePairs);
			updateAverages("mtgox", mtgoxPairs);
			
			long finish = System.currentTimeMillis();
		
			long time = finish-start;
		
			System.out.println("["+(new Date())+"] MA Update pass complete in "+time/1000+" s");
		
		} catch(Exception e) {
			
			System.out.println(e.getMessage());
			StackTraceElement[] trace = e.getStackTrace();
			for(int i=0; i<trace.length; i++) {
				System.out.println(trace[i].toString());
			}
			
			e.printStackTrace();
			
		}
		
	}
	
	
	
	private void updateAverages(String service, List<String> pairs) throws Exception {
		
		Long now = System.currentTimeMillis()/1000L;
		
		List<Rate> rates = new ArrayList<Rate>();
		
		Long fromTime = now - 120;
		
		Long lastAvgUpdateTime = lastAvgUpdateTimes.get(service);
		
		if(lastAvgUpdateTime != null) {
			fromTime = lastAvgUpdateTime;
		} else {
			lastAvgUpdateTime = fromTime;
			lastAvgUpdateTimes.put(service, lastAvgUpdateTime);
		}
		
		for(String pair : pairs) {
			
			Query query = new Query(Criteria.where("pair").is(pair).andOperator(
					Criteria.where("service").is(service),
					Criteria.where("time").gt(fromTime)
				)).with(new Sort(Sort.Direction.ASC, "time"));
			
			List<Rate> pairRates = mongoTemplate.find(query, Rate.class);
			
			//List<Rate> rates = rateRepository.findByPairAndTimeGreaterThanOrderByTimeAsc("ltc_usd", fromTime);
		
			if(pairRates.size() > 0) {
			
				int last = pairRates.size()-1;
				Rate pairRate = pairRates.get(last);
				setNewMovingAverages(pairRate);
				for(int i=0; i<last; i++) {
					Rate rate = pairRates.get(i);
					rate.setMovingAverages(pairRate.getMovingAverages());
					rates.add(rate);
				}
			
				lastAvgUpdateTime = pairRate.getTime();
				lastAvgUpdateTimes.put(service, lastAvgUpdateTime);
		
			}
			
		}
		
		System.out.println("Saving "+rates.size()+" rates from "+service);
		
		rateRepository.save(rates);
		
	}
	
	
	
	
	private void setNewMovingAverages(Rate lastRate) {
		
		Long maStartTime = System.currentTimeMillis();
		
		Long now = lastRate.getTime();
		Long from = now - (30*24*60*60);
		Long until = now;
		
		List<Rate> rates = new ArrayList<Rate>();
		
		System.out.println("Querying rates for pair "+lastRate.getPair()+"@"+lastRate.getService()+" from "+from+" until "+until);
		
		Query query = new Query(Criteria.where("pair").is(lastRate.getPair()).andOperator(
				Criteria.where("service").is(lastRate.getService()),
				Criteria.where("setType").is("1min"),
				Criteria.where("time").gte(from),
				Criteria.where("time").lt(until)
			)).with(new Sort(Sort.Direction.ASC, "time"));
		
		rates = mongoTemplate.find(query, Rate.class);
		
		//rates = rateRepository.findByPairAndSetTypeAndTimeBetweenOrderByTimeAsc(lastRate.getPair(), "1min", from, until);
		
		Long ratesFetchedTime = (System.currentTimeMillis()-maStartTime)/1000L;
		System.out.println("Fetched "+rates.size()+" rates in "+ratesFetchedTime+" s.");
		
		Map<String, Double> movingAverages = lastRate.getMovingAverages();
		
		Map<String, Long> fromTimes = new HashMap<String, Long>();
		fromTimes.put("1m", until - (30*24*60*60));
		fromTimes.put("21d", until - (21*24*60*60));
		fromTimes.put("14d", until - (14*24*60*60));
		fromTimes.put("7d", until - (7*24*60*60));
		fromTimes.put("1d", until - (24*60*60));
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
		
		Long maCalcedStartTime = System.currentTimeMillis();
		
		for(Rate rate : rates) {
			
			for(String period : timeKeys) {
				addRateToSma(rate, fromTimes, fromTotals, fromCounts, period);
				addRateToEma(rate, fromTimes, emaUpperSums, emaLowerSums, emaIndexes, fromCounts.get(period), period);
			}
					
		}
		
		Long maCalcedTime = (System.currentTimeMillis()-maCalcedStartTime)/1000L;
		System.out.println(rates.size()+" calculated in "+maCalcedTime+" s.");
		
		for(String period : timeKeys) {
			
			Double total = fromTotals.get(period);
			Integer count = fromCounts.get(period);
			
			if(total == null || count == null) {
				continue;
			}
			
			//Double alpha = 2.0/(count.doubleValue()+1.0);
			
			Double sma = total / count;
			
			Double emaUpperSum = emaUpperSums.get(period);
			Double emaLowerSum = emaLowerSums.get(period);
			//Double ema = alpha * emaUpperSum;
			Double ema = emaUpperSum / emaLowerSum;
			
			movingAverages.put("sma"+period, sma);
			movingAverages.put("ema"+period, ema);
			
			lastRate.setMovingAverages(movingAverages);
			
			System.out.println("SMA "+period+" = "+sma);
			System.out.println("EMA "+period+" = "+ema);
			
		}
		
		Long maTime = (System.currentTimeMillis()-maStartTime)/1000L;
		System.out.println("Moving averages created in "+maTime+" s.");
		
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
			
			Double value = rate.getLast();
			
			BigDecimal bdTotal = new BigDecimal(total);
			bdTotal = bdTotal.add(new BigDecimal(value));
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
			
			
			Double value = rate.getLast();
			
			Double upperSumIncr = value*Math.pow(1-alpha, (count-index));
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
	
	/*
	private Double getFirstRate(Rate lastRate, Long ago) {
		
		Long now = lastRate.getTime();
		Long from = now - ago;
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		Query query = 
				new Query(Criteria.where("setType").is("15s")
						.andOperator(Criteria.where("pair").is(lastRate.getPair()), Criteria.where("time").gte(from)))
						.with(new Sort(Direction.ASC, "time")).limit(1);
		
		List<Rate> rates = mongoOps.find(query, Rate.class);
		
		Double rate = 0D;
		
		if(rates.size() > 0) {
			rate = rates.get(0).getLast();
			System.out.println("Dound rate at "+rates.get(0).getTime());
		}
		
		return rate;
		
	}
	
	
	private void setSmaFromPrevRate(Rate lastRate) {
		
		System.out.println("Do sma");
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		Query query = 
				new Query(Criteria.where("setType").is("15s")
						.andOperator(Criteria.where("pair").is(lastRate.getPair())))
						.with(new Sort(Direction.DESC, "time")).limit(1);
		
		List<Rate> latestRates = mongoOps.find(query, Rate.class);
		
		Rate latestRate = latestRates.get(0);
		Map<String, Double> lastMovingAverages = latestRate.getMovingAverages();
		
		Long count1m = 30L*24L*60L*4L;
		Long count7d = 7L*24L*60L*4L;
		
		Double lastSma1m = lastMovingAverages.get("sma1m");
		Double lastSma7d = lastMovingAverages.get("sma7d");
		
		System.out.println("Get firsts");
		Double rate1m = getFirstRate(lastRate, (30L*24L*60L*60L));
		Double rate7d = getFirstRate(lastRate, (7L*24L*60L*60L));
		
		System.out.println("Got em");
		
		Double newSma1m = (-rate1m/count1m)+lastSma1m+(latestRate.getLast()/count1m);
		Double newSma7d = (-rate7d/count7d)+lastSma7d+(latestRate.getLast()/count7d);
		
		Map<String, Double> movingAverages = lastRate.getMovingAverages();
		movingAverages.put("sma1m", newSma1m);
		movingAverages.put("sma7d", newSma7d);
			
		System.out.println("Sma done");
		
	}
	
	
	private void setEmaFromPrevRate(Rate lastRate) {
		
		System.out.println("Do ema");
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		Query query = 
				new Query(Criteria.where("setType").is("15s")
						.andOperator(Criteria.where("pair").is(lastRate.getPair())))
						.with(new Sort(Direction.DESC, "time")).limit(1);
		
		List<Rate> latestRates = mongoOps.find(query, Rate.class);
		
		Rate latestRate = latestRates.get(0);
		Map<String, Double> lastMovingAverages = latestRate.getMovingAverages();
		
		Long count1m = 30L*24L*60L*4L;
		Long count7d = 7L*24L*60L*4L;
		Long count1d = 24L*60L*4L;
		Long count12h = 12L*60L*4L;
		Long count6h = 6L*60L*4L;
		Long count4h = 4L*60L*4L;
		Long count2h = 2L*60L*4L;
		Long count1h = 60L*4L;
		Long count30min = 30L*4L;
		Long count10min = 10L*4L;
		
		Double lastEma1m = lastMovingAverages.get("ema1m");
		Double lastEma7d = lastMovingAverages.get("ema7d");
		Double lastEma1d = lastMovingAverages.get("ema1d");
		Double lastEma12h = lastMovingAverages.get("ema12h");
		Double lastEma6h = lastMovingAverages.get("ema6h");
		Double lastEma4h = lastMovingAverages.get("ema4h");
		Double lastEma2h = lastMovingAverages.get("ema2h");
		Double lastEma1h = lastMovingAverages.get("ema1h");
		Double lastEma30min = lastMovingAverages.get("ema30min");
		Double lastEma10min = lastMovingAverages.get("ema10min");
		
		Double alpha1m = 2.0/(count1m.doubleValue()+1.0);
		Double alpha7d = 2.0/(count7d.doubleValue()+1.0);
		Double alpha1d = 2.0/(count1d.doubleValue()+1.0);
		Double alpha12h = 2.0/(count12h.doubleValue()+1.0);
		Double alpha6h = 2.0/(count6h.doubleValue()+1.0);
		Double alpha4h = 2.0/(count4h.doubleValue()+1.0);
		Double alpha2h = 2.0/(count2h.doubleValue()+1.0);
		Double alpha1h = 2.0/(count1h.doubleValue()+1.0);
		Double alpha30min = 2.0/(count30min.doubleValue()+1.0);
		Double alpha10min = 2.0/(count10min.doubleValue()+1.0);
		
		Double newEma1m = lastEma1m + (alpha1m * (lastRate.getLast()-lastEma1m));
		Double newEma7d = lastEma7d + (alpha7d * (lastRate.getLast()-lastEma7d));
		Double newEma1d = lastEma1d + (alpha1d * (lastRate.getLast()-lastEma1d));
		Double newEma12h = lastEma12h + (alpha12h * (lastRate.getLast()-lastEma12h));
		Double newEma6h = lastEma6h + (alpha6h * (lastRate.getLast()-lastEma6h));
		Double newEma4h = lastEma4h + (alpha4h * (lastRate.getLast()-lastEma4h));
		Double newEma2h = lastEma2h + (alpha2h * (lastRate.getLast()-lastEma2h));
		Double newEma1h = lastEma1h + (alpha1h * (lastRate.getLast()-lastEma1h));
		Double newEma30min = lastEma30min + (alpha30min * (lastRate.getLast()-lastEma30min));
		Double newEma10min = lastEma10min + (alpha10min * (lastRate.getLast()-lastEma10min));
		
		Map<String, Double> movingAverages = lastRate.getMovingAverages();
		movingAverages.put("ema1m", newEma1m);
		movingAverages.put("ema7d", newEma7d);
		movingAverages.put("ema1d", newEma1d);
		movingAverages.put("ema12h", newEma12h);
		movingAverages.put("ema6h", newEma6h);
		movingAverages.put("ema4h", newEma4h);
		movingAverages.put("ema2h", newEma2h);
		movingAverages.put("ema1h", newEma1h);
		movingAverages.put("ema30min", newEma30min);
		movingAverages.put("ema10min", newEma10min);
		
		System.out.println("Ema done");
		
	}
	
	
	private void setMovingAveragesFromPrevRates(Rate lastRate) {
		
		setSmaFromPrevRate(lastRate);
		setEmaFromPrevRate(lastRate);
		
	}
	*/
	
}
