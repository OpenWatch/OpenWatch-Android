package net.openwatch.reporter.http;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.openwatch.reporter.OWApplication;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.Constants.HIT_TYPE;
import net.openwatch.reporter.constants.Constants.CONTENT_TYPE;
import net.openwatch.reporter.constants.Constants.MEDIA_TYPE;
import net.openwatch.reporter.constants.Constants.OWFeedType;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.contentprovider.OWContentProvider;
import net.openwatch.reporter.location.DeviceLocation;
import net.openwatch.reporter.location.DeviceLocation.GPSRequestCallback;
import net.openwatch.reporter.model.OWAudio;
import net.openwatch.reporter.model.OWFeed;
import net.openwatch.reporter.model.OWInvestigation;
import net.openwatch.reporter.model.OWMediaObject;
import net.openwatch.reporter.model.OWServerObject;
import net.openwatch.reporter.model.OWPhoto;
import net.openwatch.reporter.model.OWVideoRecording;
import net.openwatch.reporter.model.OWTag;
import net.openwatch.reporter.model.OWStory;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.orm.androrm.DatabaseAdapter;
import com.orm.androrm.Filter;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;

/**
 * OWService (Django) Requests
 * 
 * @author davidbrodsky
 * 
 */
public class OWServiceRequests {

	private static final String TAG = "OWServiceRequests";

	public interface RequestCallback {
		public void onFailure();
		public void onSuccess();
	}
	
	public interface PaginatedRequestCallback{
		public void onSuccess(int page, int object_count, int total_pages);
		public void onFailure(int page);
	}
	
	
	public static void increaseHitCount(final Context app_context, int server_id, final int media_obj_id, final CONTENT_TYPE content_type, final MEDIA_TYPE media_type, final HIT_TYPE hit_type){
		final String METHOD = "increaseHitCount";
		JsonHttpResponseHandler post_handler = new JsonHttpResponseHandler(){
			@Override
			public void onSuccess(JSONObject response) {
				Log.i(TAG, METHOD + "success! " + response.toString());
				try {
					if (response.has(Constants.OW_STATUS) && response.getString(Constants.OW_STATUS).compareTo(Constants.OW_SUCCESS) ==0) {
				
						OWServerObject obj = OWServerObject.objects(app_context, OWServerObject.class).get(media_obj_id);
						if(response.has(Constants.OW_HITS)){
							switch(hit_type){
								case VIEW:
									obj.setViews(app_context, response.getInt(Constants.OW_HITS));
									break;
								case CLICK:
									obj.setActions(app_context, response.getInt(Constants.OW_HITS));
									break;
							}
						}
						obj.save(app_context);
					}
				} catch (JSONException e) {
					Log.e(TAG, "Error processing hitcount response");
					e.printStackTrace();
				}
			}
		};
		
		JSONObject params = new JSONObject();
		try {
			params.put(Constants.OW_HIT_SERVER_ID, server_id);
			if(content_type == CONTENT_TYPE.MEDIA_OBJECT)
				params.put(Constants.OW_HIT_MEDIA_TYPE, media_type.toString().toLowerCase());
			else
				params.put(Constants.OW_HIT_MEDIA_TYPE, content_type.toString().toLowerCase());
			params.put(Constants.OW_HIT_TYPE, hit_type.toString().toLowerCase());
		} catch (JSONException e) {
			e.printStackTrace();
		}
	
		AsyncHttpClient http_client = HttpClient.setupAsyncHttpClient(app_context);
		http_client.post(app_context, Constants.OW_API_URL + Constants.OW_HIT_URL, Utils.JSONObjectToStringEntity(params),
				"application/json", post_handler);
		
	}

	public static void getStory(final Context app_context, final int id,
			final RequestCallback callback) {
		final String METHOD = "getStory";

		JsonHttpResponseHandler get_handler = new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(JSONObject response) {
				Log.i(TAG, METHOD + "success! " + response.toString());
				if (response.has(Constants.OW_STORY)) {
					try {
						OWStory.createOrUpdateOWStoryWithJson(app_context,
								response.getJSONObject(Constants.OW_STORY));
						callback.onSuccess();
					} catch (JSONException e) {
						Log.e(TAG,
								"Error creating or updating Recording with json");
						e.printStackTrace();
					}
				}
			}

			@Override
			public void onFailure(Throwable e, JSONObject errorResponse) {
				Log.i(TAG, METHOD + " failed: " + errorResponse.toString());
				e.printStackTrace();
				callback.onFailure();
			}
		};

