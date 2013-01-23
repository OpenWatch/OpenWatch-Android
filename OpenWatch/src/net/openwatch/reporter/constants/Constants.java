package net.openwatch.reporter.constants;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

import com.github.ignition.core.widgets.RemoteImageView;

/**
 * A class containing values that are not user-facing and so
 * are not intended for translation.
 * All user-facing values are contained in res/values/
 * @author davidbrodsky
 *
 */
public class Constants {
	// For view tag
	public static enum OWContentType { VIDEO, STORY };
	public static final String OW_CONTENT_TYPE = "owcontent_type";
	
	// Date Formatter for OW server time
	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
	
	static{
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	// SharedPreferences titles
	public static final String PROFILE_PREFS = "Profile";
	
	// External storage 
	public static final String ROOT_OUTPUT_DIR = "OpenWatch";
	public static final String RECORDING_OUTPUT_DIR = "recordings";
	
	// User profile keys. Used for SharedPreferences and Intent Extras
	public static final String EMAIL = "email";
	public static final String AUTHENTICATED = "authenticated";
	public static final String DB_READY = "db_ready";
	public static final String PUB_TOKEN = "public_upload_token"; 		// used for profile and to parse server login response
	public static final String PRIV_TOKEN = "private_upload_token";		// used for profile and to parse server login response
	public static final String REGISTERED = "registered"; 
	public static final String VIEW_TAG_MODEL = "model";		// key set on listview item holding corresponding model pk
	public static final String INTERNAL_DB_ID = "id";
	public static final String IS_LOCAL_RECORDING = "is_local";
	public static final String FEED_TYPE = "feed_type";
	
	// Email REGEX
	public static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
            "[a-zA-Z0-9+._%-+]{1,256}" +
            "@" +
            "[a-zA-Z0-9][a-zA-Z0-9-]{0,64}" +
            "(" +
            "." +
            "[a-zA-Z0-9][a-zA-Z0-9-]{0,25}" +
            ")+"
        );
	
	// API Request timeout (ms)
	public static final int TIMEOUT = 5000;
	
	// OpenWatch web service root url and endpoints
	//public static final String OW_URL = "http://www.openwatch.net/api/"; // TODO: HTTPS
	public static final String OW_API_URL = "http://192.168.1.27:8000/api/";
	public static final String OW_URL = "http://192.168.1.27:8000/";
	public static final String OW_MEDIA_URL = "http://192.168.1.27:5000/";
	//public static final String OW_MEDIA_URL = "https://capture.openwatch.net/";
	//public static final String OW_API_URL = "https://alpha.openwatch.net/api/";
	//public static final String OW_URL = "https://alpha.openwatch.net/";
	public static final String OW_VIEW = "v/";
	public static final String OW_LOGIN = "login_account";
	public static final String OW_SIGNUP = "create_account";
	public static final String OW_REGISTER = "register_app";
	public static final String OW_RECORDING = "recording";
	public static final String OW_STORY = "story";
	public static final String OW_TAGS = "tags";
	public static final String OW_UPDATE_META = "update_metadata";
	public static final String OW_FEED = "feed";
	
	// Feed names
	public static final String OW_LOCAL = "local";
	public static final String OW_FEATURED = "featured";
	public static final String OW_FOLLOWING = "following";
	// Feed types : Each is related to an API endpoint in OWServiceRequests getFeed
	public static enum OWFeedType{
		FEATURED, LOCAL, FOLLOWING
	}
	
	public static String feedEndpointFromType(OWFeedType type){
		String endpoint = "";
		switch(type){
		case FEATURED:
			endpoint = Constants.OW_FEATURED;
			break;
		case LOCAL:
			endpoint = Constants.OW_LOCAL;
			break;
		case FOLLOWING:
			endpoint = Constants.OW_FOLLOWING;
			break;
		}
		return endpoint;
	}
	
	// OpenWatch web service POST keys
	public static final String OW_EMAIL = "email_address";
	public static final String OW_PW = "password";
	public static final String OW_SIGNUP_TYPE = "signup_type";
	
	// OpenWatch web service response keys
	public static final String OW_SUCCESS = "success";
	public static final String OW_ERROR = "code";
	public static final String OW_REASON = "reason";
	public static final String OW_SERVER_ID = "id";
	public static final String OW_THUMB_URL = "thumbnail_url";
	public static final String OW_LAST_EDITED = "last_edited";
	public static final String OW_CREATION_TIME = "reported_creation_time";
	public static final String OW_ACTIONS = "clicks";
	public static final String OW_NO_VALUE = "None";
	public static final String OW_USER = "user";
	public static final String OW_VIEWS = "views";
	public static final String OW_TITLE = "title";
	public static final String OW_USERNAME = "username";
	public static final String OW_CLICKS = "clicks";
	public static final String OW_UUID = "uuid";
	public static final String OW_VIDEO_URL = "video_url";
	public static final String OW_FIRST_POSTED = "first_posted";
	public static final String OW_END_LOCATION = "end_location";
	public static final String OW_START_LOCATION = "start_location";
	public static final String OW_BLURB = "blurb";
	public static final String OW_SLUG = "slug";
	public static final String OW_BODY = "body";
	public static final String OW_NAME = "name";
	
	// OpenWatch media capture web service url and endpoints
	public static final String OW_MEDIA_START = "start";
	public static final String OW_MEDIA_END = "end";
	public static final String OW_MEDIA_UPLOAD = "upload";
	public static final String OW_MEDIA_HQ_UPLOAD = "upload_hq";
	public static final String OW_MEDIA_UPDATE_META = "update_metadata";
	
	// OpenWatch media capture web service POST keys
	public static final String OW_REC_START = "recording_start";
	public static final String OW_REC_END = "recording_end";
	public static final String OW_REC_UUID = "uuid";
	public static final String OW_ALL_FILES = "all_files";
	public static final String OW_UP_TOKEN = "upload_token";
	public static final String OW_FILE = "upload";
	public static final String OW_MEDIA_TITLE = "title";
	public static final String OW_DESCRIPTION = "description";
	public static final String OW_EDIT_TIME = "last_edited";
	public static final String OW_START_LOC = "start_location";
	public static final String OW_END_LOC = "end_location";
	public static final String OW_START_LAT = "start_lat";
	public static final String OW_START_LON = "start_lon";
	public static final String OW_END_LAT = "end_lat";
	public static final String OW_END_LON = "end_lon";
	public static final String OW_LAT = "latitude";
	public static final String OW_LON = "longitude";

}
