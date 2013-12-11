package com.lenin.tradingplatform.data.entities;

import java.io.Serializable;

public class AutoTradingOptions implements Serializable {

	
	private static final long serialVersionUID = 4044855246441420579L;

	private Boolean tradeAuto = false;
	
	private String autoTradingModel = "movingAvg";
	
	private Double sellThreshold = 2.5;
	private Double buyThreshold = 2.5;
	private Double rateBuffer = 0.0;
	private Double buyChunk = 10.0;
	private Double sellChunk = 10.0;
	private Double sellFloor = 1.0;
	private Double buyCeiling = 1.0;
	
	private String maLong = "ema1h";
	private String maShort = "ema30min";
	private Double tradingRangeBottom = 0.0;
	private Double tradingRangeTop = 0.0;
	private Boolean manualSettings = true;
	private Double autoDuration = 20.0;
	private Double autoFrequency = 80.0;
	
	private Boolean allIn = false;
	
	public AutoTradingOptions() {
		
	}

	
	public Boolean getTradeAuto() {
		return tradeAuto;
	}


	public void setTradeAuto(Boolean tradeAuto) {
		this.tradeAuto = tradeAuto;
	}


	public String getAutoTradingModel() {
		return autoTradingModel;
	}

	public void setAutoTradingModel(String autoTradingModel) {
		this.autoTradingModel = autoTradingModel;
	}
	
	public Double getSellThreshold() {
		return sellThreshold;
	}


	public void setSellThreshold(Double sellThreshold) {
		this.sellThreshold = sellThreshold;
	}


	public Double getBuyThreshold() {
		return buyThreshold;
	}


	public void setBuyThreshold(Double buyThreshold) {
		this.buyThreshold = buyThreshold;
	}


	public Double getRateBuffer() {
		return rateBuffer;
	}

	public void setRateBuffer(Double rateBuffer) {
		this.rateBuffer = rateBuffer;
	}

	public Double getBuyChunk() {
		return buyChunk;
	}

	public void setBuyChunk(Double buyChunk) {
		this.buyChunk = buyChunk;
	}
	
	public Double getSellChunk() {
		return sellChunk;
	}

	public void setSellChunk(Double sellChunk) {
		this.sellChunk = sellChunk;
	}
	
	public Double getSellFloor() {
		return sellFloor;
	}

	public void setSellFloor(Double sellFloor) {
		this.sellFloor = sellFloor;
	}

	public Double getBuyCeiling() {
		return buyCeiling;
	}

	public void setBuyCeiling(Double buyCeiling) {
		this.buyCeiling = buyCeiling;
	}


	public String getMaLong() {
		return maLong;
	}

	public void setMaLong(String maLong) {
		this.maLong = maLong;
	}


	public String getMaShort() {
		return maShort;
	}


	public void setMaShort(String maShort) {
		this.maShort = maShort;
	}


	public Double getTradingRangeBottom() {
		return tradingRangeBottom;
	}


	public void setTradingRangeBottom(Double tradingRangeBottom) {
		this.tradingRangeBottom = tradingRangeBottom;
	}


	public Double getTradingRangeTop() {
		return tradingRangeTop;
	}


	public void setTradingRangeTop(Double tradingRangeTop) {
		this.tradingRangeTop = tradingRangeTop;
	}


	public Boolean getManualSettings() {
		return manualSettings;
	}


	public void setManualSettings(Boolean manualSettings) {
		this.manualSettings = manualSettings;
	}


	public Double getAutoDuration() {
		return autoDuration;
	}


	public void setAutoDuration(Double autoDuration) {
		this.autoDuration = autoDuration;
	}


	public Double getAutoFrequency() {
		return autoFrequency;
	}


	public void setAutoFrequency(Double autoFrequency) {
		this.autoFrequency = autoFrequency;
	}


	public Boolean getAllIn() {
		return allIn;
	}


	public void setAllIn(Boolean allIn) {
		this.allIn = allIn;
	}
	
	
	
}
