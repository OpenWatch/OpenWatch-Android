package org.ale.openwatch;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.ale.openwatch.account.Authentication;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.http.OWServiceRequests;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by davidbrodsky on 5/16/13.
 */
public class FancyLoginActivity extends SherlockActivity {
    private static final String TAG = "FancyLoginActivity";

    boolean image_2_visible = false;

    ImageView image_1;
    ImageView image_2;

    ScrollView scrollContainer;
    View activityRootView;

    Button mLoginButton;

    EditText mEmailView;
    EditText mPasswordView;

    View progressView;

    boolean password_field_visible = false;

    String mEmail;
    String mPassword;

    Timer timer;

    Animation zoom;

    public static boolean isVisible = false;

    long animation_clock = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        requestWindowFeature(com.actionbarsherlock.view.Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_fancy_login);

        image_1 = (ImageView) findViewById(R.id.image_1);
        image_2 = (ImageView) findViewById(R.id.image_2);
        mEmailView = (EditText) findViewById(R.id.field_email);
        mPasswordView = (EditText) findViewById(R.id.field_password);
        mLoginButton = (Button) findViewById(R.id.button_login);
        progressView = findViewById(R.id.button_login_progress);
        scrollContainer = (ScrollView) findViewById(R.id.scroll_container);
        scrollContainer.setSmoothScrollingEnabled(true);

        zoom = AnimationUtils.loadAnimation(this, R.anim.zoom);

