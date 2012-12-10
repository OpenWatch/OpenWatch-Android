package net.openwatch.reporter.model;

import java.util.List;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;

public class OWTag extends Model{
	
	@Column(name = "name")
	public String name;
	
	public List<OWRecording> recordings(){
		return getMany(OWRecording.class, "Tag");
	}
	
	public List<OWUser> users(){
		return getMany(OWUser.class, "Tag");
	}
	
	@Column(name = "featured")
	public boolean is_featured;

}
