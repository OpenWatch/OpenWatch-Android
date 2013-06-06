package org.ale.openwatch.model;

import android.content.Context;
import android.util.Log;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;
import com.orm.androrm.field.BooleanField;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.ForeignKeyField;
import com.orm.androrm.field.OneToManyField;

import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.contentprovider.OWContentProvider;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class OWLocalVideoRecording extends Model implements OWServerObjectInterface{
	private static final String TAG = "OWLocalRecording";

	public CharField recording_end_time = new CharField();

	public CharField filepath = new CharField();
	public CharField hq_filepath = new CharField();
	public BooleanField hq_synced = new BooleanField();
	public BooleanField lq_synced = new BooleanField();

	public OneToManyField<OWLocalVideoRecording, OWLocalVideoRecordingSegment> segments = new OneToManyField<OWLocalVideoRecording, OWLocalVideoRecordingSegment>(
			OWLocalVideoRecording.class, OWLocalVideoRecordingSegment.class);
	//public OneToManyField<OWLocalRecording, OWRecording> recording = new OneToManyField<OWLocalRecording, OWRecording>(OWLocalRecording.class, OWRecording.class);
	public ForeignKeyField<OWVideoRecording> recording = new ForeignKeyField<OWVideoRecording>(OWVideoRecording.class);
	//public ManyToManyField<OWLocalRecording, OWRecordingTag> tags = new ManyToManyField<OWLocalRecording, OWRecordingTag> (OWLocalRecording.class, OWRecordingTag.class);
	
	public OWLocalVideoRecording() {
		super();
	}
	
	public OWLocalVideoRecording(Context c){
		super();
		this.save(c);
		
		OWVideoRecording recording = new OWVideoRecording(c); // creates OWMediaObject
		recording.creation_time.set(Constants.utc_formatter.format(new Date()));
		recording.local.set(this.getId());
		recording.media_object.get(c).local_video_recording.set(getId());
		recording.media_object.get(c).save(c);
		recording.save(c);
		this.recording.set(recording);
		this.save(c);
		Log.i(TAG, String.format("New OWLocalVideoRecording. local_id: %d, recording_id: %d, media_obj_id: %d ", this.getId(), this.recording.get(c).getId(), this.recording.get(c).media_object.get(c).getId()) );
	}

	public int addSegment(Context c, String filepath, String filename) {
		OWLocalVideoRecordingSegment segment = new OWLocalVideoRecordingSegment();
		segment.filepath.set(filepath);
		segment.filename.set(filename);
		segment.local_recording.set(this);
		segment.save(c);
		this.segments.add(segment);
		return this.getId();
	}
	
	public boolean areSegmentsSynced(Context c){
		QuerySet<OWLocalVideoRecordingSegment> segments = this.segments.get(c, this);
		for(OWLocalVideoRecordingSegment segment : segments){
			if(!segment.uploaded.get()){
				Log.i(TAG, "Segment " + segment.getId() + " is not synced");
				return false;
			}
		}
		return true;
	}
	
	public JSONArray segmentsToJSONArray(Context c){
		JSONArray result = new JSONArray();
		for(OWLocalVideoRecordingSegment segment : this.segments.get(c, this)){
			result.put(segment.filename.get());
		}
		return result;
	}

    // deprecated
	public JSONObject toOWMediaServerJSON(Context c){
		JSONObject result = new JSONObject();
		try {
			if(recording.get(c).begin_lat.get() != null){
				result.put(Constants.OW_START_LOC, new JSONObject().put(Constants.OW_LAT, recording.get(c).begin_lat.get())
															   	   .put(Constants.OW_LON, recording.get(c).begin_lon.get()));
			}
			if(recording.get(c).end_lat.get() != null){
				result.put(Constants.OW_END_LOC, new JSONObject().put(Constants.OW_LAT, recording.get(c).end_lat.get())
					   										     .put(Constants.OW_LON, recording.get(c).end_lon.get()));
			}
			if(recording.get(c).getTitle(c) != null && recording.get(c).getTitle(c).compareTo("") != 0){
				result.put(Constants.OW_MEDIA_TITLE, recording.get(c).getTitle(c));
			}
			if(recording.get(c).getDescription(c) != null && recording.get(c).getDescription(c).compareTo("") != 0){
				result.put(Constants.OW_DESCRIPTION, recording.get(c).getDescription(c));
			}
			//if(this.segments.get(c, this).count() > 0){
			result.put(Constants.OW_ALL_FILES, segmentsToJSONArray(c));
			//}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return result;
	}

    @Override
    public String getTitle(Context c) {
        return null;
    }

    @Override
    public void setTitle(Context c, String title) {

    }

    @Override
    public String getFirstPosted(Context c) {
        return null;
    }

    @Override
    public void setFirstPosted(Context c, String first_posted) {

    }

    @Override
    public String getLastEdited(Context c) {
        return null;
    }

    @Override
    public void setLastEdited(Context c, String last_edited) {

    }

    @Override
    public void setViews(Context c, int views) {

    }

    @Override
    public Integer getViews(Context c) {
        return null;
    }

    @Override
    public void setActions(Context c, int actions) {

    }

    @Override
    public Integer getActions(Context c) {
        return null;
    }

    @Override
    public void setServerId(Context c, int server_id) {

    }

    @Override
    public Integer getServerId(Context c) {
        return null;
    }

    @Override
    public void setDescription(Context c, String description) {

    }

    @Override
    public String getDescription(Context c) {
        return null;
    }

    @Override
    public void setThumbnailUrl(Context c, String url) {

    }

    @Override
    public String getThumbnailUrl(Context c) {
        return null;
    }

    @Override
    public OWUser getUser(Context c) {
        return null;
    }

    @Override
    public void setUser(Context c, OWUser user) {

    }

    @Override
    public String getUUID(Context c) {
        if(recording.get(c) != null)
            return recording.get(c).getUUID(c);
        return null;
    }

    @Override
    public void setUUID(Context c, String uuid) {

    }

    @Override
    public void setLat(Context c, double lat) {

    }

    @Override
    public double getLat(Context c) {
        return 0;
    }

    @Override
    public void setLon(Context c, double lon) {

    }

    @Override
    public double getLon(Context c) {
        return 0;
    }

    @Override
    public Constants.MEDIA_TYPE getMediaType(Context c) {
        return Constants.MEDIA_TYPE.VIDEO;
    }

    @Override
    public Constants.CONTENT_TYPE getContentType(Context c) {
        return Constants.CONTENT_TYPE.MEDIA_OBJECT;
    }

    @Override
    public void setMediaFilepath(Context c, String filepath) {
        hq_filepath.set(filepath);
    }

    @Override
    public String getMediaFilepath(Context c) {
        return hq_filepath.get();
    }

    @Override
    public QuerySet<OWTag> getTags(Context c) {
        return null;
    }

    @Override
    public void resetTags(Context c) {

    }

    @Override
    public void addTag(Context c, OWTag tag) {

    }

    @Override
    public void addToFeed(Context c, OWFeed feed) {

    }

    @Override
    public void updateWithJson(Context c, JSONObject json) {
        if(json.has("media_url")){
                setSynced(c, true);
        }
        if(recording.get(c) != null)
            recording.get(c).updateWithJson(c, json);
    }

    @Override
    public JSONObject toJsonObject(Context c) {
        if(recording.get(c) != null)
            return recording.get(c).toJsonObject(c);
        return null;
    }

    /**
	 * Save and notify content provider
	 */
	@Override
	public boolean save(Context context) {
		if(recording.get(context) != null){
			recording.get(context).media_object.get(context).setLastEdited(context, Constants.utc_formatter.format(new Date()) );
			context.getContentResolver().notifyChange(
					OWContentProvider.getLocalRecordingUri(this.getId()), null);
			context.getContentResolver().notifyChange(
					OWContentProvider.getMediaObjectUri(recording.get(context).media_object.get(context).getId()), null);
		}
		return super.save(context);
	}

    @Override
    public void setSynced(Context c, boolean isSynced) {
        hq_synced.set(true);
        save(c);
    }

}
