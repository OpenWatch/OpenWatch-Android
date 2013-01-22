package net.openwatch.reporter.model;

import org.json.JSONException;
import org.json.JSONObject;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.contentprovider.OWContentProvider;
import android.content.Context;

import com.orm.androrm.Model;
import com.orm.androrm.field.BooleanField;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.IntegerField;
import com.orm.androrm.field.ManyToManyField;

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

}
