package net.openwatch.reporter.model;

import java.util.Date;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.contentprovider.OWContentProvider;
import android.content.Context;

import com.orm.androrm.Model;
import com.orm.androrm.field.BooleanField;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.DoubleField;
import com.orm.androrm.field.ForeignKeyField;
import com.orm.androrm.field.IntegerField;
import com.orm.androrm.field.ManyToManyField;
import com.orm.androrm.field.OneToManyField;

public class OWLocalRecording extends Model {
	
	public CharField title;
	public CharField description;
	public CharField thumb_url;
	public IntegerField user_id;
	public CharField username; 
	// username is queried often by ListViews, so it makes sense to duplicate info for performance 
	public CharField creation_time;
	public CharField first_posted;
	public CharField uuid;
	public CharField last_edited;
	public IntegerField server_id;
	public CharField video_url;
	public DoubleField begin_lat;
	public DoubleField begin_lon;
	public DoubleField end_lat;
	public DoubleField end_lon;
	
	
	public CharField filepath;
	public CharField hq_filepath;
	public BooleanField hq_synced;
	public BooleanField lq_synced;
	
	public OneToManyField<OWLocalRecording, OWLocalRecordingSegment> segments;
	public ManyToManyField<OWLocalRecording, OWRecordingTag> tags;
	public ForeignKeyField<OWRecording> recording;
	
	public OWLocalRecording(){
		super();
		
		title = new CharField();
		description = new CharField();
		user_id = new IntegerField();
		username = new CharField(); 
		thumb_url = new CharField();
		creation_time = new CharField();
		creation_time.set(Constants.sdf.format(new Date()));
		first_posted = new CharField();
		uuid = new CharField();
		last_edited = new CharField();
		server_id = new IntegerField();
		video_url = new CharField();
		begin_lat = new DoubleField();
		begin_lon = new DoubleField();
		end_lat = new DoubleField();
		end_lon = new DoubleField();
		
		filepath = new CharField();
		hq_filepath = new CharField();
		hq_synced = new BooleanField();
		lq_synced = new BooleanField();
		
		segments = new OneToManyField<OWLocalRecording, OWLocalRecordingSegment> (OWLocalRecording.class, OWLocalRecordingSegment.class);
		tags = new ManyToManyField<OWLocalRecording, OWRecordingTag> (OWLocalRecording.class, OWRecordingTag.class);
		recording = new ForeignKeyField<OWRecording> (OWRecording.class);
	}
	
	public void initializeRecording(Context c, String title, String uuid, double lat, double lon){
		this.title.set(title);
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
	
	@Override
	public boolean save(Context context) {
		// notify the ContentProvider that the dataset has changed
		context.getContentResolver().notifyChange(OWContentProvider.getLocalRecordingUri(this.getId()), null);
		return super.save(context);
	}

}
