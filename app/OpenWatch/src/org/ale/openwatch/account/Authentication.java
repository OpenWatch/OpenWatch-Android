package org.ale.openwatch.account;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import com.loopj.android.http.JsonHttpResponseHandler;

import com.orm.androrm.Filter;
import com.orm.androrm.QuerySet;
import org.ale.openwatch.Analytics;
import org.ale.openwatch.OWApplication;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.constants.DBConstants;
import org.ale.openwatch.http.HttpClient;
import org.ale.openwatch.http.OWServiceRequests;
import org.ale.openwatch.model.OWUser;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Sauce for interpreting
 * ow account api responses
 * found in http.OWServiceRequests
 * and manipulating stored user state
 * in SharedPreferences and httpclient cookiestore
 * Created by davidbrodsky on 5/16/13.
 */
public class Authentication {
    private static final String TAG = "Authentication";

    public interface AuthenticationCallback{
        public void onComplete();
    }

    /**
     * Save the OpenWatch service login response data to SharedPreferences this
     * includes the public and private upload token, authentication state, and
     * the submitted email.
     *
     * @author davidbrodsky
     *
     */
    public static void setUserAuthenticated(Context c, JSONObject server_response, String expected_email) {
        Analytics.identifyUser(c, expected_email);
        new SetAuthedTask(c, expected_email).execute(server_response);
    }

    private static class SetAuthedTask extends AsyncTask<JSONObject, Void, Void> {
        Context c;
        String expectedEmail;

        public SetAuthedTask(Context c, String expectedEmail) {
            this.c = c;
            this.expectedEmail = expectedEmail;
        }

        protected Void doInBackground(JSONObject... server_response_array) {
            SharedPreferences profile = c.getSharedPreferences(
                    Constants.PROFILE_PREFS, c.MODE_PRIVATE);
            JSONObject server_response = server_response_array[0];

            try {

                if (server_response.getBoolean(Constants.OW_SUCCESS)
                        && !profile.getBoolean(Constants.REGISTERED, false)) {
                    try {
                        RegisterApp(c, server_response.getString(Constants.PUB_TOKEN));
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing pub token from JSON");
                        e.printStackTrace();
                    }
                }

                // Confirm returned email matches
                SharedPreferences.Editor editor = profile.edit();
                if (server_response.getBoolean(Constants.OW_SUCCESS)) {
                    if ((server_response.getString(Constants.OW_EMAIL))
                            .compareTo(expectedEmail) != 0)
                        Log.e(TAG,
                                "Email mismatch. Client submitted "
                                        + expectedEmail
                                        + " Server responded: "
                                        + ((String) server_response
                                        .get(Constants.OW_EMAIL)));
                    editor.putBoolean(Constants.AUTHENTICATED, true);
                    editor.putInt(DBConstants.USER_SERVER_ID,
                            server_response.getInt(DBConstants.USER_SERVER_ID));
                    editor.putString(Constants.PUB_TOKEN,
                            server_response.getString(Constants.PUB_TOKEN));
                    editor.putString(Constants.PRIV_TOKEN,
                            server_response.getString(Constants.PRIV_TOKEN));
                    Log.i(TAG,
                            "Got upload tokens. Pub: "
                                    + server_response
                                    .getString(Constants.PUB_TOKEN)
                                    + " Priv: "
                                    + server_response
                                    .getString(Constants.PRIV_TOKEN));
                    if (c != null) {
                        OWUser myself = null;
                        Filter userFilter = new Filter();
                        userFilter.is(DBConstants.USER_SERVER_ID, server_response.getInt(DBConstants.USER_SERVER_ID));
                        QuerySet<OWUser> potential_users = OWUser.objects(c, OWUser.class).filter(userFilter);
                        for(OWUser user : potential_users){
                            myself = user;
                            break;
                        }
                        if(myself == null)
                            myself = new OWUser();
                        myself.updateWithJson(c, server_response);
                        editor.putInt(Constants.INTERNAL_USER_ID, myself.getId());
                    }
                } else {
                    Log.i(TAG, "Set user not authenticated");
                    editor.putBoolean(Constants.AUTHENTICATED, false);
                }
                editor.putString(Constants.EMAIL, expectedEmail); // save email even if login unsuccessful
                editor.commit();
                Log.i(TAG, "committetd sharedPrefs");
                // TODO: stop using user_data
                OWApplication.user_data = c.getSharedPreferences(Constants.PROFILE_PREFS, c.MODE_PRIVATE).getAll();
            } catch (JSONException e) {
                Log.e(TAG,
                        "SavePreferenceTask: Error reading JSONObject response: "
                                + server_response.toString());
                e.printStackTrace();
            }
            return null;
        }

        protected Void onPostExecute() {
            return null;
        }
    }

    /**
     * Registers this mobile app with the OpenWatch service sends the
     * application version number
     */
    public static void RegisterApp(final Context c, String public_upload_token) {
        // Post public_upload_token, signup_type
        JsonHttpResponseHandler response_handler = new JsonHttpResponseHandler() {
            private static final String TAG = "OWServiceRequests";

            @Override
            public void onSuccess(JSONObject response) {
                Log.i(TAG, "OW app register success: " + response);

                try {
                    if (response.getBoolean(Constants.OW_SUCCESS) == true) {
                        Log.i(TAG,
                                "OW app registration success: "
                                        + response.toString());

                        new setRegisteredTask(c).execute();
                        return;
                    } else {
                        int error_code = response.getInt(Constants.OW_ERROR);
                        switch (error_code) {

                            case 415: // invalid public upload token
                                Log.e(TAG,
                                        "invalid public upload token on app registration");
                                break;
                            default:
                                Log.e(TAG, "Other error on app registration: "
                                        + response.getString(Constants.OW_REASON));
                                break;
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing json registration response");
                }
            }

            @Override
            public void onFailure(Throwable e, String response) {
                Log.i(TAG, "OW app registration failure: " + response);
            }

            @Override
            public void onFinish() {
                Log.i(TAG, "OW app registration finish");
            }
        };

        OWServiceRequests.RegisterApp(c,
                public_upload_token, response_handler);
    }

    /**
     * Set the SharedPreferences to reflect app registration complete
     *
     * @author davidbrodsky
     *
     */
    private static class setRegisteredTask extends AsyncTask<Void, Void, Void> {
        Context c;

        public setRegisteredTask(Context c){
            this.c = c;
        }
        protected Void doInBackground(Void... server_response_array) {
            SharedPreferences profile = c.getSharedPreferences(
                    Constants.PROFILE_PREFS, c.MODE_PRIVATE);
            SharedPreferences.Editor editor = profile.edit();
            editor.putBoolean(Constants.REGISTERED, true);
            editor.commit();
            return null;
        }

        protected Void onPostExecute() {
            return null;
        }
    }

    public static void logOut(Context c){
        SharedPreferences profile = c.getSharedPreferences(Constants.PROFILE_PREFS, c.MODE_PRIVATE);
        profile.edit().clear().commit();
        HttpClient.clearCookieStore(c);
    }



}
