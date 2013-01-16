package net.openwatch.reporter.model;

import net.openwatch.reporter.constants.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.orm.androrm.Model;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.IntegerField;
import com.orm.androrm.field.ManyToManyField;
import com.orm.androrm.field.OneToManyField;

public class OWUser extends Model{
	private static final String TAG = "OWUser";
	
	public CharField username = new CharField();;
	public CharField thumbnail_url = new CharField();;
	public IntegerField server_id = new IntegerField();;
	
	public OneToManyField<OWUser, OWVideoRecording> recordings = new OneToManyField<OWUser, OWVideoRecording>(OWUser.class, OWVideoRecording.class);
	public ManyToManyField<OWUser, OWTag> tags = new ManyToManyField<OWUser, OWTag>(OWUser.class, OWTag.class);
	
	public OWUser(){
		super();				
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

}
