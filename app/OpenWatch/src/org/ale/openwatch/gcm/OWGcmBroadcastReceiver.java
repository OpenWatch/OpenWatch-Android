package org.ale.openwatch.gcm;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import org.ale.openwatch.FeedFragmentActivity;
import org.ale.openwatch.OWMissionViewActivity;
import org.ale.openwatch.R;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.database.DatabaseManager;
import org.ale.openwatch.http.OWServiceRequests;
import org.ale.openwatch.model.OWMission;
import org.ale.openwatch.model.OWServerObject;

/**
 * Created by davidbrodsky on 5/29/13.
 */
public class OWGcmBroadcastReceiver extends BroadcastReceiver {
    static final String TAG = "GCMDemo";
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;
    Context ctx;


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "received message");
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        ctx = context;
        String messageType = gcm.getMessageType(intent);

        if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
            Log.e(TAG, "GCM Send Error: " + intent.getExtras().toString());
        } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
            Log.e(TAG, "GCM Deleted Messages on Server: " + intent.getExtras().toString());
        } else {
            sendNotification(intent.getExtras());
        }
        setResultCode(Activity.RESULT_OK);
    }

    // Put the GCM message into a notification and post it.
    private void sendNotification(Bundle gcmData) {
        DatabaseManager.registerModels(ctx);
        String msg = gcmData.getString("message");
        int missionId = 0;
        if(gcmData.containsKey("m"))
            missionId = Integer.parseInt(gcmData.getString("m"));
        mNotificationManager = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        final int finalMissionServerId = missionId;
        Intent notificationIntent;
        PendingIntent contentIntent;
        if(missionId > 0){
            OWServerObject serverObject = OWMission.getOWServerObjectByOWMissionServerId(ctx, missionId);
            if(serverObject == null){
                OWServiceRequests.updateOrCreateOWServerObject(ctx, Constants.CONTENT_TYPE.MISSION, String.valueOf(missionId), "", new OWServiceRequests.RequestCallback() {
                    @Override
                    public void onFailure() {}

                    @Override
                    public void onSuccess() {
                        OWServerObject serverObject = OWMission.getOWServerObjectByOWMissionServerId(ctx, finalMissionServerId);
                        if(serverObject != null){
                            OWServiceRequests.postMissionAction(ctx, serverObject, OWMission.ACTION.RECEIVED_PUSH);
                        }
                    }
                });
            }else{
                OWServiceRequests.postMissionAction(ctx, serverObject, OWMission.ACTION.RECEIVED_PUSH);
            }
            notificationIntent  = new Intent(ctx, OWMissionViewActivity.class);
            notificationIntent.putExtra(Constants.SERVER_ID, finalMissionServerId);
            notificationIntent.putExtra("viewed_push", true);
            contentIntent = PendingIntent.getActivity(ctx, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }else{
            notificationIntent  = new Intent(ctx, FeedFragmentActivity.class);
            contentIntent = PendingIntent.getActivity(ctx, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        showNotificationWithContentIntent(ctx, mNotificationManager, msg, contentIntent);
    }

    private static void showNotificationWithContentIntent(Context ctx, NotificationManager notificationManager, String msg, PendingIntent contentIntent){
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(ctx)
                        .setSmallIcon(R.drawable.ic_ab_icon)
                        .setContentTitle(ctx.getString(R.string.mission_available))
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setTicker(msg)
                        .setAutoCancel(true)
                        .setVibrate(new long[]{0, 200, 200, 200})
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}