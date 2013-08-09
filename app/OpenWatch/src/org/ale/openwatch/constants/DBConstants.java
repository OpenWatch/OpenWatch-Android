package org.ale.openwatch.constants;

public class DBConstants {
	
	public static final String ID = "_id";
	
	// Recordings Database table and column names
	// These correlate to field names in OWRecording
	public static final String DB_NAME = "OpenWatchDB";

    // Generic
    public static final String SYNCED = "synced";
    public static final String SERVER_ID = "server_id";
    public static final String TITLE = "title";
    public static final String USERNAME = "username";
    public static final String THUMBNAIL_URL = "thumbnail_url";
    public static final String DESCRIPTION = "description";
    public static final String LAST_EDITED = "last_edited";
    public static final String FIRST_POSTED = "first_posted";
    public static final String ACTIONS = "actions";
    public static final String VIEWS = "views";
	
	// OWMediaObject table
	public static final String MEDIA_OBJECT_TABLENAME = "owserverobject";
	public static final String MEDIA_OBJECT_STORY = "story";
    public static final String MEDIA_OBJECT_MISSION = "mission";
	public static final String MEDIA_OBJECT_USER = "user";
	public static final String MEDIA_OBJECT_VIDEO = "video_recording";
	public static final String MEDIA_OBJECT_AUDIO = "audio";
	public static final String MEDIA_OBJECT_PHOTO = "photo";
    public static final String MEDIA_OBJECT_INVESTIGATION = "investigation";
	public static final String MEDIA_OBJECT_LOCAL_VIDEO = "local_video_recording";
    public static final String MEDIA_OBJECT_USER_THUMBNAIL = "user_thumbnail_url";
    public static final String MEDIA_OBJECT_METRO_CODE = "metro_code";
	
	// Local Video Recordings Table
	public static final String LOCAL_RECORDINGS_TABLENAME = "owlocalvideorecording";
    public static final String LOCAL_RECORDINGS_LQ_SYNCED = "ly_synced";
    public static final String LOCAL_RECORDINGS_HQ_SYNCED = "hq_synced";
	
	// Video Recording Table
	public static final String RECORDINGS_TABLENAME = "owvideorecording";
	public static final String RECORDINGS_TABLE_TITLE = "title";
	public static final String RECORDINGS_TABLE_DESC = "description";
	public static final String RECORDINGS_TABLE_THUMB_URL = "thumbnail_url";
	public static final String RECORDINGS_TABLE_CREATION_TIME = "creation_time";
	public static final String RECORDINGS_TABLE_UUID = "uuid";
	public static final String RECORDINGS_TABLE_FIRST_POSTED = "first_posted";
	public static final String RECORDINGS_TABLE_USER_ID = "user_id";
	public static final String RECORDINGS_TABLE_USERNAME = "username";
	public static final String RECORDINGS_TABLE_LAST_EDITED = "last_edited";
	public static final String RECORDINGS_TABLE_SERVER_ID = "server_id";
	public static final String RECORDINGS_TABLE_VIDEO_URL = "video_url";
	public static final String RECORDINGS_TABLE_BEGIN_LAT = "begin_lat";
	public static final String RECORDINGS_TABLE_BEGIN_LON = "begin_lon";
	public static final String RECORDINGS_TABLE_END_LAT = "end_lat";
	public static final String RECORDINGS_TABLE_END_LON = "end_lon";
	public static final String RECORDINGS_TABLE_LOCAL = "local";
	
	// Tag table
	public static final String TAG_TABLENAME = "owtag";
	public static final String TAG_TABLE_NAME = "name";
	public static final String TAB_TABLE_FEATURED = "is_featured";
	public static final String TAG_TABLE_RECORDINGS = "recordings";
	public static final String TAG_TABLE_SERVER_ID = "server_id";
	
	// User table
	public static final String USER_TABLENAME = "owuser";
	public static final String USER_USERNAME = "username";
	public static final String USER_SERVER_ID = "server_id";
	public static final String USER_RECORDINGS = "recordings";
	public static final String USER_TAGS = "tags";
	
	// Feed table
	public static final String FEED_TABLENAME = "owfeed";
	public static final String FEED_NAME = "name";
	
	// OWMediaObject - Feed Relation table
	public static final String FEED_MEDIA_OBJ_TABLENAME = "owfeed_owserverobject";

	// Story table
	public static final String STORY_TABLENAME = "owstory";
	public static final String STORY_SERVER_ID = "server_id";
	
	// Story - Feed Relation Table
	public static final String FEED_STORY_TABLENAME = "owfeed_owstory";
	
	// Photo table
	public static final String PHOTO_TABLENAME = "owphoto";
	
	// Audio table
	public static final String AUDIO_TABLENAME = "owaudio";
	
	// Investigation table
	public static final String INVESTIGATION_TABLENAME = "owinvestigation";

    // Mission Table
    public static final String EXPIRES = "expires";

}
