package net.openwatch.reporter.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.List;

import net.openwatch.reporter.R;
import net.openwatch.reporter.SECRETS;

import org.apache.http.conn.ssl.SSLSocketFactory;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.PersistentCookieStore;

public class HttpClient {
	private static final String TAG = "HttpClient";
	
	/**
	 * Returns a new AsyncHttpClient initialized with a PersistentCookieStore
	 * @param c the context used to get SharePreferences for cookie persistence and sslsocketfactory creation
	 * @return an initialized AsyncHttpClient
	 */
	public static AsyncHttpClient setupHttpClient(Context c){
		AsyncHttpClient http_client = setupHttpClient();
    	PersistentCookieStore cookie_store = new PersistentCookieStore(c);
    	http_client.setCookieStore(cookie_store);
    	//List cookies = cookie_store.getCookies();
    	//Log.i(TAG, "Setting cookie store. size: " + cookie_store.getCookies().size());
    	http_client.setSSLSocketFactory(createApacheOWSSLSocketFactory(c));
    	return http_client;
    }
	
	// For non ssl, no-cookie store use
	public static AsyncHttpClient setupHttpClient(){		
		return new AsyncHttpClient();
	}
	
	public static SSLSocketFactory createApacheOWSSLSocketFactory(Context context) {
        try {
            // Get an instance of the Bouncy Castle KeyStore format
            KeyStore trusted = KeyStore.getInstance("BKS");
            // Get the raw resource, which contains the keystore with
            // your trusted certificates (root and any intermediate certs)
            InputStream in = context.getResources().openRawResource(R.raw.owkeystore);
            try {
                // Initialize the keystore with the provided trusted certificates
                // Also provide the password of the keystore
                trusted.load(in, SECRETS.SSL_KEYSTORE_PASS.toCharArray());
            } finally {
                in.close();
            }
            // Pass the keystore to the SSLSocketFactory. The factory is responsible
            // for the verification of the server certificate.
            SSLSocketFactory sf = new SSLSocketFactory(trusted);
            // Hostname verification from certificate
            // http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d4e506
            sf.setHostnameVerifier(SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);
            return sf;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

}
