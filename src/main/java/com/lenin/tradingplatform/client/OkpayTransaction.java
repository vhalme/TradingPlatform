package com.lenin.tradingplatform.client;

public class OkpayTransaction extends FundTransaction {

	private String receiverName;
	private String receiverAccountId;
	private String receiverWalletId;
	
	private String senderName;
	private String senderAccountId;
	private String senderWalletId;
	
	private String comment;
	private String invoice;
	
	private Double netAmount;
	
	public OkpayTransaction() {
		
	}

	public String getReceiverName() {
		return receiverName;
	}

	public void setReceiverName(String receiverName) {
		this.receiverName = receiverName;
	}

	public String getReceiverAccountId() {
		return receiverAccountId;
	}

	public void setReceiverAccountId(String receiverAccountId) {
		this.receiverAccountId = receiverAccountId;
	}

	public String getReceiverWalletId() {
		return receiverWalletId;
	}

	public void setReceiverWalletId(String receiverWalletId) {
		this.receiverWalletId = receiverWalletId;
	}

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}

	public String getSenderAccountId() {
		return senderAccountId;
	}

	public void setSenderAccountId(String senderAccountId) {
		this.senderAccountId = senderAccountId;
	}

	public String getSenderWalletId() {
		return senderWalletId;
	}

	public void setSenderWalletId(String senderWalletId) {
		this.senderWalletId = senderWalletId;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getInvoice() {
		return invoice;
	}

	public void setInvoice(String invoice) {
		this.invoice = invoice;
	}

	public Double getNetAmount() {
		return netAmount;
	}

	public void setNetAmount(Double netAmount) {
		this.netAmount = netAmount;
	}
	

}
