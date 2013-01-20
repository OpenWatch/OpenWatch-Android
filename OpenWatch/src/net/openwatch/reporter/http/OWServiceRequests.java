package net.openwatch.reporter.http;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.openwatch.reporter.OWApplication;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.Constants.OWFeedType;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.model.OWFeed;
import net.openwatch.reporter.model.OWMediaObject;
import net.openwatch.reporter.model.OWVideoRecording;
import net.openwatch.reporter.model.OWTag;
import net.openwatch.reporter.model.OWStory;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.orm.androrm.DatabaseAdapter;
import com.orm.androrm.Filter;
import com.orm.androrm.QuerySet;

/**
 * OWService (Django) Requests 
 * @author davidbrodsky
 *
 */
public class OWServiceRequests {
	
	private static final String TAG = "OWServiceRequests";
	
	public interface RequestCallback{
		public void onFailure();
		public void onSuccess();
	}
	
	public static void getStory(final Context app_context, final int id, final RequestCallback callback){
		final String METHOD = "getStory";
		
		JsonHttpResponseHandler get_handler = new JsonHttpResponseHandler(){
			@Override
    		public void onSuccess(JSONObject response){
				if(response.has(Constants.OW_STORY)){
					try {
						OWStory.createOrUpdateOWStoryWithJson(app_context, response.getJSONObject(Constants.OW_STORY));
						callback.onSuccess();
					} catch (JSONException e) {
						Log.e(TAG, "Error creating or updating Recording with json");
						e.printStackTrace();
					}
				}
			}
			
			@Override
			public void onFailure(Throwable e, JSONObject errorResponse){
				Log.i(TAG, METHOD + " failed: " + errorResponse.toString());
				e.printStackTrace();
				callback.onFailure();
			}
		};
		
		AsyncHttpClient http_client = HttpClient.setupHttpClient(app_context);
		http_client.get(Constants.OW_API_URL + Constants.OW_STORY + File.separator + String.valueOf(id), get_handler);
		Log.i(TAG, METHOD + " : " + Constants.OW_API_URL + Constants.OW_STORY + File.separator + String.valueOf(id));
	}
	
	public static void getRecording(final Context app_context, final String uuid, final RequestCallback callback){
		final String METHOD = "getRecording";
		if(uuid == null)
			return;
		
		JsonHttpResponseHandler get_handler = new JsonHttpResponseHandler(){
			@Override
    		public void onSuccess(JSONObject response){
				if(response.has(Constants.OW_RECORDING)){
					try {
						OWVideoRecording.createOrUpdateOWRecordingWithJson(app_context, response.getJSONObject(Constants.OW_RECORDING));
						callback.onSuccess();
					} catch (JSONException e) {
						Log.e(TAG, "Error creating or updating Recording with json");
						e.printStackTrace();
					}
				}
			}
			
			@Override
			public void onFailure(Throwable e, JSONObject errorResponse){
				Log.i(TAG, METHOD + " failed: " + errorResponse.toString());
				e.printStackTrace();
				callback.onFailure();
			}
		};
		
		AsyncHttpClient http_client = HttpClient.setupHttpClient(app_context);
		http_client.get(Constants.OW_API_URL + Constants.OW_RECORDING + File.separator + uuid, get_handler);
		Log.i(TAG, METHOD + " : " + Constants.OW_API_URL + Constants.OW_RECORDING + File.separator + uuid);
	}

	/**
	 * Parse an OpenWatch.net feed, saving its contents to the databasee
	 * @param app_context
	 * @param feed The type of feed to return. See OWServiceRequests.OWFeed
	 */
	public static void getFeed(final Context app_context, final OWFeedType feed, int page){
		final String METHOD = "getFeed";
		
		JsonHttpResponseHandler get_handler = new JsonHttpResponseHandler(){
			@Override
    		public void onSuccess(JSONObject response){
				if(response.has("objects")){
					Log.i(TAG, String.format("got %s feed response: %s ",feed.toString(), response.toString()) );
					final JSONArray json_array;
					try {
						json_array = response.getJSONArray("objects");
						/*
						new Thread(new Runnable(){

							@Override
							public void run() {
							
								try {
								*/
									DatabaseAdapter adapter = DatabaseAdapter.getInstance(app_context);
									adapter.beginTransaction();
									
									JSONObject json_obj;
									for(int x=0; x<json_array.length(); x++){
										json_obj = json_array.getJSONObject(x);
										if(json_obj.has("type")){
											if(json_obj.getString("type").compareTo("video") == 0)
												OWVideoRecording.createOrUpdateOWRecordingWithJson(app_context, json_obj, OWFeed.getFeedFromFeedType(app_context, feed));
											else if(json_obj.getString("type").compareTo("story") == 0)
												OWStory.createOrUpdateOWStoryWithJson(app_context, json_obj, OWFeed.getFeedFromFeedType(app_context, feed));
											// TODO: Story, audio, etc
										}
									}
									adapter.commitTransaction();
								/*
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								
							}
							
						}).start();
						*/
						//OWRecording.createOWRecordingsFromJSONArray(app_context, json_array, OWFeed.getFeedFromFeedType(app_context, feed));
					} catch (JSONException e) {
						Log.e(TAG, METHOD + " Error parsing recordings array from response");
						e.printStackTrace();
					}
				}else
					Log.e(TAG, "Feed response format unexpected" + response.toString());
			}
			
			@Override
			public void onFailure(Throwable e, JSONObject errorResponse){
				Log.i(TAG, METHOD + " failed: " + errorResponse.toString());
				e.printStackTrace();
			}
		};
		
		AsyncHttpClient http_client = HttpClient.setupHttpClient(app_context);
		String endpoint = Constants.feedEndpointFromType(feed);

		http_client.get(Constants.OW_API_URL + Constants.OW_FEED + File.separator + endpoint + File.separator + String.valueOf(page), get_handler);
		Log.i(TAG, "getFeed: " + Constants.OW_API_URL + Constants.OW_FEED + File.separator + endpoint);
	}

