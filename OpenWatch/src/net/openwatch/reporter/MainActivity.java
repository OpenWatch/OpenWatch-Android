package net.openwatch.reporter;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.database.DatabaseManager;
import net.openwatch.reporter.http.OWServiceRequests;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.View;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		checkUserStatus();
	}

	public void recordButtonClick(View v) {
		Intent i = new Intent(this, RecorderActivity.class);
		startActivity(i);
	}
	
	public void watchButtonClick(View v){
		Intent i = new Intent(this, FeedActivity.class);
		startActivity(i);
	}
	
	public void settingsButtonClick(View v){
		Intent i = new Intent(this, SettingsActivity.class);
		startActivity(i);
	}
	
	public void localButtonClick(View v){
		Intent i = new Intent(this, FeedActivity.class);
		startActivity(i);
	}
	
	public void savedButtonClick(View v){
		Intent i = new Intent(this, FeedActivity.class);
		startActivity(i);
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
			OWServiceRequests.onLaunchSync(this.getApplicationContext()); // get list of tags, etc
		}
		if(!authenticated){
			Intent i = new Intent(this, LoginActivity.class	);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			String email = profile.getString(Constants.EMAIL, null);
			if(email != null)
				i.putExtra(Constants.EMAIL, email);
			startActivity(i);
		}
		
	}

}
