package com.lenin.tradingplatform;

import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.lenin.tradingplatform.client.BtceApi;
import com.lenin.tradingplatform.client.TradingClient;
import com.lenin.tradingplatform.data.entities.AutoTradingOptions;
import com.lenin.tradingplatform.data.entities.Order;
import com.lenin.tradingplatform.data.entities.Rate;
import com.lenin.tradingplatform.data.entities.TradingSession;
import com.lenin.tradingplatform.data.repositories.OrderRepository;
import com.lenin.tradingplatform.data.repositories.RateRepository;
import com.lenin.tradingplatform.data.repositories.TradingSessionRepository;

public class MaAutoTrader {
	
	private TradingClient client;
	private TradingSession tradingSession;
	
	private MongoTemplate mongoTemplate;
	
	public MaAutoTrader(TradingClient client) {
		
		this.client = client;
		
		tradingSession = client.getTradingSession();
		mongoTemplate = client.getMongoTemplate();
		
	}
	
	
	public void autoTrade(Boolean newTrades) {
		
		AutoTradingOptions options = tradingSession.getAutoTradingOptions();
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		if(options.getManualSettings() == false) {
			updateRange();
			//tradingSessionRepository.save(tradingSession);
		}
		
		String maLongPeriod = options.getMaLong();
		String maShortPeriod = options.getMaShort();
		
		if(tradingSession.getLive() == false) {
			maLongPeriod = "testLong";
			maShortPeriod = "testShort";
		}
		
		System.out.println("maLong: "+maLongPeriod+", maShort: "+maShortPeriod+" (v5)");
		
		Rate lastRate = tradingSession.getRate();
		Map<String, Double> lastMovingAverages = lastRate.getMovingAverages();
		
		Rate prevRate = tradingSession.getPreviousRate();
		Map<String, Double> prevMovingAverages = prevRate.getMovingAverages();
		
		Double lastMaLong = lastMovingAverages.get(maLongPeriod);
		Double lastMaShort = lastMovingAverages.get(maShortPeriod);
		Double prevMaLong = prevMovingAverages.get(maLongPeriod);
		Double prevMaShort = prevMovingAverages.get(maShortPeriod);
		
		System.out.println("prevMaLong: "+prevMaLong+", prevMaShort: "+prevMaShort);
		System.out.println("lastMaLong: "+lastMaLong+", lastMaShort: "+lastMaShort);
		
		Double lastMaTransactionRate = tradingSession.getLastMaTransactionRate();
		System.out.println("lastTransactionRate = "+lastMaTransactionRate);
		Double maChange = lastMaLong - lastMaTransactionRate;
		
		Double tradingRangeCeiling = options.getTradingRangeTop();
		Double tradingRangeFloor = options.getTradingRangeBottom();
		
		Double sellThreshold = lastRate.getSell()*(options.getSellThreshold()/100.0);
		Double buyThreshold = lastRate.getBuy()*(options.getBuyThreshold()/100.0);
		
		System.out.println("lastMaTransaction: "+tradingSession.getLastMaTransactionRate()+", maChange: "+maChange+", sellThreshold: "+sellThreshold+", buyThreshold: "+buyThreshold);
		
		if(maChange > sellThreshold) {
			
			if(prevMaLong < prevMaShort && lastMaLong > lastMaShort) {
				
				Double fraction = maChange / (tradingRangeCeiling - tradingSession.getLastMaTransactionRate());
				
				System.out.println("SELL CROSSOVER: "+fraction+" = "+maChange+" / ("+tradingRangeCeiling+" - "+tradingSession.getLastMaTransactionRate()+")");
				
				if(lastRate.getSell() > tradingRangeCeiling) {
					fraction = 1.0;
				}
				
				Double sellChunk = tradingSession.getFundsRight() * fraction;
				if(options.getAllIn() == true) {
					sellChunk = tradingSession.getFundsRight();
				}
				
				if(sellChunk <= 0) {
					return;
				}
				
				Query orderQuery = new Query(Criteria.where("tradingSession").is(tradingSession).andOperator(
							Criteria.where("type").is("buy"),
							Criteria.where("mode").is("auto"),
							Criteria.where("rate").lt(lastRate.getSell())
						)).with(new Sort(Direction.ASC, "rate"));
				
				List<Order> reversibleBuys = mongoTemplate.find(orderQuery, Order.class);
				
				//List<Order> reversibleBuys = 
				//		orderRepository.findByTradingSessionAndTypeAndModeAndRateLessThanOrderByRateAsc(tradingSession, "buy", "auto", lastRate.getSell());
				
				Double sellAmount = sellChunk;
				System.out.println("Sell amount: "+sellAmount);
				
				for(Order reversibleBuy : reversibleBuys) {
					
					Order reversedOrder = reversibleBuy.getReversedOrder();
					Boolean isReversed = reversibleBuy.getIsReversed();
					Boolean isFilled = reversibleBuy.getIsFilled();
					
					if(reversedOrder != null || (!isReversed && !isFilled) || reversibleBuy.getRate() > lastRate.getSell()-sellThreshold) {
						continue;
					}
					
					Double orderAmount = reversibleBuy.getAmount();
					if(reversibleBuy.getIsFilled() == false) {
						orderAmount = reversibleBuy.getAmount()-reversibleBuy.getFilledAmount();
					}
					
					Double sellAmountDiff = sellAmount - orderAmount;
					System.out.println("Sell amount diff: "+sellAmount+" - "+orderAmount+" = "+sellAmountDiff);
					
					if(sellAmountDiff >= 0) {
						
						sellAmount = sellAmount - orderAmount;
						if(reversibleBuy.getIsFilled() == true) {
							System.out.println("FULL BUY/SELL REVERSE ON ("+reversibleBuy.getOrderId()+"/"+reversibleBuy.getAmount()+"/"+reversibleBuy.getRate()+"), sellAmount="+sellAmount);
							client.reverseTrade(reversibleBuy, false);
						} else {
							System.out.println("FILL UP BUY/SELL REVERSE ON ("+reversibleBuy.getOrderId()+"/"+reversibleBuy.getAmount()+"/"+reversibleBuy.getRate()+"), sellAmount="+sellAmount);
							client.partialReverseTrade(reversibleBuy, orderAmount, false);
						}
						
					} else {
						
						System.out.println("PARTIAL BUY/SELL REVERSE ON ("+reversibleBuy.getOrderId()+"/"+reversibleBuy.getAmount()+"/"+reversibleBuy.getRate()+"), sellAmount="+sellAmount);
						client.partialReverseTrade(reversibleBuy, sellAmount, false);
						sellAmount = 0.0;
						
					}
					
				}
				
				if(sellAmount > 0) {
					
					System.out.println("NEW SELL ("+sellAmount+"/"+client.actualTradeRate("sell")+")");
					
					Order sellOrder = 
							BtceApi.createOrder(tradingSession.getCurrencyRight()+"_"+tradingSession.getCurrencyLeft(), 
									sellAmount, client.actualTradeRate("sell"), "sell");
				
					sellOrder.setMode("auto");
					sellOrder.setSave(true);
					
					client.trade(sellOrder);
					
				}
				
				tradingSession.setLastMaTransactionRate(lastMaLong);
				
				mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
						new Update().set("lastMaTransactionRate", tradingSession.getLastMaTransactionRate()), TradingSession.class);
				
				//tradingSessionRepository.save(tradingSession);
				
			}
			
		}