		AsyncHttpClient http_client = HttpClient.setupAsyncHttpClient(app_context);
		http_client.get(Constants.OW_API_URL + Constants.OW_STORY
				+ File.separator + String.valueOf(id), get_handler);
		Log.i(TAG, METHOD + " : " + Constants.OW_API_URL + Constants.OW_STORY
				+ File.separator + String.valueOf(id));
	}

	public static void getRecording(final Context app_context,
			final String uuid, final RequestCallback callback) {
		final String METHOD = "getRecording";
		if (uuid == null)
			return;

		JsonHttpResponseHandler get_handler = new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(JSONObject response) {
				if (response.has(Constants.OW_RECORDING)) {
					try {
						OWVideoRecording.createOrUpdateOWRecordingWithJson(
								app_context,
								response.getJSONObject(Constants.OW_RECORDING));
						callback.onSuccess();
					} catch (JSONException e) {
						Log.e(TAG,
								"Error creating or updating Recording with json");
						e.printStackTrace();
					}
				}
			}

			@Override
			public void onFailure(Throwable e, JSONObject errorResponse) {
				Log.i(TAG, METHOD + " failed: " + errorResponse.toString());
				e.printStackTrace();
				callback.onFailure();
			}
		};

		AsyncHttpClient http_client = HttpClient.setupAsyncHttpClient(app_context);
		http_client.get(Constants.OW_API_URL + Constants.OW_RECORDING
				+ File.separator + uuid, get_handler);
		Log.i(TAG, METHOD + " : " + Constants.OW_API_URL
				+ Constants.OW_RECORDING + File.separator + uuid);
	}
	
	/**
	 * Gets the device's current location and posts it in OW standard format
	 * along with a request for a feed.
	 * 
	 * OW standard location format:
	 * {
   	 *	'location': {
     *  		'latitude':39.00345433,
     *  		'longitude':-70.2440393
   	 *	}
`	 * }
	 * @param app_context
	 * @param feed
	 * @param page
	 * @param cb
	 */
	public static void getGeoFeed(final Context app_context, final Location gps_location, final String feed_name, final int page, final PaginatedRequestCallback cb){
	
		JSONObject root = new JSONObject();
		JSONObject location = new JSONObject();
		try {
			if(gps_location != null){
				location.put("latitude", gps_location.getLatitude());
				location.put("longitude", gps_location.getLongitude());
			}

			root.put("location", location);
			getFeed(app_context, root, feed_name, page, cb);
			Log.i(TAG, "got location, fetching geo feed");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void getFeed(final Context app_context, final String feed_name, final int page, final PaginatedRequestCallback cb){
		getFeed(app_context, null, feed_name, page, cb);
	}
	

	/**
	 * Parse an OpenWatch.net feed, saving its contents to the databasee
	 * 
	 * @param app_context
	 * @param feed
	 *            The type of feed to return. See OWServiceRequests.OWFeed
	 */
	private static void getFeed(final Context app_context, JSONObject params, final String ext_feed_name, final int page, final PaginatedRequestCallback cb){
		final String METHOD = "getFeed";
		final String feed_name = ext_feed_name.trim().toLowerCase();
		
		JsonHttpResponseHandler get_handler = new JsonHttpResponseHandler(){
			@Override
    		public void onSuccess(JSONObject response){
				if(response.has("objects")){
					int internal_user_id = 0;
					Log.i(TAG, String.format("got %s feed response: %s ",feed_name, response.toString()) );
					if(feed_name.compareTo(OWFeedType.USER.toString().toLowerCase()) == 0){
						SharedPreferences profile = app_context.getSharedPreferences(Constants.PROFILE_PREFS, 0);
				        internal_user_id = profile.getInt(Constants.INTERNAL_USER_ID, 0);
					}
					
					ParseFeedTask parseTask = new ParseFeedTask(app_context, cb, feed_name, page, internal_user_id);
					parseTask.execute(response);
					
				}else
					Log.e(TAG, "Feed response format unexpected" + response.toString());
			}
			
			@Override
			public void onFailure(Throwable e, JSONObject errorResponse){
				if(cb != null)
					cb.onFailure(page);
				Log.i(TAG, METHOD + " failed: " + errorResponse.toString());
				e.printStackTrace();
			}
		};
		
		AsyncHttpClient http_client = HttpClient.setupAsyncHttpClient(app_context);
		String endpoint = Constants.feedExternalEndpointFromString(feed_name, page);
		
		if(feed_name.compareTo(OWFeedType.LOCAL.toString().toLowerCase()) == 0){
			http_client.post(app_context, endpoint, Utils.JSONObjectToStringEntity(params), "application/json", get_handler);
		}
		//else if(OWFeedType.`){
			
		//}
		else{
			http_client.get(endpoint, get_handler);
		}		
		
		Log.i(TAG, "getFeed: " + endpoint);
	}

	/**
	 * Parses a Feed, updating the database and notifying
	 * the appropriate feed uris of changed content
	 * @author davidbrodsky
	 *
	 */
	public static class ParseFeedTask extends AsyncTask<JSONObject, Void, Boolean> {
		
		PaginatedRequestCallback cb;
		Context c;
		String feed_name;
		boolean success = false;
		
		int object_count = -1;
		int page_count = -1;
		int page_number = -1;
		int user_id = -1;
		
		int current_page = -1;
		
		public ParseFeedTask(Context c, PaginatedRequestCallback cb, String feed_name, int current_page, int user_id){
			this.cb = cb;
			this.c = c;
			this.feed_name = feed_name;
			this.current_page = current_page;
			this.user_id = user_id;
		}
		

		@Override
		protected Boolean doInBackground(JSONObject... params) {
			DatabaseAdapter adapter = DatabaseAdapter.getInstance(c);
			adapter.beginTransaction();
			
			JSONObject response = params[0];
			try{
				//If we're fetching the first page, it 
				//means we're rebuilding this feed
				if(current_page == 1){
					Filter filter = new Filter();
					filter.is(DBConstants.FEED_NAME, feed_name);
					QuerySet<OWFeed> feedset = OWFeed.objects(c, OWFeed.class).filter(filter);
					for(OWFeed feed : feedset){
						feed.delete(c);
					}
				}
				// Now parse the new data and create the feed anew
				JSONArray json_array = response.getJSONArray("objects");
				JSONObject json_obj;
				for(int x=0; x<json_array.length(); x++){
					json_obj = json_array.getJSONObject(x);
					if(json_obj.has("type")){
						if(json_obj.getString("type").compareTo("video") == 0)
							OWVideoRecording.createOrUpdateOWRecordingWithJson(c, json_obj, OWFeed.getFeedFromString(c, feed_name));
						else if(json_obj.getString("type").compareTo("story") == 0)
							OWStory.createOrUpdateOWStoryWithJson(c, json_obj, OWFeed.getFeedFromString(c, feed_name));
						else if(json_obj.getString("type").compareTo("photo") == 0)
							OWPhoto.createOrUpdateOWPhotoWithJson(c, json_obj, OWFeed.getFeedFromString(c, feed_name));
						else if(json_obj.getString("type").compareTo("audio") == 0)
							OWAudio.createOrUpdateOWAudioWithJson(c, json_obj, OWFeed.getFeedFromString(c, feed_name));
						else if(json_obj.getString("type").compareTo("investigation") == 0)
							OWInvestigation.createOrUpdateOWInvestigationWithJson(c, json_obj, OWFeed.getFeedFromString(c, feed_name));
						
					}
				}
				adapter.commitTransaction();
				Uri baseUri;
				if(feed_name.compareTo(OWFeedType.USER.toString().toLowerCase()) == 0){
					baseUri = OWContentProvider.getUserRecordingsUri(user_id);
				}else{
					baseUri = OWContentProvider.getFeedUri(feed_name);
				}
				Log.i("URI" + feed_name, "notify change on uri: " + baseUri.toString());
				c.getContentResolver().notifyChange(baseUri, null);   
	
				
				if(cb != null){
					if(response.has("meta")){
						JSONObject meta = response.getJSONObject("meta");
						if(meta.has("object_count"))
							object_count = meta.getInt("object_count");
						if(meta.has("page_count"))
							page_count = meta.getInt("page_count");
						if(meta.has("page_number")){
							try{
							page_number = Integer.parseInt(meta.getString("page_number"));
							}catch(Exception e){};
						}
						
						
					}
					
				}
				success = true;
			}catch(JSONException e){
				adapter.rollbackTransaction();
				Log.e(TAG, "Error parsing " + feed_name + " feed response");
				e.printStackTrace();
			}
			
			return null;
		}
		
		@Override
	    protected void onPostExecute(Boolean result) {
	        super.onPostExecute(result);
	        if(success)
	        	cb.onSuccess(page_number, object_count, page_count);
	    }
		
	}
	/*
	public static void syncOWMobileGeneratedObject(final Context app_context, OWMobileGeneratedObject object ){
		JsonHttpResponseHandler get_handler = new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(JSONObject response) {
				Log.i(TAG, "getMeta response: " + response.toString());
				
			}

			@Override
			public void onFailure(Throwable e, String response) {
				Log.i(TAG, "syncRecording fail: " + response);
				e.printStackTrace();
			}

			@Override
			public void onFinish() {
				Log.i(TAG, "syncRecording finish");
			}

		};
		
	}*/
	
	public static void createOWMobileGeneratedObject(final Context app_context, final OWMediaObject object){
		JsonHttpResponseHandler post_handler = new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(JSONObject response) {
				Log.i(TAG, "getObject response: " + response.toString());
				try {
					if(response.has("success") && response.getBoolean("success")){
						Log.i(TAG, "create object success!");
						object.updateWithJson(app_context, response.getJSONObject("object"));
						sendOWMobileGeneratedObjectMedia(app_context, object);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onFailure(Throwable e, String response) {
				Log.i(TAG, "getObject fail: " + response);
				e.printStackTrace();
			}

			@Override
			public void onFinish() {
				Log.i(TAG, "getObject finish");
			}

		};
		
		AsyncHttpClient http_client = HttpClient.setupAsyncHttpClient(app_context);
		Log.i(TAG, "Commencing Create OWMGObject " + Constants.OW_API_URL
				+ Constants.OW_RECORDING);
		//Log.i(TAG, object.getType().toString());
		//Log.i(TAG, object.toJsonObject(app_context).toString());
		//Log.i(TAG, Utils.JSONObjectToStringEntity(object.toJsonObject(app_context)).toString());
		http_client.post(app_context, endpointForMediaType(object.getMediaType(app_context)) , Utils
				.JSONObjectToStringEntity(object.toJsonObject(app_context)), "application/json", post_handler);
	}
	
	public static void sendOWMobileGeneratedObjectMedia(final Context app_context, final OWMediaObject object){
		new Thread(){
			public void run(){
				String file_response;
				try {
					file_response = OWMediaRequests.ApacheFilePost(app_context, instanceEndpointForOWMediaObject(app_context, object), object.getMediaFilepath(app_context), "file_data");
					Log.i(TAG, "binary media sent! response: " + file_response);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}.start();

	}
	
	private static String instanceEndpointForOWMediaObject(Context c, OWMediaObject object){
		return Constants.OW_API_URL + Constants.API_ENDPOINT_BY_MEDIA_TYPE.get(object.getMediaType(c)) + "/" + object.getUUID(c) +"/";
	}
	
	private static String endpointForMediaType(MEDIA_TYPE type){
		return Constants.OW_API_URL + Constants.API_ENDPOINT_BY_MEDIA_TYPE.get(type) + "/";
	}
	

	/**
	 * Fetch server recording data, check last_edited, and push update if
	 * necessary
	 * 
	 * @param app_context
	 * @param cb
	 */
	public static void syncOWMediaObject(final Context app_context,
			 final OWMediaObject object) {
		final String METHOD = "syncRecording";
		final int model_id = ((Model) object).getId(); // will this work?
		JsonHttpResponseHandler get_handler = new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(JSONObject response) {
				Log.i(TAG, "getMeta response: " + response.toString());
				if (response.has("recording")) {
					try {
						JSONObject object_json = response
								.getJSONObject("object");
						// response was successful
						OWServerObject media_object = OWServerObject.objects(
								app_context, OWServerObject.class).get(
								model_id);
						Date last_edited_remote = Constants.utc_formatter
								.parse(object_json.getString("last_edited"));
						Date last_edited_local = Constants.utc_formatter.parse(media_object
								.getLastEdited(app_context));
						Log.i(TAG,
								"Sync dates. Remote: "
										+ object_json
												.getString("last_edited")
										+ " local: "
										+ media_object.getLastEdited(app_context));
						// If local recording has no server_id, pull in fields
						// generated by server as there's no risk of conflict
						boolean doSave = false;
						// using the interface methods requires saving before
						// the object ref is lost
						if (object_json.has(Constants.OW_SERVER_ID)) {
							media_object.server_id.set(object_json
									.getInt(Constants.OW_SERVER_ID));
							// recording.setServerId(app_context,
							// recording_json.getInt(Constants.OW_SERVER_ID));
							doSave = true;
						}
						if (object_json.has(Constants.OW_FIRST_POSTED)) {
							media_object.first_posted.set(object_json
									.getString(Constants.OW_FIRST_POSTED));
							// recording.setFirstPosted(app_context,
							// recording_json.getString(Constants.OW_FIRST_POSTED));
							doSave = true;
						}
						if (object_json.has(Constants.OW_THUMB_URL)) {
							media_object.thumbnail_url.set(object_json
									.getString(Constants.OW_THUMB_URL));
							// recording.setThumbnailUrl(app_context,
							// recording_json.getString(Constants.OW_THUMB_URL));
							// recording.media_object.get(app_context).save(app_context);
							doSave = true;
						}
						if (doSave == true) {
							// media_obj.save(app_context);
							media_object.save(
									app_context);
						}

						if (last_edited_remote.after(last_edited_local)) {
							// copy remote data to local
							Log.i(TAG, "remote recording data is more recent");
							media_object.updateWithJson(app_context,
									object_json);
						} else if (last_edited_remote.before(last_edited_local)) {
							// copy local to remote
							Log.i(TAG, "local recording data is more recent");
							OWServiceRequests.editOWMediaObject(app_context,
									object, new JsonHttpResponseHandler() {
										@Override
										public void onSuccess(
												JSONObject response) {
											Log.i(TAG,
													"editRecording response: "
															+ response);
										}

										@Override
										public void onFailure(Throwable e,
												JSONObject response) {
											Log.i(TAG, "editRecording failed: "
													+ response);
											e.printStackTrace();
										}
									});
						}
					} catch (Exception e) {
						Log.e(TAG, METHOD + "failed to handle response");
						e.printStackTrace();
					}
				}

			}

			@Override
			public void onFailure(Throwable e, String response) {
				Log.i(TAG, "syncRecording fail: " + response);
				e.printStackTrace();
			}

			@Override
			public void onFinish() {
				Log.i(TAG, "syncRecording finish");
			}

		};
		getOWMediaObjectMeta(app_context, object, get_handler);

	}

	public static void getOWMediaObjectMeta(Context app_context, OWMediaObject object, JsonHttpResponseHandler response_handler) {
		AsyncHttpClient http_client = HttpClient.setupAsyncHttpClient(app_context);
		Log.i(TAG, "Commencing Get Recording Meta: " + instanceEndpointForOWMediaObject(app_context, object));
		http_client.get(instanceEndpointForOWMediaObject(app_context, object), response_handler);
	}

	/**
	 * Post recording data to server
	 * 
	 * @param app_context
	 * @param recording
	 * @param response_handler
	 */
	public static void editOWMediaObject(Context app_context,
			OWMediaObject object, JsonHttpResponseHandler response_handler) {
		AsyncHttpClient http_client = HttpClient.setupAsyncHttpClient(app_context);
		Log.i(TAG,
				"Commencing Edit Recording: "
						+ object.toJsonObject(app_context));
		http_client.post(app_context, instanceEndpointForOWMediaObject(app_context, object), Utils
				.JSONObjectToStringEntity(object.toJsonObject(app_context)),
				"application/json", response_handler);

	}

	public static void setTags(final Context app_context, QuerySet<OWTag> tags) {
		final String METHOD = "setTags";
		JSONObject result = new JSONObject();
		JSONArray tag_list = new JSONArray();
		for (OWTag tag : tags) {
			tag_list.put(tag.toJson());
		}
		try {
			result.put(Constants.OW_TAGS, tag_list);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		AsyncHttpClient client = HttpClient.setupAsyncHttpClient(app_context);
		String url = Constants.OW_API_URL + Constants.OW_TAGS;
		Log.i(TAG,
				"commencing " + METHOD + " : " + url + " json: "
						+ result.toString());
		client.post(app_context, url, Utils.JSONObjectToStringEntity(result),
				"application/json", new JsonHttpResponseHandler() {

					@Override
					public void onSuccess(JSONObject response) {
						Log.i(TAG, METHOD + " success : " + response.toString());
					}

					@Override
					public void onFailure(Throwable e, String response) {
						Log.i(TAG, METHOD + " failure: " + response);

					}

					@Override
					public void onFinish() {
						Log.i(TAG, METHOD + " finish: ");

					}
				});
	}

	/**
	 * Merges server tag list with stored. Assume names are unchanging (treated
	 * as primary key)
	 * 
	 * @param app_context
	 * @param cb
	 */
	public static void getTags(final Context app_context,
			final RequestCallback cb) {
		final String METHOD = "getTags";
		AsyncHttpClient client = HttpClient.setupAsyncHttpClient(app_context);
		String url = Constants.OW_API_URL + Constants.OW_TAGS;
		Log.i(TAG, "commencing getTags: " + url);
		client.get(url, new JsonHttpResponseHandler() {

			@Override
			public void onSuccess(JSONObject response) {

				JSONArray array_json;
				try {
					array_json = (JSONArray) response.get("tags");
					JSONObject tag_json;

					int tag_count = OWTag.objects(app_context, OWTag.class)
							.count();

					DatabaseAdapter adapter = DatabaseAdapter
							.getInstance(app_context);
					adapter.beginTransaction();

					OWTag tag = null;
					for (int x = 0; x < array_json.length(); x++) {
						tag_json = array_json.getJSONObject(x);
						tag = OWTag.getOrCreateTagFromJson(app_context, tag_json);
						tag.save(app_context);
						//Log.i(TAG,
						Log.i("TAGGIN",
								METHOD + " saved tag: " + tag.name.get()
										+ " featured: "
										+ String.valueOf(tag.is_featured.get()));

					}

					adapter.commitTransaction();
					Log.i(TAG, "getTags success :" + response.toString());
					if (cb != null)
						cb.onSuccess();
				} catch (JSONException e) {
					Log.e(TAG, METHOD + " failed to parse JSON: " + response);
					e.printStackTrace();
				}

			}

			@Override
			public void onFailure(Throwable e, String response) {
				Log.i(TAG, METHOD + " failure: " + response);
				if (cb != null)
					cb.onFailure();
			}

			@Override
			public void onFinish() {
				Log.i(TAG, METHOD + " finished");
			}

		});
	}

	/**
	 * Login an existing account with the OpenWatch service
	 */
	public static void UserLogin(Context app_context, StringEntity post_body,
			JsonHttpResponseHandler response_handler) {
		AsyncHttpClient http_client = HttpClient.setupAsyncHttpClient(app_context);
		Log.i(TAG, "Commencing login to: " + Constants.OW_API_URL
				+ Constants.OW_LOGIN);
		http_client.post(app_context,
				Constants.OW_API_URL + Constants.OW_LOGIN, post_body,
				"application/json", response_handler);

	}

	/**
	 * Create a new account with the OpenWatch servicee
	 */
	public static void UserSignup(Context app_context, StringEntity post_body,
			JsonHttpResponseHandler response_handler) {

		AsyncHttpClient http_client = HttpClient.setupAsyncHttpClient(app_context);
		Log.i(TAG, "Commencing signup to: " + Constants.OW_API_URL
				+ Constants.OW_SIGNUP);
		http_client.post(app_context, Constants.OW_API_URL
				+ Constants.OW_SIGNUP, post_body, "application/json",
				response_handler);

	}

	/**
	 * Registers this mobile app with the OpenWatch service sends the
	 * application version number
	 */
	public static void RegisterApp(Context app_context,
			String public_upload_token, JsonHttpResponseHandler response_handler) {
		PackageInfo pInfo;
		String app_version = "Android-";
		try {
			pInfo = app_context.getPackageManager().getPackageInfo(
					app_context.getPackageName(), 0);
			app_version += pInfo.versionName;
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Unable to read PackageName in RegisterApp");
			e.printStackTrace();
			app_version += "unknown";
		}

		HashMap<String, String> params = new HashMap<String, String>();
		params.put(Constants.PUB_TOKEN, public_upload_token);
		params.put(Constants.OW_SIGNUP_TYPE, app_version);
		Gson gson = new Gson();
		StringEntity se = null;
		try {
			se = new StringEntity(gson.toJson(params));
		} catch (UnsupportedEncodingException e1) {
			Log.e(TAG, "Failed to put JSON string in StringEntity");
			e1.printStackTrace();
			return;
		}

		// Post public_upload_token, signup_type
		AsyncHttpClient http_client = HttpClient.setupAsyncHttpClient(app_context);
		Log.i(TAG, "Commencing ap registration to: " + Constants.OW_API_URL
				+ Constants.OW_REGISTER + " pub_token: " + public_upload_token
				+ " version: " + app_version);
		http_client.post(app_context, Constants.OW_API_URL
				+ Constants.OW_REGISTER, se, "application/json",
				response_handler);

	}

	public static void onLaunchSync(final Context app_context) {
		RequestCallback cb = new RequestCallback() {

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