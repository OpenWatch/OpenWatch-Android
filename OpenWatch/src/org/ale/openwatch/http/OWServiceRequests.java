package org.ale.openwatch.http;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.orm.androrm.DatabaseAdapter;
import com.orm.androrm.Filter;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;

import org.ale.openwatch.*;
import org.ale.openwatch.account.Authentication;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.constants.DBConstants;
import org.ale.openwatch.constants.Constants.CONTENT_TYPE;
import org.ale.openwatch.constants.Constants.HIT_TYPE;
import org.ale.openwatch.constants.Constants.OWFeedType;
import org.ale.openwatch.contentprovider.OWContentProvider;
import org.ale.openwatch.location.DeviceLocation;
import org.ale.openwatch.model.*;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;

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


	public static void increaseHitCount(final Context app_context, int server_id, final int media_obj_id, final CONTENT_TYPE content_type, final HIT_TYPE hit_type){
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
		http_client.get(Constants.OW_API_URL + "s"
				+ File.separator + String.valueOf(id), get_handler);
		Log.i(TAG, METHOD + " : " + Constants.OW_API_URL + "s"
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
	 * @param page
	 * @param cb
	 */
	public static void getGeoFeed(final Context app_context, final Location gps_location, final String feed_name, final int page, final PaginatedRequestCallback cb){

        String urlparams = "";
        if(gps_location != null){
            urlparams += String.format("&latitude=%f&longitude=%f",gps_location.getLatitude(), gps_location.getLongitude());
            Log.i(TAG, String.format("got location for geo feed: lat: %f , lon: %f", gps_location.getLatitude(), gps_location.getLongitude()));
        }

        getFeed(app_context, urlparams, feed_name, page, cb);

	}
	
	public static void getFeed(final Context app_context, final String feed_name, final int page, final PaginatedRequestCallback cb){
		getFeed(app_context, "", feed_name, page, cb);
	}
	

	/**
	 * Parse an OpenWatch.net feed, saving its contents to the databasee
	 * 
	 * @param app_context
     */
    private static String last_feed_name;
    private static long last_request_time = System.currentTimeMillis();
    private static final long request_threshold = 250; // ignore duplicate requests separated by less than this many ms
	private static void getFeed(final Context app_context, String getParams, final String ext_feed_name, final int page, final PaginatedRequestCallback cb){
        if(last_feed_name != null && last_feed_name.compareTo(ext_feed_name) == 0){
            if(System.currentTimeMillis() - last_request_time < request_threshold){
                Log.i(TAG, String.format("Aborting request for feed %s, last completed %d ms ago", last_feed_name, last_request_time - System.currentTimeMillis()));
                if(cb != null)
                    cb.onFailure(page);
                return;
            }
        }
        last_feed_name = ext_feed_name;
        last_request_time = System.currentTimeMillis();
		final String METHOD = "getFeed";
		final String feed_name = ext_feed_name.trim().toLowerCase();
		
		JsonHttpResponseHandler get_handler = new JsonHttpResponseHandler(){
			@Override
    		public void onSuccess(JSONObject response){
				if(response.has("objects")){
					int internal_user_id = 0;
                    Log.i(TAG, String.format("got %s feed response: ",feed_name) );
					//Log.i(TAG, String.format("got %s feed response: %s ",feed_name, response.toString()) );
					if(feed_name.compareTo(OWFeedType.USER.toString().toLowerCase()) == 0){
						SharedPreferences profile = app_context.getSharedPreferences(Constants.PROFILE_PREFS, 0);
				        internal_user_id = profile.getInt(Constants.INTERNAL_USER_ID, 0);
					}
					
					ParseFeedTask parseTask = new ParseFeedTask(app_context, cb, feed_name, page, internal_user_id);
					parseTask.execute(response);
					
				}else try {
                    if(response.has("success") && response.getBoolean("success") == false){
                        if(response.has("code") && response.getInt("code") == 406){
                            // Auth cookie wrong / missing. Prompt user to re-login
                            Authentication.logOut(app_context);
                            OWUtils.goToLoginActivityWithMessage(app_context, app_context.getString(R.string.message_account_expired));
                        }
                    }else{
                        Log.e(TAG, "Feed response format unexpected" + response.toString());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

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

		http_client.get(endpoint + getParams, get_handler);

		Log.i(TAG, "getFeed: " + endpoint+getParams);
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
            // Benchmarking
            long start_time = System.currentTimeMillis();
            //Log.i("Benchmark", String.format("begin parsing %s feed", feed_name));
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
                //Benchmark
                long obj_start_time;
				// Now parse the new data and create the feed anew
				JSONArray json_array = response.getJSONArray("objects");
				JSONObject json_obj;
				for(int x=0; x<json_array.length(); x++){
					json_obj = json_array.getJSONObject(x);
					if(json_obj.has("type")){
						if(json_obj.getString("type").compareTo("video") == 0){
                            //obj_start_time = System.currentTimeMillis();
							OWVideoRecording.createOrUpdateOWRecordingWithJson(c, json_obj, OWFeed.getFeedFromString(c, feed_name), adapter);
                            //Log.i("Benchamrk", String.format("createdOrUpdated video in %d ms", System.currentTimeMillis() - obj_start_time));
                        }
						else if(json_obj.getString("type").compareTo("story") == 0){
							OWStory.createOrUpdateOWStoryWithJson(c, json_obj, OWFeed.getFeedFromString(c, feed_name));
                        }
						else if(json_obj.getString("type").compareTo("photo") == 0){
                            //obj_start_time = System.currentTimeMillis();
							OWPhoto.createOrUpdateOWPhotoWithJson(c, json_obj, OWFeed.getFeedFromString(c, feed_name));
                            //Log.i("Benchamrk", String.format("createdOrUpdated photo in %d ms", System.currentTimeMillis() - obj_start_time));
                        }
						else if(json_obj.getString("type").compareTo("audio") == 0){
                            //obj_start_time = System.currentTimeMillis();
							OWAudio.createOrUpdateOWAudioWithJson(c, json_obj, OWFeed.getFeedFromString(c, feed_name));
                            //Log.i("Benchamrk", String.format("createdOrUpdated audio in %d ms", System.currentTimeMillis() - obj_start_time));
                        }
						else if(json_obj.getString("type").compareTo("investigation") == 0){
                            //obj_start_time = System.currentTimeMillis();
							OWInvestigation.createOrUpdateOWInvestigationWithJson(c, json_obj, OWFeed.getFeedFromString(c, feed_name));
                            //Log.i("Benchamrk", String.format("createdOrUpdated inv in %d ms", System.currentTimeMillis() - obj_start_time));
                        }else if(json_obj.getString("type").compareTo("mission") == 0){
                            OWMission.createOrUpdateOWMissionWithJson(c, json_obj, OWFeed.getFeedFromString(c, feed_name));
                        }

					}
				}
				adapter.commitTransaction();
				Uri baseUri;
                /*
				if(feed_name.compareTo(OWFeedType.USER.toString().toLowerCase()) == 0){
					baseUri = OWContentProvider.getUserRecordingsUri(user_id);
				}else{
					baseUri = OWContentProvider.getFeedUri(feed_name);
				}
				*/
                baseUri = OWContentProvider.getFeedUri(feed_name);
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
                // Benchmark
                long end_time = System.currentTimeMillis();
                Log.i("Benchmark", String.format("finish parsing %s feed in %d ms", feed_name, end_time-start_time));
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
	
	public static void createOWServerObject(final Context app_context, final OWServerObjectInterface object, final RequestCallback cb){
		JsonHttpResponseHandler post_handler = new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(JSONObject response) {
				Log.i(TAG, "createObject response: " + response.toString());
				try {
					if(response.has("success") && response.getBoolean("success")){
						Log.i(TAG, "create object success!");
						object.updateWithJson(app_context, response.getJSONObject("object"));
                        if(cb != null)
                            cb.onSuccess();
						sendOWMobileGeneratedObjectMedia(app_context, object);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onFailure(Throwable e, String response) {
				Log.i(TAG, "createObject fail: " + response);
				e.printStackTrace();
                if(cb != null)
                    cb.onFailure();
			}

			@Override
			public void onFinish() {
				Log.i(TAG, "createObject finish");
			}

		};
		
		AsyncHttpClient http_client = HttpClient.setupAsyncHttpClient(app_context);
		Log.i(TAG, String.format("Commencing Create OWServerObject: %s with json: %s", endpointForContentType(object.getContentType(app_context)), object.toJsonObject(app_context) ));
		//Log.i(TAG, object.getType().toString());
		//Log.i(TAG, object.toJsonObject(app_context).toString());
		//Log.i(TAG, Utils.JSONObjectToStringEntity(object.toJsonObject(app_context)).toString());
		http_client.post(app_context, endpointForContentType(object.getContentType(app_context)) , Utils
				.JSONObjectToStringEntity(object.toJsonObject(app_context)), "application/json", post_handler);
	}

    public static void sendOWMobileGeneratedObjectMedia(final Context app_context, final OWServerObjectInterface object){
        sendOWMobileGeneratedObjectMedia(app_context, object, null);
    }

	public static void sendOWMobileGeneratedObjectMedia(final Context app_context, final OWServerObjectInterface object, final RequestCallback cb){
		new Thread(){
			public void run(){
				String file_response;
				try {
                    String filepath = object.getMediaFilepath(app_context);
                    if(filepath == null){
                        Log.e(TAG, String.format("Error, OWServerobject %d has null filepath", ((Model)object).getId()));
                        object.setSynced(app_context, true); // set object synced, because we have no hope of ever syncing it mobile file deleted
                        return;
                    }
					file_response = OWMediaRequests.ApacheFilePost(app_context, instanceEndpointForOWMediaObject(app_context, object), object.getMediaFilepath(app_context), "file_data");
					Log.i(TAG, "binary media sent! response: " + file_response);
                    if(file_response.contains("object")){
                        // BEGIN OWServerObject Sync Broadcast
                        Log.d("OWPhotoSync", "Broadcasting sync success message");
                        object.setSynced(app_context, true);
                        //if(object.getMediaType(app_context) == MEDIA_TYPE.PHOTO)
                            //(Photo)
                        Intent intent = new Intent(Constants.OW_SYNC_STATE_FILTER);
                        // You can also include some extra data.
                        intent.putExtra("status", 1);
                        intent.putExtra("child_model_id", ((Model)object).getId());
                        LocalBroadcastManager.getInstance(app_context).sendBroadcast(intent);
                        // END OWServerObject Sync Broadcast
                        if(cb != null)
                            cb.onSuccess();

                    }else{
                        if(cb != null)
                            cb.onFailure();
                    }
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NullPointerException e){
                    e.printStackTrace();
                }
				
			}
		}.start();

	}

    private static String instanceEndpointForOWUser(Context c, OWUser u){
        return Constants.OW_API_URL + "u/" + String.valueOf(u.server_id.get()) + "/";
    }
	
	private static String instanceEndpointForOWMediaObject(Context c, OWServerObjectInterface object){
		CONTENT_TYPE contentType = object.getContentType(c);
        if(contentType == CONTENT_TYPE.VIDEO || contentType == CONTENT_TYPE.AUDIO || contentType == CONTENT_TYPE.PHOTO)
			return Constants.OW_API_URL + Constants.API_ENDPOINT_BY_CONTENT_TYPE.get(contentType) + "/" + object.getUUID(c) +"/";
		else
			return Constants.OW_API_URL + Constants.API_ENDPOINT_BY_CONTENT_TYPE.get(contentType) + "/" + object.getServerId(c) +"/";
	}
	
	private static String endpointForContentType(CONTENT_TYPE type){
		return Constants.OW_API_URL + Constants.API_ENDPOINT_BY_CONTENT_TYPE.get(type) + "/";
	}

    public static void syncOWServerObject(final Context app_context,
                                          final OWServerObjectInterface object) {
        syncOWServerObject(app_context, object, false, null);
    }

    public static void syncOWServerObject(final Context app_context, final OWServerObjectInterface object, RequestCallback cb){
        syncOWServerObject(app_context, object, false, cb);
    }
	

	/**
	 * Fetch server recording data, check last_edited, and push update if
	 * necessary. if forcePushLocalData, push local data even if server's last-edited
     * is greater than local
	 * 
	 * @param app_context
	 */
	public static void syncOWServerObject(final Context app_context,
			 final OWServerObjectInterface object, final boolean forcePushLocalData, final RequestCallback cb) {
		final String METHOD = "syncRecording";
		final int model_id = ((Model)object).getId();
		JsonHttpResponseHandler get_handler = new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(JSONObject response) {
				Log.i(TAG, "getMeta response: " + response.toString());
				if (response.has("id")) {
					try {
						//JSONObject object_json = response
						//		.getJSONObject("object");
						// response was successful
						OWServerObject server_object = OWServerObject.objects(
								app_context, OWServerObject.class).get(
								model_id);
                        if(server_object == null)
                            Log.e(TAG, String.format("Could not locate OWServerObject with id: %d", model_id) );
						Date last_edited_remote = Constants.utc_formatter
								.parse(response.getString("last_edited"));
						Date last_edited_local = Constants.utc_formatter.parse(server_object
								.getLastEdited(app_context));
						Log.i(TAG,
								"Sync dates. Remote: "
										+ response
												.getString("last_edited")
										+ " local: "
										+ server_object.getLastEdited(app_context));
						// If local recording has no server_id, pull in fields
						// generated by server as there's no risk of conflict
						boolean doSave = false;
						// using the interface methods requires saving before
						// the object ref is lost
						if (response.has(Constants.OW_SERVER_ID)) {
							server_object.server_id.set(response
									.getInt(Constants.OW_SERVER_ID));
							// recording.setServerId(app_context,
							// recording_json.getInt(Constants.OW_SERVER_ID));
							doSave = true;
						}
						if (response.has(Constants.OW_FIRST_POSTED)) {
							server_object.first_posted.set(response
									.getString(Constants.OW_FIRST_POSTED));
							// recording.setFirstPosted(app_context,
							// recording_json.getString(Constants.OW_FIRST_POSTED));
							doSave = true;
						}
						if (response.has(Constants.OW_THUMB_URL)) {
							server_object.thumbnail_url.set(response
									.getString(Constants.OW_THUMB_URL));
							// recording.setThumbnailUrl(app_context,
							// recording_json.getString(Constants.OW_THUMB_URL));
							// recording.media_object.get(app_context).save(app_context);
							doSave = true;
						}
						if (doSave == true) {
							// media_obj.save(app_context);
							server_object.save(
									app_context);
						}
						
						OWServerObjectInterface child_obj = (OWServerObjectInterface) server_object.getChildObject(app_context);
						if(child_obj == null)
							Log.e(TAG, "getChildObject returned null for OWServerObject " + String.valueOf(server_object.getId()));

						if (!forcePushLocalData && last_edited_remote.after(last_edited_local)) {
							// copy remote data to local
							Log.i(TAG, "remote recording data is more recent");
							//server_object.updateWithJson(app_context, object_json);
							// call child object's updateWithJson -> calls OWServerObject's updateWithJson
							
							if(child_obj != null){
								child_obj.updateWithJson(app_context, response);
                                if(cb != null)
                                    cb.onSuccess();
                            }
							
						} else if ( forcePushLocalData || last_edited_remote.before(last_edited_local)) {
							// copy local to remote
							Log.i(TAG, "local recording data is more recent");
							if(child_obj != null){
								OWServiceRequests.editOWServerObject(app_context,
									child_obj, new JsonHttpResponseHandler() {
										@Override
										public void onSuccess(
												JSONObject response) {
											Log.i(TAG,
													"editRecording response: "
															+ response);

                                            if(cb != null)
                                                cb.onSuccess();
										}

										@Override
										public void onFailure(Throwable e,
												JSONObject response) {
											Log.i(TAG, "editRecording failed: "
													+ response);
											e.printStackTrace();

                                            if(cb != null)
                                                cb.onFailure();
										}
									});
							}
						}
					} catch (Exception e) {
						Log.e(TAG, METHOD + "failed to handle response");
						e.printStackTrace();
					}
				}else try {
                    if(response.has("success") && response.getBoolean("success") == false){
                        if(response.has("code") && response.getInt("code") == 406){
                            // Auth cookie wrong / missing. Prompt user to re-login
                            Authentication.logOut(app_context);
                            OWUtils.goToLoginActivityWithMessage(app_context, app_context.getString(R.string.message_account_expired));
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
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
		getOWServerObjectMeta(app_context, object, "", get_handler);

	}

	public static void getOWServerObjectMeta(Context app_context, OWServerObjectInterface object, String http_get_string, JsonHttpResponseHandler response_handler) {
		AsyncHttpClient http_client = HttpClient.setupAsyncHttpClient(app_context);
		if(http_get_string == null)
			http_get_string = "";
        if(object == null)
            return;
		Log.i(TAG, "Commencing Get Recording Meta: " + instanceEndpointForOWMediaObject(app_context, object) + http_get_string);
		http_client.get(instanceEndpointForOWMediaObject(app_context, object) + http_get_string, response_handler);
	}

	/**
	 * Post recording data to server. object should be a child OW object. i.e: NOT OWServerObject
	 * 
	 * @param app_context
	 * @param response_handler
	 */
	public static void editOWServerObject(Context app_context,
			OWServerObjectInterface object, JsonHttpResponseHandler response_handler) {
		AsyncHttpClient http_client = HttpClient.setupAsyncHttpClient(app_context);
		Log.i(TAG,
				"Commencing Edit Recording: "
						+ object.toJsonObject(app_context));
		http_client.post(app_context, instanceEndpointForOWMediaObject(app_context, object), Utils
				.JSONObjectToStringEntity(object.toJsonObject(app_context)),
				"application/json", response_handler);
	}

    public static void flagOWServerObjet(Context app_context, OWServerObjectInterface object){
        AsyncHttpClient http_client = HttpClient.setupAsyncHttpClient(app_context);
        JSONObject json = new JSONObject();
        try {
            json.put("flagged", true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        http_client.post(app_context, instanceEndpointForOWMediaObject(app_context, object), Utils.JSONObjectToStringEntity(json), "application/json", new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(JSONObject response) {
                Log.i(TAG, "flag object success " + response.toString());
            }
            @Override
            public void onFailure(Throwable e, String response) {
                Log.i(TAG, "flag object failure: " + response);

            }

            @Override
            public void onFinish() {
                Log.i(TAG, "flag object finish: ");

            }
        });
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

    public static void sendOWUserAvatar(final Context app_context, final OWUser user, final File image, final RequestCallback cb){
        new Thread(){
            public void run(){
                try {
                    String fileResponse = OWMediaRequests.ApacheFilePost(app_context, instanceEndpointForOWUser(app_context, user), image.getAbsolutePath(), "file_data");
                    if(fileResponse.contains("object")){
                        try {
                            JSONObject jsonResponse= new JSONObject(fileResponse);
                            OWUser.objects(app_context, OWUser.class).get(user.getId()).updateWithJson(app_context, jsonResponse);
                            if(cb != null)
                                cb.onSuccess();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }else{
                        if(cb != null)
                            cb.onFailure();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }.start();

    }

    /*
        Immediatley send user agent_applicant status, then re-send location on success
     */

    public static void syncOWUser(final Context app_context, final OWUser user, final RequestCallback cb){
        final AsyncHttpClient http_client = HttpClient.setupAsyncHttpClient(app_context);
        Log.i(TAG,
                "Commencing Edit User: "
                        + user.toJSON());

        final JsonHttpResponseHandler _cb = new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(JSONObject response) {
                user.updateWithJson(app_context, response);
                if(cb != null)
                    cb.onSuccess();
            }

            @Override
            public void onFailure(Throwable e, String response) {
                if(cb != null)
                    cb.onFailure();
            }

        };

        if(user.agent_applicant.get() == true){
            DeviceLocation.getLastKnownLocation(app_context, false, new DeviceLocation.GPSRequestCallback() {
                @Override
                public void onSuccess(Location result) {
                    user.lat.set(result.getLatitude());
                    user.lon.set(result.getLongitude());
                    user.save(app_context);
                    Log.i(TAG, String.format("Got user location %fx%f", result.getLatitude(), result.getLongitude()));
                    http_client.post(app_context, instanceEndpointForOWUser(app_context, user), Utils
                            .JSONObjectToStringEntity(user.toJSON()),
                            "application/json", _cb);
                }
            });

            http_client.post(app_context, instanceEndpointForOWUser(app_context, user), Utils
                    .JSONObjectToStringEntity(user.toJSON()),
                    "application/json", _cb);
        }

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
	public static void userLogin(Context app_context, StringEntity post_body,
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
	public static void userSignup(Context app_context, StringEntity post_body,
                                  JsonHttpResponseHandler response_handler) {

		AsyncHttpClient http_client = HttpClient.setupAsyncHttpClient(app_context);
		Log.i(TAG, "Commencing signup to: " + Constants.OW_API_URL
				+ Constants.OW_SIGNUP);
		http_client.post(app_context, Constants.OW_API_URL
				+ Constants.OW_SIGNUP, post_body, "application/json",
				response_handler);

	}

    /**
     * Create a new account with the OpenWatch servicee
     */
    public static void quickUserSignup(Context app_context, String email,
                                  JsonHttpResponseHandler response_handler) {

        JSONObject json = new JSONObject();
        StringEntity se = null;
        try {
            json.put("email", email);
            se = new StringEntity(json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error loading email to json");
            e.printStackTrace();
            return;
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Error creating StringEntity from json");
            e.printStackTrace();
            return;
        }

        AsyncHttpClient http_client = HttpClient.setupAsyncHttpClient(app_context);
        Log.i(TAG, "Commencing signup to: " + Constants.OW_API_URL
                + "quick_signup");
        http_client.post(app_context, Constants.OW_API_URL
                + "quick_signup", se, "application/json",
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

    public static void checkOWEmailAvailable(Context app_context, String email, JsonHttpResponseHandler response_handler){
        AsyncHttpClient http_client = HttpClient.setupAsyncHttpClient(app_context);
        Log.i(TAG, "Checking email available: " + email);
        http_client.get(app_context, Constants.OW_API_URL + "email_available?email=" + email, response_handler);
    }
}