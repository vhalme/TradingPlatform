package com.lenin.tradingplatform.client;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class LitecoinClient {
	
	public LitecoinClient() {
		
	}
	
	public void checkPayment(String currency, String address) {
		
		
		BitcoinApi api = createBitcoinApi(currency);
		
		List<String> params = new ArrayList<String>();
		params.add(address);
		
		JSONObject result = api.exec("listreceivedbyaddress", params);
		
		try {
			
			System.out.println(result.get("result"));
		
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	
	public List<Transaction> getTransactions() {
		
		List<Transaction> transactions = new ArrayList<Transaction>();
		
		BitcoinApi api = createBitcoinApi("ltc");
		
		List<String> params = new ArrayList<String>();
		JSONObject result = api.exec("listtransactions", params);
		
		try {
			
			JSONArray data = result.getJSONArray("result");
			System.out.println(data);
			
			for(int i=0; i<data.length(); i++) {
				
				JSONObject txJson = data.getJSONObject(i);
				
				Transaction transaction = new Transaction();
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
		
		return transactions;
		
	}
	
	
	private BitcoinApi createBitcoinApi(String currency) {
		
		BitcoinApi api = null;
		
		if(currency.equals("ltc")) {
			api = new BitcoinApi("127.0.0.1", 8332, "fluxltc1", "fLuxThuyu1eP");
		}
		
		return api;
		
	}

}
