package net.openwatch.reporter.model;

import java.io.File;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.Constants.MEDIA_TYPE;

import org.json.JSONObject;

import com.orm.androrm.QuerySet;

import android.content.Context;

public interface OWMobileGeneratedObject {
	
	public int getUserServerId();
	public void setUserServerId(int user_server_id);
	
	public String getTitle(Context c);
	public void setTitle(Context c, String title);
	
	public String getFirstPosted(Context c);
	public void setFirstPosted(Context c, String first_posted);
	
	public String getUUID(Context c);
	public void setUUID(Context c, String uuid);
	
	public void setLat(Context c, double lat);
	public double getLat(Context c);
	
	public void setLon(Context c, double lon);
	public double getLon(Context c);
	
	public MEDIA_TYPE getType();
	
	public void setMediaFilepath(Context c, String filepath);
	public String getMediaFilepath(Context c);
	
	public void updateWithJson(Context c, JSONObject json);
	
	public JSONObject toJsonObject(Context c);
	
	public boolean save(Context c);
}
