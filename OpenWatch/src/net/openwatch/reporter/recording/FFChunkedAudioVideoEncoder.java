package net.openwatch.reporter.recording;

import java.util.ArrayList;
import java.util.Date;

import android.util.Log;

/**
 * JNI Wrapper around the OpenWatch FFMPEG encoding library
 * Flow:
 * 1. initializeEncoder(..)
 * 2. encodeFrame()
 * 2a (optional): shiftEncoder to chunk lq recording
 * 3. finalizeEncoder()
 * 
 * @author David Brodsky
 *
 */
public class FFChunkedAudioVideoEncoder {
	private static final String TAG = "FFChunkedAudioVideoEncoder";

	static {
    	System.loadLibrary("FFNewChunkedAudioVideoEncoder");
    }
	
	// Constants
	private final String HQ_SUFFIX = "_HQ";
	private final String FILE_EXT = ".mp4";
	
	// Recording-wide variables
	private ChunkedRecorderListener listener;
	public String output_filename_base;
	public String current_filename;
	public String buffered_filename;
	public String output_filename_hq;
	public Date start; // first video frame received
	public Date end; // time when finalizeENcoder called
	public ArrayList<String> all_files; // finalized files
	private boolean got_first_frame;
	
	int chunk; // current chunk
			   // buffered_filename = output_filename_base + chunk + FILE_EXT
	
	Date video_frame_date;
	
	public interface ChunkedRecorderListener{
		public void encoderStarted(Date start_date);
		public void encoderShifted(String finalized_file);
		public void encoderStopped(Date start_date, Date stop_date, String hq_filename, ArrayList<String> all_files);
	}
	
	private synchronized String getChunkFilename(int count){
		return output_filename_base + "_" + String.valueOf(count) + FILE_EXT;
	}
	
	public synchronized void setRecorderListener(ChunkedRecorderListener listener){
		this.listener = listener;
	}
	
	public synchronized int initializeEncoder(String filename_base, int width, int height, int fps){
		chunk = 1;
		all_files = new ArrayList<String>();
		got_first_frame = false;
		output_filename_base = filename_base;
		output_filename_hq = output_filename_base + HQ_SUFFIX + FILE_EXT;
		current_filename = getChunkFilename(chunk);
		buffered_filename = getChunkFilename(chunk+1);
		int num_samples = internalInitializeEncoder(output_filename_hq, current_filename, buffered_filename, width, height, fps);
		
		Log.i(TAG,"initializeEncoder");
		return num_samples;
	}

	public synchronized void chunkFile(){
		Log.i(TAG, "pre shiftEncoders");
		chunk ++;
		all_files.add(current_filename);
		current_filename = buffered_filename;
		buffered_filename = getChunkFilename(chunk+1);
		shiftEncoders(buffered_filename);
		
		Log.i(TAG, "post shiftEncoders");
		if(listener != null){
			Log.i(TAG, "calling listener.encoderShifted");
			listener.encoderShifted(all_files.get(all_files.size()-1));
		} else
			Log.e(TAG, "listener is null on chunkFile");
	}
	
	public synchronized void finalizeEncoder(){
		all_files.add(current_filename);
		Log.i(TAG, "pre finalizeEncoder()");
		finalizeEncoder(1);
		end = new Date();
		
		Log.i(TAG, "post finalizeEncoder");
		if(listener != null){
			Log.i(TAG, "calling listener.encoderStopped");
			listener.encoderStopped(start, end, output_filename_hq, all_files);
		} else
			Log.e(TAG, "listener is null on finalize");
		
	}
	
	public synchronized void encodeFrame(byte[] video_frame, short[] audio_frames){
		processAVData(video_frame, new Date().getTime(), audio_frames, audio_frames.length);
		if(!got_first_frame){
			got_first_frame = true;
			start = new Date();
			if(listener != null){
				Log.i(TAG, "calling listener.encoderStarted");
				listener.encoderStarted(start);
			} else
				Log.e(TAG, "listener is null on encodeFrame");
		}
	}
	
	private native int internalInitializeEncoder(String filename_hq, String filename_lq1, String filename_lq2, int width, int height, int fps);
	private native void shiftEncoders(String new_filename);
	private native void processAVData(byte[] video_frame, long timestamp, short[] audio_data, int audio_length);
	private native void finalizeEncoder(int is_final); // 0: false, !0: true

}
