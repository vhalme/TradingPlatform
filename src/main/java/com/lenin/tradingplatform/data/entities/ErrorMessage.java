package com.lenin.tradingplatform.data.entities;

public class ErrorMessage {
	
	private String message;
	private Integer code;
	
	public ErrorMessage() {
		
	}
	
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public Integer getCode() {
		return code;
	}
	public void setCode(Integer code) {
		this.code = code;
	}
	
	

}
