package com.lenin.tradingplatform.client;

import https.api_okpay.IOkPayAPI;
import https.api_okpay.OkPayAPIImplementation;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.xml.bind.JAXBElement;

import org.datacontract.schemas._2004._07.okpayapi.ArrayOfTransactionInfo;
import org.datacontract.schemas._2004._07.okpayapi.HistoryInfo;
import org.datacontract.schemas._2004._07.okpayapi.TransactionInfo;

public class OkpayClient {
	
	public OkpayClient() {
	}
	
	public List<FundTransaction> getTransactions(Long sinceTime) {
		
		Date now = new Date();
		
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd:HH");
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		String fmtDate = format.format(now);
		
		String unhashed = "Bp3m8TRc25EsWt69Pre7F4Hig:"+fmtDate;
		
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
			
			IOkPayAPI api = new OkPayAPIImplementation().getBasicHttpBindingIOkPayAPI();
			HistoryInfo historyInfo = api.transactionHistory("OK990732954", hashed, "2013-04-20 00:00:00", "2013-04-25 00:00:00", 10, 1);
			
			List<TransactionInfo> txInfos = historyInfo.getTransactions().getValue().getTransactionInfo();
			
			System.out.println("tx info size: "+txInfos.size());
			
			for(TransactionInfo txInfo : txInfos) {
				System.out.println("Amount: "+txInfo.getAmount());
				System.out.println("Net amount: "+txInfo.getNet());
				System.out.println("Sender: "+txInfo.getSender().getValue().getName().getValue());
				System.out.println("Comment: "+txInfo.getComment().getValue());
				System.out.println("Invoice: "+txInfo.getInvoice().getValue());
			}
			
		
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		List<FundTransaction> transactions = new ArrayList<FundTransaction>();
		
		/*
		Transaction transaction = new Transaction();
		transaction.setAccount("GJNDE1369224905477");
		transaction.setAddress("LXKw8jkYiy9VX95qTpeLRy66VYK5SRrR2u");
		transaction.setAmount(207.0);
		transaction.setConfirmations(1);
		transaction.setTime(1723987199L);
		transactions.add(transaction);
		*/
		
		return transactions;
		
	}
	

}
