package net.openwatch.reporter.database;

import java.util.ArrayList;
import java.util.List;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.model.OWLocalRecording;
import net.openwatch.reporter.model.OWLocalRecordingSegment;
import net.openwatch.reporter.model.OWRecording;
import net.openwatch.reporter.model.OWRecordingTag;
import net.openwatch.reporter.model.OWUser;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.CursorJoiner.Result;
import android.util.Log;

import com.orm.androrm.DatabaseAdapter;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;
import com.orm.androrm.Where;

public class DatabaseManager {
	
	private static final String TAG = "DatabaseManager";
	
	private static final boolean DEBUG = false;
	
	public static void setupDB(Context c){
		final Context context = c.getApplicationContext();
		
		List<Class<? extends Model>> models = new ArrayList<Class<? extends Model>>();
		models.add(OWRecording.class);
		models.add(OWRecordingTag.class);
		models.add(OWLocalRecording.class);
		models.add(OWLocalRecordingSegment.class);
		models.add(OWUser.class);
		
		pointToDB();
		DatabaseAdapter adapter = DatabaseAdapter.getInstance(context);
		adapter.setModels(models); // setModels calls open and close
		
		if(DEBUG){
			testDB(c);
			/*
			adapter.open();
			
			Cursor result = adapter.query("SELECT * from owrecording JOIN owlocalrecording on uuid");
			adapter.close();
			if(result != null){
				while(result.moveToNext()){
					Log.i(TAG,"all recordings QuerySet: " + result.toString());
				}
			}
			*/
		}
		
		new Thread(new Runnable(){
			@Override
			public void run(){
				SharedPreferences profile = context.getSharedPreferences(Constants.PROFILE_PREFS, 0);
				SharedPreferences.Editor editor = profile.edit();
				editor.putBoolean(Constants.DB_READY, true);
				editor.commit();
				Log.i(TAG, "db ready");
				
			}
		}).start();
		
	}
	
	public static void pointToDB(){
		DatabaseAdapter.setDatabaseName(DBConstants.DB_NAME);
	}
	
	public static void testDB(Context context){
		DatabaseAdapter adapter = DatabaseAdapter.getInstance(context.getApplicationContext());
		
		String TAG = "DatabaseManager-DEBUG";
		
		OWRecordingTag sample_tag = new OWRecordingTag();
		sample_tag.name.set("test tag");
		sample_tag.is_featured.set(true);
		sample_tag.save(context);
		
		OWRecording sample_recording = new OWRecording();
		
		sample_recording.tags.add(sample_tag);
		sample_recording.title.set("test recording");
		sample_recording.save(context);
		
		OWLocalRecording sample_local = new OWLocalRecording();
		sample_local.title.set("test local recording");
		sample_local.description.set("test description");
		sample_local.tags.add(sample_tag);
		sample_local.recording.set(sample_recording);
		sample_local.save(context);
		
		Log.i(TAG, "models saved");
		
		QuerySet<OWRecording> qs = OWRecording.objects(context, OWRecording.class).all();
		for(OWRecording ow : qs){
			Log.i(TAG, "OWRecording QuerySet " + ow.title.toString() + " tags: " +  ow.tags.get(context, ow));
		}
		
		QuerySet<OWLocalRecording> qs3 = OWRecording.objects(context, OWLocalRecording.class).all();
		for(OWLocalRecording ow : qs3){
			Log.i(TAG, "OWLocalRecording QuerySet " + ow.title.toString() + " tags: " +  ow.tags.get(context, ow));
		}
		
		// Test manual db actions
		ContentValues cv = new ContentValues();
		cv.put("name", "test name2");
		adapter.open();
		adapter.doInsertOrUpdate(DBConstants.TAG_TABLENAME, cv, new Where().and("name","test name"));
		adapter.close();
		
		QuerySet<OWRecordingTag> qs2= OWRecordingTag.objects(context, OWRecordingTag.class).all();
		for(OWRecordingTag tag : qs2){
			Log.i(TAG,"OWRecordingTag QuerySet: " + tag.name.toString() + " id: " + String.valueOf(tag.getId()));
		}
	}
	
	public static void addChunk(Context c, String filepath){
		String[] filepathArray = filepath.split("/");
		String filename = filepathArray[filepathArray.length-1];
		
		OWLocalRecordingSegment segment = new OWLocalRecordingSegment();
		segment.filepath.set(filepath);
		segment.filename.set(filename);
		segment.save(c);
		
	}
	
	public static void insert(Context c, ContentValues cv, String recording_id){
		DatabaseAdapter adapter = new DatabaseAdapter(c);
		adapter.doInsertOrUpdate(DBConstants.LOCAL_RECORDINGS_TABLENAME, cv, new Where().and(DBConstants.RECORDINGS_TABLE_UUID, recording_id));
		adapter.close();
	}

}