        activityRootView = findViewById(R.id.container);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
                if (heightDiff > 100) { // if more than 100 pixels, its probably a keyboard...
                    scrollFullDown();
                }

            }
        });

        mEmailView
                .setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView textView, int id,
                                                  KeyEvent keyEvent) {
                        if (id == R.id.login || id == EditorInfo.IME_NULL) {
                            onLoginButtonClick(mLoginButton);
                            return true;
                        }
                        return false;
                    }
                });
        mPasswordView
                .setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView textView, int id,
                                                  KeyEvent keyEvent) {
                        if (id == R.id.login || id == EditorInfo.IME_NULL) {
                            onLoginButtonClick(mLoginButton);
                            return true;
                        }
                        return false;
                    }
                });

        Analytics.trackEvent(getApplicationContext(), Analytics.VIEWING_FANCY_LOGIN, null);
    }

    @SuppressLint("NewApi")
    @Override
    public void onResume(){
        super.onResume();
        isVisible = true;
        if(Build.VERSION.SDK_INT > 11){
            timer = new Timer();
            timer.scheduleAtFixedRate(new FadeTimerTask(), animation_clock, animation_clock);
            if(image_2_visible){
                image_2.animate()
                        .scaleX((float) 1.10)
                        .scaleY((float) 1.10)
                        .setDuration(animation_clock - 50).start();
            }else{
                image_1.animate()
                        .scaleX((float) 1.10)
                        .scaleY((float) 1.10)
                        .setDuration(animation_clock - 50).start();
            }
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        isVisible = false;
        if(Build.VERSION.SDK_INT > 11){
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @SuppressLint("NewApi")
    private void crossfade(){
        if(image_2_visible){
            //fade out image_2
            image_2.animate()
                    .alpha(0f)
                    .setDuration(1000)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            //fadeOut.setVisibility(View.GONE);
                            image_2.setScaleX((float)1.0);
                            image_2.setScaleY((float)1.0);
                            image_2_visible = false;
                        }
                    }).start();
            // Zoom image_1

            image_1.animate()
                    .scaleX((float) 1.10)
                    .scaleY((float) 1.10)
                    .setDuration(animation_clock - 50).start();

        } else{
            // fade in image_2
            image_2.animate()
                    .alpha(1f)
                    .setDuration(1000)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            image_2_visible = true;
                            image_1.setScaleX((float)1.0);
                            image_1.setScaleY((float)1.0);
                        }
                    }).start();

            image_2.animate()
                    .scaleX((float) 1.10)
                    .scaleY((float) 1.10)
                    .setDuration(animation_clock - 50).start();

        }
    }

    private class FadeTimerTask extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    crossfade();
                }
            });
        }
    }

    public void onForgotPasswordClick(View v){
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.OW_URL + Constants.PASSWORD_RESET_ENDPOINT));
        startActivity(browserIntent);
    }

    public void onLoginButtonClick(View v){
        showProgress(true);
        if(password_field_visible){
            attemptLogin();
        }else{
            checkEmailAvailableAndSignup();
        }
    }

    public void checkEmailAvailableAndSignup(){
        if(!validateInput())
            return;

        mEmailView.setError(null);
        mEmail = mEmailView.getText().toString().trim();
        showProgress(true);
        OWServiceRequests.checkOWEmailAvailable(getApplicationContext(), mEmail, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(JSONObject response) {
                Log.i(TAG, "checkOWEmailAvailable response " + response.toString());
                try {
                    if(response.has("available") && response.getBoolean("available")){
                        // Create a new account
                        quickUserSignup();
                    }else if(response.getBoolean("available") == false){
                        // Collect password and login account
                        showPasswordField();
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "checkEmailAvailableAndSignup failed to parse JSON: " + response);
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(Throwable e, String response) {
                Log.i(TAG,"checkEmailAvailableAndSignup failure: " + response);
                OWUtils.showConnectionErrorDialog(FancyLoginActivity.this);
            }

            @Override
            public void onFinish() {
                showProgress(false);
                Log.i(TAG,"checkEmailAvailableAndSignup finished");
            }

        });
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        boolean formValid = validateInput();
        if (formValid) {
            showProgress(true);
            UserLogin();
        }
        // if form is not valid, validateInput will present errors inline
    }

    /*
        Validate the mEmailView and mPasswordView values
        returns True if valid, False otherwise
        if False, view errors are populated
     */
    private boolean validateInput(){
        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        mEmail = mEmailView.getText().toString().trim();
        mPassword = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if(mPasswordView.getVisibility() == View.VISIBLE){
            if (TextUtils.isEmpty(mPassword)) {
                mPasswordView.setError(getString(R.string.error_field_required));
                focusView = mPasswordView;
                cancel = true;
            } else if (mPassword.length() < 4) {
                mPasswordView.setError(getString(R.string.error_invalid_password));
                focusView = mPasswordView;
                cancel = true;
            }
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(mEmail)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!OWUtils.checkEmail(mEmail)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
            showProgress(false);
            return false;
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            //mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
            //showProgress(true);
            //UserLogin();
            return true;
        }
    }

    /**
     * Login an existing account with the OpenWatch service assuming mEmail and
     * mPassword are pre-populated from the EditText fields
     */
    public void UserLogin() {
        Log.i("OWServiceRequests", "Commencing User Login from FLA");
        Analytics.trackEvent(getApplicationContext(), Analytics.LOGIN_ATTEMPT, null);
        JsonHttpResponseHandler response_handler = new JsonHttpResponseHandler() {
            private static final String TAG = "OWServiceRequests";

            @Override
            public void onStart() {
                Log.i(TAG, "onStart");
            }

            @Override
            public void onSuccess(JSONObject response) {
                Log.i("OWServiceRequests", "OW login success: " + response.toString());
                try {
                    Analytics.trackEvent(FancyLoginActivity.this.getApplicationContext(), Analytics.LOGIN_EXISTING, null);
                    Authentication.setUserAuthenticated(getApplicationContext(), response, mEmail);

                    if ((Boolean) response.getBoolean(Constants.OW_SUCCESS) == true) {
                        Log.i(TAG, "OW login success: " + response.toString());
                        navigateToOnBoardingActivity(true);
                        return;
                    } else {
                        AlertDialog.Builder dialog = new AlertDialog.Builder(
                                FancyLoginActivity.this);
                        int error_code = response.getInt(Constants.OW_ERROR);

                        switch (error_code) {

                            case 403: // No account with provided email
                                      // We won't login until email checked
                                break;
                            default: // Incorrect email address / password (Error
                                // 412)
                                dialog.setTitle(R.string.login_dialog_denied_title)
                                        .setMessage(
                                                R.string.login_dialog_denied_msg)
                                        .setNeutralButton(R.string.dialog_ok,
                                                defaultDialogOnClickListener)
                                        .show();
                                Analytics.identifyUser(FancyLoginActivity.this.getApplicationContext(), mEmail);
                                Analytics.trackEvent(FancyLoginActivity.this.getApplicationContext(), Analytics.LOGIN_FAILED, null);
                                break;
                        }
                        showProgress(false);

                    }
                } catch (JSONException e) {
                    Log.e(TAG, "error parsing json response");
                }

            }

            @Override
            public void onFailure(Throwable e, String response) {
                Log.i(TAG, "OW login failure: " + response);
                AlertDialog.Builder dialog = new AlertDialog.Builder(
                        FancyLoginActivity.this);
                dialog.setTitle(R.string.login_dialog_failed_title)
                        .setMessage(R.string.login_dialog_failed_msg)
                        .setNeutralButton(R.string.dialog_ok,
                                defaultDialogOnClickListener).show();
                showProgress(false);
            }

            @Override
            public void onFinish() {
            }
        };

        OWServiceRequests.userLogin(getApplicationContext(), getAuthJSON(),
                response_handler);

    }

    public DialogInterface.OnClickListener defaultDialogOnClickListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
        }

    };

    public void showProgress(final boolean show) {
        if(show){
            progressView.setVisibility(View.VISIBLE);
        }else{
            progressView.setVisibility(View.GONE);
        }
    }

    public StringEntity getAuthJSON() {
        if(mEmail == null || mPassword == null)
            return null;

        JSONObject json = new JSONObject();
        StringEntity se = null;
        try {
            json.put(Constants.OW_EMAIL, mEmail);
            json.put(Constants.OW_PW, mPassword);
            se = new StringEntity(json.toString());
        } catch (JSONException e) {
            Log.e(TAG, String.format("Error creating json from email %s password %s", mEmail, mPassword));
            e.printStackTrace();
        } catch (UnsupportedEncodingException e1) {
            Log.e(TAG, "Failed to put JSON string in StringEntity");
            e1.printStackTrace();
        }
        return se;
    }

    private void navigateToOnBoardingActivity(boolean didLogin) {
        Intent i = new Intent(FancyLoginActivity.this, OnBoardingActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // It's possible the sharedPreference setting won't be written by the
        // time MainActivity
        // checks its state, causing an erroneous redirect back to LoginActivity
        if (didLogin)
            i.putExtra(Constants.AUTHENTICATED, true);

        startActivity(i);
    }

    private void showPasswordField(){
        mPasswordView.setVisibility(View.VISIBLE);
        scrollFullDown();
        mPasswordView.requestFocus();
        mLoginButton.setText(getString(R.string.login_button_text));
        password_field_visible = true;
    }

    private void scrollFullDown(){
        //scrollContainer.fullScroll(View.FOCUS_DOWN);
        scrollContainer.smoothScrollBy(0, 250);
    }

    private void quickUserSignup(){
        final String email = mEmail;
        Analytics.trackEvent(getApplicationContext(), Analytics.LOGIN_ATTEMPT, null);
        OWServiceRequests.quickUserSignup(getApplicationContext(), mEmail, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(JSONObject response) {
                Log.i(TAG, "OW quicksignup success: " + response.toString());
                Analytics.trackEvent(FancyLoginActivity.this.getApplicationContext(), Analytics.LOGIN_NEW, null);
                Authentication.setUserAuthenticated(getApplicationContext(), response, email);
                navigateToOnBoardingActivity(true);
            }

            @Override
            public void onFailure(Throwable e, String response) {
                Log.i(TAG, "OW quicksignup failure: " + response);
                if(isVisible){
                    AlertDialog.Builder dialog = new AlertDialog.Builder(FancyLoginActivity.this);
                    dialog.setTitle(R.string.login_dialog_failed_title)
                            .setMessage(R.string.login_dialog_failed_msg)
                            .setNeutralButton(R.string.dialog_ok,
                                    defaultDialogOnClickListener).show();
                }


            }

            @Override
            public void onFinish() {
                showProgress(false);
            }
        });

    }


}