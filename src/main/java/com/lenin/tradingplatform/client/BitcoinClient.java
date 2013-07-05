package com.lenin.tradingplatform.client;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.lenin.tradingplatform.data.entities.BitcoinTransaction;
import com.lenin.tradingplatform.data.entities.FundTransaction;


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
		//params.add(account);
		//params.add(""+number);
		//params.add(""+from);
		
		JSONObject result = api.exec("listtransactions", params);
		//System.out.println(result);
		
		try {
			
			JSONArray data = result.getJSONArray("result");
			//System.out.println(data);
			
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
				
				//System.out.println("transaction for "+transaction.getAmount()+" detected for "+transaction.getAccount()+" at "+transaction.getTime());
				//System.out.println(transaction.getTime()+" >= "+fromTime);
				
				transactions.add(transaction);
				
			}
			
			opResult.setSuccess(1);
			opResult.setData(transactions);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return opResult;
		
	}
	
	
	public OperationResult transferFunds(String fromAccount, String toAddress, Double amount) {
		
		OperationResult opResult = new OperationResult();
		opResult.setSuccess(0);
		
		BitcoinApi api = createBitcoinApi(currency);
		
		amount = amount - api.getTransferFee();
		
		List<Object> params = new ArrayList<Object>();
		params.add(fromAccount);
		params.add(toAddress);
		params.add(amount);
		
		JSONObject result = api.exec("sendfrom", params);
		
		if(result != null) {
			
			try {
				
				String txId = result.getString("result");
				System.out.println("Transfer result txId: "+txId);
				
				if(txId != null && !txId.equals("null")) {
					opResult.setData(txId);
					opResult.setSuccess(1);
				} else {
					opResult.setSuccess(-1);
				}
				
			} catch(Exception e) {
				e.printStackTrace();
			}
		
		} else {
			
			opResult.setSuccess(-2);
			
		}
		
		
		return opResult;
		
	}
	
	private BitcoinApi createBitcoinApi(String currency) {
		
		BitcoinApi api = null;
		
		if(currency.equals("ltc")) {
			api = new BitcoinApi("82.196.14.26", 8332, "fluxltc1", "fLuxThuyu1eP");
			api.setTransferFee(0.01);
		} else if(currency.equals("btc")) {
			api = new BitcoinApi("82.196.8.147", 9332, "fluxltc1", "fLuxThuyu1eP");
			api.setTransferFee(0.0004);
		}
		
		return api;
		
	}

}
