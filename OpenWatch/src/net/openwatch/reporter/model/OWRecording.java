package net.openwatch.reporter.model;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;
import com.orm.androrm.DatabaseAdapter;
import com.orm.androrm.Filter;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.DoubleField;
import com.orm.androrm.field.ForeignKeyField;
import com.orm.androrm.field.IntegerField;
import com.orm.androrm.field.ManyToManyField;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.Constants.OWFeedType;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.contentprovider.OWContentProvider;
import net.openwatch.reporter.http.OWServiceRequests;


public class OWRecording extends Model{
	private static final String TAG = "OWRecording";
	
	public CharField title = new CharField();
	public CharField description = new CharField();
	public CharField thumb_url = new CharField();
	public CharField username = new CharField(); 
	// username is queried often by ListViews, so it makes sense to duplicate info for performance 
	public CharField creation_time = new CharField();
	public CharField first_posted = new CharField();
	public CharField uuid = new CharField();
	public CharField last_edited = new CharField();
	public IntegerField server_id = new IntegerField();
	public CharField video_url = new CharField();
	public DoubleField begin_lat = new DoubleField();
	public DoubleField begin_lon = new DoubleField();
	public DoubleField end_lat = new DoubleField();
	public DoubleField end_lon = new DoubleField();
	public IntegerField views = new IntegerField();
	public IntegerField actions = new IntegerField();
	
	// For internal use
	
	public ForeignKeyField<OWLocalRecording> local; // = new ForeignKeyField<OWLocalRecording>(OWLocalRecording.class);
	public ManyToManyField<OWRecording, OWRecordingTag> tags = new ManyToManyField<OWRecording, OWRecordingTag> (OWRecording.class, OWRecordingTag.class);
	public ForeignKeyField<OWUser> user = new ForeignKeyField<OWUser>(OWUser.class);
	
	public OWRecording(){
		super();
		local = new ForeignKeyField<OWLocalRecording>(OWLocalRecording.class);
	}
	
	public OWRecording(Context c){
		super();
		local = new ForeignKeyField<OWLocalRecording>(OWLocalRecording.class);
		this.save(c);
		OWLocalRecording local = new OWLocalRecording();
		local.recording_id.set(this.getId());
		local.save(c);
		
		this.creation_time.set(Constants.sdf.format(new Date()));
		this.local.set(local);
		this.save(c);
	}
	
	public void initializeRecording(Context c, String title, String uuid,
			double lat, double lon) {
		this.title.set(title);
		this.uuid.set(uuid);
		this.begin_lat.set(lat);
		this.begin_lon.set(lon);
		this.save(c);
	}
	
