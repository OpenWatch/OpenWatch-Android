package net.openwatch.reporter.model;

import java.util.Collection;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.http.OWServiceRequests;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.orm.androrm.Filter;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.IntegerField;
import com.orm.androrm.field.ManyToManyField;
import com.orm.androrm.field.OneToManyField;

public class OWUser extends Model{
	private static final String TAG = "OWUser";
	
	public CharField username = new CharField();
	public CharField thumbnail_url = new CharField();
	public IntegerField server_id = new IntegerField();
	
	public OneToManyField<OWUser, OWVideoRecording> recordings = new OneToManyField<OWUser, OWVideoRecording>(OWUser.class, OWVideoRecording.class);
	public ManyToManyField<OWUser, OWTag> tags = new ManyToManyField<OWUser, OWTag>(OWUser.class, OWTag.class);
	
	public OWUser(){
		super();
	}
	
	public OWUser(Context c){
		super();
		initializeNewUser(c);
		save(c);
	}
	
	public void initializeNewUser(Context c){
		Filter filter = new Filter();
		filter.is(DBConstants.TAB_TABLE_FEATURED, 1);
		QuerySet<OWTag> featured_tags = OWTag.objects(c, OWTag.class).filter(filter);
		Log.i(TAG, "Added new tags: " + String.valueOf(featured_tags.count()));
		tags.addAll(featured_tags.toList());
	}
	
	public JSONObject toJSON(){
		JSONObject json_obj = new JSONObject();
		try{
			if(this.username.get() != null)
				json_obj.put(Constants.OW_USERNAME, this.username.get());
			if(this.server_id.get() != null)
				json_obj.put(Constants.OW_SERVER_ID, this.server_id.get());
			if(this.thumbnail_url.get() != null)
				json_obj.put(Constants.OW_THUMB_URL, thumbnail_url.get());
		}catch (JSONException e){
			Log.e(TAG, "Error serialiazing OWUser");
			e.printStackTrace();
		}
		return json_obj;
	}
	
	public void addTag(Context c, OWTag tag){
		this.tags.add(tag);
		save(c);
		OWServiceRequests.setTags(c, tags.get(c, this));
	}

}
