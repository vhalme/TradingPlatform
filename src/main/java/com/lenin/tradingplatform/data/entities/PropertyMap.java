package com.lenin.tradingplatform.data.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;


public class PropertyMap implements Serializable {
	
	private static final long serialVersionUID = 156718411704934347L;

	@Id
	private String id;
	
	private String name;
	
	private Map<String, Object> properties = new HashMap<String, Object>();
	
	
	public PropertyMap() {
		
	}
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public Map<String, Object> getProperties() {
		return properties;
	}


	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}


		
	
}
