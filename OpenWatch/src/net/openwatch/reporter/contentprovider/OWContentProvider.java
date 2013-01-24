package net.openwatch.reporter.contentprovider;

import com.orm.androrm.DatabaseAdapter;

import net.openwatch.reporter.constants.Constants.OWFeedType;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.model.OWFeed;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

public class OWContentProvider extends ContentProvider {
	
	private static final String TAG = "OWContentProvider";
	
	private static final String AUTHORITY = "net.openwatch.reporter.contentprovider.OWContentProvider";
	
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
     
     // Externally accessed uris
     public static final Uri LOCAL_RECORDING_URI = AUTHORITY_URI.buildUpon().appendPath(DBConstants.LOCAL_RECORDINGS_TABLENAME).build();
     public static final Uri REMOTE_RECORDING_URI = AUTHORITY_URI.buildUpon().appendPath(DBConstants.RECORDINGS_TABLENAME).build();
     public static final Uri MEDIA_OBJECT_URI = AUTHORITY_URI.buildUpon().appendPath(DBConstants.MEDIA_OBJECT_TABLENAME).build();
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
     public static Uri getFeedUri(OWFeedType feed_type){
    	 return MEDIA_OBJECT_URI.buildUpon().appendEncodedPath(Constants.feedInternalEndpointFromType(feed_type)).build();
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
     
     public OWContentProvider(){
		// Create and initialize URI matcher.
	    mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	    mUriMatcher.addURI(AUTHORITY, DBConstants.LOCAL_RECORDINGS_TABLENAME, LOCAL_RECORDINGS);
	    mUriMatcher.addURI(AUTHORITY, DBConstants.LOCAL_RECORDINGS_TABLENAME + "/#", LOCAL_RECORDING_ID);
	    mUriMatcher.addURI(AUTHORITY, DBConstants.MEDIA_OBJECT_TABLENAME + "/*", MEDIA_OBJS_BY_FEED);
	    mUriMatcher.addURI(AUTHORITY, DBConstants.TAG_TABLENAME, TAGS);
	    mUriMatcher.addURI(AUTHORITY, DBConstants.MEDIA_OBJECT_TABLENAME + "/user/#", MEDIA_OBJS_BY_USER);
	    mUriMatcher.addURI(AUTHORITY, DBConstants.TAG_TABLENAME + "/#", TAG_ID);
	    mUriMatcher.addURI(AUTHORITY, DBConstants.TAG_TABLENAME + "/search/*", TAG_SEARCH);
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
		Log.i(TAG, adapter.getDatabaseName());
		Log.i(TAG, uri.toString());
		switch(uriType){
			case MEDIA_OBJS_BY_USER:
				result = adapter.open().query(select + " FROM " + DBConstants.MEDIA_OBJECT_TABLENAME + " WHERE " + DBConstants.MEDIA_OBJECT_USER + " = " + uri.getLastPathSegment() + " ORDER BY " + sortby);
				break;
			case LOCAL_RECORDINGS:
				Log.i(TAG, select + " FROM " + DBConstants.MEDIA_OBJECT_TABLENAME + " WHERE " + DBConstants.MEDIA_OBJECT_LOCAL_VIDEO + " IS NOT NULL " + sortby);
				result = adapter.open().query(select + " FROM " + DBConstants.MEDIA_OBJECT_TABLENAME + " WHERE " + DBConstants.MEDIA_OBJECT_LOCAL_VIDEO + " IS NOT NULL " + " ORDER BY " + sortby);
				break;
			case LOCAL_RECORDING_ID:
				Log.i(TAG, select + " FROM " + DBConstants.RECORDINGS_TABLENAME + "WHERE _id="+uri.getLastPathSegment());
				result = adapter.open().query(select + " FROM " + DBConstants.MEDIA_OBJECT_TABLENAME + " WHERE " + DBConstants.MEDIA_OBJECT_LOCAL_VIDEO + " IS NOT NULL AND " + DBConstants.ID + "=" +uri.getLastPathSegment());
				//adapter.close();
				break;
			case MEDIA_OBJS_BY_FEED:
				Log.i("URI"+uri.getLastPathSegment(), "Query CP for Feed ");
				int feed_id = -1;
				//Log.i(TAG, "get feed _id query:" + " SELECT " + DBConstants.ID + " from owfeed where NAME = \"" + uri.getLastPathSegment() + "\"");
				Cursor feed_cursor = adapter.open().query("SELECT " + DBConstants.ID + "  from " + DBConstants.FEED_TABLENAME +" WHERE " + DBConstants.FEED_NAME+ "= \"" + uri.getLastPathSegment() + "\""); // empty
				if(feed_cursor.moveToFirst()){
					feed_id = feed_cursor.getInt(0);
					Log.i(TAG, String.format("got feed_id: %d", feed_id));
					feed_cursor.close();
				}
				else{
					Log.i(TAG, "Could not find requested feed!");
					return null;
				}			
				//Log.i(TAG, String.format("fetching feed id: %d", feed_id));
				String query = select + " FROM " + DBConstants.MEDIA_OBJECT_TABLENAME + " JOIN " + DBConstants.FEED_MEDIA_OBJ_TABLENAME + " ON " + DBConstants.FEED_MEDIA_OBJ_TABLENAME+"."+DBConstants.MEDIA_OBJECT_TABLENAME + "=" + DBConstants.MEDIA_OBJECT_TABLENAME+"." + DBConstants.ID + " WHERE " + DBConstants.FEED_MEDIA_OBJ_TABLENAME + "." + DBConstants.FEED_TABLENAME + "=" + String.valueOf(feed_id);
				//Log.i(TAG, "Query: " + query);
				result = adapter.open().query(query);
				if(result == null)
					Log.i(TAG, "Feed query was null!");
				//if(result.moveToFirst())
				//	Log.i(TAG, "Got feed cursor: " + result.getColumnCount());
				break;
			case REMOTE_RECORDINGS:
				result = adapter.open().query(select + " FROM " + DBConstants.RECORDINGS_TABLENAME + " " + where + sortby);
				break;
			case REMOTE_RECORDING_ID:
				result = adapter.open().query(select + " FROM " + DBConstants.RECORDINGS_TABLENAME + " WHERE " + DBConstants.ID + "=" + uri.getLastPathSegment());
				break;
			case TAGS:
				Log.i(TAG, select + " FROM " + DBConstants.TAG_TABLE_NAME + where + sortby);
				result = adapter.open().query(select + " FROM " + DBConstants.TAG_TABLENAME + " " + where + sortby);
				break;
			case TAG_SEARCH:
				//queryBuilder.setTables(DBConstants.TAG_TABLENAME);
				//queryBuilder.appendWhere(DBConstants.TAG_TABLE_NAME + " LIKE \"%" + uri.getLastPathSegment() + "%\"");
				//Log.i(TAG, select + " FROM " + DBConstants.TAG_TABLENAME + " where name LIKE \"%" + uri.getLastPathSegment() + "%\"" + sortby);
				result = adapter.open().query(select + " FROM " + DBConstants.TAG_TABLENAME + " where name LIKE \"%" + uri.getLastPathSegment() + "%\"" + sortby);
				//Log.i(TAG, String.valueOf(result.getCount()));
				break;
		}
		
		// adapter is not closed!
		//adapter.close();
		// Make sure that potential listeners are getting notified
		if(result != null){
			Log.i("URI" + uri.getLastPathSegment(), "set notificationUri: " + uri.toString());
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
