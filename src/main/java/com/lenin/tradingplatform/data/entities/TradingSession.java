package com.lenin.tradingplatform.data.entities;

import java.io.Serializable;
import java.util.Date;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;


public class TradingSession implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -820345864243515844L;

	@Id
	private String id;
	
	private Boolean live = false;
	
	private String service;
	private String pair;
	private String currencyLeft;
	private String currencyRight;
	
	private Rate rate;
	private Rate previousRate;
	
	private Double oldRate = 0.0;
	private Double lastMaTransactionRate = 0.0;
	
	private Double fundsLeft = 0.0;
	private Double fundsRight = 0.0;
	private Double profitLeft = 0.0;
	private Double profitRight = 0.0;
	private Date profitLeftSince;
	private Date profitRightSince;
	
	private AutoTradingOptions autoTradingOptions;
	
	private String userId;
	private String username;
	
	private Boolean deleted = false;
	
	private Boolean tradeOk = false;
	
	
	public TradingSession() {
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public Boolean getLive() {
		return live;
	}

	public void setLive(Boolean live) {
		this.live = live;
	}
	
	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getCurrencyLeft() {
		return currencyLeft;
	}

	public void setCurrencyLeft(String currencyLeft) {
		this.currencyLeft = currencyLeft;
	}

	public String getCurrencyRight() {
		return currencyRight;
	}

	public void setCurrencyRight(String currencyRight) {
		this.currencyRight = currencyRight;
	}

	
	public Rate getRate() {
		return rate;
	}

	public void setRate(Rate rate) {
		this.rate = rate;
	}
	
	public Rate getPreviousRate() {
		return previousRate;
	}

	public void setPreviousRate(Rate previousRate) {
		this.previousRate = previousRate;
	}

	public Double getOldRate() {
		return oldRate;
	}

	public void setOldRate(Double oldRate) {
		this.oldRate = oldRate;
	}

	
	public Double getLastMaTransactionRate() {
		return lastMaTransactionRate;
	}

	public void setLastMaTransactionRate(Double lastMaTransactionRate) {
		this.lastMaTransactionRate = lastMaTransactionRate;
	}

	public Double getFundsLeft() {
		return fundsLeft;
	}

	public void setFundsLeft(Double fundsLeft) {
		this.fundsLeft = fundsLeft;
	}

	public Double getFundsRight() {
		return fundsRight;
	}

	public void setFundsRight(Double fundsRight) {
		this.fundsRight = fundsRight;
	}

	public Double getProfitLeft() {
		return profitLeft;
	}

	public void setProfitLeft(Double profitLeft) {
		this.profitLeft = profitLeft;
	}

	public Double getProfitRight() {
		return profitRight;
	}

	public void setProfitRight(Double profitRight) {
		this.profitRight = profitRight;
	}
	
	public Date getProfitLeftSince() {
		return profitLeftSince;
	}

	public void setProfitLeftSince(Date profitLeftSince) {
		this.profitLeftSince = profitLeftSince;
	}

	public Date getProfitRightSince() {
		return profitRightSince;
	}

	public void setProfitRightSince(Date profitRightSince) {
		this.profitRightSince = profitRightSince;
	}

	public AutoTradingOptions getAutoTradingOptions() {
		return autoTradingOptions;
	}

	public void setAutoTradingOptions(AutoTradingOptions autoTradingOptions) {
		this.autoTradingOptions = autoTradingOptions;
	}

	public Double getFunds(String fund) {
		
		if(fund.equals("left")) {
			return getFundsLeft();
		} else if(fund.equals("right")) {
			return getFundsRight();
		} else {
			return null;
		}
		
	}
	
	public void setFunds(String fund, Double amount) {
		
		if(fund.equals("left")) {
			setFundsLeft(amount);
		} else if(fund.equals("right")) {
			setFundsRight(amount);
		}
		
	}
	
	public String getPair() {
		return currencyRight+"_"+currencyLeft;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}

	public Boolean getTradeOk() {
		return tradeOk;
	}

	public void setTradeOk(Boolean tradeOk) {
		this.tradeOk = tradeOk;
	}
 	
	

}
