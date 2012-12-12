package net.openwatch.reporter.model;

import com.orm.androrm.Model;
import com.orm.androrm.field.BooleanField;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.ForeignKeyField;

public class OWLocalRecordingSegment extends Model{

	public CharField filepath;
	public CharField filename;
	public BooleanField uploaded;
	public ForeignKeyField<OWLocalRecording> local_recording;
	
	public OWLocalRecordingSegment(){
		super();
		
		filepath = new CharField();
		filename = new CharField();
		uploaded = new BooleanField();
		local_recording = new ForeignKeyField<OWLocalRecording>(OWLocalRecording.class);
	}

}
