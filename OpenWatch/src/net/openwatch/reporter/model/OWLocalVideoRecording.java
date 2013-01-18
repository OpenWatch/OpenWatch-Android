package net.openwatch.reporter.model;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.contentprovider.OWContentProvider;
import android.content.Context;
import android.util.Log;

import com.orm.androrm.Model;
import com.orm.androrm.field.BooleanField;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.ForeignKeyField;
import com.orm.androrm.field.OneToManyField;

public class OWLocalVideoRecording extends Model {
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
		recording.creation_time.set(Constants.sdf.format(new Date()));
		recording.local.set(this.getId());
		recording.media_object.get(c).local_video_recording.set(getId());
		recording.media_object.get(c).save(c);
		recording.save(c);
		this.recording.set(recording);
		this.save(c);
		Log.i(TAG, String.format("New OWLocalVideoRecording. local_id: %d, recording_id: %d, media_obj_id: %d ", this.getId(), this.recording.get(c).getId(), this.recording.get(c).media_object.get(c).getId()) );
	}

	public void addSegment(Context c, String filepath, String filename) {
		OWLocalVideoRecordingSegment segment = new OWLocalVideoRecordingSegment();
		segment.filepath.set(filepath);
		segment.filename.set(filename);
		segment.local_recording.set(this);
		segment.save(c);
		this.segments.add(segment);
	}
	
	public JSONArray segmentsToJSONArray(Context c){
		JSONArray result = new JSONArray();
		for(OWLocalVideoRecordingSegment segment : this.segments.get(c, this)){
			result.put(segment.filename.get());
		}
		return result;
	}
	
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
			if(this.segments.get(c, this).count() > 0){
				result.put(Constants.OW_ALL_FILES, segmentsToJSONArray(c));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Save and notify content provider
	 */
	@Override
	public boolean save(Context context) {
		if(recording.get(context) != null){
			recording.get(context).media_object.get(context).setLastEdited(context, Constants.sdf.format(new Date()) );
			context.getContentResolver().notifyChange(
					OWContentProvider.getLocalRecordingUri(this.getId()), null);
			context.getContentResolver().notifyChange(
					OWContentProvider.getMediaObjectUri(recording.get(context).media_object.get(context).getId()), null);
		}
		return super.save(context);
	}
	
}
