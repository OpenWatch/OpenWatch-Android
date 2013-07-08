package org.ale.openwatch.twitter;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
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
 */
public class TwitterUtils {
    private static final String TAG = "TwitterUtils";
    //65535
    public static final int TWITTER_RESULT = 31415;

    private static String OAUTH_TOKEN = null;
    private static String OAUTH_SECRET = null;

    private static RequestToken lastLoginRequestToken;

    private static Configuration getTwitterConfiguration(){
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(Constants.TWITTER_CONSUMER_KEY);
        builder.setOAuthConsumerSecret(SECRETS.TWITTER_CONSUMER_SECRET);
        if(OAUTH_SECRET != null && OAUTH_TOKEN != null){
            builder.setOAuthAccessToken(OAUTH_TOKEN)
                    .setOAuthAccessTokenSecret(OAUTH_SECRET);
        }
        return builder.build();
    }

    public static void twitterLoginConfirmation(final Activity act, String pin, final int serverObjectId){

        new AsyncTask<String, Void, Void>(){

            @Override
            protected Void doInBackground(String... params) {
            TwitterFactory factory = new TwitterFactory(getTwitterConfiguration());
            Twitter twitter = factory.getInstance();
            try {
                AccessToken accessToken = twitter.getOAuthAccessToken(lastLoginRequestToken, params[0]);
                OAUTH_TOKEN = accessToken.getToken();
                OAUTH_SECRET = accessToken.getTokenSecret();

                updateStatus(act, Share.generateShareText(act.getBaseContext(), OWServerObject.objects(act.getApplicationContext(), OWServerObject.class).get(serverObjectId)));
            } catch (TwitterException e) {
                e.printStackTrace();
            }
            return null;
            }
        }.execute(pin);
    }

    public static void updateStatus(final Activity act, int serverObjectId){
        updateStatus(act, Share.generateShareText(act, OWServerObject.objects(act, OWServerObject.class).get(serverObjectId)));
    }

    public static void updateStatus(final Activity act, String status){


        new AsyncTask<String, Void, Void>(){

            @Override
            protected Void doInBackground(String... params) {
                ConfigurationBuilder builder = new ConfigurationBuilder();
                builder.setOAuthConsumerKey(Constants.TWITTER_CONSUMER_KEY);
                builder.setOAuthConsumerSecret(SECRETS.TWITTER_CONSUMER_SECRET);
                builder.setOAuthAccessToken(OAUTH_TOKEN);
                builder.setOAuthAccessTokenSecret(OAUTH_SECRET);
                Configuration configuration = builder.build();

                TwitterFactory factory = new TwitterFactory(configuration);

                Twitter twitter = factory.getInstance();
                /*
                ConfigurationBuilder builder2 = getOauthToken(act, twitter);
                twitter = new TwitterFactory(builder2.build()).getInstance();
                */
                try {
                    twitter4j.Status tweet = twitter.updateStatus(params[0]);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute(status);


    }

    public static void tweet(final Activity act, int serverObjectId){
        tweet(act, Share.generateShareText(act, OWServerObject.objects(act, OWServerObject.class).get(serverObjectId)));
    }

    public static void tweet(final Activity act, String status){
        new AsyncTask<String, Void, Void>(){

            @Override
            protected Void doInBackground(String... params) {
                TwitterFactory factory = new TwitterFactory(getTwitterConfiguration());
                Twitter twitter = factory.getInstance();

                try {
                    RequestToken requestToken = twitter.getOAuthRequestToken();
                    OAUTH_TOKEN = requestToken.getToken();
                    OAUTH_SECRET = requestToken.getTokenSecret();
                    lastLoginRequestToken = requestToken;
                    Intent i = new Intent(act.getApplicationContext(), WebViewActivity.class);
                    Log.i(TAG, requestToken.getAuthenticationURL());
                    i.putExtra(WebViewActivity.URL_INTENT_KEY, requestToken.getAuthenticationURL());
                    act.startActivityForResult(i, TWITTER_RESULT);
                    // Initiating Activity will handle calling updateStatus() after pin confirmation
                } catch (TwitterException e) {
                    e.printStackTrace();
                } catch(IllegalStateException ie){
                    if (!twitter.getAuthorization().isEnabled()) {
                        Log.e(TAG, "OAuth consumer key/secret not set!");
                    }else{
                        // AccessToken already valid.
                        updateStatus(act, params[0]);
                    }
                }
                return null;
            }
        }.execute(status);

    }


}
