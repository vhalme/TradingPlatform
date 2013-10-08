package com.lenin.tradingplatform.data.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.lenin.tradingplatform.data.entities.Rate;
import com.lenin.tradingplatform.data.entities.TradingSession;

public interface RateRepository extends MongoRepository<Rate, String> {
	
	List<Rate> findByPairAndTimeGreaterThanOrderByTimeAsc(String pair, Long from);
	
	List<Rate> findByPairAndTimeLessThanOrderByTimeAsc(String pair, Long until);
	
	List<Rate> findByPairAndSetTypeAndTimeBetweenOrderByTimeAsc(String pair, String setType, Long from, Long until);
	
	List<Rate> findByPairAndServiceAndTimeBetweenOrderByTimeAsc(String pair, String service, Long from, Long until);
	
	List<Rate> findByPairAndSetTypeAndTestSessionIdAndTimeBetweenOrderByTimeAsc(String pair, String setType, String testSessionId, Long from, Long until);
	
	List<Rate> findByPairAndTimeBetweenOrderByTimeAsc(String pair, Long from, Long until);
	
	List<Rate> findAll();
	
	Rate save(Rate rate);
	
	void delete(Rate rate);
	
	void deleteAll();
	
	
	
}
