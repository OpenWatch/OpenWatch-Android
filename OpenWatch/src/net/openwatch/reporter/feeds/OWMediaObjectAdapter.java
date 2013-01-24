package net.openwatch.reporter.feeds;

import com.github.ignition.core.widgets.RemoteImageView;

import net.openwatch.reporter.R;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.DBConstants;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.TextView;

public class OWMediaObjectAdapter extends SimpleCursorAdapter {

	public OWMediaObjectAdapter(Context context, Cursor c) {
		super(context, R.layout.remote_feed_item, c, new String[]{}, new int[]{},0);
	}
	
	@Override
	public void bindView(View view, Context context, Cursor cursor){
		super.bindView(view, context, cursor);
		
		ViewCache view_cache = (ViewCache) view.getTag(R.id.list_item_cache);
        if (view_cache == null) {
        	view_cache = new ViewCache();
        	view_cache.title = (TextView) view.findViewById(R.id.title);
        	view_cache.username = (TextView) view.findViewById(R.id.username);
        	view_cache.thumbnail = (RemoteImageView) view.findViewById(R.id.thumbnail);
        	view_cache.views = (TextView) view.findViewById(R.id.view_count);
        	view_cache.actions = (TextView) view.findViewById(R.id.action_count);
            
        	view_cache.title_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_TITLE);   
        	view_cache.username_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_USERNAME);   
        	view_cache.thumbnail_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_THUMB_URL);
        	view_cache.views_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_VIEWS);
        	view_cache.actions_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_ACTIONS);
        	view_cache._id_col = cursor.getColumnIndexOrThrow(DBConstants.ID);
            view.setTag(R.id.list_item_cache, view_cache);
            
        }
        
        view_cache.title.setText(cursor.getString(view_cache.title_col));
        view_cache.username.setText(cursor.getString(view_cache.username_col));
        view_cache.views.setText(cursor.getString(view_cache.views_col));
        view_cache.actions.setText(cursor.getString(view_cache.actions_col));
        
        if(cursor.getString(view_cache.thumbnail_col) != null && cursor.getString(view_cache.thumbnail_col).compareTo("") != 0){
        	view_cache.thumbnail.setImageUrl(cursor.getString(view_cache.thumbnail_col));
        	view_cache.thumbnail.loadImage();
        }
        
        view.setTag(R.id.list_item_model, cursor.getInt(view_cache._id_col));
        
        if(!cursor.isNull(cursor.getColumnIndex(DBConstants.MEDIA_OBJECT_VIDEO)))
        	view.setTag(R.id.list_item_model_type, Constants.CONTENT_TYPE.VIDEO);
        if(!cursor.isNull(cursor.getColumnIndex(DBConstants.MEDIA_OBJECT_STORY)))
        	view.setTag(R.id.list_item_model_type, Constants.CONTENT_TYPE.STORY);
	
        view_cache.last_seen_id = cursor.getInt(cursor.getColumnIndexOrThrow(DBConstants.ID));
	}
	
	// Cache the views within a ListView row item 
    static class ViewCache {
        TextView title;
        TextView username;
        RemoteImageView thumbnail;
        TextView views;
        TextView actions;
        
        int last_seen_id;
                
        int title_col; 
        int thumbnail_col;
        int username_col;
        int views_col;
        int actions_col;
        int _id_col;
    }

}
