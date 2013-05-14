package net.openwatch.reporter;

import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.JsonHttpResponseHandler;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.http.OWServiceRequests;
import net.openwatch.reporter.model.OWServerObject;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.webkit.WebView;

public class OWInvestigationViewActivity extends Activity implements OWMediaObjectBackedEntity {
	private static final String TAG = "OWInvestigationViewActivity";
	
	private WebView web_view;
	private int model_id;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_owinvestigation_view);
		
		web_view = (WebView)findViewById(R.id.web_view);
		
		model_id = getIntent().getExtras().getInt(Constants.INTERNAL_DB_ID);
		OWServerObject object = OWServerObject.objects(getApplicationContext(), OWServerObject.class).get(model_id);
		populateViews(object, getApplicationContext());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.owinvestigation_view, menu);
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

}
