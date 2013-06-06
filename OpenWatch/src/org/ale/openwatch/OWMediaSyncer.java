package org.ale.openwatch;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
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
import org.ale.openwatch.model.OWServerObjectInterface;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by davidbrodsky on 6/5/13.
 */
public class OWMediaSyncer {
    private static final String TAG = "OWMediaSyncer";

    private static ExecutorService syncing_service = Executors.newSingleThreadExecutor();


    public static void syncMedia(final Context c){

        Filter filter = new Filter();
        filter.is(DBConstants.SYNCED, 0);
        QuerySet<OWPhoto> unSyncedPhotos = OWPhoto.objects(c, OWPhoto.class).filter(filter);

        filter = new Filter();
        filter.is(DBConstants.LOCAL_RECORDINGS_HQ_SYNCED, 0);
        QuerySet<OWLocalVideoRecording> unSyncedRecordings = OWLocalVideoRecording.objects(c, OWLocalVideoRecording.class).filter(filter);

        SyncTask syncTask;
        int numTasks = 0;
        for(OWPhoto photo : unSyncedPhotos){
            //objectsToSync.add(photo);
            syncTask = new SyncTask(c, photo);
            syncing_service.submit(syncTask);
            numTasks++;
        }
        for(OWLocalVideoRecording video : unSyncedRecordings){
            //objectsToSync.add(video);
            syncTask = new SyncTask(c, video);
            syncing_service.submit(syncTask);
            numTasks++;
        }
        syncing_service.shutdown();

        if(numTasks > 0){
            Log.d(TAG, "Broadcasting background sync begin");
            Intent intent = new Intent("server_object_sync");
            // You can also include some extra data.
            intent.putExtra("status", 1);
            intent.putExtra("child_model_id", ((Model)object).getId());
            LocalBroadcastManager.getInstance(app_context).sendBroadcast(intent);
        }

        Log.i(TAG, String.format("Found %d unsynced objects", numTasks));
    }

    // encoding_task = new EncoderTask(ffencoder, video_frame_data, audio_samples);
    //encoding_service.submit(encoding_task);
    //encoding_service.shutdown();

    private static class SyncTask implements Runnable{
        private static final String TAG = "OWMediaSyncer";

        OWServerObjectInterface object;
        Context c;

        public SyncTask(Context c, OWServerObjectInterface object){
            this.object = object;
            this.c = c;
        }

        @Override
        public void run() {
            if(object != null){
                Log.i(TAG, String.format("Syncing %s with id %d", object.getMediaType(c).toString(), ((Model)object).getId()));
                OWServiceRequests.getOWServerObjectMeta(c, object, "", get_handler);
            }

        }

        JsonHttpResponseHandler get_handler = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.i(TAG, "getMeta response: " + response.toString());
                if (response.has("id") && !response.has("media_url")) {
                    // send binary media
                    Log.i(TAG, String.format("sending %s media with id %d", object.getMediaType(c).toString(), ((Model)object).getId()));
                    if(object.getMediaType(c) == Constants.MEDIA_TYPE.VIDEO){
                        Log.i(TAG, "Object is type Video");
                        new Thread(){
                            public void run(){
                                SharedPreferences prefs = c.getSharedPreferences(Constants.PROFILE_PREFS, c.MODE_PRIVATE);
                                String public_upload_token = prefs.getString(Constants.PUB_TOKEN, "");
                                OWMediaRequests.end(c, public_upload_token, ((OWLocalVideoRecording)object).recording.get(c));
                                OWMediaRequests.safeSendHQFile(c, public_upload_token, object.getUUID(c), object.getMediaFilepath(c), ((Model)object).getId());
                            }
                        }.start();

                    }else{ // send photo
                        OWServiceRequests.sendOWMobileGeneratedObjectMedia(c, object);
                    }
                }else if (response.has("id") && response.has("media_url")) {
                    if(object.getMediaType(c) == Constants.MEDIA_TYPE.VIDEO){
                        // server has media_url. if it's not hq, send hq. if it is, set local recording as synced
                        try {
                            if(response.getString("media_url").contains(Constants.OW_HQ_FILENAME)){
                                // hq is already synced
                                Log.i(TAG, String.format("%s with id %d is already synced. marking local as such", object.getMediaType(c).toString(), ((Model)object).getId()));
                                object.setSynced(c, true);
                            }else{
                                // non hq synced
                                new Thread(){
                                    public void run(){
                                        Log.i(TAG, String.format("sending %s hq media with id %d", object.getMediaType(c).toString(), ((Model)object).getId()));
                                        SharedPreferences prefs = c.getSharedPreferences(Constants.PROFILE_PREFS, c.MODE_PRIVATE);
                                        String public_upload_token = prefs.getString(Constants.PUB_TOKEN, "");
                                        OWMediaRequests.safeSendHQFile(c, public_upload_token, object.getUUID(c), object.getMediaFilepath(c), ((Model)object).getId());
                                    }
                                }.start();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }else{ // object is synced
                        object.setSynced(c, true);
                    }
                }
            }

            @Override
            public void onFailure(Throwable e, String response) {
                Log.i(TAG, "getRecording failed. let's try creating object: " + response);
                //TODO: We should confirm the response status code is 404
                if(object.getMediaType(c) == Constants.MEDIA_TYPE.VIDEO){
                    new Thread(){
                        public void run(){
                            SharedPreferences prefs = c.getSharedPreferences(Constants.PROFILE_PREFS, c.MODE_PRIVATE);
                            String public_upload_token = prefs.getString(Constants.PUB_TOKEN, "");
                            OWMediaRequests.start(c, public_upload_token, object.getUUID(c), "");
                            OWMediaRequests.end(c, public_upload_token, ((OWLocalVideoRecording)object).recording.get(c));
                            OWMediaRequests.safeSendHQFile(c, public_upload_token, object.getUUID(c), object.getMediaFilepath(c), ((Model)object).getId());
                        }
                    }.start();

                }else
                    OWServiceRequests.createOWServerObject(c, object, null);
                e.printStackTrace();
            }

            @Override
            public void onFinish() {
                Log.i(TAG, "getRecording finish");
            }

        };
    }


}