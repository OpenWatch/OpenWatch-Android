
package org.ale.openwatch.model;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import com.orm.androrm.*;
import com.orm.androrm.field.*;

import com.orm.androrm.migration.Migrator;
import com.orm.androrm.statement.Statement;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.constants.DBConstants;
import org.ale.openwatch.constants.Constants.CONTENT_TYPE;
import org.ale.openwatch.constants.Constants.OWFeedType;
import org.ale.openwatch.contentprovider.OWContentProvider;
import org.ale.openwatch.http.OWServiceRequests;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class OWServerObject extends Model implements OWServerObjectInterface{
	private static final String TAG = "OWMediaObject";
	
	public CharField title = new CharField();
	public IntegerField views = new IntegerField();
	public IntegerField actions = new IntegerField();
	public IntegerField server_id = new IntegerField();
	public CharField description = new CharField();
	public CharField thumbnail_url = new CharField();
    public CharField user_thumbnail_url = new CharField();
	public CharField username = new CharField();
	public CharField first_posted = new CharField();
	public CharField last_edited = new CharField();
    public CharField metro_code = new CharField();
    public BooleanField is_private = new BooleanField();
	
	public ForeignKeyField<OWUser> user = new ForeignKeyField<OWUser>(
			OWUser.class);
	
	public ManyToManyField<OWServerObject, OWFeed> feeds = new ManyToManyField<OWServerObject, OWFeed> (OWServerObject.class, OWFeed.class);
	
	public ManyToManyField<OWServerObject, OWTag> tags = new ManyToManyField<OWServerObject, OWTag> (OWServerObject.class, OWTag.class);
	
	public ForeignKeyField<OWPhoto> photo = new ForeignKeyField<OWPhoto>(OWPhoto.class);
	
	public ForeignKeyField<OWAudio> audio = new ForeignKeyField<OWAudio>(OWAudio.class);
	
	public ForeignKeyField<OWVideoRecording> video_recording = new ForeignKeyField<OWVideoRecording>(OWVideoRecording.class);
	
	public ForeignKeyField<OWInvestigation> investigation = new ForeignKeyField<OWInvestigation>(OWInvestigation.class);
	
	public ForeignKeyField<OWLocalVideoRecording> local_video_recording = new ForeignKeyField<OWLocalVideoRecording>(OWLocalVideoRecording.class);
	
	public ForeignKeyField<OWStory> story = new ForeignKeyField<OWStory>(OWStory.class);

    public ForeignKeyField<OWMission> mission = new ForeignKeyField<OWMission>(OWMission.class);
	
	
	public OWServerObject() {
		super();
	}

    @Override
    protected void migrate(Context context) {
        /*
            Migrator automatically keeps track of which migrations have been run.
            All we do is add a migration for each change that occurs after the initial app release
         */
        Migrator<OWServerObject> migrator = new Migrator<OWServerObject>(OWServerObject.class);

        migrator.addField("mission", new ForeignKeyField<OWMission>(OWMission.class));

        migrator.addField("user_thumbnail_url", new CharField());

        migrator.addField("metro_code", new CharField());

        migrator.addField("is_private", new BooleanField());

        // roll out all migrations
        migrator.migrate(context);
    }
	
	public boolean save(Context context, boolean doNotify){
		boolean did_it = this.save(context);
		if(doNotify){
			OWFeedType feed_type = null;
			for(OWFeed feed : feeds.get(context, this)){
				//feed_type = OWFeed.getFeedTypeFromString(context, feed.name.get());
				//Log.i(TAG, "feed_type: " + feed_type);
				if(feed.name.get() != null && feed.name.get().compareTo("") !=0){
					Log.i(TAG, "NotifyingChange on feed: " + OWContentProvider.getFeedUri(feed.name.get()).toString());
					context.getContentResolver().notifyChange(OWContentProvider.getFeedUri(feed.name.get()), null);
				}
			}
			if(this.user.get(context) != null)
				Log.i(TAG, "notify url : " + OWContentProvider.getUserRecordingsUri(this.user.get(context).getId()));
				try{
					context.getContentResolver().notifyChange(OWContentProvider.getUserRecordingsUri(this.user.get(context).getId()), null);
				} catch(NullPointerException ne){
					ne.printStackTrace();
				}
		}
		
		return did_it;
	}

    public boolean saveAndSetEdited(Context context) {
        setLastEdited(context, Constants.utc_formatter.format(new Date()));
        return save(context);
    }

	@Override
	public boolean save(Context context) {
		context = context.getApplicationContext();
		//setLastEdited(context, Constants.utc_formatter.format(new Date()));
		boolean super_save =  super.save(context);
		//Log.i(TAG, String.format("setLastEdited: %s", getLastEdited(context)));
		// notify the ContentProvider that the dataset has changed
		context.getContentResolver().notifyChange(OWContentProvider.getMediaObjectUri(getId()), null);
		// notify all of this object's feed uris
		/*
		 * Don't do this here - Often OWMediaObjects are added in batch transactions so leave it to the request response handler
		OWFeedType feed_type = null;
		for(OWFeed feed : feeds.get(context, this)){
			feed_type = OWFeed.getFeedTypeFromString(context, feed.name.get());
			Log.i(TAG, "feed_type: " + feed_type);
			if(feed_type != null){
				Log.i(TAG, "NotifyingChange on feed: " + OWContentProvider.getFeedUri(feed_type).toString());
				context.getContentResolver().notifyChange(OWContentProvider.getFeedUri(feed_type), null);
			}
		}
		*/
	    return super_save;
	}

    @Override
    public void setSynced(Context c, boolean isSynced) {
        ((OWServerObjectInterface) getChildObject(c)).setSynced(c, isSynced);
    }

    public boolean hasTag(Context context, String tag_name) {
		Filter filter = new Filter();
		filter.is(DBConstants.TAG_TABLE_NAME, tag_name);
		return (tags.get(context, this).filter(filter).count() > 0);
	}

    public static void createOrUpdateWithJson(Context c, JSONObject json, OWFeed feed, DatabaseAdapter adapter) throws JSONException {
        int feedId = feed.getId();
        Where where = new Where();
        where.and(DBConstants.SERVER_ID, json.getInt(Constants.OW_SERVER_ID));
        String type = json.getString("type");
        String notNullColumn = null;
        if(type.compareTo("video") == 0){
            notNullColumn = DBConstants.MEDIA_OBJECT_VIDEO;
        }else if(type.compareTo("investigation") == 0){
            notNullColumn = DBConstants.MEDIA_OBJECT_INVESTIGATION;
        }else if(type.compareTo("mission") == 0){
            notNullColumn = DBConstants.MEDIA_OBJECT_MISSION;
        }else if(type.compareTo("photo") == 0){
            notNullColumn = DBConstants.MEDIA_OBJECT_PHOTO;
        }else if(type.compareTo("audio") == 0){
            notNullColumn = DBConstants.MEDIA_OBJECT_AUDIO;
        }
        if(notNullColumn != null)
            where.and(new Statement(notNullColumn, "!=", "NULL"));
        else
            Log.e(TAG, "cannot recognize type of this json object");
        ContentValues serverObjectValues = new ContentValues();
        if(json.has(Constants.OW_TITLE))
            serverObjectValues.put(DBConstants.TITLE, json.getString(Constants.OW_TITLE));
        if(json.has(Constants.OW_DESCRIPTION))
            serverObjectValues.put(DBConstants.DESCRIPTION,  json.getString(Constants.OW_DESCRIPTION));
        if(json.has(Constants.OW_VIEWS))
            serverObjectValues.put(DBConstants.VIEWS,  json.getInt(Constants.OW_VIEWS));
        if(json.has(Constants.OW_CLICKS))
            serverObjectValues.put(DBConstants.ACTIONS, json.getInt(Constants.OW_CLICKS));
        if(json.has(Constants.OW_SERVER_ID))
            serverObjectValues.put(DBConstants.SERVER_ID, json.getInt(Constants.OW_SERVER_ID));
        if(json.has(Constants.OW_LAST_EDITED)){
            serverObjectValues.put(DBConstants.LAST_EDITED, json.getString(Constants.OW_LAST_EDITED));
            //Log.i("LastEdited", String.format("id %d last_edited %s",json.getInt(Constants.OW_SERVER_ID), json.getString(Constants.OW_LAST_EDITED) ));
        }if(json.has(Constants.OW_FIRST_POSTED))
            serverObjectValues.put(DBConstants.FIRST_POSTED, json.getString(Constants.OW_FIRST_POSTED));
        if(json.has(DBConstants.MEDIA_OBJECT_METRO_CODE))
            serverObjectValues.put(DBConstants.MEDIA_OBJECT_METRO_CODE, json.getString(DBConstants.MEDIA_OBJECT_METRO_CODE));
        if(json.has("video_recording"))
            serverObjectValues.put("video_recording", json.getInt("video_recording"));
        if(json.has("public")){
            serverObjectValues.put("is_private", (json.getBoolean("public") ? 1 : 0 ));
        }


        if(json.has(Constants.OW_THUMB_URL) && json.getString(Constants.OW_THUMB_URL).compareTo(Constants.OW_NO_VALUE)!= 0)
            serverObjectValues.put(DBConstants.THUMBNAIL_URL, json.getString(Constants.OW_THUMB_URL));

        //investigations are weird
        if(json.has("logo"))
            serverObjectValues.put(DBConstants.THUMBNAIL_URL, json.getString("logo"));

        int userId = -1;
        if(json.has(Constants.OW_USER)){
            JSONObject jsonUser = json.getJSONObject(Constants.OW_USER);
            ContentValues userValues = new ContentValues();
            Where userWhere = new Where();
            userWhere.and(DBConstants.USER_SERVER_ID, jsonUser.getInt(Constants.OW_SERVER_ID));

            if(jsonUser.has(Constants.OW_USERNAME)){
                userValues.put(DBConstants.USERNAME, jsonUser.getString(Constants.OW_USERNAME));
                serverObjectValues.put("username", jsonUser.getString(Constants.OW_USERNAME));
            }if(jsonUser.has(Constants.OW_SERVER_ID))
                userValues.put(DBConstants.SERVER_ID, jsonUser.getInt(Constants.OW_SERVER_ID));
            if(jsonUser.has(Constants.OW_THUMB_URL)){
                userValues.put(DBConstants.THUMBNAIL_URL, jsonUser.getString(Constants.OW_THUMB_URL));
                serverObjectValues.put("user_thumbnail_url", jsonUser.getString(Constants.OW_THUMB_URL));
                //user_thumbnail_url.set(jsonUser.getString(Constants.OW_THUMB_URL));
            }
            int transactionId = adapter.doInsertOrUpdate(DBConstants.USER_TABLENAME, userValues, userWhere);
            //Log.i("DBA", jsonUser.toString());
            //Log.i("DBA", String.format("update user w/ server_id %d and insortOrUpdate response: %d", jsonUser.getInt(Constants.OW_SERVER_ID),transactionId));
            Cursor cursor = adapter.query(String.format("SELECT _id FROM owuser WHERE server_id = %d",jsonUser.getInt(Constants.OW_SERVER_ID)));
            if(cursor != null && cursor.moveToFirst())
                userId = cursor.getInt(0);
        }

        if(userId != -1)
            serverObjectValues.put("user", userId);

        // Skip saving tags for now
        int transactionId = adapter.doInsertOrUpdate(DBConstants.MEDIA_OBJECT_TABLENAME, serverObjectValues, where);
        //Log.i("DBA", json.toString());
        //Log.i("DBA", String.format("update mediaObject with server_id %s and insertOrUpdate response: %d", json.getString(Constants.OW_SERVER_ID) , transactionId));
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
            if(json.has(DBConstants.MEDIA_OBJECT_METRO_CODE))
                this.metro_code.set(json.getString(DBConstants.MEDIA_OBJECT_METRO_CODE));

			
			if(json.has(Constants.OW_THUMB_URL) && json.getString(Constants.OW_THUMB_URL).compareTo(Constants.OW_NO_VALUE)!= 0)
				this.setThumbnailUrl(app_context, json.getString(Constants.OW_THUMB_URL));
			
			//investigations are weird
			if(json.has("logo"))
				this.setThumbnailUrl(app_context, json.getString("logo"));
			
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
				}
                if(json_user.has(Constants.OW_USERNAME))
                    user.username.set(json_user.getString(Constants.OW_USERNAME));
                if(json_user.has(Constants.OW_SERVER_ID))
                    user.server_id.set(json_user.getInt(Constants.OW_SERVER_ID));
                if(json_user.has(Constants.OW_THUMB_URL)){
                    user.thumbnail_url.set(json_user.getString(Constants.OW_THUMB_URL));
                    user_thumbnail_url.set(json_user.getString(Constants.OW_THUMB_URL));
                }
                user.save(app_context);
				//this.username.set(user.username.get());
				//this.user.set(user.getId());
				this.setUser(app_context, user);
			} // end if user

			
			if(json.has(Constants.OW_TAGS)){
				this.resetTags(app_context);
				JSONArray tag_array =  json.getJSONArray("tags");
				Filter filter;
				OWTag tag = null;
				for(int x=0;x<tag_array.length();x++){
					
					tag = OWTag.getOrCreateTagFromJson(app_context, tag_array.getJSONObject(x));
					tag.save(app_context);
					this.addTag(app_context, tag);
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
	public Integer getViews(Context c) {
		return this.views.get();
	}


	@Override
	public Integer getActions(Context c) {
		return this.actions.get();
	}


	@Override
	public Integer getServerId(Context c) {
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
			if(getViews(c) != null)
				json_obj.put(Constants.OW_VIEWS, getViews(c));
			if(getActions(c) != null)
				json_obj.put(Constants.OW_ACTIONS, getActions(c));
			if(getServerId(c) != null)
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

            Log.i("race", String.format("OwServerObject toJson id %d is_private: %b", getId(), is_private.get()));
            json_obj.put("public", !is_private.get());
			
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

	@Override
	public String getUUID(Context c) {
		CONTENT_TYPE type = getContentType(c);
		switch(type){
		case VIDEO:
			return this.video_recording.get(c).getUUID(c);
		case AUDIO:
			return this.audio.get(c).getUUID(c);
		case PHOTO:
			return this.photo.get(c).getUUID(c);
		}
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
        OWServerObjectInterface child = (OWServerObjectInterface)getChildObject(c);
		if(child != null){
            return child.getLat(c);
        }
        return 0.0;
	}

	@Override
	public void setLon(Context c, double lon) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double getLon(Context c) {
        OWServerObjectInterface child = (OWServerObjectInterface)getChildObject(c);
        if(child != null){
            return child.getLon(c);
        }
        return 0.0;
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
	
	public void saveAndSync(Context c){
		this.save(c);
		OWServiceRequests.syncOWServerObject(c, this, true, null);
	}

	@Override
	public CONTENT_TYPE getContentType(Context c) {
		if(this.video_recording.get(c) != null)
			return CONTENT_TYPE.VIDEO;
        else if(this.audio.get(c) != null)
            return CONTENT_TYPE.AUDIO;
        else if(this.photo.get(c) != null)
            return CONTENT_TYPE.PHOTO;
		else if(this.investigation.get(c) != null)
			return CONTENT_TYPE.INVESTIGATION;
		else if(this.story.get(c) != null)
			return CONTENT_TYPE.STORY;
        else if(this.mission.get(c) != null)
            return CONTENT_TYPE.MISSION;
		Log.e(TAG, "Unable to determine CONTENT_TYPE for OWServerObject " + String.valueOf(this.getId()));
		return null;
	}
	
	public Object getChildObject(Context c){
		if(this.investigation.get(c) != null)
			return this.investigation.get(c);
		else if(this.photo.get(c) != null)
			return this.photo.get(c);
		else if(this.audio.get(c) != null)
			return this.audio.get(c);
		else if(this.video_recording.get(c) != null)
			return this.video_recording.get(c);
        else if(this.mission.get(c) != null)
            return this.mission.get(c);
		return null;
	}

}
