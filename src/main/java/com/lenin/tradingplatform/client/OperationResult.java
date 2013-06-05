package com.lenin.tradingplatform.client;

import java.io.Serializable;

public class OperationResult implements Serializable {
	
	private static final long serialVersionUID = -3700217473195020024L;
	
	private Integer success;
	private String message;
	private Object data;
	
	public OperationResult() {
		
	}

	public Integer getSuccess() {
		return success;
	}

	public void setSuccess(Integer success) {
		this.success = success;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}
	
	

}
