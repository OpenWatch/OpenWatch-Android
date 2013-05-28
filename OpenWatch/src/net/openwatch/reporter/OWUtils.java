package net.openwatch.reporter;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import android.widget.ImageView;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.model.OWServerObject;

import java.util.UUID;

public class OWUtils {
	
	public static String generateRecordingIdentifier()
	{
		return UUID.randomUUID().toString();
	}
	
	public static void loadScaledPicture(String image_path, ImageView target) {
	    // Get the dimensions of the View
	    int targetW = target.getWidth();
	    int targetH = target.getHeight();
	  
	    // Get the dimensions of the bitmap
	    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
	    bmOptions.inJustDecodeBounds = true;
	    BitmapFactory.decodeFile(image_path, bmOptions);
	    int photoW = bmOptions.outWidth;
	    int photoH = bmOptions.outHeight;
	  
	    // Determine how much to scale down the image
	    int scaleFactor = Math.min(photoW/targetW, photoH/targetH);
	  
	    // Decode the image file into a Bitmap sized to fill the View
	    bmOptions.inJustDecodeBounds = false;
	    bmOptions.inSampleSize = scaleFactor;
	    bmOptions.inPurgeable = true;
	  
	    Bitmap bitmap = BitmapFactory.decodeFile(image_path, bmOptions);
	    target.setImageBitmap(bitmap);
	}
	
	public static String urlForOWServerObject(OWServerObject obj, Context c){
		String url = Constants.OW_URL;
		if(obj.getMediaType(c) != null)
			url += Constants.API_ENDPOINT_BY_MEDIA_TYPE.get(obj.getMediaType(c));
		else if(obj.getContentType(c) != null)
			url += Constants.API_ENDPOINT_BY_CONTENT_TYPE.get(obj.getContentType(c));
		
		url += "/" + String.valueOf(obj.getServerId(c)) + "/";
		return url;
	}

    public static boolean checkEmail(String email) {
        return Constants.EMAIL_ADDRESS_PATTERN.matcher(email).matches();
    }

    public static String getPackageVersion(Context c){
        String packageVersion = "";
        try {
            PackageInfo pInfo = c.getPackageManager().getPackageInfo(
                    c.getPackageName(), 0);
            packageVersion += "I have OpenWatch version " + pInfo.versionName;
            packageVersion += " running on Android API " + String.valueOf(Build.VERSION.SDK_INT) + ".";
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("getPackageVersion", "Unable to read PackageName in RegisterApp");
            e.printStackTrace();
        }
        return packageVersion;
            //USER_AGENT += " (Android API " + Build.VERSION.RELEASE + ")";
    }

}
