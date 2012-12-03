package net.openwatch.reporter.recording;

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

	static {
    	System.loadLibrary("FFNewChunkedAudioVideoEncoder");
    }
	
	public String output_filename;
	
	public int initializeEncoder(String filename_hq, String filename_lq1, String filename_lq2, int width, int height, int fps){
		output_filename = filename_lq1;
		return internalInitializeEncoder(filename_hq, filename_lq1, filename_lq2, width, height, fps);
	}
		
	public native int internalInitializeEncoder(String filename_hq, String filename_lq1, String filename_lq2, int width, int height, int fps);
	public native void shiftEncoders(String new_filename);
	public native void processAVData(byte[] video_frame, long timestamp, short[] audio_data, int audio_length);
	public native void finalizeEncoder(int is_final); // 0: false, !0: true

}
