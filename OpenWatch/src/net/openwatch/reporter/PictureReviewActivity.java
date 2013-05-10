package net.openwatch.reporter;

import net.openwatch.reporter.http.OWServiceRequests;
import net.openwatch.reporter.model.OWPhoto;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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

public class PictureReviewActivity extends Activity {
	
	private ImageView previewImageView;
	private int owphoto_id = -1;
	private boolean didAttemptSync = false;
	
	private EditText photoEditText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_picture_review);
		// Show the Up button in the action bar.
		setupActionBar();
		Log.i("PictureReview","got it");
		final Intent i = getIntent();
		
		photoEditText = (EditText) findViewById(R.id.picture_caption);
		previewImageView = (ImageView) findViewById(R.id.picture_preview);
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
	    	loadScaledPicture(intent.getStringExtra(MediaStore.EXTRA_OUTPUT), previewImageView);
	    }else if(intent.hasExtra("owphoto_id")){
	    	Log.i("loadScaledPic", "got output from intent");
	    	owphoto_id = intent.getIntExtra("owphoto_id", -1);
	    	Log.i("PictureReview-loadScaled", "get owphoto_id: " + String.valueOf(owphoto_id));
	    	OWPhoto photo = OWPhoto.objects(getApplicationContext(), OWPhoto.class).get(owphoto_id);
	    	loadScaledPicture(photo.filepath.get(), previewImageView);
	    }
	}
	
	private void loadScaledPicture(String image_path, ImageView target) {
	    // Get the dimensions of the View
	    int targetW = target.getWidth();
	    int targetH = target.getHeight();
	  
	    // Get the dimensions of the bitmap
	    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
	    bmOptions.inJustDecodeBounds = true;
	    BitmapFactory.decodeFile(image_path, bmOptions);
	    int photoW = bmOptions.outWidth;
	    int photoH = bmOptions.outHeight;
	  
	    // Determine how much to scale down the image
	    int scaleFactor = Math.min(photoW/targetW, photoH/targetH);
	  
	    // Decode the image file into a Bitmap sized to fill the View
	    bmOptions.inJustDecodeBounds = false;
	    bmOptions.inSampleSize = scaleFactor;
	    bmOptions.inPurgeable = true;
	  
	    Bitmap bitmap = BitmapFactory.decodeFile(image_path, bmOptions);
	    target.setImageBitmap(bitmap);
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
		getMenuInflater().inflate(R.menu.picture_review, menu);
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
			 syncPhoto();
			 this.finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void syncPhoto(){
		OWPhoto photo = OWPhoto.objects(getApplicationContext(), OWPhoto.class).get(owphoto_id);
		photo.title.set(photoEditText.getText().toString());
		photo.save(getApplicationContext());
		OWServiceRequests.createOWMobileGeneratedObject(getApplicationContext(), photo);
		didAttemptSync = true;
	}
	
	public void onPause(){
		if(!didAttemptSync){
			 syncPhoto();
		}
		super.onPause();
	}
	

}
