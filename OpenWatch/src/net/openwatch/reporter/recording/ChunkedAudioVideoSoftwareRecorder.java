package net.openwatch.reporter.recording;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;

import net.openwatch.reporter.recording.FFChunkedAudioVideoEncoder.ChunkedRecorderListener;
import net.openwatch.reporter.recording.audio.AudioSoftwarePoller;
import net.openwatch.reporter.recording.FFChunkedAudioVideoEncoder;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceView;

public class ChunkedAudioVideoSoftwareRecorder {

	private static final String TAG = "ChunkedVideoSoftwareRecorder";
	public static Camera camera;

	public static boolean is_recording = false;
	public static boolean got_first_video_frame = false;

	private static FFChunkedAudioVideoEncoder ffencoder = new FFChunkedAudioVideoEncoder();
	private ExecutorService encoding_service = Executors.newSingleThreadExecutor(); // re-use encoding_service
	private EncoderTask encoding_task;

	private final String file_ext = ".mp4";
	private final int output_width = 320;
	private final int output_height = 240;
	private final int fps = 24;

	private int chunk_frame_count = 0; // frames recorded in current chunk
	private int chunk_frame_max = fps * 5; // chunk every this many frames

	private Date video_frame_date;
	private Date start_time; // time when first video frame received
	private Date end_time; // time when stopRecording called
	int frame_count = 0;
	int audio_data_length = 0;

	private AudioSoftwarePoller audio_poller;
	private static short[] audio_samples;

	private void initializeRecorder() {
		frame_count = 0;
		audio_data_length = 0;
		audio_samples = null;
		chunk_frame_count = 0;
		got_first_video_frame = false;
	}
	
	public void setChunkedRecorderListener(ChunkedRecorderListener listener){
		if(ffencoder == null)
			ffencoder = new FFChunkedAudioVideoEncoder();
		ffencoder.setRecorderListener(listener);
	}


	@SuppressLint("NewApi")
	public void startRecording(Camera camera, SurfaceView camera_surface_view,
			String output_filename_base) throws Exception {

		// Debug.startMethodTracing("AV_Profiling");

		// Ready FFEncoder
		if(ffencoder == null)
			ffencoder = new FFChunkedAudioVideoEncoder();
		// Ready this class's recording parameters
		initializeRecorder();

		int num_samples = ffencoder.initializeEncoder(output_filename_base, output_width, output_height, fps);
		
		// Attach AudioSoftwarePoller to FFEncoder. Calls
		// ffencoder.encodeAudioFrame(...)
		audio_poller = new AudioSoftwarePoller();
		audio_poller.recorderTask.samples_per_frame = num_samples;
		Log.i("AUDIO_FRAME_SIZE",
				"audio frame size: " + String.valueOf(num_samples));
		audio_poller.startPolling();

		this.camera = camera;

		Camera.Parameters camera_parameters = camera.getParameters();
		camera_parameters.setPreviewFormat(ImageFormat.NV21);

		// camera_parameters.setPreviewSize(output_width, output_height);

		if (Build.VERSION.SDK_INT >= 9)
			camera_parameters = setCameraPreviewMinFPS(camera_parameters, fps);

		camera_parameters = setCameraPreviewSize(camera_parameters,
				output_width, output_height);
		camera.setParameters(camera_parameters);

		try {
			camera.setPreviewDisplay(camera_surface_view.getHolder());
		} catch (IOException e) {
			Log.e(TAG, "setPreviewDisplay IOE");
			e.printStackTrace();
		}

		Size previewSize = camera.getParameters().getPreviewSize();
		// int dataBufferSize = (int) (previewSize.height * previewSize.width *
		// (ImageFormat
		// .getBitsPerPixel(camera.getParameters().getPreviewFormat()) / 8.0));

		camera.setPreviewCallback(new Camera.PreviewCallback() {

			@Override
			public void onPreviewFrame(byte[] video_frame_data, Camera camera) {
				//Log.d("FRAME", "video frame polled");

				if (!got_first_video_frame) {
					// Ensure a video frame worth of audio is prepared
					// before accepting first frame
					if (audio_poller.recorderTask.buffer_write_index < (1.0 / fps)
							* audio_poller.SAMPLE_RATE) {
						// Log.i("FRAME","Audio not ready. write index: " +
						// String.valueOf(audio_poller.recorderTask.buffer_write_index)
						// + " required: " + String.valueOf((1.0/fps) *
						// audio_poller.SAMPLE_RATE));
						return;
					}

					got_first_video_frame = true;
					start_time = new Date();
					
				} else {
					audio_samples = audio_poller.emptyBuffer();
					audio_data_length = audio_poller.read_distance;
				}

				frame_count++;
				video_frame_date = new Date();
				
				encoding_task = new EncoderTask(ffencoder, video_frame_data, audio_samples);
				encoding_service.submit(encoding_task);
				/*
				ffencoder.processAVData(video_frame_data,
						video_frame_date.getTime(), audio_samples,
						audio_data_length);*/
				// Log.d("PROCESSAVDATA-1","DONE");
				chunk_frame_count++;
				if (chunk_frame_count >= chunk_frame_max) {
					Log.d("FRAME", "chunking video");
					chunk_frame_count = 0;
					swapEncoders();
				}
			}
		});

		camera.startPreview();
		is_recording = true;
	}

