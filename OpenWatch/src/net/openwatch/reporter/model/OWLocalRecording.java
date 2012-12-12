package net.openwatch.reporter.model;

import android.content.Context;

import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;
import com.orm.androrm.field.BooleanField;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.DoubleField;
import com.orm.androrm.field.IntegerField;
import com.orm.androrm.field.OneToManyField;

public class OWLocalRecording extends Model {
	
	public CharField title = new CharField();
	public CharField description = new CharField();
	public CharField thumb_url = new CharField();
	public IntegerField user_id = new IntegerField();
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
	
	
	public CharField filepath;
	public BooleanField hq_synced;
	public BooleanField lq_synced;
	
	public OneToManyField<OWLocalRecording, OWLocalRecordingSegment> segments;
	
	public OWLocalRecording(){
		super();
		filepath = new CharField();
		hq_synced = new BooleanField();
		lq_synced = new BooleanField();
		
		segments = new OneToManyField<OWLocalRecording, OWLocalRecordingSegment> (OWLocalRecording.class, OWLocalRecordingSegment.class);
	}
	
	public void initializeRecording(Context c, String uuid, double lat, double lon){
		this.uuid.set(uuid);
		this.begin_lat.set(lat);
		this.begin_lon.set(lon);
		this.save(c);
	}
	
	public void addSegment(Context c, String filepath, String filename){
		OWLocalRecordingSegment segment = new OWLocalRecordingSegment();
		segment.filepath.set(filepath);
		segment.filename.set(filename);
		segment.local_recording.set(this);
		segment.save(c);
	}

}
