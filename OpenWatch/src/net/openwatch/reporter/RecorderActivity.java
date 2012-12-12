package net.openwatch.reporter;

import java.io.File;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import org.json.JSONArray;

import com.orm.androrm.DatabaseAdapter;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.file.FileUtils;
import net.openwatch.reporter.http.MediaServerRequests;
import net.openwatch.reporter.model.OWLocalRecording;
import net.openwatch.reporter.recording.ChunkedAudioVideoSoftwareRecorder;
import net.openwatch.reporter.recording.FFChunkedAudioVideoEncoder.ChunkedRecorderListener;

import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;

public class RecorderActivity extends Activity implements
		SurfaceHolder.Callback {

	private static final String TAG = "RecorderActivity";
	
	private SurfaceView camera_preview;

	private Camera mCamera;

	private ChunkedAudioVideoSoftwareRecorder av_recorder = new ChunkedAudioVideoSoftwareRecorder();
	
	private boolean ready_to_record = false;
	
	String output_filename;
	String recording_id;
	String recording_start;
	String recording_end;
	
	/*
	 * ChunkedRecorderListener is set on FFChunkedAudioVideoEncoder 
	 */
	ChunkedRecorderListener chunk_listener = new ChunkedRecorderListener(){
		Context c; // for db transactions
		String recording_id; // recording uuid for OW service
		int owlocalrecording_id = -1; // database id for OWLocalRecording
		ArrayList<String> all_files = null;
		
		@Override
		public void setRecordingID(String recording_id) {
			this.recording_id = recording_id;
			
		}
				
		public void setContext(Context c){
			this.c = c;
		}
		
		@Override
		public void encoderShifted(final String finalized_file) {
			setupSDF();
			new MediaSignalTask().execute("chunk", finalized_file);
		}

		@Override
		public void encoderStarted(Date start_date) {
			setupSDF();
			new MediaSignalTask().execute("start", Constants.sdf.format(start_date));
			
		}

		@Override
		public void encoderStopped(Date start_date, Date stop_date,
				String hq_filename, ArrayList<String> all_files) {
			setupSDF();
			this.all_files = all_files;
			Log.i(TAG,"start-date: " + Constants.sdf.format(start_date) + " stop-date: " + Constants.sdf.format(stop_date));
			new MediaSignalTask().execute("end", Constants.sdf.format(start_date), Constants.sdf.format(stop_date), new JSONArray(all_files).toString());
			new MediaSignalTask().execute("hq", hq_filename);
		}
		
		private void setupSDF(){
			if(!Constants.sdf.getTimeZone().equals(TimeZone.getTimeZone("UTC")))
					Constants.sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		}
		
		class MediaSignalTask extends AsyncTask<String, Void, Void> {
	        protected Void doInBackground(String... command) {
	        	Log.i(TAG, "sendMediaCapture command: " + command[0] + " command length: " + command.length);
	        	SharedPreferences profile = getSharedPreferences(Constants.PROFILE_PREFS, MODE_PRIVATE);
	            String public_upload_token = profile.getString(Constants.PUB_TOKEN, "");
	            if(public_upload_token.compareTo("") == 0 || recording_id == null)
	            	return null;

	        	if(command[0].compareTo("start") == 0){
	        		if(command.length == 2){
	        			// make db entry
	        			OWLocalRecording recording = new OWLocalRecording();
	    	        	recording.initializeRecording(c, recording_id, 0, 0);
	    	        	// make network request
	    	        	owlocalrecording_id = recording.getId();
	                	MediaServerRequests.start(public_upload_token, recording_id, command[1]);
	                }
	        	} else if(command[0].compareTo("end") == 0){
	        		if(command.length == 4){
	        			if(all_files != null){
	        				OWLocalRecording recording = (OWLocalRecording) OWLocalRecording.objects(c, OWLocalRecording.class).get(owlocalrecording_id);
		        			String last_segment = all_files.get(all_files.size()-1);
		        			String filename = last_segment.substring(last_segment.lastIndexOf(File.separator),last_segment.length());
		        			String filepath = last_segment.substring(0,last_segment.lastIndexOf(File.separator));
		        			recording.addSegment(c, filepath, filename);
	        			}
	        			MediaServerRequests.end(public_upload_token, recording_id, command[1], command[2], command[3]);
	        		}
	        	} else if(command[0].compareTo("chunk") == 0){
	        		if(command.length == 2){
	        			if(owlocalrecording_id != -1){
		        			OWLocalRecording recording = (OWLocalRecording) OWLocalRecording.objects(c, OWLocalRecording.class).get(owlocalrecording_id);
		        			String filename = command[1].substring(command[1].lastIndexOf(File.separator),command[1].length());
		        			String filepath = command[1].substring(0,command[1].lastIndexOf(File.separator));
		        			recording.addSegment(c, filepath, filename);
	        			}
	        			MediaServerRequests.sendLQChunk(public_upload_token, recording_id, command[1]);
	        		}
	        	} else if(command[0].compareTo("hq") == 0){
	        		if(command.length == 2)
	        			MediaServerRequests.sendHQFile(public_upload_token, recording_id, command[1]);
	        	}
	        	return null;
	        }

	        protected Void onPostExecute() {
	        	return null;
	        }
	    }
	};
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		Log.i(TAG, "on Destroy. isFinishing: " + String.valueOf(this.isFinishing()));
		if(!this.isFinishing() && av_recorder.is_recording){
			stopRecording(); // if the activity is being closed to save memory, finalize the recording
		}
		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_recorder);
		ready_to_record = false;
		Log.i(TAG,"onCreate");

		camera_preview = (SurfaceView) findViewById(R.id.camera_surface_view);
		camera_preview.getHolder().addCallback(this); // register the Activity
														// to be called when the
														// SurfaceView is ready

		camera_preview.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (av_recorder.is_recording) {
					av_recorder.stopRecording();
					Intent i = new Intent(RecorderActivity.this, MainActivity.class);
					i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
					startActivity(i);
				}
			}

		}); // end onClickListener
		
		chunk_listener.setContext(this);
		av_recorder.setChunkedRecorderListener(chunk_listener);
		
		if(!av_recorder.is_recording){
			try {
				// Create an instance of Camera
				mCamera = getCameraInstance();
				prepareOutputLocation();
			} catch (Exception e) {
				Log.e("Recorder init error", "Could not init Camera");
				e.printStackTrace();
			}
		}
		ready_to_record = true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_recorder, menu);
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
		if(!av_recorder.is_recording && ready_to_record)
			startRecording();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}
	
	public void prepareOutputLocation(){
		File recording_dir = FileUtils.getStorageDirectory(
				FileUtils.getRootStorageDirectory(RecorderActivity.this,
						Constants.ROOT_OUTPUT_DIR),
				Constants.RECORDING_OUTPUT_DIR);
		
		recording_id = generateRecordingIdentifier();
		
		output_filename = FileUtils.getStorageDirectory(recording_dir, recording_id).getAbsolutePath();
		output_filename += "/" + String.valueOf(new Date().getTime());
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
			av_recorder.startRecording(mCamera, camera_preview, output_filename);
			recording_start = String.valueOf(new Date().getTime() / 1000);
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
		av_recorder.stopRecording();
		recording_end = String.valueOf(new Date().getTime() / 1000);
		Intent i = new Intent(RecorderActivity.this, MainActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(i);	
	}
	
	
	

}
