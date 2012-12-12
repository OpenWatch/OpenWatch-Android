package net.openwatch.reporter.database;

import java.util.ArrayList;
import java.util.List;

import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.model.OWLocalRecording;
import net.openwatch.reporter.model.OWLocalRecordingSegment;
import net.openwatch.reporter.model.OWRecording;
import net.openwatch.reporter.model.OWRecordingTag;
import net.openwatch.reporter.model.OWUser;

import android.content.ContentValues;
import android.content.Context;

import com.orm.androrm.DatabaseAdapter;
import com.orm.androrm.Model;
import com.orm.androrm.Where;

public class DatabaseManager {
	
	public static void setupDB(final Context c){
		
		List<Class<? extends Model>> models = new ArrayList<Class<? extends Model>>();
		models.add(OWRecording.class);
		models.add(OWRecordingTag.class);
		models.add(OWLocalRecording.class);
		models.add(OWLocalRecordingSegment.class);
		models.add(OWUser.class);
		
		DatabaseAdapter.setDatabaseName(DBConstants.DB_NAME);
		DatabaseAdapter adapter = new DatabaseAdapter(c);
		adapter.setModels(models);
		adapter.close();
		// If I do this concurrently, it's possible a db transaction will begin
		// before the db is ready. So i'll lag the UI on first-launch at the expense of stability
		// TODO: Idea: Make a task queue for db related things?
		/*
		new Thread(new Runnable(){

			@Override
			public void run() {
				List<Class<? extends Model>> models = new ArrayList<Class<? extends Model>>();
				models.add(OWRecording.class);
				models.add(OWRecordingTag.class);
				models.add(OWLocalRecording.class);
				models.add(OWLocalRecordingSegment.class);
				models.add(OWUser.class);
				
				DatabaseAdapter.setDatabaseName("OpenWatchDB");
				DatabaseAdapter adapter = new DatabaseAdapter(c);
				adapter.setModels(models);
				adapter.close();
			}
			
		}).start();
		*/
		
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
		adapter.doInsertOrUpdate(DBConstants.LOCAL_RECORDING_TABLENAME, cv, new Where().and(DBConstants.RECORDINGS_TABLE_UUID, recording_id));
		adapter.close();
	}

}
