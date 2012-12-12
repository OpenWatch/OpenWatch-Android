package net.openwatch.reporter.contentprovider;

import net.openwatch.reporter.constants.DBConstants;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;

public class OWContentProvider extends ContentProvider {
	
	private static final String AUTHORITY = "net.openwatch.reporter.contentprovider.OWContentProvider";
	
	public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY + "/");
	
	private final UriMatcher mUriMatcher;
	
	 private static final int RECORDING = 1;
     private static final int RECORDING_ID = 2;
     
     public static final Uri LOCAL_RECORDING_URI = AUTHORITY_URI.buildUpon().appendPath(DBConstants.LOCAL_RECORDINGS_TABLENAME).build();
     public static final Uri RECORDING_URI = AUTHORITY_URI.buildUpon().appendPath(DBConstants.RECORDINGS_TABLENAME).build();
	
     public OWContentProvider(){
	// Create and initialize URI matcher.
    mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    mUriMatcher.addURI(AUTHORITY, DBConstants.RECORDINGS_TABLENAME, RECORDING);
    mUriMatcher.addURI(AUTHORITY, DBConstants.RECORDINGS_TABLENAME + "/#", RECORDING_ID);
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
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}


}
