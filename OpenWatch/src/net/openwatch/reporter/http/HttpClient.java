package net.openwatch.reporter.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import net.openwatch.reporter.R;
import net.openwatch.reporter.SECRETS;
import net.openwatch.reporter.constants.Constants;

import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.PersistentCookieStore;

public class HttpClient {
	private static final String TAG = "HttpClient";
	
	public static String USER_AGENT = null;

	/**
	 * Returns a new AsyncHttpClient initialized with a PersistentCookieStore
	 * 
	 * @param c
	 *            the context used to get SharePreferences for cookie
	 *            persistence and sslsocketfactory creation
	 * @return an initialized AsyncHttpClient
	 */
	public static AsyncHttpClient setupAsyncHttpClient(Context c) {
		AsyncHttpClient http_client = setupAsyncHttpClient();
		PersistentCookieStore cookie_store = new PersistentCookieStore(c);
		http_client.setCookieStore(cookie_store);
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
		http_client.setUserAgent(USER_AGENT);
		// Pin SSL cert if not hitting dev endpoint
		if(!Constants.USE_DEV_ENDPOINTS){
			http_client.setSSLSocketFactory(createApacheOWSSLSocketFactory(c));
		}
		return http_client;
	}

	// For non ssl, no-cookie store use
	
	private static AsyncHttpClient setupAsyncHttpClient() {
		return new AsyncHttpClient();
	}
	
	public static DefaultHttpClient setupDefaultHttpClient(Context c){
		DefaultHttpClient http_client = new DefaultHttpClient();
		PersistentCookieStore cookie_store = new PersistentCookieStore(c);
		http_client.setCookieStore(cookie_store);
		SSLSocketFactory socketFactory;
		try {
			// Pin SSL cert if not hitting dev endpoint
			if(!Constants.USE_DEV_ENDPOINTS){
				socketFactory = new SSLSocketFactory(loadOWKeyStore(c));
				Scheme sch = new Scheme("https", socketFactory, 443);
		        http_client.getConnectionManager().getSchemeRegistry().register(sch);
			}
	        return http_client;
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
