package org.ale.openwatch.file;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.ale.openwatch.constants.Constants;

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
	
	/**
	 * Open a bitmap at the specified Uri, downsampling as we go to
	 * minimize memory usage
	 * @param c
	 * @param selectedImage
	 * @return
	 * @throws FileNotFoundException
	 */
	public static Bitmap decodeUri(Context c, Uri selectedImage, final int REQUIRED_SIZE) throws FileNotFoundException {

        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(c.getContentResolver().openInputStream(selectedImage), null, o);

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
               || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(c.getContentResolver().openInputStream(selectedImage), null, o2);

    }
	
	/**
	 * Prepare an output directory or file for writing
	 * @param c
	 * @param type
	 * @param filename
	 * @return
	 */
	public static File prepareOutputLocation(Context c, Constants.CONTENT_TYPE type, String uuid, String filename, String extension){
		String media_dir = "";
		switch(type){
		case VIDEO:
			media_dir = Constants.VIDEO_OUTPUT_DIR;
            break;
		case AUDIO:
			media_dir = Constants.AUDIO_OUTPUT_DIR;
            break;
		case PHOTO:
			media_dir = Constants.PHOTO_OUTPUT_DIR;
            break;
		}
		File output = FileUtils.getStorageDirectory(
				FileUtils.getRootStorageDirectory(c,
						Constants.ROOT_OUTPUT_DIR),
				media_dir);
		
		
		output = FileUtils.getStorageDirectory(output, uuid);
        return prepareOutputLocation(c, output, filename, extension);
		
	}

    public static File prepareOutputLocation(Context c, File root, String filename, String extension){
        File output = null;
        try {
            if(filename != null){
                output = File.createTempFile(filename, extension, root);
            }
            return output;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getRealPathFromURI(Context c, Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = c.getContentResolver().query(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

}
