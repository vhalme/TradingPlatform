package com.lenin.tradingplatform.data.entities;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;


public class AccountFunds implements Serializable {

	private static final long serialVersionUID = -3685935243452186122L;

	@Id
	private String id;
	
	private String accountName;
	
	private Map<String, String> addresses = new HashMap<String, String>();
	private Map<String, Double> reserves = new HashMap<String, Double>();
	private Map<String, Map<String, Double>> activeFunds = new HashMap<String, Map<String, Double>>();
	
	//private Map<String, ServiceInfo> serviceInfos = new HashMap<String, ServiceInfo>();
	private Map<String, PropertyMap> serviceProperties = new HashMap<String, PropertyMap>();
	
	private Boolean deleted = false;
	
	public AccountFunds() {
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public Map<String, String> getAddresses() {
		return addresses;
	}

	public void setAddresses(Map<String, String> addresses) {
		this.addresses = addresses;
	}

	public Map<String, Double> getReserves() {
		return reserves;
	}

	public void setReserves(Map<String, Double> reserves) {
		this.reserves = reserves;
	}

	public Map<String, Map<String, Double>> getActiveFunds() {
		return activeFunds;
	}

	public void setActiveFunds(Map<String, Map<String, Double>> activeFunds) {
		this.activeFunds = activeFunds;
	}

	public Map<String, PropertyMap> getServiceProperties() {
		return serviceProperties;
	}

	public void setServiceProperties(Map<String, PropertyMap> serviceProperties) {
		this.serviceProperties = serviceProperties;
	}

	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
	}

	
}
