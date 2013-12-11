package com.lenin.tradingplatform;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.lenin.tradingplatform.client.BtceApi;
import com.lenin.tradingplatform.client.MtgoxApi;
import com.lenin.tradingplatform.data.entities.ErrorMessage;
import com.lenin.tradingplatform.data.entities.User;
import com.lenin.tradingplatform.data.repositories.OrderRepository;
import com.lenin.tradingplatform.data.repositories.UserRepository;

@Service
public abstract class UserInfoProcess extends ExchangeApiProcess {
	
	@Autowired
	private OrderRepository orderRepository;
	
	@Autowired
	private UserRepository userRepository;
	
	public void init() {
		
		
		
	}
	
	
	public void update() {
		
		try {
			
			long start = System.currentTimeMillis();
			
			System.out.println("Updating user infos");
			updateUsers();
			
			long finish = System.currentTimeMillis();
		
			long time = finish-start;
		
			System.out.println("["+(new Date())+"] Update pass complete in "+time/1000+" s");
		
		} catch(Exception e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	
	protected abstract List<ErrorMessage> updateUserInfo(User user) throws Exception;
	
	
	private void updateUsers() throws Exception {
		
		Long tradeUpdateStart = System.currentTimeMillis();
		
		Query usersQuery = new Query(Criteria.where("live").is(true).and("deleted").is(false));
		List<User> users = mongoTemplate.find(usersQuery, User.class);
		
		//List<User> users = userRepository.findAll();
		
		for(User user : users) {
			
			Long userUpdateStart = System.currentTimeMillis();
			
			System.out.println("Updating user info for "+user.getUsername());
			
			if(user.getLive() == true && user.getDeleted() == false) {
				
				String service = getService();
				
				System.out.println("UPDATING "+user.getUsername()+" FOR "+service);
				
				resetSessionErrors(user, service);
				
				Boolean keyValid = checkApiKeyValidity(user, service);
				
				List<ErrorMessage> sessionErrors = user.getSessionErrors().get(service);
				
				if(keyValid == true) {
					
					List<ErrorMessage> userInfoErrors = new ArrayList<ErrorMessage>();
					
					try {
					
						userInfoErrors = updateUserInfo(user);
						
					} catch(Exception e) {
						e.printStackTrace();
					}
					
					sessionErrors.addAll(userInfoErrors);
				
				}
				
				System.out.println("set sessionErrors."+service+" with "+sessionErrors.size()+" errors");
				mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(user.getId()))),
						new Update().set("sessionErrors."+service, sessionErrors), User.class);
				
				if(sessionErrors.size() > 0) {
					
					mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(user.getId()))),
						new Update().set("tradeOk."+service, false), User.class);
				
				} else {
					
					mongoTemplate.updateFirst(new Query(Criteria.where("_id").is(new ObjectId(user.getId()))),
							new Update().set("tradeOk."+service, true), User.class);
				
				}
				
			}
			
			Long userUpdateTime = (System.currentTimeMillis() - userUpdateStart)/1000L;
			System.out.println("User info update for "+user.getUsername()+" completed in "+userUpdateTime+" s.");
			
		}
		
		Long tradeUpdateTime = (System.currentTimeMillis() - tradeUpdateStart)/1000L;
		System.out.println("Update user info cycle completed in "+tradeUpdateTime+" s.");
		
	}
	
	private void resetSessionErrors(User user, String service) {
		
		Map<String, List<ErrorMessage>> sessionErrors = user.getSessionErrors();
		List<ErrorMessage> errors = sessionErrors.get(service);
		sessionErrors.put(service, new ArrayList<ErrorMessage>());
		
	}
	
	
	private Boolean checkApiKeyValidity(User user, String service) {
		
		String accountKey = null;
		String accountSecret = null;
		
		if(service.equals("btce")) {
			BtceApi btceApi = apiFactory.createBtceApi(user);
			accountKey = btceApi.getKey();
			accountSecret = btceApi.getSecret();
		} else if(service.equals("mtgox")) {
			MtgoxApi mtgoxApi = apiFactory.createMtgoxApi(user);
			accountKey = mtgoxApi.getKey();
			accountSecret = mtgoxApi.getSecret();
		}
		
		
		if(accountKey == null || accountKey.length() < 32) {
			
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setMessage("invalid api key");
			errorMessage.setCode(0);
			user.getSessionErrors().get(service).add(errorMessage);
			
			return false;
			
		} else if(accountSecret == null || accountSecret.length() < 32) {
			
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setMessage("invalid sign");
			errorMessage.setCode(0);
			user.getSessionErrors().get(service).add(errorMessage);
				
			return false;
			
		} else {
			
			return true;
		
		}
		
		
		
	}

	
	
}