		if(maChange < -buyThreshold) {
			
			if(prevMaLong > prevMaShort && lastMaLong < lastMaShort) {
				
				Double fraction = maChange / (tradingRangeFloor - tradingSession.getLastMaTransactionRate());
				
				System.out.println("BUY CROSSOVER: "+fraction+" = "+maChange+" / ("+tradingRangeFloor+" - "+tradingSession.getLastMaTransactionRate()+")");
				
				if(lastRate.getBuy() < tradingRangeFloor) {
					fraction = 1.0;
				}
				
				Double buyChunk = (tradingSession.getFundsLeft() * fraction) / lastRate.getBuy();
				if(options.getAllIn() == true) {
					buyChunk = (tradingSession.getFundsLeft()*0.96)/lastRate.getBuy();
				}
				
				if(buyChunk <= 0) {
					return;
				}
				
				Query orderQuery = new Query(Criteria.where("tradingSession").is(tradingSession).andOperator(
						Criteria.where("type").is("sell"),
						Criteria.where("mode").is("auto"),
						Criteria.where("rate").gt(lastRate.getSell())
					)).with(new Sort(Direction.DESC, "rate"));
				
				List<Order> reversibleSells = mongoTemplate.find(orderQuery, Order.class);
				
				//List<Order> reversibleSells = 
				//		orderRepository.findByTradingSessionAndTypeAndModeAndRateGreaterThanOrderByRateDesc(tradingSession, "sell", "auto", lastRate.getBuy());
				
				Double buyAmount = buyChunk;
				System.out.println("Buy amount: "+buyAmount);
				
				for(Order reversibleSell : reversibleSells) {
					
					Order reversedOrder = reversibleSell.getReversedOrder();
					Boolean isReversed = reversibleSell.getIsReversed();
					Boolean isFilled = reversibleSell.getIsFilled();
					
					if(reversedOrder != null || (!isReversed && !isFilled) || reversibleSell.getRate() < lastRate.getBuy()+buyThreshold) {
						continue;
					}
					
					Double orderAmount = reversibleSell.getAmount();
					if(reversibleSell.getIsFilled() == false) {
						orderAmount = reversibleSell.getAmount()-reversibleSell.getFilledAmount();
					}
					
					Double buyAmountDiff = buyAmount - orderAmount;
					System.out.println("Buy amount diff: "+buyAmount+" - "+orderAmount+" = "+buyAmountDiff);
					
					if(buyAmountDiff >= 0) {
						
						buyAmount = buyAmount - orderAmount;
						if(reversibleSell.getIsFilled() == true) {
							System.out.println("FULL SELL/BUY REVERSE ON ("+reversibleSell.getOrderId()+"/"+reversibleSell.getAmount()+"/"+reversibleSell.getRate()+"), buyAmount="+buyAmount);
							client.reverseTrade(reversibleSell, false);
						} else {
							System.out.println("FILL UP SELL/BUY REVERSE ON ("+reversibleSell.getOrderId()+"/"+reversibleSell.getAmount()+"/"+reversibleSell.getRate()+"), buyAmount="+buyAmount);
							client.partialReverseTrade(reversibleSell, orderAmount, false);
						}
						
					} else {
						
						System.out.println("PARTIAL SELL/BUY REVERSE ON ("+reversibleSell.getOrderId()+"/"+reversibleSell.getAmount()+"/"+reversibleSell.getRate()+"), buyAmount="+buyAmount);
						client.partialReverseTrade(reversibleSell, buyAmount, false);
						buyAmount = 0.0;
						
					}
					
				}
				
				if(buyAmount > 0) {
					
					System.out.println("NEW BUY ("+buyAmount+"/"+client.actualTradeRate("buy")+")");
					
					Order buyOrder = 
							BtceApi.createOrder(tradingSession.getCurrencyRight()+"_"+tradingSession.getCurrencyLeft(), 
									buyAmount, client.actualTradeRate("buy"), "buy");
				
					buyOrder.setMode("auto");
					buyOrder.setSave(true);
					
					client.trade(buyOrder);
					
				}
				
				tradingSession.setLastMaTransactionRate(lastMaLong);
				mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
						new Update().set("lastMaTransactionRate", tradingSession.getLastMaTransactionRate()), TradingSession.class);
				//tradingSessionRepository.save(tradingSession);
			
			}
			
		}
		
		
	}
	
	private void updateRange() {
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		Double lastRate = tradingSession.getRate().getLast();
		AutoTradingOptions options = tradingSession.getAutoTradingOptions();
		
		Double range = lastRate * 0.5;
		Double rangeBottom = lastRate - range;
		Double rangeTop = lastRate + range;
		
		options.setTradingRangeBottom(rangeBottom);
		options.setTradingRangeTop(rangeTop);
		
		mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
				new Update().set("autoTradingOptions.tradingRangeBottom", rangeBottom), TradingSession.class);
		mongoOps.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(tradingSession.getId()))),
				new Update().set("autoTradingOptions.tradingRangeTop", rangeTop), TradingSession.class);
		
	}

}
