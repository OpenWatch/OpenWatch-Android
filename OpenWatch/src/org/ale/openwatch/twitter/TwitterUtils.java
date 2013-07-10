package org.ale.openwatch.twitter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import org.ale.openwatch.SECRETS;
import org.ale.openwatch.WebViewActivity;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.model.OWServerObject;
import org.ale.openwatch.share.Share;
import twitter4j.*;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Created by davidbrodsky on 7/5/13.
 *
 * If twitter tokens exist within Android's AccountManager: tweet(...) will retrieve them and send the tweet.
 *
 * If twitter tokens do not exist, tweet(..) will launch WebViewActivity with Twitter's authorization url, allow
 * the user to authorize our app, and will store the resulting tokens. The initiating activity must catch WebViewActivity's
 * conclusion in OnActivityResult and direct the user provided pin to twitterLoginConfirmation(...) with an optional
 * TwitterAuthCallback defining the action to take after login is complete.
 */
public class TwitterUtils {
    private static final String TAG = "TwitterUtils";
    public static final int TWITTER_RESULT = 31415;

    private static String OAUTH_TOKEN = null;
    private static String OAUTH_SECRET = null;

    private static RequestToken lastLoginRequestToken;
    private static int twitterAccountId = 0; // Index in Accounts[] returned by AccountManager

    private static final String TWITTER_ACCOUNT_TYPE = "com.twitter.android.auth.login";
    private static final String TWITTER_TOKEN = "com.twitter.android.oauth.token";
    private static final String TWITTER_SECRET = "com.twitter.android.oauth.token.secret";

    private static boolean usingAccountManager = false;

    private static final int maxAuthRetries = 1;
    private static int numAuthRetries = 0;

    public interface TwitterAuthCallback{
        public void onAuth();
    }

    private static Configuration getTwitterConfiguration(){
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setUseSSL(true); // default=false. wtf.
        builder.setOAuthConsumerKey(Constants.TWITTER_CONSUMER_KEY);
        builder.setOAuthConsumerSecret(SECRETS.TWITTER_CONSUMER_SECRET);

        if(OAUTH_SECRET != null && OAUTH_TOKEN != null){
            builder.setOAuthAccessToken(OAUTH_TOKEN)
                    .setOAuthAccessTokenSecret(OAUTH_SECRET);
        }else
            Log.e(TAG, "OAUTH_TOKEN or SECRET null on getTwitterConfiguration()");
        return builder.build();
    }

