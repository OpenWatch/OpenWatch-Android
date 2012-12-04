package net.openwatch.reporter.constants;

import java.util.regex.Pattern;

/**
 * A class containing values that are not user-facing and so
 * are not intended for translation.
 * All user-facing values are contained in res/values/
 * @author davidbrodsky
 *
 */
public class Constants {
	// SharedPreferences titles
	public static final String PROFILE_PREFS = "Profile";
	
	// External storage 
	public static final String ROOT_OUTPUT_DIR = "OpenWatch";
	public static final String RECORDING_OUTPUT_DIR = "recordings";
	
	// User profile keys. Used for SharedPreferences and Intent Extras
	public static final String EMAIL = "email";
	public static final String AUTHENTICATED = "authenticated";
	
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
	
	// OpenWatch web service root url and endpoints
	//public static final String OW_URL = "http://www.openwatch.net/api/"; // TODO: HTTPS
	public static final String OW_URL = "http://192.168.1.27:8000/api/";
	public static final String OW_LOGIN = "login_account";
	public static final String OW_SIGNUP = "create_account";
	public static final String OW_REGISTER = "register_app";
	
	// OpenWatch web service POST keys
	public static final String OW_EMAIL = "email_address";
	public static final String OW_PW = "password";
	
	// OpenWatch web service response keys
	public static final String OW_SUCCESS = "success";

}
