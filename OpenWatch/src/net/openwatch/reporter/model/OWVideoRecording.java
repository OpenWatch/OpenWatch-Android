package net.openwatch.reporter.model;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.orm.androrm.DatabaseAdapter;
import com.orm.androrm.Filter;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.DoubleField;
import com.orm.androrm.field.ForeignKeyField;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.contentprovider.OWContentProvider;
import net.openwatch.reporter.http.OWServiceRequests;


public class OWVideoRecording extends Model implements OWMediaObjectInterface{
	private static final String TAG = "OWRecording";

	// Specific to OWRecording
	public CharField creation_time = new CharField();
	public CharField first_posted = new CharField();
	public CharField uuid = new CharField();
	public CharField last_edited = new CharField();
	public CharField video_url = new CharField();
	public DoubleField begin_lat = new DoubleField();
	public DoubleField begin_lon = new DoubleField();
	public DoubleField end_lat = new DoubleField();
	public DoubleField end_lon = new DoubleField();
	
	public ForeignKeyField<OWMediaObject> media_object = new ForeignKeyField<OWMediaObject> ( OWMediaObject.class );

	// For internal use
	public ForeignKeyField<OWLocalVideoRecording> local = new ForeignKeyField<OWLocalVideoRecording>(OWLocalVideoRecording.class);
	
	public OWVideoRecording(){
		super();
	}
	
	public OWVideoRecording(Context c){
		super();
		
		this.save(c);
		OWMediaObject media_object = new OWMediaObject();
		media_object.video_recording.set(getId());
		media_object.save(c);
		this.media_object.set(media_object);
		this.save(c);
		
		//TODO: Do not create local recording here
		OWLocalVideoRecording local = new OWLocalVideoRecording();
		local.recording_id.set(this.getId());
		local.save(c);
		
		this.creation_time.set(Constants.sdf.format(new Date()));
		this.local.set(local);
		
		this.save(c);
	}
	

	public void initializeRecording(Context c, String title, String uuid,
			double lat, double lon) {
		this.media_object.get(c).title.set(title);
		this.uuid.set(uuid);
		this.begin_lat.set(lat);
		this.begin_lon.set(lon);
		this.save(c);
	}
	
	public void initializeRecordingAsLocal(Context c, String title, String uuid,
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
	public void saveAndSync(Context context){
		// notify the ContentProvider that the dataset has changed
		save(context);
		OWServiceRequests.syncRecording(context, this);
		return;
	}
	
	
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
	

	public static OWVideoRecording createOrUpdateOWRecordingWithJson(Context app_context, JSONObject json_obj, OWFeed feed) throws JSONException{
		OWVideoRecording rec = createOrUpdateOWRecordingWithJson(app_context, json_obj);
		// add recording to feed if not null
		if(feed != null){
			Log.i(TAG, String.format("Adding recording %s to feed %s", rec.uuid.get(), feed.name.get()));
			rec.addToFeed(app_context, feed);
			rec.save(app_context);
			//Log.i(TAG, String.format("Feed %s now has %d items", feed.name.get(), feed.video_recordings.get(app_context, feed).count()) );
		}
		
		return rec;
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
			if(existing_rec != null)
				Log.i(TAG, "found existing video recording for id: " + String.valueOf( json_obj.getString(Constants.OW_SERVER_ID)));
		}
		
		if(existing_rec == null){
			Log.i(TAG, "creating new recording");
			existing_rec = new OWVideoRecording(app_context);
		}
		
		existing_rec.updateWithJson(app_context, json_obj);

		return existing_rec;
	}
	
	public boolean hasTag(Context context, String tag_name) {
		Filter filter = new Filter();
		filter.is(DBConstants.TAG_TABLE_NAME, tag_name);
		return (this.media_object.get(context).tags.get(context, this.media_object.get(context)).filter(filter).count() > 0);
	}
	
	public void removeTag(Context app_context, OWTag tag){
		ArrayList<OWTag> tags = (ArrayList<OWTag>) this.media_object.get(app_context).tags.get(app_context, this.media_object.get(app_context)).all().toList();
		tags.remove(tag);
		this.media_object.get(app_context).resetTags(app_context);
		this.media_object.get(app_context).tags.addAll(tags);
		this.save(app_context);
	}
	
