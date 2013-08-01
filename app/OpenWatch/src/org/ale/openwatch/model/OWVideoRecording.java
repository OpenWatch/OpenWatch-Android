package org.ale.openwatch.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import com.orm.androrm.*;
import com.orm.androrm.field.BooleanField;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.DoubleField;
import com.orm.androrm.field.ForeignKeyField;

import com.orm.androrm.migration.Migrator;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.constants.DBConstants;
import org.ale.openwatch.constants.Constants.CONTENT_TYPE;
import org.ale.openwatch.contentprovider.OWContentProvider;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;


public class OWVideoRecording extends Model implements OWServerObjectInterface{
	private static final String TAG = "OWRecording";

	// Specific to OWRecording
	public CharField creation_time = new CharField();
	public CharField uuid = new CharField();
	public DoubleField begin_lat = new DoubleField();
	public DoubleField begin_lon = new DoubleField();
	public DoubleField end_lat = new DoubleField();
	public DoubleField end_lon = new DoubleField();
    public CharField media_url = new CharField();
	
	public ForeignKeyField<OWServerObject> media_object = new ForeignKeyField<OWServerObject> ( OWServerObject.class );

	// For internal use
	public ForeignKeyField<OWLocalVideoRecording> local = new ForeignKeyField<OWLocalVideoRecording>(OWLocalVideoRecording.class);
	
	public OWVideoRecording(){
		super();
	}
	
	public OWVideoRecording(Context c){
		super();
		save(c);
		OWServerObject media_object = new OWServerObject();
		media_object.video_recording.set(this);
		media_object.save(c);
		this.media_object.set(media_object);
		save(c);
		
		//TODO: Do not create local recording here
		//OWLocalVideoRecording local = new OWLocalVideoRecording();
		//local.recording_id.set(getId());
		//local.save(c);
		
		creation_time.set(Constants.utc_formatter.format(new Date()));
		//this.local.set(local);
		
		save(c);
	}
	

	public void initializeRecording(Context c, String title, String uuid,
			double lat, double lon) {
		this.media_object.get(c).setTitle(c, title);
		this.uuid.set(uuid);
		this.begin_lat.set(lat);
		this.begin_lon.set(lon);
		this.save(c);
	}
	
	
	/**
	 * Save, sync with OpenWatch.net, and notify content provider.
	 * Called from LocalRecordingInfoFragment
	 * @param context
	 * @return
	 */
	/*
	public void saveAndSync(Context context){
		// notify the ContentProvider that the dataset has changed
		save(context);
		OWServiceRequests.syncOWMediaObject(context, this);
		return;
	}
	*/
	
	/**
	 * Creates recordings with basic information provided in an OpenWatch feed response
	 * @param app_context
	 * @param json_array
	 * @param feed
	 */
	public static void createOWRecordingsFromJSONArray(Context app_context, JSONArray json_array, OWFeed feed){
		String TAG = "createOWRecordingsFromJSONArray";
		try {
			DatabaseAdapter adapter = DatabaseAdapter.getInstance(app_context);
			adapter.beginTransaction();
			

			OWVideoRecording existing_rec = null;
			// TESTING
			Log.i(TAG, "Total OWRecordings: " + String.valueOf(OWVideoRecording.objects(app_context, OWVideoRecording.class).count()) );
			Log.i(TAG, "json len: " + String.valueOf(json_array.length()) );
			for(int x=0; x<json_array.length();x++){	
				existing_rec = createOrUpdateOWRecordingWithJson(app_context, json_array.getJSONObject(x) );
				// add recording to feed if not null
				if(feed != null){
					Log.i(TAG, String.format("Adding recording %s to feed %s", existing_rec.uuid.get(), feed.name.get()));
					existing_rec.addToFeed(app_context, feed);
					existing_rec.save(app_context);
					Log.i(TAG, String.format("Added video %s to feed %s",existing_rec.media_object.get(app_context).title.get(), feed.name.get() ));
				}
			} // end json recording for loop
			adapter.commitTransaction();
			// TESTING
			Log.i(TAG, "Total OWRecordings: " + String.valueOf(OWVideoRecording.objects(app_context, OWVideoRecording.class).count()) );
		} catch (JSONException e) {
			Log.e(TAG, " Error parsing feed's recording array");
			e.printStackTrace();
		}
	}
	

