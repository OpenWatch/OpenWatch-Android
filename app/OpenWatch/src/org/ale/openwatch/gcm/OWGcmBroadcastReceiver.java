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
import org.ale.openwatch.*;
import org.ale.openwatch.constants.Constants;

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
        String msg = gcmData.getString("message");
        int missionId = gcmData.getInt("m", 0);
        mNotificationManager = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notificationIntent;
        if(missionId > 0){
            notificationIntent  = new Intent(ctx, OWMissionViewActivity.class);
            notificationIntent.putExtra(Constants.SERVER_ID, missionId);
        }else
            notificationIntent  = new Intent(ctx, RecorderActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, notificationIntent, 0);


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
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}