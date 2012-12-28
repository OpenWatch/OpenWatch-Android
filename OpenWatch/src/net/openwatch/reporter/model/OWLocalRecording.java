package net.openwatch.reporter.model;

import java.util.Date;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.contentprovider.OWContentProvider;
import android.content.Context;

import com.orm.androrm.Filter;
import com.orm.androrm.Model;
import com.orm.androrm.field.BooleanField;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.ForeignKeyField;
import com.orm.androrm.field.IntegerField;
import com.orm.androrm.field.OneToManyField;

public class OWLocalRecording extends Model {
	private static final String TAG = "OWLocalRecording";

	public CharField recording_end_time = new CharField();

	public CharField filepath = new CharField();
	public CharField hq_filepath = new CharField();
	public BooleanField hq_synced = new BooleanField();
	public BooleanField lq_synced = new BooleanField();
	public IntegerField recording_id = new IntegerField();

	public OneToManyField<OWLocalRecording, OWLocalRecordingSegment> segments = new OneToManyField<OWLocalRecording, OWLocalRecordingSegment>(
			OWLocalRecording.class, OWLocalRecordingSegment.class);
	//public OneToManyField<OWLocalRecording, OWRecording> recording = new OneToManyField<OWLocalRecording, OWRecording>(OWLocalRecording.class, OWRecording.class);
	//public ForeignKeyField<OWRecording> recording = new ForeignKeyField<OWRecording>(OWRecording.class);
	//public ManyToManyField<OWLocalRecording, OWRecordingTag> tags = new ManyToManyField<OWLocalRecording, OWRecordingTag> (OWLocalRecording.class, OWRecordingTag.class);
	
	public OWLocalRecording() {
		super();
	}
	
	public OWLocalRecording(Context c){
		super();
		this.save(c);
		OWRecording recording = new OWRecording();
		recording.creation_time.set(Constants.sdf.format(new Date()));
		recording.local.set(this.getId());
		recording.save(c);
		this.recording_id.set(recording.getId());
		this.save(c);
	}

	public void addSegment(Context c, String filepath, String filename) {
		OWLocalRecordingSegment segment = new OWLocalRecordingSegment();
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
			OWRecording.objects(context, OWRecording.class).get(this.recording_id.get()).last_edited.set(Constants.sdf.format(new Date()));
		context.getContentResolver().notifyChange(
				OWContentProvider.getLocalRecordingUri(this.getId()), null);
		return super.save(context);
	}
}
