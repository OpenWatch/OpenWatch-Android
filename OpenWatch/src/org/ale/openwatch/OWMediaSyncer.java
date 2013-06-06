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

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by davidbrodsky on 6/5/13.
 */
public class OWMediaSyncer {
    private static final String TAG = "OWMediaSyncer";

    private static ExecutorService syncing_service = Executors.newSingleThreadExecutor();

    public static boolean syncing = false;


    public static void syncMedia(final Context c){

        Filter filter = new Filter();
        filter.is(DBConstants.SYNCED, 0);
        QuerySet<OWPhoto> unSyncedPhotos = OWPhoto.objects(c, OWPhoto.class).filter(filter);

        filter = new Filter();
        filter.is(DBConstants.LOCAL_RECORDINGS_HQ_SYNCED, 0);
        QuerySet<OWLocalVideoRecording> unSyncedRecordings = OWLocalVideoRecording.objects(c, OWLocalVideoRecording.class).filter(filter);

        ArrayList<Runnable> tasks = new ArrayList<Runnable>();
        SyncTask syncTask;
        for(OWPhoto photo : unSyncedPhotos){
            //objectsToSync.add(photo);
            syncTask = new SyncTask(c, photo, false);
            tasks.add(syncTask);
        }
        for(OWLocalVideoRecording video : unSyncedRecordings){
            //objectsToSync.add(video);
            syncTask = new SyncTask(c, video, false);
            tasks.add(syncTask);
        }
        if(tasks.size() > 0){
            ((SyncTask)tasks.get(tasks.size()-1)).finalTask = true;
            for(Runnable task : tasks){
                syncing_service.submit(task);
            }
        }else
            syncing = false;

        syncing_service.shutdown();

        // set syncing false

        Log.i(TAG, String.format("Found %d unsynced objects", tasks.size()));
    }

    // encoding_task = new EncoderTask(ffencoder, video_frame_data, audio_samples);
    //encoding_service.submit(encoding_task);
    //encoding_service.shutdown();

    private static class SyncTask implements Runnable{
        private static final String TAG = "OWMediaSyncer";

        OWServerObjectInterface object;
        Context c;
        public boolean finalTask = false;

        public SyncTask(Context c, OWServerObjectInterface object, boolean finalTask){
            this.object = object;
            this.c = c;
            this.finalTask = finalTask;
        }

        @Override
        public void run() {
            if(object != null){
                Log.i(TAG, String.format("Syncing %s with id %d", object.getMediaType(c).toString(), ((Model)object).getId()));
                OWServiceRequests.getOWServerObjectMeta(c, object, "", get_handler);
            }

        }

        OWServiceRequests.RequestCallback mediaSyncRequestCallback = new OWServiceRequests.RequestCallback() {
            @Override
            public void onFailure() {
                if(finalTask){
                    Log.i(TAG, "broadcasting bulk sync finish (failed)");
                    broadcastMessage(c, Constants.OW_SYNC_STATUS_END_BULK);
                }
            }

            @Override
            public void onSuccess() {
                if(finalTask){
                    Log.i(TAG, "broadcasting bulk sync finish");
                    broadcastMessage(c, Constants.OW_SYNC_STATUS_END_BULK);
                }
            }
        };

