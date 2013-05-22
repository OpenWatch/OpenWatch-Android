package net.openwatch.reporter;

import java.io.File;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.Constants.MEDIA_TYPE;
import net.openwatch.reporter.constants.Constants.OWFeedType;
import net.openwatch.reporter.database.DatabaseManager;
import net.openwatch.reporter.file.FileUtils;
import net.openwatch.reporter.http.OWServiceRequests;
import net.openwatch.reporter.location.DeviceLocation;
import net.openwatch.reporter.model.OWPhoto;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.bugsense.trace.BugSenseHandler;

public class MainActivity extends SherlockActivity {
	
	private static int camera_action_code = 444;
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
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		checkUserStatus();
	}


	public void camcorderButtonClick(View v) {
		Intent i = new Intent(this, RecorderActivity.class);
		startActivity(i);
	}
	
	public void cameraButtonClick(View v) {
		String uuid = OWUtils.generateRecordingIdentifier();
		File photo_location = FileUtils.prepareOutputLocation(getApplicationContext(), MEDIA_TYPE.PHOTO, uuid ,"photo", ".jpg");
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		takePictureIntent.putExtra("uuid", uuid);
		Log.i("takePicture", "location " + photo_location.getAbsolutePath() + " exists: " + String.valueOf(photo_location.exists()));
		takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo_location));
		
		OWPhoto photo = new OWPhoto(getApplicationContext());
		photo.filepath.set(photo_location.getAbsolutePath());
		photo.directory.set(photo_location.getParentFile().getAbsolutePath());
		photo.uuid.set(uuid);
		photo.save(getApplicationContext());
		owphoto_id = photo.getId();
        owphoto_parent_id = photo.media_object.get(getApplicationContext()).getId();
		Log.i("MainActivity-cameraButtonClick", "get owphoto_id: " + String.valueOf(owphoto_id));
        DeviceLocation.setOWServerObjectLocation(getApplicationContext(), owphoto_parent_id, false);
	    startActivityForResult(takePictureIntent, camera_action_code);
	}
	
	public void microphoneButtonClick(View v) {
		Intent i = new Intent(this, RecorderActivity.class);
		startActivity(i);
	}
	
	public void featuredButtonClick(View v){
		Intent i = new Intent(this, FeedFragmentActivity.class);
		i.putExtra(Constants.FEED_TYPE, OWFeedType.FEATURED);
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
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "OpenWatch-Android feedback");
        emailIntent.putExtra(Intent.EXTRA_TEXT,"Hey OpenWatch!\n\n" + OWUtils.getPackageVersion(getApplicationContext()));
        startActivity(Intent.createChooser(emailIntent, "Send us feedback"));
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
			// TODO: Attempt to login with stored credentials if we
			// check this application state
			//OWServiceRequests.onLaunchSync(this.getApplicationContext()); // get list of tags, etc
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
		if(requestCode == camera_action_code && resultCode == RESULT_OK){
			Intent i = new Intent(this, OWPhotoReviewActivity.class);
			i.putExtra("owphoto_id", owphoto_id);
            i.putExtra(Constants.INTERNAL_DB_ID, owphoto_parent_id); // server object id
			Log.i("MainActivity-onActivityResult", String.format("bundling owphoto_id: %d, owserverobject_id: %d",owphoto_id, owphoto_parent_id));
			startActivity(i);
		}
		
	}

}
