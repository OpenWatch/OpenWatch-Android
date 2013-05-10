package net.openwatch.reporter.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.openwatch.reporter.OWApplication;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.Constants.MEDIA_TYPE;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.contentprovider.OWContentProvider;
import android.content.Context;
import android.util.Log;

import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;
import com.orm.androrm.field.BooleanField;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.DoubleField;
import com.orm.androrm.field.IntegerField;

public class OWPhoto extends Model implements OWMobileGeneratedObject{
	private static final String TAG = "OWMobileGeneratedObject";
	// New model format. Forget relation to OWMediaObject. It's sloppy
	
	public CharField title = new CharField();
	public CharField uuid = new CharField();
	public BooleanField is_featured = new BooleanField();
	public IntegerField server_id = new IntegerField();
	public CharField first_posted = new CharField();
	public CharField directory = new CharField();
	public CharField filepath = new CharField();
	public BooleanField synced = new BooleanField();
	public DoubleField lat = new DoubleField();
	public DoubleField lon = new DoubleField();
	
	public CharField thumbnail_url = new CharField();
	public CharField media_url = new CharField();
		
	public OWPhoto(){
		super();
	}
	
	@Override
	public boolean save(Context context) {
		// notify the ContentProvider that the dataset has changed
		context.getContentResolver().notifyChange(OWContentProvider.getTagUri(this.getId()), null);
		return super.save(context);
	}


	@Override
	public String getTitle(Context c) {
		return this.title.get();
	}

	@Override
	public void setTitle(Context c, String title) {
		this.title.set(title);
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
	public String getUUID(Context c) {
		return this.uuid.get();
	}

	@Override
	public void setUUID(Context c, String uuid) {
		this.uuid.set(uuid);
	}

	@Override
	public void setLat(Context c, double lat) {
		this.lat.set(lat);
	}

	@Override
	public double getLat(Context c) {
		return this.lat.get();
	}

	@Override
	public void setLon(Context c, double lon) {
		this.lon.set(lon);
	}

	@Override
	public double getLon(Context c) {
		return this.lon.get();
	}

	@Override
	public void setMediaFilepath(Context c, String filepath) {
		this.filepath.set(filepath);
	}

	@Override
	public String getMediaFilepath(Context c) {
		return this.filepath.get();
	}

	@Override
	public void updateWithJson(Context c, JSONObject json) {
		
		try {
			if(json.has(Constants.OW_TITLE))
				this.setTitle(c, json.getString(Constants.OW_TITLE));
			if(json.has(Constants.OW_UUID))
				this.setUUID(c, json.getString(Constants.OW_UUID));
			if(json.has(Constants.OW_LAT))
				this.setLat(c, json.getDouble(Constants.OW_LAT));
			if(json.has(Constants.OW_LON))
				this.setLon(c, json.getDouble(Constants.OW_LON));
			if(json.has(Constants.OW_FIRST_POSTED))
				this.setFirstPosted(c, json.getString(Constants.OW_FIRST_POSTED));
			if(json.has(Constants.OW_THUMB_URL))
				this.thumbnail_url.set(json.getString(Constants.OW_THUMB_URL));
			if(json.has("media_url"))
				this.media_url.set(json.getString("media_url"));
			this.save(c);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public JSONObject toJsonObject(Context c) {
		JSONObject json_obj = new JSONObject();
		try {
			if(getTitle(c) != null)
				json_obj.put(Constants.OW_TITLE, getTitle(c));
			if(getUUID(c) != null)
				json_obj.put(Constants.OW_UUID, getUUID(c));
			if(this.getLat(c) != 0)
				json_obj.put("end_lat", getLat(c));
			if(this.getLon(c) != 0)
				json_obj.put("end_lon", getLon(c));
			if(this.getFirstPosted(c) != null)
				json_obj.put(Constants.OW_FIRST_POSTED, getFirstPosted(c));
			//if(getUserServerId() != -1)
			//	json_obj.put(Constants.OW_USER, getUserServerId());
			
			
		} catch (JSONException e) {
			Log.e(TAG, "Error serializing OWMediaObject");
			e.printStackTrace();
		}
		return json_obj;
	}

	@Override
	public int getUserServerId() {
		if(OWApplication.user_data == null)
			return -1;
		else
			return (Integer) OWApplication.user_data.get(DBConstants.USER_SERVER_ID);
	}

	@Override
	public void setUserServerId(int user_server_id) {
		if(OWApplication.user_data == null)
			return;
		else
			OWApplication.user_data.put(DBConstants.USER_SERVER_ID, user_server_id);

	}

	@Override
	public MEDIA_TYPE getType() {
		return Constants.MEDIA_TYPE.PHOTO;
	}

}
