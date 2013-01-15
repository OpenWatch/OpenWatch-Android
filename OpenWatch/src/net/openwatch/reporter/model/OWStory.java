package net.openwatch.reporter.model;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.DBConstants;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.orm.androrm.DatabaseAdapter;
import com.orm.androrm.Filter;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;
import com.orm.androrm.Where;
import com.orm.androrm.field.ForeignKeyField;

public class OWStory extends Model implements OWMediaObjectInterface{
	private static final String TAG = "OWStory";
	
	public ForeignKeyField<OWMediaObject> media_object = new ForeignKeyField<OWMediaObject> ( OWMediaObject.class );
	
	public OWStory() {
		super();
	}
	
	public OWStory(Context c){
		super();
		this.save(c);
		OWMediaObject media_object = new OWMediaObject();
		media_object.save(c);
		media_object.story.set(getId());
		this.media_object.set(media_object);
	}
	
	public void updateWithJson(Context app_context, JSONObject json){
		this.media_object.get(app_context).updateWithJson(app_context, json);
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
				this.save(app_context);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
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
				existing_story = OWMediaObject.objects(app_context, OWMediaObject.class).get(media_obj_id).story.get(app_context);
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
	public int getViews(Context c) {
		return this.media_object.get(c).getViews(c);
	}

	@Override
	public int getActions(Context c) {
		return this.media_object.get(c).getActions(c);
	}

	@Override
	public int getServerId(Context c) {
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

}
