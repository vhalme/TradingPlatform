package com.lenin.tradingplatform.client;

import https.api_okpay.IOkPayAPI;
import https.api_okpay.OkPayAPIImplementation;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import javax.xml.bind.JAXBElement;

import org.datacontract.schemas._2004._07.okpayapi.ArrayOfTransactionInfo;
import org.datacontract.schemas._2004._07.okpayapi.HistoryInfo;
import org.datacontract.schemas._2004._07.okpayapi.TransactionInfo;

import com.lenin.tradingplatform.data.entities.FundTransaction;
import com.lenin.tradingplatform.data.entities.OkpayTransaction;
import com.lenin.tradingplatform.data.entities.OkpayTransaction;

import com.lenin.tradingplatform.data.entities.FundTransaction;

public class OkpayClient {
	
	public OkpayClient() {
	}
	
	public OperationResult getTransactions(Long fromTime, Long untilTime, String sourceId) {
		
		OperationResult opResult = new OperationResult();
		
		SimpleDateFormat paramDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		Date fromDate = new Date((fromTime*1000L) + 1000L);
		String fromParam = paramDateFormat.format(fromDate); //"2013-04-20 00:00:00"
		
		Date untilDate = new Date(untilTime*1000L);
		String untilParam = paramDateFormat.format(untilDate);  //"2013-04-25 00:00:00"
		
		//System.out.println(fromParam+"/"+untilParam);
		
		List<FundTransaction> transactions = new ArrayList<FundTransaction>();
		
		try {
	        
			String hashed = getHashedKey("Bp3m8TRc25EsWt69Pre7F4Hig");
			//String hashed = getHashedKey("Ms49Nfk7Q2GeXz36HjRa5q8LK");
			
			String walletId = sourceId;
			System.out.println("Read transactions for "+walletId);
			
			IOkPayAPI api = new OkPayAPIImplementation().getBasicHttpBindingIOkPayAPI();
			System.err.println("SOAP api created: "+api);
			
			HistoryInfo historyInfo = api.transactionHistory(walletId, hashed, fromParam, untilParam, 10, 1);
			System.err.println("SOAP call performed, result: "+historyInfo);
			
			List<TransactionInfo> txInfos = historyInfo.getTransactions().getValue().getTransactionInfo();
			
			System.out.println("tx info size: "+txInfos.size());
			
			
			for(TransactionInfo txInfo : txInfos) {
				
				String txDateStr = txInfo.getDate().getValue();
				Date txDate = paramDateFormat.parse(txDateStr);
				Long txTime = txDate.getTime()/1000L;
				
				Double txAmount = txInfo.getAmount().doubleValue();
				Double txNetAmount = txInfo.getNet().doubleValue();
				String txCurrency = txInfo.getCurrency().getValue();
				
				String txSenderName = txInfo.getSender().getValue().getName().getValue();
				String txSenderAccount = ""+txInfo.getSender().getValue().getAccountID();
				String txSenderWallet = txInfo.getSender().getValue().getWalletID().getValue();
				
				String txReceiverName = txInfo.getReceiver().getValue().getName().getValue();
				String txReceiverAccount = ""+txInfo.getReceiver().getValue().getAccountID();
				String txReceiverWallet = txInfo.getReceiver().getValue().getWalletID().getValue();
				
				String txComment = txInfo.getComment().getValue();
				String txInvoice = txInfo.getInvoice().getValue();
				
				String txAccount = null;
				
				if(txInvoice != null && txInvoice.length() > 0) {
					txAccount = txInvoice;
				} else if(txComment != null && txComment.length() > 0) {
					txAccount = txComment;
				}
				
				if(txAccount != null) {
					int dashIndex = txAccount.indexOf("_");
					if(dashIndex != -1) {
						txAccount = txAccount.substring(dashIndex+1);
					}
				}
				
				//System.out.println("txComment="+txComment+"/txInvoice="+txInvoice+"/txAccount="+txAccount);
				
				OkpayTransaction transaction = new OkpayTransaction();
				transaction.setType("deposit");
				transaction.setState("confirmed");
				transaction.setAccount(txAccount);
				transaction.setAmount(txNetAmount);
				transaction.setComment(txComment);
				transaction.setCurrency(txCurrency.toLowerCase());
				transaction.setInvoice(txInvoice);
				transaction.setNetAmount(txNetAmount);
				
				transaction.setReceiverAccountId(txReceiverAccount);
				transaction.setReceiverName(txReceiverName);
				transaction.setReceiverWalletId(txReceiverWallet);
				
				transaction.setSenderAccountId(txSenderAccount);
				transaction.setSenderName(txSenderName);
				transaction.setSenderWalletId(txSenderWallet);
				
				transaction.setTime(txTime);
				
				/*
				System.out.println(txDateStr);
				System.out.println("Amount: "+txAmount);
				System.out.println("Net amount: "+txNetAmount);
				System.out.println("Sender: "+txSenderName+"/"+txSenderAccount+"/"+txSenderWallet);
				System.out.println("Receiver: "+txReceiverName+"/"+txReceiverAccount+"/"+txReceiverWallet);
				System.out.println("Comment: "+txComment);
				System.out.println("Invoice: "+txInvoice);
				System.out.println("Currency: "+txCurrency);
				
				System.out.println("OPNAME: "+txInfo.getOperationName().getValue()+", STATUS: "+txInfo.getStatus().value()+", FEES: "+txInfo.getFees());
				*/
				
				if(txReceiverWallet.equals(sourceId)) {
					transaction.setAmount(Math.abs(txNetAmount));
					transaction.setNetAmount(Math.abs(txNetAmount));
					transactions.add(transaction);
				}
				
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
	
	
	public OperationResult transferFunds(String fromWalletId, String toWalletId, String toAccount, Double amount) {
		
		OperationResult opResult = new OperationResult();
		opResult.setSuccess(0);
		
		//System.out.println("TRANSFER from "+fromWalletId+" to "+toWalletId+", Amount "+amount);
		
		try {
			
			String hashed = getHashedKey("Bp3m8TRc25EsWt69Pre7F4Hig");
			//String hashed = getHashedKey("Ms49Nfk7Q2GeXz36HjRa5q8LK");
			
			String invoice = System.currentTimeMillis()+"_"+toAccount;
			
			IOkPayAPI api = new OkPayAPIImplementation().getBasicHttpBindingIOkPayAPI();
			TransactionInfo txInfo = api.sendMoney(fromWalletId, hashed, toWalletId, "USD", new BigDecimal(amount), "Transfer from "+fromWalletId, true, invoice);
			
			if(txInfo != null) {
				opResult.setSuccess(1);
			}
			
			//System.out.println("tx info: "+txInfo);
			
			//System.out.println("OPNAME: "+txInfo.getOperationName().getValue()+", STATUS: "+txInfo.getStatus().value()+", FEES: "+txInfo.getFees());
			
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		
		return opResult;
		
	}
	
	
	private String getHashedKey(String password)
			throws NoSuchAlgorithmException, UnsupportedEncodingException {

		// OK847848324

		Date now = new Date();

		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd:HH");
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		String fmtDate = format.format(now);

		String unhashed = password + ":" + fmtDate;

		SimpleDateFormat paramDateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");

		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(unhashed.getBytes("UTF-8"));
		byte[] digest = md.digest();

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < digest.length; i++) {
			sb.append(Integer.toString((digest[i] & 0xff) + 0x100, 16)
					.substring(1));
		}
		
		String hashed = sb.toString().toUpperCase();

		return hashed;

	}
	

}
