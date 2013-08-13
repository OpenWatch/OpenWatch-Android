package org.ale.openwatch.model;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import com.orm.androrm.DatabaseAdapter;
import com.orm.androrm.Filter;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;
import com.orm.androrm.field.*;
import com.orm.androrm.migration.Migrator;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.constants.Constants.CONTENT_TYPE;
import org.ale.openwatch.constants.DBConstants;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class OWMission extends Model implements OWServerObjectInterface{
	private static final String TAG = "OWMission";

	public CharField body = new CharField();
    public CharField tag = new CharField();
    public BooleanField active = new BooleanField();
    public BooleanField completed = new BooleanField();
    public DoubleField usd = new DoubleField();
    public DoubleField karma = new DoubleField();
    public DoubleField lat = new DoubleField();
    public DoubleField lon = new DoubleField();
    public CharField media_url = new CharField();
    public CharField expires = new CharField();
    public CharField joined = new CharField();
    public BooleanField viewed_push = new BooleanField();
    public BooleanField viewed_mission = new BooleanField();
    public IntegerField members = new IntegerField();
    public IntegerField submissions = new IntegerField();

    public static enum ACTION {JOINED, LEFT, RECEIVED_PUSH, VIEWED_PUSH, VIEWED_MISSION};

	public ForeignKeyField<OWServerObject> media_object = new ForeignKeyField<OWServerObject> ( OWServerObject.class );

	public OWMission() {
		super();
	}

	public OWMission(Context c){
		super();
		this.save(c);
		OWServerObject media_object = new OWServerObject();
		media_object.mission.set(getId());
		media_object.save(c);
		this.media_object.set(media_object);
		this.save(c);
	}

    public void updateWithActionComplete(Context c, ACTION action){
        switch(action){
            case JOINED:
                this.joined.set(Constants.utc_formatter.format(new Date()));
                break;
            case LEFT:
                this.joined.set(null);
                break;
            case RECEIVED_PUSH:
                // not necessary. this will be reported directly by GCMBroadcastReceiver
                break;
            case VIEWED_PUSH:
                this.viewed_push.set(true);
                break;
            case VIEWED_MISSION:
                this.viewed_mission.set(true);
                break;
        }
        this.save(c);
    }

    @Override
    protected void migrate(Context context) {
        /*
            Migrator automatically keeps track of which migrations have been run.
            All we do is add a migration for each change that occurs after the initial app release
         */
        Migrator<OWMission> migrator = new Migrator<OWMission>(OWMission.class);

        migrator.addField("joined", new CharField());
        migrator.addField("left", new BooleanField());
        migrator.addField("viewed_push", new BooleanField());
        migrator.addField("viewed_mission", new BooleanField());

        migrator.addField("expires", new CharField());
        migrator.addField("members", new IntegerField());
        migrator.addField("submissions", new IntegerField());


        // roll out all migrations
        migrator.migrate(context);
    }
	
	@Override
	public boolean save(Context context) {
		// notify the ContentProvider that the dataset has changed
		return super.save(context);
	}

    @Override
    public void setSynced(Context c, boolean isSynced) {

    }

    public void updateWithJson(Context app_context, JSONObject json){
 
		this.media_object.get(app_context).updateWithJson(app_context, json);
		
		try{
			if(json.has(Constants.OW_BODY))
				body.set(json.getString(Constants.OW_BODY));
			if(json.has(Constants.OW_END_LAT))
				lat.set(json.getDouble(Constants.OW_END_LAT));
			if(json.has(Constants.OW_END_LON))
				lon.set(json.getDouble(Constants.OW_END_LON));
            if(json.has(Constants.OW_USD))
                usd.set(json.getDouble(Constants.OW_USD));
            if(json.has(Constants.OW_KARMA))
                karma.set(json.getDouble(Constants.OW_KARMA));
            if(json.has(Constants.OW_ACTIVE))
                active.set(json.getBoolean(Constants.OW_ACTIVE));
            if(json.has(Constants.OW_COMPLETED))
                completed.set(json.getBoolean(Constants.OW_COMPLETED));
            if(json.has(Constants.OW_MEDIA_BUCKET))
                media_url.set(json.getString(Constants.OW_MEDIA_BUCKET));
            if(json.has("primary_tag"))
                tag.set(json.getString("primary_tag"));
            if(json.has(Constants.OW_EXPIRES))
                expires.set(json.getString(Constants.OW_EXPIRES));
            if(json.has("joined"))
                joined.set(json.getString("joined"));
            if(json.has(Constants.OW_AGENTS))
                members.set(json.getInt(Constants.OW_AGENTS));
            if(json.has(Constants.OW_SUBMISSIONS))
                submissions.set(json.getInt(Constants.OW_SUBMISSIONS));
		}catch(JSONException e){
			Log.e(TAG, "Error deserializing story");
			e.printStackTrace();
		}
		// If mission has no thumbnail_url, try using user's thumbnail
		if( (this.getThumbnailUrl(app_context) == null || this.getThumbnailUrl(app_context).compareTo("") == 0) && json.has(Constants.OW_USER)){
			JSONObject json_user = null;
			OWUser user = null;
			try {
				json_user = json.getJSONObject(Constants.OW_USER);
				if(json_user.has(Constants.OW_THUMB_URL)){
					this.setThumbnailUrl(app_context, json_user.getString(Constants.OW_THUMB_URL));
					Log.i(TAG, "mission has no thumbnail, using user thumbnail instead");
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		this.save(app_context);
	}
	
	public static OWMission createOrUpdateOWMissionWithJson(Context app_context, JSONObject json_obj, OWFeed feed) throws JSONException{
		OWMission mission = createOrUpdateOWMissionWithJson(app_context, json_obj);
		// add mission to feed if not null
		if(feed != null){
			Log.i(TAG, String.format("Adding mission %s to feed %s", mission.getTitle(app_context), feed.name.get()));
			mission.addToFeed(app_context, feed);
			//mission.save(app_context);
			Log.i(TAG, String.format("mission %s now belongs to %d feeds", mission.getTitle(app_context), mission.media_object.get(app_context).feeds.get(app_context, mission.media_object.get(app_context)).count() ));
		}
		
		return mission;
	}
	
	public static OWMission createOrUpdateOWMissionWithJson(Context app_context, JSONObject json_obj)throws JSONException{
		OWMission existingMission = null;

		DatabaseAdapter dba = DatabaseAdapter.getInstance(app_context);
		String query_string = String.format("SELECT %s FROM %s WHERE %s NOTNULL AND %s=%d", DBConstants.ID, DBConstants.MEDIA_OBJECT_TABLENAME, DBConstants.MEDIA_OBJECT_MISSION, DBConstants.SERVER_ID, json_obj.get(Constants.OW_SERVER_ID));
		Cursor result = dba.open().query(query_string);
		if(result != null && result.moveToFirst()){
			int serverObjectId = result.getInt(0);
			if(serverObjectId != 0){
				existingMission = OWServerObject.objects(app_context, OWServerObject.class).get(serverObjectId).mission.get(app_context);
			}
			if(existingMission != null)
				Log.i(TAG, "found existing mission for id: " + String.valueOf( json_obj.getString(Constants.OW_SERVER_ID)));
		}
		
		if(existingMission == null){
			Log.i(TAG, "creating new mission");
			existingMission = new OWMission(app_context);
		}
		
		existingMission.updateWithJson(app_context, json_obj);
		return existingMission;
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
			if (body.get() != null)
				json_obj.put(Constants.OW_BODY, body.get().toString());
            if (active.get() != null)
                json_obj.put(Constants.OW_ACTIVE, active.get());
            if (completed.get() != null)
                json_obj.put(Constants.OW_COMPLETED, completed.get());
            if (usd.get() != null)
                json_obj.put(Constants.OW_USD, usd.get());
            if (karma.get() != null)
                json_obj.put(Constants.OW_KARMA, karma.get());
            if (lat.get() != null)
                json_obj.put(Constants.OW_END_LAT, lat.get());
            if (lon.get() != null)
                json_obj.put(Constants.OW_END_LON, lon.get());
            if (expires.get() != null)
                json_obj.put(Constants.OW_EXPIRES, expires.get());
            if (members.get() != null)
                json_obj.put(Constants.OW_AGENTS, members.get());
            if (submissions.get() != null)
                json_obj.put(Constants.OW_SUBMISSIONS, submissions.get());


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
		return this.lat.get();
	}

	@Override
	public void setLon(Context c, double lon) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double getLon(Context c) {
		return this.lon.get();
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
		return CONTENT_TYPE.MISSION;
	}

    public static OWServerObject getByServerId(Context c, int serverId){
        Filter filter = new Filter();
        filter.is(DBConstants.SERVER_ID, serverId);
        filter.is(DBConstants.MEDIA_OBJECT_MISSION, "!=","NULL");
        QuerySet<OWServerObject> serverObjects = OWServerObject.objects(c, OWServerObject.class).filter(filter);
        for(OWServerObject aServerObject : serverObjects){
            return aServerObject;
        }
        return null;
    }

}
