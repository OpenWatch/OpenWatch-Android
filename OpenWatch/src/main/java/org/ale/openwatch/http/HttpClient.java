package org.ale.openwatch.http;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.Log;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.PersistentCookieStore;
import org.ale.openwatch.R;

import org.ale.openwatch.SECRETS;
import org.ale.openwatch.constants.Constants;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.InputStream;
import java.security.*;

public class HttpClient {
	private static final String TAG = "HttpClient";

    private static final boolean USE_SSL_PINNING = true;
	
	public static String USER_AGENT = null;

    // Cache http clients
    private static AsyncHttpClient asyncHttpClient;

	/**
	 * Returns a new AsyncHttpClient initialized with a PersistentCookieStore
	 * 
	 * @param c
	 *            the context used to get SharePreferences for cookie
	 *            persistence and sslsocketfactory creation
	 * @return an initialized AsyncHttpClient
	 */
	public static AsyncHttpClient setupAsyncHttpClient(Context c) {
        if(asyncHttpClient != null){
            //Log.i("setupAsyncHttpClient","client cached");
            return asyncHttpClient;
        }else{
            //Log.i("setupAsyncHttpClient","client not cached");
        }
		AsyncHttpClient httpClient = setupVanillaAsyncHttpClient();
		PersistentCookieStore cookie_store = new PersistentCookieStore(c);
		httpClient.setCookieStore(cookie_store);
		// List cookies = cookie_store.getCookies();
		// Log.i(TAG, "Setting cookie store. size: " +
		// cookie_store.getCookies().size());
		if(USER_AGENT == null){
			USER_AGENT = Constants.USER_AGENT_BASE;
			try {
				PackageInfo pInfo = c.getPackageManager().getPackageInfo(
						c.getPackageName(), 0);
				USER_AGENT += pInfo.versionName;
			} catch (NameNotFoundException e) {
				Log.e(TAG, "Unable to read PackageName in RegisterApp");
				e.printStackTrace();
				USER_AGENT += "unknown";
			}
			USER_AGENT += " (Android API " + Build.VERSION.RELEASE + ")";
		}
		httpClient.setUserAgent(USER_AGENT);
		// Pin SSL cert if not hitting dev endpoint
		if(!Constants.USE_DEV_ENDPOINTS && USE_SSL_PINNING){
			httpClient.setSSLSocketFactory(createApacheOWSSLSocketFactory(c));
		}
        asyncHttpClient = httpClient;
		return asyncHttpClient;
	}

	// For non ssl, no-cookie store use
	
	private static AsyncHttpClient setupVanillaAsyncHttpClient() {
		return new AsyncHttpClient();
	}
	
	public static DefaultHttpClient setupDefaultHttpClient(Context c){
        //if(defaultHttpClient != null)
        //    return defaultHttpClient;

		DefaultHttpClient httpClient = new DefaultHttpClient();
		PersistentCookieStore cookie_store = new PersistentCookieStore(c);
		httpClient.setCookieStore(cookie_store);
		SSLSocketFactory socketFactory;
		try {
			// Pin SSL cert if not hitting dev endpoint
			if(!Constants.USE_DEV_ENDPOINTS && USE_SSL_PINNING){
				socketFactory = new SSLSocketFactory(loadOWKeyStore(c));
				Scheme sch = new Scheme("https", socketFactory, 443);
		        httpClient.getConnectionManager().getSchemeRegistry().register(sch);
			}
            //defaultHttpClient = httpClient;
	        //return defaultHttpClient;
            return httpClient;
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return null;
	}

	/**
	 * For Android-Async-Http's AsyncClient
	 * 
	 * @param context
	 * @return
	 */
	public static SSLSocketFactory createApacheOWSSLSocketFactory(
			Context context) {
		try {
			// Pass the keystore to the SSLSocketFactory. The factory is
			// responsible
			// for the verification of the server certificate.
			SSLSocketFactory sf = new SSLSocketFactory(loadOWKeyStore(context));
			// Hostname verification from certificate
			// http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d4e506
			sf.setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);
			return sf;
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}


	private static KeyStore loadOWKeyStore(Context c) {
		KeyStore trusted = null;
		try {
			// Get an instance of the Bouncy Castle KeyStore format
			trusted = KeyStore.getInstance("BKS");
			// Get the raw resource, which contains the keystore with
			// your trusted certificates (root and any intermediate certs)
			InputStream in = c.getResources().openRawResource(R.raw.owkeystore);
			try {
				// Initialize the keystore with the provided trusted
				// certificates
				// Also provide the password of the keystore
				trusted.load(in, SECRETS.SSL_KEYSTORE_PASS.toCharArray());
			} finally {
				in.close();
			}

		} catch (Exception e) {
			throw new AssertionError(e);
		}

		return trusted;
	}

    public static void clearCookieStore(Context c){
        PersistentCookieStore cookie_store = new PersistentCookieStore(c);
        cookie_store.clear();
    }

}
