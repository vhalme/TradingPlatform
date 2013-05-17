package com.lenin.tradingplatform.data.entities;

import java.io.Serializable;
import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;


public class Rate implements Serializable {
	
	private static final long serialVersionUID = 2564403728524688276L;
	
	@Id
	private String id;
	
	private String setType;
	
	private Long time = System.currentTimeMillis()/1000L;
	
	private String pair;
	private Double last = 0.0;
	private Double buy = 0.0;
	private Double sell = 0.0;
	
	public Rate() {
		
	}
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	
	
	public String getSetType() {
		return setType;
	}


	public void setSetType(String setType) {
		this.setType = setType;
	}


	public Long getTime() {
		return time;
	}


	public void setTime(Long time) {
		this.time = time;
	}


	public String getPair() {
		return pair;
	}


	public void setPair(String pair) {
		this.pair = pair;
	}


	public Double getLast() {
		return last;
	}


	public void setLast(Double last) {
		this.last = last;
	}


	public Double getBuy() {
		return buy;
	}


	public void setBuy(Double buy) {
		this.buy = buy;
	}


	public Double getSell() {
		return sell;
	}


	public void setSell(Double sell) {
		this.sell = sell;
	}

	
	
	
}
