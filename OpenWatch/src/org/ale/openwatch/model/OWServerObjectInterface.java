package org.ale.openwatch.model;

import android.content.Context;
import com.orm.androrm.QuerySet;

import org.ale.openwatch.constants.Constants.CONTENT_TYPE;
import org.ale.openwatch.constants.Constants.MEDIA_TYPE;
import org.json.JSONObject;

public interface OWServerObjectInterface {
	
	public String getTitle(Context c);
	public void setTitle(Context c, String title);
	
	public String getFirstPosted(Context c);
	public void setFirstPosted(Context c, String first_posted);
	
	public String getLastEdited(Context c);
	public void setLastEdited(Context c, String last_edited);
	
	public void setViews(Context c, int views);
	public Integer getViews(Context c);
	
	public void setActions(Context c, int actions);
	public Integer getActions(Context c);
	
	public void setServerId(Context c, int server_id);
	public Integer getServerId(Context c);
	
	public void setDescription(Context c, String description);
	public String getDescription(Context c);
	
	public void setThumbnailUrl(Context c, String url);
	public String getThumbnailUrl(Context c);
	
	public OWUser getUser(Context c);
	public void setUser(Context c, OWUser user);
	
	public String getUUID(Context c);
	public void setUUID(Context c, String uuid);
	
	public void setLat(Context c, double lat);
	public double getLat(Context c);
	
	public void setLon(Context c, double lon);
	public double getLon(Context c);
	
	public MEDIA_TYPE getMediaType(Context c);
	public CONTENT_TYPE getContentType(Context c);
	
	public void setMediaFilepath(Context c, String filepath);
	public String getMediaFilepath(Context c);
	
	public QuerySet<OWTag> getTags(Context c);
	public void resetTags(Context c);
	public void addTag(Context c, OWTag tag); 
	
	public void addToFeed(Context c, OWFeed feed); 
	
	public void updateWithJson(Context c, JSONObject json);
	
	public JSONObject toJsonObject(Context c);
	
	public boolean save(Context c);
}
