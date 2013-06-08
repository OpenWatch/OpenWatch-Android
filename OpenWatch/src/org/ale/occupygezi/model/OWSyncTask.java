package org.ale.occupygezi.model;

import android.content.Context;
import com.orm.androrm.Filter;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;
import com.orm.androrm.field.BooleanField;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.IntegerField;
import com.orm.androrm.field.ManyToManyField;

import org.ale.occupygezi.constants.Constants;
import org.ale.occupygezi.constants.DBConstants;
import org.ale.occupygezi.contentprovider.OWContentProvider;
import org.json.JSONException;
import org.json.JSONObject;

public class OWSyncTask extends Model{
	
	public CharField name;
	public BooleanField is_featured;
	public IntegerField server_id;
	
	public ManyToManyField<OWSyncTask, OWVideoRecording> recordings;
	
	public OWSyncTask(){
		super();
		name = new CharField();
		is_featured = new BooleanField();
		server_id = new IntegerField();
		
		recordings = new ManyToManyField<OWSyncTask, OWVideoRecording>(OWSyncTask.class, OWVideoRecording.class);
	}
	
	@Override
	public boolean save(Context context) {
		// notify the ContentProvider that the dataset has changed
		context.getContentResolver().notifyChange(OWContentProvider.getTagUri(this.getId()), null);
		return super.save(context);
	}
	
	public JSONObject toJson(){
		JSONObject result = new JSONObject();
		if(name.get() != null){
			try {
				result.put(Constants.OW_NAME, name.get());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return result;
		}
		return null;
	}
	
	public static OWSyncTask getOrCreateTagFromJson(Context c, JSONObject json) throws JSONException{

		Filter filter;
		OWSyncTask tag = null;

		filter = new Filter();
		// If a user added a tag that hasn't yet been synced, it could be duplicated due to lack of server_id
		filter.is(DBConstants.TAG_TABLE_NAME, json.getString(Constants.OW_NAME));
		//filter.is(DBConstants.TAG_TABLE_SERVER_ID, json.getInt(Constants.OW_SERVER_ID));

		QuerySet<OWSyncTask> tags = OWSyncTask.objects(c, OWSyncTask.class).filter(filter);
		for(OWSyncTask existing_tag : tags){
			tag = existing_tag;
			break;
		}
		
		if(tag == null){
			tag= new OWSyncTask();
		}
		if(json.has(Constants.OW_SERVER_ID))
			tag.server_id.set(json.getInt(Constants.OW_SERVER_ID));
		if(json.has(Constants.OW_NAME))
			tag.name.set(json.getString(Constants.OW_NAME));
		if(json.has(Constants.OW_FEATURED))
			tag.is_featured.set(json.getBoolean(Constants.OW_FEATURED));

		return tag;
	}

}
