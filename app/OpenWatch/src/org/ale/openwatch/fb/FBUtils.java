package org.ale.openwatch.fb;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import com.facebook.*;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import org.ale.openwatch.OWUtils;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.model.OWServerObject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by davidbrodsky on 7/3/13.
 */
public class FBUtils {
    private static final String TAG = "FBUtils";

    private static final int REAUTH_ACTIVITY_CODE = 100;
    private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");

    public interface FacebookAuthCallback{
        public void onAuth();
    }

    public static Session createSession(Context c, String applicationId) {
        Session activeSession = Session.getActiveSession();
        if (activeSession == null || activeSession.getState().isClosed()) {
            activeSession = new Session.Builder(c).setApplicationId(applicationId).build();
            Session.setActiveSession(activeSession);
        }
        return activeSession;
    }

    public static void authenticate(final FaceBookSessionActivity act, final FacebookAuthCallback cb){
        if (act.getSession().isOpened()) {
            Log.i(TAG, "Session is Open");
            if(cb != null)
                cb.onAuth();
        } else {
            Log.i(TAG, "Session is not Open, Doing that now");
            Session.StatusCallback callback = new Session.StatusCallback() {
                public void call(Session session, SessionState state, Exception exception) {
                    if (exception != null) {
                        /*
                        new AlertDialog.Builder(((Activity)act))
                                .setTitle(R.string.login_failed_dialog_title)
                                .setMessage(exception.getMessage())
                                .setPositiveButton(R.string.dialog_ok, null)
                                .show();
                        */
                        act.setSession(FBUtils.createSession((Activity)act, Constants.FB_APP_ID));
                    }
                    else if(session.isOpened()){
                        Log.i(TAG, "Opened Session successfully!");
                        if(cb != null)
                            cb.onAuth();
                    }else{
                        Log.e(TAG, "session not opened");
                    }

                }
            };
            act.setPendingRequest(true);
            act.getSession().openForPublish(new Session.OpenRequest((Activity)act).setPermissions(PERMISSIONS).setCallback(callback));
        }

    }

    /**
     *
     * @param act
     * @param serverObjectId
     */
    public static void authenticateAndPostVideoAction(final FaceBookSessionActivity act, final int serverObjectId){
        FacebookAuthCallback cb = new FacebookAuthCallback() {
            @Override
            public void onAuth() {
                FBUtils.postVideoAction(act, serverObjectId);
            }
        };
        authenticate(act, cb);
    }

    /**
     * Execute request to create Facebook "Post a Video" Action given an OWServerObject
     * @param act the initiating FaceBookSessionActivity
     * @param serverObjectId id of a OWServerObject related to a OWVideoRecording
     */
    public static void postVideoAction(final FaceBookSessionActivity act, final int serverObjectId){
        final OWServerObject serverObject = OWServerObject.objects(((Activity) act), OWServerObject.class).get(serverObjectId);

        if(serverObject == null || serverObject.getContentType(((Activity)act).getApplicationContext()) != Constants.CONTENT_TYPE.VIDEO)
            return;
        act.setPendingRequest(false);
        //requestPublishPermissions((Activity) act, Session.getActiveSession());
        AsyncTask<Void, Void, Response> task = new AsyncTask<Void, Void, Response>() {

            @Override
            protected Response doInBackground(Void... voids) {
                Bundle params = new Bundle();
                params.putString("type", "video.other");
                params.putBoolean("fb:explicitly_shared", true);
                //params.putString("url", "https://openwatch.net/v/1074/");
                params.putString("other", OWUtils.urlForOWServerObject(serverObject, ((Activity) act).getApplicationContext()));
                //params.putString("title", serverObject.title.get());
                params.putString("description", "");

                Request request = new Request(
                        Session.getActiveSession(),
                        "me/openwatch:post_a_video",
                        params,
                        HttpMethod.POST
                );
                act.setPendingRequest(false);
                return request.executeAndWait();
            }

            @Override
            protected void onPostExecute(Response response) {
                //onPostActionResponse(response);
                PostResponse postResponse = response.getGraphObjectAs(PostResponse.class);
                if (postResponse != null && postResponse.getId() != null) {
                    Log.i(TAG, response.toString());

                } else {
                    Log.e(TAG, response.getError().toString());
                    act.onFBError(response.getError().toString());
                    //handleError(response.getError());
                }
            }
        };

        task.execute();
    }

    public interface FacebookUserCallback{
        public void gotProfile(GraphUser user);
    }

    public static void getProfile(final Session session, final FacebookUserCallback cb) {
        // Make an API call to get user data and define a
        // new callback to handle the response.
        Request request = Request.newMeRequest(session,
                new Request.GraphUserCallback() {
                    @Override
                    public void onCompleted(GraphUser user, Response response) {
                        // If the response is successful
                        if (session == Session.getActiveSession()) {
                            if (user != null) {
                                if(cb != null)
                                    cb.gotProfile(user);
                            }
                        }
                        if (response.getError() != null) {
                            // Handle errors, will do so later.
                        }
                    }
                });
        request.executeAsync();
    }


    /**
     * Used to inspect the response from posting an action
     */
    private interface PostResponse extends GraphObject {
        String getId();
    }


    public interface FaceBookSessionActivity{
        public void onFBError(String response);
        public boolean getPendingRequest();
        public Session getSession();
        public void setSession(Session session);
        public void setPendingRequest(boolean pendingRequest);
    }
}
