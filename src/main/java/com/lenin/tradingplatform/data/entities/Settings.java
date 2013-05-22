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
	
	private Map<String, Long> lastTransactionTimes;
	
	public Settings() {
		
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Map<String, Long> getLastTransactionTimes() {
		return lastTransactionTimes;
	}

	public void setLastTransactionTimes(Map<String, Long> lastTransactionTimes) {
		this.lastTransactionTimes = lastTransactionTimes;
	}

	
	
	
}