	/**
	 * Fetch server recordingd data, check last_edited, and push update if necessary
	 * @param app_context
	 * @param cb
	 */
	public static void syncRecording(final Context app_context, OWVideoRecording recording){
		final String METHOD = "syncRecording";
		final int model_id = recording.getId();
		JsonHttpResponseHandler get_handler = new JsonHttpResponseHandler(){
			@Override
    		public void onSuccess(JSONObject response){
				Log.i(TAG, "getMeta response: " + response.toString());
				if(response.has("recording")){
					try{
						JSONObject recording_json = response.getJSONObject("recording");
						// response was successful
						OWVideoRecording recording = OWVideoRecording.objects(app_context, OWVideoRecording.class).get(model_id);
						Date last_edited_remote = Constants.sdf.parse(recording_json.getString("last_edited"));
						Date last_edited_local = Constants.sdf.parse(recording.getLastEdited(app_context));
						Log.i(TAG, "Sync dates. Remote: " + recording_json.getString("last_edited") + " local: " + recording.getLastEdited(app_context));
						// If local recording has no server_id, pull in fields generated by server as there's no risk of conflict
						boolean doSave = false;
						// using the interface methods requires saving before the object ref is lost
						OWMediaObject media_obj = recording.media_object.get(app_context);
						if(recording_json.has(Constants.OW_SERVER_ID)){
							media_obj.server_id.set(recording_json.getInt(Constants.OW_SERVER_ID));
							//recording.setServerId(app_context, recording_json.getInt(Constants.OW_SERVER_ID));
							doSave = true;
						}
						if(recording_json.has(Constants.OW_FIRST_POSTED)){
							media_obj.first_posted.set(recording_json.getString(Constants.OW_FIRST_POSTED));
							//recording.setFirstPosted(app_context, recording_json.getString(Constants.OW_FIRST_POSTED));
							doSave = true;
						}
						if(recording_json.has(Constants.OW_THUMB_URL)){
							media_obj.thumbnail_url.set(recording_json.getString(Constants.OW_THUMB_URL));
							//recording.setThumbnailUrl(app_context, recording_json.getString(Constants.OW_THUMB_URL));
							//recording.media_object.get(app_context).save(app_context);
							doSave = true;
						}
						if(doSave == true){
							//media_obj.save(app_context);
							recording.media_object.get(app_context).save(app_context);
							recording.save(app_context);
						}
					
						if(last_edited_remote.after(last_edited_local)){
							// copy remote data to local
							Log.i(TAG, "remote recording data is more recent");
							recording.updateWithJson(app_context, recording_json);
						}else if(last_edited_remote.before(last_edited_local)){
							// copy local to remote
							Log.i(TAG, "local recording data is more recent");
							OWServiceRequests.editRecording(app_context, recording, new JsonHttpResponseHandler(){
								@Override
					    		public void onSuccess(JSONObject response){
									Log.i(TAG, "editRecording response: " + response);
								}
								@Override
								public void onFailure(Throwable e, JSONObject response){
									Log.i(TAG, "editRecording failed: " + response);
									e.printStackTrace();
								}
							});
						}
					} catch(Exception e){
						Log.e(TAG, METHOD + "failed to handle response");
						e.printStackTrace();
					}
				}
					
			}
			
			@Override
			public void onFailure(Throwable e, String response){
				Log.i(TAG, "syncRecording fail: " + response);
				e.printStackTrace();
			}
			
			@Override
		     public void onFinish() {
		         Log.i(TAG, "syncRecording finish");
		     }
			
		};
		getRecordingMeta(app_context, recording.uuid.get(), get_handler);

	}
	
	public static void getRecordingMeta(Context app_context, String recording_uuid, JsonHttpResponseHandler response_handler){
    	AsyncHttpClient http_client = HttpClient.setupHttpClient(app_context);
    	Log.i(TAG,"Commencing Get Recording Meta: " + Constants.OW_API_URL + Constants.OW_RECORDING);
    	http_client.get(Constants.OW_API_URL + Constants.OW_RECORDING + "/" + recording_uuid, response_handler);
    }
	
