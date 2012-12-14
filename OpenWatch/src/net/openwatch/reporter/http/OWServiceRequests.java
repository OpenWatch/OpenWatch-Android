package net.openwatch.reporter.http;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.model.OWRecordingTag;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.orm.androrm.DatabaseAdapter;
import com.orm.androrm.Filter;

/**
 * OWService (Django) Requests 
 * @author davidbrodsky
 *
 */
public class OWServiceRequests {
	
	private static final String TAG = "OWServiceRequests";
	
	public static void getTags(final Context app_context){
		final String METHOD = "getTags";
		AsyncHttpClient client = HttpClient.setupHttpClient();
		String url = Constants.OW_URL + Constants.OW_TAGS;
		Log.i(TAG, "POST: " + url);
		client.post(url, new JsonHttpResponseHandler(){

    		@Override
    		public void onSuccess(JSONObject response){
    			
    			JSONArray array_json;
				try {
					array_json = (JSONArray) response.get("tags");
					JSONObject tag_json;
	    			
	    			DatabaseAdapter adapter = DatabaseAdapter.getInstance(app_context);
	    			adapter.beginTransaction();
	    		
	    			OWRecordingTag tag;
	    			for(int x=0; x<array_json.length(); x++){
	    				tag_json = array_json.getJSONObject(x);
	    				Filter filter = new Filter();
	    				filter.is(DBConstants.TAG_TABLE_NAME, tag_json.getString("name"));
	    				if(OWRecordingTag.objects(app_context, OWRecordingTag.class).filter(filter).count() == 0){
	    					tag = new OWRecordingTag();
		    				tag.name.set(tag_json.getString("name"));
		    				tag.is_featured.set(tag_json.getBoolean("featured"));
		    				tag.save(app_context);
	    				}
	    				Log.i(TAG, METHOD + " added tag: " +tag_json.getString("name") );
	    				
	    			}
	    			
	    			adapter.commitTransaction();
	    			
				} catch (JSONException e) {
					Log.e(TAG, METHOD + " failed to parse JSON");
					e.printStackTrace();
				}

    		}
    		
    		@Override
    	     public void onFailure(Throwable e, String response) {
    			Log.i(TAG, METHOD + " failure: " +  response);
    	     }
    		
    		@Override
    	     public void onFinish() {
    	        Log.i(TAG, METHOD +" finished");
    	     }

		});
	}
	
	/**
     * Login an existing account with the OpenWatch service
     */
    public static void UserLogin(Context app_context, StringEntity post_body, JsonHttpResponseHandler response_handler){
    	AsyncHttpClient http_client = HttpClient.setupHttpClient(app_context);
    	Log.i(TAG,"Commencing login to: " + Constants.OW_URL + Constants.OW_LOGIN);
    	http_client.post(app_context, Constants.OW_URL + Constants.OW_LOGIN, post_body, "application/json", response_handler);
    	
    }
    
    /**
     * Create a new account with the OpenWatch servicee
     */
    public static void UserSignup(Context app_context, StringEntity post_body, JsonHttpResponseHandler response_handler){
    	AsyncHttpClient http_client = HttpClient.setupHttpClient(app_context);
    	Log.i(TAG,"Commencing signup to: " + Constants.OW_URL + Constants.OW_SIGNUP);
    	http_client.post(app_context, Constants.OW_URL + Constants.OW_SIGNUP, post_body, "application/json", response_handler);
    	
    }
    
    /**
     * Registers this mobile app with the OpenWatch service
     * sends the application version number
     */
    public static void RegisterApp(Context app_context, String public_upload_token, JsonHttpResponseHandler response_handler){
    	PackageInfo pInfo;
    	String app_version = "Android-";
		try {
			pInfo = app_context.getPackageManager().getPackageInfo(app_context.getPackageName(), 0);
			app_version += pInfo.versionName;
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Unable to read PackageName in RegisterApp");
			e.printStackTrace();
			app_version += "unknown";
		}
		
		HashMap<String,String> params = new HashMap<String, String>();
    	params.put(Constants.PUB_TOKEN, public_upload_token);
    	params.put(Constants.OW_SIGNUP_TYPE, app_version);
    	Gson gson = new Gson();
    	StringEntity se = null;
    	try {
			se = new StringEntity(gson.toJson(params));
		} catch (UnsupportedEncodingException e1) {
			Log.e(TAG,"Failed to put JSON string in StringEntity");
			e1.printStackTrace();
			return;
		}
		
		// Post public_upload_token, signup_type
		AsyncHttpClient http_client = HttpClient.setupHttpClient(app_context);
    	Log.i(TAG,"Commencing ap registration to: " + Constants.OW_URL + Constants.OW_REGISTER + " pub_token: " + public_upload_token + " version: " + app_version);
    	http_client.post(app_context, Constants.OW_URL + Constants.OW_REGISTER, se, "application/json", response_handler);
    	
    }
}