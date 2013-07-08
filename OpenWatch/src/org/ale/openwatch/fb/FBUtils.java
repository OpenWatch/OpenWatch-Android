package org.ale.openwatch.fb;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import com.facebook.*;
import com.facebook.model.GraphObject;
import org.ale.openwatch.OWUtils;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.model.OWServerObject;

import java.util.Arrays;
import java.util.List;

/**
 * Created by davidbrodsky on 7/3/13.
 */
public class FBUtils {
    private static final String TAG = "FBUtils";

    private static final int REAUTH_ACTIVITY_CODE = 100;
    private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");

    public static Session createSession(Context c, String applicationId) {
        Session activeSession = Session.getActiveSession();
        if (activeSession == null || activeSession.getState().isClosed()) {
            activeSession = new Session.Builder(c).setApplicationId(applicationId).build();
            Session.setActiveSession(activeSession);
        }
        return activeSession;
    }

    public static void postVideoAction(final FaceBookSessionActivity act, final int serverObjectId){
        //postVideoAction(act, OWServerObject.objects(((Activity)act), OWServerObject.class).get(serverObjectId));
        if (act.getSession().isOpened()) {
            Log.i(TAG, "Session is Open");
            FBUtils.postVideoAction(act, OWServerObject.objects(((Activity) act), OWServerObject.class).get(serverObjectId));
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
                        FBUtils.postVideoAction(act, OWServerObject.objects(((Activity) act), OWServerObject.class).get(serverObjectId));
                    }else{
                        Log.e(TAG, "session not opened");
                    }

                }
            };
            act.setPendingRequest(true);
            act.getSession().openForPublish(new Session.OpenRequest((Activity)act).setPermissions(PERMISSIONS).setCallback(callback));
        }
    }

    private static void postVideoAction(final FaceBookSessionActivity act, final OWServerObject serverObject){
        act.setPendingRequest(false);
        //requestPublishPermissions((Activity) act, Session.getActiveSession());

        AsyncTask<Void, Void, Response> task = new AsyncTask<Void, Void, Response>() {

            @Override
            protected Response doInBackground(Void... voids) {

                /*
                EatAction eatAction = GraphObject.Factory.create(EatAction.class);
                for (BaseListElement element : listElements) {
                    element.populateOGAction(eatAction);
                }
                Request request = new Request(Session.getActiveSession(),
                        POST_ACTION_PATH, null, HttpMethod.POST);
                request.setGraphObject(eatAction);
                */
                //
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
                    //handleError(response.getError());
                }
            }
        };

        task.execute();
    }


    /**
     * Used to inspect the response from posting an action
     */
    private interface PostResponse extends GraphObject {
        String getId();
    }

    private static void requestPublishPermissions(Activity act, Session session) {
        if (session != null) {
            Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(act, PERMISSIONS)
                    // demonstrate how to set an audience for the publish permissions,
                    // if none are set, this defaults to FRIENDS
                    .setDefaultAudience(SessionDefaultAudience.FRIENDS)
                    .setRequestCode(REAUTH_ACTIVITY_CODE);
            session.requestNewPublishPermissions(newPermissionsRequest);
        }
    }

    public interface FaceBookSessionActivity{
        public boolean getPendingRequest();
        public Session getSession();
        public void setSession(Session session);
        public void setPendingRequest(boolean pendingRequest);
    }
}
