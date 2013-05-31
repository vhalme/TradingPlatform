package com.lenin.tradingplatform.client;

public class BitcoinTransaction extends FundTransaction {

	private String address;
	private Integer confirmations;
	
	public BitcoinTransaction() {
		
	}
	
	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}
	
	public Integer getConfirmations() {
		return confirmations;
	}

	public void setConfirmations(Integer confirmations) {
		this.confirmations = confirmations;
	}
	

}
