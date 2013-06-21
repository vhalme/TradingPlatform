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
import com.lenin.tradingplatform.client.OperationResult;
import com.lenin.tradingplatform.client.TransferClient;
import com.lenin.tradingplatform.data.entities.BitcoinTransaction;
import com.lenin.tradingplatform.data.entities.FundTransaction;
import com.lenin.tradingplatform.data.entities.OkpayTransaction;
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
			
			processPendingTransactions(settings, BitcoinTransaction.class, "transferReqBtce");
			processPendingTransactions(settings, BitcoinTransaction.class, "withdrawalReq");
			
			updateBitcoinTransactions(currency);
			
			processPendingTransactions(settings, BitcoinTransaction.class, "confirmed");
			processPendingTransactions(settings, BitcoinTransaction.class, "confirmedAddBtce");
			processPendingTransactions(settings, BitcoinTransaction.class, "confirmedWithdrawal");
			
			processTransferRequests(currency, "transferReqBtce");
			processTransferRequests(currency, "withdrawalReq");
			
		} else if(currency.equals("usd")) {
			
			getNewOkpayTransactions(settings, currency);
			
			processPendingTransactions(settings, OkpayTransaction.class, "confirmed");
			
		}
		
		
	}
	
	
	private void updateBitcoinTransactions(String currency) {
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		Query searchUsers = new Query(Criteria.where("live").is(true));
		List<User> users = mongoOps.find(searchUsers, User.class);
		
		BitcoinClient client = new BitcoinClient(currency);
		
		Map<String, BitcoinTransaction> txByTxId = new HashMap<String, BitcoinTransaction>();
		List<String> txIds = new ArrayList<String>();
		
		for(User u : users) {
			
			String account = u.getAccountName();
			if(account != null && account.length() > 1) {
				
				OperationResult opResult = client.getTransactions(account, 100, 0);
				List<BitcoinTransaction> transactions = (List<BitcoinTransaction>)opResult.getData();
				
				for(BitcoinTransaction transaction : transactions) {
					
					txIds.add(transaction.getTxId());
					txByTxId.put(transaction.getTxId(), transaction);
					
				}
				
			
			}
			
		}
		
		
		Query searchTransactionsByTxId = new Query(Criteria.where("txId").in(txIds));
		List<BitcoinTransaction> existingTxs = mongoOps.find(searchTransactionsByTxId, BitcoinTransaction.class);
		
		Map<String, BitcoinTransaction> existingTxById = new HashMap<String, BitcoinTransaction>();
		for(BitcoinTransaction existingTx : existingTxs) {
			existingTxById.put(existingTx.getTxId(), existingTx);
		}
		
		for(int i=0; i<txIds.size(); i++) {
			
			String txId = txIds.get(i);
			BitcoinTransaction tx = txByTxId.get(txId);
			
			BitcoinTransaction existingTx = existingTxById.get(txId);
			
			if(existingTx != null) {
				
				System.out.println("Updating existing tx "+existingTx.getId());
				existingTx.setConfirmations(tx.getConfirmations());
				
				if(existingTx.getState().equals("transferBtce") || existingTx.getState().equals("transferReqBtce")) {
					
					if(tx.getConfirmations() >= 6) {
						existingTx.setState("confirmedAddBtce");
						existingTx.setStateInfo("Confirmed by BTC-E. Calculating available BTC-E funds.");
					}
					
				} if(existingTx.getState().equals("withdrawing") || existingTx.getState().equals("withdrawReq")) {
					
					if(tx.getConfirmations() > 0) {
						existingTx.setState("confirmedWithdrawal");
						existingTx.setStateInfo("Withdrawal confirmed and completed");
					}
					
				} else if(existingTx.getState().equals("deposited")) {
					
					if(tx.getConfirmations() > 0) {
						existingTx.setState("confirmed");
						existingTx.setStateInfo("Confirmed with at least 1 confirmation.");
					}
					
				}
				
				mongoOps.save(existingTx);
			
			} else {
				
				System.out.println("Adding new tx");
				tx.setState("deposited");
				tx.setStateInfo("Awaiting network confirmation.");
				mongoOps.insert(tx);
				
			}
				
		}
		
		
	}
	
	private void getNewOkpayTransactions(Settings settings, String currency) {
		
		
		String serviceWalletId = settings.getServiceOkpayWalletId(); //"OK990732954"
		
		Map<String, Long> lastTxTimes = settings.getLastTransactionTimes();
		Long fromTime = lastTxTimes.get(currency);
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		OkpayClient client = new OkpayClient();
		OperationResult opResult = client.getTransactions(fromTime, System.currentTimeMillis()/1000L, serviceWalletId);
		List<FundTransaction> transactions = (List<FundTransaction>)opResult.getData();
		
		if(transactions.size() > 0) {
			
			Long maxTime = fromTime;
			
			for(FundTransaction transaction : transactions) {
				
				Long time = transaction.getTime();
				
				System.out.println(time+" <> "+maxTime);
				
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
	
	
	private void processPendingTransactions(Settings settings, Class transactionClass, String state) {
		
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		Query searchTransactionsByState = new Query(Criteria.where("state").is(state));
		List<FundTransaction> transactions = mongoOps.find(searchTransactionsByState, transactionClass);
		
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
				
				Double txAmount = transaction.getAmount();
				
				String type = transaction.getType();
				String txState = transaction.getState();
				
				String nextState = txState;
				
				if(type.equals("addToBtce")) {
					
					if(txState.equals("confirmedAddBtce")) {
						
						Map<String, Map<String, Double>> activeFunds = user.getActiveFunds();
						Map<String, Double> activeBtceFunds = activeFunds.get("btce");
						Double activeCurrencyFunds = activeBtceFunds.get(currency);
					
						activeCurrencyFunds += txAmount;
					
						activeBtceFunds.put(currency, activeCurrencyFunds);
						activeFunds.put("btce", activeBtceFunds);
						user.setActiveFunds(activeFunds);
						
						nextState = "completed";
						
					} else if(txState.equals("transferReqBtce")) {
					
						currencyFunds -= txAmount;
						userFunds.put(currency, currencyFunds);
						
					}
					
				} else if(type.equals("withdrawal")) {
					
					if(txState.equals("withdrawalReq")) {
						
						currencyFunds -= txAmount;
						userFunds.put(currency, currencyFunds);
					
					} else if(txState.equals("confirmedWithdrawal")) {
						
						nextState = "completed";
						
					}
					
				} else if(type.equals("deposit")) {
					
					
					if(txState.equals("confirmed")) {
						
						currencyFunds += txAmount;
						userFunds.put(currency, currencyFunds);
						
						nextState = "completed";
						
					}
					
				}
				
				transaction.setState(nextState);
				mongoOps.save(transaction);
				
			}
			
			user.setFunds(userFunds);
			
		}
		
		if(users.size() > 0) {
			userRepository.save(users);
		}
		
	}
	
	
	private void processTransferRequests(String currency, String state) {
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		Query searchTransactionsByState = new Query(Criteria.where("state").is(state));
		List<BitcoinTransaction> transactions = mongoOps.find(searchTransactionsByState, BitcoinTransaction.class);
		
		BitcoinClient client = new BitcoinClient(currency);
		
		for(BitcoinTransaction transaction : transactions) {
			
			String nextState = null;
			String nextStateInfo = null;
			
			if(transaction.getType().equals("addToBtce")) {
				nextState = "transferBtce";
				nextStateInfo = "Transfer succeeded. BTC-E requires 6 confirmations.";
			} else if(transaction.getType().equals("addToMtgox")) {
				nextState = "transferMtgox";
				nextStateInfo = "Transfer succeeded. Mt. Gox requires 6 confirmations.";
			} if(transaction.getType().equals("withdrawal")) {
				nextState = "withdrawing";
				nextStateInfo = "Transfer succeeded. Waiting for network confirmation.";
			}
			
			if(nextState != null) {
				
				String account = transaction.getAccount();
				String address = transaction.getAddress();
				Double amount = transaction.getAmount();
			
				OperationResult opResult = client.transferFunds(account, address, amount);
			
				if(opResult.getSuccess() == 1) {
					transaction.setState(nextState);
					transaction.setTxId((String)opResult.getData());
					transaction.setStateInfo(nextStateInfo);
				} else {
					transaction.setState(nextState+"Failed");
					transaction.setStateInfo("Failed to send payment.");
				}
			
				mongoOps.save(transaction);
			
			}
			
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
