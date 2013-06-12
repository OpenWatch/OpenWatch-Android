package org.ale.openwatch;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.bugsense.trace.BugSenseHandler;

import java.io.File;

import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.constants.Constants.MEDIA_TYPE;
import org.ale.openwatch.constants.Constants.OWFeedType;
import org.ale.openwatch.database.DatabaseManager;
import org.ale.openwatch.file.FileUtils;
import org.ale.openwatch.http.OWServiceRequests;
import org.ale.openwatch.location.DeviceLocation;
import org.ale.openwatch.model.OWPhoto;
import org.ale.openwatch.model.OWServerObject;
import org.ale.openwatch.model.OWUser;

public class MainActivity extends SherlockActivity {
	
	private static int owphoto_id = -1;
    private static int owphoto_parent_id = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		BugSenseHandler.initAndStartSession(getApplicationContext(), SECRETS.BUGSENSE_API_KEY);
		setContentView(R.layout.activity_main);
		ActionBar ab = this.getSupportActionBar();
		ab.setDisplayShowTitleEnabled(false);
		ab.setDisplayShowHomeEnabled(false);
		ab.setDisplayShowCustomEnabled(true);
		LayoutInflater inflator = (LayoutInflater) this
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflator.inflate(R.layout.main_activity_ab, null);
        ab.setCustomView(v);

