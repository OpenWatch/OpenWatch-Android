package net.openwatch.reporter;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.http.OWServiceRequests;
import net.openwatch.reporter.model.OWPhoto;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.support.v4.app.NavUtils;
import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.provider.MediaStore;

public class OWPhotoReviewActivity extends SherlockFragmentActivity {
	
	private static final String TAG = "OWPhotoReviewActivity";
	
	private ImageView previewImageView;
    static int owphoto_parent_id = -1;
	private int owphoto_id = -1;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_picture_review);
        this.getSupportActionBar().setTitle("Describe your Picture");
		// Show the Up button in the action bar.
		setupActionBar();
		Log.i("PictureReview","got it");
		final Intent i = getIntent();
        if(i.hasExtra("owphoto_id")){
            owphoto_id = i.getIntExtra("owphoto_id", 0);
            postOWPhoto();
        } else
            Log.e(TAG, "unable to read OWPhoto id from intent");
        if(i.hasExtra(Constants.INTERNAL_DB_ID))
            owphoto_parent_id = i.getIntExtra(Constants.INTERNAL_DB_ID, 0);
        else
            Log.e(TAG, "unable to read photo's OWServerObject id from intent");
		
		previewImageView = (ImageView) findViewById(R.id.picture_preview);
        if(i != null)
		    previewImageView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener(){
			
			@Override
			public void onGlobalLayout() {
				loadScaledPictureFromIntent(i);
			}
		});
	}
	
	private void loadScaledPictureFromIntent(Intent intent) {
	    //Bundle extras = intent.getExtras();
	    if(intent.hasExtra(MediaStore.EXTRA_OUTPUT)){
	    	Log.i("loadScaledPic", "got extra_output from intent");
	    	OWUtils.loadScaledPicture(intent.getStringExtra(MediaStore.EXTRA_OUTPUT), previewImageView);
	    }else if(owphoto_id > 0){
	    	Log.i("loadScaledPic", "got output from intent");
	    	Log.i("PictureReview-loadScaled", "get owphoto_id: " + String.valueOf(owphoto_id));
	    	OWPhoto photo = OWPhoto.objects(getApplicationContext(), OWPhoto.class).get(owphoto_id);
	    	OWUtils.loadScaledPicture(photo.filepath.get(), previewImageView);
	    }
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.picture_review, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
			
		case R.id.menu_submit:
			 this.finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void postOWPhoto(){
        /* Title sync will be handled onPause() by OWMediaObjectInfoFragment

        Log.i(TAG, String.format("Preparing to sync Photo %d with ServerObject %d", owphoto_id, photo.media_object.get(getApplicationContext()).getId()));
        Log.i(TAG, String.format("Attempting to set photo title from edittext: %s", photoEditText.getText().toString()));
		photo.setTitle(getApplicationContext(), photoEditText.getText().toString());
        photo.media_object.get(getApplicationContext()).save(getApplicationContext());
		photo.save(getApplicationContext());
		Log.i(TAG, String.format("postOWPhoto, uuid: %s title: %s",  photo.getUUID(getApplicationContext()), photo.getTitle(getApplicationContext())) );

		*/
        OWPhoto photo = OWPhoto.objects(getApplicationContext(), OWPhoto.class).get(owphoto_id);
		OWServiceRequests.createOWServerObject(getApplicationContext(), photo);
	}
	
	public void onPause(){
		super.onPause();
	}
	

}
