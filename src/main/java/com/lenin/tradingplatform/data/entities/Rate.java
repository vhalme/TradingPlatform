package com.lenin.tradingplatform.data.entities;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;


public class Rate implements Serializable {
	
	private static final long serialVersionUID = 2564403728524688276L;
	
	@Id
	private String id;
	
	private String testSessionId;
	
	private String setType;
	
	private Long time = System.currentTimeMillis()/1000L;
	
	private String service;
	private String pair;
	private Double last = 0.0;
	private Double buy = 0.0;
	private Double sell = 0.0;
	private Double average = 0.0;
	private Double high = 0.0;
	private Double low = 0.0;
	private Double open = 0.0;
	private Double close = 0.0;
	private Double volume = 0.0;
	private Double currentVolume = 0.0;
	
	private Map<String, Double> movingAverages = new HashMap<String, Double>();
	
	
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

	
	public Double getAverage() {
		return average;
	}


	public void setAverage(Double average) {
		this.average = average;
	}


	public Double getHigh() {
		return high;
	}


	public void setHigh(Double high) {
		this.high = high;
	}


	public Double getLow() {
		return low;
	}


	public void setLow(Double low) {
		this.low = low;
	}

	
	public Double getOpen() {
		return open;
	}


	public void setOpen(Double open) {
		this.open = open;
	}


	public Double getClose() {
		return close;
	}


	public void setClose(Double close) {
		this.close = close;
	}


	public Double getVolume() {
		return volume;
	}


	public void setVolume(Double volume) {
		this.volume = volume;
	}


	public Double getCurrentVolume() {
		return currentVolume;
	}


	public void setCurrentVolume(Double currentVolume) {
		this.currentVolume = currentVolume;
	}


	public Map<String, Double> getMovingAverages() {
		return movingAverages;
	}


	public void setMovingAverages(Map<String, Double> movingAverages) {
		this.movingAverages = movingAverages;
	}


	public String getService() {
		return service;
	}


	public void setService(String service) {
		this.service = service;
	}


	public String getTestSessionId() {
		return testSessionId;
	}


	public void setTestSessionId(String testSessionId) {
		this.testSessionId = testSessionId;
	}

	
	
	
}
