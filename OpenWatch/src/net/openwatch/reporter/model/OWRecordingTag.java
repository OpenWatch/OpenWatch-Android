package net.openwatch.reporter.model;

import java.util.List;

import com.orm.androrm.Model;
import com.orm.androrm.field.BooleanField;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.ManyToManyField;

public class OWRecordingTag extends Model{
	
	public CharField name;
	public BooleanField is_featured;
	
	public ManyToManyField<OWRecordingTag, OWRecording> recordings;
	
	public OWRecordingTag(){
		super();
		name = new CharField();
		is_featured = new BooleanField();
		
		recordings = new ManyToManyField<OWRecordingTag, OWRecording>(OWRecordingTag.class, OWRecording.class);
	}

}