        JsonHttpResponseHandler get_handler = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.i(TAG, "getMeta response: " + response.toString());
                if (response.has("id"))
                    broadcastMessage(c, Constants.OW_SYNC_STATUS_BEGIN_BULK);
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
                                OWMediaRequests.safeSendHQFile(c, public_upload_token, object.getUUID(c), object.getMediaFilepath(c), ((Model)object).getId(), mediaSyncRequestCallback);
                                OWServiceRequests.syncOWServerObject(c, ((OWLocalVideoRecording)object).recording.get(c).media_object.get(c));
                            }
                        }.start();

                    }else{ // if object is not video, send media to django
                        OWServiceRequests.sendOWMobileGeneratedObjectMedia(c, object, mediaSyncRequestCallback);
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
                                Log.i(TAG, String.format("%s with id %d has server-side media_url (%s), but does not appear to be hq. sending hq", object.getMediaType(c).toString(), ((Model)object).getId(), response.getString("media_url")));
                                // non hq synced
                                new Thread(){
                                    public void run(){
                                        Log.i(TAG, String.format("sending %s hq media with id %d", object.getMediaType(c).toString(), ((Model)object).getId()));
                                        SharedPreferences prefs = c.getSharedPreferences(Constants.PROFILE_PREFS, c.MODE_PRIVATE);
                                        String public_upload_token = prefs.getString(Constants.PUB_TOKEN, "");
                                        OWMediaRequests.safeSendHQFile(c, public_upload_token, object.getUUID(c), object.getMediaFilepath(c), ((Model)object).getId(), mediaSyncRequestCallback);
                                    }
                                }.start();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }else{ // object is not a video, but has media_url set on server
                        Log.i(TAG, String.format("%s with id %d has server-side media_url. mark synced", object.getMediaType(c).toString(), ((Model)object).getId()));
                        object.setSynced(c, true);
                        if(finalTask)
                            broadcastMessage(c, Constants.OW_SYNC_STATUS_END_BULK);
                    }
                }
            }

            @Override
            public void onFailure(Throwable e, String response) {
                e.printStackTrace();
                Log.i(TAG+"getRecordingFailed", String.format("message: %s . cause: %s",e.getMessage(),e.getCause()));
                if(e.getMessage().compareTo("NOT FOUND") == 0){
                    Log.i(TAG, String.format("%s with id %d does not exist server-side. creating now", object.getMediaType(c).toString(), ((Model)object).getId()));
                    broadcastMessage(c, Constants.OW_SYNC_STATUS_BEGIN_BULK);
                    if(object.getMediaType(c) == Constants.MEDIA_TYPE.VIDEO){
                        new Thread(){
                            public void run(){
                                Log.i(TAG, "uploading video via media server signals");
                                SharedPreferences prefs = c.getSharedPreferences(Constants.PROFILE_PREFS, c.MODE_PRIVATE);
                                String public_upload_token = prefs.getString(Constants.PUB_TOKEN, "");
                                OWMediaRequests.start(c, public_upload_token, object.getUUID(c), "");
                                OWMediaRequests.end(c, public_upload_token, ((OWLocalVideoRecording)object).recording.get(c));
                                OWMediaRequests.safeSendHQFile(c, public_upload_token, object.getUUID(c), object.getMediaFilepath(c), ((Model)object).getId(), mediaSyncRequestCallback);
                                OWServiceRequests.syncOWServerObject(c, ((OWLocalVideoRecording)object).recording.get(c).media_object.get(c));
                            }
                        }.start();

                    }else{
                        Log.i(TAG, "creating object via django");
                        OWServiceRequests.createOWServerObject(c, object, mediaSyncRequestCallback);
                    }
                    e.printStackTrace();
                }else{
                    //network error
                    if(finalTask)
                        broadcastMessage(c, Constants.OW_SYNC_STATUS_END_BULK);
                }
            }

            @Override
            public void onFinish() {
                Log.i(TAG, "getRecording finish");
            }

        };
    }

    private static void broadcastMessage(Context c, int status){
        if(status == Constants.OW_SYNC_STATUS_BEGIN_BULK)
            syncing = true;
        else if(status == Constants.OW_SYNC_STATUS_END_BULK)
            syncing = false;
        Log.i(TAG, String.format("broadcasting message %d", status));
        Intent intent = new Intent(Constants.OW_SYNC_STATE_FILTER);
        intent.putExtra(Constants.OW_SYNC_STATE_STATUS, status);
        LocalBroadcastManager.getInstance(c).sendBroadcast(intent);
    }

}