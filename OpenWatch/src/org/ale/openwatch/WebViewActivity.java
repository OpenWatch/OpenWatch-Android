package org.ale.openwatch;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.actionbarsherlock.app.SherlockActivity;

/**
 * Created by davidbrodsky on 7/5/13.
 */
public class WebViewActivity extends SherlockActivity {

    public WebView webView;

    public static final String URL_INTENT_KEY = "url";
    private static final String TAG = "WebViewActivity";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        webView = (WebView) findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url){
                // do your handling codes here, which url is the requested url
                // probably you need to open that url rather than redirect:
                view.loadUrl(url);
                return false; // then it is not handled by default action
            }
        });
        setUrl(getUrlFromIntent(getIntent()));
    }

    public void setUrl(String url){
        if(webView != null && url != null){
            Log.i(TAG, String.format("Setting webview url: %s", url));
            webView.loadUrl(url);
        }
    }

    public void onNewIntent(Intent i){
        super.onNewIntent(i);
        setUrl(getUrlFromIntent(i));

    }

    public String getUrlFromIntent(Intent i){
        if(i.hasExtra(URL_INTENT_KEY)){
            return i.getStringExtra(URL_INTENT_KEY);
        }
        return null;
    }

}