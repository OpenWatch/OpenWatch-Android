package net.openwatch.reporter;

import java.util.UUID;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.model.OWServerObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

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

}
