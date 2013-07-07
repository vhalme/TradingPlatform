package com.lenin.tradingplatform.data.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.lenin.tradingplatform.data.entities.Order;
import com.lenin.tradingplatform.data.entities.TradingSession;

public interface OrderRepository extends MongoRepository<Order, String> {
	
	Order findByOrderId(String orderId);
	
	Order findByReversedOrder(Order reversedOrder);
	
	List<Order> findByTradingSession(TradingSession tradingSession);
	
	List<Order> findByTradingSessionAndType(TradingSession tradingSession, String type);
	
	List<Order> findByTradingSessionIdAndType(String id, String type);
	
	List<Order> findAll();
	
	Order save(Order order);
	
	void delete(Order order);
	
	void deleteAll();
	
	
	
}
