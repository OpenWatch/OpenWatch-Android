package net.openwatch.reporter.model;


import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.DBConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.orm.androrm.Filter;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.ForeignKeyField;
import com.orm.androrm.field.IntegerField;
import com.orm.androrm.field.ManyToManyField;

public class OWMediaObject extends Model implements OWMediaObjectInterface{
	private static final String TAG = "OWMediaObject";
	
	public CharField title = new CharField();
	public IntegerField views = new IntegerField();
	public IntegerField actions = new IntegerField();
	public IntegerField server_id = new IntegerField();
	public CharField description = new CharField();
	public CharField thumbnail_url = new CharField();
	public CharField username = new CharField();
	public CharField first_posted = new CharField();
	public CharField last_edited = new CharField();
	
	public ForeignKeyField<OWUser> user = new ForeignKeyField<OWUser>(
			OWUser.class);
	
	public ManyToManyField<OWMediaObject, OWFeed> feeds = new ManyToManyField<OWMediaObject, OWFeed> (OWMediaObject.class, OWFeed.class);
	
	public ManyToManyField<OWMediaObject, OWTag> tags = new ManyToManyField<OWMediaObject, OWTag> (OWMediaObject.class, OWTag.class);
	
	public ForeignKeyField<OWVideoRecording> video_recording = new ForeignKeyField<OWVideoRecording>(OWVideoRecording.class);
	
	public ForeignKeyField<OWLocalVideoRecording> local_video_recording = new ForeignKeyField<OWLocalVideoRecording>(OWLocalVideoRecording.class);
	
	public ForeignKeyField<OWStory> story = new ForeignKeyField<OWStory>(OWStory.class);
	
	
	public OWMediaObject() {
		super();
	}
	
	public boolean hasTag(Context context, String tag_name) {
		Filter filter = new Filter();
		filter.is(DBConstants.TAG_TABLE_NAME, tag_name);
		return (tags.get(context, this).filter(filter).count() > 0);
	}
	

	@Override
	public void updateWithJson(Context app_context, JSONObject json) {
		try {
			if(json.has(Constants.OW_TITLE))
				this.setTitle(app_context, json.getString(Constants.OW_TITLE));
			if(json.has(Constants.OW_DESCRIPTION))
				this.setDescription(app_context, json.getString(Constants.OW_DESCRIPTION));
			if(json.has(Constants.OW_VIEWS))
				this.setViews(app_context, json.getInt(Constants.OW_VIEWS));
			if(json.has(Constants.OW_CLICKS))
				this.setActions(app_context, json.getInt(Constants.OW_CLICKS));
			if(json.has(Constants.OW_SERVER_ID))
				this.setServerId(app_context, json.getInt(Constants.OW_SERVER_ID));
			if(json.has(Constants.OW_LAST_EDITED))
				this.last_edited.set(json.getString(Constants.OW_LAST_EDITED));
			if(json.has(Constants.OW_FIRST_POSTED))
				this.first_posted.set(json.getString(Constants.OW_FIRST_POSTED));

			
			if(json.has(Constants.OW_THUMB_URL) && json.getString(Constants.OW_THUMB_URL).compareTo(Constants.OW_NO_VALUE)!= 0)
				this.setThumbnailUrl(app_context, json.getString(Constants.OW_THUMB_URL));
			
			if(json.has(Constants.OW_USER)){
				JSONObject json_user = null;
				OWUser user = null;
				json_user = json.getJSONObject(Constants.OW_USER);
				Filter filter = new Filter();
				filter.is(DBConstants.USER_SERVER_ID, json_user.getInt(Constants.OW_SERVER_ID));
				QuerySet<OWUser> existing_users = OWUser.objects(app_context, OWUser.class).filter(filter);
				for(OWUser existing_user : existing_users){
					user = existing_user;
					break;
				}
				if(user == null){
					user = new OWUser();
					user.username.set(json_user.getString(Constants.OW_USERNAME));
					user.server_id.set(json_user.getInt(Constants.OW_SERVER_ID));
					user.save(app_context);
				}
				//this.username.set(user.username.get());
				//this.user.set(user.getId());
				this.setUser(app_context, user);
			} // end if user

			
			if(json.has(Constants.OW_TAGS)){
				this.resetTags(app_context);
				JSONArray tag_array =  json.getJSONArray("tags");
				Filter filter;
				for(int x=0;x<tag_array.length();x++){
					filter = new Filter();
					filter.is("name", tag_array.getString(x).toString());
					QuerySet<OWTag> tags = OWTag.objects(app_context, OWTag.class).filter(filter);
					if(tags.count() > 0){ // add existing tag
						for(OWTag tag : tags){
							this.addTag(app_context, tag);
							break;
						}
					} else{ // add a new tag
						OWTag new_tag = new OWTag();
						new_tag.name.set(tag_array.getString(x).toString());
						new_tag.save(app_context);
						this.addTag(app_context, new_tag);
					}
				}
			} // end tags update
			this.save(app_context);
			//Log.i(TAG, "updateWIthJson. server_id: " + String.valueOf(this.server_id.get()));
		} catch (JSONException e) {
			Log.e(TAG, "failed to update model with json");
			e.printStackTrace();
		}
	}