        /*
        findViewById(R.id.progress_header_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v.getVisibility() == View.VISIBLE)
                    v.setVisibility(View.GONE);
                else
                    v.setVisibility(View.VISIBLE);
            }
        });
        */
	}
	
	@Override
	protected void onResume(){
		super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(serverObjectSyncStateMessageReceiver,
                new IntentFilter(Constants.OW_SYNC_STATE_FILTER));

        if(OWMediaSyncer.syncing)
            findViewById(R.id.progress_header_container).setVisibility(View.VISIBLE);
        else
            findViewById(R.id.progress_header_container).setVisibility(View.GONE);

		checkUserStatus();
	}

    @Override
    public void onPause(){
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serverObjectSyncStateMessageReceiver);
        super.onPause();
    }


	public void camcorderButtonClick(View v) {
		Intent i = new Intent(this, RecorderActivity.class);
		startActivity(i);
	}
	
	public void cameraButtonClick(View v) {

		String uuid = OWUtils.generateRecordingIdentifier();
        OWPhoto photo  = OWPhoto.initializeOWPhoto(getApplicationContext(), uuid);
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		takePictureIntent.putExtra("uuid", uuid);
		takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.parse(photo.filepath.get()));
		owphoto_id = photo.getId();
        owphoto_parent_id = photo.media_object.get(getApplicationContext()).getId();
		Log.i("MainActivity-cameraButtonClick", "get owphoto_id: " + String.valueOf(owphoto_id));
        DeviceLocation.setOWServerObjectLocation(getApplicationContext(), owphoto_parent_id, false);
	    startActivityForResult(takePictureIntent, Constants.CAMERA_ACTION_CODE);
	}
	
	public void microphoneButtonClick(View v) {
		Intent i = new Intent(this, RecorderActivity.class);
		startActivity(i);
	}

    public void missionButtonClick(View v) {
        Intent i = new Intent(this, FeedFragmentActivity.class);
        i.putExtra(Constants.FEED_TYPE, OWFeedType.MISSION);
        startActivity(i);
    }
	
	public void featuredButtonClick(View v){
		Intent i = new Intent(this, FeedFragmentActivity.class);
		i.putExtra(Constants.FEED_TYPE, OWFeedType.TOP);
		startActivity(i);
	}
	
	public void settingsButtonClick(View v){
		Intent i = new Intent(this, SettingsActivity.class);
		startActivity(i);
	}
	
	public void localButtonClick(View v){
		Intent i = new Intent(this, FeedFragmentActivity.class);
		i.putExtra(Constants.FEED_TYPE, OWFeedType.LOCAL);
		startActivity(i);
	}
	
	public void savedButtonClick(View v){
		Intent i = new Intent(this, FeedFragmentActivity.class);
		i.putExtra(Constants.FEED_TYPE, OWFeedType.USER);
		startActivity(i);
	}

    public void feedbackButtonClick(View v){
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto",Constants.SUPPORT_EMAIL, null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT,getString(R.string.share_email_subject));
        emailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_email_text) + OWUtils.getPackageVersion(getApplicationContext()));
        startActivity(Intent.createChooser(emailIntent, getString(R.string.share_chooser_title)));
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	/**
	 * Check if the user is authenticated, and if not, 
	 * direct the user to LoginActivity
	 * @return
	 */
	public void checkUserStatus(){

        boolean debug_fancy = false;
		
		SharedPreferences profile = getSharedPreferences(Constants.PROFILE_PREFS, 0);
		boolean authenticated = profile.getBoolean(Constants.AUTHENTICATED, false);
		boolean db_initialized = profile.getBoolean(Constants.DB_READY, false);
	
		if(!db_initialized){
			DatabaseManager.setupDB(getApplicationContext()); // do this every time to auto handle migrations
			//DatabaseManager.testDB(this);
		}else{
			DatabaseManager.registerModels(getApplicationContext()); // ensure androrm is set to our custom Database name.
		}
		
		if(authenticated && db_initialized && !((OWApplication) this.getApplicationContext()).per_launch_sync){
			// TODO: Attempt to login with stored credentials and report back if error

            OWMediaSyncer.syncMedia(getApplicationContext());
            ((OWApplication) getApplicationContext()).per_launch_sync = true;
            // If we have a User object for the current user, and they've applied as an agent, send their current location
            if(profile.getInt(Constants.INTERNAL_USER_ID, 0) != 0){
                OWUser user = OWUser.objects(getApplicationContext(), OWUser.class).get(profile.getInt(Constants.INTERNAL_USER_ID,0));
                if(user.agent_applicant.get() == true){
                    Log.i("MainActivity", "Sending agent location");
                    OWServiceRequests.syncOWUser(getApplicationContext(), user);
                }
            }
        }
		if(debug_fancy || (!authenticated && !this.getIntent().hasExtra(Constants.AUTHENTICATED) ) ){
			Intent i = new Intent(this, FancyLoginActivity.class	);
            //Intent i = new Intent(this, LoginActivity.class	);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
			String email = profile.getString(Constants.EMAIL, null);
			if(email != null)
				i.putExtra(Constants.EMAIL, email);
			startActivity(i);
            // This will set OWApplication.user_data
		}
        else if(authenticated){
            // If the user state is stored, load user_data into memory
            OWApplication.user_data = getApplicationContext().getSharedPreferences(Constants.PROFILE_PREFS, getApplicationContext().MODE_PRIVATE).getAll();
        }
		
	}
	
	@Override
	protected void onActivityResult (int requestCode, int resultCode, Intent data){
		Log.i("MainActivity-onActivityResult","got it");
		if(data == null)
			Log.i("MainActivity-onActivityResult", "data null");
		if(requestCode == Constants.CAMERA_ACTION_CODE && resultCode == RESULT_OK){
			Intent i = new Intent(this, OWPhotoReviewActivity.class);
			i.putExtra("owphoto_id", owphoto_id);
            i.putExtra(Constants.INTERNAL_DB_ID, owphoto_parent_id); // server object id
			Log.i("MainActivity-onActivityResult", String.format("bundling owphoto_id: %d, owserverobject_id: %d",owphoto_id, owphoto_parent_id));
			startActivity(i);
		}
		
	}

    private BroadcastReceiver serverObjectSyncStateMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            int status = intent.getIntExtra(Constants.OW_SYNC_STATE_STATUS, Constants.OW_SYNC_STATUS_FAILED);
            Log.i("MainActivity", String.format("got broadcastReceiver message %d", status));
            if(status == Constants.OW_SYNC_STATUS_BEGIN_BULK){
                findViewById(R.id.progress_header_container).setVisibility(View.VISIBLE);
                ((TextView)findViewById(R.id.sync_progress_text)).setText(getString(R.string.syncing_existing_media));

            }else if(status == Constants.OW_SYNC_STATUS_END_BULK){
                (findViewById(R.id.progress_header_container)).setVisibility(View.GONE);
            }
        }
    };

}
