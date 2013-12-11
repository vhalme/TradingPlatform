package com.lenin.tradingplatform.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.lenin.tradingplatform.data.entities.Order;
import com.lenin.tradingplatform.data.entities.Settings;

public class MtgoxApi implements ExchangeApi {
	
    private int readTimout = 600000;
    private int connectTimeout = 10000;
    private int killTimeout = 600000;
    
    private final String SIGN_HASH_FUNCTION = "HmacSHA512";
    
	private String key;
	private String secret;
	
	private Double orderFee; // = 0.005;
	private MongoOperations mongoOps;
	
	
	public MtgoxApi(String key, String secret, MongoOperations mongoOps) {
		
		this.key = key;
		this.secret = secret;
		this.mongoOps = mongoOps;
		
		TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager() {

	                @Override
	                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	                    return null;
	                }

	                @Override
	                public void checkClientTrusted(
	                        java.security.cert.X509Certificate[] certs, String authType) {
	                }

	                @Override
	                public void checkServerTrusted(
	                        java.security.cert.X509Certificate[] certs, String authType) {
	                }
	            }
	        };

		try {
	        	
	    	SSLContext sc = SSLContext.getInstance("SSL");
	        sc.init(null, trustAllCerts, new java.security.SecureRandom());
	        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	            
	    } catch (Exception e) {
	        	
	    	e.printStackTrace();
	        
	    }
	        
		
	}
	
	
	public JSONObject getActiveOrderList() {
		return null;
	}
	
	public JSONObject getRates(String pair) {
		
		
		try {
	    	
			HttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet("https://data.mtgox.com/api/1/BTCUSD/ticker");
    	
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
					resultStr += inputLine;
				}
    	        
				in.close();
    	    	instream.close();
    	    	
    	    	if(!resultStr.startsWith("{")) {
    	    		System.out.println("Malformed response from Mt. Gox, returning null.");
    	    		return null;
    	    	} else {
    	    		jsonResult = new JSONObject(resultStr);
    	    	}
    	    	
			}
			
			return jsonResult;
			
    	} catch(Exception e) {
    		
    		e.printStackTrace();
    		return null;

  	    }
		
	
	}
	
	
	public JSONObject getTradeList(Long since) {
		
		
		return null;
		
	}

	
	public JSONObject getOpenOrders() {
		
		try {
			
			String url = "https://data.mtgox.com/api/1/BTCUSD/private/orders";
	        String result = getResultFromStreamAsString(getMtGoxHTTPInputStream(url));
			JSONObject jsonResult = new JSONObject(result);
			
			return jsonResult;
			
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}

	
	public JSONObject getAccountInfo() {
		
		try {
			
			String url = "https://data.mtgox.com/api/1/BTCUSD/private/info";
	    	String result = getResultFromStreamAsString(getMtGoxHTTPInputStream(url));
	    	
			JSONObject jsonResult = new JSONObject(result);
			return jsonResult;
			
		} catch(Exception e) {
			
			//e.printStackTrace();
			return null;
		}
		
	}
	
	
	public JSONObject trade(Order order, Double feeFactor) {
		
		try {
			
			String amountStr = Long.toString(Math.round((order.getAmount()*feeFactor)*100000000));
			String rateStr = Long.toString(Math.round(order.getRate()*100000));
			
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("amount_int", amountStr);
			params.put("price_int", rateStr);
			
			if(order.getType().equals("sell")) {
				params.put("type", "ask");
			} else {
				params.put("type", "bid");
			}
			
			String url = "https://data.mtgox.com/api/1/BTCUSD/private/order/add";
	        String orderResult = getResultFromStreamAsString(getMtGoxHTTPInputStream(url, params));
			JSONObject jsonResult = new JSONObject(orderResult);
			
			return jsonResult;
			
		} catch(Exception e) {
			
			e.printStackTrace();
			return null;
			
		}
		
		
	}
	
	

	public JSONObject cancelOrder(Order order) {
		
		try {
			
			String url = "https://data.mtgox.com/api/1/BTCUSD/private/order/cancel";
	    	HashMap<String, String> params = new HashMap<String, String>();
	    	params.put("oid", order.getOrderId());
	    	
	    	String result = getResultFromStreamAsString(getMtGoxHTTPInputStream(url, params));
			JSONObject jsonResult = new JSONObject(result);
			
			return jsonResult;
			
		} catch(Exception e) {
			
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
	
	public String getResultFromStreamAsString(InputStream in) throws IOException {
        
    	BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    	String result = "";
    	String line = null;
    	while((line = reader.readLine()) != null) {
    		result += line;
    		System.out.println(line);
    	}
        
        return result;
    
    }
	
    
    
    protected InputStream getMtGoxHTTPInputStream(String path, HashMap<String, String> args) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        HttpURLConnection connection;
        
        List<Settings> settingsResult = mongoOps.findAll(Settings.class);
		Settings settings = settingsResult.get(0);
		Long nonce = settings.getNonceMtgox();
		
		Long nextTimestamp = (System.currentTimeMillis() + 4000L);
		
		if(nonce < nextTimestamp) {
			nonce = nextTimestamp;
		}
		
        args.put("nonce", ""+nonce);
        String post_data = buildQueryString(args);
        
        System.setProperty("jsse.enableSNIExtension", "false");

        URL queryUrl = new URL(path);
        connection = (HttpURLConnection) queryUrl.openConnection();
        connection.setDoOutput(true);
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimout);
        connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; mtgox-java client)");
        
        //new Thread(new InterruptThread(Thread.currentThread(), connection)).start(); // Kill the connection on timeout
        
        Mac mac = Mac.getInstance(SIGN_HASH_FUNCTION);
        SecretKeySpec secret_spec = new SecretKeySpec(Base64.decodeBase64(secret), SIGN_HASH_FUNCTION);
        mac.init(secret_spec);
        String signature = Base64.encodeBase64String(mac.doFinal(post_data.getBytes()));
        
        connection.setRequestProperty("Rest-Key", key);
        connection.setRequestProperty("Rest-Sign", signature);
        
        System.out.println("mtgox nonce: "+nonce);
        connection.getOutputStream().write(post_data.getBytes());
        System.out.println("write: "+post_data+"("+key+"/"+secret+") to "+path+", "+connection.getResponseCode());
        
        InputStream is = connection.getInputStream();
        System.out.println("got input stream: "+is);
        
        mongoOps.updateFirst(new Query(), new Update().set("nonceMtgox", nonce+1), Settings.class);
        
        return is;
        
    }

    protected InputStream getMtGoxHTTPInputStream(String path) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        return getMtGoxHTTPInputStream(path, new HashMap<String, String>());
    }
    
    private static String buildQueryString(HashMap<String, String> args) throws UnsupportedEncodingException {
    	
        String result = new String();
        for (String hashkey : args.keySet()) {
            if (result.length() > 0) {
                result += '&';
            }
            result += URLEncoder.encode(hashkey, "UTF-8") + "="
                    + URLEncoder.encode(args.get(hashkey), "UTF-8");

        }
        return result;
    }
    
    class InterruptThread implements Runnable {

        Thread parent;
        HttpURLConnection con;

        public InterruptThread(Thread parent, HttpURLConnection con) {
            this.parent = parent;
            this.con = con;
        }

        public void run() {
            try {
                Thread.sleep(killTimeout);
                if (con != null) {
                    con.disconnect();
                }
            } catch (Exception e) {
            }

        }
    }
 
	
}
