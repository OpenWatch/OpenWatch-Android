package org.ale.openwatch;

import android.app.AlertDialog;
import android.content.*;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.VideoView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.facebook.Session;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.orm.androrm.Filter;
import com.orm.androrm.QuerySet;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.constants.DBConstants;
import org.ale.openwatch.contentprovider.OWContentProvider;
import org.ale.openwatch.fb.FBUtils;
import org.ale.openwatch.http.OWServiceRequests;
import org.ale.openwatch.http.Utils;
import org.ale.openwatch.model.OWMission;
import org.ale.openwatch.model.OWServerObject;
import org.ale.openwatch.model.OWVideoRecording;
import org.ale.openwatch.share.Share;
import org.ale.openwatch.twitter.TwitterUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class WhatHappenedActivity extends SherlockFragmentActivity implements FBUtils.FaceBookSessionActivity {

    TextView editTitle;

	private static final String TAG = "WhatHappenedActivity";
	static int model_id = -1;
	int recording_server_id = -1;
	String recording_uuid;
    String hq_filepath;
    String missionTag;
    int missionId = 0;

    boolean video_playing = false;
    boolean isResumed = false;

    // Facebook
    static final String PENDING_REQUEST_BUNDLE_KEY = "org.ale.openwatch.WhatHappenedActivity:PendingRequest";
    Session session;
    boolean pendingRequest;

    CompoundButton fbToggle;
    CompoundButton twitterToggle;
    CompoundButton owToggle;
    Button missionButton;

    // Keep track of OW-Sync before Sharing to FB / Twitter
    boolean syncedWithOW = false;
    boolean didTweet = false;
    boolean didFBShare = false;

    boolean didClickDone = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_what_happened);
        OWUtils.setupAB(this);

        missionButton = ((Button)findViewById(R.id.missionButton));
        ((CompoundButton) findViewById(R.id.owSwitch)).setChecked(true);

        fbToggle = (CompoundButton) findViewById(R.id.fbSwitch);
        fbToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    FBUtils.authenticate(WhatHappenedActivity.this, null);
                }
            }
        });
        twitterToggle = (CompoundButton) findViewById(R.id.twitterSwitch);
        twitterToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    TwitterUtils.authenticate(WhatHappenedActivity.this, null);
                }
            }
        });
        owToggle = (CompoundButton) findViewById(R.id.owSwitch);
        owToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(!isChecked){
                    fbToggle.setChecked(false);
                    fbToggle.setEnabled(false);
                    twitterToggle.setChecked(false);
                    twitterToggle.setEnabled(false);
                    missionButton.setEnabled(false);
                }else{
                    fbToggle.setEnabled(true);
                    twitterToggle.setEnabled(true);
                    missionButton.setEnabled(true);
                }
            }
        });

        didClickDone = false;

		Log.i(TAG, "onCreate");
		try{
			model_id = getIntent().getExtras().getInt(Constants.INTERNAL_DB_ID);
			recording_uuid = getIntent().getExtras().getString(Constants.OW_REC_UUID);
            hq_filepath = getIntent().getExtras().getString("hq_filepath");
            OWServerObject server_object = OWServerObject.objects(getApplicationContext(), OWServerObject.class).get(model_id);
            if(server_object.video_recording.get(getApplicationContext()).local.get(getApplicationContext()).hq_synced.get() == true){
                setStatusBarWithObjectUrl();
            }
            setupVideoView(server_object);
		}catch (Exception e){
			Log.e(TAG, "could not load recording_id from intent");
		}
        if(model_id != -1)
		    fetchRecordingFromOW();

        if(getIntent().getStringExtra(Constants.OBLIGATORY_TAG) != null)
            ((TextView)this.findViewById(R.id.editTitle)).setText("#" + getIntent().getStringExtra(Constants.OBLIGATORY_TAG));
		
		Log.i(TAG, "sent recordingMeta request");

        editTitle = ((TextView)findViewById(R.id.editTitle));
        editTitle.setFocusableInTouchMode(true);
        editTitle.requestFocus();
        editTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                syncedWithOW = false;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Facebook
        this.session = FBUtils.createSession(this, Constants.FB_APP_ID);

        populateViews();
	}

    private void populateViews(){
        Filter filter = new Filter();
        filter.is("joined", "!=","NULL");
        QuerySet<OWMission> joinedMissions = OWMission.objects(getApplicationContext(), OWMission.class).filter(filter).orderBy("-joined");
        int mostRecentJoinedMissionServerObjectId = 0;
        OWMission mostRecentJoinedMission = null;
        for(OWMission mission : joinedMissions){
            mostRecentJoinedMissionServerObjectId = mission.media_object.get(getApplicationContext()).getId();
            mostRecentJoinedMission = mission;
            break;
        }

        if(getIntent().hasExtra(Constants.MISSION_SERVER_OBJ_ID))
            onMissionSelected(getIntent().getIntExtra(Constants.MISSION_SERVER_OBJ_ID,0));
        else{
            SharedPreferences prefs = getSharedPreferences(Constants.PROFILE_PREFS, MODE_PRIVATE);
            String lastSelectedMissionDate = prefs.getString(Constants.LAST_MISSION_DATE,"2012");
            if(mostRecentJoinedMission == null || lastSelectedMissionDate.compareTo(mostRecentJoinedMission.joined.get()) > 0)
                onMissionSelected(prefs.getInt(Constants.LAST_MISSION_ID,0));
            else if(mostRecentJoinedMission != null)
                onMissionSelected(mostRecentJoinedMissionServerObjectId);
        }
    }

    public void syncAndPostSocial(final OWUtils.SOCIAL_TYPE type){
        if(type == OWUtils.SOCIAL_TYPE.FB && didFBShare)
            return;
        else if(type == OWUtils.SOCIAL_TYPE.TWITTER && didTweet)
            return;

        // TODO: Let's make a network request service
        // There's a pretty serious bug here where the syncPostSocial request won't wait to ensure
        // the video is live on OW...
        if(type == OWUtils.SOCIAL_TYPE.FB)
            didFBShare = true;
        else
            didTweet = true;
        // If a description has been entered, sync that before posting to FB
        //TextView editTitle = (TextView) this.getSupportFragmentManager().findFragmentById(R.id.media_object_info).getView().findViewById(R.id.editTitle);
        if(!syncedWithOW){
            OWServerObject serverObject = OWServerObject.objects(getApplicationContext(), OWServerObject.class).get(model_id);
            serverObject.title.set(editTitle.getText().toString());
            serverObject.save(getApplicationContext());
            OWServiceRequests.syncOWServerObject(getApplicationContext(), serverObject, true, new OWServiceRequests.RequestCallback() {
                @Override
                public void onFailure() {
                    // If somehow the ow metadata request fails, post anyway...
                    this.onSuccess();
                }

                @Override
                public void onSuccess() {
                    syncedWithOW = true;
                    OWUtils.postSocial(WhatHappenedActivity.this, type, model_id);

                }
            });
        }else{
            OWUtils.postSocial(WhatHappenedActivity.this, type, model_id);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("FBUtils", String.format("onActivityResult requestCode: %d , resultCode: %d", requestCode, resultCode));
        if(requestCode == TwitterUtils.TWITTER_RESULT){

            TwitterUtils.TwitterAuthCallback cb = new TwitterUtils.TwitterAuthCallback() {
                @Override
                public void onAuth() {
                    TwitterUtils.tweet(WhatHappenedActivity.this, Share.generateShareText(WhatHappenedActivity.this, OWServerObject.objects(WhatHappenedActivity.this, OWServerObject.class).get(model_id)));
                }
            };

            if(data != null && data.hasExtra("oauth_callback_url")){
                String oauthCallbackUrl = data.getExtras().getString("oauth_callback_url");
                TwitterUtils.twitterLoginConfirmation(WhatHappenedActivity.this, oauthCallbackUrl, cb);
            }else{
                Log.e(TAG, "onActivityResult did not provide Intent data with twitter oauth callback url");
                twitterToggle.setChecked(false);
            }


        }else{
            // Facebook
            if(this.session.onActivityResult(this, requestCode, resultCode, data) &&
                    pendingRequest &&
                    this.session.getState().isOpened()) {
                //syncAndPostSocial(OWUtils.SOCIAL_TYPE.FB);
            }else{
                fbToggle.setChecked(false);
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        pendingRequest = savedInstanceState.getBoolean(PENDING_REQUEST_BUNDLE_KEY, pendingRequest);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(PENDING_REQUEST_BUNDLE_KEY, pendingRequest);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_submit:
            this.finish();
			//showCompleteDialog();
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

    public void onDoneButtonClick(View v){
        if(didClickDone)
            return;
        didClickDone = true;
        try {
            JSONObject analyticsPayload = new JSONObject()
                    .put(Analytics.ow_public, owToggle.isChecked())
                    .put(Analytics.to_fb, fbToggle.isChecked())
                    .put(Analytics.to_twitter, twitterToggle.isChecked());
            Analytics.trackEvent(getApplicationContext(), Analytics.POST_VIDEO, analyticsPayload);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        SharedPreferences.Editor prefs = getSharedPreferences(Constants.PROFILE_PREFS, MODE_PRIVATE).edit();
        prefs.putInt(Constants.LAST_MISSION_ID, missionId);
        prefs.putString(Constants.LAST_MISSION_DATE, Constants.utc_formatter.format(new Date()));
        prefs.commit();
        Intent feedFragmentIntent = new Intent(this, FeedFragmentActivity.class);
        if(owToggle.isChecked())
            feedFragmentIntent.putExtra(Constants.VICTORY, true);
        feedFragmentIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        feedFragmentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(feedFragmentIntent);
    }


    @Override
    public void onResume(){
        super.onResume();
        isResumed = true;
        LocalBroadcastManager.getInstance(this).registerReceiver(serverObjectSyncStateMessageReceiver,
                new IntentFilter(Constants.OW_SYNC_STATE_FILTER));

        if(!Utils.isDeviceOnline(this)){
            findViewById(R.id.sync_progress_container).setVisibility(View.GONE);
        }
    }


    @Override
	public void onPause(){
        isResumed = false;
        syncOWServerObject();

		SharedPreferences profile = getSharedPreferences(Constants.PROFILE_PREFS, 0);
		int user_id = profile.getInt(DBConstants.USER_SERVER_ID, 0);
		if(user_id > 0)
			getContentResolver().notifyChange(OWContentProvider.getUserRecordingsUri(user_id), null);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serverObjectSyncStateMessageReceiver);
        super.onPause();
	}

    private void syncOWServerObject(){
        if(model_id > 0){
            boolean doSync = false;
            OWServerObject serverObject = OWServerObject.objects(getApplicationContext(), OWServerObject.class).get(model_id);

            if(editTitle.getText().toString().length() > 0){
                serverObject.title.set(editTitle.getText().toString());
                serverObject.save(getApplicationContext());
                doSync = true;
            }

            if(!owToggle.isChecked()){
                serverObject.is_private.set(true);
                serverObject.save(getApplicationContext());
                doSync = true;
            }

            if(fbToggle.isChecked()){
                syncAndPostSocial(OWUtils.SOCIAL_TYPE.FB);
                doSync = false;
            }
            if(twitterToggle.isChecked()){
                syncAndPostSocial(OWUtils.SOCIAL_TYPE.TWITTER);
                doSync = false;
            }

            if(doSync){
                serverObject.saveAndSync(this.getApplicationContext());
            }

        }
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
            video_view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(video_playing){
                        ((VideoView)v).pause();
                        video_playing = false;
                    }else{
                        ((VideoView)v).start();
                        video_playing = true;
                    }
                    return false;
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
                if(model_id == intent.getIntExtra(Constants.OW_SYNC_STATE_MODEL_ID, -1) && model_id > -1){
                    setStatusBarWithObjectUrl();
                }
            }else if(status == Constants.OW_SYNC_STATUS_FAILED){

            }
        }
    };

    private void setStatusBarWithObjectUrl(){
        //Log.d("WhatHappenedActivity-BroadcastReceived", "sync complete. serverObject serverID: " + String.valueOf(serverObject.getServerId(getApplicationContext())));
        String tempUrl = "";
        if(model_id <= 0){
            // This shouldn't ever happen
            setStatusBarCompleteWithUrl("");
        }else if(model_id > 0){
            OWServerObject serverObject = OWServerObject.objects(getApplicationContext(), OWServerObject.class).get(model_id);
            if(serverObject.server_id.get() == null || serverObject.server_id.get() == 0){
                // No server_id available, so get it.
                OWServiceRequests.syncOWServerObject(getApplicationContext(), serverObject, new OWServiceRequests.RequestCallback() {
                    @Override
                    public void onFailure() {
                        setStatusBarCompleteWithUrl("");
                    }

                    @Override
                    public void onSuccess() {
                        OWServerObject serverObject = OWServerObject.objects(getApplicationContext(), OWServerObject.class).get(model_id);
                        setStatusBarCompleteWithUrl(OWUtils.urlForOWServerObject(serverObject, getApplicationContext()));
                    }
                });
            }else{
                setStatusBarCompleteWithUrl(OWUtils.urlForOWServerObject(serverObject, getApplicationContext()));
            }
        }

    }

    private void setStatusBarCompleteWithUrl(final String url){
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

    @Override
    public void onFBError(String response) {
        if(isResumed){
            fbToggle.setChecked(false);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.whoops))
                    .setMessage(getString(R.string.fb_error))
                    .setPositiveButton(getString(R.string.dialog_bummer), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
        }
    }

    @Override
    public boolean getPendingRequest() {
        return pendingRequest;
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public void setSession(Session session) {
        this.session = session;
    }

    @Override
    public void setPendingRequest(boolean pendingRequest) {
        this.pendingRequest = pendingRequest;
    }

    public void onSelectMissionClick(View v){
        showNoticeDialog();
    }

    public void showNoticeDialog() {
        // Create an instance of the dialog fragment and show it
        DialogFragment dialog = new MissionChooserDialogFragment();
        dialog.show(getSupportFragmentManager(), "MissionChooserDialogFragment");
    }

    public void onMissionSelected(int model_id){
        missionId = model_id;
        // Remove previous missionTag from editTitle if present
        String titleText = ((TextView)this.findViewById(R.id.editTitle)).getText().toString();
        if(missionTag != null && titleText.contains("#"+missionTag))
            titleText = titleText.replace(" #"+missionTag,"").replace("#"+missionTag,"");

        if(model_id == 0){
            missionButton.setText(R.string.select_mission);
            ((TextView)this.findViewById(R.id.editTitle)).setText(titleText);
            missionTag = null;
        }else{
            OWServerObject serverObject = OWServerObject.objects(getApplicationContext(), OWServerObject.class).get(model_id);
            missionButton.setText(serverObject.title.get());
            missionTag = serverObject.mission.get(getApplicationContext()).tag.get();
            ((TextView)this.findViewById(R.id.editTitle)).setText(titleText + " #" + missionTag);
        }
    }
}
