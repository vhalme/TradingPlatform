package com.lenin.tradingplatform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.endpoint.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.lenin.tradingplatform.client.BitcoinClient;
import com.lenin.tradingplatform.client.OkpayClient;
import com.lenin.tradingplatform.client.FundTransaction;
import com.lenin.tradingplatform.client.OperationResult;
import com.lenin.tradingplatform.client.TransferClient;
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
		
		System.out.println("Update deposits");
		
		update("ltc");
		update("usd");
	
		System.out.println("Done updating deposits");
		
	}
	
	
	private void update(String currency) {
		
		System.out.println("Updating "+currency);
		
		Settings settings = getSettings(currency);
		System.out.println("Settings: "+settings);
		
		Map<String, Long> lastTxTimes = settings.getLastTransactionTimes();
		
		
		TransferClient client = null;
		
		if(currency.equals("btc") || currency.equals("ltc")) {
			client = new BitcoinClient(currency);
		} else if(currency.equals("usd")) {
			client = new OkpayClient();
		}
		
		if(client != null) {
			
			System.out.println("Getting new "+currency+" transactions");
			getNewTransactions(settings, currency, client);
			
			System.out.println("Redirecting "+currency+" transactions");
			redirectTransactions(settings, client);
			
			System.out.println("Processing redirected "+currency+" transactions");
			processRedirectedTransactions(settings);
			
		}
		
	}
	
	
	
	private void getNewTransactions(Settings settings, String currency, TransferClient client) {
		
		String serviceWalletId = settings.getServiceOkpayWalletId(); //"OK990732954"
		
		Map<String, Long> lastTxTimes = settings.getLastTransactionTimes();
		Long fromTime = lastTxTimes.get(currency);
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		OperationResult opResult = client.getTransactions(0L, System.currentTimeMillis(), serviceWalletId);
		List<FundTransaction> transactions = (List<FundTransaction>)opResult.getData();
		
		if(transactions.size() > 0) {
			
			Long maxTime = fromTime;
			
			for(FundTransaction transaction : transactions) {
					
				Long time = transaction.getTime();
				if(time > maxTime) {
					maxTime = time;
				}

			}
			
			lastTxTimes.put(currency, maxTime);
			settings.setLastTransactionTimes(lastTxTimes);
			
			mongoOps.insertAll(transactions);
			mongoOps.save(settings);
			
		}
		
	}
	
	private void redirectTransactions(Settings settings, TransferClient client) {
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		Query searchTransactionsByState = new Query(Criteria.where("state").is("deposited"));
		List<FundTransaction> transactions = mongoOps.find(searchTransactionsByState, FundTransaction.class);
		
		String fromWalletId = settings.getServiceOkpayWalletId();
		String toWalletId = settings.getBtceOkpayWalletId();
		
		for(FundTransaction transaction : transactions) {
			
			Double amount = transaction.getAmount();
			
			OperationResult opResult = client.transferFunds(fromWalletId, toWalletId, amount);
			
			if(opResult.getSuccess() == 1) {
				transaction.setState("redirected");
				transaction.setStateInfo("Redirection succeeded");
			} else {
				transaction.setStateInfo("Redirection failed");
			}
			
			mongoOps.save(transaction);
			
		}
		
	
	}
	
	
	private void processRedirectedTransactions(Settings settings) {
		
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		Query searchTransactionsByState = new Query(Criteria.where("state").is("redirected"));
		List<FundTransaction> transactions = mongoOps.find(searchTransactionsByState, FundTransaction.class);
		
		Map<String, List<FundTransaction>> txByAccount = new HashMap<String, List<FundTransaction>>();
		
		for(FundTransaction transaction : transactions) {
				
			List<FundTransaction> accountTransactions = txByAccount.get(transaction.getAccount());
			if(accountTransactions == null) {
				accountTransactions = new ArrayList<FundTransaction>();
				txByAccount.put(transaction.getAccount(), accountTransactions);
			}
				
			accountTransactions.add(transaction);			
				
		}
		
		List<String> accountNames = new ArrayList<String>(txByAccount.keySet());
		
		Query searchUsersByAccounts = new Query(Criteria.where("accountName").in(accountNames));
		List<User> users = mongoOps.find(searchUsersByAccounts, User.class);
		
		for(User user : users) {
			
			Map<String, Double> userFunds = user.getFunds();
			
			List<FundTransaction> accountTransactions = txByAccount.get(user.getAccountName());
			
			for(FundTransaction transaction : transactions) {
				String currency = transaction.getCurrency();
				Double currencyFunds = userFunds.get(currency);
				currencyFunds += transaction.getAmount();
				userFunds.put(currency, currencyFunds);
			}
			
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
		if(lastTxTimes == null) {
			lastTxTimes = new HashMap<String, Long>();
		}
		
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