	public static OWVideoRecording createOrUpdateOWRecordingWithJson(Context app_context, JSONObject json_obj, OWFeed feed, DatabaseAdapter adapter) throws JSONException{
		//OWVideoRecording rec = createOrUpdateOWRecordingWithJson(app_context, json_obj);
        // Parse video json for insert into video table
        int feedId = feed.getId();
        Where where = new Where();
        where.and(DBConstants.RECORDINGS_TABLE_UUID, json_obj.getString(Constants.OW_UUID));
        ContentValues values = new ContentValues();
        if(json_obj.has(Constants.OW_CREATION_TIME))
            values.put(DBConstants.RECORDINGS_TABLE_CREATION_TIME, json_obj.getString(Constants.OW_CREATION_TIME));

        if(json_obj.has("media_url")){
            values.put("media_url", json_obj.getString("media_url"));
        }

        if(json_obj.has("start_lat") && json_obj.has("start_lon")){
            values.put("begin_lat", json_obj.getString("start_lat"));
            values.put("begin_lon", json_obj.getString("start_lon"));
            //Log.i(TAG, String.format("got start_location. Lat: %f Lon: %f", begin_lat.get(), begin_lon.get()));
        }
        if(json_obj.has("end_lat") && json_obj.has("end_lon") ){
            values.put("end_lat", json_obj.getString("end_lat"));
            values.put("end_lon", json_obj.getString("end_lon"));
            //Log.i(TAG, String.format("got end_location. Lat: %f Lon: %f", end_lat.get(), end_lon.get()));
        }

        if(json_obj.has(Constants.OW_UUID))
            values.put("uuid", json_obj.getString("uuid"));

        // Insert video row
        adapter.doInsertOrUpdate(DBConstants.RECORDINGS_TABLENAME, values, where);

        int videoId = 0;
        Cursor cursor = adapter.query(String.format("SELECT _id FROM %s WHERE uuid =\"%s\"", DBConstants.RECORDINGS_TABLENAME, json_obj.getString(Constants.OW_UUID)));
        if(cursor != null && cursor.moveToFirst()){
            videoId = cursor.getInt(0);
        }
        cursor.close();

        // Create OWServerObject record
        json_obj.put("video_recording", videoId);
        OWServerObject.createOrUpdateWithJson(app_context, json_obj, feed, adapter);

		// add recording to feed if not null
        cursor = adapter.query(String.format("SELECT _id FROM %s WHERE server_id =\"%s\" and video_recording IS NOT NULL", DBConstants.MEDIA_OBJECT_TABLENAME, json_obj.getString(Constants.OW_SERVER_ID)));
        if(cursor != null && cursor.moveToFirst()){
            int serverObjectId = cursor.getInt(0);
            if(feed != null){
                // Associate OWServerObject with feed
                values = new ContentValues();
                values.put("owserverobject", serverObjectId);
                values.put("owfeed", feedId);
                Where feedWhere = new Where();
                feedWhere.and("owserverobject", serverObjectId);
                feedWhere.and("owfeed", feedId);
                adapter.doInsertOrUpdate("owfeed_owserverobject", values, feedWhere);
            }
            // Associate video with OWServerObject
            values = new ContentValues();
            values.put("media_object", serverObjectId);
            adapter.doInsertOrUpdate(DBConstants.RECORDINGS_TABLENAME, values, where);
        }
        cursor.close();

        /*
		if(feed != null){
			//Log.i(TAG, String.format("Adding recording %s to feed %s", rec.uuid.get(), feed.name.get()));
			rec.addToFeed(app_context, feed);
			rec.save(app_context);
			//Log.i(TAG, String.format("Feed %s now has %d items", feed.name.get(), feed.video_recordings.get(app_context, feed).count()) );
		}
		*/
		return null;
		//return rec;
	}
	

