package org.ale.openwatch;

import android.content.Context;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import org.json.JSONObject;

/**
 * Created by davidbrodsky on 9/4/13.
 */
public class Analytics {

    public static final String LOGIN_FAILED = "Login Failed - Bad Password";
    public static final String LOGIN_EXISTING = "Login Success - Existing Account";
    public static final String LOGIN_NEW = "Login Success - New Account";
    public static final String LOGIN_ATTEMPT = "Login Attempt";
    public static final String ONBOARD_COMPLETE = "Onboarding Complete";
    public static final String AGENT = "agent";
    public static final String VIEWING_FANCY_LOGIN = "Viewing Fancy Login Screen";
    public static final String VIEWING_LOGIN = "Viewing Login Screen";
    public static final String SESSION_EXPIRED = "Session Expired";

    public static final String SELECTED_FEED = "Selected Feed";
    public static final String FEED = "Feed";

    public static final String POST_VIDEO = "Post Video";
    public static final String OW_PUBLIC = "ow";
    public static final String TO_FB = "fb";
    public static final String TO_TWITTER = "twitter";

    private static MixpanelAPI mixpanelAPI;

    private static MixpanelAPI getInstance(Context c){
        if(mixpanelAPI == null)
            mixpanelAPI = MixpanelAPI.getInstance(c, SECRETS.MIXPANEL_KEY);
        return mixpanelAPI;
    }

    public static void cleanUp(){
        if(mixpanelAPI != null)
            mixpanelAPI.flush();
    }

    public static void trackEvent(Context c, String name, JSONObject event){
        getInstance(c).track(name, event);
    }

    public static void registerProperty(Context c, JSONObject properties){
        getInstance(c).registerSuperProperties(properties);
    }

    public static void identifyUser(Context c, String id){
        getInstance(c).identify(id);
    }
}
