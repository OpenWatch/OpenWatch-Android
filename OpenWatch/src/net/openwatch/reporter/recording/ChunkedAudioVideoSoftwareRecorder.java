package net.openwatch.reporter.recording;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

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

	private static FFChunkedAudioVideoEncoder ffencoder;

	private String output_filename_base = "";

	private final String file_ext = ".mp4";
	private final int output_width = 320;
	private final int output_height = 240;
	private final int fps = 24;

	private int chunk = 1; // video chunks in this recording

	private int chunk_frame_count = 0; // frames recorded in current chunk
	private int chunk_frame_max = fps * 5; // chunk every this many frames

	private Date video_frame_date;
	private long start_time;
	private long end_time;
	int frame_count = 0;
	int audio_data_length = 0;

	private AudioSoftwarePoller audio_poller;
	private static short[] audio_samples;

	private void initializeRecorder() {
		chunk = 1;
		frame_count = 0;
		audio_data_length = 0;
		audio_samples = null;
		chunk_frame_count = 0;
		got_first_video_frame = false;

	}

	@SuppressLint("NewApi")
	public void startRecording(Camera camera, SurfaceView camera_surface_view,
			String output_filename_base) throws Exception {

		// Debug.startMethodTracing("AV_Profiling");

		// Ready FFEncoder
		ffencoder = new FFChunkedAudioVideoEncoder();
		// Ready this class's recording parameters
		initializeRecorder();

		// num_samples is the # of audio samples / frame
		int num_samples = ffencoder.initializeEncoder(output_filename_base
				+ "_HQ" + file_ext,
				output_filename_base + "_" + String.valueOf(chunk) + file_ext,
				output_filename_base + "_" + String.valueOf(chunk + 1)
						+ file_ext, output_width, output_height, fps);

		chunk += 2;

		// Attach AudioSoftwarePoller to FFEncoder. Calls
		// ffencoder.encodeAudioFrame(...)
		audio_poller = new AudioSoftwarePoller();
		audio_poller.recorderTask.samples_per_frame = num_samples;
		Log.i("AUDIO_FRAME_SIZE",
				"audio frame size: " + String.valueOf(num_samples));
		audio_poller.startPolling();

		this.camera = camera;
		this.output_filename_base = output_filename_base;

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
				Log.d("FRAME", "video frame polled");

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
					start_time = new Date().getTime();
				} else {
					audio_samples = audio_poller.emptyBuffer();
					audio_data_length = audio_poller.read_distance;
				}

				frame_count++;
				video_frame_date = new Date();

				ffencoder.processAVData(video_frame_data,
						video_frame_date.getTime(), audio_samples,
						audio_data_length);
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
		int current_encoder_int = chunk % 2 + 1; // returns 1 or 2

		ffencoder.shiftEncoders(getFilePath(output_filename_base + "_"
				+ String.valueOf(chunk) + file_ext));

		chunk++;
	}

	public void stopRecording() {
		camera.stopPreview();
		camera.setPreviewCallback(null);
		audio_poller.stopPolling();
		ffencoder.finalizeEncoder(1);
		camera.release();
		camera = null;
		is_recording = false;
		Log.i("AUDIO_STATS",
				"Written: "
						+ String.valueOf(audio_poller.recorderTask.total_frames_written)
						+ " Read: "
						+ String.valueOf(audio_poller.recorderTask.total_frames_read));
		end_time = new Date().getTime();
		double elapsed_time = (double) (end_time - start_time) / 1000;
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

}