	public static OWVideoRecording createOrUpdateOWRecordingWithJson(Context app_context, JSONObject json_obj) throws JSONException{
		//Log.i(TAG, "Evaluating json recording: " + json_obj.toString());
		OWVideoRecording existing_rec = null;
		
		DatabaseAdapter dba = DatabaseAdapter.getInstance(app_context);
		String query_string = String.format("SELECT %s FROM %s WHERE %s = \"%s\"", DBConstants.ID, DBConstants.RECORDINGS_TABLENAME, DBConstants.RECORDINGS_TABLE_UUID, json_obj.getString(Constants.OW_UUID));
		Cursor result = dba.open().query(query_string);
		if(result != null && result.moveToFirst()){
			int recording_id = result.getInt(0);
			if(recording_id != 0)
				existing_rec = OWVideoRecording.objects(app_context, OWVideoRecording.class).get(recording_id);
			if(existing_rec != null){
				//Log.i(TAG, "found existing video recording for id: " + String.valueOf( json_obj.getString(Constants.OW_SERVER_ID)));
            }
		}
		
		if(existing_rec == null){
			//Log.i(TAG, "creating new recording");
			existing_rec = new OWVideoRecording(app_context);
		}
		
		existing_rec.updateWithJson(app_context, json_obj);

		return existing_rec;
	}
	
	public boolean hasTag(Context context, String tag_name) {
		Filter filter = new Filter();
		filter.is(DBConstants.TAG_TABLE_NAME, tag_name);
		return (media_object.get(context).tags.get(context, media_object.get(context)).filter(filter).count() > 0);
	}
	
	public void removeTag(Context app_context, OWTag tag){
		ArrayList<OWTag> tags = (ArrayList<OWTag>) media_object.get(app_context).tags.get(app_context, media_object.get(app_context)).all().toList();
		tags.remove(tag);
		media_object.get(app_context).resetTags(app_context);
		media_object.get(app_context).tags.addAll(tags);
		save(app_context);
	}
	
	/**
	 * Update a recording with JSON formatted as the /api/recording/<uuid> response
	 * Calls save
	 * @param app_context
	 * @param json
	 */
	public void updateWithJson(Context app_context, JSONObject json){
		if(app_context == null)
			Log.i(TAG, "app_context is null!");
		if(json == null)
			Log.i(TAG, "json is null");
		if(media_object.get(app_context) == null)
			Log.i(TAG, "media_object is null");
		media_object.get(app_context).updateWithJson(app_context, json);
		try {
			if(json.has(Constants.OW_CREATION_TIME))
				creation_time.set(json.getString(Constants.OW_CREATION_TIME));

            if(json.has("media_url")){
                media_url.set(json.getString("media_url"));
            }
			
			if(json.has("start_lat") && json.has("start_lon")){
				begin_lat.set(json.getDouble("start_lat"));
				begin_lon.set(json.getDouble("start_lon"));
                //Log.i(TAG, String.format("got start_location. Lat: %f Lon: %f", begin_lat.get(), begin_lon.get()));
			}
			if(json.has("end_lat") && json.has("end_lon") ){
				end_lat.set(json.getDouble("end_lat"));
				end_lon.set(json.getDouble("end_lon"));
                //Log.i(TAG, String.format("got end_location. Lat: %f Lon: %f", end_lat.get(), end_lon.get()));
			}
			
			if(json.has(Constants.OW_UUID))
				uuid.set(json.getString(Constants.OW_UUID));

			save(app_context);
			//Log.i(TAG, "updateWIthJson. server_id: " + String.valueOf(getServerId(app_context)) );
		} catch (JSONException e) {
			Log.e(TAG, "failed to update model with json");
			e.printStackTrace();
		}
	}
	
	@Override
	public JSONObject toJsonObject(Context app_context) {
		JSONObject json_obj = media_object.get(app_context).toJsonObject(app_context);
		try{
			if (begin_lat.get() != null)
				json_obj.put(Constants.OW_START_LAT, begin_lat.get().toString());
			if (begin_lon.get() != null)
				json_obj.put(Constants.OW_START_LON, begin_lon.get().toString());
			if (end_lat.get() != null)
				json_obj.put(Constants.OW_END_LAT, end_lat.get().toString());
			if (end_lon.get() != null)
				json_obj.put(Constants.OW_END_LON, end_lon.get().toString());

            Log.i(TAG, "VideoToJson: " + json_obj.toString());
		}catch(JSONException e){
			Log.e(TAG, "Error serializing recording to json");
			e.printStackTrace();
		}
		return json_obj;
	}
	
