package net.openwatch.reporter;

import java.io.File;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.file.FileUtils;
import net.openwatch.reporter.http.Uploader;
import net.openwatch.reporter.recording.ChunkedAudioVideoSoftwareRecorder;
import net.openwatch.reporter.recording.ChunkedAudioVideoSoftwareRecorder.ChunkedRecorderListener;

import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
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
	
	String output_filename;
	String recording_id;
	String recording_start;
	String recording_end;
	
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
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if(!av_recorder.is_recording)
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
		SecureRandom random = new SecureRandom();
	    return new BigInteger(130, random).toString(32);
	}
	
	/**
	 * Starts recording, noting the time in recording_start
	 */
	private void startRecording(){
		try {
			av_recorder.startRecording(mCamera, camera_preview, output_filename);
			av_recorder.setChunkListener(chunk_listener);
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
	
	/**
	 * Sends a signal to the OW MediaCapture service
	 * 
	 * When calling execute pass a String argument to indicate preference
	 * Options:
	 * "start" Sends start signal
	 * "end" Sends end signal
	 * 
	 * The following two options require a String[] argument:
	 * Pass a String[] with the [0] index equal to one of the following commands
	 * and the [1] index containing an absolute filepath:
	 * "chunk" Sends video chunk signal
	 * "hq" Sends HQ video signal
	 * @author davidbrodsky
	 *
	 */
	public class sendMediaCaptureSignal extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... command) {
        	Log.i(TAG, "sendMediaCapture command: " + command[0] + " command length: " + command.length);
        	SharedPreferences profile = getSharedPreferences(Constants.PROFILE_PREFS, MODE_PRIVATE);
            String public_upload_token = profile.getString(Constants.PUB_TOKEN, "");
            if(public_upload_token.compareTo("") == 0 || recording_id == null)
            	return null;

        	if(command[0].compareTo("start") == 0){
        		if(command.length == 2){
                	Uploader.sendStartSignal(public_upload_token, recording_id, command[1]);
                }
        	} else if(command[0].compareTo("end") == 0){
        		if(command.length == 4){
        			Uploader.sendEndSignal(public_upload_token, recording_id, command[1], command[2], command[3]);
        		}
        	} else if(command[0].compareTo("chunk") == 0){
        		if(command.length == 2)
        			Uploader.sendVideoChunk(public_upload_token, recording_id, command[1]);
        	} else if(command[0].compareTo("hq") == 0){
        		if(command.length == 2)
        			Uploader.sendHQVideo(public_upload_token, recording_id, command[1]);
        	}
        	return null;
        }

        protected Void onPostExecute() {
        	return null;
        }
    }
	
	/*
	 * ChunkedRecorderListener is set on ChunkedAudioVideoEncoder 
	 * to be called back when the recorder finalizes an lq chunk
	 * signaling it is ready for transmission
	 */
	ChunkedRecorderListener chunk_listener = new ChunkedRecorderListener(){

		@Override
		public void encoderShifted(final String finalized_file) {
			new sendMediaCaptureSignal().execute("chunk", finalized_file);
		}

		@Override
		public void encoderStarted(Date start_date) {
			new sendMediaCaptureSignal().execute("start", String.valueOf(start_date.getTime() / 1000));
			
		}

		@Override
		public void encoderStopped(Date start_date, Date stop_date,
				String hq_filename, ArrayList<String> all_files) {
			new sendMediaCaptureSignal().execute("end", String.valueOf(start_date.getTime() / 1000), String.valueOf(stop_date.getTime() / 1000), new JSONArray(all_files).toString());
			new sendMediaCaptureSignal().execute("hq", hq_filename);
			
		}
		
	};

}
