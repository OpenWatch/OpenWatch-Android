package net.openwatch.reporter;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import net.openwatch.reporter.account.Authentication;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.gson.Gson;
import com.loopj.android.http.*;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.file.FileUtils;
import net.openwatch.reporter.http.OWServiceRequests;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ImageView;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends SherlockActivity {

	private static final String TAG = "LoginActivity";
	private static final int SELECT_PHOTO = 100;
	private static final int TAKE_PHOTO = 101;

	// Values for mEmailView and password at the time of the login attempt.
	private String mEmail;
	private String mPassword;

	// UI references.
	private EditText mEmailView;
	private EditText mPasswordView;
	private View mLoginFormView;
	private View mLoginStatusView;
	private TextView mLoginStatusMessageView;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.activity_login);
		this.getSupportActionBar().setTitle(getString(R.string.sign_in));
		// Set up the login form.
		mEmail = getIntent().getStringExtra(Constants.EMAIL);
		mEmailView = (EditText) findViewById(R.id.email);
		mEmailView.setText(mEmail);

		mPasswordView = (EditText) findViewById(R.id.password);
		mPasswordView
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView textView, int id,
							KeyEvent keyEvent) {
						if (id == R.id.login || id == EditorInfo.IME_NULL) {
							attemptLogin();
							return true;
						}
						return false;
					}
				});

		mLoginFormView = findViewById(R.id.login_form);
		mLoginStatusView = findViewById(R.id.login_status);
		mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

		findViewById(R.id.sign_in_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						attemptLogin();
					}
				});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getSupportMenuInflater().inflate(R.menu.activity_login, menu);
		return true;
	}

	@Override
	public void onResume() {
		super.onResume();
		SharedPreferences profile = getSharedPreferences(
				Constants.PROFILE_PREFS, MODE_PRIVATE);
		if (profile.contains(Constants.EMAIL)) {
			mEmailView.setText(profile.getString(Constants.EMAIL, ""));
			if (profile.getBoolean(Constants.AUTHENTICATED, false)){
				setViewsAsAuthenticated();
				return;
			}
		}
		setViewsAsNotAuthenticated();
		
		// If the user has never provided an email:
		if(!profile.contains(Constants.EMAIL) && !profile.getBoolean(Constants.AUTHENTICATED, false)){
			((TextView) findViewById(R.id.login_state_message)).setText(getString(R.string.create_account_prompt));
			this.findViewById(R.id.login_state_message).setVisibility(View.VISIBLE);
		}
		
	}
	
	public void setUserAvatar(View v){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.take_choose_picture_title))
		.setMessage(getString(R.string.take_choose_picture_prompt))
		.setPositiveButton(getString(R.string.take_picture), new OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				startActivityForResult(takePicture, TAKE_PHOTO);
			}
			
		}).setNegativeButton(getString(R.string.choose_picture), new OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
				photoPickerIntent.setType("image/*");
				startActivityForResult(photoPickerIntent, SELECT_PHOTO);   
			}
			
		}).show();
		 
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) { 
	    super.onActivityResult(requestCode, resultCode, imageReturnedIntent); 
	    
	    switch(requestCode) { 
	    case SELECT_PHOTO:
	        if(resultCode == RESULT_OK){  
	        	Uri selectedImage = imageReturnedIntent.getData();
	            InputStream imageStream;
	    		try {
	    			Bitmap yourSelectedImage = FileUtils.decodeUri(getApplicationContext(), selectedImage, 100);
	    			((ImageView)this.findViewById(R.id.user_thumbnail)).setImageBitmap(yourSelectedImage);
	    			//TODO: Send thumbnail to server
	    		} catch (FileNotFoundException e) {
	    			e.printStackTrace();
	    		}
	            break;
	        }
	    case TAKE_PHOTO:
	    	if(resultCode == RESULT_OK){
	    		((ImageView)this.findViewById(R.id.user_thumbnail)).setImageBitmap((Bitmap)imageReturnedIntent.getExtras().get("data"));
	            break;
	    	}
	    }
	   
	}

	private void setViewsAsAuthenticated() {
		((TextView) this.findViewById(R.id.login_state_message)).setText(getString(R.string.message_account_stored));
		this.findViewById(R.id.login_state_message).setVisibility(View.VISIBLE);
		this.findViewById(R.id.sign_in_button).setVisibility(View.GONE);
		mEmailView.setEnabled(false);
		mPasswordView.setVisibility(View.GONE);
	}

	private void setViewsAsNotAuthenticated() {
		this.findViewById(R.id.login_state_message).setVisibility(View.GONE);
		this.findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
		mEmailView.setEnabled(true);
		mPasswordView.setVisibility(View.VISIBLE);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		SharedPreferences profile = getSharedPreferences(
				Constants.PROFILE_PREFS, MODE_PRIVATE);
		if (profile.getBoolean(Constants.AUTHENTICATED, false))
			return true;
		else
			return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_logout:
			logOut();
			item.setVisible(false);
			break;
		}

		return true;
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {
		// Reset errors.
		mEmailView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		mEmail = mEmailView.getText().toString().trim();
		mPassword = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password.
		if (TextUtils.isEmpty(mPassword)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
			cancel = true;
		} else if (mPassword.length() < 4) {
			mPasswordView.setError(getString(R.string.error_invalid_password));
			focusView = mPasswordView;
			cancel = true;
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
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
			showProgress(true);
			UserLogin();
		}
	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			mLoginStatusView.setVisibility(View.VISIBLE);
			mLoginStatusView.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginStatusView.setVisibility(show ? View.VISIBLE
									: View.GONE);
						}
					});

			mLoginFormView.setVisibility(View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mLoginFormView.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

	/**
	 * Login an existing account with the OpenWatch service assuming mEmail and
	 * mPassword are pre-populated from the EditText fields
	 */
	public void UserLogin() {
		JsonHttpResponseHandler response_handler = new JsonHttpResponseHandler() {
			private static final String TAG = "OWServiceRequests";

			@Override
			public void onStart() {
				Log.i(TAG, "onStart");
			}

			@Override
			public void onSuccess(JSONObject response) {
				Log.i(TAG, "OW login success: " + response.toString());
				try {
					Authentication.setUserAuthenticated(getApplicationContext(), response, mEmail);

					if ((Boolean) response.getBoolean(Constants.OW_SUCCESS) == true) {
						Log.i(TAG, "OW login success: " + response.toString());

						returnToMainActivity(true);
						return;
					} else {
						AlertDialog.Builder dialog = new AlertDialog.Builder(
								LoginActivity.this);
						int error_code = response.getInt(Constants.OW_ERROR);

						switch (error_code) {

						case 403: // No account with provided email
							dialog.setTitle(
									R.string.login_dialog_unknown_email_title)
									.setMessage(
											getString(R.string.login_dialog_unknown_email_msg)
													+ " " + mEmail + "?")
									.setNegativeButton(
											R.string.login_dialog_signup,
											signupDialogOnClickListener)
									.setPositiveButton(
											R.string.login_dialog_no,
											defaultDialogOnClickListener)
									.show();
							break;
						default: // Incorrect email address / password (Error
									// 412)
							dialog.setTitle(R.string.login_dialog_denied_title)
									.setMessage(
											R.string.login_dialog_denied_msg)
									.setNeutralButton(R.string.login_dialog_ok,
											defaultDialogOnClickListener)
									.show();
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
						LoginActivity.this);
				dialog.setTitle(R.string.login_dialog_failed_title)
						.setMessage(R.string.login_dialog_failed_msg)
						.setNeutralButton(R.string.login_dialog_ok,
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

	/**
	 * Create a new account with the OpenWatch service assuming mEmail and
	 * mPassword are pre-populated from the EditText fields
	 */
	public void UserSignup() {
		JsonHttpResponseHandler response_handler = new JsonHttpResponseHandler() {
			private static final String TAG = "OWServiceRequests";

			@Override
			public void onSuccess(JSONObject response) {
				Log.i(TAG, "OW signup success: " + response);
				try {
					if (response.getBoolean(Constants.OW_SUCCESS) == true) {
						Log.i(TAG, "OW signup success: " + response.toString());
						// Set authed preference
						Authentication.setUserAuthenticated(getApplicationContext(), response, mEmail);

						toWelcomeActivity();
						return;
					} else {
						AlertDialog.Builder dialog = new AlertDialog.Builder(
								LoginActivity.this);
						int error_code = response.getInt(Constants.OW_ERROR);
						switch (error_code) {

						case 405: // email address in use
							dialog.setTitle(
									R.string.signup_dialog_email_taken_title)
									.setMessage(
											mEmail
													+ " "
													+ getString(R.string.signup_dialog_email_taken_msg));
							break;
						default:
							dialog.setTitle(R.string.signup_dialog_failed_title)
									.setMessage(
											R.string.signup_dialog_failed_msg);
							break;
						}
						dialog.setNeutralButton(R.string.login_dialog_ok,
								defaultDialogOnClickListener);
						dialog.show();
					}
				} catch (JSONException e) {
					Log.e(TAG, "Error parsing signup JSON");
				}

			}

			@Override
			public void onFailure(Throwable e, String response) {
				Log.i(TAG, "OW signup failure: " + response);
				AlertDialog.Builder dialog = new AlertDialog.Builder(
						LoginActivity.this);
				dialog.setTitle(R.string.login_dialog_failed_title)
						.setMessage(R.string.login_dialog_failed_msg)
						.setNeutralButton(R.string.login_dialog_ok,
								defaultDialogOnClickListener).show();
			}

			@Override
			public void onFinish() {
			}
		};

		OWServiceRequests.userSignup(getApplicationContext(), getAuthJSON(),
                response_handler);

	}



	private void logOut() {
        Authentication.logOut(getApplicationContext());
        this.setViewsAsNotAuthenticated();
	}

	public StringEntity getAuthJSON() {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(Constants.OW_EMAIL, mEmail);
		params.put(Constants.OW_PW, mPassword);
		Gson gson = new Gson();
		StringEntity se = null;
		try {
			se = new StringEntity(gson.toJson(params));
		} catch (UnsupportedEncodingException e1) {
			Log.e(TAG, "Failed to put JSON string in StringEntity");
			e1.printStackTrace();
		}
		return se;
	}

	public OnClickListener defaultDialogOnClickListener = new OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			dialog.dismiss();
		}

	};

	public OnClickListener signupDialogOnClickListener = new OnClickListener() {

		@Override
		public void onClick(DialogInterface dialog, int which) {
			showProgress(true);
			UserSignup();
		}

	};

	@SuppressLint("NewApi")
	private void returnToMainActivity(boolean didLogin) {
		Intent i = new Intent(LoginActivity.this, MainActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TASK);
		// It's possible the sharedPreference setting won't be written by the
		// time MainActivity
		// checks its state, causing an erroneous redirect back to LoginActivity
		if (didLogin)
			i.putExtra(Constants.AUTHENTICATED, true);

		startActivity(i);
	}
	
	@SuppressLint("NewApi")
	private void toWelcomeActivity(){
		Intent i = new Intent(LoginActivity.this, WelcomeActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(i);
	}

}
