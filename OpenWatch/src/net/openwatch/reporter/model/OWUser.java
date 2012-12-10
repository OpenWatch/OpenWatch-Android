package net.openwatch.reporter.model;

import java.util.List;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;

public class OWUser extends Model{
	
	@Column(name = "username")
	public String username;
	
	@Column(name = "server_id")
	public int server_id;
	
	public List<OWRecording> recordings(){
		return getMany(OWRecording.class, "User");
	}
	
	public List<OWTag> tags(){
		return getMany(OWTag.class, "User");
	}
	
	
	
}
