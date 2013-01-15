package net.openwatch.reporter.model;

import com.orm.androrm.Model;
import com.orm.androrm.field.BooleanField;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.ForeignKeyField;

public class OWLocalVideoRecordingSegment extends Model{

	public CharField filepath;
	public CharField filename;
	public BooleanField uploaded;
	public ForeignKeyField<OWLocalVideoRecording> local_recording;
	
	public OWLocalVideoRecordingSegment(){
		super();
		
		filepath = new CharField();
		filename = new CharField();
		uploaded = new BooleanField();
		local_recording = new ForeignKeyField<OWLocalVideoRecording>(OWLocalVideoRecording.class);
	}

}
