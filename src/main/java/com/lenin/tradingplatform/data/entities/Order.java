package com.lenin.tradingplatform.data.entities;

import java.io.Serializable;

import org.springframework.data.mongodb.core.mapping.DBRef;


public class Order extends BtceApiCall implements Serializable {
	
	
	private static final long serialVersionUID = 2034644454740810443L;
	
	@DBRef
	private User user;
	
	@DBRef
	private TradingSession tradingSession;
	
	@DBRef
	private Order reversedOrder;
	
	private Boolean live;
	
	private Boolean save;
	private Boolean isReversed = false;
	
	private Long time;
	
	private Double rate;
	private Double amount;
	private Double brokerAmount;
	private Double finalAmount;
	private String pair;
	private String type;
	
	private String orderId;
	private Double received = 0.0;
	private Double remains = 0.0;
	private Double filledAmount = 0.0;
	
	
	public Order() {
		
	}

	
	public User getUser() {
		return user;
	}


	public void setUser(User user) {
		this.user = user;
	}

	public TradingSession getTradingSession() {
		return tradingSession;
	}


	public void setTradingSession(TradingSession tradingSession) {
		this.tradingSession = tradingSession;
	}


	public Order getReversedOrder() {
		return reversedOrder;
	}


	public void setReversedOrder(Order reversedOrder) {
		this.reversedOrder = reversedOrder;
	}

	
	public Boolean getLive() {
		return live;
	}


	public void setLive(Boolean live) {
		this.live = live;
	}


	public Boolean getSave() {
		return save;
	}


	public void setSave(Boolean save) {
		this.save = save;
	}

	

	public Boolean getIsReversed() {
		return isReversed;
	}


	public void setIsReversed(Boolean isReversed) {
		this.isReversed = isReversed;
	}


	public Long getTime() {
		return time;
	}


	public void setTime(Long time) {
		this.time = time;
	}


	public Double getRate() {
		return rate;
	}


	public void setRate(Double rate) {
		this.rate = rate;
	}


	public Double getAmount() {
		return amount;
	}


	public void setAmount(Double amount) {
		this.amount = amount;
	}

	
	public Double getBrokerAmount() {
		return brokerAmount;
	}


	public void setBrokerAmount(Double brokerAmount) {
		this.brokerAmount = brokerAmount;
	}


	public Double getFinalAmount() {
		return finalAmount;
	}


	public void setFinalAmount(Double finalAmount) {
		this.finalAmount = finalAmount;
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


	public String getOrderId() {
		return orderId;
	}


	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}


	public Double getReceived() {
		return received;
	}


	public void setReceived(Double received) {
		this.received = received;
	}


	public Double getRemains() {
		return remains;
	}


	public void setRemains(Double remains) {
		this.remains = remains;
	}


	public Double getFilledAmount() {
		return filledAmount;
	}


	public void setFilledAmount(Double filledAmount) {
		this.filledAmount = filledAmount;
	}
	
	
	public Double calcTradeRevenue(Trade trade) {
		
		Double tradeRevenue = 0.0;
		
		if(reversedOrder != null) {
			
			Double totalFeeFactor = (1 - 0.002);
			//Double totalFeeFactor = (1 - UserTrader.transactionFee);
			Double tradeAmount = trade.getAmount()*totalFeeFactor;
		
			if(type.equals("sell")) {
			
				tradeRevenue = 
						(tradeAmount*rate) - 
						(tradeAmount*reversedOrder.getRate());
		
			} else if(type.equals("buy")) {
			
				tradeRevenue = 
					(tradeAmount*reversedOrder.getRate()) -
					(tradeAmount*rate);
		
			}
			
		}
		
		return tradeRevenue;
		
	}
	
	
	public Double calcFinalRevenue() {
		
		Double finalRevenue = 0.0;
		
		if(reversedOrder != null) {
					
			if(type.equals("sell")) {
			
				finalRevenue = 
						(finalAmount*rate) - 
						(reversedOrder.getFinalAmount()*reversedOrder.getRate());
		
			} else if(type.equals("buy")) {
			
				finalRevenue = 
					(reversedOrder.getFinalAmount()*reversedOrder.getRate()) -
					(finalAmount*rate);
		
			}
			
		}
		
		return finalRevenue;
		
	}

	
}
