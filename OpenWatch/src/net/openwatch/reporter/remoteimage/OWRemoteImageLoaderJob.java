package net.openwatch.reporter.remoteimage;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import net.openwatch.reporter.R;
import net.openwatch.reporter.SECRETS;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.github.ignition.support.cache.ImageCache;
import com.github.ignition.support.images.remote.RemoteImageLoaderHandler;

public class OWRemoteImageLoaderJob implements Runnable {

    private static final String LOG_TAG = "Ignition/ImageLoader";

    private static final int DEFAULT_RETRY_HANDLER_SLEEP_TIME = 1000;

    private Context context;
    private String imageUrl;
    private RemoteImageLoaderHandler handler;
    private ImageCache imageCache;
    private int numRetries, defaultBufferSize;

    public OWRemoteImageLoaderJob(Context context, String imageUrl, RemoteImageLoaderHandler handler, ImageCache imageCache,
            int numRetries, int defaultBufferSize) {
        this.context = context;
    	this.imageUrl = imageUrl;
        this.handler = handler;
        this.imageCache = imageCache;
        this.numRetries = numRetries;
        this.defaultBufferSize = defaultBufferSize;
    }

    /**
     * The job method run on a worker thread. It will first query the image cache, and on a miss,
     * download the image from the Web.
     */
    @Override
    public void run() {
        Bitmap bitmap = null;

        if (imageCache != null) {
            // at this point we know the image is not in memory, but it could be cached to SD card
            bitmap = imageCache.getBitmap(imageUrl);
        }

        if (bitmap == null) {
            bitmap = downloadImage();
        }

        notifyImageLoaded(imageUrl, bitmap);
    }

    // TODO: we could probably improve performance by re-using connections instead of closing them
    // after each and every download
    protected Bitmap downloadImage() {
        int timesTried = 1;

        while (timesTried <= numRetries) {
            try {
                byte[] imageData = retrieveImageData();

                if (imageData == null) {
                    break;
                }

                if (imageCache != null) {
                    imageCache.put(imageUrl, imageData);
                }

                return BitmapFactory.decodeByteArray(imageData, 0, imageData.length);

            } catch (Throwable e) {
                Log.w(LOG_TAG, "download for " + imageUrl + " failed (attempt " + timesTried + ")");
                e.printStackTrace();
                SystemClock.sleep(DEFAULT_RETRY_HANDLER_SLEEP_TIME);
                timesTried++;
            }
        }

        return null;
    }

    protected byte[] retrieveImageData() throws IOException {
        URL url = new URL(imageUrl);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        
        // Keystore + TrustManager
        KeyStore keyStore;
		try {
			keyStore = KeyStore.getInstance("BKS");
	        InputStream in = context.getResources().openRawResource(R.raw.owkeystore);
	        try {
	            // Initialize the keystore with the provided trusted certificates
	            // Also provide the password of the keystore
	        	keyStore.load(in, SECRETS.SSL_KEYSTORE_PASS.toCharArray());
	        	TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
	        	tmf.init(keyStore);
	        	
	        	SSLContext context = SSLContext.getInstance("TLS");
	        	context.init(null, tmf.getTrustManagers(), null);
	        	
	        	connection.setSSLSocketFactory(context.getSocketFactory());
	        	Log.i(LOG_TAG, "OWRemoteImageLoaderJob set SSLSocketFactory");
	        } catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (CertificateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (KeyManagementException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
	            in.close();
	        }
		} catch (KeyStoreException e1) {
			e1.printStackTrace();
		}
		
		
        // determine the image size and allocate a buffer
        int fileSize = connection.getContentLength();
        Log.d(LOG_TAG, "fetching image " + imageUrl + " (" + (fileSize <= 0 ? "size unknown" : Integer.toString(fileSize)) + ")");

        BufferedInputStream istream = new BufferedInputStream(connection.getInputStream());

        try {   
            if (fileSize <= 0) {
                Log.w(LOG_TAG,
                        "Server did not set a Content-Length header, will default to buffer size of "
                                + defaultBufferSize + " bytes");
                ByteArrayOutputStream buf = new ByteArrayOutputStream(defaultBufferSize);
                byte[] buffer = new byte[defaultBufferSize];
                int bytesRead = 0;
                while (bytesRead != -1) {
                    bytesRead = istream.read(buffer, 0, defaultBufferSize);
                    if (bytesRead > 0)
                        buf.write(buffer, 0, bytesRead);
                }
                return buf.toByteArray();
            } else {
                byte[] imageData = new byte[fileSize];
        
                int bytesRead = 0;
                int offset = 0;
                while (bytesRead != -1 && offset < fileSize) {
                    bytesRead = istream.read(imageData, offset, fileSize - offset);
                    offset += bytesRead;
                }
                return imageData;
            }
        } finally {
            // clean up
            try {
                istream.close();
                connection.disconnect();
            } catch (Exception ignore) { }
        }
    }

    protected void notifyImageLoaded(String url, Bitmap bitmap) {
        Message message = new Message();
        message.what = RemoteImageLoaderHandler.HANDLER_MESSAGE_ID;
        Bundle data = new Bundle();
        data.putString(RemoteImageLoaderHandler.IMAGE_URL_EXTRA, url);
        Bitmap image = bitmap;
        data.putParcelable(RemoteImageLoaderHandler.BITMAP_EXTRA, image);
        message.setData(data);

        handler.sendMessage(message);
    }
}
