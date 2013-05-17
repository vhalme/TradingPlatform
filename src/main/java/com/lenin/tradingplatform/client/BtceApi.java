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
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.lenin.tradingplatform.data.entities.Order;

public class BtceApi {
	
	public static long _nonce = System.currentTimeMillis() / 10000L;
	
	private static String _key = "XSR43QT2-B7PBL6EY-U6JCVFCM-7IMTI26B-7XEL3DGO";
	private static String _secret = "a93adec600bd65960d26d779343b70700fbb4a93e333e15350b2bb1a21fb46de";
	
	public static Double currentRateLtcUsd = 0.0;
	public static Double currentBuyRateLtcUsd = 0.0;
	public static Double currentSellRateLtcUsd = 0.0;
	
	public static Double oldRateLtcUsd = 0.0;
	
	public static Double orderFee = 0.002;
	
	
	public static JSONObject getRates(String pair) {
		
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
	
	
	public static JSONObject getActiveOrderList() {
		
		List<NameValuePair> methodParams = new ArrayList<NameValuePair>();
		methodParams.add(new BasicNameValuePair("method", "OrderList"));
		methodParams.add(new BasicNameValuePair("active", "1"));
		JSONObject orderListResult = authenticatedHTTPRequest(methodParams);
		
		return orderListResult;
		
	}
	
	
	public static JSONObject getTradeList(Long since) {
		
		List<NameValuePair> methodParams = new ArrayList<NameValuePair>();
		methodParams.add(new BasicNameValuePair("method", "TradeHistory"));
		methodParams.add(new BasicNameValuePair("since", ""+since));
		JSONObject tradeListResult = authenticatedHTTPRequest(methodParams);
		
		return tradeListResult;
		
	}

	
	public static JSONObject getAccountInfo() {
		
		List<NameValuePair> methodParams = new ArrayList<NameValuePair>();
		methodParams.add(new BasicNameValuePair("method", "getInfo"));
		JSONObject userInfoResult = authenticatedHTTPRequest(methodParams);
		
		return userInfoResult;
		
	}
	
	
	public static JSONObject trade(Order order, Double feeFactor) {
		
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
		
		JSONObject tradeResult = authenticatedHTTPRequest(methodParams);
		
		return tradeResult;
		
	}
	

	public static JSONObject cancelOrder(Order order) {
		
		List<NameValuePair> methodParams = new ArrayList<NameValuePair>();
		methodParams.add(new BasicNameValuePair("method", "CancelOrder"));
		methodParams.add(new BasicNameValuePair("order_id", order.getOrderId()));
		
		JSONObject cancelOrderResult = authenticatedHTTPRequest(methodParams);
		
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
	
	
	public static JSONObject authenticatedHTTPRequest(List<NameValuePair> methodParams) {
        
		Date now = new Date();
		
		// Request parameters and other properties.
        List<NameValuePair> params = new ArrayList<NameValuePair>(2);
        params.add(new BasicNameValuePair("nonce", "" + ++_nonce));
        
        String paramsString = "nonce="+_nonce;
    	
        if(methodParams != null) {
        	for(NameValuePair nvp : methodParams) {
        		params.add(nvp);
        		paramsString += "&"+nvp.getName()+"="+nvp.getValue();
        	}
        }
        
        //System.out.println(paramsString);
        
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("https://btc-e.com/tapi");
        
        try {
        	
        	UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(params, "UTF-8");
            httppost.setEntity(uefe);
            
    		SecretKeySpec key = new SecretKeySpec(_secret.getBytes("UTF-8"), "HmacSHA512");
    		Mac mac = Mac.getInstance("HmacSHA512" );
        	mac.init(key);
        	
        	String sign = Hex.encodeHexString(mac.doFinal(paramsString.getBytes("UTF-8")));
        	httppost.addHeader("Key", _key);
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
        		//System.out.println("Ready to read result...");
        	
        		String line = null;
        		while((line = rd.readLine()) != null) {
            		result += line;
            	}
            	
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


	
}
