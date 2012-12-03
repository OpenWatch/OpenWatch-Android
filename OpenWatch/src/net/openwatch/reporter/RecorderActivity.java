package net.openwatch.reporter;

import java.io.File;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Date;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.file.FileUtils;
import net.openwatch.reporter.recording.ChunkedAudioVideoSoftwareRecorder;

import android.hardware.Camera;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_recorder);

		camera_preview = (SurfaceView) findViewById(R.id.camera_surface_view);
		camera_preview.getHolder().addCallback(this); // register the Activity
														// to be called when the
														// SurfaceView is ready

		camera_preview.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				/*
				 * File output_dir_file = new File(OUTPUT_DIR); if
				 * (!output_dir_file.exists()) output_dir_file.mkdir();
				 * output_filename = OUTPUT_DIR + String.valueOf(new
				 * Date().getTime()); FFVideoEncoder.testFFMPEG(output_filename
				 * + "H264.mp4");
				 * //VideoSoftwareRecorder.startRecording((SurfaceView)
				 * MainActivity.this.findViewById(R.id.camera_surface_view), new
				 * File(output_filename));
				 */

				if (av_recorder.is_recording) {
					av_recorder.stopRecording();
					Intent i = new Intent(RecorderActivity.this,
							MainActivity.class);
					startActivity(i);
					/*
					 * action_bar.setBackgroundDrawable(new
					 * ColorDrawable(Color.WHITE));
					 * action_bar.setDisplayShowTitleEnabled(false);
					 * action_bar.setDisplayShowTitleEnabled(true);
					 * action_bar.setTitle(""); Log.i("ACTION_BAR","WHITE");
					 */
				}
			}

		}); // end onClickListener

		prepareOutputLocation();
		
		try {
			// Create an instance of Camera
			mCamera = getCameraInstance();

			/*
			 * action_bar.setBackgroundDrawable(new ColorDrawable(Color.RED));
			 * action_bar.setDisplayShowTitleEnabled(false);
			 * action_bar.setDisplayShowTitleEnabled(true);
			 * action_bar.setTitle("RECORDING"); Log.i("ACTION_BAR","RED");
			 */
		} catch (Exception e) {
			Log.e("Recorder init error", "Could not init av_recorder");
			e.printStackTrace();
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
		try {
			av_recorder.startRecording(mCamera, camera_preview, output_filename);
		} catch (Exception e) {
			Log.e(TAG, "failed to start recording");
			e.printStackTrace();
		}

		Log.d(TAG, "startRecording()");

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub

	}
	
	public void prepareOutputLocation(){
		File recording_dir = FileUtils.getStorageDirectory(
				FileUtils.getRootStorageDirectory(RecorderActivity.this,
						Constants.ROOT_OUTPUT_DIR),
				Constants.RECORDING_OUTPUT_DIR);
		
		String recording_id = generateRecordingIdentifier();
		
		output_filename = FileUtils.getStorageDirectory(recording_dir, recording_id).getAbsolutePath();
		output_filename += "/" + String.valueOf(new Date().getTime());
	}
	
	
	public String generateRecordingIdentifier()
	{
		SecureRandom random = new SecureRandom();
	    return new BigInteger(130, random).toString(32);
	}

}
