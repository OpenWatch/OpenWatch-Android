package org.ale.openwatch.model;

import android.content.Context;
import android.util.Log;
import com.orm.androrm.Model;
import com.orm.androrm.field.*;

import com.orm.androrm.migration.Migrator;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.constants.DBConstants;
import org.ale.openwatch.http.OWServiceRequests;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OWUser extends Model{
	private static final String TAG = "OWUser";
	
	public CharField username = new CharField();
    public CharField first_name = new CharField();
    public CharField last_name = new CharField();
    public CharField blurb = new CharField();
	public CharField thumbnail_url = new CharField();
	public IntegerField server_id = new IntegerField();
    public CharField gcm_registration_id = new CharField();

    public DoubleField lat = new DoubleField();
    public DoubleField lon = new DoubleField();

    public BooleanField agent_applicant = new BooleanField();
    public BooleanField agent_approved = new BooleanField();
	
	public OneToManyField<OWUser, OWVideoRecording> recordings = new OneToManyField<OWUser, OWVideoRecording>(OWUser.class, OWVideoRecording.class);
	public ManyToManyField<OWUser, OWTag> tags = new ManyToManyField<OWUser, OWTag>(OWUser.class, OWTag.class);

    @Override
    protected void migrate(Context context) {
        /*
            Migrator automatically keeps track of which migrations have been run.
            All we do is add a migration for each change that occurs after the initial app release
         */
        Migrator<OWUser> migrator = new Migrator<OWUser>(OWUser.class);

        migrator.addField("first_name", new CharField());

        migrator.addField("last_name", new CharField());

        migrator.addField("blurb", new CharField());

        migrator.addField("gcm_registration_id", new CharField());

        // roll out all migrations
        migrator.migrate(context);
    }
	
	public OWUser(){
		super();
	}
	
	public OWUser(Context c){
		super();
		save(c);
	}
	
	public JSONObject toJSON(){
		JSONObject json_obj = new JSONObject();
		try{
			if(this.username.get() != null)
				json_obj.put(Constants.OW_USERNAME, this.username.get());
            if(this.first_name.get() != null)
                json_obj.put(Constants.OW_FIRST_NAME, this.first_name.get());
            if(this.last_name.get() != null)
                json_obj.put(Constants.OW_LAST_NAME, this.last_name.get());
            if(this.blurb.get() != null)
                json_obj.put(Constants.OW_BLURB, this.blurb.get());
			if(this.server_id.get() != null)
				json_obj.put(Constants.OW_SERVER_ID, this.server_id.get());
			if(this.thumbnail_url.get() != null)
				json_obj.put(Constants.OW_THUMB_URL, thumbnail_url.get());
            if(this.agent_applicant.get() != null)
                json_obj.put("agent_applicant", agent_applicant.get());
            if(this.gcm_registration_id.get() != null)
                json_obj.put("google_push_token", gcm_registration_id.get());
            if(this.lat.get() != null && this.lon.get() != null){
                json_obj.put(Constants.OW_LAT, lat.get());
                json_obj.put(Constants.OW_LON, lon.get());
            }
		}catch (JSONException e){
			Log.e(TAG, "Error serialiazing OWUser");
			e.printStackTrace();
		}
		return json_obj;
	}
	
	public void updateWithJson(Context c, JSONObject json){
		try {
			if(json.has(DBConstants.USER_SERVER_ID))
				server_id.set(json.getInt(DBConstants.USER_SERVER_ID));
			if(json.has(Constants.OW_USERNAME))
				username.set(json.getString(Constants.OW_USERNAME));
            if(json.has(Constants.OW_FIRST_NAME))
                first_name.set(json.getString(Constants.OW_FIRST_NAME));
            if(json.has(Constants.OW_LAST_NAME))
                last_name.set(json.getString(Constants.OW_LAST_NAME));
            if(json.has(Constants.OW_BLURB))
                blurb.set(json.getString(Constants.OW_BLURB));
            if(json.has(Constants.OW_LAT) && json.has(Constants.OW_LON)){
                lat.set(json.getDouble(Constants.OW_LAT));
                lon.set(json.getDouble(Constants.OW_LON));
            }
            if(json.has(Constants.OW_THUMB_URL))
                thumbnail_url.set(json.getString(Constants.OW_THUMB_URL));
            if(json.has("agent_approved"))
                agent_approved.set(json.getBoolean("agent_approved"));
            if(json.has("agent_applicant"))
                agent_approved.set(json.getBoolean("agent_applicant"));
			if(json.has(Constants.OW_TAGS)){
				this.tags.reset();
				JSONArray tag_array =  json.getJSONArray("tags");
				OWTag tag = null;
				for(int x=0;x<tag_array.length();x++){
					tag = OWTag.getOrCreateTagFromJson(c, tag_array.getJSONObject(x));
					tag.save(c);
					this.addTag(c, tag, false);
				}
			} // end tags update
			this.save(c);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void addTag(Context c, OWTag tag, boolean syncWithOW){
		this.tags.add(tag);
		save(c);
		if(syncWithOW)
			OWServiceRequests.setTags(c, tags.get(c, this));
	}

}
