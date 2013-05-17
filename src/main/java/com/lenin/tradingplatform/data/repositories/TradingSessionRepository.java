package com.lenin.tradingplatform.data.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.lenin.tradingplatform.data.entities.TradingSession;

public interface TradingSessionRepository extends MongoRepository<TradingSession, String> {
	
	List<TradingSession> findAll();
	
	TradingSession save(TradingSession tradingSession);
	
	void delete(TradingSession tradingSession);
	
	void deleteAll();
	
	
	
}
