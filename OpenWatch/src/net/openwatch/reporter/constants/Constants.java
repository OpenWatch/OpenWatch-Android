package net.openwatch.reporter.constants;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
	
	// Set this flag to toggle between production 
	// and development endpoint addresses
	public static final boolean USE_DEV_ENDPOINTS = false;
	
	public static final String PROD_HOST = "https://alpha.openwatch.net/";
	public static final String PROD_CAPTURE_HOST = "https://capture.openwatch.net/";
	
	public static final String DEV_HOST = "http://192.168.1.27:8000/";
	public static final String DEV_CAPTURE_HOST = "http://192.168.1.27:5000/";
	
	// OpenWatch web service root url and endpoints
	public static final String OW_MEDIA_URL;
	public static final String OW_API_URL;
	public static final String OW_URL;
	
	static {
		if(USE_DEV_ENDPOINTS){
			OW_MEDIA_URL = DEV_CAPTURE_HOST;
			OW_URL = DEV_HOST;
			OW_API_URL = DEV_HOST + "api/";
		}else{
			OW_MEDIA_URL = PROD_CAPTURE_HOST;
			OW_URL = PROD_HOST;
			OW_API_URL = PROD_HOST + "api/";
		}
	}
	
	// For view tag
	public static enum CONTENT_TYPE { VIDEO, STORY };
	public static final String OW_CONTENT_TYPE = "owcontent_type";
	
	// Date Formatter for OW server time
	public static SimpleDateFormat utc_formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
	// Human readable
	public static SimpleDateFormat user_datetime_formatter = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.US);
	public static SimpleDateFormat user_time_formatter = new SimpleDateFormat("h:mm a", Locale.US);
	
	static{
		utc_formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		user_datetime_formatter.setTimeZone(TimeZone.getDefault());
		user_time_formatter.setTimeZone(TimeZone.getDefault());
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
	public static final String INTERNAL_USER_ID = "id";
	public static final String IS_LOCAL_RECORDING = "is_local";
	public static final String IS_USER_RECORDING = "is_user_recording";
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
	
	// openwatch.net api endpoints
	public static final String OW_STORY_VIEW = "s/";
	public static final String OW_RECORDING_VIEW = "v/";
	public static final String OW_LOGIN = "login_account";
	public static final String OW_SIGNUP = "create_account";
	public static final String OW_REGISTER = "register_app";
	public static final String OW_RECORDING = "recording";
	public static final String OW_RECORDINGS = "recordings";
	public static final String OW_STORY = "story";
	public static final String OW_TAGS = "tags";
	public static final String OW_TAG = "tag";
	public static final String OW_UPDATE_META = "update_metadata";
	public static final String OW_FEED = "feed";
	
	// Feed names
	public static final String OW_LOCAL = "local";
	public static final String OW_FEATURED = "featured";
	public static final String OW_FOLLOWING = "following";
	// Feed types : Each is related to an API endpoint in OWServiceRequests getFeed
	public static enum OWFeedType{
		FEATURED, LOCAL, FOLLOWING, RECORDINGS
	}
	
	private static ArrayList<String> OW_FEEDS = new ArrayList<String>();
	static{
		OWFeedType[] feed_types = OWFeedType.values();
		for(int x=0; x< feed_types.length; x++){
			OW_FEEDS.add(feed_types[x].toString().toLowerCase());
		}
	}
	
	public static boolean isOWFeedTypeGeoSensitive(String feed_type){
		if(feed_type.trim().toLowerCase().compareTo(OWFeedType.LOCAL.toString().toLowerCase()) == 0)
			return true;
		return false;
	}
	
	/**
	 * To be removed
	 * For user with external OWServiceRequests
	 * @param type
	 * @return
	 */
	public static String feedExternalEndpointFromType(OWFeedType type, int page){
		String endpoint = "";
		
		switch(type){
		case RECORDINGS:
			endpoint = Constants.OW_API_URL + Constants.OW_RECORDINGS;
			break;
		default:
			endpoint = Constants.OW_API_URL + Constants.OW_FEED + File.separator + feedInternalEndpointFromType(type);
			break;
		}
		return endpoint + File.separator + String.valueOf(page);
	}
	
	public static String feedExternalEndpointFromString(String type, int page){
		String endpoint = "";
		if(type.compareTo(OWFeedType.RECORDINGS.toString().toLowerCase()) == 0){
			endpoint = Constants.OW_API_URL + Constants.OW_RECORDINGS;
		}else if(!OW_FEEDS.contains(type) ){
			endpoint = Constants.OW_API_URL + Constants.OW_TAG + File.separator + type;
		}else{
			endpoint = Constants.OW_API_URL + Constants.OW_FEED + File.separator + type;
		}
		
		return endpoint + File.separator + String.valueOf(page);
	}
	
	/**
	 * For user with the internal ContentProvider
	 * To remove
	 * @param type
	 * @return
	 */
	public static String feedInternalEndpointFromType(OWFeedType type){
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
		case RECORDINGS:
			endpoint = Constants.OW_RECORDINGS;
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
	
	//Hit counts
	public static enum HIT_TYPE { VIEW, CLICK };
	public static final String OW_STATUS = "status";
	public static final String OW_HIT_URL = "increase_hitcount";
	public static final String OW_HIT_SERVER_ID = "serverID";
	public static final String OW_HIT_MEDIA_TYPE = "media_type";
	public static final String OW_HIT_TYPE = "hit_type";
	public static final String OW_HITS = "hits";


}
