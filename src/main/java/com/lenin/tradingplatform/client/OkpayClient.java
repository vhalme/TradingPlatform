package com.lenin.tradingplatform.client;

import https.api_okpay.IOkPayAPI;
import https.api_okpay.OkPayAPIImplementation;

import java.security.MessageDigest;
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

public class OkpayClient implements TransferClient {
	
	public OkpayClient() {
	}
	
	public OperationResult getTransactions(Long fromTime, Long untilTime, String sourceId) {
		
		OperationResult opResult = new OperationResult();
		
		Date now = new Date();
		
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd:HH");
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		String fmtDate = format.format(now);
		
		String unhashed = "Bp3m8TRc25EsWt69Pre7F4Hig:"+fmtDate;
		//String unhashed = "Ms49Nfk7Q2GeXz36HjRa5q8LK:"+fmtDate;
		
		SimpleDateFormat paramDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		Date fromDate = new Date((fromTime*1000L) + 1000L);
		String fromParam = paramDateFormat.format(fromDate); //"2013-04-20 00:00:00"
		
		Date untilDate = new Date(untilTime*1000L);
		String untilParam = paramDateFormat.format(untilDate);  //"2013-04-25 00:00:00"
		
		System.out.println(fromParam+"/"+untilParam);
		
		List<FundTransaction> transactions = new ArrayList<FundTransaction>();
		
		try {
			
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(unhashed.getBytes("UTF-8"));
			byte[] digest = md.digest();
			
			StringBuffer sb = new StringBuffer();
	        for(int i=0; i<digest.length; i++) {
	        	sb.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1));
	        }
	        
			String hashed = sb.toString().toUpperCase();
			
			System.out.println(unhashed+"/"+hashed);
			
			String walletId = sourceId;
			System.out.println("Read transactions for "+walletId);
			
			IOkPayAPI api = new OkPayAPIImplementation().getBasicHttpBindingIOkPayAPI();
			HistoryInfo historyInfo = api.transactionHistory(walletId, hashed, fromParam, untilParam, 10, 1);
			
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
				
				OkpayTransaction transaction = new OkpayTransaction();
				transaction.setType("deposit");
				transaction.setState("deposited");
				transaction.setAmount(txAmount);
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
				
				System.out.println(txDateStr);
				System.out.println("Amount: "+txAmount);
				System.out.println("Net amount: "+txNetAmount);
				System.out.println("Sender: "+txSenderName);
				System.out.println("Comment: "+txComment);
				System.out.println("Invoice: "+txInvoice);
				System.out.println("Currency: "+txCurrency);
				
				if(txAmount > 0) {
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
	
	
	public OperationResult transferFunds(String fromWalletId, String toWalletId, Double amount) {
		
		OperationResult opResult = new OperationResult();
		
		System.out.println("TRANSFER from "+fromWalletId+" to "+toWalletId+", Amount "+amount);
		
		return opResult;
		
	}
	

}
