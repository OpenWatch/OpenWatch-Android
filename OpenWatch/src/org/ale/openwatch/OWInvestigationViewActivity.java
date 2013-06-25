package org.ale.openwatch;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.loopj.android.http.JsonHttpResponseHandler;

import com.nostra13.universalimageloader.core.ImageLoader;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.constants.Constants.CONTENT_TYPE;
import org.ale.openwatch.constants.Constants.HIT_TYPE;
import org.ale.openwatch.http.OWServiceRequests;
import org.ale.openwatch.model.OWInvestigation;
import org.ale.openwatch.model.OWServerObject;
import org.ale.openwatch.model.OWUser;
import org.ale.openwatch.share.Share;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;

public class OWInvestigationViewActivity extends SherlockActivity implements OWMediaObjectBackedEntity {
	private static final String TAG = "OWInvestigationViewActivity";
	
    //private View progress;

	private int model_id;
	private int server_id;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_owinvestigation_view);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        OWUtils.setReadingFontOnChildren((ViewGroup) findViewById(R.id.relativeLayout));

		model_id = getIntent().getExtras().getInt(Constants.INTERNAL_DB_ID);
		OWServerObject object = OWServerObject.objects(getApplicationContext(), OWServerObject.class).get(model_id);
		server_id = object.getServerId(getApplicationContext());
		this.getSupportActionBar().setTitle(object.title.get());
		populateViews(object, getApplicationContext());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.server_object, menu);
		return true;
	}

	@Override
	public void populateViews(OWServerObject media_object, Context app_context) {
		OWServiceRequests.getOWServerObjectMeta(getApplicationContext(), media_object, "", new JsonHttpResponseHandler() {

			@Override
			public void onSuccess(JSONObject response) {
				Log.i(TAG, "get i success : " + response.toString());
                try {
                    //populate views
                    showProgress(false);
                    OWInvestigation.createOrUpdateOWInvestigationWithJson(getApplicationContext(), response);
                    OWServerObject serverObject = OWServerObject.objects(getApplicationContext(), OWServerObject.class).get(model_id);
                    OWInvestigation investigation = serverObject.investigation.get(getApplicationContext());
                    ((TextView) findViewById(R.id.title)).setText(serverObject.title.get());
                    ((TextView) findViewById(R.id.blurb)).setText(investigation.blurb.get());
                    ((TextView) findViewById(R.id.questions)).setText(Html.fromHtml(investigation.questions.get()));
                    ImageLoader.getInstance().displayImage(investigation.big_logo_url.get(), (ImageView) findViewById(R.id.image));
                    OWUser user = serverObject.user.get(getApplicationContext());
                    if(user != null){
                        ((TextView) findViewById(R.id.userTitle)).setText(user.username.get());
                        if(user.thumbnail_url.get() != null && user.thumbnail_url.get().compareTo("") != 0)
                            ImageLoader.getInstance().displayImage(user.thumbnail_url.get(), (ImageView) findViewById(R.id.userImage));
                    }
                    // http://stackoverflow.com/questions/8421670/webpage-not-available-with-webview-loaddata-only-in-emulator
                } catch (JSONException e) {
                    Log.e(TAG, "unable to load html from investigation response: " + response.toString());
                    e.printStackTrace();
                }
			}

			@Override
			public void onFailure(Throwable e, String response) {
				Log.i(TAG, "get i failure: " + response);

			}

			@Override
			public void onFinish() {
				Log.i(TAG, "get i finish: ");

			}
		});
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu){
		//menu.removeItem(R.id.menu_delete);
		menu.removeItem(R.id.menu_save);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.menu_share:
			if(server_id > 0){
				OWServerObject object = OWServerObject.objects(getApplicationContext(), OWServerObject.class).get(model_id);
				Share.showShareDialogWithInfo(this, getString(R.string.share_investigation), object.getTitle(getApplicationContext()),OWUtils.urlForOWServerObject(object, getApplicationContext()));
				OWServiceRequests.increaseHitCount(getApplicationContext(), server_id, model_id, CONTENT_TYPE.INVESTIGATION, HIT_TYPE.CLICK);
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}

    private void showProgress(boolean show){
        /*
        if(show){
            progress.setVisibility(View.VISIBLE);
        }else{
            progress.setVisibility(View.GONE);
        }
        */
    }




}
