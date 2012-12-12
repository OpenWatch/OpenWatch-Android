package net.openwatch.reporter.model;

import android.content.Context;

import com.orm.androrm.field.BooleanField;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.OneToManyField;

public class OWLocalRecording extends OWRecording {
	
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
