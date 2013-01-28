package net.openwatch.reporter;

import java.text.ParseException;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.Constants.CONTENT_TYPE;
import net.openwatch.reporter.constants.Constants.HIT_TYPE;
import net.openwatch.reporter.http.OWServiceRequests;
import net.openwatch.reporter.http.OWServiceRequests.RequestCallback;
import net.openwatch.reporter.model.OWMediaObject;
import net.openwatch.reporter.model.OWStory;
import net.openwatch.reporter.model.OWVideoRecording;
import net.openwatch.reporter.share.Share;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ScrollView;
import android.widget.TextView;

public class StoryViewActivity extends SherlockActivity implements OWMediaObjectBackedEntity{
	private static final String TAG = "StoryViewActivity";
	
	static int model_id = -1;
	int server_id = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_story_view);
		// Show the Up button in the action bar.
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		setCustomFont();
		
		try{
			model_id = getIntent().getExtras().getInt(Constants.INTERNAL_DB_ID);
			server_id = OWMediaObject.objects(this, OWMediaObject.class).get(model_id).server_id.get();
			if( OWMediaObject.objects(this, OWMediaObject.class).get(model_id).story.get(getApplicationContext()) != null && OWMediaObject.objects(this, OWMediaObject.class).get(model_id).story.get(getApplicationContext()).body.get() != null){
				populateViews(OWMediaObject.objects(this, OWMediaObject.class).get(model_id), getApplicationContext());
			} else if(OWMediaObject.objects(this, OWMediaObject.class).get(model_id).title.get() != null){
				((TextView)findViewById(R.id.title)).setText(OWMediaObject.objects(this, OWMediaObject.class).get(model_id).title.get());
				this.getSupportActionBar().setTitle(OWMediaObject.objects(this, OWMediaObject.class).get(model_id).title.get());
				final Context c = this.getApplicationContext();
				int story_server_id = OWMediaObject.objects(this, OWMediaObject.class).get(model_id).getServerId(c);
				OWServiceRequests.getStory(getApplicationContext(), story_server_id, new RequestCallback(){

					@Override
					public void onFailure() {
						
					}

					@Override
					public void onSuccess() {
						if( OWMediaObject.objects(c, OWMediaObject.class).get(model_id).story.get(getApplicationContext()).body.get() != null ){
							StoryViewActivity.this.populateViews(OWMediaObject.objects(c, OWMediaObject.class).get(model_id), c);
						}
					}
					
				});
			}
			OWServiceRequests.increaseHitCount(getApplicationContext(), OWMediaObject.objects(this, OWMediaObject.class).get(model_id).server_id.get(), model_id, CONTENT_TYPE.STORY, HIT_TYPE.VIEW);
		}catch(Exception e){
			Log.e(TAG, "Error retrieving model");
			e.printStackTrace();
		}
	}
	
	private void setCustomFont(){
		Typeface font = Typeface.createFromAsset(getAssets(), "Palatino.ttc");  
		ViewGroup container = (ViewGroup) findViewById(R.id.story_container);
		View this_view;
		for (int x = 0; x < container.getChildCount(); x++) {
			this_view = container.getChildAt(x);
			if(this_view.getTag() != null){
				if(this_view.getTag().toString().compareTo("custom_font") == 0){
					((TextView)this_view).setTypeface(font, Typeface.NORMAL);
				}else if(this_view.getTag().toString().compareTo("custom_font_bold") == 0){
					((TextView)this_view).setTypeface(font, Typeface.BOLD);
				}else if(this_view.getTag().toString().compareTo("custom_font_italic") == 0){
					((TextView)this_view).setTypeface(font, Typeface.ITALIC);
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.activity_story_view, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.menu_share:
			if(server_id > 0){
				Share.showShareDialog(this, getString(R.string.share_story), OWStory.getUrlFromId(server_id));
				OWServiceRequests.increaseHitCount(getApplicationContext(), server_id, model_id, CONTENT_TYPE.STORY, HIT_TYPE.CLICK);
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onResume(){
		super.onResume();
	}

	@Override
	public void populateViews(OWMediaObject media_object, Context app_context) {
		 this.getSupportActionBar().setTitle(media_object.getTitle(app_context));
		((TextView) this.findViewById(R.id.title)).setText(media_object.getTitle(app_context));
		((TextView) this.findViewById(R.id.blurb)).setText(media_object.story.get(app_context).blurb.get());
		((TextView) this.findViewById(R.id.author)).setText(media_object.username.get());
		try {
			((TextView) this.findViewById(R.id.date)).setText(Constants.user_datetime_formatter.format(Constants.utc_formatter.parse(media_object.getFirstPosted(app_context))));
		} catch (ParseException e) {
			Log.e(TAG, "Error parsing date string");
			((TextView) this.findViewById(R.id.date)).setText(media_object.getFirstPosted(app_context));
			e.printStackTrace();
		}
		((TextView) this.findViewById(R.id.body)).setMovementMethod(LinkMovementMethod.getInstance());
		((TextView) this.findViewById(R.id.body)).setText(Html.fromHtml(media_object.story.get(app_context).body.get()));
		if(findViewById(R.id.body_scroll_view) != null){
			((ScrollView)findViewById(R.id.body_scroll_view)).pageScroll(ScrollView.FOCUS_UP);
		}
	}
}