	private void swapEncoders() {
		encoding_task = new EncoderTask(ffencoder, EncoderTaskType.SHIFT_ENCODER);
		encoding_service.submit(encoding_task);		
	}

	public void stopRecording() {
		camera.stopPreview();
		camera.setPreviewCallback(null);
		audio_poller.stopPolling();
		encoding_task = new EncoderTask(ffencoder);
		encoding_service.submit(encoding_task);
		camera.release();
		camera = null;
		end_time = new Date();
		encoding_service.shutdown();
		
		is_recording = false;
		Log.i("AUDIO_STATS",
				"Written: "
						+ String.valueOf(audio_poller.recorderTask.total_frames_written)
						+ " Read: "
						+ String.valueOf(audio_poller.recorderTask.total_frames_read));
		
		double elapsed_time = (double) (end_time.getTime() - start_time.getTime()) / 1000;
		int expected_num_frames = (int) (elapsed_time * fps);
		double frame_success = 100 * ((double) frame_count)
				/ expected_num_frames;
		Log.i("MORE_STATS",
				"Time: " + String.valueOf(elapsed_time) + " V-Frames: "
						+ String.valueOf(frame_count) + "/"
						+ String.valueOf(expected_num_frames) + " ("
						+ String.valueOf(frame_success) + "%)");
		// Debug.stopMethodTracing();
	}

	public static String getFilePath(String output_filename) {
		File output_file = new File(output_filename);
		if (!output_file.exists()) {
			try {
				output_file.createNewFile();
			} catch (IOException e) {
				Log.e(TAG, "New File IOE");
				e.printStackTrace();
			}
		}
		return output_file.getAbsolutePath();

	}

