package com.lenin.tradingplatform.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.lenin.tradingplatform.data.entities.BitcoinTransaction;
import com.lenin.tradingplatform.data.entities.BitcoinTransaction;
import com.lenin.tradingplatform.data.entities.FundTransaction;
import com.lenin.tradingplatform.data.entities.FundTransaction;
import com.lenin.tradingplatform.data.entities.User;

public class BitcoinClient {
	
	
	private String currency;
	
	public BitcoinClient(String currency) {
		
		this.currency = currency;
		
	}
	
	public OperationResult getTransactions(String account, int number, int from) {
		
		OperationResult opResult = new OperationResult();
		
		List<FundTransaction> transactions = new ArrayList<FundTransaction>();
		
		BitcoinApi api = createBitcoinApi(currency);
		
		List<Object> params = new ArrayList<Object>();
		params.add(account);
		//params.add(""+number);
		//params.add(""+from);
		
		JSONObject result = api.exec("listtransactions", params);
		
		try {
			
			JSONArray data = result.getJSONArray("result");
			System.out.println(data);
			
			for(int i=0; i<data.length(); i++) {
				
				JSONObject txJson = data.getJSONObject(i);
				
				BitcoinTransaction	transaction = new BitcoinTransaction();
				transaction.setType("deposit");
				transaction.setCurrency(currency);
				transaction.setTxId(txJson.getString("txid"));
				transaction.setAccount(txJson.getString("account"));
				transaction.setAddress(txJson.getString("address"));
				transaction.setAmount(txJson.getDouble("amount"));
				transaction.setConfirmations(txJson.getInt("confirmations"));
				transaction.setTime(txJson.getLong("time"));
				transaction.setCategory(txJson.getString("category"));
				
				System.out.println("transaction for "+transaction.getAmount()+" detected for "+transaction.getAccount()+" at "+transaction.getTime());
				//System.out.println(transaction.getTime()+" >= "+fromTime);
				
				transactions.add(transaction);
				
			}
			
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		opResult.setData(transactions);
		
		return opResult;
		
	}
	
	
	public OperationResult transferFunds(String fromAccount, String toAddress, Double amount) {
		
		OperationResult opResult = new OperationResult();
		
		BitcoinApi api = createBitcoinApi(currency);
		
		List<Object> params = new ArrayList<Object>();
		params.add(fromAccount);
		params.add(toAddress);
		params.add(amount);
		
		JSONObject result = api.exec("sendfrom", params);
		
		if(result != null) {
			
			try {
				
				String txId = result.getString("result");
				
				opResult.setData(txId);
				
			} catch(Exception e) {
				e.printStackTrace();
			}
		
		}
		
		/*
		try {
			
			JSONArray data = result.getJSONArray("result");
			System.out.println(data);
			
			for(int i=0; i<data.length(); i++) {
				
				JSONObject txJson = data.getJSONObject(i);
				
				BitcoinTransaction	transaction = new BitcoinTransaction();
				transaction.setType("deposit");
				transaction.setCurrency(currency);
				transaction.setTxId(txJson.getString("txid"));
				transaction.setAccount(txJson.getString("account"));
				transaction.setAddress(txJson.getString("address"));
				transaction.setAmount(txJson.getDouble("amount"));
				transaction.setConfirmations(txJson.getInt("confirmations"));
				transaction.setTime(txJson.getLong("time"));
				transaction.setCategory(txJson.getString("category"));
				
				System.out.println("transaction for "+transaction.getAmount()+" detected for "+transaction.getAccount()+" at "+transaction.getTime());
				//System.out.println(transaction.getTime()+" >= "+fromTime);
				
				transactions.add(transaction);
				
			}
			
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		*/
		
		opResult.setSuccess(1);
		return opResult;
		
	}
	
	private BitcoinApi createBitcoinApi(String currency) {
		
		BitcoinApi api = null;
		
		if(currency.equals("ltc")) {
			api = new BitcoinApi("127.0.0.1", 8332, "fluxltc1", "fLuxThuyu1eP");
		}
		
		return api;
		
	}

}
