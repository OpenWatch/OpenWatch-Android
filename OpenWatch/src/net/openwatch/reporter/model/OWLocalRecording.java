package net.openwatch.reporter.model;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.contentprovider.OWContentProvider;
import net.openwatch.reporter.http.OWServiceRequests;
import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.orm.androrm.Filter;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;
import com.orm.androrm.field.BooleanField;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.DoubleField;
import com.orm.androrm.field.ForeignKeyField;
import com.orm.androrm.field.IntegerField;
import com.orm.androrm.field.ManyToManyField;
import com.orm.androrm.field.OneToManyField;

public class OWLocalRecording extends Model {
	private static final String TAG = "OWLocalRecording";

	public CharField title;
	public CharField description;
	public CharField thumb_url;
	public IntegerField user_id;
	public CharField username;
	// username is queried often by ListViews, so it makes sense to duplicate
	// info for performance
	public CharField creation_time;
	public CharField recording_end_time;
	public CharField first_posted;
	public CharField uuid;
	public CharField last_edited;
	public IntegerField server_id;
	public CharField video_url;
	public DoubleField begin_lat;
	public DoubleField begin_lon;
	public DoubleField end_lat;
	public DoubleField end_lon;

	public CharField filepath;
	public CharField hq_filepath;
	public BooleanField hq_synced;
	public BooleanField lq_synced;

	public OneToManyField<OWLocalRecording, OWLocalRecordingSegment> segments;
	public ManyToManyField<OWLocalRecording, OWRecordingTag> tags;
	public ForeignKeyField<OWRecording> recording;

	public OWLocalRecording() {
		super();

		title = new CharField();
		description = new CharField();
		user_id = new IntegerField();
		username = new CharField();
		thumb_url = new CharField();
		creation_time = new CharField();
		creation_time.set(Constants.sdf.format(new Date()));
		recording_end_time = new CharField();
		first_posted = new CharField();
		uuid = new CharField();
		last_edited = new CharField();
		server_id = new IntegerField();
		video_url = new CharField();
		begin_lat = new DoubleField();
		begin_lon = new DoubleField();
		end_lat = new DoubleField();
		end_lon = new DoubleField();

		filepath = new CharField();
		hq_filepath = new CharField();
		hq_synced = new BooleanField();
		lq_synced = new BooleanField();

		segments = new OneToManyField<OWLocalRecording, OWLocalRecordingSegment>(
				OWLocalRecording.class, OWLocalRecordingSegment.class);
		tags = new ManyToManyField<OWLocalRecording, OWRecordingTag>(
				OWLocalRecording.class, OWRecordingTag.class);
		recording = new ForeignKeyField<OWRecording>(OWRecording.class);
	}

	public void initializeRecording(Context c, String title, String uuid,
			double lat, double lon) {
		this.title.set(title);
		this.uuid.set(uuid);
		this.begin_lat.set(lat);
		this.begin_lon.set(lon);
		this.save(c);
	}

	public void addSegment(Context c, String filepath, String filename) {
		OWLocalRecordingSegment segment = new OWLocalRecordingSegment();
		segment.filepath.set(filepath);
		segment.filename.set(filename);
		segment.local_recording.set(this);
		segment.save(c);
		this.segments.add(segment);
	}

	@Override
	public boolean save(Context context) {
		this.last_edited.set(Constants.sdf.format(new Date()));
		// notify the ContentProvider that the dataset has changed
		context.getContentResolver().notifyChange(
				OWContentProvider.getLocalRecordingUri(this.getId()), null);
		
		OWServiceRequests.syncRecording(context, this);
		
		return super.save(context);
	}

	public boolean hasTag(Context context, String tag_name) {
		Filter filter = new Filter();
		filter.is(DBConstants.TAG_TABLE_NAME, tag_name);
		return (this.tags.get(context, this).filter(filter).count() > 0);
	}
	
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
			this.save(app_context);
			Log.i(TAG, "updateWIthJson. server_id: " + String.valueOf(this.server_id.get()));
		} catch (JSONException e) {
			Log.e(TAG, "failed to update model with json");
			e.printStackTrace();
		}
	}

	public StringEntity toJson(Context app_context) {
		HashMap<String, String> params = new HashMap<String, String>();
		if (this.title.get() != null)
			params.put(Constants.OW_TITE, this.title.get());
		if (this.description.get() != null)
			params.put(Constants.OW_DESCRIPTION, this.description.get());
		if (this.last_edited.get() != null)
			params.put(Constants.OW_EDIT_TIME, this.last_edited.get());
		if (this.begin_lat.get() != null)
			params.put(Constants.OW_START_LAT, this.begin_lat.get().toString());
		if (this.begin_lon.get() != null)
			params.put(Constants.OW_START_LON, this.begin_lon.get().toString());
		if (this.end_lat.get() != null)
			params.put(Constants.OW_START_LAT, this.end_lat.get().toString());
		if (this.end_lon.get() != null)
			params.put(Constants.OW_START_LON, this.end_lon.get().toString());
		
		ArrayList<String> tags = new ArrayList<String>();
		QuerySet<OWRecordingTag> qs = this.tags.get(app_context, this);
		for (OWRecordingTag tag : qs) {
			tags.add(tag.name.get());
		}
		Gson gson = new Gson();
		StringEntity se = null;
		try {
			se = new StringEntity(gson.toJson(tags));
		} catch (UnsupportedEncodingException e) {
			Log.e("OWLocalRecording", "Failed make json from tags");
		}

		params.put(Constants.OW_TAGS, se.toString());

		se = null;

		try {
			se = new StringEntity(gson.toJson(params));
		} catch (UnsupportedEncodingException e1) {
			Log.e("OWLocalRecording", "Failed to make json from hashmap");
			e1.printStackTrace();
		}

		return se;
	}

}
