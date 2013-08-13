package org.ale.openwatch;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.format.DateUtils;
import android.text.util.Linkify;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.ale.openwatch.R;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.constants.DBConstants;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OWMissionAdapter extends SimpleCursorAdapter {

    // A transform filter that simply returns just the text captured by the
    // first regular expression group.
    Linkify.TransformFilter mentionFilter = new Linkify.TransformFilter() {
        public final String transformUrl(final Matcher match, String url) {
            return match.group(1);
        }
    };

    // Match @mentions and capture just the username portion of the text.
    Pattern pattern = Pattern.compile("#([A-Za-z0-9_-]+)");
    String scheme = "openwatch://openwatch.net/w/";

    public OWMissionAdapter(Context context, Cursor c) {
        super(context, R.layout.mission_list_item, c, new String[]{}, new int[]{},0);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor){
        //super.bindView(view, context, cursor);

        ViewCache view_cache = (ViewCache) view.getTag(R.id.list_item_cache);
        if (view_cache == null) {
            view_cache = new ViewCache();
            view_cache.title = (TextView) view.findViewById(R.id.title);
            view_cache.thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
            view_cache.expires = (TextView) view.findViewById(R.id.expires);


            view_cache.mission_col = cursor.getColumnIndexOrThrow(DBConstants.MEDIA_OBJECT_MISSION);
            view_cache.title_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_TITLE);
            view_cache.thumbnail_col = cursor.getColumnIndexOrThrow(DBConstants.RECORDINGS_TABLE_THUMB_URL);
            view_cache.expires_col = cursor.getColumnIndexOrThrow(DBConstants.EXPIRES); // TODO: "owmission.expires"?
            view_cache._id_col = cursor.getColumnIndexOrThrow(DBConstants.ID);
            view.setTag(R.id.list_item_cache, view_cache);

        }

        if(cursor.getString(view_cache.title_col) == null || cursor.getString(view_cache.title_col).compareTo("")==0)
            view_cache.title.setVisibility(View.GONE);
        else{
            view_cache.title.setText(cursor.getString(view_cache.title_col));
            Linkify.addLinks(view_cache.title, pattern, scheme, null, mentionFilter);
            view_cache.title.setVisibility(View.VISIBLE);
            view_cache.title.setMovementMethod(null); // We're using a custom TextView to only intercept touches on links
        }

        try {
            view_cache.expires.setText("Expires " + DateUtils.getRelativeTimeSpanString(Constants.utc_formatter.parse(cursor.getString(view_cache.expires_col)).getTime()));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        view_cache.thumbnail.bringToFront();

        //view_cache.views.setText(cursor.getString(view_cache.views_col));
        //view_cache.actions.setText(cursor.getString(view_cache.actions_col));

        if(view_cache.last_seen_id != cursor.getInt(cursor.getColumnIndexOrThrow(DBConstants.ID)) && cursor.getString(view_cache.thumbnail_col) != null && cursor.getString(view_cache.thumbnail_col).compareTo("") != 0){
            ImageLoader.getInstance().displayImage(cursor.getString(view_cache.thumbnail_col), view_cache.thumbnail);
        }else if(cursor.getString(view_cache.thumbnail_col) == null || cursor.getString(view_cache.thumbnail_col).compareTo("") == 0){
            view_cache.thumbnail.setImageResource(R.drawable.thumbnail_placeholder);
        }

        view.setTag(R.id.list_item_model, cursor.getInt(view_cache._id_col));

        view_cache.last_seen_id = cursor.getInt(cursor.getColumnIndexOrThrow(DBConstants.ID));
    }

    // Cache the views within a ListView row item
    static class ViewCache {
        TextView title;
        TextView expires;
        ImageView thumbnail;

        int title_col;
        int thumbnail_col;
        int expires_col;
        int _id_col;
        int mission_col;

        int last_seen_id;
    }

}