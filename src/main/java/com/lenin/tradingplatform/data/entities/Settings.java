package com.lenin.tradingplatform.data.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
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
	private Map<String, String> lastBlockHashes;
	
	private Map<String, Map<String, Double>> serviceFees;
	
	private Map<String, Double> totalProfits = new HashMap<String, Double>();
	
	private String postUrl = "";
	
	private Long nonce = 0L;
	private Long nonceMtgox = 0L;
	
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
	
	public Map<String, String> getLastBlockHashes() {
		return lastBlockHashes;
	}

	public void setLastBlockHashes(Map<String, String> lastBlockHashes) {
		this.lastBlockHashes = lastBlockHashes;
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

	public Map<String, Map<String, Double>> getServiceFees() {
		return serviceFees;
	}

	public void setServiceFees(Map<String, Map<String, Double>> serviceFees) {
		this.serviceFees = serviceFees;
	}

	public Map<String, Double> getTotalProfits() {
		return totalProfits;
	}

	public void setTotalProfits(Map<String, Double> totalProfits) {
		this.totalProfits = totalProfits;
	}

	public String getPostUrl() {
		return postUrl;
	}
	
	public void setPostUrl(String postUrl) {
		this.postUrl = postUrl;
	}

	public Long getNonce() {
		return nonce;
	}

	public void setNonce(Long nonce) {
		this.nonce = nonce;
	}

	public Long getNonceMtgox() {
		return nonceMtgox;
	}

	public void setNonceMtgox(Long nonceMtgox) {
		this.nonceMtgox = nonceMtgox;
	}

	
	
}
