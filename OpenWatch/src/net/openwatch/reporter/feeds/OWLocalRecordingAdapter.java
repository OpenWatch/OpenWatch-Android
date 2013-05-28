package net.openwatch.reporter.feeds;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.nostra13.universalimageloader.core.ImageLoader;
import net.openwatch.reporter.R;
import net.openwatch.reporter.constants.DBConstants;

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
        	//view_cache.date = (TextView) view.findViewById(R.id.date);
        	//view_cache.views = (TextView) view.findViewById(R.id.view_count);
        	//view_cache.actions = (TextView) view.findViewById(R.id.action_count);
        	//view_cache.status = (TextView) view.findViewById(R.id.status);
            view_cache.typeIcon = (ImageView) view.findViewById(R.id.type_icon);
            
        	view_cache.title_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_TITLE);   
        	view_cache.date_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_FIRST_POSTED);   
        	view_cache.thumbnail_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_THUMB_URL);
        	//view_cache.views_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_VIEWS);
        	//view_cache.actions_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_ACTIONS);
        	//view_cache.local_vid_col = cursor.getColumnIndexOrThrow(DBConstants.MEDIA_OBJECT_LOCAL_VIDEO);
            view_cache.audio_col = cursor.getColumnIndexOrThrow(DBConstants.MEDIA_OBJECT_AUDIO);
            view_cache.photo_col = cursor.getColumnIndexOrThrow(DBConstants.MEDIA_OBJECT_PHOTO);
            view_cache.video_col = cursor.getColumnIndexOrThrow(DBConstants.MEDIA_OBJECT_VIDEO);
            view_cache.investigation_col = cursor.getColumnIndexOrThrow(DBConstants.MEDIA_OBJECT_INVESTIGATION);

        	view_cache._id_col = cursor.getColumnIndexOrThrow(DBConstants.ID);
            view.setTag(R.id.list_item_cache, view_cache);
        }
        
        view_cache.title.setText(cursor.getString(view_cache.title_col));
        //view_cache.views.setText(cursor.getString(view_cache.views_col));
        //view_cache.actions.setText(cursor.getString(view_cache.actions_col));

        if(!cursor.isNull(view_cache.audio_col))
            view_cache.typeIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.microphone_icon));
        if(!cursor.isNull(view_cache.investigation_col))
            view_cache.typeIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.investigation_icon));
        if(!cursor.isNull(view_cache.photo_col))
            view_cache.typeIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.camera_icon));
        if(!cursor.isNull(view_cache.video_col))
            view_cache.typeIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.camcorder_icon));

        /*
        if(cursor.isNull(cursor.getColumnIndex(DBConstants.MEDIA_OBJECT_LOCAL_VIDEO))){
        	view_cache.status.setVisibility(View.GONE);
        }else{
        	view_cache.status.setVisibility(View.VISIBLE);
        }
        */
        
        /* Don't show date for now
        if(cursor.getString(view_cache.date_col) != null){
        	view.findViewById(R.id.calendar_icon).setVisibility(View.VISIBLE);
        	view_cache.date.setText(cursor.getString(view_cache.date_col));
        }
        else
        	view.findViewById(R.id.calendar_icon).setVisibility(View.GONE);
        */
        
        if(view_cache.last_seen_id != cursor.getInt(cursor.getColumnIndexOrThrow(DBConstants.ID)) && cursor.getString(view_cache.thumbnail_col) != null && cursor.getString(view_cache.thumbnail_col).compareTo("") != 0){
        	ImageLoader.getInstance().displayImage(cursor.getString(view_cache.thumbnail_col), view_cache.thumbnail);
        }else if(cursor.getString(view_cache.thumbnail_col) == null || cursor.getString(view_cache.thumbnail_col).compareTo("") == 0){
        	view_cache.thumbnail.setImageResource(R.drawable.thumbnail_placeholder);
        }
        //Log.i("OWLocalRecordingAdapter", "got id: " + String.valueOf(cursor.getInt(view_cache._id_col)));
        view.setTag(R.id.list_item_model, cursor.getInt(view_cache._id_col));
        
        view_cache.last_seen_id = cursor.getInt(cursor.getColumnIndexOrThrow(DBConstants.ID));
	}
	
	// Cache the views within a ListView row item 
    static class ViewCache {
        TextView title;
        ImageView thumbnail;
        ImageView typeIcon;
        //TextView views;
        //TextView actions;
        //TextView status;
        
        int last_seen_id;
                        
        int title_col; 
        int thumbnail_col;
        int date_col;
        //int views_col;
        //int actions_col;
        //int local_vid_col;
        int _id_col;

        int audio_col;
        int photo_col;
        int video_col;
        int investigation_col;

    }

}
