package org.ale.occupygezi.feeds;

import org.ale.occupygezi.constants.DBConstants;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.ale.occupygezi.R;

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
        	view_cache.thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
        	//view_cache.views = (TextView) view.findViewById(R.id.view_count);
        	//view_cache.actions = (TextView) view.findViewById(R.id.action_count);
            view_cache.typeIcon = (ImageView) view.findViewById(R.id.type_icon);
            
        	view_cache.title_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_TITLE);   
        	view_cache.username_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_USERNAME);   
        	view_cache.thumbnail_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_THUMB_URL);
        	//view_cache.views_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_VIEWS);
        	//view_cache.actions_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_ACTIONS);
            view_cache.audio_col = cursor.getColumnIndexOrThrow(DBConstants.MEDIA_OBJECT_AUDIO);
            view_cache.photo_col = cursor.getColumnIndexOrThrow(DBConstants.MEDIA_OBJECT_PHOTO);
            view_cache.video_col = cursor.getColumnIndexOrThrow(DBConstants.MEDIA_OBJECT_VIDEO);
            view_cache.investigation_col = cursor.getColumnIndexOrThrow(DBConstants.MEDIA_OBJECT_INVESTIGATION);
            view_cache._id_col = cursor.getColumnIndexOrThrow(DBConstants.ID);
            view.setTag(R.id.list_item_cache, view_cache);
            
        }
        
        view_cache.title.setText(cursor.getString(view_cache.title_col));
        view_cache.username.setText(cursor.getString(view_cache.username_col));
        //view_cache.views.setText(cursor.getString(view_cache.views_col));
        //view_cache.actions.setText(cursor.getString(view_cache.actions_col));
        
        if(view_cache.last_seen_id != cursor.getInt(cursor.getColumnIndexOrThrow(DBConstants.ID)) && cursor.getString(view_cache.thumbnail_col) != null && cursor.getString(view_cache.thumbnail_col).compareTo("") != 0){
        	ImageLoader.getInstance().displayImage(cursor.getString(view_cache.thumbnail_col), view_cache.thumbnail);
        }else if(cursor.getString(view_cache.thumbnail_col) == null || cursor.getString(view_cache.thumbnail_col).compareTo("") == 0){
        	view_cache.thumbnail.setImageResource(R.drawable.thumbnail_placeholder);
        }

        if(!cursor.isNull(view_cache.audio_col))
            view_cache.typeIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.microphone_icon));
        if(!cursor.isNull(view_cache.investigation_col))
            view_cache.typeIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.investigation_icon));
        if(!cursor.isNull(view_cache.photo_col))
            view_cache.typeIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.camera_icon));
        if(!cursor.isNull(view_cache.video_col))
            view_cache.typeIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.camcorder_icon));


        view.setTag(R.id.list_item_model, cursor.getInt(view_cache._id_col));
	
        view_cache.last_seen_id = cursor.getInt(cursor.getColumnIndexOrThrow(DBConstants.ID));
	}
	
	// Cache the views within a ListView row item 
    static class ViewCache {
        TextView title;
        TextView username;
        ImageView thumbnail;
        ImageView typeIcon;
        //TextView views;
        //TextView actions;
        
        int last_seen_id;
                
        int title_col; 
        int thumbnail_col;
        int username_col;
        //int views_col;
        //int actions_col;
        int _id_col;

        int audio_col;
        int photo_col;
        int video_col;
        int investigation_col;
    }

}
