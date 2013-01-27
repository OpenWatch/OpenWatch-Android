package net.openwatch.reporter;

import org.json.JSONObject;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.loopj.android.http.JsonHttpResponseHandler;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.contentprovider.OWContentProvider;
import net.openwatch.reporter.http.OWServiceRequests;
import net.openwatch.reporter.model.OWMediaObject;
import net.openwatch.reporter.model.OWVideoRecording;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.util.Log;

public class WhatHappenedActivity extends SherlockFragmentActivity {
	private static final String TAG = "WhatHappenedActivity";
	static int model_id = -1;
	int recording_server_id = -1;
	String recording_uuid;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_what_happened);
		this.getSupportActionBar().setTitle(getString(R.string.what_happened));
		Log.i(TAG, "onCreate");
		try{
			model_id = getIntent().getExtras().getInt(Constants.INTERNAL_DB_ID);
			recording_uuid = getIntent().getExtras().getString(Constants.OW_REC_UUID);
		}catch (Exception e){
			Log.e(TAG, "could not load recording_id from intent");
		}
		fetchRecordingFromOW();
		
		Log.i(TAG, "sent recordingMeta request");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_submit:
			showCompleteDialog();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void fetchRecordingFromOW(){
		final Context app_context = this.getApplicationContext();
		OWServiceRequests.getRecordingMeta(app_context, recording_uuid, new JsonHttpResponseHandler(){
			private static final String TAG = "OWServiceRequests";
			@Override
    		public void onSuccess(JSONObject response){
				Log.i(TAG, "getRecording response: " + response.toString());
				if(response.has("recording")){
					Log.i(TAG, "Got server recording response!");
					try{
						JSONObject recording_json = response.getJSONObject("recording");
						// response was successful
						OWVideoRecording recording = OWMediaObject.objects(app_context, OWMediaObject.class).get(model_id).video_recording.get(app_context);
						recording.updateWithJson(app_context, recording_json);
						if(recording_json.has(Constants.OW_SERVER_ID) )
							recording_server_id = recording_json.getInt(Constants.OW_SERVER_ID);
						Log.i(TAG, "recording updated with server meta response");
						return;
					} catch(Exception e){
						Log.e(TAG, "Error processing getRecording response");
						e.printStackTrace();
					}
				}
				Log.i(TAG, "Failed to handle server recording response!");
					
			}
			
			@Override
			public void onFailure(Throwable e, String response){
				Log.i(TAG, "get recording meta failed: " + response);
			}
			
			@Override
			public void onFinish(){
				Log.i(TAG, "get recording meta finish");
			}
			
		});
	}

	
	/**
	 * If a server_id was received, give option to share, else return to MainActivity
	 */
	private void showCompleteDialog(){
		if(model_id == -1){
			Log.e(TAG, "model_id not set. aborting showCompleteDialog");
			return;
		}
		//final OWVideoRecording recording = OWMediaObject.objects(getApplicationContext(), OWMediaObject.class).get(model_id).video_recording.get(getApplicationContext());
		if( recording_server_id == -1){
			Log.i(TAG, "recording does not have a valid server_id. Cannot present share dialog");
			Intent i = new Intent(WhatHappenedActivity.this, MainActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(i);
			this.finish();
			return;
		}
		Log.i(TAG, "recording server_id: " + String.valueOf(recording_server_id));
			
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.share_dialog_title))
		.setMessage(getString(R.string.share_dialog_message))
		.setPositiveButton(getString(R.string.share_dialog_no), new OnClickListener(){

			@SuppressLint("NewApi")
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// No thanks
				Intent i = new Intent(WhatHappenedActivity.this, MainActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
				startActivity(i);
				dialog.dismiss();
			}
			
		}).setNegativeButton(getString(R.string.share_dialog_title), new OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Share
				dialog.dismiss();
				showShareDialog(recording_server_id);
			}
			
		}).show();
	}
	
	private void showShareDialog(int recording_server_id){
		String url = Constants.OW_URL +  Constants.OW_RECORDING_VIEW + String.valueOf(recording_server_id);
		Log.i(TAG, "model_id: " + String.valueOf(model_id) + " url: " + url);
		
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("text/plain");
		i.putExtra(Intent.EXTRA_TEXT, url);
		i.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
		WhatHappenedActivity.this.finish();
		startActivity(Intent.createChooser(i, getString(R.string.share_dialog_title)));
	}
	
	@Override
	public void onPause(){
		SharedPreferences profile = getSharedPreferences(Constants.PROFILE_PREFS, 0);
		int user_id = profile.getInt(DBConstants.USER_SERVER_ID, 0);
		if(user_id > 0)
			getContentResolver().notifyChange(OWContentProvider.getUserRecordingsUri(user_id), null);
		super.onPause();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.activity_what_happened, menu);
		return true;
	}

}