	@SuppressLint("NewApi")
	public Camera.Parameters setCameraPreviewMinFPS(
			Camera.Parameters parameters, int fps) {
		List<int[]> fpsRanges = parameters.getSupportedPreviewFpsRange();
		int scaled_fps = fps * 1000;
		int desired_fps_index = -1;
		for (int x = 0; x < fpsRanges.size(); x++) {
			if (fpsRanges.get(x)[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] >= scaled_fps) {
				desired_fps_index = x;
				break;
			}
		}
		if (desired_fps_index == -1) {
			Log.e("setCameraPreviewMinFPS", "Couldn't find desired fps: "
					+ String.valueOf(fps));
		} else {
			int min_fps = fpsRanges.get(desired_fps_index)[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
			int max_fps = fpsRanges.get(desired_fps_index)[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
			parameters.setPreviewFpsRange(min_fps, max_fps);
			Log.i("setCameraPreviewMinFPS",
					"Set fps: " + String.valueOf(min_fps) + " - "
							+ String.valueOf(max_fps));
		}

		return parameters;

	}

	public Camera.Parameters setCameraPreviewSize(Camera.Parameters parameters,
			int desired_width, int desired_height) throws Exception {
		List<Camera.Size> preview_sizes = parameters.getSupportedPreviewSizes();
		int desired_preview_size_index = -1;
		for (int x = 0; x < preview_sizes.size(); x++) {
			if (preview_sizes.get(x).width == desired_width
					&& preview_sizes.get(x).height == desired_height)
				desired_preview_size_index = x;
		}
		if (desired_preview_size_index == -1) {
			Log.e("setCameraPreviewSize", "Could not find desired preview size");
			throw new Exception("Couldnt find desired preview size");
		} else {
			Log.i("setCameraPreviewSize", String.valueOf(desired_width) + " x "
					+ String.valueOf(desired_height));
			parameters.setPreviewSize(desired_width, desired_height);
		}
		return parameters;
	}
	
	// Interface describing callback listener
	
	
	enum EncoderTaskType{
		ENCODE_FRAME, SHIFT_ENCODER, FINALIZE_ENCODER;
	}
	
	private class EncoderTask implements Runnable{
		private static final String TAG = "encoderTask";
		
		private FFChunkedAudioVideoEncoder encoder;
		
		private EncoderTaskType type;
		boolean is_initialized = false;
		
		// vars for type ENCODE_FRAME
		private byte[] video_data;
		private short[] audio_data;
				
		public EncoderTask(FFChunkedAudioVideoEncoder encoder, EncoderTaskType type){
			setEncoder(encoder);
			
			switch(type){
			case SHIFT_ENCODER:
				setShiftEncoderParams();
				break;
			case FINALIZE_ENCODER:
				setFinalizeEncoderParams();
				break;
			}
			
		}
		
		public EncoderTask(FFChunkedAudioVideoEncoder encoder, byte[] video_data, short[] audio_data){
			setEncoder(encoder);
			setEncodeFrameParams(video_data, audio_data);
		}
		
		public EncoderTask(FFChunkedAudioVideoEncoder encoder){
			setEncoder(encoder);
			setFinalizeEncoderParams();
		}
		
		private void setEncoder(FFChunkedAudioVideoEncoder encoder){
			this.encoder = encoder;
		}
		
		private void setFinalizeEncoderParams(){
			this.type = EncoderTaskType.FINALIZE_ENCODER;
			is_initialized = true;
		}
		
		private void setEncodeFrameParams(byte[] video_data, short[] audio_data){
			this.video_data = video_data;
			this.audio_data = audio_data;
			
			this.type = EncoderTaskType.ENCODE_FRAME;
			is_initialized = true;
		}
		
		private void setShiftEncoderParams(){			
			this.type = EncoderTaskType.SHIFT_ENCODER;
			is_initialized = true;
		}
		
		private void encodeFrame(){
			encoder.encodeFrame(video_data, audio_data);
		}
		
		private void shiftEncoder(){
			encoder.chunkFile();
			Log.i(TAG, "chunkFile()");
		}
		
		private void finalizeEncoder(){
			encoder.finalizeEncoder();
			
		}

		@Override
		public void run() {
			if(is_initialized){
				Log.i(TAG, "run encoderTask type: " + String.valueOf(type));
				switch(type){
				case ENCODE_FRAME:
					encodeFrame();
					Log.i(TAG, "encodeFrame-1");
					break;
				case SHIFT_ENCODER:
					Log.i(TAG, "shiftEncoder()-0");
					shiftEncoder();
					Log.i(TAG, "shiftEncoder()-1");
					break;
				case FINALIZE_ENCODER:
					Log.i(TAG, "finalizeEncoder()-0");
					finalizeEncoder();
					Log.i(TAG, "finalizeEncoder()-1");
					break;
				}
				// prevent multiple execution of same task
				is_initialized = false;
			}
			else{
				Log.e(TAG, "run() called but EncoderTask not initialized");
			}
				
		}
		
	}


}
