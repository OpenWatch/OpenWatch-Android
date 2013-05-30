package org.ale.openwatch;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ScrollView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import java.text.ParseException;

import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.constants.Constants.HIT_TYPE;
import org.ale.openwatch.http.OWServiceRequests;
import org.ale.openwatch.http.OWServiceRequests.RequestCallback;
import org.ale.openwatch.model.OWServerObject;
import org.ale.openwatch.model.OWStory;
import org.ale.openwatch.share.Share;

public class OWStoryViewActivity extends SherlockActivity implements OWMediaObjectBackedEntity{
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
			server_id = OWServerObject.objects(this, OWServerObject.class).get(model_id).server_id.get();
			if( OWServerObject.objects(this, OWServerObject.class).get(model_id).story.get(getApplicationContext()) != null && OWServerObject.objects(this, OWServerObject.class).get(model_id).story.get(getApplicationContext()).body.get() != null){
				populateViews(OWServerObject.objects(this, OWServerObject.class).get(model_id), getApplicationContext());
			} else if(OWServerObject.objects(this, OWServerObject.class).get(model_id).title.get() != null){
				((TextView)findViewById(R.id.title)).setText(OWServerObject.objects(this, OWServerObject.class).get(model_id).title.get());
				this.getSupportActionBar().setTitle(OWServerObject.objects(this, OWServerObject.class).get(model_id).title.get());
				final Context c = this.getApplicationContext();
				int story_server_id = OWServerObject.objects(this, OWServerObject.class).get(model_id).getServerId(c);
				OWServiceRequests.getStory(getApplicationContext(), story_server_id, new RequestCallback(){

					@Override
					public void onFailure() {
						
					}

					@Override
					public void onSuccess() {
						if( OWServerObject.objects(c, OWServerObject.class).get(model_id).story.get(getApplicationContext()).body.get() != null ){
							OWStoryViewActivity.this.populateViews(OWServerObject.objects(c, OWServerObject.class).get(model_id), c);
						}
					}
					
				});
			}
			OWServerObject server_object = OWServerObject.objects(this, OWServerObject.class).get(server_id);
			OWServiceRequests.increaseHitCount(getApplicationContext(), server_id , model_id, server_object.getContentType(getApplicationContext()), server_object.getMediaType(getApplicationContext()), HIT_TYPE.VIEW);
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
                OWServerObject server_object = OWServerObject.objects(this, OWServerObject.class).get(server_id);
				Share.showShareDialogWithInfo(this, getString(R.string.share_story), server_object.getTitle(getApplicationContext()), OWStory.getUrlFromId(server_id));
				OWServiceRequests.increaseHitCount(getApplicationContext(), server_id, model_id, server_object.getContentType(getApplicationContext()), server_object.getMediaType(getApplicationContext()), HIT_TYPE.CLICK);
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
	public void populateViews(OWServerObject media_object, Context app_context) {
		 this.getSupportActionBar().setTitle(media_object.getTitle(app_context));
		((TextView) this.findViewById(R.id.title)).setText(media_object.getTitle(app_context));
		((TextView) this.findViewById(R.id.blurb)).setText(media_object.story.get(app_context).blurb.get());
		((TextView) this.findViewById(R.id.author)).setText("By " + media_object.username.get() + ".");
		try {
			((TextView) this.findViewById(R.id.date)).setText(Constants.user_date_formatter.format(Constants.utc_formatter.parse(media_object.getFirstPosted(app_context))));
		} catch (ParseException e) {
			Log.e(TAG, "Error parsing date string");
			((TextView) this.findViewById(R.id.date)).setText(media_object.getFirstPosted(app_context));
			e.printStackTrace();
		}
		((TextView) this.findViewById(R.id.body)).setMovementMethod(LinkMovementMethod.getInstance());
		//((TextView) this.findViewById(R.id.body)).scrollTo(0, 0);
		((TextView) this.findViewById(R.id.body)).setText(Html.fromHtml(media_object.story.get(app_context).body.get()));
		
		((TextView) this.findViewById(R.id.body)).getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener(){
			
			@Override
			public void onGlobalLayout() {
				if(findViewById(R.id.body_scroll_view) != null){
					((ScrollView)findViewById(R.id.body_scroll_view)).scrollTo(0, 0);
				}
			}
		});
	}
}
