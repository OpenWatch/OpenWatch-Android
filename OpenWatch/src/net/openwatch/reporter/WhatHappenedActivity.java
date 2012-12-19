package net.openwatch.reporter;

import java.util.Date;

import org.json.JSONObject;

import com.loopj.android.http.JsonHttpResponseHandler;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.http.OWServiceRequests;
import net.openwatch.reporter.model.OWLocalRecording;
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
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class WhatHappenedActivity extends FragmentActivity {
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
			@Override
    		public void onSuccess(JSONObject response){
				Log.i(TAG, "Got server recording response!");
				if(response.has("recording")){
					try{
						JSONObject recording_json = response.getJSONObject("recording");
						// response was successful
						OWLocalRecording recording = OWLocalRecording.objects(app_context, OWLocalRecording.class).get(model_id);
						recording.updateWithJson(app_context, recording_json);
					} catch(Exception e){
						Log.e(TAG, "failed to handle response");
						e.printStackTrace();
					}
				}
					
			}
			
		});
		Log.i(TAG, "sent recordingMeta request");
	}
	
	public void submitButtonClick(View v){
		saveRecordingMeta();
		showCompleteDialog();
		//showShareDialog();
		
	}
	
	private void showCompleteDialog(){
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
				showShareDialog();
			}
			
		}).show();
	}
	
	private void showShareDialog(){
		OWLocalRecording recording = OWLocalRecording.objects(this.getApplicationContext(), OWLocalRecording.class).get(model_id);
		
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
		saveRecordingMeta();
	}
	
	private void saveRecordingMeta(){
		if(model_id == -1)
			return;
		
		String title = ((EditText)this.findViewById(R.id.editTitle)).getText().toString().trim();
		String description = ((EditText)this.findViewById(R.id.editDescription)).getText().toString().trim();
		
		Log.i(TAG, "Saving recording. ");
    	//this.getActivity().getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder)
    	OWLocalRecording recording = OWLocalRecording.objects(this.getApplicationContext(), OWLocalRecording.class).get(model_id);
    	if(title.compareTo("") != 0)
    		recording.title.set(title);
    	recording.description.set(description);
    	recording.save(this.getApplicationContext());
    	OWServiceRequests.editRecording(this.getApplicationContext(), recording, new JsonHttpResponseHandler(){
    		// TODO: What should happen if this request fails?
    	});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_what_happened, menu);
		return true;
	}

}
