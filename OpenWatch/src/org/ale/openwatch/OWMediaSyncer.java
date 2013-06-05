package org.ale.openwatch;

import android.content.Context;
import android.util.Log;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.orm.androrm.Filter;
import com.orm.androrm.QuerySet;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.constants.DBConstants;
import org.ale.openwatch.http.OWServiceRequests;
import org.ale.openwatch.model.OWLocalVideoRecording;
import org.ale.openwatch.model.OWPhoto;
import org.ale.openwatch.model.OWServerObject;
import org.ale.openwatch.model.OWServerObjectInterface;
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
        QuerySet<OWLocalVideoRecording> unSyncedRecordings = OWLocalVideoRecording.objects(c, OWLocalVideoRecording.class).filter(filter);

        for(OWPhoto photo : unSyncedPhotos){
            objectsToSync.add(photo);
        }
        for(OWLocalVideoRecording video : unSyncedRecordings){
            objectsToSync.add(video);
        }

        get_handler = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.i(TAG, "getMeta response: " + response.toString());
                if (response.has("id") && !response.has("media_url")) {
                    // send media
                    OWServiceRequests.sendOWMobileGeneratedObjectMedia(c, getCurrentObject());
                }
            }

            @Override
            public void onFailure(Throwable e, String response) {
                Log.i(TAG, "getRecording failed. let's try creating object: " + response);
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