	/**
	 * Update a recording with JSON formatted as the /api/recording/<uuid> response
	 * @param app_context
	 * @param json
	 */
	public void updateWithJson(Context app_context, JSONObject json){
		this.media_object.get(app_context).updateWithJson(app_context, json);
		try {
			if(json.has(Constants.OW_LAST_EDITED))
				this.last_edited.set(json.getString(Constants.OW_LAST_EDITED));
			if(json.has(Constants.OW_FIRST_POSTED))
				this.first_posted.set(json.getString(Constants.OW_FIRST_POSTED));
			if(json.has(Constants.OW_CREATION_TIME))
				this.creation_time.set(json.getString(Constants.OW_CREATION_TIME));
			if(json.has(Constants.OW_VIDEO_URL))
				this.video_url.set(json.getString(Constants.OW_VIDEO_URL));
			
			
			if(json.has(Constants.OW_START_LOCATION)){
				this.begin_lat.set(json.getJSONObject(Constants.OW_START_LOCATION).getDouble(Constants.OW_LAT));
				this.begin_lon.set(json.getJSONObject(Constants.OW_START_LOCATION).getDouble(Constants.OW_LON));
			}
			if(json.has(Constants.OW_END_LOCATION)){
				this.end_lat.set(json.getJSONObject(Constants.OW_END_LOCATION).getDouble(Constants.OW_LAT));
				this.end_lon.set(json.getJSONObject(Constants.OW_END_LOCATION).getDouble(Constants.OW_LON));
			}
			
			if(json.has(Constants.OW_UUID))
				this.uuid.set(json.getString(Constants.OW_UUID));
			if(json.has(Constants.OW_LAST_EDITED))
				this.last_edited.set(json.getString(Constants.OW_LAST_EDITED));

			this.save(app_context);
			Log.i(TAG, "updateWIthJson. server_id: " + String.valueOf(this.getServerId(app_context)) );
		} catch (JSONException e) {
			Log.e(TAG, "failed to update model with json");
			e.printStackTrace();
		}
	}

	public StringEntity toJson(Context app_context) {
		JSONObject json_obj = new JSONObject();
		try{
			if (this.media_object.get(app_context).title.get() != null)
				json_obj.put(Constants.OW_TITLE, this.media_object.get(app_context).title.get());
			if (this.media_object.get(app_context).description.get() != null)
				json_obj.put(Constants.OW_DESCRIPTION, this.media_object.get(app_context).description.get());
			if (this.last_edited.get() != null)
				json_obj.put(Constants.OW_EDIT_TIME, this.last_edited.get());
			if (this.begin_lat.get() != null)
				json_obj.put(Constants.OW_START_LAT, this.begin_lat.get().toString());
			if (this.begin_lon.get() != null)
				json_obj.put(Constants.OW_START_LON, this.begin_lon.get().toString());
			if (this.end_lat.get() != null)
				json_obj.put(Constants.OW_START_LAT, this.end_lat.get().toString());
			if (this.end_lon.get() != null)
				json_obj.put(Constants.OW_START_LON, this.end_lon.get().toString());
			
			
			
			QuerySet<OWTag> qs = this.getTags(app_context);
			JSONArray tags = new JSONArray();
			
			for (OWTag tag : qs) {
				tags.put(tag.name.get());
			}
			json_obj.put(Constants.OW_TAGS, tags);
			StringEntity se = null;
			try {
				se = new StringEntity(json_obj.toString());
				Log.i(TAG, "recordingToJson: " + json_obj.toString());
				return se;
			} catch (UnsupportedEncodingException e1) {
				Log.e(TAG, "json->stringentity failed");
				e1.printStackTrace();
			}
		}catch(JSONException e){
			Log.e(TAG, "Error serializing recording to json");
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public boolean save(Context context) {
		// notify the ContentProvider that the dataset has changed
		context.getContentResolver().notifyChange(OWContentProvider.getRemoteRecordingUri(this.getId()), null);
		this.last_edited.set(Constants.sdf.format(new Date()));
		return super.save(context);
	}


	@Override
	public void setTitle(Context c, String title) {
		this.media_object.get(c).setTitle(c, title);
	}

	@Override
	public void setViews(Context c, int views) {
		this.media_object.get(c).setViews(c, views);
	}

	@Override
	public void setActions(Context c, int actions) {
		this.media_object.get(c).setActions(c, actions);
	}

	@Override
	public void setServerId(Context c, int server_id) {
		this.media_object.get(c).setServerId(c, server_id);
	}

	@Override
	public void setDescription(Context c, String description) {
		this.media_object.get(c).setDescription(c, description);
	}

	@Override
	public void setThumbnailUrl(Context c, String url) {
		this.media_object.get(c).setThumbnailUrl(c, url);
	}

	@Override
	public void setUser(Context c, OWUser user) {
		this.media_object.get(c).setUser(c, user);
	}

	@Override
	public void resetTags(Context c) {
		this.media_object.get(c).resetTags(c);
	}

	@Override
	public void addTag(Context c, OWTag tag) {
		this.media_object.get(c).addTag(c, tag);
	}

	@Override
	public String getTitle(Context c) {
		return this.media_object.get(c).getTitle(c);
	}

	@Override
	public String getDescription(Context c) {
		return this.media_object.get(c).description.get();
	}

	@Override
	public QuerySet<OWTag> getTags(Context c) {
		return this.media_object.get(c).getTags(c); 
	}
	
	@Override
	public int getViews(Context c) {
		return this.media_object.get(c).getViews(c);
	}

	@Override
	public int getActions(Context c) {
		return this.media_object.get(c).getActions(c);
	}

	@Override
	public int getServerId(Context c) {
		return this.media_object.get(c).getServerId(c);
	}

	@Override
	public String getThumbnailUrl(Context c) {
		return this.media_object.get(c).getThumbnailUrl(c);
	}

	@Override
	public OWUser getUser(Context c) {
		return this.media_object.get(c).getUser(c);
	}

	@Override
	public void addToFeed(Context c, OWFeed feed) {
		this.media_object.get(c).addToFeed(c, feed);
	}

}
