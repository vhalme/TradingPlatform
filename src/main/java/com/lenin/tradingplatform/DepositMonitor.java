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

import com.lenin.tradingplatform.client.BitcoinApi;
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
		
		update("btc");
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
			
			updateBitcoinTransactions(settings, currency);
			
			processPendingTransactions(settings, BitcoinTransaction.class, "confirmed");
			processPendingTransactions(settings, BitcoinTransaction.class, "confirmedAddBtce");
			processPendingTransactions(settings, BitcoinTransaction.class, "confirmedWithdrawal");
			
			processBitcoinTransferRequests(currency, "readyTransferBtce");
			processBitcoinTransferRequests(currency, "readyWithdraw");
			
			processPendingTransactions(settings, BitcoinTransaction.class, "completedFailed");
			
		} else if(currency.equals("usd")) {
			
			System.out.println("process pending withdrawals");
			processPendingTransactions(settings, OkpayTransaction.class, "withdrawalReq");
			
			System.out.println("get new transactions");
			getNewOkpayTransactions(settings, currency);
			
			System.out.println("process pending confirmed");
			processPendingTransactions(settings, OkpayTransaction.class, "confirmed");
			processPendingTransactions(settings, OkpayTransaction.class, "confirmedAddBtce");
			processPendingTransactions(settings, OkpayTransaction.class, "confirmedWithdrawalReq");
			
			System.out.println("process transfer reqs");
			processOkpayTransferRequests(currency, "readyWithdraw");
			
			System.out.println("process pending failed");
			processPendingTransactions(settings, BitcoinTransaction.class, "completedFailed");
			
		}
		
		
	}
	
	
	private void updateBitcoinTransactions(Settings settings, String currency) {
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		Map<String, String> lastBlockHashes = settings.getLastBlockHashes();
		String lastBlockHash = lastBlockHashes.get(currency);
		
		Query searchUsers = new Query(Criteria.where("live").is(true));
		List<User> users = mongoOps.find(searchUsers, User.class);
		
		BitcoinClient client = new BitcoinClient(currency);
		
		Map<String, BitcoinTransaction> txByTxId = new HashMap<String, BitcoinTransaction>();
		List<String> txIds = new ArrayList<String>();
		
		OperationResult opResult = client.getTransactions(lastBlockHash);
		List<BitcoinTransaction> transactions = (List<BitcoinTransaction>)opResult.getData();
		
		for(BitcoinTransaction transaction : transactions) {
			
			txIds.add(transaction.getTxId());
			txByTxId.put(transaction.getTxId(), transaction);
			
		}
		
		/*
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
		*/
		
		Query searchTransactionsByTxId = new Query(Criteria.where("txId").in(txIds));
		List<BitcoinTransaction> existingTxs = mongoOps.find(searchTransactionsByTxId, BitcoinTransaction.class);
		
		Map<String, BitcoinTransaction> existingTxById = new HashMap<String, BitcoinTransaction>();
		for(BitcoinTransaction existingTx : existingTxs) {
			existingTxById.put(existingTx.getTxId(), existingTx);
		}
		
		int existingCount = 0;
		int newCount = 0;
		
		Long earliestUnconfirmedTime = 9999999999999999L;
		Long earliestConfirmedTime = 9999999999999999L;
		BitcoinTransaction earliestConfirmedTx = null;
		BitcoinTransaction earliestUnconfirmedTx = null;
		
		for(int i=0; i<txIds.size(); i++) {
			
			String txId = txIds.get(i);
			BitcoinTransaction tx = txByTxId.get(txId);
			
			BitcoinTransaction existingTx = existingTxById.get(txId);
			
			if(existingTx != null) {
				
				//System.out.println("Updating existing tx "+existingTx.getId());
				
				int txConfirmations = tx.getConfirmations();
				int existingConfirmations = existingTx.getConfirmations();
				
				String txCurrency = tx.getCurrency();
				
				if(existingTx.getState().equals("transferBtce") || existingTx.getState().equals("transferReqBtce")) {
					
					int requiredConfirmations = 0;
					if(txCurrency.equals("btc")) {
						requiredConfirmations = 3;
					} else if(txCurrency.equals("ltc")) {
						requiredConfirmations = 6;
					}
					
					existingTx.setStateInfo("Waiting for "+txConfirmations+"/"+requiredConfirmations+" confirmations.");
					
					if((txCurrency.equals("ltc") && txConfirmations >= 6 && existingConfirmations < 6) ||
						(txCurrency.equals("btc") && txConfirmations >= 3 && existingConfirmations < 3)) {
						
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
				
				existingTx.setConfirmations(txConfirmations);
				
				if(tx.getBlockHash() != null && existingTx.getBlockHash() == null) {
					existingTx.setBlockHash(tx.getBlockHash());
				}
				
				if(tx.getTime() != null && existingTx.getTime() == null) {
					existingTx.setTime(tx.getTime());
				}
				
				if(tx.getCategory() != null && existingTx.getCategory() == null) {
					existingTx.setCategory(tx.getCategory());
				}
				
				if(existingTx.getState().startsWith("confirmed")) {
					if(existingTx.getSystemTime() < earliestConfirmedTime) {
						earliestConfirmedTime = existingTx.getSystemTime();
						earliestConfirmedTx = existingTx;
					}
				} else {
					if(existingTx.getSystemTime() < earliestUnconfirmedTime && existingTx.getBlockHash() != null) {
						earliestUnconfirmedTime = existingTx.getSystemTime();
						earliestUnconfirmedTx = existingTx;
					}
				}
				
				
				
				mongoOps.save(existingTx);
				
				existingCount++;
				
			} else {
				
				//System.out.println("Adding new tx");
				
				tx.setState("deposited");
				tx.setStateInfo("Awaiting network confirmation.");
				tx.setSystemTime(System.currentTimeMillis());
				
				mongoOps.insert(tx);
				
				newCount++;
				
			}
			
		}
		
		if(earliestConfirmedTx != null && earliestConfirmedTx.getBlockHash() != null) {
			
			Boolean updateHash = false;
			
			String confirmedHash = earliestConfirmedTx.getBlockHash();
			
			if(earliestConfirmedTime < earliestUnconfirmedTime) {
				
				if(earliestUnconfirmedTx == null || 
						(earliestUnconfirmedTx.getBlockHash() != null && !confirmedHash.equals(earliestUnconfirmedTx.getBlockHash())) ||
						(earliestUnconfirmedTx.getBlockHash() == null && (earliestUnconfirmedTx.getTime()-earliestConfirmedTx.getTime()) > 360)) { 
			
					lastBlockHashes.put(currency, earliestConfirmedTx.getBlockHash());
					settings.setLastBlockHashes(lastBlockHashes);
					mongoOps.save(settings);
				
				}
				
				
			}
			
		}
		
		System.out.println("updateBitcoinTransactions: "+existingCount+" existing, "+newCount+" new");
		
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
				
				//System.out.println(time+" <> "+maxTime);
				
				if(time > maxTime) {
					maxTime = time;
				}

			}
			
			lastTxTimes.put(currency, maxTime);
			settings.setLastTransactionTimes(lastTxTimes);
			
			mongoOps.insertAll(transactions);
			mongoOps.save(settings);
			
		}
		
		System.out.println("getNewOkpayTransactions: "+transactions.size()+" new");
		
		
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
				
				if(type.equals("addToBtce") || type.equals("returnFromBtce")) {
					
					Map<String, Map<String, Double>> activeFunds = user.getActiveFunds();
					Map<String, Double> activeBtceFunds = activeFunds.get("btce");
					Double activeCurrencyFunds = activeBtceFunds.get(currency);
					
					if(txState.equals("completedFailed")) {
						
						if(type.equals("addToBtce")) {
							
							currencyFunds += txAmount;
							userFunds.put(currency, currencyFunds);
						
							nextState = "failedReimbursed";
					
						}
						
					} else if(type.equals("addToBtce") && txState.equals("confirmedAddBtce")) {
						
						Double txFee = BitcoinApi.getFee(currency);
						
						activeCurrencyFunds += (txAmount-txFee);
						
						activeBtceFunds.put(currency, activeCurrencyFunds);
						activeFunds.put("btce", activeBtceFunds);
						user.setActiveFunds(activeFunds);
						
						nextState = "completed";
						
					} else if(type.equals("returnFromBtce") && txState.equals("transferReqBtce")) {
						
						activeCurrencyFunds -= txAmount;
						
						activeBtceFunds.put(currency, activeCurrencyFunds);
						activeFunds.put("btce", activeBtceFunds);
						user.setActiveFunds(activeFunds);
						
						nextState = "readyTransferBtce";
						
					} else if(type.equals("addToBtce") && txState.equals("transferReqBtce")) {
					
						currencyFunds -= txAmount;
						userFunds.put(currency, currencyFunds);
						
						nextState = "readyTransferBtce";
						
					}
					
				} else if(type.equals("withdrawal")) {
					
					if(txState.equals("withdrawalReq")) {
						
						currencyFunds -= txAmount;
						userFunds.put(currency, currencyFunds);
						
						nextState = "readyWithdraw";
					
					} else if(txState.equals("completedFailed")) {
						
						currencyFunds += txAmount;
						userFunds.put(currency, currencyFunds);
						
						nextState = "failedReimbursed";
					
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
	
	
	private void processBitcoinTransferRequests(String currency, String state) {
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		Query searchTransactionsByState = new Query(Criteria.where("state").is(state).andOperator(Criteria.where("currency").is(currency)));
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
					transaction.setState("completedFailed");
					transaction.setStateInfo("Failed to send payment.");
				}
			
				mongoOps.save(transaction);
			
			}
			
		}
		
	
	}
	
	
	private void processOkpayTransferRequests(String currency, String state) {
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		
		Query searchTransactionsByState = new Query(Criteria.where("state").is(state));
		List<OkpayTransaction> transactions = mongoOps.find(searchTransactionsByState, OkpayTransaction.class);
		
		OkpayClient client = new OkpayClient();
		
		for(OkpayTransaction transaction : transactions) {
			
			String nextState = null;
			String nextStateInfo = null;
			
			if(transaction.getType().equals("withdrawal")) {
				nextState = "completed";
				nextStateInfo = "Transfer succeeded. Waiting for network confirmation.";
			}
			
			if(nextState != null) {
				
				String account = transaction.getAccount();
				String fromWalletId = transaction.getSenderWalletId();
				String toWalletId = transaction.getReceiverWalletId();
				Double amount = transaction.getAmount();
			
				OperationResult opResult = client.transferFunds(fromWalletId, toWalletId, account, amount);
				
				if(opResult.getSuccess() == 1) {
					transaction.setState(nextState);
					transaction.setStateInfo(nextStateInfo);
				} else {
					transaction.setState("completedFailed");
					transaction.setStateInfo("Failed to send payment.");
				}
			
				mongoOps.save(transaction);
			
			}
			
		}
		
	
	}
	

	private Settings getSettings(String currency) {
		
		
		Long lastTxTime = 0L;
		String lastBlockHash = "";
		
		MongoOperations mongoOps = (MongoOperations)mongoTemplate;
		List<Settings> settingsResult = mongoOps.findAll(Settings.class);
		
		Settings settings = null;
		
		if(settingsResult.size() > 0) {
			
			settings = settingsResult.get(0);
		
		} else {
			
			settings = new Settings();
			Map<String, Long> lastTxTimes = new HashMap<String, Long>();
			Map<String, String> lastBlockHashes = new HashMap<String, String>();
			settings.setLastTransactionTimes(lastTxTimes);
			settings.setLastBlockHashes(lastBlockHashes);
		
		}
		
		Map<String, Long> lastTxTimes = settings.getLastTransactionTimes();
		Map<String, String> lastBlockHashes = settings.getLastBlockHashes();
		
		if(currency.equals("usd")) {
			
			if(lastTxTimes == null) {
				lastTxTimes = new HashMap<String, Long>();
			}
			
			lastTxTime = lastTxTimes.get(currency);
		
			if(lastTxTime == null) {
				lastTxTime = 0L;
				lastTxTimes.put(currency, lastTxTime);
				settings.setLastTransactionTimes(lastTxTimes);	
			}
		
			mongoOps.save(settings);
			
		} else if(currency.equals("ltc") || currency.equals("btc")) {
			
			if(lastBlockHashes == null) {
				lastBlockHashes = new HashMap<String, String>();
			}
			
			lastBlockHash = lastBlockHashes.get(currency);
			
			if(lastBlockHash == null) {
				lastBlockHash = "";
				lastBlockHashes.put(currency, lastBlockHash);
				settings.setLastBlockHashes(lastBlockHashes);
			}
			
			mongoOps.save(settings);
			
		}
		
		
		return settings;
		
	}


}
