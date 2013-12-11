package com.lenin.tradingplatform.data.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;


public class User implements Serializable {
	
	private static final long serialVersionUID = 8124064122670998564L;

	@Id
	private String id;
	
	private Boolean live = false;
	private Boolean deleted = false;
	
	private String email;
	private String username;
	private String password;
	private String authToken;
	private String lastIp;
	private Long lastActivity;
	
	private Boolean emailVerified = false;
	private Long verificationEmailSent = 0L;
	
	private Map<String, Double> realFunds = new HashMap<String, Double>();
	
	private Boolean loggedIn = false;
			
	@DBRef
	private List<TradingSession> tradingSessions = new ArrayList<TradingSession>();
	
	@DBRef
	private TradingSession currentTradingSession;
	
	@DBRef
	private AccountFunds accountFunds;
	
	private Long lastBtceTradeTime = 0L;
	
	private List<ErrorMessage> errors = new ArrayList<ErrorMessage>();
	private Map<String, List<ErrorMessage>> sessionErrors = new HashMap<String, List<ErrorMessage>>();
	
	private Map<String, Boolean> tradeOk = new HashMap<String, Boolean>();
	
	private Map<String, Double> exchangeFees = new HashMap<String, Double>();
	
	
	public User() {
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

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public List<TradingSession> getTradingSessions() {
		return tradingSessions;
	}

	public void setTradingSessions(List<TradingSession> tradingSessions) {
		this.tradingSessions = tradingSessions;
	}

	public TradingSession getCurrentTradingSession() {
		return currentTradingSession;
	}

	public void setCurrentTradingSession(TradingSession currentTradingSession) {
		this.currentTradingSession = currentTradingSession;
	}
	
	public String getAuthToken() {
		return authToken;
	}

	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}
	
	
	public String getLastIp() {
		return lastIp;
	}

	public void setLastIp(String lastIp) {
		this.lastIp = lastIp;
	}

	public Long getLastActivity() {
		return lastActivity;
	}

	public void setLastActivity(Long lastActivity) {
		this.lastActivity = lastActivity;
	}

	
	
	public Boolean getEmailVerified() {
		return emailVerified;
	}

	public void setEmailVerified(Boolean emailVerified) {
		this.emailVerified = emailVerified;
	}
	
	
	public Long getVerificationEmailSent() {
		return verificationEmailSent;
	}

	public void setVerificationEmailSent(Long verificationEmailSent) {
		this.verificationEmailSent = verificationEmailSent;
	}

	public AccountFunds getAccountFunds() {
		return accountFunds;
	}

	public void setAccountFunds(AccountFunds accountFunds) {
		this.accountFunds = accountFunds;
	}

	
	public Double getFunds(String fund) {
		
		if(fund.equals("left")) {
			return currentTradingSession.getFundsLeft();
		} else if(fund.equals("right")) {
			return currentTradingSession.getFundsRight();
		} else {
			return null;
		}
		
	}
	
	public void setFunds(String fund, Double amount) {
		
		if(fund.equals("left")) {
			currentTradingSession.setFundsLeft(amount);
		} else if(fund.equals("right")) {
			currentTradingSession.setFundsRight(amount);
		}
		
	}


	public void addTradingSession(TradingSession tradingSession) {
		tradingSessions.add(tradingSession);
	}

	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}

	public Long getLastBtceTradeTime() {
		return lastBtceTradeTime;
	}

	public void setLastBtceTradeTime(Long lastBtceTradeTime) {
		this.lastBtceTradeTime = lastBtceTradeTime;
	}

	public List<ErrorMessage> getErrors() {
		return errors;
	}

	public void setErrors(List<ErrorMessage> errors) {
		this.errors = errors;
	}

	
	public Map<String, List<ErrorMessage>> getSessionErrors() {
		return sessionErrors;
	}

	public void setSessionErrors(Map<String, List<ErrorMessage>> sessionErrors) {
		this.sessionErrors = sessionErrors;
	}


	public Map<String, Double> getRealFunds() {
		return realFunds;
	}

	public void setRealFunds(Map<String, Double> realFunds) {
		this.realFunds = realFunds;
	}

	public Map<String, Boolean> getTradeOk() {
		return tradeOk;
	}

	public void setTradeOk(Map<String, Boolean> tradeOk) {
		this.tradeOk = tradeOk;
	}

	public Boolean getLoggedIn() {
		return loggedIn;
	}

	public void setLoggedIn(Boolean loggedIn) {
		this.loggedIn = loggedIn;
	}

	public Map<String, Double> getExchangeFees() {
		return exchangeFees;
	}
	
	public void setExchangeFees(Map<String, Double> exchangeFees) {
		this.exchangeFees = exchangeFees;
	}

	
	
	
}
