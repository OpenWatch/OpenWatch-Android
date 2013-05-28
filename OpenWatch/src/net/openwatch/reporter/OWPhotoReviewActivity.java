package net.openwatch.reporter;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.loopj.android.http.JsonHttpResponseHandler;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.http.OWServiceRequests;
import net.openwatch.reporter.model.OWPhoto;
import net.openwatch.reporter.model.OWServerObject;
import net.openwatch.reporter.share.Share;
import org.json.JSONObject;

public class OWPhotoReviewActivity extends SherlockFragmentActivity {
	
	private static final String TAG = "OWPhotoReviewActivity";
	
	private ImageView previewImageView;
    static int owphoto_parent_id = -1;
	private int owphoto_id = -1;

    boolean didShare = false;


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
            showCompleteDialog();
            //OWServerObject server_obj = OWServerObject.objects(this, OWServerObject.class).get(owphoto_parent_id);
            //Share.showShareDialog(this, getString(R.string.share_story), OWUtils.urlForOWServerObject(server_obj, getApplicationContext()));
            //OWServiceRequests.increaseHitCount(getApplicationContext(), server_obj.getServerId(getApplicationContext()), owphoto_parent_id, server_obj.getContentType(getApplicationContext()), server_obj.getMediaType(getApplicationContext()), Constants.HIT_TYPE.CLICK);
            //this.finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

    /**
     * If a server_id was received, give option to share, else return to MainActivity
     */
    private void showCompleteDialog(){
        if(owphoto_parent_id == -1){
            Log.e(TAG, "model_id not set. aborting showCompleteDialog");
            return;
        }
        final OWServerObject server_obj = OWServerObject.objects(this, OWServerObject.class).get(owphoto_parent_id);
        final Context c = this;
        //final OWVideoRecording recording = OWMediaObject.objects(getApplicationContext(), OWMediaObject.class).get(model_id).video_recording.get(getApplicationContext());
        int server_id = -1;
        if(server_obj.getServerId(getApplicationContext()) != null)
            server_id = server_obj.getServerId(getApplicationContext());
        if(!(server_id > 0) ){
            Log.i(TAG, "photo does not have a valid server_id. Cannot present share dialog");
            Intent i = new Intent(OWPhotoReviewActivity.this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            this.finish();
            return;
        }
        Log.i(TAG, "photo server_id: " + String.valueOf(server_id));
        LayoutInflater inflater = (LayoutInflater)
                c.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.share_prompt,
                (ViewGroup) findViewById(R.id.root_layout), false);
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setView(layout);
        final AlertDialog dialog = builder.create();

        ((TextView) layout.findViewById(R.id.share_title)).setText("Your photo is live! Spread the word and make an impact!");
        layout.findViewById(R.id.button_share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                OWServerObject server_obj = OWServerObject.objects(c, OWServerObject.class).get(owphoto_parent_id);
                Share.showShareDialogWithInfo(c, "Share Photo", server_obj.getTitle(getApplicationContext()), OWUtils.urlForOWServerObject(server_obj, getApplicationContext()));
                OWServiceRequests.increaseHitCount(getApplicationContext(), server_obj.getServerId(getApplicationContext()), owphoto_parent_id, server_obj.getContentType(getApplicationContext()), server_obj.getMediaType(getApplicationContext()), Constants.HIT_TYPE.CLICK);
            }
        });
        layout.findViewById(R.id.share_nothanks).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent i = new Intent(OWPhotoReviewActivity.this, MainActivity.class);
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
		OWServiceRequests.createOWServerObject(getApplicationContext(), photo, new OWServiceRequests.RequestCallback(){
            @Override
            public void onFailure() {

            }

            @Override
            public void onSuccess() {
                OWServerObject serverObject = OWServerObject.objects(getApplicationContext(), OWServerObject.class).get(owphoto_parent_id);
                final String url = OWUtils.urlForOWServerObject(serverObject, getApplicationContext());
                findViewById(R.id.sync_progress).setVisibility(View.GONE);
                findViewById(R.id.sync_complete).setVisibility(View.VISIBLE);
                TextView sync_progress = ((TextView)findViewById(R.id.sync_progress_text));
                sync_progress.setText("Your Photo is Live! \n" + url);
                sync_progress.setClickable(true);
                sync_progress.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(browserIntent);
                    }
                });
            }
        });
	}
	
	public void onPause(){
		super.onPause();
	}

    private void fetchOWPhotoFromOW(){
        final Context app_context = this.getApplicationContext();
        OWServiceRequests.getOWServerObjectMeta(app_context, OWServerObject.objects(app_context, OWServerObject.class).get(owphoto_parent_id), "", new JsonHttpResponseHandler(){
            private static final String TAG = "OWServiceRequests";
            @Override
            public void onSuccess(JSONObject response){
                Log.i(TAG, "getRecording response: " + response.toString());
                if(response.has("id")){
                    Log.i(TAG, "Got server recording response!");
                    try{
                        // response was successful
                        OWPhoto photo = OWServerObject.objects(app_context, OWServerObject.class).get(owphoto_parent_id).photo.get(app_context);
                        photo.updateWithJson(app_context, response);
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

}
