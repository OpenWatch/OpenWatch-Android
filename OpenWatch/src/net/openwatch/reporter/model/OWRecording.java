package net.openwatch.reporter.model;

import java.util.List;

import com.orm.androrm.Model;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.DoubleField;
import com.orm.androrm.field.IntegerField;
import com.orm.androrm.field.ManyToManyField;

import net.openwatch.reporter.constants.DBConstants;


public class OWRecording extends Model{
	
	public CharField title;
	public CharField description;
	public IntegerField user_id;
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

	public ManyToManyField<OWRecording, OWRecordingTag> tags;
	
	public OWRecording(){
		super();
		title = new CharField();
		description = new CharField();
		user_id = new IntegerField();
		creation_time = new CharField();
		first_posted = new CharField();
		uuid = new CharField();
		last_edited = new CharField();
		server_id = new IntegerField();
		video_url = new CharField();
		begin_lat = new DoubleField();
		begin_lon = new DoubleField();
		end_lat = new DoubleField();
		end_lon = new DoubleField();
		tags = new ManyToManyField<OWRecording, OWRecordingTag> (OWRecording.class, OWRecordingTag.class);
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