	@Override
	public void setTitle(Context c, String title) {
		this.title.set(title);
		
	}

	@Override
	public void setViews(Context c, int views) {
		this.views.set(views);	
	}

	@Override
	public void setActions(Context c, int actions) {
		this.actions.set(actions);
	}

	@Override
	public void setServerId(Context c, int server_id) {
		this.server_id.set(server_id);
	}

	@Override
	public void setDescription(Context c, String description) {
		this.description.set(description);
	}

	@Override
	public void setThumbnailUrl(Context c, String url) {
		this.thumbnail_url.set(url);
	}

	@Override
	public void setUser(Context c, OWUser user) {
		this.user.set(user);
		this.username.set(user.username.get());
	}

	@Override
	public void resetTags(Context c) {
		this.tags.reset();
	}

	@Override
	public void addTag(Context c, OWTag tag) {
		this.tags.add(tag);
		this.save(c);
	}


	@Override
	public String getTitle(Context c) {
		return this.title.get();
	}


	@Override
	public String getDescription(Context c) {
		return this.description.get();
	}


	@Override
	public QuerySet<OWTag> getTags(Context c) {
		return this.tags.get(c, this);
	}


	@Override
	public int getViews(Context c) {
		return this.views.get();
	}


	@Override
	public int getActions(Context c) {
		return this.actions.get();
	}


	@Override
	public int getServerId(Context c) {
		return this.server_id.get();
	}


	@Override
	public String getThumbnailUrl(Context c) {
		return this.thumbnail_url.get();
	}


	@Override
	public OWUser getUser(Context c) {
		return this.user.get(c);
	}


	@Override
	public void addToFeed(Context c, OWFeed feed) {
		this.feeds.add(feed);
		this.save(c);
	}


	@Override
	public String getFirstPosted(Context c) {
		return this.first_posted.get();
	}


	@Override
	public void setFirstPosted(Context c, String first_posted) {
		this.first_posted.set(first_posted);
	}


	@Override
	public String getLastEdited(Context c) {
		return this.last_edited.get();
	}


	@Override
	public void setLastEdited(Context c, String last_edited) {
		this.last_edited.set(last_edited);
	}


	@Override
	public JSONObject toJsonObject(Context c) {
		JSONObject json_obj = new JSONObject();
		try {
			if(getTitle(c) != null)
				json_obj.put(Constants.OW_TITLE, getTitle(c));
			if(getViews(c) != 0)
				json_obj.put(Constants.OW_VIEWS, getViews(c));
			if(getActions(c) != 0)
				json_obj.put(Constants.OW_ACTIONS, getActions(c));
			if(getServerId(c) != 0)
				json_obj.put(Constants.OW_SERVER_ID, getServerId(c));
			if(getDescription(c) != null)
				json_obj.put(Constants.OW_DESCRIPTION, getDescription(c));
			if(getThumbnailUrl(c) != null)
				json_obj.put(Constants.OW_THUMB_URL, getThumbnailUrl(c));
			if(getUser(c) != null)
				json_obj.put(Constants.OW_USER, getUser(c).toJSON());
			if(getLastEdited(c) != null)
				json_obj.put(Constants.OW_EDIT_TIME, getLastEdited(c));
			if(getFirstPosted(c) != null)
				json_obj.put(Constants.OW_FIRST_POSTED, getFirstPosted(c));
			
			QuerySet<OWTag> qs = getTags(c);
			JSONArray tags = new JSONArray();
			
			for (OWTag tag : qs) {
				tags.put(tag.name.get());
			}
			json_obj.put(Constants.OW_TAGS, tags);
			
		} catch (JSONException e) {
			Log.e(TAG, "Error serializing OWMediaObject");
			e.printStackTrace();
		}
		return json_obj;
	}

}
