package com.lenin.tradingplatform;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.lenin.tradingplatform.client.TradingClient;
import com.lenin.tradingplatform.data.entities.ErrorMessage;
import com.lenin.tradingplatform.data.entities.Rate;
import com.lenin.tradingplatform.data.entities.User;
import com.lenin.tradingplatform.data.repositories.OrderRepository;
import com.lenin.tradingplatform.data.repositories.RateRepository;
import com.lenin.tradingplatform.data.repositories.TradeRepository;
import com.lenin.tradingplatform.data.repositories.TradingSessionRepository;
import com.lenin.tradingplatform.data.repositories.UserRepository;

@Service
public abstract class OrderProcess extends ExchangeApiProcess {
	
	
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
	
	
	public void init() {
		
		
	}
	
	
	public void update() {
		
		try {
			
			long start = System.currentTimeMillis();
			
			System.out.println("Updating orders");
			updateUsers();
			
			long finish = System.currentTimeMillis();
		
			long time = finish-start;
		
			System.out.println("["+(new Date())+"] Update pass complete in "+time/1000+" s");
		
		} catch(Exception e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	
	private void updateUsers() throws Exception {
		
		Long tradeUpdateStart = System.currentTimeMillis();
		
		String service = getService();
		
		Query usersQuery = null;
		
		if(service != null) {
			
			usersQuery = new Query(Criteria.where("deleted").is(false).andOperator(
					Criteria.where("live").is(true),
					Criteria.where("tradeOk."+service).is(true)
				));
		
		} else {
			
			usersQuery = new Query(Criteria.where("deleted").is(false).andOperator(
					Criteria.where("live").is(false),
					Criteria.where("loggedIn").is(true)
				));
			
		}
		
		List<User> users = mongoTemplate.find(usersQuery, User.class);
		
		for(User user : users) {
			
			Long userUpdateStart = System.currentTimeMillis();
			
			System.out.println("Updating data for "+user.getUsername());
			
			updateOrders(user);
			
			Long userUpdateTime = (System.currentTimeMillis() - userUpdateStart)/1000L;
			System.out.println("Trades and orders update for "+user.getUsername()+" completed in "+userUpdateTime+" s.");
			
		}
		
		Long tradeUpdateTime = (System.currentTimeMillis() - tradeUpdateStart)/1000L;
		System.out.println("Update trades and orders cycle completed in "+tradeUpdateTime+" s.");
		
		
	}

	
	protected abstract void updateOrders(User user);
	
	
	
}
