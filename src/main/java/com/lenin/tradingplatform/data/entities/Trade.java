package com.lenin.tradingplatform.data.entities;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;


public class Trade implements Serializable {
	
	private static final long serialVersionUID = -6096286190530844035L;
	
	@Id
	private String id;
	
	private Boolean live = false;
	
	private String service;
	private String tradeId;
	private String pair;
	private String type;
	private Double amount;
	private Double rate;
	private String orderId;
	private Long time;
	
	@DBRef
	private AccountFunds accountFunds;
	
	
	public Trade() {
		
	}
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	
	public String getTradeId() {
		return tradeId;
	}


	public void setTradeId(String tradeId) {
		this.tradeId = tradeId;
	}

	
	
	public Boolean getLive() {
		return live;
	}


	public void setLive(Boolean live) {
		this.live = live;
	}


	public String getPair() {
		return pair;
	}


	public void setPair(String pair) {
		this.pair = pair;
	}


	public String getType() {
		return type;
	}


	public void setType(String type) {
		this.type = type;
	}

	
	public Double getAmount() {
		return amount;
	}


	public void setAmount(Double amount) {
		this.amount = amount;
	}


	public Double getRate() {
		return rate;
	}


	public void setRate(Double rate) {
		this.rate = rate;
	}


	public String getOrderId() {
		return orderId;
	}


	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}


	public Long getTime() {
		return time;
	}


	public void setTime(Long time) {
		this.time = time;
	}


	public String getService() {
		return service;
	}


	public void setService(String service) {
		this.service = service;
	}


	public AccountFunds getAccountFunds() {
		return accountFunds;
	}


	public void setAccountFunds(AccountFunds accountFunds) {
		this.accountFunds = accountFunds;
	}

	
	
	
	
}
