package com.rtamboli.anditwitt;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.Calendar;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class Twitter {
	
	private static String consumerKey=null;
	private static String consumerSecretKey=null;
	
	private static String callBackURL=null;
	
	private static String twitterUserId=null;
	private static String twitterScreenName=null;
	
	
	// the signature method used for all Twitter API calls
	private static final String SIGNATURE_METHOD="HMAC-SHA1";
	
	// call Twitter API with method type [ Get or Post ]
	private static final String REQUEST_GET="GET";
	private static final String REQUEST_POST="POST";
	
	// Twitter API's response status
	public static final int REQUEST_TOKEN_SUCCESS=1;
	public static final int REQUEST_TOKEN_FAIL=2;	
	public static final int ACCESS_TOKEN_SUCCESS=3;
	public static final int ACCESS_TOKEN_FAIL=4;
	public static final int POST_ON_TWITTER_SUCCESS=5;
	public static final int POST_ON_TWITTER_FAIL=6;
	public static final int OAUTH_SIGNATURE_GENERATE_FAIL=7;
	
	private static final String SIGNATURE_ERROR="signature_generate_fail";
	
	private static final String requestTokenURL = "https://api.twitter.com/oauth/request_token";
	private static final String accessTokenURL = "https://api.twitter.com/oauth/access_token";
	public static final String authorizeTokenURL = "https://api.twitter.com/oauth/authorize";
	private static final String twitterEndpointHost = "api.twitter.com";

	
	private static final String LOG_TAG="AndiTwitt";
	
	//region [ Set Consumer Key ]
	public static void setConsumerKey(final String _consumerKey) {
		consumerKey=_consumerKey;
	}
	//endregion
	
	//region [ Set Consumer Secret Key ]
	public static void setConsumerSecretKey(final String _consumerSecretKey) {
		consumerSecretKey=_consumerSecretKey;
	}
	//endregion
	
	//region [ Set Callback URl to catch twitter's respose ]
	public static void setCallBackURL(final String _callBackURL) {
		callBackURL=_callBackURL;
	}
	//endregion
	
	//region [ First Step ]
	// To start sign in twitter, first step is to request token to twitter server
	public void requestTokenToTwitter(final TwitterCallBack twitterCallBack) {
		
		String nonce=computeNonce();
		String timeStamp=computeTimeStamp();
		
		String parametersToPassToTwitter="oauth_callback="+encode(callBackURL)+"&oauth_consumer_key="+consumerKey+"&oauth_nonce="+nonce+"&oauth_signature_method="+SIGNATURE_METHOD+"&oauth_timestamp="+timeStamp+"&oauth_version=1.0";
		Log("ACTION - REQUEST TOKEN :: parameter string - "+parametersToPassToTwitter);
		
		String signatureBase=REQUEST_POST + "&"+ encode(requestTokenURL) + "&" + encode(parametersToPassToTwitter);
		Log("ACTION - REQUEST TOKEN :: signature base - "+signatureBase);
		
		String oAuthSignature=null;
		try {
			oAuthSignature=computeSignature(signatureBase, consumerSecretKey+"&");
			Log("ACTION - REQUEST TOKEN :: OAuth-Signature - "+ oAuthSignature);
			
			final String headerKeyValues="OAuth oauth_callback=\"" + encode(callBackURL) + "\", oauth_consumer_key=\"" + consumerKey + "\", oauth_nonce=\"" + nonce + "\", oauth_signature=\"" + encode(oAuthSignature) + "\", oauth_signature_method=\"HMAC-SHA1\", oauth_timestamp=\"" +timeStamp + "\", oauth_version=\"1.0\"";
			Log("ACTION - REQUEST TOKEN :: Key-Values of Header - "+ oAuthSignature);
			
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					String responseFromTwitterServer=callTwitterAPI(requestTokenURL,headerKeyValues,null);
					if(responseFromTwitterServer==null) {
						twitterCallBack.onResponse(REQUEST_TOKEN_FAIL,responseFromTwitterServer);
					} else {
						twitterCallBack.onResponse(REQUEST_TOKEN_SUCCESS,responseFromTwitterServer);
					}
				}
			}).start();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch(GeneralSecurityException e) {
			e.printStackTrace();
		}
	}
	//endregion
	
	
	
	public void getAccessTokenFromTwitter(final String oAuthToken,final String oAuthVerifier,final TwitterCallBack twitterCallBack) {
		String nonce=computeNonce();
		String timeStamp=computeTimeStamp();
		
		String parametersToPassToTwitter="oauth_callback="+encode(callBackURL)+"&oauth_consumer_key="+consumerKey+"&oauth_nonce="+nonce+"&oauth_signature_method="+SIGNATURE_METHOD+"&oauth_timestamp="+timeStamp+"&oauth_version=1.0";
		Log("ACTION - ACCESS TOKEN :: parameter string - "+parametersToPassToTwitter);
		
		String signatureBase=REQUEST_POST + "&"+ encode(accessTokenURL) + "&" + encode(parametersToPassToTwitter);
		Log("ACTION - ACCESS TOKEN :: signature base - "+signatureBase);
		
		String oAuthSignature=null;
		try {
			oAuthSignature=computeSignature(signatureBase, consumerSecretKey+"&");
			Log("ACTION - ACCESS TOKEN :: OAuth-Signature - "+ oAuthSignature);
			
			final String headerKeyValues="OAuth oauth_consumer_key=\"" + consumerKey + "\",oauth_signature_method=\"HMAC-SHA1\",oauth_timestamp=\"" + timeStamp + "\",oauth_nonce=\"" + nonce + "\",oauth_version=\"1.0\",oauth_signature=\"" + encode(oAuthSignature) + "\",oauth_token=\"" + encode(oAuthToken) + "\"";
			Log("ACTION - ACCESS TOKEN :: Key-Values of Header - "+ oAuthSignature);
			
			final String keyValue="oauth_verifier="+oAuthVerifier;
					
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					
					String responseFromTwitterServer=callTwitterAPI(accessTokenURL,headerKeyValues,keyValue);
					if(responseFromTwitterServer==null) {
						twitterCallBack.onResponse(ACCESS_TOKEN_FAIL,responseFromTwitterServer);
					} else {
						twitterCallBack.onResponse(ACCESS_TOKEN_SUCCESS,responseFromTwitterServer);
					}
				}
			}).start();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch(GeneralSecurityException e) {
			e.printStackTrace();
		}
	}
	
	
	private String callTwitterAPI(final String URL,final String headerData,final String bodyData) {
		
		BufferedReader in = null;
		StringBuilder sb = new StringBuilder();

		try {

			URL url = new URL(URL);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();			
			conn.setRequestProperty("Host",twitterEndpointHost);
			conn.setRequestProperty("Authorization", headerData);	
			conn.setRequestMethod("POST");
			
			if(bodyData!=null) {
				
				DataOutputStream out = new  DataOutputStream(conn.getOutputStream());
				out.writeBytes(bodyData);
				out.flush();
				out.close();
			}
			
			conn.connect();
			InputStream inst = conn.getInputStream();
			in = new BufferedReader(new InputStreamReader(inst));

			String temp = null;

			while((temp = in.readLine()) != null){
				sb.append(temp).append(" ");
			}
		}catch(Exception e){
			e.printStackTrace();
						
		}finally{
			if(in!=null){
				try{
					in.close();
				}catch(IOException e){
					e.printStackTrace();										
				}
			}
		}

		if(sb!=null){
			Log(sb.toString());
			return sb.toString();
		}

		return null;
		
	}
	
	
	
	// Twitter DOC : Parts of the Twitter API, particularly those dealing with OAuth signatures,
	// require strings to be encoded according to RFC 3986
	// DOC LINK : https://dev.twitter.com/docs/auth/percent-encoding-parameters
	public String encode(String value){

		String encoded = null;
		try {
			encoded = URLEncoder.encode(value, "UTF-8");
		} catch (UnsupportedEncodingException ignore) {
		}
		StringBuilder buf = new StringBuilder(encoded.length());
		char focus;
		for (int i = 0; i < encoded.length(); i++) {
			focus = encoded.charAt(i);
			if (focus == '*') {
				buf.append("%2A");
			} else if (focus == '+') {
				buf.append("%20");
			} else if (focus == '%' && (i + 1) < encoded.length()
					&& encoded.charAt(i + 1) == '7' && encoded.charAt(i + 2) == 'E') {
				buf.append('~');
				i += 2;
			} else {
				buf.append(focus);
			}
		}
		return buf.toString();
	}
	
	
	public static String computeSignature(String baseString, String keyString) throws GeneralSecurityException, UnsupportedEncodingException{

		SecretKey secretKey = null;

		byte[] keyBytes = keyString.getBytes();
		secretKey = new SecretKeySpec(keyBytes, "HmacSHA1");

		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(secretKey);

		byte[] text = baseString.getBytes();

		return new String(Base64.encodeBase64(mac.doFinal(text))).trim();
	}
	
	public String computeTimeStamp(){

		// get the timestamp
		Calendar tempcal = Calendar.getInstance();
		long ts = tempcal.getTimeInMillis();// get current time in milliseconds
		return (new Long(ts/1000)).toString(); // then divide by 1000 to get seconds
	}
	
	public String computeNonce(){

		// generate any fairly random alphanumeric string as the "nonce". Nonce = Number used ONCE.
		String uuid_string = UUID.randomUUID().toString();
		uuid_string = uuid_string.replaceAll("-", "");
		return  uuid_string; // any relatively random alphanumeric string will work here
	}
	
	public static void Log(String message){
		android.util.Log.i(LOG_TAG, "~~"+message+"~~");
	}
	
}
