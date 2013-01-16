package net.openwatch.reporter.model;

import org.json.JSONObject;

import com.orm.androrm.QuerySet;

import android.content.Context;

public interface OWMediaObjectInterface {
	
	public String getTitle(Context c);
	public void setTitle(Context c, String title);
	
	public String getFirstPosted(Context c);
	public void setFirstPosted(Context c, String first_posted);
	
	public String getLastEdited(Context c);
	public void setLastEdited(Context c, String last_edited);
	
	public void setViews(Context c, int views);
	public int getViews(Context c);
	
	public void setActions(Context c, int actions);
	public int getActions(Context c);
	
	public void setServerId(Context c, int server_id);
	public int getServerId(Context c);
	
	public void setDescription(Context c, String description);
	public String getDescription(Context c);
	
	public void setThumbnailUrl(Context c, String url);
	public String getThumbnailUrl(Context c);
	
	public OWUser getUser(Context c);
	public void setUser(Context c, OWUser user);
	
	public QuerySet<OWTag> getTags(Context c);
	public void resetTags(Context c);
	public void addTag(Context c, OWTag tag); 
	
	public void addToFeed(Context c, OWFeed feed); 
	
	public void updateWithJson(Context c, JSONObject json);
	
	public JSONObject toJsonObject(Context c);
	
	public boolean save(Context c);
}
