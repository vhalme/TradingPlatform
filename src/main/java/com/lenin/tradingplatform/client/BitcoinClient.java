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
	
	public OperationResult getTransactions(String lastBlockHash) {
		
		OperationResult opResult = new OperationResult();
		
		List<FundTransaction> transactions = new ArrayList<FundTransaction>();
		
		BitcoinApi api = createBitcoinApi(currency);
		
		List<Object> params = new ArrayList<Object>();
		
		if(lastBlockHash != null && !lastBlockHash.equals("")) {
			params.add(lastBlockHash);
		}
		
		//params.add(""+number);
		//params.add(""+from);
		
		JSONObject result = api.exec("listsinceblock", params);
		//System.out.println(result);
		
		JSONArray data = null;
		
		try {
			
			JSONObject resultObj = result.getJSONObject("result");
			data = resultObj.getJSONArray("transactions");
			
		} catch(Exception e) {
			e.printStackTrace();
		}
			//System.out.println(data);
			
		for(int i=0; i<data.length(); i++) {
			
			Boolean addOk = true;
			
			BitcoinTransaction	transaction = new BitcoinTransaction();
			transaction.setType("deposit");
			transaction.setCurrency(currency);
			
			JSONObject txJson = null;
			
			try {
			
				txJson = data.getJSONObject(i);
				
			} catch(Exception e) {
				e.printStackTrace();
				addOk = false;
			}
			
			if(addOk) {
				
				try {
					
					String blockHash = txJson.getString("blockhash");
					transaction.setBlockHash(blockHash);
					
				} catch(Exception e) {
					//e.printStackTrace();
				}
				
				try {
				
					transaction.setTxId(txJson.getString("txid"));
					//transaction.setBlockHash();
					transaction.setAccount(txJson.getString("account"));
					transaction.setAddress(txJson.getString("address"));
					transaction.setAmount(txJson.getDouble("amount"));
					transaction.setConfirmations(txJson.getInt("confirmations"));
					transaction.setTime(txJson.getLong("time"));
					transaction.setCategory(txJson.getString("category"));
					
				} catch(Exception e) {
					e.printStackTrace();
					addOk = false;
				}
				
				if(addOk) {
					transactions.add(transaction);
				}
				
			}
			
			//System.out.println("transaction for "+transaction.getAmount()+" detected for "+transaction.getAccount()+" at "+transaction.getTime());
			//System.out.println(transaction.getTime()+" >= "+fromTime);
			
				
		}
			
		opResult.setSuccess(1);
		opResult.setData(transactions);
		
		
		return opResult;
		
	}
	
	
	public OperationResult transferFunds(String fromAccount, String toAddress, Double amount) {
		
		OperationResult opResult = new OperationResult();
		opResult.setSuccess(0);
		
		BitcoinApi api = createBitcoinApi(currency);
		
		amount = amount - BitcoinApi.getFee(currency);
		
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
			api = new BitcoinApi("127.0.0.1", 9332, "fluxltc1", "fLuxThuyu1eP");
		} else if(currency.equals("btc")) {
			api = new BitcoinApi("127.0.0.1", 9332, "fluxltc1", "fLuxThuyu1eP");
		}
		
		return api;
		
	}

}
