package com.lenin.tradingplatform.client;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class BitcoinClient implements TransferClient {
	
	
	private String currency;
	
	public BitcoinClient(String currency) {
		this.currency = currency;
	}
	
	public OperationResult getTransactions(Long fromTime, Long untilTime, String sourceId) {
		
		OperationResult opResult = new OperationResult();
		
		List<FundTransaction> transactions = new ArrayList<FundTransaction>();
		
		BitcoinApi api = createBitcoinApi(currency);
		
		List<String> params = new ArrayList<String>();
		//params.add("");
		//params.add("9999999");
		params.add(""+fromTime);
		
		JSONObject result = api.exec("listtransactions", params);
		
		try {
			
			JSONArray data = result.getJSONArray("result");
			System.out.println(data);
			
			for(int i=0; i<data.length(); i++) {
				
				JSONObject txJson = data.getJSONObject(i);
				
				BitcoinTransaction	transaction = new BitcoinTransaction();
				transaction.setType("deposit");
				transaction.setState("deposited");
				transaction.setCurrency(currency);
				transaction.setAccount(txJson.getString("account"));
				transaction.setAddress(txJson.getString("address"));
				transaction.setAmount(txJson.getDouble("amount"));
				transaction.setConfirmations(txJson.getInt("confirmations"));
				transaction.setTime(txJson.getLong("time"));
				
				System.out.println("transaction for "+transaction.getAmount()+" detected for "+transaction.getAccount());
				transactions.add(transaction);
				
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		/*
		Transaction transaction = new Transaction();
		transaction.setAccount("GJNDE1369224905477");
		transaction.setAddress("LXKw8jkYiy9VX95qTpeLRy66VYK5SRrR2u");
		transaction.setAmount(207.0);
		transaction.setConfirmations(1);
		transaction.setTime(1723987199L);
		transactions.add(transaction);
		*/
		
		opResult.setData(transactions);
		
		return opResult;
		
	}
	
	
	public OperationResult transferFunds(String fromId, String toId, Double amount) {
		
		OperationResult opResult = new OperationResult();
		
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
