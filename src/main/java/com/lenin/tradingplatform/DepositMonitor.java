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

import com.lenin.tradingplatform.client.BitcoinClient;
import com.lenin.tradingplatform.client.OkpayClient;
import com.lenin.tradingplatform.client.FundTransaction;
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
		updateBitcoin("ltc");
		updateOkpay();
	}
	
	
	private void updateBitcoin(String currency) {
		
		Settings settings = getSettings(currency);
		Map<String, Long> lastTxTimes = settings.getLastTransactionTimes();
		Long txSince = lastTxTimes.get(currency);
		
		BitcoinClient client = new BitcoinClient(currency);
		List<FundTransaction> transactions = client.getTransactions(txSince);
		
		processTransactions(transactions, settings, currency);
		
		
	}
	
	
	private void updateOkpay() {
		
		Settings settings = getSettings("usd");
		
		OkpayClient client = new OkpayClient();
		
		List<FundTransaction> transactions = client.getTransactions(0L);
		
		processTransactions(transactions, settings, "usd");
		
	}
	
	
	private void processTransactions(List<FundTransaction> transactions, Settings settings, String currency) {
		
		Map<String, Long> lastTxTimes = settings.getLastTransactionTimes();
		Long txSince = lastTxTimes.get(currency);
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		Map<String, List<FundTransaction>> txByAccount = new HashMap<String, List<FundTransaction>>();
		
		if(transactions.size() > 0) {
			
			Long maxTime = txSince;
			
			for(FundTransaction transaction : transactions) {
				
				if(transaction.getTime() > maxTime) {
				
					List<FundTransaction> accountTransactions = txByAccount.get(transaction.getAccount());
					if(accountTransactions == null) {
						accountTransactions = new ArrayList<FundTransaction>();
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
		
		List<String> accountNames = new ArrayList<String>(txByAccount.keySet());
		
		Query searchUsersByAccounts = new Query(Criteria.where("accountName").in(accountNames));
		List<User> users = mongoOps.find(searchUsersByAccounts, User.class);
		
		for(User user : users) {
			
			Map<String, Double> userFunds = user.getFunds();
			Double fundsLtc = userFunds.get(currency);
			
			List<FundTransaction> accountTransactions = txByAccount.get(user.getAccountName());
			
			for(FundTransaction transaction : transactions) {
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
