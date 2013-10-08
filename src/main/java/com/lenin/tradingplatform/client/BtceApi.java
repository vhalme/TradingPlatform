package com.lenin.tradingplatform.client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.lenin.tradingplatform.data.entities.Order;

public class BtceApi {
	
	public static long _nonce = System.currentTimeMillis() / 10000L;
	
	private String key;
	private String secret;
	
	private Double orderFee; // = 0.002;
	
	public BtceApi(String key, String secret) {
		this.key = key;
		this.secret = secret;
	}
	
	
	public JSONObject getRates(String pair) {
		
		try {
	    	
			//Create connection
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet("https://btc-e.com/api/2/"+pair+"/ticker");
    	
			HttpResponse httpResponse = client.execute(get);
			HttpEntity entity = httpResponse.getEntity();
			
			JSONObject jsonResult = null;
			
			if(entity != null) {
    	    
				InputStream instream = entity.getContent();
    	       
				BufferedReader in = 
					new BufferedReader(new InputStreamReader(instream));
    	        
				String resultStr = "";
				String inputLine = null;
					
				while((inputLine = in.readLine()) != null) {
					//System.out.println(inputLine);
					resultStr += inputLine;
				}
    	        
				in.close();
    	    	instream.close();
    	    	
    	    	jsonResult = new JSONObject(resultStr);
    	    	
			}
			
			return jsonResult;
    	
    	} catch(Exception e) {
    		
    		//e.printStackTrace();
    		return null;

  	    }
		
	
	}
	
	
	public JSONObject getActiveOrderList() {
		
		List<NameValuePair> methodParams = new ArrayList<NameValuePair>();
		methodParams.add(new BasicNameValuePair("method", "OrderList"));
		methodParams.add(new BasicNameValuePair("active", "1"));
		JSONObject orderListResult = authenticatedHTTPRequest(methodParams, key, secret);
		
		return orderListResult;
		
	}
	
	
	public JSONObject getTradeList(Long since) {
		
		List<NameValuePair> methodParams = new ArrayList<NameValuePair>();
		methodParams.add(new BasicNameValuePair("method", "TradeHistory"));
		methodParams.add(new BasicNameValuePair("since", ""+since));
		JSONObject tradeListResult = authenticatedHTTPRequest(methodParams, key, secret);
		
		return tradeListResult;
		
	}

	
	public JSONObject getAccountInfo() {
		
		List<NameValuePair> methodParams = new ArrayList<NameValuePair>();
		methodParams.add(new BasicNameValuePair("method", "getInfo"));
		JSONObject userInfoResult = authenticatedHTTPRequest(methodParams, key, secret);
		
		return userInfoResult;
		
	}
	
	
	public JSONObject trade(Order order, Double feeFactor) {
		
		List<NameValuePair> methodParams = new ArrayList<NameValuePair>();
		methodParams.add(new BasicNameValuePair("method", "Trade"));
		methodParams.add(new BasicNameValuePair("type", order.getType()));
		methodParams.add(new BasicNameValuePair("pair", order.getPair()));
		
		String amountStr = ""+(order.getAmount()*feeFactor);
		if(amountStr.length() > 8) {
			amountStr = amountStr.substring(0, 8);
		}
		
		String rateStr = ""+order.getRate();
		if(rateStr.length() > 8) {
			rateStr = rateStr.substring(0, 8);
		}
		
		methodParams.add(new BasicNameValuePair("amount", amountStr));
		methodParams.add(new BasicNameValuePair("rate", ""+rateStr));
		
		JSONObject tradeResult = authenticatedHTTPRequest(methodParams, key, secret);
		
		return tradeResult;
		
	}
	

	public JSONObject cancelOrder(Order order) {
		
		List<NameValuePair> methodParams = new ArrayList<NameValuePair>();
		methodParams.add(new BasicNameValuePair("method", "CancelOrder"));
		methodParams.add(new BasicNameValuePair("order_id", order.getOrderId()));
		
		JSONObject cancelOrderResult = authenticatedHTTPRequest(methodParams, key, secret);
		
		return cancelOrderResult;
		
	}

	
	public static Order createOrder(String pair, Double amount, Double rate, String type) {
		
		Order order = new Order();
		
		order.setTime((new Date()).getTime());
		order.setPair(pair);
		order.setAmount(amount);
		order.setRate(rate);
		order.setType(type);
		
		return order;
		
	}
	
	
	public static JSONObject authenticatedHTTPRequest(List<NameValuePair> methodParams, String key, String secret) {
        
		Long newNonce = (System.currentTimeMillis() / 1000L) + 100L;
		
		
		if(newNonce <= _nonce) {
			newNonce = _nonce+1;
		}
		
		_nonce = newNonce;
		
		Date now = new Date();
		
		// Request parameters and other properties.
        List<NameValuePair> params = new ArrayList<NameValuePair>(2);
        params.add(new BasicNameValuePair("nonce", "" + _nonce));
        
        String paramsString = "nonce="+_nonce;
        System.out.println(paramsString);
    	
        if(methodParams != null) {
        	for(NameValuePair nvp : methodParams) {
        		params.add(nvp);
        		paramsString += "&"+nvp.getName()+"="+nvp.getValue();
        	}
        }
        
        //System.out.println(paramsString);
        
        HttpClient httpclient = new DefaultHttpClient();
        
        HttpParams httpParams = new BasicHttpParams();
	    HttpConnectionParams.setConnectionTimeout(httpParams, 6000);
	    
        HttpPost httppost = new HttpPost("https://btc-e.com/tapi");
        
        try {
        	
        	UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(params, "UTF-8");
            httppost.setEntity(uefe);
            
    		SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA512");
    		Mac mac = Mac.getInstance("HmacSHA512" );
        	mac.init(keySpec);
        	
        	String sign = Hex.encodeHexString(mac.doFinal(paramsString.getBytes("UTF-8")));
        	httppost.addHeader("Key", key);
        	httppost.addHeader("Sign", sign);
        	
        } catch(Exception e) {
        	e.printStackTrace();
        }
        
        String result = "";
        
        try {
        
        	//Execute and get the response.
        	HttpResponse response = httpclient.execute(httppost);
        	HttpEntity entity = response.getEntity();

        	if(entity != null) {
        	
        		InputStream instream = entity.getContent();
        		
        		BufferedReader rd = new BufferedReader(new InputStreamReader(instream));
        		System.out.println("Ready to read result...");
        	
        		String line = null;
        		while((line = rd.readLine()) != null) {
            		result += line;
            	}
            	
        		System.out.println(result);
        		
        	}
            
        } catch(Exception e) {
    		
        	System.out.println("["+now+"] Failed to make a request. "+e+": "+e.getMessage());
        	System.out.println("     -> request params: "+paramsString);
        	
        	return null;
        			
        }
        
        try {
        	
        	//System.out.println(result);
        	
        	JSONObject jsonResult = new JSONObject(result);
        	
        	return jsonResult;
        	
        } catch(JSONException e) {
        	
        	System.out.println("["+now+"] API server did not return a proper response. "+e+": "+e.getMessage());
        	System.out.println("     -> request params: "+paramsString);
        	
        	return null;
        	
        }
        
        
        
    }

	
	public String getKey() {
		return key;
	}



	public void setKey(String key) {
		this.key = key;
	}



	public String getSecret() {
		return secret;
	}



	public void setSecret(String secret) {
		this.secret = secret;
	}



	public Double getOrderFee() {
		return orderFee;
	}



	public void setOrderFee(Double orderFee) {
		this.orderFee = orderFee;
	}
	
	
}
