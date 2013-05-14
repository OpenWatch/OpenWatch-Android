package net.openwatch.reporter;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.json.JSONArray;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.file.FileUtils;
import net.openwatch.reporter.http.OWMediaRequests;
import net.openwatch.reporter.location.DeviceLocation;
import net.openwatch.reporter.model.OWLocalVideoRecording;
import net.openwatch.reporter.model.OWServerObject;
import net.openwatch.reporter.model.OWVideoRecording;
import net.openwatch.reporter.recording.ChunkedAudioVideoSoftwareRecorder;
import net.openwatch.reporter.recording.FFChunkedAudioVideoEncoder.ChunkedRecorderListener;

import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;

public class RecorderActivity extends SherlockActivity implements
		SurfaceHolder.Callback {

	static final String TAG = "RecorderActivity";
	
	SurfaceView camera_preview;

	Camera mCamera;
	
	PowerManager.WakeLock wl;
	
	Context c;
	
	boolean ready_to_record = false;
	boolean is_recording = false;
	
	// VideoRecording data
	int media_object_id = 0;
	int owrecording_id = 0;
	String recording_uuid = "";
	
	String output_filename;
	String mRecording_uuid;
	String recording_start;
	String recording_end;
	
	/*
	 * ChunkedRecorderListener is set on FFChunkedAudioVideoEncoder 
	 */
	/*
	ChunkedRecorderListener chunk_listener = new ChunkedRecorderListener(){
		private static final String TAG = "ChunkedRecorderListener";
		Context c; // for db transactions
		String recording_uuid; // recording uuid for OW service
		int owrecording_id = -1; // database id for OWLocalRecording
		int owmediaobject_id = -1; // db id for OWMediaObject
		ArrayList<String> all_files = null;
		
		@Override
		public int getRecordingDBID(){
			return owrecording_id;
		}
		@Override
		public int getMediaObjectDBID(){
			return owmediaobject_id;
		}
		@Override
		public void setRecordingUUID(String recording_uuid) {
			this.recording_uuid = recording_uuid;
			
		}
				
		public void setContext(Context c){
			this.c = c.getApplicationContext();
		}
		
		@Override
		public void encoderShifted(final String finalized_file) {
			new MediaSignalTask().execute("chunk", finalized_file);
		}

		@Override
		public void encoderStarted(Date start_date) {
			new MediaSignalTask().execute("start", Constants.utc_formatter.format(start_date));
			
		}

		@Override
		public void encoderStopped(Date start_date, Date stop_date, ArrayList<String> all_files) {
			this.all_files = all_files;
			//Log.i(TAG,"start-date: " + Constants.sdf.format(start_date) + " stop-date: " + Constants.sdf.format(stop_date));
			//Log.i(TAG, "sending all_files: " + new JSONArray(all_files).toString());
			new MediaSignalTask().execute("end", Constants.utc_formatter.format(start_date), Constants.utc_formatter.format(stop_date), new JSONArray(all_files).toString());
			//new MediaSignalTask().execute("hq", hq_filename);
			Log.i(TAG, "fired end and hq mediaSignalTasks");
		}
		
		
	*/
	
	class MediaSignalTask extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... command) {
        	Log.i(TAG, "sendMediaCapture command: " + command[0] + " command length: " + command.length + " recording_uuid: " + recording_uuid);
        	SharedPreferences profile = getSharedPreferences(Constants.PROFILE_PREFS, MODE_PRIVATE);
            final String public_upload_token = profile.getString(Constants.PUB_TOKEN, "");
            if(public_upload_token.compareTo("") == 0 || recording_uuid == null)
            	return null;

        	if(command[0].compareTo("start") == 0){
        		if(command.length == 2){
        			// make db entry
        			if(recording_uuid != null){
	        			OWLocalVideoRecording local = new OWLocalVideoRecording(c);
	    	        	local.recording.get(c).initializeRecording(c, command[1], recording_uuid, 0.0, 0.0);
	    	        	Log.i(TAG, "initialize OWLocalRecording. id: " + String.valueOf(local.getId()));
	    	        	local.save(c);
	    	        	owrecording_id = local.recording.get(c).getId();
	    	        	media_object_id = local.recording.get(c).media_object.get(c).getId();
	    	        	Log.i(TAG, "get mediaObjectId: " + local.recording.get(c).media_object.get(c).server_id.get());
	    	        	// poll for device location
	    	        	RecorderActivity.this.runOnUiThread(new Runnable(){

							@Override
							public void run() {
								DeviceLocation.setRecordingLocation(c, public_upload_token, owrecording_id, true);
							}
	    	        		
	    	        	});
	    	        	
	        			OWMediaRequests.start(c, public_upload_token, recording_uuid, command[1]);
        			}
                }
        	} else if(command[0].compareTo("end") == 0){
        		if(command.length == 4){
    				OWVideoRecording recording = (OWVideoRecording) OWVideoRecording.objects(c, OWVideoRecording.class).get(owrecording_id);
        			OWLocalVideoRecording local = recording.local.get(c);
        			local.recording_end_time.set(command[2]);
        			local.save(c);
        			Log.i(TAG, "owlocalrecording addsegment");
        			// poll for device location
        			RecorderActivity.this.runOnUiThread(new Runnable(){

						@Override
						public void run() {
							DeviceLocation.setRecordingLocation(c, public_upload_token, owrecording_id, false);
						}
    	        		
    	        	});
        			//OWMediaRequests.safeSendLQFile(c, public_upload_token, recording_uuid, last_segment, segment_id);
        			OWMediaRequests.end(c, public_upload_token, recording);
        			
        		}
        	} else if(command[0].compareTo("hq") == 0){
        		if(command.length == 2){
        			OWVideoRecording recording = (OWVideoRecording) OWVideoRecording.objects(c, OWVideoRecording.class).get(owrecording_id);
        			OWLocalVideoRecording local = recording.local.get(c);
        			local.hq_filepath.set(command[1]);
        			local.save(c);
        			Log.i(TAG, "id: " + owrecording_id + " hq filepath set:" + command[1]);
        			OWMediaRequests.safeSendHQFile(c, public_upload_token, recording_uuid, command[1], local.getId());
        			//OWMediaRequests.sendHQFileChunked(public_upload_token, recording_uuid, command[1]);
        			//OWMediaRequests.sendHQFile(public_upload_token, recording_uuid, command[1]);
        		}
        	}
        	return null;
        }

        protected Void onPostExecute() {
        	return null;
        }
};
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		Log.i(TAG, "on Destroy. isFinishing: " + String.valueOf(this.isFinishing()));
		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_recorder);
		this.getSupportActionBar().setTitle(getString(R.string.recording));
		ready_to_record = false;
		Log.i(TAG,"onCreate");
		
		this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		camera_preview = (SurfaceView) findViewById(R.id.camera_surface_view);
		camera_preview.getHolder().addCallback(this); // register the Activity
														// to be called when the
														// SurfaceView is ready
		c = this;
		
		if(!is_recording){
			try {
				// Create an instance of Camera
				mCamera = getCameraInstance();
				prepareOutputLocation();
				ready_to_record = true;
			} catch (Exception e) {
				Log.e("Recorder init error", "Could not init Camera");
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.activity_recorder, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_stop:
			showStopRecordingDialog();
			break;
		}

		return true;
	}

	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
			Log.e(TAG, "Could not open camera");
		}
		return c; // returns null if camera is unavailable
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if(!is_recording && ready_to_record)
			startRecording();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}
	
	public void prepareOutputLocation(){
		File recording_dir = FileUtils.getStorageDirectory(
				FileUtils.getRootStorageDirectory(RecorderActivity.this,
						Constants.ROOT_OUTPUT_DIR),
				Constants.VIDEO_OUTPUT_DIR);
		
		mRecording_uuid = generateRecordingIdentifier();
		
		output_filename = FileUtils.getStorageDirectory(recording_dir, mRecording_uuid).getAbsolutePath();
		output_filename += "/";
	}
	
	
	public String generateRecordingIdentifier()
	{
		return UUID.randomUUID().toString();
	}
	
	/**
	 * Starts recording, noting the time in recording_start
	 */
	private void startRecording(){
		try {
			//av_recorder.startRecording(mCamera, camera_preview, output_filename);
			recording_start = String.valueOf(new Date().getTime() / 1000);
			
			//PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			//wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OWRecording");
			//wl.acquire();
			Log.d(TAG, "startRecording()");
		} catch (Exception e) {
			Log.e(TAG, "failed to start recording");
			e.printStackTrace();
		}
	}
	
	/**
	 * Stops recording, nothing the time in recording_end, and
	 * returns the focus to MainActivity, clearing the back stack
	 */
	private void stopRecording(){
		if (is_recording) {
			if(wl != null)
				wl.release();
			stopRecording();
			recording_end = String.valueOf(new Date().getTime() / 1000);
			Intent i = new Intent(RecorderActivity.this, WhatHappenedActivity.class);
			if(media_object_id > 0){
				i.putExtra(Constants.INTERNAL_DB_ID,media_object_id);
				Log.i(TAG, "Bundling media_obj_id: " + String.valueOf(media_object_id));
			}else
				Log.e(TAG, "Error getting mediaobject id from chunk_listener");

			i.putExtra(Constants.OW_REC_UUID, mRecording_uuid);
			i.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
			startActivity(i);
			finish(); // ensure this activity removed from the stack
		}
	}
	
	@Override
	public void onBackPressed() {
		showStopRecordingDialog();
	}
	
	private void showStopRecordingDialog(){
		if(is_recording){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.stop_recording_dialog_title))
			.setMessage(getString(R.string.stop_recording_dialog_message))
			.setPositiveButton(getString(R.string.stop_recording_dialog_stop), new DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					stopRecording();
					
				}
				
			}).setNegativeButton(getString(R.string.stop_recording_dialog_continue), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					
				}
			}).show();
		}
	}

}
