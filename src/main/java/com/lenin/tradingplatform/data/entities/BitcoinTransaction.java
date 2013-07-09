package com.lenin.tradingplatform.data.entities;


public class BitcoinTransaction extends FundTransaction {

	private String txId;
	private String address;
	private String category;
	private String blockHash;
	
	private Integer confirmations;
	
	
	public BitcoinTransaction() {
		
	}
	
	public String getTxId() {
		return txId;
	}

	public void setTxId(String txId) {
		this.txId = txId;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}
	
	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}
	
	public String getBlockHash() {
		return blockHash;
	}
	
	public void setBlockHash(String blockHash) {
		this.blockHash = blockHash;
	}

	public Integer getConfirmations() {
		return confirmations;
	}

	public void setConfirmations(Integer confirmations) {
		this.confirmations = confirmations;
	}
	

}
