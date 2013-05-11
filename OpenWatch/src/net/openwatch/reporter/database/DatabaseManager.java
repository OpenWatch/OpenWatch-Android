package net.openwatch.reporter.database;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.model.OWAudio;
import net.openwatch.reporter.model.OWFeed;
import net.openwatch.reporter.model.OWLocalVideoRecording;
import net.openwatch.reporter.model.OWLocalVideoRecordingSegment;
import net.openwatch.reporter.model.OWMediaObject;
import net.openwatch.reporter.model.OWPhoto;
import net.openwatch.reporter.model.OWVideoRecording;
import net.openwatch.reporter.model.OWTag;
import net.openwatch.reporter.model.OWStory;
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
	
	private static final boolean DEBUG = true;
	private static boolean models_registered = false; // ensure this is only called once per app launch
	
	public static void setupDB(final Context app_context){
		
		registerModels(app_context);
		
		if(DEBUG){
			testDB(app_context);
		}
		
		new Thread(new Runnable(){
			@Override
			public void run(){
				SharedPreferences profile = app_context.getSharedPreferences(Constants.PROFILE_PREFS, 0);
				SharedPreferences.Editor editor = profile.edit();
				editor.putBoolean(Constants.DB_READY, true);
				editor.commit();
				Log.i(TAG, "db ready");
				
			}
		}).start();
		
	}
	
	public static void registerModels(Context app_context){
		if(models_registered)
			return;
		
		List<Class<? extends Model>> models = new ArrayList<Class<? extends Model>>();
		models.add(OWVideoRecording.class);
		models.add(OWTag.class);
		models.add(OWLocalVideoRecording.class);
		models.add(OWLocalVideoRecordingSegment.class);
		models.add(OWUser.class);
		models.add(OWFeed.class);
		models.add(OWStory.class);
		models.add(OWMediaObject.class);
		models.add(OWPhoto.class);
		models.add(OWAudio.class);
		
		pointToDB();
		DatabaseAdapter adapter = DatabaseAdapter.getInstance(app_context);
		adapter.setModels(models);
		models_registered = true;
	}
	
	public static void pointToDB(){
		DatabaseAdapter.setDatabaseName(DBConstants.DB_NAME);
		Log.i(TAG, "pointToDB finished");
	}
	
	public static void testDB(Context app_context){
		DatabaseAdapter adapter = DatabaseAdapter.getInstance(app_context);
		
		String TAG = "DatabaseManager-DEBUG";
		
		// Some devices have an initial bogus recording
		int count = OWLocalVideoRecording.objects(app_context, OWLocalVideoRecording.class).count();
		if(count > 0)
			Log.i(TAG, "Database has " + String.valueOf(count) + " OWRecordings");
		
		/*
		OWRecordingTag sample_tag = new OWRecordingTag();
		sample_tag.name.set("test tag");
		sample_tag.is_featured.set(true);
		sample_tag.save(app_context);
		
		
		OWRecording sample_recording = new OWRecording();
		OWLocalRecording sample_local = new OWLocalRecording();
		boolean saved_local = sample_local.save(app_context);
		
		OWLocalRecordingSegment segment = new OWLocalRecordingSegment();
		segment.filename.set("test");
		segment.local_recording.set(sample_local);
		segment.save(app_context);
		int seg_id = segment.getId();
		
		segment = OWLocalRecordingSegment.objects(app_context, OWLocalRecordingSegment.class).get(seg_id);
		// is segment.local_recording null?
		
		sample_recording.local.set(sample_local);
		//sample_recording.initializeRecording(app_context, Constants.sdf.format(new Date()), "dksdlf3209dfklsdfklf", 0.0, 0.0);
		boolean saved_recording = sample_recording.save(app_context);
		int rec_id = sample_recording.getId();
		
		sample_recording = OWRecording.objects(app_context, OWRecording.class).get(rec_id);
		sample_recording.local.set(rec_id);
		sample_recording.save(app_context);
		sample_recording = OWRecording.objects(app_context, OWRecording.class).get(rec_id);
		OWLocalRecording local = sample_recording.local.get();
		*/
		/*
		sample_recording.tags.add(sample_tag);
		sample_recording.title.set("test recording");
		sample_recording.description.set("test description");
		sample_recording.save(app_context);
		
		OWLocalRecording sample_local = new OWLocalRecording();
		//sample_local.recording.set(sample_recording);
		sample_local.save(app_context);
		
		Log.i(TAG, "models saved");
		
		QuerySet<OWRecording> qs = OWRecording.objects(app_context, OWRecording.class).all();
		for(OWRecording ow : qs){
			Log.i(TAG, "OWRecording QuerySet " + ow.title.toString() + " tags: " +  ow.tags.get(app_context, ow));
		}
		
		QuerySet<OWLocalRecording> qs3 = OWRecording.objects(app_context, OWLocalRecording.class).all();
		for(OWLocalRecording ow : qs3){
			//Log.i(TAG, "OWLocalRecording QuerySet " + ow.recording.get().title.toString() + " tags: " +  ow.recording.get().tags.get(app_context, ow.recording.get()));
		}
		
		// Test manual db actions
		ContentValues cv = new ContentValues();
		cv.put("name", "test name2");
		adapter.open();
		adapter.doInsertOrUpdate(DBConstants.TAG_TABLENAME, cv, new Where().and("name","test name"));
		//adapter.close();
		
		QuerySet<OWRecordingTag> qs2= OWRecordingTag.objects(app_context, OWRecordingTag.class).all();
		for(OWRecordingTag tag : qs2){
			Log.i(TAG,"OWRecordingTag QuerySet: " + tag.name.toString() + " id: " + String.valueOf(tag.getId()));
		}
		*/
	}
	
	public static void addChunk(Context c, String filepath){
		String[] filepathArray = filepath.split("/");
		String filename = filepathArray[filepathArray.length-1];
		
		OWLocalVideoRecordingSegment segment = new OWLocalVideoRecordingSegment();
		segment.filepath.set(filepath);
		segment.filename.set(filename);
		segment.save(c);
		
	}
	/*
	public static void insert(Context c, ContentValues cv, String recording_id){
		DatabaseAdapter adapter = new DatabaseAdapter(c);
		adapter.doInsertOrUpdate(DBConstants.LOCAL_RECORDINGS_TABLENAME, cv, new Where().and(DBConstants.RECORDINGS_TABLE_UUID, recording_id));
		adapter.close();
	}
	*/

}
