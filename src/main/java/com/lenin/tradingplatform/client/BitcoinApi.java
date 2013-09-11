package com.lenin.tradingplatform.client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;


public class BitcoinApi {
	
	private String username;
	private String password;
	
	private String host;
	private int port;
	
	public static Double btcFee = 0.0004;
	public static Double ltcFee = 0.01;
	
	
	public BitcoinApi(String host, int port, String username, String password) {
		
		this.host = host;
		this.port = port;
		
		this.username = username;
		this.password = password;
		
	}


	public JSONObject exec(String method, List<Object> params) {

		
		DefaultHttpClient httpclient = new DefaultHttpClient();
		
		HttpParams httpParams = new BasicHttpParams();
	    HttpConnectionParams.setConnectionTimeout(httpParams, 30000);
	    
		httpclient.getCredentialsProvider().setCredentials(
				new AuthScope(host, port, AuthScope.ANY_REALM),
				new UsernamePasswordCredentials(username, password));
		
		
		String hostUrl = "http://"+host+":"+port;
		
		System.out.println("Connecting to host: "+hostUrl);
		
		HttpPost httppost = new HttpPost(hostUrl);
		httppost.setHeader("Content-type", "application/json");

		try {

			JSONObject bitcoinApiCall = new JSONObject();
			bitcoinApiCall.put("id", "1");
			bitcoinApiCall.put("method", method);
			bitcoinApiCall.put("jsonrpc", "1.0");
			bitcoinApiCall.put("params", params);

			String apiCallStr = bitcoinApiCall.toString();

			System.out.println(apiCallStr);

			StringEntity se = new StringEntity(apiCallStr);
			se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
			se.setContentType("application/json;charset=UTF-8");

			httppost.setEntity(se);

		} catch(Exception e) {
			e.printStackTrace();
		}

		String result = "";

		try {

			//Execute and get the response.
			HttpResponse httpResponse = httpclient.execute(httppost);
			
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			String statusPhrase = httpResponse.getStatusLine().getReasonPhrase();
			
			System.out.println("Request result: "+statusCode+" "+statusPhrase);
			
			HttpEntity entity = httpResponse.getEntity();

			if(entity != null) {

				InputStream instream = entity.getContent();

				BufferedReader rd = new BufferedReader(new InputStreamReader(instream));
				System.out.println("Ready to read result...");

				String line = null;
				while((line = rd.readLine()) != null) {
					result += line;
				}
				
				System.out.println("Result: "+result);
				
				rd.close();
				instream.close();
				
				
			}

		} catch(Exception e) {

			e.printStackTrace();

		}


		try {
			
			//JSONArray jsonResultArray = new JSONArray(result);
			JSONObject jsonResult = new JSONObject(result);
			//jsonResult.put("result", jsonResultArray);
			
			//System.out.println(result);
			
			return jsonResult;

		} catch(JSONException e) {
			
			return null;

		}
		

	}
	
	
	public static Double getFee(String currency) {
		
		if(currency.equals("btc")) {
			return btcFee;
		} else if(currency.equals("ltc")) {
			return ltcFee;
		} else {
			return 0.0;
		}
		
	}

}