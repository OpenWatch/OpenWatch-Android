package net.openwatch.reporter.model;

import com.orm.androrm.Model;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.IntegerField;
import com.orm.androrm.field.ManyToManyField;
import com.orm.androrm.field.OneToManyField;

public class OWUser extends Model{
	
	public CharField username;
	public IntegerField server_id;
	
	public OneToManyField<OWUser, OWRecording> recordings;
	public ManyToManyField<OWUser, OWRecordingTag> tags;
	
	public OWUser(){
		super();
		username = new CharField();
		server_id = new IntegerField();
		
		recordings = new OneToManyField<OWUser, OWRecording>(OWUser.class, OWRecording.class);
		tags = new ManyToManyField<OWUser, OWRecordingTag>(OWUser.class, OWRecordingTag.class);
		
	}

}
