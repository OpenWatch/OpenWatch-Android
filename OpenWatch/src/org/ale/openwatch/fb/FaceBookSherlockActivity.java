package org.ale.openwatch.fb;

import android.content.Intent;
import android.os.Bundle;
import com.actionbarsherlock.app.SherlockActivity;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;

/**
 * Created by davidbrodsky on 7/10/13.
 * Helper Activity to manage Facebook Session state. Your implementation should override  onSessionStateChanged(boolean isOpened)
 * to update your UI accordingly.
 *
 * Requirements:
 *
 * You must add a string resource called fb_app_id with value equal to your Facebook application ID
 * and the following two elements to your AndroidManifest.xml:
 *   <meta-data android:name="com.facebook.sdk.ApplicationId" android:value="@string/fb_app_id"/>
 *   <activity android:name="com.facebook.LoginActivity"/>
 *
 */
public abstract class FaceBookSherlockActivity extends SherlockActivity {

    private UiLifecycleHelper uiHelper;
    private Session.StatusCallback callback =
            new Session.StatusCallback() {
                @Override
                public void call(Session session,
                                 SessionState state, Exception exception) {
                    onSessionStateChange(session, state, exception);
                }
            };

    private boolean isResumed = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        uiHelper.onResume();
        isResumed = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
        isResumed = false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        // Only make changes if the activity is visible
        if (isResumed) {
            if (state.isOpened()) {
                onSessionStateChanged(true);
            } else if (state.isClosed()) {
                onSessionStateChanged(false);
            }
        }
    }

    protected void onSessionStateChanged(boolean isOpened){

    }
}
