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
 * If twitter tokens exist within Android's AccountManager: authenticateAndTweet(...) will retrieve them and send the authenticateAndTweet.
 *
 * If twitter tokens do not exist, authenticateAndTweet(..) will launch WebViewActivity with Twitter's authorization url, allow
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

    /**
     * Sets user's OAuth Token and Secret given Twitter's OAuth callback url.
     * @param c Application context
     * @param oauthCallbackUrl the url redirected to after OAuth authorization is complete. Contains oauth_verifier GET parameter
     * @param cb optional callback to perform if authentication successful
     */
    public static void twitterLoginConfirmation(final Context c, final String oauthCallbackUrl, final TwitterAuthCallback cb){
        new AsyncTask<String, Void, Boolean>(){

            @Override
            protected Boolean doInBackground(String... params) {
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
                    return true;
                } catch (TwitterException e) {
                    Log.e(TAG, "TwitterException on twitterLoginConfirmation");
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if(success){
                    if(cb != null)
                        cb.onAuth();
                }
                return;
            }
        }.execute();
    }

    public interface TwitterUserCallback{
        public void gotUser(User u);
    }

    public static void getUser(final Activity act, final TwitterUserCallback cb){

        new AsyncTask<String, Void, User>(){

            @Override
            protected User doInBackground(String... params) {
                TwitterFactory factory = new TwitterFactory(getTwitterConfiguration());
                Twitter twitter = factory.getInstance();
                try {
                    long i = twitter.getId();
                    return twitter.showUser(i);

                } catch (TwitterException e) {
                    Log.e(TAG, "Unable to get twitter user");
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(User twitterUser) {
                if(twitterUser != null){
                    if(cb != null)
                        cb.gotUser(twitterUser);
                }
            }
        }.execute();

    }

    public static void tweet(final Activity act, int serverObjectId){
        tweet(act, Share.generateShareText(act, OWServerObject.objects(act, OWServerObject.class).get(serverObjectId)));
    }

    /**
     * User facing method to send a authenticateAndTweet. Does not handle authentication.
     * @param act The initiating Activity, which should be prepared to capture the PIN resulting from Twitter's
     *            web authentication flow in it's OnActivityResult(...) and pass it to twitterLoginConfirmation(...)
     * @param status the text of the authenticateAndTweet
     */
    public static void tweet(final Activity act, final String status){


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
                                tweet(act, status);
                            }
                        });

                    }
                }
                return null;
            }
        }.execute(status);


    }

    public static void authenticateAndTweet(final Activity act, int serverObjectId){
        authenticateAndTweet(act, Share.generateShareText(act, OWServerObject.objects(act, OWServerObject.class).get(serverObjectId)));
    }

    /**
     * User facing method to send a authenticateAndTweet. Handles authentication.
     * @param act The initiating Activity, which should be prepared to capture the PIN resulting from Twitter's
     *            web authentication flow in it's OnActivityResult(...) and pass it to twitterLoginConfirmation(...)
     * @param status the text of the authenticateAndTweet
     */
    public static void authenticateAndTweet(final Activity act, final String status){

        final TwitterAuthCallback cb = new TwitterAuthCallback() {
            @Override
            public void onAuth() {
                tweet(act, status);
            }
        };

        TwitterFactory factory = new TwitterFactory(getTwitterConfiguration());
        Twitter twitter = factory.getInstance();

        try {
            authenticate(act, cb);
            // Initiating Activity will handle calling tweet() after pin confirmation
        } catch(IllegalStateException ie){
            if (!twitter.getAuthorization().isEnabled()) {
                Log.e(TAG, "OAuth consumer key/secret not set!");
            }else{
                // AccessToken already valid.
                //tweet(act, params[0]);
            }
        }

    }

    public static boolean isAuthenticated(Context c){
        if(OAUTH_TOKEN != null && OAUTH_SECRET != null){
            Log.i(TAG, String.format("is authed OAUTH_TOKEN: %s, OAUTH SEC: %s", OAUTH_TOKEN, OAUTH_SECRET));
            return true;
        }

        SharedPreferences prefs = c.getSharedPreferences(Constants.PROFILE_PREFS, c.MODE_PRIVATE);
        if(prefs.contains(Constants.TWITTER_TOKEN) && prefs.contains(Constants.TWITTER_SECRET)){
            Log.i(TAG, String.format("is authed prefs OAUTH_TOKEN: %s, OAUTH SEC: %s", prefs.getString(Constants.TWITTER_TOKEN,""), prefs.getString(Constants.TWITTER_SECRET,"")));
            return true;
        }else{
            return false;
        }
    }

    public static void disconnect(final Context c){
        OAUTH_TOKEN = null;
        OAUTH_SECRET = null;
        new Thread(){
            public void run(){
                SharedPreferences.Editor prefs = c.getSharedPreferences(Constants.PROFILE_PREFS, c.MODE_PRIVATE).edit();
                prefs.remove(Constants.TWITTER_TOKEN);
                prefs.remove(Constants.TWITTER_SECRET);
                prefs.commit();
            }
        }.start();


    }

    /**
     * Attempt to retrieve Twitter OAuth tokens from Android's AccountManager. If no such tokens exist,
     * call getTwitterOAuthViaWeb(...), which will initiate authentication via Twitter's Web flow.
     * @param act The initiating Activity, which should be prepared to capture the PIN resulting from Twitter's
     *            web authentication flow in it's OnActivityResult(...) and pass it to twitterLoginConfirmation(...)
     * @param cb an optional callback with actions to perform after the user is authenticated with Twitter
     */
    public static void authenticate(final Activity act, final TwitterAuthCallback cb){
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
        usingAccountManager = false;
        Log.i(TAG, "unable to get twitter acct via AccountManager. Trying via web");

        getTwitterOAuthViaWeb(act);
        // We don't pass the callback here because getTwitterOAuthViaWeb will require the user
        // getting the oauth confirmation pin via webview and entering it into the initiating
        // activity via OnActivityResult.

    }

    /**
     *
     * @param act the activity to act which will prompt the user for the PIN provided by Twitter's authorization web flow
     *            in it's OnActivityResult(...) method
     * @return true if the token is already valid, false if web flow was initiated or an error occurred
     */
    private static void getTwitterOAuthViaWeb(final Activity act){
        new AsyncTask<Void, Void, RequestToken>(){

            @Override
            protected RequestToken doInBackground(Void... params) {
                TwitterFactory factory = new TwitterFactory(getTwitterConfiguration());
                Twitter twitter = factory.getInstance();
                RequestToken requestToken = null;
                try {
                    return twitter.getOAuthRequestToken();
                } catch (TwitterException e) {
                    e.printStackTrace();
                }catch(IllegalStateException ie){
                    if (!twitter.getAuthorization().isEnabled()) {
                        Log.e(TAG, "OAuth consumer key/secret not set!");
                    }else{
                        // AccessToken already valid.
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(RequestToken requestToken) {
                lastLoginRequestToken = requestToken;
                if(requestToken.getAuthenticationURL() != null){
                    Intent i = new Intent(act.getApplicationContext(), WebViewActivity.class);
                    Log.i(TAG, requestToken.getAuthenticationURL());
                    i.putExtra(WebViewActivity.URL_INTENT_KEY, requestToken.getAuthenticationURL());
                    act.startActivityForResult(i, TWITTER_RESULT);
                }
            }
        }.execute();

    }


}
