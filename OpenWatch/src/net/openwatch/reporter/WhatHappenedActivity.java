package net.openwatch.reporter;

import java.util.Date;

import org.json.JSONObject;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.loopj.android.http.JsonHttpResponseHandler;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.http.OWServiceRequests;
import net.openwatch.reporter.model.OWRecording;
import android.net.Uri;
import android.os.Bundle;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class WhatHappenedActivity extends SherlockFragmentActivity {
	private static final String TAG = "WhatHappenedActivity";
	static int model_id = -1;
	String recording_uuid;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_what_happened);
		Log.i(TAG, "onCreate");
		try{
			model_id = getIntent().getExtras().getInt(Constants.INTERNAL_DB_ID);
			recording_uuid = getIntent().getExtras().getString(Constants.OW_REC_UUID);
		}catch (Exception e){
			Log.e(TAG, "could not load recording_id from intent");
		}
		final Context app_context = this.getApplicationContext();
		OWServiceRequests.getRecordingMeta(app_context, recording_uuid, new JsonHttpResponseHandler(){
			private static final String TAG = "OWServiceRequests";
			@Override
    		public void onSuccess(JSONObject response){
				if(response.has("recording")){
					Log.i(TAG, "Got server recording response!");
					try{
						JSONObject recording_json = response.getJSONObject("recording");
						// response was successful
						OWRecording recording = OWRecording.objects(app_context, OWRecording.class).get(model_id);
						recording.updateWithJson(app_context, recording_json);
						Log.i(TAG, "recording updated with server meta response");
						return;
					} catch(Exception e){
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
		Log.i(TAG, "sent recordingMeta request");
	}
	
	public void submitButtonClick(View v){
		showCompleteDialog();
		//showShareDialog();
	}
	
	/**
	 * If a server_id was received, give option to share, else return to MainActivity
	 */
	private void showCompleteDialog(){
		final OWRecording recording = OWRecording.objects(this.getApplicationContext(), OWRecording.class).get(model_id);
		if(recording.server_id.get() == null || recording.server_id.get() == 0){
			Log.i(TAG, "recording does not have a valid server_id. Cannot present share dialog");
			Intent i = new Intent(WhatHappenedActivity.this, MainActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(i);
			return;
		}
		Log.i(TAG, "recording server_id: " + String.valueOf(recording.server_id.get()));
			
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.share_dialog_title))
		.setMessage(getString(R.string.share_dialog_message))
		.setPositiveButton(getString(R.string.share_dialog_no), new OnClickListener(){

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
				showShareDialog(recording);
			}
			
		}).show();
	}
	
	private void showShareDialog(OWRecording recording){
		String url = Constants.OW_URL +  Constants.OW_VIEW + recording.server_id.get();
		Log.i(TAG, "model_id: " + String.valueOf(model_id) + " url: " + url);
		
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("text/plain");
		i.putExtra(Intent.EXTRA_TEXT, url);
		startActivity(Intent.createChooser(i, getString(R.string.share_dialog_title)));
	}
	
	@Override
	public void onPause(){
		super.onPause();
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.activity_what_happened, menu);
		return true;
	}

}
