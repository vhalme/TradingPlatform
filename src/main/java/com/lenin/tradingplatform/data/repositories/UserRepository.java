package com.lenin.tradingplatform.data.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.lenin.tradingplatform.data.entities.User;

/* User repository */

public interface UserRepository extends MongoRepository<User, String> {
	
	User findByUsername(String username);
	
	User findByUsernameAndAuthToken(String username, String authToken);
	
	List<User> findAll();
	
	User save(User user);
	
	void delete(User user);
	
	void deleteAll();
	
	
	
}
