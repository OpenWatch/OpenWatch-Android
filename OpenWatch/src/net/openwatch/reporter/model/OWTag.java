package net.openwatch.reporter.model;

import android.content.Context;
import com.orm.androrm.Filter;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;
import com.orm.androrm.field.BooleanField;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.IntegerField;
import com.orm.androrm.field.ManyToManyField;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.contentprovider.OWContentProvider;
import org.json.JSONException;
import org.json.JSONObject;

public class OWTag extends Model{
	
	public CharField name;
	public BooleanField is_featured;
	public IntegerField server_id;
	
	public ManyToManyField<OWTag, OWVideoRecording> recordings;
	
	public OWTag(){
		super();
		name = new CharField();
		is_featured = new BooleanField();
		server_id = new IntegerField();
		
		recordings = new ManyToManyField<OWTag, OWVideoRecording>(OWTag.class, OWVideoRecording.class);
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
	
	public static OWTag getOrCreateTagFromJson(Context c, JSONObject json) throws JSONException{

		Filter filter;
		OWTag tag = null;

		filter = new Filter();
		// If a user added a tag that hasn't yet been synced, it could be duplicated due to lack of server_id
		filter.is(DBConstants.TAG_TABLE_NAME, json.getString(Constants.OW_NAME));
		//filter.is(DBConstants.TAG_TABLE_SERVER_ID, json.getInt(Constants.OW_SERVER_ID));

		QuerySet<OWTag> tags = OWTag.objects(c, OWTag.class).filter(filter);
		for(OWTag existing_tag : tags){
			tag = existing_tag;
			break;
		}
		
		if(tag == null){
			tag= new OWTag();
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
