package org.ale.openwatch.constants;

import org.ale.openwatch.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * A class containing values that are not user-facing and so
 * are not intended for translation.
 * All user-facing values are contained in res/values/
 * @author davidbrodsky
 *
 */
public class Constants {

    public static final String SUPPORT_EMAIL = "team@openwatch.net";
    public static final String GOOGLE_STORE_URL = "https://play.google.com/store/apps/details?id=org.ale.openwatch";
    public static final String APPLE_STORE_URL = "https://itunes.apple.com/us/app/openwatch-social-muckraking/id642680756?mt=8";

    // Facebook
    public static final String FB_APP_ID = "297496017037529";

    // Twitter
    public static final String TWITTER_CONSUMER_KEY = "rRMW0cVIED799WgbeoA";
	
	// Set this flag to toggle between production 
	// and development endpoint addresses
	public static final boolean USE_DEV_ENDPOINTS = false;
	
	public static final String PROD_HOST = "https://openwatch.net/";
	public static final String PROD_CAPTURE_HOST = "https://capture.openwatch.net/";

    public static final String DEV_HOST = "https://staging.openwatch.net/";
    public static final String DEV_CAPTURE_HOST = "https://capture-staging.openwatch.net/";

	//public static final String DEV_HOST = "http://192.168.1.27:8000/";
    //public static final String DEV_CAPTURE_HOST = "http://192.168.1.27:5000/";

    public static final String PASSWORD_RESET_ENDPOINT = "accounts/password/reset/";
	
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
	public static enum CONTENT_TYPE { VIDEO, PHOTO, AUDIO, STORY, INVESTIGATION, MISSION };
	// for fileUtils and OWServiceRequests. TODO Delete following
	//public static enum MEDIA_TYPE { VIDEO, PHOTO, AUDIO };
	//public static HashMap<MEDIA_TYPE, String> API_ENDPOINT_BY_MEDIA_TYPE = new HashMap<MEDIA_TYPE, String>() {{put(MEDIA_TYPE.VIDEO, "v"); put(MEDIA_TYPE.PHOTO, "p"); put(MEDIA_TYPE.AUDIO, "a"); }};
	public static HashMap<CONTENT_TYPE, String> API_ENDPOINT_BY_CONTENT_TYPE = new HashMap<CONTENT_TYPE, String>() {{ put(CONTENT_TYPE.VIDEO, "v"); put(CONTENT_TYPE.PHOTO, "p"); put(CONTENT_TYPE.AUDIO, "a");put(CONTENT_TYPE.MISSION, "mission"); put(CONTENT_TYPE.INVESTIGATION, "i"); put(CONTENT_TYPE.STORY, "s"); }};
	public static final String OW_CONTENT_TYPE = "owcontent_type";
	
