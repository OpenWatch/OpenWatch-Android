package net.openwatch.reporter;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.webkit.WebView;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.loopj.android.http.JsonHttpResponseHandler;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.Constants.CONTENT_TYPE;
import net.openwatch.reporter.constants.Constants.HIT_TYPE;
import net.openwatch.reporter.http.OWServiceRequests;
import net.openwatch.reporter.model.OWServerObject;
import net.openwatch.reporter.share.Share;
import org.json.JSONException;
import org.json.JSONObject;

public class OWInvestigationViewActivity extends SherlockActivity implements OWMediaObjectBackedEntity {
	private static final String TAG = "OWInvestigationViewActivity";
	
	private WebView web_view;
	private int model_id;
	private int server_id;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_owinvestigation_view);
		
		web_view = (WebView)findViewById(R.id.web_view);
		
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
		OWServiceRequests.getOWServerObjectMeta(getApplicationContext(), media_object, "?html=true", new JsonHttpResponseHandler() {

			@Override
			public void onSuccess(JSONObject response) {
				Log.i(TAG, "get i success : " + response.toString());
				if(response.has("html"))
					try {
						web_view.loadData(response.getString("html"), "text/html", null);
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
				OWServiceRequests.increaseHitCount(getApplicationContext(), server_id, model_id, CONTENT_TYPE.INVESTIGATION, null, HIT_TYPE.CLICK);
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}


}