	/**
	 * Post recording data to server
	 * @param app_context
	 * @param recording
	 * @param response_handler
	 */
	public static void editRecording(Context app_context, OWVideoRecording recording, JsonHttpResponseHandler response_handler){
    	AsyncHttpClient http_client = HttpClient.setupHttpClient(app_context);
    	Log.i(TAG,"Commencing Edit Recording: " + recording.toJsonObject(app_context));
    	http_client.post(app_context, Constants.OW_API_URL + Constants.OW_RECORDING + "/" + recording.uuid.get().toString(), Utils.JSONObjectToStringEntity(recording.toJsonObject(app_context)), "application/json", response_handler);
    	
    }
	
	/**
	 * Merges server tag list with stored. Assume names are unchanging (treated as primary key)
	 * @param app_context
	 * @param cb
	 */
	public static void getTags(final Context app_context, final RequestCallback cb){
		final String METHOD = "getTags";
		AsyncHttpClient client = HttpClient.setupHttpClient(app_context);
		String url = Constants.OW_API_URL + Constants.OW_TAGS;
		Log.i(TAG, "commencing getTags: " + url);
		client.post(url, new JsonHttpResponseHandler(){

    		@Override
    		public void onSuccess(JSONObject response){
    			
    			JSONArray array_json;
				try {
					array_json = (JSONArray) response.get("tags");
					JSONObject tag_json;
					
					int tag_count = OWTag.objects(app_context, OWTag.class).count();
	    			
	    			DatabaseAdapter adapter = DatabaseAdapter.getInstance(app_context);
	    			adapter.beginTransaction();
	    			
	    			OWTag tag = null;
	    			for(int x=0; x<array_json.length(); x++){
	    				tag_json = array_json.getJSONObject(x);
	    				Filter filter = new Filter();
	    				filter.is(DBConstants.TAG_TABLE_SERVER_ID, tag_json.getString("id"));
	    				
	    				tag = null;
	    				
	    				if(tag_count != 0){
	    					// TODO: Override QuerySet.get to work on server_id field
	    					QuerySet<OWTag> tags = OWTag.objects(app_context, OWTag.class).filter(filter);
	    					for(OWTag temp_tag : tags){
	    						tag = temp_tag;
	    						break;
	    					}
	    				}
	    				if(tag == null){
	    					// this is a new tag
	    					tag = new OWTag();
	    					tag.server_id.set(tag_json.getInt("id"));
	    				}
    					tag.is_featured.set(tag_json.getBoolean("featured")); 
    					tag.name.set(tag_json.getString("name")); 

	    				tag.save(app_context);
	    				//Log.i(TAG, METHOD + " saved tag: " + tag_json.getString("name") );
	    				
	    			}
	    			
	    			adapter.commitTransaction();
	    			Log.i(TAG, "getTags success");
	    			if(cb != null)
	    				cb.onSuccess();
				} catch (JSONException e) {
					Log.e(TAG, METHOD + " failed to parse JSON");
					e.printStackTrace();
				}

    		}
    		
    		@Override
    	     public void onFailure(Throwable e, String response) {
    			Log.i(TAG, METHOD + " failure: " +  response);
    			if(cb != null)
    				cb.onFailure();
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
    	Log.i(TAG,"Commencing login to: " + Constants.OW_API_URL + Constants.OW_LOGIN);
    	http_client.post(app_context, Constants.OW_API_URL + Constants.OW_LOGIN, post_body, "application/json", response_handler);
    	
    }
    
    /**
     * Create a new account with the OpenWatch servicee
     */
    public static void UserSignup(Context app_context, StringEntity post_body, JsonHttpResponseHandler response_handler){
    	
    	AsyncHttpClient http_client = HttpClient.setupHttpClient(app_context);
    	Log.i(TAG,"Commencing signup to: " + Constants.OW_API_URL + Constants.OW_SIGNUP);
    	http_client.post(app_context, Constants.OW_API_URL + Constants.OW_SIGNUP, post_body, "application/json", response_handler);
    	
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
    	Log.i(TAG,"Commencing ap registration to: " + Constants.OW_API_URL + Constants.OW_REGISTER + " pub_token: " + public_upload_token + " version: " + app_version);
    	http_client.post(app_context, Constants.OW_API_URL + Constants.OW_REGISTER, se, "application/json", response_handler);
    	
    }
    
    public static void onLaunchSync(final Context app_context){
    	RequestCallback cb = new RequestCallback(){

			@Override
			public void onFailure() {
				Log.i(TAG, "per_launch_sync failed");
			}

			@Override
			public void onSuccess() {
				((OWApplication) app_context).per_launch_sync = true;
				Log.i(TAG, "per_launch_sync success");
			}
    	};
    	
    	OWServiceRequests.getTags(app_context, cb);
    }
}