package org.ale.openwatch;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.orm.androrm.Filter;
import com.orm.androrm.Model;
import com.orm.androrm.QuerySet;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.constants.DBConstants;
import org.ale.openwatch.http.OWMediaRequests;
import org.ale.openwatch.http.OWServiceRequests;
import org.ale.openwatch.model.OWLocalVideoRecording;
import org.ale.openwatch.model.OWPhoto;
import org.ale.openwatch.model.OWServerObject;
import org.ale.openwatch.model.OWServerObjectInterface;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by davidbrodsky on 6/5/13.
 */
public class OWMediaSyncer {
    private static final String TAG = "OWMediaSyncer";

    private static ArrayList<OWServerObjectInterface> objectsToSync;

    static JsonHttpResponseHandler get_handler;

    public static void syncMedia(final Context c){
        if(objectsToSync == null)
            objectsToSync = new ArrayList<OWServerObjectInterface>();

        Filter filter = new Filter();
        filter.is(DBConstants.SYNCED, 0);
        QuerySet<OWPhoto> unSyncedPhotos = OWPhoto.objects(c, OWPhoto.class).filter(filter);

        filter = new Filter();
        filter.is(DBConstants.LOCAL_RECORDINGS_HQ_SYNCED, 0);
        QuerySet<OWLocalVideoRecording> unSyncedRecordings = OWLocalVideoRecording.objects(c, OWLocalVideoRecording.class).filter(filter);

        for(OWPhoto photo : unSyncedPhotos){
            objectsToSync.add(photo);
        }
        for(OWLocalVideoRecording video : unSyncedRecordings){
            objectsToSync.add(video);
        }

        Log.i(TAG, String.format("Found %d unsynced objects", objectsToSync.size()));

        get_handler = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.i(TAG, "getMeta response: " + response.toString());
                if (response.has("id") && !response.has("media_url")) {
                    // send media
                    Log.i(TAG, "Sending media for object");
                    OWServerObjectInterface object = getCurrentObject();
                    SharedPreferences prefs = c.getSharedPreferences(Constants.PROFILE_PREFS, c.MODE_PRIVATE);
                    String public_upload_token = prefs.getString(Constants.PUB_TOKEN, "");
                    if(object.getMediaType(c) == Constants.MEDIA_TYPE.VIDEO){
                        OWMediaRequests.end(c, public_upload_token, ((OWLocalVideoRecording)object).recording.get(c));
                        OWMediaRequests.safeSendHQFile(c, public_upload_token, object.getUUID(c), object.getMediaFilepath(c), ((Model)object).getId());
                    }else{
                        OWServiceRequests.sendOWMobileGeneratedObjectMedia(c, getCurrentObject());
                    }
                }
            }

            @Override
            public void onFailure(Throwable e, String response) {
                //Log.i(TAG, "getRecording failed. let's try creating object: " + response);
                //TODO: We should confirm the response status code is 404
                if(getCurrentObject().getMediaType(c) == Constants.MEDIA_TYPE.VIDEO){
                    new Thread(){
                        public void run(){
                            OWServerObjectInterface object = getCurrentObject();
                            SharedPreferences prefs = c.getSharedPreferences(Constants.PROFILE_PREFS, c.MODE_PRIVATE);
                            String public_upload_token = prefs.getString(Constants.PUB_TOKEN, "");
                            OWMediaRequests.start(c, public_upload_token, object.getUUID(c), "");
                            OWMediaRequests.end(c, public_upload_token, ((OWLocalVideoRecording)object).recording.get(c));
                            OWMediaRequests.safeSendHQFile(c, public_upload_token, object.getUUID(c), object.getMediaFilepath(c), ((Model)object).getId());
                        }
                    }.start();

                }else
                    OWServiceRequests.createOWServerObject(c, getCurrentObject(), null);
                e.printStackTrace();
            }

            @Override
            public void onFinish() {
                Log.i(TAG, "getRecording finish");
                if(objectsToSync != null && objectsToSync.size() > 1){
                    objectsToSync.remove(objectsToSync.size()-1);
                    processNextObject(c, get_handler);
                }else if(objectsToSync.size() == 1){
                    objectsToSync = null;
                }
            }

        };

        if(getCurrentObject() != null)
            OWServiceRequests.getOWServerObjectMeta(c, getCurrentObject(), "", get_handler);

    }

    private static void processNextObject(Context c, JsonHttpResponseHandler get_handler){
        if(objectsToSync != null)
            OWServiceRequests.getOWServerObjectMeta(c, getCurrentObject(), "", get_handler);
    }

    private static OWServerObjectInterface getCurrentObject(){
        if(objectsToSync != null && objectsToSync.size() > 0)
            return objectsToSync.get(objectsToSync.size()-1);
        return null;
    }


}