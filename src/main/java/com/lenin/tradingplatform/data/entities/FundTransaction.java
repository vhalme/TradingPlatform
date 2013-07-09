package com.lenin.tradingplatform.data.entities;

import java.io.Serializable;

import org.springframework.data.annotation.Id;

public class FundTransaction implements Serializable {
	
	private static final long serialVersionUID = -3783809470171649438L;
	
	@Id
	private String id;
	
	private String type;
	private String state;
	private String stateInfo;
	
	private String account;
	private String currency;
	
	private Double amount;
	
	private Long time;
	private Long systemTime;
	
	public FundTransaction() {
		
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}
	
	
	public String getStateInfo() {
		return stateInfo;
	}

	public void setStateInfo(String stateInfo) {
		this.stateInfo = stateInfo;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	
	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public Double getAmount() {
		return amount;
	}

	public void setAmount(Double amount) {
		this.amount = amount;
	}
	
	
	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}

	public Long getSystemTime() {
		return systemTime;
	}

	public void setSystemTime(Long systemTime) {
		this.systemTime = systemTime;
	}
	
	

}