	public void initializeRecordingAsLocal(Context c, String title, String uuid,
			double lat, double lon) {
		this.title.set(title);
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
			

			OWRecording existing_rec = null;
			// TESTING
			Log.i(TAG, "Total OWRecordings: " + String.valueOf(OWRecording.objects(app_context, OWRecording.class).count()) );
			Log.i(TAG, "json len: " + String.valueOf(json_array.length()) );
			for(int x=0; x<json_array.length();x++){	
				createOrUpdateOWRecordingWithJson(app_context, json_array.getJSONObject(x) );
				// add recording to feed if not null
				if(feed != null){
					feed.recordings.add(existing_rec);
				}
			} // end json recording for loop
			adapter.commitTransaction();
			// TESTING
			Log.i(TAG, "Total OWRecordings: " + String.valueOf(OWRecording.objects(app_context, OWRecording.class).count()) );
		} catch (JSONException e) {
			Log.e(TAG, " Error parsing feed's recording array");
			e.printStackTrace();
		}
	}
	
	public static OWRecording createOrUpdateOWRecordingWithJson(Context app_context, JSONObject json_obj) throws JSONException{
		//Log.i(TAG, "Evaluating json recording: " + json_obj.toString());
		OWRecording existing_rec = null;
		Filter filter = new Filter();
		filter.is(DBConstants.RECORDINGS_TABLE_UUID, json_obj.getString(Constants.OW_UUID));
		QuerySet<OWRecording>existing_recs = OWRecording.objects(app_context, OWRecording.class).filter(filter);
		Log.i(TAG, String.format("Found %d recordings with uuid %s", existing_recs.count(), json_obj.getString(Constants.OW_UUID)));
		for(OWRecording rec : existing_recs){
			existing_rec = rec;
			Log.i(TAG, "found existing recording for uuid: " + String.valueOf( json_obj.getString(Constants.OW_UUID)));
			break;
		}
		if(existing_rec == null){
			Log.i(TAG, "creating new recording");
			existing_rec = new OWRecording();
		}
		
		existing_rec.updateWithJson(app_context, json_obj);

		return existing_rec;
	}
	
	public boolean hasTag(Context context, String tag_name) {
		Filter filter = new Filter();
		filter.is(DBConstants.TAG_TABLE_NAME, tag_name);
		return (this.tags.get(context, this).filter(filter).count() > 0);
	}
	
	public void removeTag(Context app_context, OWRecordingTag tag){
		ArrayList<OWRecordingTag> tags = (ArrayList<OWRecordingTag>) this.tags.get(app_context, this).all().toList();
		tags.remove(tag);
		this.tags.reset();
		this.tags.addAll(tags);
		this.save(app_context);
	}
	
	/**
	 * Update a recording with JSON formatted as the /api/recording/<uuid> response
	 * @param app_context
	 * @param json
	 */
	public void updateWithJson(Context app_context, JSONObject json){
		try {
			if(json.has("title"))
				this.title.set(json.getString("title"));
			if(json.has("description"))
				this.description.set(json.getString("description"));
			if(json.has("last_edited"))
				this.last_edited.set(json.getString("last_edited"));
			if(json.has("first_posted"))
				this.first_posted.set(json.getString("first_posted"));
			if(json.has("reported_creation_time"))
				this.creation_time.set(json.getString("reported_creation_time"));
			if(json.has("video_url"))
				this.video_url.set(json.getString("video_url"));
			if(json.has("thumbnail_url"))
				this.thumb_url.set(json.getString("thumbnail_url"));
			if(json.has("id"))
				this.server_id.set(json.getInt("id"));
			
			
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
			if(json.has(Constants.OW_VIEWS))
				this.views.set(json.getInt(Constants.OW_VIEWS));
			if(json.has(Constants.OW_TITLE))
				this.title.set(json.getString(Constants.OW_TITLE));
			if(json.has(Constants.OW_SERVER_ID))
				this.server_id.set(json.getInt(Constants.OW_SERVER_ID));
			if(json.has(Constants.OW_THUMB_URL) && json.getString(Constants.OW_THUMB_URL).compareTo(Constants.OW_NO_VALUE)!= 0)
				this.thumb_url.set(json.getString(Constants.OW_THUMB_URL));
			if(json.has(Constants.OW_LAST_EDITED))
				this.last_edited.set(json.getString(Constants.OW_LAST_EDITED));
			if(json.has(Constants.OW_ACTIONS))
				this.actions.set(json.getInt(Constants.OW_ACTIONS));
			
			if(json.has(Constants.OW_USER)){
				JSONObject json_user = null;
				OWUser user = null;
				json_user = json.getJSONObject(Constants.OW_USER);
				Filter filter = new Filter();
				filter.is(DBConstants.USER_SERVER_ID, json_user.getInt(Constants.OW_SERVER_ID));
				QuerySet<OWUser> existing_users = OWUser.objects(app_context, OWUser.class).filter(filter);
				for(OWUser existing_user : existing_users){
					user = existing_user;
					break;
				}
				if(user == null){
					user = new OWUser();
					user.username.set(json_user.getString(Constants.OW_USERNAME));
					user.server_id.set(json_user.getInt(Constants.OW_SERVER_ID));
					user.save(app_context);
				}
				this.username.set(user.username.get());
				this.user.set(user.getId());
			} // end if user

			
			if(json.has(Constants.OW_TAGS)){
				this.tags.reset();
				JSONArray tag_array =  json.getJSONArray("tags");
				Filter filter;
				for(int x=0;x<tag_array.length();x++){
					filter = new Filter();
					filter.is("name", tag_array.getString(x).toString());
					QuerySet<OWRecordingTag> tags = OWRecordingTag.objects(app_context, OWRecordingTag.class).filter(filter);
					if(tags.count() > 0){ // add existing tag
						for(OWRecordingTag tag : tags){
							this.tags.add(tag);
							break;
						}
					} else{ // add a new tag
						OWRecordingTag new_tag = new OWRecordingTag();
						new_tag.name.set(tag_array.getString(x).toString());
						new_tag.save(app_context);
						this.tags.add(new_tag);
					}
				}
			} // end tags update
			this.save(app_context);
			Log.i(TAG, "updateWIthJson. server_id: " + String.valueOf(this.server_id.get()));
		} catch (JSONException e) {
			Log.e(TAG, "failed to update model with json");
			e.printStackTrace();
		}
	}

	public StringEntity toJson(Context app_context) {
		JSONObject json_obj = new JSONObject();
		try{
			if (this.title.get() != null)
				json_obj.put(Constants.OW_TITLE, this.title.get());
			if (this.description.get() != null)
				json_obj.put(Constants.OW_DESCRIPTION, this.description.get());
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
			
			
			
			QuerySet<OWRecordingTag> qs = this.tags.get(app_context, this);
			JSONArray tags = new JSONArray();
			
			for (OWRecordingTag tag : qs) {
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
	
	/*
	recording: {
		reported_creation_time: "2012-12-10 21:20:30",
		uuid: "96f9be13-6159-4c66-ad39-1cbc4eeea315",
		tags: [ ],
		first_posted: "2012-12-10 21:20:35",
		user: 4,
		last_edited: "2012-12-10 21:20:35",
		id: 1
	}
	 */

}
