package net.openwatch.reporter.http;

import android.util.Log;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

public class Utils {
	private static final String TAG = "HttpUtils";
	
	public static StringEntity JSONObjectToStringEntity(JSONObject json_obj){
		StringEntity se = null;
		try {
			se = new StringEntity(json_obj.toString());
		} catch (UnsupportedEncodingException e1) {
			Log.e(TAG, "json->stringentity failed");
			e1.printStackTrace();
		}
		return se;
	}

}
