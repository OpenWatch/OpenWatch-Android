package net.openwatch.reporter.http;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.PersistentCookieStore;

public class HttpClient {
	
	/**
	 * Returns a new AsyncHttpClient initialized with a PersistentCookieStore
	 * @param c the context used to get SharePreferences for cookie persistence
	 * @return an initialized AsyncHttpClient
	 */
	public static AsyncHttpClient setupHttpClient(Context c){
		AsyncHttpClient http_client = setupHttpClient();
    	PersistentCookieStore cookie_store = new PersistentCookieStore(c);
    	http_client.setCookieStore(cookie_store);
    	
    	return http_client;
    }
	
	public static AsyncHttpClient setupHttpClient(){
		return new AsyncHttpClient();
	}

}
