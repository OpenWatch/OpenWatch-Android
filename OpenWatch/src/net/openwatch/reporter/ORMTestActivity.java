package net.openwatch.reporter;

import java.util.ArrayList;
import java.util.List;

import com.orm.androrm.DatabaseAdapter;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;
import com.orm.androrm.Where;

import net.openwatch.reporter.model.OWLocalRecording;
import net.openwatch.reporter.model.OWRecording;
import net.openwatch.reporter.model.OWRecordingTag;
import android.os.Bundle;
import android.app.Activity;
import android.content.ContentValues;
import android.util.Log;
import android.view.Menu;

public class ORMTestActivity extends Activity {
	private static final String TAG = "ORMTestActivity";

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ormtest);
		/*
		List<Class<? extends Model>> models = new ArrayList<Class<? extends Model>>();
		models.add(OWRecording.class);
		models.add(OWRecordingTag.class);
		models.add(OWLocalRecording.class);
		
		DatabaseAdapter.setDatabaseName("OpenWatchDB");
		DatabaseAdapter adapter = new DatabaseAdapter(getApplicationContext());
		adapter.setModels(models);
		
		OWRecordingTag sample_tag = new OWRecordingTag();
		sample_tag.name.set("test name");
		sample_tag.is_featured.set(true);
		sample_tag.save(getApplicationContext());
		
		OWRecording sample_recording = new OWRecording();
		
		sample_recording.tags.add(sample_tag);
		sample_recording.title.set("test title");
		sample_recording.save(this);
		
		OWLocalRecording sample_local = new OWLocalRecording();
		sample_local.title.set("local parent title");
		sample_local.child_title.set("local child title");
		sample_local.tags.add(sample_tag);
		sample_local.save(this);
		
		Log.i(TAG, "models saved");
		
		QuerySet<OWRecording> qs = OWRecording.objects(this, OWRecording.class).all();
		for(OWRecording ow : qs){
			Log.i("QuerySet", ow.title.toString() + " tags: " +  ow.tags.get(this, ow));
		}
		
		// Test manual db actions
		ContentValues cv = new ContentValues();
		cv.put("name", "test name2");
		adapter.doInsertOrUpdate("owtag", cv, new Where().and("name","test name"));
		
		QuerySet<OWRecordingTag> qs2= OWRecordingTag.objects(this, OWRecordingTag.class).all();
		for(OWRecordingTag tag : qs2){
			Log.i("QuerySet", tag.name.toString() + " id: " + String.valueOf(tag.getId()));
		}
		
		//adapter.query("SELECT * from owrecording JOIN owlocalrecording);
		adapter.close();
		*/
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_ormtest, menu);
		return true;
	}

}
