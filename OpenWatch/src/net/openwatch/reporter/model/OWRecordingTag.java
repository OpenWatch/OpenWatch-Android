package net.openwatch.reporter.model;

import net.openwatch.reporter.contentprovider.OWContentProvider;
import android.content.Context;

import com.orm.androrm.Model;
import com.orm.androrm.field.BooleanField;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.IntegerField;
import com.orm.androrm.field.ManyToManyField;

public class OWRecordingTag extends Model{
	
	public CharField name;
	public BooleanField is_featured;
	public IntegerField server_id;
	
	public ManyToManyField<OWRecordingTag, OWRecording> recordings;
	
	public OWRecordingTag(){
		super();
		name = new CharField();
		is_featured = new BooleanField();
		server_id = new IntegerField();
		
		recordings = new ManyToManyField<OWRecordingTag, OWRecording>(OWRecordingTag.class, OWRecording.class);
	}
	
	@Override
	public boolean save(Context context) {
		// notify the ContentProvider that the dataset has changed
		context.getContentResolver().notifyChange(OWContentProvider.getTagUri(this.getId()), null);
		return super.save(context);
	}

}
