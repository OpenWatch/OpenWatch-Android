package org.ale.openwatch.contentprovider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;
import com.orm.androrm.DatabaseAdapter;
import org.ale.openwatch.BuildConfig;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.constants.Constants.OWFeedType;
import org.ale.openwatch.constants.DBConstants;

import java.util.Date;

public class OWContentProvider extends ContentProvider {
	
	private static final String TAG = "OWContentProvider";
	
	private static final String AUTHORITY = BuildConfig.PROVIDER_AUTHORITY;
	
	public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY + "/");
	
	private final UriMatcher mUriMatcher;
	
	 private static final int LOCAL_RECORDINGS = 1;
     private static final int LOCAL_RECORDING_ID = 2;
     
     private static final int REMOTE_RECORDINGS = 3;
     private static final int REMOTE_RECORDING_ID = 4;
     private static final int MEDIA_OBJS_BY_FEED = 8;
     
     private static final int MEDIA_OBJS_BY_USER = 9;
          
     private static final int TAGS = 5;
     private static final int TAG_ID = 6;
     private static final int TAG_SEARCH = 7;

    private static final int MISSIONS = 10;
     
     // Externally accessed uris
     public static final Uri LOCAL_RECORDING_URI = AUTHORITY_URI.buildUpon().appendPath(DBConstants.LOCAL_RECORDINGS_TABLENAME).build();
     public static final Uri REMOTE_RECORDING_URI = AUTHORITY_URI.buildUpon().appendPath(DBConstants.RECORDINGS_TABLENAME).build();
     public static final Uri MEDIA_OBJECT_URI = AUTHORITY_URI.buildUpon().appendPath(DBConstants.MEDIA_OBJECT_TABLENAME).build();
     public static final Uri MISSION_OBJECT_URI = AUTHORITY_URI.buildUpon().appendPath(DBConstants.MEDIA_OBJECT_MISSION).build();
     public static final Uri TAG_URI = AUTHORITY_URI.buildUpon().appendPath(DBConstants.TAG_TABLENAME).build();
     public static final Uri TAG_SEARCH_URI = TAG_URI.buildUpon().appendPath("search").build();
	
     public static Uri getUserRecordingsUri(int user_id){
    	 return MEDIA_OBJECT_URI.buildUpon().appendEncodedPath("user").appendEncodedPath(String.valueOf(user_id)).build();
     }
     public static Uri getLocalRecordingUri(int model_id){
    	 return LOCAL_RECORDING_URI.buildUpon().appendEncodedPath(String.valueOf(model_id)).build();
     }
     public static Uri getRemoteRecordingUri(int model_id){
    	 return REMOTE_RECORDING_URI.buildUpon().appendEncodedPath(String.valueOf(model_id)).build();
     }
     // to be removed
     public static Uri getFeedUri(OWFeedType feed_type){
    	 return MEDIA_OBJECT_URI.buildUpon().appendEncodedPath(Constants.feedInternalEndpointFromType(feed_type)).build();
     }
     public static Uri getFeedUri(String feed_name){
        return MEDIA_OBJECT_URI.buildUpon().appendEncodedPath(feed_name.trim().toLowerCase()).build();
     }
     public static Uri getMediaObjectUri(int model_id){
    	 return MEDIA_OBJECT_URI.buildUpon().appendEncodedPath(String.valueOf(model_id)).build();
     }
     public static Uri getTagUri(int model_id){
    	 return TAG_URI.buildUpon().appendEncodedPath(String.valueOf(model_id)).build();
     }
     public static Uri getTagSearchUri(String query){
    	 return TAG_SEARCH_URI.buildUpon().appendEncodedPath(query).build();
     }
    public static Uri getMissionUri(){
        return MISSION_OBJECT_URI;
    }
     
     public OWContentProvider(){
		// Create and initialize URI matcher.
	    mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	    mUriMatcher.addURI(AUTHORITY, DBConstants.LOCAL_RECORDINGS_TABLENAME, LOCAL_RECORDINGS);
	    mUriMatcher.addURI(AUTHORITY, DBConstants.LOCAL_RECORDINGS_TABLENAME + "/#", LOCAL_RECORDING_ID);
	    //mUriMatcher.addURI(AUTHORITY, DBConstants.MEDIA_OBJECT_TABLENAME + "/user/#", MEDIA_OBJS_BY_USER);
	    mUriMatcher.addURI(AUTHORITY, DBConstants.MEDIA_OBJECT_TABLENAME + "/*", MEDIA_OBJS_BY_FEED);
	    mUriMatcher.addURI(AUTHORITY, DBConstants.TAG_TABLENAME, TAGS);
	    mUriMatcher.addURI(AUTHORITY, DBConstants.TAG_TABLENAME + "/#", TAG_ID);
	    mUriMatcher.addURI(AUTHORITY, DBConstants.TAG_TABLENAME + "/search/*", TAG_SEARCH);
        mUriMatcher.addURI(AUTHORITY, DBConstants.MEDIA_OBJECT_MISSION, MISSIONS);
     }

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// not necessary for internal use
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// not implemented
		return null;
	}
	
	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public synchronized Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		
		int uriType = mUriMatcher.match(uri);
		Cursor result = null;
		//adapter.query("Select")
		String select = "SELECT ";
		if(projection != null){
			for(int x=0; x< projection.length;x++){
				if(x+1 < projection.length)
					select += projection[x] + ", ";
				else
					select += projection[x] + " ";
			}
		}
		String where = "";
		if(selection != null && selectionArgs != null){
			for(int x=0;x<selectionArgs.length;x++){
				if(x + 1 < selectionArgs.length)
					where = selection.replaceFirst("?", selectionArgs[x] + " AND ");
				else
					where = selection.replaceFirst("?", selectionArgs[x]);
			}
		}
		if(where != "")
			where = " WHERE " + where;
		String sortby = " ";
		if(sortOrder != null)
			sortby = sortOrder;
		
		/*
		 * 
		uri	The URI to query. This will be the full URI sent by the client; if the client is requesting a specific record, the URI will end in a record number that the implementation should parse and add to a WHERE or HAVING clause, specifying that _id value.
		projection	The list of columns to put into the cursor. If null all columns are included.
		selection	A selection criteria to apply when filtering rows. If null then all rows are included.
		selectionArgs	You may include ?s in selection, which will be replaced by the values from selectionArgs, in order that they appear in the selection. The values will be bound as Strings.
		sortOrder	How the rows in the cursor should be sorted. If null then the provider is free to define the sort order.
		 */
		DatabaseAdapter adapter = DatabaseAdapter.getInstance(getContext().getApplicationContext());
		String query;
		switch(uriType){
			case MEDIA_OBJS_BY_USER:
				query = select + " FROM " + DBConstants.MEDIA_OBJECT_TABLENAME + " WHERE " + DBConstants.MEDIA_OBJECT_USER + " = " + uri.getLastPathSegment() + " AND (" + DBConstants.MEDIA_OBJECT_VIDEO + " IS NOT NULL" + " OR " + DBConstants.MEDIA_OBJECT_AUDIO + " IS NOT NULL OR " + DBConstants.MEDIA_OBJECT_PHOTO + " IS NOT NULL )" ;
				result = adapter.open().query(query);
				break;
			case LOCAL_RECORDINGS:
				result = adapter.open().query(select + " FROM " + DBConstants.MEDIA_OBJECT_TABLENAME + " WHERE " + DBConstants.MEDIA_OBJECT_LOCAL_VIDEO + " IS NOT NULL " + " ORDER BY " + sortby);
				break;
			case LOCAL_RECORDING_ID:
				result = adapter.open().query(select + " FROM " + DBConstants.MEDIA_OBJECT_TABLENAME + " WHERE " + DBConstants.MEDIA_OBJECT_LOCAL_VIDEO + " IS NOT NULL AND " + DBConstants.ID + "=" +uri.getLastPathSegment());
				//adapter.close();
				break;
			case MEDIA_OBJS_BY_FEED:
				int feed_id = -1;
                String queryString = "SELECT " + DBConstants.ID + "  from " + DBConstants.FEED_TABLENAME +" WHERE " + DBConstants.FEED_NAME+ "= \"" + uri.getLastPathSegment() + "\"";
                Log.i(TAG, String.format("getFeed query: %s",queryString));
				Cursor feed_cursor = adapter.open().query(queryString);
				if(feed_cursor.moveToFirst()){
					feed_id = feed_cursor.getInt(0);
					//Log.i(TAG, String.format("got feed_id: %d", feed_id));
					feed_cursor.close();
				}
				else{
					Log.i(TAG, "Could not find requested feed!");
					return null;
				}			
				//Log.i(TAG, String.format("fetching feed id: %d", feed_id));
                queryString = select + " FROM " + DBConstants.MEDIA_OBJECT_TABLENAME + " JOIN " + DBConstants.FEED_MEDIA_OBJ_TABLENAME + " ON " + DBConstants.FEED_MEDIA_OBJ_TABLENAME+"."+DBConstants.MEDIA_OBJECT_TABLENAME + "=" + DBConstants.MEDIA_OBJECT_TABLENAME+"." + DBConstants.ID;
				if(uri.getLastPathSegment().compareTo(OWFeedType.MISSION.toString().toLowerCase()) == 0)
                    queryString += " JOIN owmission ON " + DBConstants.MEDIA_OBJECT_TABLENAME + "." + DBConstants.MEDIA_OBJECT_MISSION + " = " + "owmission._id" ;
                query =  queryString + " WHERE " + DBConstants.FEED_MEDIA_OBJ_TABLENAME + "." + DBConstants.FEED_TABLENAME + "=" + String.valueOf(feed_id);
                if(uri.getLastPathSegment().compareTo(OWFeedType.MISSION.toString().toLowerCase()) == 0)
                    query += " AND owmission.expires > '" + Constants.utc_formatter.format(new Date()) + "'";
				//Log.i(TAG, "Query: " + query);
                Log.i(TAG, String.format("getFeedItems query: %s",query));
				result = adapter.open().query(query);
				if(result == null)
					Log.i(TAG, "Feed query was null!");
				break;
			case REMOTE_RECORDINGS:
				result = adapter.open().query(select + " FROM " + DBConstants.RECORDINGS_TABLENAME + " " + where + sortby);
				break;
			case REMOTE_RECORDING_ID:
				result = adapter.open().query(select + " FROM " + DBConstants.RECORDINGS_TABLENAME + " WHERE " + DBConstants.ID + "=" + uri.getLastPathSegment());
				break;
			case TAGS:
				result = adapter.open().query(select + " FROM " + DBConstants.TAG_TABLENAME + " " + where + sortby);
				break;
			case TAG_SEARCH:
				result = adapter.open().query(select + " FROM " + DBConstants.TAG_TABLENAME + " where name LIKE \"%" + uri.getLastPathSegment() + "%\"" + sortby);
				break;
            case MISSIONS:
                queryString = select + " FROM " + DBConstants.MEDIA_OBJECT_TABLENAME + " JOIN " + "owmission" + " ON " + " owmission._id = owserverobject.mission where owmission.expires > '" + Constants.utc_formatter.format(new Date())+ "'";
                if(sortby != null && sortby.trim().length() > 0)
                    queryString += " ORDER BY " + sortby;
                result = adapter.open().query(queryString);
                break;
		}

		if(result != null){
			result.setNotificationUri(getContext().getContentResolver(), uri);
		}
		return result;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}


}
