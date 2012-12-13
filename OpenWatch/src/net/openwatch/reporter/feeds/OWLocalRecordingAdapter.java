package net.openwatch.reporter.feeds;

import net.openwatch.reporter.R;
import net.openwatch.reporter.constants.DBConstants;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class OWLocalRecordingAdapter extends SimpleCursorAdapter {

	public OWLocalRecordingAdapter(Context context, Cursor c) {
		super(context, R.layout.local_feed_item, c, new String[]{}, new int[]{},0);
	}
	
	@Override
	public void bindView(View view, Context context, Cursor cursor){
		super.bindView(view, context, cursor);
		
		ViewCache view_cache = (ViewCache) view.getTag(R.id.list_item_cache);
        if (view_cache == null) {
        	view_cache = new ViewCache();
        	view_cache.title = (TextView) view.findViewById(R.id.title);
        	view_cache.thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
        	view_cache.date = (TextView) view.findViewById(R.id.date);
            
        	view_cache.title_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_TITLE);   
        	view_cache.date_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_CREATION_TIME);   
        	view_cache.thumbnail_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_THUMB_URL);
        	view_cache._id_col = cursor.getColumnIndexOrThrow(DBConstants.ID);
            view.setTag(R.id.list_item_cache, view_cache);
        }
        
        view_cache.title.setText(cursor.getString(view_cache.title_col));
        view_cache.date.setText(cursor.getString(view_cache.date_col));
        //TODO: droidfu's WebImageView
        
        view.setTag(R.id.list_item_model, cursor.getInt(view_cache._id_col));
	}
	
	// Cache the views within a ListView row item 
    static class ViewCache {
        TextView title;
        ImageView thumbnail;
        TextView date;
        
        int title_col; 
        int thumbnail_col;
        int date_col;
        int _id_col;
    }

}
