package org.ale.openwatch.model;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import com.orm.androrm.DatabaseAdapter;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.ForeignKeyField;

import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.constants.DBConstants;
import org.ale.openwatch.constants.Constants.CONTENT_TYPE;
import org.ale.openwatch.constants.Constants.MEDIA_TYPE;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class OWStory extends Model implements OWServerObjectInterface{
	private static final String TAG = "OWStory";
	
	public CharField blurb = new CharField();
	public CharField slug = new CharField();
	public CharField body = new CharField();
	
	
	public ForeignKeyField<OWServerObject> media_object = new ForeignKeyField<OWServerObject> ( OWServerObject.class );
	
	public OWStory() {
		super();
	}
	
	public OWStory(Context c){
		super();
		this.save(c);
		OWServerObject media_object = new OWServerObject();
		media_object.story.set(getId());
		media_object.save(c);
		this.media_object.set(media_object);
		this.save(c);
	}
	
	@Override
	public boolean save(Context context) {
		// notify the ContentProvider that the dataset has changed
		if(media_object.get() != null){ // this is called once in <init> to get db id before medi_object created
			setLastEdited(context, Constants.utc_formatter.format(new Date()));
			//context.getContentResolver().notifyChange(OWContentProvider.getMediaObjectUri(media_object.get(context).getId()), null);
		}
		return super.save(context);
	}
	
	public void updateWithJson(Context app_context, JSONObject json){
 
		this.media_object.get(app_context).updateWithJson(app_context, json);
		
		try{
			if(json.has(Constants.OW_BODY))
				body.set(json.getString(Constants.OW_BODY));
			if(json.has(Constants.OW_BLURB))
				blurb.set(json.getString(Constants.OW_BLURB));
			if(json.has(Constants.OW_SLUG))
				slug.set(json.getString(Constants.OW_SLUG));
		}catch(JSONException e){
			Log.e(TAG, "Error deserializing story");
			e.printStackTrace();
		}
		// If story has no thumbnail_url, try using user's thumbnail
		if( (this.getThumbnailUrl(app_context) == null || this.getThumbnailUrl(app_context).compareTo("") == 0) && json.has(Constants.OW_USER)){
			JSONObject json_user = null;
			OWUser user = null;
			try {
				json_user = json.getJSONObject(Constants.OW_USER);
				if(json_user.has(Constants.OW_THUMB_URL)){
					this.setThumbnailUrl(app_context, json_user.getString(Constants.OW_THUMB_URL));
					Log.i(TAG, "Story has no thumbnail, using user thumbnail instead");
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		this.save(app_context);
	}
	
	public static OWStory createOrUpdateOWStoryWithJson(Context app_context, JSONObject json_obj, OWFeed feed) throws JSONException{
		OWStory story = createOrUpdateOWStoryWithJson(app_context, json_obj);
		// add story to feed if not null
		if(feed != null){
			Log.i(TAG, String.format("Adding story %s to feed %s", story.getTitle(app_context), feed.name.get()));
			story.addToFeed(app_context, feed);
			//story.save(app_context);
			Log.i(TAG, String.format("Story %s now belongs to %d feeds", story.getTitle(app_context), story.media_object.get(app_context).feeds.get(app_context, story.media_object.get(app_context)).count() ));
		}
		
		return story;
	}
	
	public static OWStory createOrUpdateOWStoryWithJson(Context app_context, JSONObject json_obj)throws JSONException{
		OWStory existing_story = null;

		DatabaseAdapter dba = DatabaseAdapter.getInstance(app_context);
		String query_string = String.format("SELECT %s FROM %s WHERE %s NOTNULL AND %s=%d", DBConstants.ID, DBConstants.MEDIA_OBJECT_TABLENAME, DBConstants.MEDIA_OBJECT_STORY, DBConstants.STORY_SERVER_ID, json_obj.get(Constants.OW_SERVER_ID));
		Cursor result = dba.open().query(query_string);
		if(result != null && result.moveToFirst()){
			int media_obj_id = result.getInt(0);
			if(media_obj_id != 0){
				existing_story = OWServerObject.objects(app_context, OWServerObject.class).get(media_obj_id).story.get(app_context);
			}
			if(existing_story != null)
				Log.i(TAG, "found existing story for id: " + String.valueOf( json_obj.getString(Constants.OW_SERVER_ID)));
		}
		
		if(existing_story == null){
			Log.i(TAG, "creating new story");
			existing_story = new OWStory(app_context);
		}
		
		existing_story.updateWithJson(app_context, json_obj);
		return existing_story;
	}

	@Override
	public void setTitle(Context c, String title) {
		this.media_object.get(c).setTitle(c, title);
	}

	@Override
	public void setViews(Context c, int views) {
		this.media_object.get(c).setViews(c, views);
	}

	@Override
	public void setActions(Context c, int actions) {
		this.media_object.get(c).setActions(c, actions);
	}

	@Override
	public void setServerId(Context c, int server_id) {
		this.media_object.get(c).setServerId(c, server_id);
	}

	@Override
	public void setDescription(Context c, String description) {
		this.media_object.get(c).setDescription(c, description);
	}

	@Override
	public void setThumbnailUrl(Context c, String url) {
		this.media_object.get(c).setThumbnailUrl(c, url);
	}

	@Override
	public void setUser(Context c, OWUser user) {
		this.media_object.get(c).setUser(c, user);
	}

	@Override
	public void resetTags(Context c) {
		this.media_object.get(c).resetTags(c);
	}

	@Override
	public void addTag(Context c, OWTag tag) {
		this.media_object.get(c).addTag(c, tag);
	}

	@Override
	public String getTitle(Context c) {
		return this.media_object.get(c).getTitle(c);
	}

	@Override
	public String getDescription(Context c) {
		return this.media_object.get(c).description.get();
	}

	@Override
	public QuerySet<OWTag> getTags(Context c) {
		return this.media_object.get(c).getTags(c); 
	}

	@Override
	public Integer getViews(Context c) {
		return this.media_object.get(c).getViews(c);
	}

	@Override
	public Integer getActions(Context c) {
		return this.media_object.get(c).getActions(c);
	}

	@Override
	public Integer getServerId(Context c) {
		return this.media_object.get(c).getServerId(c);
	}

	@Override
	public String getThumbnailUrl(Context c) {
		return this.media_object.get(c).getThumbnailUrl(c);
	}

	@Override
	public OWUser getUser(Context c) {
		return this.media_object.get(c).getUser(c);
	}

	@Override
	public void addToFeed(Context c, OWFeed feed) {
		this.media_object.get(c).addToFeed(c, feed);
	}

	@Override
	public String getFirstPosted(Context c) {
		return media_object.get(c).getFirstPosted(c);
	}

	@Override
	public void setFirstPosted(Context c, String first_posted) {
		media_object.get(c).setFirstPosted(c, first_posted);
	}

	@Override
	public String getLastEdited(Context c) {
		return media_object.get(c).getLastEdited(c);
	}

	@Override
	public void setLastEdited(Context c, String last_edited) {
		media_object.get(c).setLastEdited(c, last_edited);
	}

	@Override
	public JSONObject toJsonObject(Context app_context) {
		JSONObject json_obj = media_object.get(app_context).toJsonObject(app_context);
		try{
			if (blurb.get() != null)
				json_obj.put(Constants.OW_BLURB, blurb.get().toString());
			if (slug.get() != null)
				json_obj.put(Constants.OW_SLUG, slug.get().toString());
			if (body.get() != null)
				json_obj.put(Constants.OW_BODY, body.get().toString());
			
		}catch(JSONException e){
			Log.e(TAG, "Error serializing recording to json");
			e.printStackTrace();
		}
		return json_obj;
	}
	
	public static String getUrlFromId(int server_id){
		return Constants.OW_URL + Constants.OW_STORY_VIEW + String.valueOf(server_id);	
	}

	@Override
	public String getUUID(Context c) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setUUID(Context c, String uuid) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLat(Context c, double lat) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double getLat(Context c) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setLon(Context c, double lon) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double getLon(Context c) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public MEDIA_TYPE getMediaType(Context c) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setMediaFilepath(Context c, String filepath) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getMediaFilepath(Context c) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CONTENT_TYPE getContentType(Context c) {
		return CONTENT_TYPE.STORY;
	}

}
