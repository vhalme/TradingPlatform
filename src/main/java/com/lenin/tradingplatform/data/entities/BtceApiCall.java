package com.lenin.tradingplatform.data.entities;

import org.springframework.data.annotation.Id;


public abstract class BtceApiCall {
	
	@Id
	private String id;
	
	private String method;
	
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	
	
}
