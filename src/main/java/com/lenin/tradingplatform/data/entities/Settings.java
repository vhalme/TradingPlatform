package com.lenin.tradingplatform.data.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;


public class Settings implements Serializable {
	
	private static final long serialVersionUID = 2713285931035875685L;
	
	@Id
	private String id;
	
	private String btceApiKey;
	private String btceApiSecret;
	
	private String serviceOkpayWalletId;
	private String btceOkpayWalletId;
	
	private Map<String, Long> lastTransactionTimes;
	
	public Settings() {
		
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String getBtceApiKey() {
		return btceApiKey;
	}

	public void setBtceApiKey(String apiKey) {
		this.btceApiKey = btceApiKey;
	}
	
	public String getBtceApiSecret() {
		return btceApiSecret;
	}

	public void setBtceApiSecret(String apiSecret) {
		this.btceApiSecret = btceApiSecret;
	}

	public Map<String, Long> getLastTransactionTimes() {
		return lastTransactionTimes;
	}

	public void setLastTransactionTimes(Map<String, Long> lastTransactionTimes) {
		this.lastTransactionTimes = lastTransactionTimes;
	}

	public String getServiceOkpayWalletId() {
		return serviceOkpayWalletId;
	}

	public void setServiceOkpayWalletId(String serviceOkpayWalletId) {
		this.serviceOkpayWalletId = serviceOkpayWalletId;
	}

	public String getBtceOkpayWalletId() {
		return btceOkpayWalletId;
	}

	public void setBtceOkpayWalletId(String btceOkpayWalletId) {
		this.btceOkpayWalletId = btceOkpayWalletId;
	}

	
	
	
}
