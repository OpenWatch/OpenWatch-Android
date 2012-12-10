package net.openwatch.reporter.model;

import java.util.List;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name = "Recordings")
public class OWRecording extends Model{
	
	// Server fields
	@Column(name = "title")
	public String title;
	
	@Column(name = "description")
	public String description;
	
	@Column(name = "creation_time")
	public String creation_time;
	
	@Column(name = "uuid")
	public String uuid;
	
	@Column(name = "first_posted")
	public String first_posted;
	
	@Column(name = "user_id")
	public int user_id;
	
	@Column(name = "last_edited")
	public String last_edited;
	
	@Column(name = "server_id")
	public int server_id;
	
	@Column(name = "video_url")
	public String video_url;
	
	@Column(name = "begin_lat")
	public double begin_lat;
	
	@Column(name = "begin_lon")
	public double begin_lon;
	
	@Column(name = "end_lat")
	public double end_lat;
	
	@Column(name = "end_lon")
	public double end_lon;
	
	public List<OWTag> tags(){
		return getMany(OWTag.class, "Recording");
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
