package org.ale.openwatch;

import android.app.AlertDialog;
import android.content.*;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.constants.DBConstants;
import org.ale.openwatch.contentprovider.OWContentProvider;
import org.ale.openwatch.http.OWServiceRequests;
import org.ale.openwatch.http.Utils;
import org.ale.openwatch.model.OWServerObject;
import org.ale.openwatch.model.OWVideoRecording;
import org.ale.openwatch.share.Share;
import org.json.JSONObject;

public class WhatHappenedActivity extends SherlockFragmentActivity {

	private static final String TAG = "WhatHappenedActivity";
	static int model_id = -1;
	int recording_server_id = -1;
	String recording_uuid;
    String hq_filepath;

    boolean video_playing = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_what_happened);
		this.getSupportActionBar().setTitle(getString(R.string.what_happened));
		Log.i(TAG, "onCreate");
		try{
			model_id = getIntent().getExtras().getInt(Constants.INTERNAL_DB_ID);
			recording_uuid = getIntent().getExtras().getString(Constants.OW_REC_UUID);
            hq_filepath = getIntent().getExtras().getString("hq_filepath");
            OWServerObject server_object = OWServerObject.objects(getApplicationContext(), OWServerObject.class).get(model_id);
            setupVideoView(server_object);
		}catch (Exception e){
			Log.e(TAG, "could not load recording_id from intent");
		}
		fetchRecordingFromOW();
		
		Log.i(TAG, "sent recordingMeta request");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_submit:
			showCompleteDialog();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void fetchRecordingFromOW(){
		final Context app_context = this.getApplicationContext();
		OWServiceRequests.getOWServerObjectMeta(app_context, OWServerObject.objects(app_context, OWServerObject.class).get(model_id), "", new JsonHttpResponseHandler(){
			private static final String TAG = "OWServiceRequests";
			@Override
    		public void onSuccess(JSONObject response){
				Log.i(TAG, "getRecording response: " + response.toString());
				if(response.has("id")){
					Log.i(TAG, "Got server recording response!");
					try{
						// response was successful
						OWVideoRecording recording = OWServerObject.objects(app_context, OWServerObject.class).get(model_id).video_recording.get(app_context);
						recording.updateWithJson(app_context, response);
						recording_server_id = response.getInt(Constants.OW_SERVER_ID);
						Log.i(TAG, "recording updated with server meta response");
						return;
					} catch(Exception e){
						Log.e(TAG, "Error processing getRecording response");
						e.printStackTrace();
					}
				}
				Log.i(TAG, "Failed to handle server recording response!");
					
			}
			
			@Override
			public void onFailure(Throwable e, String response){
				Log.i(TAG, "get recording meta failed: " + response);
			}
			
			@Override
			public void onFinish(){
				Log.i(TAG, "get recording meta finish");
			}
			
		});
	}

	
	/**
	 * If a server_id was received, give option to share, else return to MainActivity
	 */
	private void showCompleteDialog(){
		if(model_id == -1){
			Log.e(TAG, "model_id not set. aborting showCompleteDialog");
			return;
		}
		//final OWVideoRecording recording = OWMediaObject.objects(getApplicationContext(), OWMediaObject.class).get(model_id).video_recording.get(getApplicationContext());
		if(recording_server_id == -1){
			Log.i(TAG, "recording does not have a valid server_id. Cannot present share dialog");
			Intent i = new Intent(WhatHappenedActivity.this, MainActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(i);
			this.finish();
			return;
		}
        final Context c = this;
		Log.i(TAG, "recording server_id: " + String.valueOf(recording_server_id));
        LayoutInflater inflater = (LayoutInflater)
                c.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.share_prompt,
                (ViewGroup) findViewById(R.id.root_layout), false);
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setView(layout);
        final AlertDialog dialog = builder.create();

        ((TextView) layout.findViewById(R.id.share_title)).setText(R.string.share_dialog_video_message);
        layout.findViewById(R.id.button_share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                OWServerObject server_obj = OWServerObject.objects(c, OWServerObject.class).get(model_id);
                Share.showShareDialogWithInfo(c, getString(R.string.share_video), server_obj.getTitle(getApplicationContext()), OWUtils.urlForOWServerObject(server_obj, getApplicationContext()));
                OWServiceRequests.increaseHitCount(getApplicationContext(), server_obj.getServerId(getApplicationContext()), model_id, server_obj.getContentType(getApplicationContext()), server_obj.getMediaType(getApplicationContext()), Constants.HIT_TYPE.CLICK);
                WhatHappenedActivity.this.finish();
            }
        });
        layout.findViewById(R.id.share_nothanks).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent i = new Intent(WhatHappenedActivity.this, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);

            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                //TODO: Catch returning to activity when you cancel share dialog. Instead finish activity
            }
        });
        dialog.show();
	}
	
	private void showShareDialog(int recording_server_id){
        OWServerObject server_object = OWServerObject.objects(getApplicationContext(), OWServerObject.class).get(model_id);
		String url = Constants.OW_URL +  Constants.OW_RECORDING_VIEW + String.valueOf(recording_server_id);
        String toShare = url + "\n" + server_object.getTitle(getApplicationContext()) + "\n" + "via @OpenWatch";
		Log.i(TAG, "model_id: " + String.valueOf(model_id) + " url: " + url);
		
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("text/plain");
		i.putExtra(Intent.EXTRA_TEXT, toShare);
		i.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
		WhatHappenedActivity.this.finish();
		startActivity(Intent.createChooser(i, getString(R.string.share_dialog_title)));
	}

    @Override
    public void onResume(){
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(serverObjectSyncStateMessageReceiver,
                new IntentFilter(Constants.OW_SYNC_STATE_FILTER));

        if(!Utils.isDeviceOnline(this)){
            findViewById(R.id.sync_progress_container).setVisibility(View.GONE);
        }
    }


    @Override
	public void onPause(){
		SharedPreferences profile = getSharedPreferences(Constants.PROFILE_PREFS, 0);
		int user_id = profile.getInt(DBConstants.USER_SERVER_ID, 0);
		if(user_id > 0)
			getContentResolver().notifyChange(OWContentProvider.getUserRecordingsUri(user_id), null);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serverObjectSyncStateMessageReceiver);
        super.onPause();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.activity_what_happened, menu);
		return true;
	}

    private void setupVideoView(OWServerObject object){
        String media_path = "";
        if(!video_playing){
            if( object.local_video_recording.get(getApplicationContext()) != null ){
                // This is a local recording, attempt to play HQ file
                media_path = object.local_video_recording.get(getApplicationContext()).hq_filepath.get();
                if(media_path == null && hq_filepath != null){ // temp hack
                    media_path = hq_filepath;
                }
            }
            Log.i(TAG, String.format("setupMediaView. media_url: %s", media_path));
            //setupVideoView(R.id.media_object_media_view, media_path);
            final VideoView video_view = (VideoView) findViewById(R.id.videoview);
            video_view.setVideoURI(Uri.parse(media_path));
            video_view.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                        @Override
                        public void onVideoSizeChanged(MediaPlayer mp, int width,
                                                       int height) {
                            VideoView video_view = (VideoView) findViewById(R.id.videoview);
                            //video_view.setVisibility(View.VISIBLE);
                            //(findViewById(R.id.progress_container)).setVisibility(View.GONE);
                            video_view.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                            MediaController mc = new MediaController(
                                    WhatHappenedActivity.this);
                            video_view.setMediaController(mc);
                            mc.setAnchorView(video_view);
                            video_view.requestFocus();
                            video_view.start();
                            video_playing = true;
                        }
                    });
                }
            });
            video_view.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    video_view.setVisibility(View.GONE);
                    return true;
                }
            });
            video_view.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    video_playing = false;
                }
            });
            video_view.start();
        }
    }

    private BroadcastReceiver serverObjectSyncStateMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            int status = intent.getIntExtra(Constants.OW_SYNC_STATE_STATUS, Constants.OW_SYNC_STATUS_FAILED);
            if(status == Constants.OW_SYNC_STATUS_SUCCESS){ // sync complete
                if(model_id == intent.getIntExtra(Constants.OW_SYNC_STATE_MODEL_ID, -1) && model_id != -1){
                    OWServerObject serverObject = OWServerObject.objects(getApplicationContext(), OWServerObject.class).get(model_id);
                    Log.d("WhatHappenedActivity-BroadcastReceived", "sync complete. serverObject serverID: " + String.valueOf(serverObject.getServerId(getApplicationContext())));
                    final String url = OWUtils.urlForOWServerObject(serverObject, getApplicationContext());
                    findViewById(R.id.sync_progress).setVisibility(View.GONE);
                    findViewById(R.id.sync_complete).setVisibility(View.VISIBLE);
                    TextView sync_progress = ((TextView)findViewById(R.id.sync_progress_text));
                    sync_progress.setText(getString(R.string.sync_video_complete_header_text) +  "\n" + url);
                    sync_progress.setClickable(true);
                    sync_progress.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            startActivity(browserIntent);
                        }
                    });
                }
            }else if(status == Constants.OW_SYNC_STATUS_FAILED){

            }
        }
    };

}
