package com.lenin.tradingplatform.client;


public interface TransferClient {
	
	public OperationResult getTransactions(Long fromTime, Long untilTime, String sourceId);
	
	public OperationResult transferFunds(String fromId, String toId, Double amount);

}
