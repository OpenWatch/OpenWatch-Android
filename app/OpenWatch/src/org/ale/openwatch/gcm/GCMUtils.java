package org.ale.openwatch.gcm;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import org.ale.openwatch.SECRETS;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.http.OWServiceRequests;
import org.ale.openwatch.model.OWUser;

import java.io.IOException;
import java.sql.Timestamp;

/**
 * Created by davidbrodsky on 7/19/13.
 */
public class GCMUtils {
    private static final String TAG = "GCMUtils";
    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_ON_SERVER_EXPIRATION_TIME =
            "onServerExpirationTimeMs";
    static final String SENDER_ID = SECRETS.GCM_SENDER_ID;

    public static final long REGISTRATION_EXPIRY_TIME_MS = 1000 * 3600 * 24 * 7;

    public static GoogleCloudMessaging gcm;
    public static String regid;

    public static void setUpGCM(Activity act){
        Context context = act.getApplicationContext();
        GCMUtils.regid = GCMUtils.getRegistrationId(act);

        if (GCMUtils.regid.length() == 0) {
            GCMUtils.registerBackground(act);
        }else{
            SharedPreferences userPrefs = context.getSharedPreferences(Constants.PROFILE_PREFS, context.MODE_PRIVATE);
            int userId = userPrefs.getInt(Constants.INTERNAL_USER_ID, 0);
            if(userId > 0){
                OWUser user = OWUser.objects(context, OWUser.class).get(userId);
                user.gcm_registration_id.set(regid);
                user.save(context);
                OWServiceRequests.syncOWUser(context, user, null);
            }

        }
        GCMUtils.gcm = GoogleCloudMessaging.getInstance(context);
    }

    /**
     * Gets the current registration id for application on GCM service.
     * <p>
     * If result is empty, the registration has failed.
     *
     * @return registration id, or empty string if the registration is not
     *         complete.
     */
    private static String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.length() == 0) {
            Log.v(TAG, "Registration not found.");
            return "";
        }
        // check if app was updated; if so, it must clear registration id to
        // avoid a race condition if GCM sends a message
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion || isRegistrationExpired(context)) {
            Log.v(TAG, "App version changed or registration expired.");
            return "";
        }
        return registrationId;
    }

    /**
     * Checks if the registration has expired.
     *
     * <p>To avoid the scenario where the device sends the registration to the
     * server but the server loses it, the app developer may choose to re-register
     * after REGISTRATION_EXPIRY_TIME_MS.
     *
     * @return true if the registration has expired.
     */
    private static boolean isRegistrationExpired(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        // checks if the information is not stale
        long expirationTime =
                prefs.getLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, -1);
        return System.currentTimeMillis() > expirationTime;
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration id, app versionCode, and expiration time in the
     * application's shared preferences.
     */
    private static void registerBackground(final Activity act) {
        final Context context = act.getApplicationContext();
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration id=" + regid;

                    // You should send the registration ID to your server over HTTP,
                    // so it can use GCM/HTTP or CCS to send messages to your app.

                    // For this demo: we don't need to send it because the device
                    // will send upstream messages to a server that echo back the message
                    // using the 'from' address in the message.

                    // Save the regid - no need to register again.
                    setRegistrationId(act, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                // TODO: Show success
                //mDisplay.append(msg + "\n");
            }

        }.execute(null, null, null);
    }

    /**
     * Stores the registration id, app versionCode, and expiration time in the
     * application's {@code SharedPreferences}.
     *
     * @param act activity.
     * @param regId registration id
     */
    private static void setRegistrationId(Activity act, final String regId) {
        final Context context = act.getApplicationContext();
        SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        Log.v(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        long expirationTime = System.currentTimeMillis() + REGISTRATION_EXPIRY_TIME_MS;

        Log.v(TAG, "Setting registration expiry time to " +
                new Timestamp(expirationTime));
        editor.putLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, expirationTime);
        editor.commit();

        SharedPreferences userPrefs = context.getSharedPreferences(Constants.PROFILE_PREFS, context.MODE_PRIVATE);
        final int userId = userPrefs.getInt(Constants.INTERNAL_USER_ID, 0);
        if(userId > 0){
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    OWUser user = OWUser.objects(context, OWUser.class).get(userId);
                    user.gcm_registration_id.set(regId);
                    user.save(context);
                    OWServiceRequests.syncOWUser(context.getApplicationContext(), user, null);
                }
            });

        }
    }

    public static SharedPreferences getGCMPreferences(Context c){
        return c.getSharedPreferences(Constants.GCM_PREFS, c.MODE_PRIVATE);
    }
}
