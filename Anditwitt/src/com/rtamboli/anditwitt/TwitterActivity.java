package com.rtamboli.anditwitt;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

public class TwitterActivity extends Activity implements TwitterCallBack{

	private Twitter twitter;
	
	// this will be used to load twitter login page.
	private WebView webView;
	
	private ProgressDialog pd=null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_twitter);
		
		webView=(WebView)findViewById(R.id.webView);

		showDefaultProgress();
		
		twitter=new Twitter();
		
		// first set consumer and secret key
		Twitter.setConsumerKey("your consumer key");
		Twitter.setConsumerSecretKey("your consumer secret key");
		
		// as our application is android, we need to catch the response return from the web login page (i.e. callback), "oob" set.
		Twitter.setCallBackURL(getResources().getString(R.string.app_name)+"://callback");
		
		/* *****************
		 *      STEP 1 
		 * ***************** */
		 
		twitter.requestTokenToTwitter(this);
		//above method will return response of server in "onResponse" method
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		
		/* *****************
		 *      STEP 3
		 * ***************** */
		if(twitter!=null) {
			
			showDefaultProgress();
			
			webView.setVisibility(View.GONE);
			
			// check response from twitter server is null(fail) or success.
			if(intent!=null) {
				String responseFromTwitterServer=intent.getData().toString();
				
				if(responseFromTwitterServer.contains("oauth_verifier") && responseFromTwitterServer.contains("oauth_token")) {
					// check that required data return in response from twitter server.
					
					String[] returnParametersFromTwitter=responseFromTwitterServer.split("[?]");
					String keyValues[]=returnParametersFromTwitter[1].split("&");
					final String oAuthToken=keyValues[0].split("=")[1];
					final String oAuthVerifier=keyValues[1].split("=")[1];
					
					
					// now get access token from oAuthVerifier return from twitter server which will be used to get user details
					twitter.getAccessTokenFromTwitter(oAuthToken, oAuthVerifier, TwitterActivity.this);
				} else {
					//show toast for twitter fail.
				}
			} else {
				//show toast for twitter fail.
			}
		}
	}

	//region [ On Twitter Server Response ]
	@Override
	public void onResponse(int requestedAction, String twitterResponse) {

		if(requestedAction==Twitter.REQUEST_TOKEN_SUCCESS) {
			
			// now extract token and token secret returned by twitter server in response
			String[] returnedResultFromTwitter=twitterResponse.split("&");
			
			if(returnedResultFromTwitter.length==3) {
				// it means twitter server returned the required data in response successfully.
				
				// twitter returns response in key=value format. so split it to get value from it.
				final String oAuthToken=returnedResultFromTwitter[0].split("=")[1];
				final String oAuthTokenSecret=returnedResultFromTwitter[1].split("=")[1];
				
				
				// now step 2 is redirect user to twitter login page with oAuthToken return in 1st step.
				// below url format given in twitter online doc.
				final String twitterLoginUrl=Twitter.authorizeTokenURL+"?oauth_token="+oAuthToken;
				
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						
						/* *****************
						 *      STEP 2 
						 * ***************** */
						webView.loadUrl(twitterLoginUrl);
						
						//use below one when you need to login as another user with auto clearing previous login session.
						// param force_login=true will show forcefully login screen with new session.
						// FYI: twitter doesn't support logout (i.e. session clear) direct. So for logout, also use
						// below one.
						//webView.loadUrl(twitterLoginUrl+"&force_login=true");
						
						
						// ONCE USER LOGGED IN, CALLBACK URL will be fall in "OnNewIntent" callback method of android.
						
						stopProgress();
					}
				});
				
				
			}
		} else if(requestedAction==Twitter.REQUEST_TOKEN_FAIL) {
			
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					//show toast of twitter request fail
					stopProgress();
				}
			});
		} else if(requestedAction==Twitter.ACCESS_TOKEN_SUCCESS) {
			
			/* ************************
			 *      FINAL OUTPUT
			 * ************************ */
			if(twitterResponse.contains("user_id")) {
				
				String[] parameterArray=twitterResponse.split("&");
				String twitterUserId=parameterArray[2].split("=")[1];
				String twitterScreenName=parameterArray[3].split("=")[1];
				
				Log.i("AndiTwitt", "User ID : "+twitterUserId);
				Log.i("AndiTwitt", "User Screen Name : "+twitterScreenName);
				//you successfully got user's screen name. show screen name as you want to display.
				//you can also tweet using above userid details.
				
				stopProgress();
				
			} else {
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						//show toast of twitter request fail
						stopProgress();
					}
				});
			}
			
		} else if(requestedAction==Twitter.ACCESS_TOKEN_FAIL) {
			
			runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					//show toast of twitter request fail
					stopProgress();
				}
			});
		}
	}
	//endregion
	
	//region [ show progressbar ]
	public void showDefaultProgress() {
        if (pd == null)
            pd = new ProgressDialog(this);
        pd.setMessage("please wait...");
        pd.setCancelable(false);
        pd.show();
    }
	//endregion
	
	//region [ stop progress ]
	public void stopProgress(){
		if(pd!=null)
			pd.dismiss();
	}
	//endregion
	
}
