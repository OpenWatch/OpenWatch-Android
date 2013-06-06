package org.ale.openwatch;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.file.FileUtils;
import org.ale.openwatch.http.OWMediaRequests;
import org.ale.openwatch.location.DeviceLocation;
import org.ale.openwatch.model.OWLocalVideoRecording;
import org.ale.openwatch.model.OWVideoRecording;
import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class RecorderActivity extends SherlockActivity implements
		SurfaceHolder.Callback {

	static final String TAG = "RecorderActivity";
	
	SurfaceView mCameraPreview;

	Camera mCamera;
	MediaRecorder mMediaRecorder;
	
	PowerManager.WakeLock wl;
	
	Context c;
	
	Date start_date;
	Date stop_date;
	
	boolean ready_to_record = false;
	boolean is_recording = false;
	
	// VideoRecording data
	int media_object_id = 0;
	int owrecording_id = 0;
	
	String output_filename;
	String recording_uuid;
	String recording_start;
	String recording_end;
	
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
                                DeviceLocation.setOWServerObjectLocation(c, media_object_id , true);
								//DeviceLocation.setRecordingLocation(c, public_upload_token, owrecording_id, true);
							}
	    	        		
	    	        	});
	    	        	
	        			OWMediaRequests.start(c, public_upload_token, recording_uuid, command[1]);
        			}else
        				Log.e(TAG, "recording_uuid is null on send start signal!");
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
                            DeviceLocation.setOWServerObjectLocation(c, media_object_id , false);
							//DeviceLocation.setRecordingLocation(c, public_upload_token, owrecording_id, false);
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
    }
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		Log.i(TAG, "on Destroy. isFinishing: " + String.valueOf(this.isFinishing()));
		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        requestWindowFeature(com.actionbarsherlock.view.Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_recorder);
		//this.getSupportActionBar().setTitle(getString(R.string.recording));
		ready_to_record = false;
		Log.i(TAG,"onCreate");
		
		this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mCameraPreview = (SurfaceView) findViewById(R.id.camera_surface_view);
		mCameraPreview.getHolder().addCallback(this); // register the Activity
        mCameraPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // req'd pre 3.0
		// to be called when the
		// SurfaceView is ready
        ImageView broadcast = (ImageView) findViewById(R.id.streaming_animation);
        final AnimationDrawable broadcastAnimation = ((AnimationDrawable) broadcast.getBackground());
        broadcast.post(new Runnable(){
            public void run(){
                broadcastAnimation.start();
            }
        });

		c = this;
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
            Camera.Parameters parameters = c.getParameters();
            List<String> focusModes = parameters.getSupportedFocusModes();
            if(Build.VERSION.SDK_INT >= 14)
                parameters.setRecordingHint(true);
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
            {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            c.setParameters(parameters);

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
		if(!is_recording)
			startRecording();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}
	
	public boolean prepareOutputLocation() throws IOException{
		File recording_dir = FileUtils.getStorageDirectory(
				FileUtils.getRootStorageDirectory(RecorderActivity.this,
						Constants.ROOT_OUTPUT_DIR),
				Constants.VIDEO_OUTPUT_DIR);
		
		recording_uuid = generateRecordingIdentifier();
		
		output_filename = FileUtils.getStorageDirectory(recording_dir, recording_uuid).getAbsolutePath();
		output_filename += "/hq";
		File output_file = new File(output_filename);
		if(output_file.mkdirs())
			output_filename += "/hq.mp4";
		return output_file.createNewFile();
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
			//PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			//wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OWRecording");
			//wl.acquire();
			if (prepareVideoRecorder()) {
				start_date = new Date();
				recording_start = String.valueOf(start_date.getTime() / 1000);
				new MediaSignalTask().execute("start", "");
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                mMediaRecorder.start();

                // inform the user that recording has started
                is_recording = true;
            } else {
            	Log.e(TAG, "failed to start recording");
                // prepare didn't work, release the camera
                releaseMediaRecorder();
                // inform user
            }
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
			recording_end = String.valueOf(new Date().getTime() / 1000);
			stop_date = new Date();
			new MediaSignalTask().execute("end", Constants.utc_formatter.format(start_date), Constants.utc_formatter.format(stop_date), new JSONArray(new ArrayList()).toString());
			new MediaSignalTask().execute("hq", output_filename);
			mMediaRecorder.stop();
			releaseMediaRecorder();
			mCamera.lock();
			Intent i = new Intent(RecorderActivity.this, WhatHappenedActivity.class);
			if(media_object_id > 0){
				i.putExtra(Constants.INTERNAL_DB_ID,media_object_id);
				Log.i(TAG, "Bundling media_obj_id: " + String.valueOf(media_object_id));
			}else
				Log.e(TAG, "Error getting mediaobject id from chunk_listener");
            i.putExtra("hq_filepath", output_filename);
			i.putExtra(Constants.OW_REC_UUID, recording_uuid);
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
	private boolean h263Fallback = false;

	private boolean prepareVideoRecorder(){
		try {
			prepareOutputLocation();
		} catch (IOException e1) {
			Log.e(TAG, "failed to create output file");
			e1.printStackTrace();
		}
	    mCamera = getCameraInstance();
	    mMediaRecorder = new MediaRecorder();

	    // Step 1: Unlock and set camera to MediaRecorder
	    mCamera.unlock();
	    mMediaRecorder.setCamera(mCamera);

	    // Step 2: Set sources
	    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
	    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

	    // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        if(Build.VERSION.SDK_INT >= 11)
	        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));
        else{
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            if(h263Fallback)
                mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
            else
                mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
        }

	    // Step 4: Set output file
	    mMediaRecorder.setOutputFile(output_filename);

	    // Step 5: Set the preview output
	    mMediaRecorder.setPreviewDisplay(mCameraPreview.getHolder().getSurface());

	    // Step 6: Prepare configured MediaRecorder
	    try {
	        mMediaRecorder.prepare();
	    } catch (IllegalStateException e) {
	        Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            // If we fail to initialize MPEG-4-SP pre API 11, try h263
            if(Build.VERSION.SDK_INT >= 11 && !h263Fallback){
                h263Fallback = true;
                releaseMediaRecorder();
                prepareVideoRecorder();
            }else{
                releaseMediaRecorder();
                return false;
            }
	    } catch (IOException e) {
	        Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
	        releaseMediaRecorder();
	        return false;
	    }
	    return true;
	}
	
	private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }
	
	private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }
	
	@Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
    }

    public void onStopRecordingButtonClick(View v){
        showStopRecordingDialog();
    }

}
