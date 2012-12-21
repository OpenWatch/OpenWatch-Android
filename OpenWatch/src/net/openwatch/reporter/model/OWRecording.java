package net.openwatch.reporter.model;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.orm.androrm.DatabaseAdapter;
import com.orm.androrm.Filter;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.DoubleField;
import com.orm.androrm.field.ForeignKeyField;
import com.orm.androrm.field.IntegerField;
import com.orm.androrm.field.ManyToManyField;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.contentprovider.OWContentProvider;


public class OWRecording extends Model{
	private static final String TAG = "OWRecording";
	/*
	public CharField title = new CharField();
	public CharField description = new CharField();
	public CharField thumb_url = new CharField();
	public IntegerField user_id = new IntegerField();
	public CharField username = new CharField(); 
	// username is queried often by ListViews, so it makes sense to duplicate info for performance 
	public CharField creation_time = new CharField();
	public CharField first_posted = new CharField();
	public CharField uuid = new CharField();
	public CharField last_edited = new CharField();
	public IntegerField server_id = new IntegerField();
	public CharField video_url = new CharField();
	public DoubleField begin_lat = new DoubleField();
	public DoubleField begin_lon = new DoubleField();
	public DoubleField end_lat = new DoubleField();
	public DoubleField end_lon = new DoubleField();
	*/
	
	public CharField title;
	public CharField description;
	public CharField thumb_url;
	public IntegerField user_id;
	public CharField username; 
	// username is queried often by ListViews, so it makes sense to duplicate info for performance 
	public CharField creation_time;
	public CharField first_posted;
	public CharField uuid;
	public CharField last_edited;
	public IntegerField server_id;
	public CharField video_url;
	public DoubleField begin_lat;
	public DoubleField begin_lon;
	public DoubleField end_lat;
	public DoubleField end_lon;
	public IntegerField views;
	public IntegerField actions;

	public ManyToManyField<OWRecording, OWRecordingTag> tags;
	public ForeignKeyField<OWUser> user;
	
	public OWRecording(){
		super();
		
		title = new CharField();
		description = new CharField();
		user_id = new IntegerField();
		username = new CharField(); 
		thumb_url = new CharField();
		creation_time = new CharField();
		creation_time.set(Constants.sdf.format(new Date()));
		first_posted = new CharField();
		uuid = new CharField();
		last_edited = new CharField();
		server_id = new IntegerField();
		video_url = new CharField();
		begin_lat = new DoubleField();
		begin_lon = new DoubleField();
		end_lat = new DoubleField();
		end_lon = new DoubleField();
		views = new IntegerField();
		actions = new IntegerField();
		
		tags = new ManyToManyField<OWRecording, OWRecordingTag> (OWRecording.class, OWRecordingTag.class);
		
	}
	
	public static void createOWRecordingsFromJSONArray(Context app_context, JSONArray json_array){
		try {
			DatabaseAdapter adapter = DatabaseAdapter.getInstance(app_context);
			adapter.beginTransaction();
			
			Filter filter;
			JSONObject json_obj;
			JSONObject json_user;
			QuerySet<OWUser> existing_users;
			QuerySet<OWRecording> existing_recs;
			OWRecording existing_rec = null;
			OWUser user = null;
			for(int x=0; x<json_array.length();x++){	
				json_obj = json_array.getJSONObject(x);
				filter = new Filter();
				filter.is(DBConstants.RECORDINGS_TABLE_UUID, json_obj.getString(DBConstants.RECORDINGS_TABLE_UUID));
				existing_recs = OWRecording.objects(app_context, OWRecording.class).filter(filter);
				for(OWRecording rec : existing_recs){
					existing_rec = rec;
					break;
				}
				if(existing_rec == null)
					existing_rec = new OWRecording();
				existing_rec.views.set(json_obj.getInt(DBConstants.RECORDINGS_TABLE_VIEWS));
				existing_rec.title.set(json_obj.getString(DBConstants.RECORDINGS_TABLE_TITLE));
				existing_rec.server_id.set(json_obj.getInt(Constants.OW_SERVER_ID));
				if(json_obj.getString(Constants.OW_THUMB_URL).compareTo(Constants.OW_NO_VALUE)!= 0)
					existing_rec.thumb_url.set(json_obj.getString(Constants.OW_THUMB_URL));
				existing_rec.last_edited.set(json_obj.getString(Constants.OW_LAST_EDITED));
				existing_rec.actions.set(json_obj.getInt(Constants.OW_ACTIONS));
				
				json_user = json_obj.getJSONObject(Constants.OW_USER);
				filter = new Filter();
				filter.is(DBConstants.USER_SERVER_ID, json_user.getInt(Constants.OW_SERVER_ID));
				existing_users = OWUser.objects(app_context, OWUser.class).filter(filter);
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
				existing_rec.user.set(user.getId());
				existing_rec.save(app_context);
			}
			adapter.commitTransaction();
			
		} catch (JSONException e) {
			Log.e(TAG, " Error parsing feed's recording array");
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean save(Context context) {
		// notify the ContentProvider that the dataset has changed
		context.getContentResolver().notifyChange(OWContentProvider.getRemoteRecordingUri(this.getId()), null);
		return super.save(context);
	}
	
	/*
	recording: {
		reported_creation_time: "2012-12-10 21:20:30",
		uuid: "96f9be13-6159-4c66-ad39-1cbc4eeea315",
		tags: [ ],
		first_posted: "2012-12-10 21:20:35",
		user: 4,
		last_edited: "2012-12-10 21:20:35",
		id: 1
	}
	 */

}