	@Override
	public boolean save(Context context) {
		// notify the ContentProvider that the dataset has changed
		context.getContentResolver().notifyChange(OWContentProvider.getRemoteRecordingUri(getId()), null);
		if(media_object.get(context) != null){ // this is called once in <init> to get db id before medi_object created
			setLastEdited(context, Constants.utc_formatter.format(new Date()));
            media_object.get(context).save(context);
        }
		return super.save(context);
	}

    @Override
    public void setSynced(Context c, boolean isSynced) {
        if(local.get(c) != null){
            local.get(c).hq_synced.set(isSynced);
            local.get(c).lq_synced.set(isSynced);
            save(c);
        }
    }


    @Override
	public void setTitle(Context c, String title) {
		media_object.get(c).setTitle(c, title);
	}

	@Override
	public void setViews(Context c, int views) {
		media_object.get(c).setViews(c, views);
	}

	@Override
	public void setActions(Context c, int actions) {
		media_object.get(c).setActions(c, actions);
	}

	@Override
	public void setServerId(Context c, int server_id) {
		media_object.get(c).setServerId(c, server_id);
	}

	@Override
	public void setDescription(Context c, String description) {
		media_object.get(c).setDescription(c, description);
	}

	@Override
	public void setThumbnailUrl(Context c, String url) {
		media_object.get(c).setThumbnailUrl(c, url);
	}

	@Override
	public void setUser(Context c, OWUser user) {
		media_object.get(c).setUser(c, user);
	}

	@Override
	public void resetTags(Context c) {
		media_object.get(c).resetTags(c);
	}

	@Override
	public void addTag(Context c, OWTag tag) {
		media_object.get(c).addTag(c, tag);
	}

	@Override
	public String getTitle(Context c) {
		return media_object.get(c).getTitle(c);
	}

	@Override
	public String getDescription(Context c) {
		return media_object.get(c).description.get();
	}

	@Override
	public QuerySet<OWTag> getTags(Context c) {
		return media_object.get(c).getTags(c); 
	}
	
	@Override
	public Integer getViews(Context c) {
		return media_object.get(c).getViews(c);
	}

	@Override
	public Integer getActions(Context c) {
		return media_object.get(c).getActions(c);
	}

	@Override
	public Integer getServerId(Context c) {
		return media_object.get(c).getServerId(c);
	}

	@Override
	public String getThumbnailUrl(Context c) {
		return media_object.get(c).getThumbnailUrl(c);
	}

	@Override
	public OWUser getUser(Context c) {
		return media_object.get(c).getUser(c);
	}

	@Override
	public void addToFeed(Context c, OWFeed feed) {
		media_object.get(c).addToFeed(c, feed);
	}

	@Override
	public String getFirstPosted(Context c) {
		return media_object.get(c).getFirstPosted(c);
	}

	@Override
	public void setFirstPosted(Context c, String first_posted) {
		media_object.get(c).setFirstPosted(c, first_posted);
	}

	@Override
	public String getLastEdited(Context c) {
		return media_object.get(c).getLastEdited(c);
	}

	@Override
	public void setLastEdited(Context c, String last_edited) {
		media_object.get(c).setLastEdited(c, last_edited);
	}
	
	public static String getUrlFromId(int server_id){
		return Constants.OW_URL + Constants.OW_RECORDING_VIEW + String.valueOf(server_id);	
	}

	@Override
	public String getUUID(Context c) {
		return uuid.get();
	}

	@Override
	public void setUUID(Context c, String uuid) {
		this.uuid.set(uuid);
	}

	@Override
	public void setLat(Context c, double lat) {
		// TODO Auto-generated method stub
	}

	@Override
	public double getLat(Context c) {
		return this.end_lat.get();
	}

	@Override
	public void setLon(Context c, double lon) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double getLon(Context c) {
		return this.end_lon.get();
	}


	@Override
	public void setMediaFilepath(Context c, String filepath) {
		this.local.get(c).hq_filepath.set(filepath);
	}

	@Override
	public String getMediaFilepath(Context c) {
        if(this.local.get(c) != null)
		    return this.local.get(c).hq_filepath.get();
        else
            return this.media_url.get();
	}

	@Override
	public CONTENT_TYPE getContentType(Context c) {
		return CONTENT_TYPE.VIDEO;
	}

}