    public static void twitterLoginConfirmation(final Context c, final String oauthCallbackUrl, final TwitterAuthCallback cb){
        new AsyncTask<String, Void, Void>(){

            @Override
            protected Void doInBackground(String... params) {
                TwitterFactory factory = new TwitterFactory(getTwitterConfiguration());
                Twitter twitter = factory.getInstance();
                try {

                    Uri uri=Uri.parse(oauthCallbackUrl);
                    String oauthVerifier = uri.getQueryParameter("oauth_verifier");
                    Log.i(TAG, String.format("callback url: %s. verifier: %s", oauthCallbackUrl, oauthVerifier));
                    AccessToken accessToken = twitter.getOAuthAccessToken(lastLoginRequestToken, oauthVerifier);
                    OAUTH_TOKEN = accessToken.getToken();
                    OAUTH_SECRET = accessToken.getTokenSecret();
                    Log.i(TAG, String.format("Got oath access from loginConfirmation token: %s, secret: %s", OAUTH_TOKEN, OAUTH_SECRET));
                    if(!usingAccountManager){
                        SharedPreferences.Editor prefs = c.getSharedPreferences(Constants.PROFILE_PREFS, c.MODE_PRIVATE).edit();
                        prefs.putString(Constants.TWITTER_TOKEN, accessToken.getToken());
                        prefs.putString(Constants.TWITTER_SECRET, accessToken.getTokenSecret());
                        prefs.commit();
                    }
                    if(cb != null)
                        cb.onAuth();
                } catch (TwitterException e) {
                    Log.e(TAG, "TwitterException on twitterLoginConfirmation");
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }
    /*
    public static void twitterLoginConfirmation(final Context c, final TwitterAuthCallback cb){

        new AsyncTask<String, Void, Void>(){

            @Override
            protected Void doInBackground(String... params) {
            TwitterFactory factory = new TwitterFactory(getTwitterConfiguration());
            Twitter twitter = factory.getInstance();
            try {

                AccessToken accessToken = twitter.getOAuthAccessToken();
                OAUTH_TOKEN = accessToken.getToken();
                OAUTH_SECRET = accessToken.getTokenSecret();
                Log.i(TAG, String.format("Got oath access from loginConfirmation token: %s, secret: %s", OAUTH_TOKEN, OAUTH_SECRET));
                if(!usingAccountManager){
                    SharedPreferences.Editor prefs = c.getSharedPreferences(Constants.PROFILE_PREFS, c.MODE_PRIVATE).edit();
                    prefs.putString(Constants.TWITTER_TOKEN, accessToken.getToken());
                    prefs.putString(Constants.TWITTER_SECRET, accessToken.getTokenSecret());
                    prefs.commit();
                }
                if(cb != null)
                    cb.onAuth();
            } catch (TwitterException e) {
                Log.e(TAG, "TwitterException on twitterLoginConfirmation");
                e.printStackTrace();
            }
            return null;
            }
        }.execute();
    }

    */

    public static void updateStatus(final Activity act, int serverObjectId){
        updateStatus(act, Share.generateShareText(act, OWServerObject.objects(act, OWServerObject.class).get(serverObjectId)));
    }

    /**
     * User facing method to send a tweet. Does not handle authentication.
     * @param act The initiating Activity, which should be prepared to capture the PIN resulting from Twitter's
     *            web authentication flow in it's OnActivityResult(...) and pass it to twitterLoginConfirmation(...)
     * @param status the text of the tweet
     */
    public static void updateStatus(final Activity act, final String status){


        new AsyncTask<String, Void, Void>(){

            @Override
            protected Void doInBackground(String... params) {
                ConfigurationBuilder builder = new ConfigurationBuilder();
                if(OAUTH_TOKEN == null || OAUTH_SECRET == null){
                    Log.e(TAG, "OAUTH credentials are null, aborting tweet.");
                    return null;
                }
                builder.setOAuthConsumerKey(Constants.TWITTER_CONSUMER_KEY);
                builder.setOAuthConsumerSecret(SECRETS.TWITTER_CONSUMER_SECRET);
                builder.setOAuthAccessToken(OAUTH_TOKEN);
                builder.setOAuthAccessTokenSecret(OAUTH_SECRET);
                Log.i(TAG, String.format("Attempting to tweet with token %s, secret %s", OAUTH_TOKEN, OAUTH_SECRET));
                Configuration configuration = builder.build();

                TwitterFactory factory = new TwitterFactory(configuration);

                Twitter twitter = factory.getInstance();
                try {
                    twitter4j.Status tweet = twitter.updateStatus(params[0]);
                } catch (TwitterException e) {
                    numAuthRetries ++;
                    Log.i(TAG, String.format("Got TwitterException code %d, message %s", e.getErrorCode(), e.getMessage()));
                    e.printStackTrace();
                    if(e.getMessage().hashCode() == 1267074619){ // "No authentication challenges found"
                        // If the token is invalid, attempt to get another, and retry update status.
                        if(usingAccountManager){
                            AccountManager.get(act.getBaseContext()).invalidateAuthToken(TWITTER_ACCOUNT_TYPE, OAUTH_TOKEN);
                            AccountManager.get(act.getBaseContext()).invalidateAuthToken(TWITTER_ACCOUNT_TYPE, OAUTH_SECRET);
                            Log.i(TAG, String.format("Invalidating account %s token: %s secret: %s", TWITTER_ACCOUNT_TYPE, OAUTH_TOKEN, OAUTH_SECRET));

                        }else{
                            SharedPreferences.Editor prefs = act.getSharedPreferences(Constants.PROFILE_PREFS, act.MODE_PRIVATE).edit();
                            prefs.remove(Constants.TWITTER_TOKEN);
                            prefs.remove(Constants.TWITTER_SECRET);
                            prefs.commit();
                            Log.e(TAG, "Failed auth. Clearing SharedPreferences tokens.");
                        }
                        if(numAuthRetries <= maxAuthRetries)
                            authenticate(act, new TwitterAuthCallback() {
                            @Override
                            public void onAuth() {
                                updateStatus(act, status);
                            }
                        });

                    }
                }
                return null;
            }
        }.execute(status);


    }

    public static void tweet(final Activity act, int serverObjectId){
        tweet(act, Share.generateShareText(act, OWServerObject.objects(act, OWServerObject.class).get(serverObjectId)));
    }

    /**
     * User facing method to send a tweet. Handles authentication.
     * @param act The initiating Activity, which should be prepared to capture the PIN resulting from Twitter's
     *            web authentication flow in it's OnActivityResult(...) and pass it to twitterLoginConfirmation(...)
     * @param status the text of the tweet
     */
    public static void tweet(final Activity act, final String status){

        final TwitterAuthCallback cb = new TwitterAuthCallback() {
            @Override
            public void onAuth() {
                updateStatus(act, status);
            }
        };
        new AsyncTask<Void, Void, Void>(){

            @Override
            protected Void doInBackground(Void... params) {
                TwitterFactory factory = new TwitterFactory(getTwitterConfiguration());
                Twitter twitter = factory.getInstance();

                try {
                    authenticate(act, cb);
                    // Initiating Activity will handle calling updateStatus() after pin confirmation
                } catch(IllegalStateException ie){
                    if (!twitter.getAuthorization().isEnabled()) {
                        Log.e(TAG, "OAuth consumer key/secret not set!");
                    }else{
                        // AccessToken already valid.
                        //updateStatus(act, params[0]);
                    }
                }
                return null;
            }
        }.execute();

    }

    /**
     * Attempt to retrieve Twitter OAuth tokens from Android's AccountManager. If no such tokens exist,
     * call getTwitterOAuthViaWeb(...), which will initiate authentication via Twitter's Web flow.
     * @param act The initiating Activity, which should be prepared to capture the PIN resulting from Twitter's
     *            web authentication flow in it's OnActivityResult(...) and pass it to twitterLoginConfirmation(...)
     * @param cb an optional callback with actions to perform after the user is authenticated with Twitter
     */
    private static void authenticate(final Activity act, final TwitterAuthCallback cb){
        // First see if tokens are stored
        SharedPreferences prefs = act.getSharedPreferences(Constants.PROFILE_PREFS, act.MODE_PRIVATE);
        if(prefs.contains(Constants.TWITTER_TOKEN) && prefs.contains(Constants.TWITTER_SECRET)){
            usingAccountManager = false;
            OAUTH_TOKEN = prefs.getString(Constants.TWITTER_TOKEN,"");
            OAUTH_SECRET = prefs.getString(Constants.TWITTER_SECRET, "");
            Log.i(TAG, "got twitter credentials from sharedPreferences");
            if(cb != null)
                cb.onAuth();
            return;
        }

        // Next try getting Twitter credentials from Android's AccountManager
        AccountManager am = AccountManager.get(act.getApplicationContext());
        Account[] accts = am.getAccountsByType(TWITTER_ACCOUNT_TYPE);
        Log.i(TAG, String.format("Found %d accounts for type %s", accts.length, TWITTER_ACCOUNT_TYPE));
        boolean FORCE_WEB_AUTH = true;
        if(accts.length > 0 && !FORCE_WEB_AUTH) {
            usingAccountManager = true;
            // TODO: Show Account Chooser
            Account acct = accts[0];
            twitterAccountId = 0;
            OAUTH_TOKEN = null;
            OAUTH_SECRET = null;
            am.getAuthToken(acct, TWITTER_TOKEN, null, act, new AccountManagerCallback<Bundle>() {

                @Override
                public void run(AccountManagerFuture<Bundle> arg0) {
                    try {
                        Bundle b = arg0.getResult();
                        Log.i(TAG, "Got com.twitter.android.oauth.token: " + b.getString(AccountManager.KEY_AUTHTOKEN));
                        OAUTH_TOKEN = b.getString(AccountManager.KEY_AUTHTOKEN);
                        if(OAUTH_SECRET != null && cb != null){
                            Log.i(TAG, String.format("Prepare to Tweet with Token: %s, Secret: %s", OAUTH_TOKEN, OAUTH_SECRET));
                            cb.onAuth();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "EXCEPTION@AUTHTOKEN");
                    }
                }}, null);

            am.getAuthToken(acct, TWITTER_SECRET, null, act, new AccountManagerCallback<Bundle>() {

                @Override
                public void run(AccountManagerFuture<Bundle> arg0) {
                    try {
                        Bundle b = arg0.getResult();
                        Log.i(TAG, "THIS AUTHTOKEN SECRET: " + b.getString(AccountManager.KEY_AUTHTOKEN));
                        OAUTH_SECRET = b.getString(AccountManager.KEY_AUTHTOKEN);
                        if(OAUTH_TOKEN != null && cb != null){
                            Log.i(TAG, String.format("Prepare to Tweet with Token: %s, Secret: %s", OAUTH_TOKEN, OAUTH_SECRET));
                            cb.onAuth();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "EXCEPTION@AUTHTOKEN");
                    }
                }}, null);
        }else{
            usingAccountManager = false;
            // Twitter account not stored in AccountManager.
            // First, see if we have stored Twitter tokens in SharedPreferences
                // Attempt to auth via http
                Log.i(TAG, "unable to get twitter acct via AccountManager. Trying via web");
                getTwitterOAuthViaWeb(act);
                // We don't pass the callback here because getTwitterOAuthViaWeb will require the user
                // getting the oauth confirmation pin via webview and entering it into the initiating
                // activity via OnActivityResult.
        }
    }

    /**
     *
     * @param act the activity to act which will prompt the user for the PIN provided by Twitter's authorization web flow
     *            in it's OnActivityResult(...) method
     * @return true if the token is already valid, false if web flow was initiated or an error occurred
     */
    private static boolean getTwitterOAuthViaWeb(Activity act){
        TwitterFactory factory = new TwitterFactory(getTwitterConfiguration());
        Twitter twitter = factory.getInstance();
        RequestToken requestToken = null;
        try {
            requestToken = twitter.getOAuthRequestToken();
        } catch (TwitterException e) {
            e.printStackTrace();
        }catch(IllegalStateException ie){
            if (!twitter.getAuthorization().isEnabled()) {
                Log.e(TAG, "OAuth consumer key/secret not set!");
            }else{
                // AccessToken already valid.
                return true;
            }
        }

        lastLoginRequestToken = requestToken;
        if(requestToken.getAuthenticationURL() != null){
            Intent i = new Intent(act.getApplicationContext(), WebViewActivity.class);
            Log.i(TAG, requestToken.getAuthenticationURL());
            i.putExtra(WebViewActivity.URL_INTENT_KEY, requestToken.getAuthenticationURL());
            act.startActivityForResult(i, TWITTER_RESULT);
            return false;
        }
        return false;
    }


}
