package net.openwatch.reporter.model;

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

}
