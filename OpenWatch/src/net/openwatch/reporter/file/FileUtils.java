package net.openwatch.reporter.file;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

public class FileUtils {
	
	private static final String TAG = "FileUtils";
		
	/**
	 * Returns a Java File initialized to a directory of given name 
	 * at the root storage location, with preference to external storage. 
	 * If the directory did not exist, it will be created at the conclusion of this call.
	 * If a file with conflicting name exists, this method returns null;
	 * 
	 * @param c the context to determine the internal storage location, if external is unavailable
	 * @param directory_name the name of the directory desired at the storage location
	 * @return a File pointing to the storage directory, or null if a file with conflicting name 
	 * exists
	 */
	public static File getRootStorageDirectory(Context c, String directory_name){
		File result;
		// First, try getting access to the sdcard partition
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			Log.d(TAG,"Using sdcard");
			result = new File(Environment.getExternalStorageDirectory(), directory_name);
		} else {
		// Else, use the internal storage directory for this application
			Log.d(TAG,"Using internal storage");
			result = new File(c.getApplicationContext().getFilesDir(), directory_name);
		}
		
		if(!result.exists())
			result.mkdir();
		else if(result.isFile()){
			return null;
		}
		Log.d("getRootStorageDirectory", result.getAbsolutePath());
		return result;
	}
	
	/**
	 * Returns a Java File initialized to a directory of given name
	 * within the given location. 
	 *
	 * @param parent_directory a File representing the directory in which the new child will reside
	 * @param directory_name the name of the desired directory
	 * @return a File pointing to the desired directory, or null if a file with conflicting name 
	 * exists or if getRootStorageDirectory was not called first
	 */
	public static File getStorageDirectory(File parent_directory, String new_child_directory_name){

		File result = new File(parent_directory, new_child_directory_name);
		if(!result.exists())
			if(result.mkdir())
				return result;
			else{
				Log.e("getStorageDirectory", "Error creating " + result.getAbsolutePath());
				return null;
			}
		else if(result.isFile()){
			return null;
		}
		
		Log.d("getStorageDirectory", "directory ready: " + result.getAbsolutePath());
		return result;
	}

}
