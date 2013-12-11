package com.lenin.tradingplatform.data.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.lenin.tradingplatform.data.entities.Trade;

public interface TradeRepository extends MongoRepository<Trade, String> {
	
	List<Trade> findByOrderId(String orderId);
	
	List<Trade> findByOrderIdAndRateAndAmountAndTime(String orderId, Double rate, Double amount, Long time);
	
	Trade findByTradeId(String tradeId);
	
	List<Trade> findAll();
	
	Trade save(Trade trade);
	
	void delete(Trade trade);
	
	void deleteAll();
	
	
	
}
