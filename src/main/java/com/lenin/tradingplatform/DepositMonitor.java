package com.lenin.tradingplatform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.lenin.tradingplatform.client.LitecoinClient;
import com.lenin.tradingplatform.client.Transaction;
import com.lenin.tradingplatform.data.entities.Settings;
import com.lenin.tradingplatform.data.entities.User;
import com.lenin.tradingplatform.data.repositories.UserRepository;

@Service
public class DepositMonitor {
	
	@Autowired
	private MongoTemplate mongoTemplate;
	
	@Autowired
	private UserRepository userRepository;
	
	public DepositMonitor() {
		
	}
	
	
	public void update() {
		update("ltc");
		//update("usd");
	}
	
	
	private void update(String currency) {
		
		// 1. Get the latest transaction time from settings
		
		Settings settings = getSettings(currency);
		Map<String, Long> lastTxTimes = settings.getLastTransactionTimes();
		Long txSince = lastTxTimes.get(currency);
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		// 2. List all NEW transactions from the network, save them 
		//    and make an Account->NewTransactions[] hash.
		//    Finally save the new latest transaction time to the settings.
		
		LitecoinClient ltcClient = new LitecoinClient();
		List<Transaction> transactions = ltcClient.getTransactions(txSince);
		
		Map<String, List<Transaction>> txByAccount = new HashMap<String, List<Transaction>>();
		
		if(transactions.size() > 0) {
			
			Long maxTime = txSince;
			
			for(Transaction transaction : transactions) {
				
				if(transaction.getTime() > maxTime) {
				
					List<Transaction> accountTransactions = txByAccount.get(transaction.getAccount());
					if(accountTransactions == null) {
						accountTransactions = new ArrayList<Transaction>();
						txByAccount.put(transaction.getAccount(), accountTransactions);
					}
				
					accountTransactions.add(transaction);
					
					Long time = transaction.getTime();
					if(time > maxTime) {
						maxTime = time;
					}
					
				}
				
			}
			
			lastTxTimes.put(currency, maxTime);
			settings.setLastTransactionTimes(lastTxTimes);
			
			mongoOps.insertAll(transactions);
			mongoOps.save(settings);
			
		}
		
		
		// 3. Update user funds with amounts in transactions
		
		List<String> accountNames = new ArrayList<String>(txByAccount.keySet());
		
		Query searchUsersByAccounts = new Query(Criteria.where("accountName").in(accountNames));
		List<User> users = mongoOps.find(searchUsersByAccounts, User.class);
		
		for(User user : users) {
			
			Map<String, Double> userFunds = user.getFunds();
			Double fundsLtc = userFunds.get(currency);
			
			List<Transaction> accountTransactions = txByAccount.get(user.getAccountName());
			
			for(Transaction transaction : transactions) {
				fundsLtc += transaction.getAmount();
			}
			
			userFunds.put(currency, fundsLtc);
			user.setFunds(userFunds);
			
		}
		
		if(users.size() > 0) {
			userRepository.save(users);
		}
		
		
	}
	
	
	private Settings getSettings(String currency) {
		
		Long txSince = 0L;
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		List<Settings> settingsResult = mongoOps.findAll(Settings.class);
		
		Settings settings = null;
		
		if(settingsResult.size() > 0) {
			
			settings = settingsResult.get(0);
		
		} else {
			
			settings = new Settings();
			Map<String, Long> lastTxTimes = new HashMap<String, Long>();
			settings.setLastTransactionTimes(lastTxTimes);
		
		}
		
		Map<String, Long> lastTxTimes = settings.getLastTransactionTimes();
		
		txSince = lastTxTimes.get(currency);
		if(txSince == null) {
			txSince = 0L;
			lastTxTimes.put(currency, txSince);
			settings.setLastTransactionTimes(lastTxTimes);
			mongoOps.save(settings);
		}
		
		return settings;
		
	}

	
}