	// Date Formatter for OW server time
	public static SimpleDateFormat utc_formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
	// Human readable
	public static SimpleDateFormat user_date_formatter = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
	public static SimpleDateFormat user_datetime_formatter = new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.US);
	public static SimpleDateFormat user_time_formatter = new SimpleDateFormat("h:mm a", Locale.US);
	
	static{
		utc_formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		user_datetime_formatter.setTimeZone(TimeZone.getDefault());
		user_time_formatter.setTimeZone(TimeZone.getDefault());
		user_date_formatter.setTimeZone(TimeZone.getDefault());
	}
	
	public static final String USER_AGENT_BASE = "OpenWatch/";
	
	static{
		
	}

	// SharedPreferences titles
	public static final String PROFILE_PREFS = "Profile";
    public static final String GCM_PREFS = "gcm";
	
	// External storage 
	public static final String ROOT_OUTPUT_DIR = "OpenWatch";
	public static final String VIDEO_OUTPUT_DIR = "video";
	public static final String PHOTO_OUTPUT_DIR = "photo";
	public static final String AUDIO_OUTPUT_DIR = "audio";
	
	// User profile keys. Used for SharedPreferences and Intent Extras
	public static final String EMAIL = "email";
	public static final String AUTHENTICATED = "authenticated";
	public static final String DB_READY = "db_ready";
	public static final String PUB_TOKEN = "public_upload_token"; 		// used for profile and to parse server login response
	public static final String PRIV_TOKEN = "private_upload_token";		// used for profile and to parse server login response
	public static final String REGISTERED = "registered"; 
	public static final String VIEW_TAG_MODEL = "model";		// key set on listview item holding corresponding model pk
	public static final String INTERNAL_DB_ID = "id";
    public static final String SERVER_ID = "server_id";
	public static final String INTERNAL_USER_ID = "id";
	public static final String IS_LOCAL_RECORDING = "is_local";
	public static final String IS_USER_RECORDING = "is_user_recording";
	public static final String FEED_TYPE = "feed_type";
    public static final String OBLIGATORY_TAG = "tag";
    public static final String MISSION_TIP = "mtip";
    public static final int CAMERA_ACTION_CODE = 444;
    public static final String TWITTER_TOKEN = "ttoken";
    public static final String TWITTER_SECRET = "tsecret";
    public static final String LAST_MISSION_DATE = "last_mission_date";
    public static final String LAST_MISSION_ID = "last_mission_id";
    public static final String JOINED_FIRST_MISSION = "joined_first_mission";
    public static final String MISSION_SERVER_OBJ_ID = "msid";
    public static final String VICTORY = "victory";

	
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
    public static final String OW_FEATURED_MEDIA = "featured_media";
	public static final String OW_STORY = "story";
	public static final String OW_TAGS = "tags";
	public static final String OW_TAG = "tag";
	public static final String OW_UPDATE_META = "update_metadata";
	public static final String OW_FEED = "feed";
	
	// Feed names
	public static final String OW_LOCAL = "local";
	public static final String OW_FEATURED = "featured";
	public static final String OW_FOLLOWING = "following";
    public static final String OW_RAW = "raw";
	// Feed types : Each is related to an API endpoint in OWServiceRequests getFeed
	/*
    public static enum OWFeedType{
		TOP, LOCAL, FOLLOWING, USER, RAW
	}
	*/
    public static enum OWFeedType{
        TOP, LOCAL, USER, RAW, MISSION, FEATURED_MEDIA
    }

    public static HashMap<String, Integer> FEED_TO_TITLE = new HashMap<String, Integer>() {{put(OWFeedType.FEATURED_MEDIA.toString().toLowerCase(), R.string.tab_featured_media); put(OWFeedType.MISSION.toString().toLowerCase(), R.string.tab_missions); put(OWFeedType.TOP.toString().toLowerCase(), R.string.tab_featured); put(OWFeedType.LOCAL.toString().toLowerCase(), R.string.tab_local); put(OWFeedType.RAW.toString().toLowerCase(), R.string.tab_Raw); put(OWFeedType.USER.toString().toLowerCase(), R.string.tab_local_user_recordings); }};
	
	public static ArrayList<String> OW_FEEDS = new ArrayList<String>();
	static{
		OWFeedType[] feed_types = OWFeedType.values();
		for(int x=0; x< feed_types.length; x++){
			OW_FEEDS.add(feed_types[x].toString().toLowerCase());
		}
	}
	
	public static boolean isOWFeedTypeGeoSensitive(String feed_type){
        if(feed_type == null)
            return false;
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
		/*
		String endpoint = "";

		switch(type){
		case USER:
			endpoint = Constants.OW_API_URL + Constants.OW_RECORDINGS;
			break;
		default:
			endpoint = Constants.OW_API_URL + Constants.OW_FEED + File.separator + feedInternalEndpointFromType(type);
			break;
		}
		return endpoint + File.separator + String.valueOf(page);
		*/
		return feedExternalEndpointFromString(type.toString().toLowerCase(), page);
	}
	
	public static String feedExternalEndpointFromString(String type, int page){
		String endpoint = "";
		if(!OW_FEEDS.contains(type) ){
            // tag feed
			endpoint = Constants.OW_API_URL + Constants.OW_TAG + File.separator + "?tag="+ type + "&page=" + String.valueOf(page);
		}else{
            if(type.equals("top")){
                endpoint = Constants.OW_API_URL + API_ENDPOINT_BY_CONTENT_TYPE.get(CONTENT_TYPE.INVESTIGATION) + "/?page=" + String.valueOf(page);
            }else if(type.equals("mission")){
                endpoint = Constants.OW_API_URL + API_ENDPOINT_BY_CONTENT_TYPE.get(CONTENT_TYPE.MISSION) + "/?page=" + String.valueOf(page);
            }else
			    endpoint = Constants.OW_API_URL + Constants.OW_FEED + File.separator + "?type="+ type + "&page=" + String.valueOf(page);
		}
		
		return endpoint;
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
		case TOP:
			endpoint = Constants.OW_FEATURED;
			break;
		case LOCAL:
			endpoint = Constants.OW_LOCAL;
			break;
		//case FOLLOWING:
		//	endpoint = Constants.OW_FOLLOWING;
		//	break;
		case USER:
			endpoint = Constants.OW_RECORDINGS;
			break;
        case FEATURED_MEDIA:
            endpoint = Constants.OW_FEATURED_MEDIA;
            break;
        case RAW:
                endpoint = Constants.OW_RAW;
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
    public static final String OW_FIRST_NAME = "first_name";
    public static final String OW_LAST_NAME = "last_name";
	public static final String OW_CLICKS = "clicks";
	public static final String OW_UUID = "uuid";
	public static final String OW_VIDEO_URL = "video_url";
	public static final String OW_FIRST_POSTED = "first_posted";
	public static final String OW_END_LOCATION = "end_location";
	public static final String OW_START_LOCATION = "start_location";
	public static final String OW_BLURB = "bio";
	public static final String OW_SLUG = "slug";
	public static final String OW_BODY = "body";
	public static final String OW_NAME = "name";
    public static final String OW_EXPIRES = "expires";
    public static final String OW_AGENTS = "agents";
    public static final String OW_SUBMISSIONS = "submissions";
	
	// OpenWatch media capture web service url and endpoints
	public static final String OW_MEDIA_START = "start";
	public static final String OW_MEDIA_END = "end";
	public static final String OW_MEDIA_UPLOAD = "upload";
	public static final String OW_MEDIA_HQ_UPLOAD = "upload_hq";
	public static final String OW_MEDIA_UPDATE_META = "update_metadata";

    public static final String OW_HQ_FILENAME = "hq.mp4";
	
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
    public static final String OW_USD = "usd";
    public static final String OW_KARMA = "karma";
    public static final String OW_ACTIVE = "active";
    public static final String OW_COMPLETED = "completed";
    public static final String OW_MEDIA_BUCKET = "media_url";
	
	// Hit counts
	public static enum HIT_TYPE { VIEW, CLICK };
	public static final String OW_STATUS = "status";
	public static final String OW_HIT_URL = "increase_hitcount";
	public static final String OW_HIT_SERVER_ID = "serverID";
	public static final String OW_HIT_MEDIA_TYPE = "media_type";
	public static final String OW_HIT_TYPE = "hit_type";
	public static final String OW_HITS = "hits";

    // BroadcastReceiver Intent filter
    public static final String OW_SYNC_STATE_FILTER = "server_object_sync";
    public static final String OW_SYNC_STATE_STATUS = "status";
    public static final String OW_SYNC_STATE_MODEL_ID = "model_id";
    public static final String OW_SYNC_STATE_CHILD_ID = "child_model_id";
    public static final int OW_SYNC_STATUS_BEGIN = 0;
    public static final int OW_SYNC_STATUS_BEGIN_BULK = 10;
    public static final int OW_SYNC_STATUS_END_BULK = 20;
    public static final int OW_SYNC_STATUS_FAILED = -1;
    public static final int OW_SYNC_STATUS_SUCCESS = 1;

    // General
    public static final String USD = "$";

}
