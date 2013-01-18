package net.openwatch.reporter.model;

import java.util.Locale;

import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.constants.Constants.OWFeedType;

import android.content.Context;
import android.util.Log;

import com.orm.androrm.Filter;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;
import com.orm.androrm.field.CharField;
import com.orm.androrm.field.ManyToManyField;

public class OWFeed extends Model{
	
	public CharField name = new CharField();
	
	//public ManyToManyField<OWFeed, OWStory> stories = new ManyToManyField<OWFeed, OWStory>(OWFeed.class, OWStory.class);
	//public ManyToManyField<OWFeed, OWRecording> audio_recordings = new ManyToManyField<OWFeed, OWRecording>(OWFeed.class, OWRecording.class);
	
	public OWFeed(){
		super();		
	}
	
	/*
	 * Assumes enum Constants.OWFeedType lowercase face value is equal
	 * to the feed name
	 */
	public static OWFeed getFeedFromFeedType(Context app_context, OWFeedType type){
		Filter f = new Filter();
		f.is(DBConstants.FEED_NAME, type.toString().toLowerCase(Locale.US));
		QuerySet<OWFeed> feedset = OWFeed.objects(app_context, OWFeed.class).filter(f);
		for(OWFeed feed : feedset){
			return feed;
		}
		// Existing feed does not exist
		OWFeed new_feed = new OWFeed();
		new_feed.name.set(type.toString().toLowerCase(Locale.US));
		new_feed.save(app_context);
		return new_feed;
	}
	
	public static OWFeedType getFeedTypeFromString(Context app_context, String feed_name){
		for(OWFeedType type : OWFeedType.values()){
			//Log.i("FeedTypeFromString", String.format("Checking if %s = %s",type.toString().toLowerCase(), feed_name));
			if(type.toString().toLowerCase().compareTo(feed_name) == 0)
				return type;
		}
		return null;
	}

}
