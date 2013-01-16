package net.openwatch.reporter.model;

import java.util.Date;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.contentprovider.OWContentProvider;
import android.content.Context;

import com.orm.androrm.Model;
import com.orm.androrm.field.BooleanField;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.IntegerField;
import com.orm.androrm.field.OneToManyField;

public class OWLocalVideoRecording extends Model {
	private static final String TAG = "OWLocalRecording";

	public CharField recording_end_time = new CharField();

	public CharField filepath = new CharField();
	public CharField hq_filepath = new CharField();
	public BooleanField hq_synced = new BooleanField();
	public BooleanField lq_synced = new BooleanField();
	public IntegerField recording_id = new IntegerField();

	public OneToManyField<OWLocalVideoRecording, OWLocalVideoRecordingSegment> segments = new OneToManyField<OWLocalVideoRecording, OWLocalVideoRecordingSegment>(
			OWLocalVideoRecording.class, OWLocalVideoRecordingSegment.class);
	//public OneToManyField<OWLocalRecording, OWRecording> recording = new OneToManyField<OWLocalRecording, OWRecording>(OWLocalRecording.class, OWRecording.class);
	//public ForeignKeyField<OWRecording> recording = new ForeignKeyField<OWRecording>(OWRecording.class);
	//public ManyToManyField<OWLocalRecording, OWRecordingTag> tags = new ManyToManyField<OWLocalRecording, OWRecordingTag> (OWLocalRecording.class, OWRecordingTag.class);
	
	public OWLocalVideoRecording() {
		super();
	}
	
	public OWLocalVideoRecording(Context c){
		super();
		this.save(c);
		
		OWVideoRecording recording = new OWVideoRecording(c);
		recording.creation_time.set(Constants.sdf.format(new Date()));
		recording.local.set(this.getId());
		recording.media_object.get(c).local_video_recording.set(getId());
		recording.save(c);
		this.recording_id.set(recording.getId());
		this.save(c);
	}

	public void addSegment(Context c, String filepath, String filename) {
		OWLocalVideoRecordingSegment segment = new OWLocalVideoRecordingSegment();
		segment.filepath.set(filepath);
		segment.filename.set(filename);
		segment.local_recording.set(this);
		segment.save(c);
		this.segments.add(segment);
	}

	/**
	 * Save and notify content provider
	 */
	@Override
	public boolean save(Context context) {
		if(this.recording_id.get() != 0)
			OWVideoRecording.objects(context, OWVideoRecording.class).get(this.recording_id.get()).media_object.get(context).setLastEdited(context, Constants.sdf.format(new Date()) );
		context.getContentResolver().notifyChange(
				OWContentProvider.getLocalRecordingUri(this.getId()), null);
		return super.save(context);
	}
}
