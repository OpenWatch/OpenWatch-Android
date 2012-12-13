package net.openwatch.reporter;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.database.DatabaseManager;
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
		//Intent i =  new Intent(this, ORMTestActivity.class);
		startActivity(i);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
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
		
		DatabaseManager.setupDB(this); // do this every time to auto handle migrations
		if(db_initialized){
			DatabaseManager.pointToDB(); // ensure androrm is set to our custom Database name.
			//DatabaseManager.testDB(this);
		}
		
		if(authenticated){
			// TODO: Attempt to login with stored credentials
		}
		else{
			Intent i = new Intent(this, LoginActivity.class	);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			String email = profile.getString(Constants.EMAIL, null);
			if(email != null)
				i.putExtra(Constants.EMAIL, email);
			startActivity(i);
		}
		
	}

}
