package net.openwatch.reporter.model;

import java.util.Date;

import android.content.Context;

import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.DoubleField;
import com.orm.androrm.field.IntegerField;
import com.orm.androrm.field.ManyToManyField;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.contentprovider.OWContentProvider;


public class OWRecording extends Model{
	/*
	public CharField title = new CharField();
	public CharField description = new CharField();
	public CharField thumb_url = new CharField();
	public IntegerField user_id = new IntegerField();
	public CharField username = new CharField(); 
	// username is queried often by ListViews, so it makes sense to duplicate info for performance 
	public CharField creation_time = new CharField();
	public CharField first_posted = new CharField();
	public CharField uuid = new CharField();
	public CharField last_edited = new CharField();
	public IntegerField server_id = new IntegerField();
	public CharField video_url = new CharField();
	public DoubleField begin_lat = new DoubleField();
	public DoubleField begin_lon = new DoubleField();
	public DoubleField end_lat = new DoubleField();
	public DoubleField end_lon = new DoubleField();
	*/
	
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
	public IntegerField views;
	public IntegerField actions;

	public ManyToManyField<OWRecording, OWRecordingTag> tags;
	
	public OWRecording(){
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
		views = new IntegerField();
		actions = new IntegerField();
		
		tags = new ManyToManyField<OWRecording, OWRecordingTag> (OWRecording.class, OWRecordingTag.class);
		
	}
	
	@Override
	public boolean save(Context context) {
		// notify the ContentProvider that the dataset has changed
		context.getContentResolver().notifyChange(OWContentProvider.getRecordingUri(this.getId()), null);
		return super.save(context);
	}
	
	/*
	recording: {
		reported_creation_time: "2012-12-10 21:20:30",
		uuid: "96f9be13-6159-4c66-ad39-1cbc4eeea315",
		tags: [ ],
		first_posted: "2012-12-10 21:20:35",
		user: 4,
		last_edited: "2012-12-10 21:20:35",
		id: 1
	}
	 */

}
