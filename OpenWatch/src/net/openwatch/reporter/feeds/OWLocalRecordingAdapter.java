package net.openwatch.reporter.feeds;

import com.github.ignition.core.widgets.RemoteImageView;

import net.openwatch.reporter.R;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.DBConstants;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.View;
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
        	view_cache.thumbnail = (RemoteImageView) view.findViewById(R.id.thumbnail);
        	//view_cache.date = (TextView) view.findViewById(R.id.date);
        	view_cache.views = (TextView) view.findViewById(R.id.view_count);
        	view_cache.actions = (TextView) view.findViewById(R.id.action_count);
        	view_cache.status = (TextView) view.findViewById(R.id.status);
            
        	view_cache.title_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_TITLE);   
        	view_cache.date_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_LAST_EDITED);   
        	view_cache.thumbnail_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_THUMB_URL);
        	view_cache.views_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_VIEWS);
        	view_cache.actions_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_ACTIONS);
        	view_cache.local_vid_col = cursor.getColumnIndexOrThrow(DBConstants.MEDIA_OBJECT_LOCAL_VIDEO);
        	view_cache._id_col = cursor.getColumnIndexOrThrow(DBConstants.ID);
            view.setTag(R.id.list_item_cache, view_cache);
        }
        
        view_cache.title.setText(cursor.getString(view_cache.title_col));
        
        if(cursor.isNull(cursor.getColumnIndex(DBConstants.MEDIA_OBJECT_LOCAL_VIDEO))){
        	view_cache.status.setVisibility(View.GONE);
        }else{
        	view_cache.status.setVisibility(View.VISIBLE);
        }
        
        /* Don't show date for now
        if(cursor.getString(view_cache.date_col) != null){
        	view.findViewById(R.id.calendar_icon).setVisibility(View.VISIBLE);
        	view_cache.date.setText(cursor.getString(view_cache.date_col));
        }
        else
        	view.findViewById(R.id.calendar_icon).setVisibility(View.GONE);
        */
        
        if(cursor.getString(view_cache.thumbnail_col) != null && cursor.getString(view_cache.thumbnail_col).compareTo("") != 0){
        	view_cache.thumbnail.setImageUrl(cursor.getString(view_cache.thumbnail_col));
        	view_cache.thumbnail.loadImage();
        }
        //Log.i("OWLocalRecordingAdapter", "got id: " + String.valueOf(cursor.getInt(view_cache._id_col)));
        view.setTag(R.id.list_item_model, cursor.getInt(view_cache._id_col));
       
	}
	
	// Cache the views within a ListView row item 
    static class ViewCache {
        TextView title;
        RemoteImageView thumbnail;
        TextView views;
        TextView actions;
        TextView status;
                        
        int title_col; 
        int thumbnail_col;
        int date_col;
        int views_col;
        int actions_col;
        int local_vid_col;
        int _id_col;
    }

}
