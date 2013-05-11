package net.openwatch.reporter;

import java.util.ArrayList;

import net.openwatch.reporter.FeedFragmentActivity.TabsAdapter;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.constants.Constants.CONTENT_TYPE;
import net.openwatch.reporter.constants.Constants.HIT_TYPE;
import net.openwatch.reporter.http.OWServiceRequests;
import net.openwatch.reporter.http.OWServiceRequests.RequestCallback;
import net.openwatch.reporter.model.OWServerObject;
import net.openwatch.reporter.model.OWStory;
import net.openwatch.reporter.model.OWVideoRecording;
import net.openwatch.reporter.share.Share;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.VideoView;

public class RecordingViewActivity extends SherlockFragmentActivity {

	private static final String TAG = "RecordingViewActivity";
	
	private ArrayList<Fragment> attached_fragments = new ArrayList<Fragment>();

	TabHost mTabHost;
	ViewPager mViewPager;
	TabsAdapter mTabsAdapter;
	VideoView video_view;

	public static int model_id = -1;
	int server_id = -1;
	boolean is_local = false;
	boolean is_user_recording = false;
	boolean video_playing = false;
	boolean is_landscape = false;
	
	LayoutInflater inflater;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			is_landscape = true;
		}else
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	
		setContentView(R.layout.activity_local_recording_view);
		video_view = (VideoView) findViewById(R.id.videoview);
		if(!is_landscape){
			//make a guess of the videoView height so when we fit it to the loaded video
			// the swap isn't too jarring
			Display display = getWindowManager().getDefaultDisplay();
			int height;
			if(Build.VERSION.SDK_INT >=11){
				Point size = new Point();
				display.getSize(size);
				height = (int)(size.x * 3 / 4.0); // assuming 4:3 aspect
			}else{
				height = (int)(display.getWidth() * 3 / 4.0); 
			}
			video_view.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, height));
		}

		inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		String video_path = null;
		try {
			model_id = getIntent().getExtras().getInt(Constants.INTERNAL_DB_ID);
			OWServerObject media_obj = OWServerObject.objects(this, OWServerObject.class).get(model_id);
			SharedPreferences prefs = this.getSharedPreferences(Constants.PROFILE_PREFS, MODE_PRIVATE);
			int user_id = prefs.getInt(DBConstants.USER_SERVER_ID, 0);
			if(user_id != 0){
				Log.i("UserRecCheck", "user_id " + user_id + "media_user_id: " + media_obj.user.get(getApplicationContext()).server_id.get());
				if (media_obj.user.get(getApplicationContext()) != null && user_id == media_obj.user.get(getApplicationContext()).server_id.get()){
					is_user_recording = true;
				}
			}
			server_id = media_obj.server_id.get();
			if(!is_landscape && media_obj != null && media_obj.title.get() != null)
				this.getSupportActionBar().setTitle(media_obj.title.get());
			if( media_obj.local_video_recording.get(getApplicationContext()) != null ){
				// This is a local recording, attempt to play HQ file
				is_local = true;
				video_path = media_obj.local_video_recording.get(getApplicationContext()).hq_filepath.get();
				
			} else if( media_obj.video_recording.get(getApplicationContext()) != null && media_obj.video_recording.get(getApplicationContext()).video_url.get() != null){
				// remote recording, and video_url present
				video_path = media_obj.video_recording.get(getApplicationContext()).video_url.get();
			}
			
			fetchOWRecording(model_id);
			
			if(video_path != null){
				Log.i(TAG, "Video uri: " + video_path);
				setupVideoView(R.id.videoview, video_path);
			} else{
				Log.e(TAG, "Recording has no local or remote video uri specified");
			}
			if(server_id > 0)
				OWServiceRequests.increaseHitCount(getApplicationContext(), server_id, model_id, CONTENT_TYPE.VIDEO, HIT_TYPE.VIEW);
			// Log.i(TAG, "got model_id : " + String.valueOf(model_id));
		} catch (Exception e) {
			Log.e(TAG, "Could not load Intent extras");
			e.printStackTrace();
		}

		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
			mTabHost = (TabHost) findViewById(android.R.id.tabhost);
			mTabHost.setup();
			mTabHost.requestFocus();
			mViewPager = (ViewPager) findViewById(R.id.pager);
			mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);
			
			Bundle fragBundle = new Bundle(1);
			fragBundle.putBoolean(Constants.IS_LOCAL_RECORDING, is_local);
			fragBundle.putBoolean(Constants.IS_USER_RECORDING, is_user_recording);
			mTabsAdapter.addTab(mTabHost.newTabSpec(getString(R.string.tab_info))
					.setIndicator(inflateCustomTab(getString(R.string.tab_info))),
					OWMobileGeneratedObjectInfoFragment.class, fragBundle);
					
			mTabsAdapter.addTab(mTabHost.newTabSpec(getString(R.string.tab_map))
					.setIndicator(inflateCustomTab(getString(R.string.tab_map))), MapFragment.class,
					null);
	
			if (savedInstanceState != null) {
				mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
			}
		}
	}


	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getSupportMenuInflater().inflate(R.menu.activity_local_recording_view, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu){
		if(!is_local){
			menu.removeItem(R.id.menu_delete);
			
		}
		if(!is_user_recording){
			menu.removeItem(R.id.menu_save);
		}
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.menu_save:
			finish();
			return true;
		case R.id.menu_share:
			if(server_id > 0){
				Share.showShareDialog(this, getString(R.string.share_story), OWVideoRecording.getUrlFromId(server_id));
				OWServiceRequests.increaseHitCount(getApplicationContext(), server_id, model_id, CONTENT_TYPE.VIDEO, HIT_TYPE.CLICK);
			}
			break;
		case R.id.menu_delete:
			OWMediaObjectViewFunctions.showDeleteDialog(this, model_id);
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void fetchOWRecording(final int model_id){
		// remote recording, and need to get video_url
		final Context c = this.getApplicationContext();
		RequestCallback cb = new RequestCallback(){

			@Override
			public void onFailure() {
			
			}

			@Override
			public void onSuccess() {
				if( OWServerObject.objects(c, OWServerObject.class).get(model_id).video_recording.get(c).video_url.get() != null ){
					if(!video_playing)
						setupVideoView(R.id.videoview, OWServerObject.objects(c, OWServerObject.class).get(model_id).video_recording.get(c).video_url.get());
					if(getMapFragment() != null)
						((OWMediaObjectBackedEntity) RecordingViewActivity.this.getMapFragment() ).populateViews(OWServerObject.objects(c, OWServerObject.class).get(model_id), c);
					if(getInfoFragment() != null)
					((OWMediaObjectBackedEntity) RecordingViewActivity.this.getInfoFragment() ).populateViews(OWServerObject.objects(c, OWServerObject.class).get(model_id), c);
				}
			}
			
		};
		OWServiceRequests.getRecording(c, OWServerObject.objects(this, OWServerObject.class).get(model_id).video_recording.get(c).uuid.get(), cb);
	}

	public void setVideoViewVisible(boolean visible) {
		View video = findViewById(R.id.videoview);
		if (visible) {
			video.setVisibility(View.VISIBLE);
		} else {
			video.setVisibility(View.GONE);
		}

	}
	
	private View inflateCustomTab(String tab_title){
    	ViewGroup tab = (ViewGroup) inflater.inflate(R.layout.tab_indicator_openwatch, (ViewGroup) this.findViewById(android.R.id.tabs), false);
		((TextView)tab.findViewById(R.id.title)).setText(tab_title);
		return tab;
	}

	public void setupVideoView(int view_id, String filepath) {
		VideoView video_view = (VideoView) findViewById(view_id);
		video_view.setVideoURI(Uri.parse(filepath));
		video_view.setOnPreparedListener(new OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				mp.setOnVideoSizeChangedListener(new OnVideoSizeChangedListener() {
					@Override
					public void onVideoSizeChanged(MediaPlayer mp, int width,
							int height) {
						VideoView video_view = (VideoView) findViewById(R.id.videoview);
						//video_view.setVisibility(View.VISIBLE);
						(findViewById(R.id.progress_container)).setVisibility(View.GONE);
						video_view.setLayoutParams( new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
						MediaController mc = new MediaController(
								RecordingViewActivity.this);
						video_view.setMediaController(mc);
						mc.setAnchorView(video_view);
						video_view.requestFocus();
						video_view.start();
						video_playing = true;
					}
				});
			}
		});
		video_view.start();
	}
	
	public void onAttachFragment (Fragment fragment){
    	if(OWMediaObjectBackedEntity.class.isInstance(fragment))
    		attached_fragments.add((Fragment)fragment);
    }
	
	public Fragment getMapFragment(){
		if(attached_fragments.size() == 2)
			return attached_fragments.get(1);
		return null;
	}
	
	public Fragment getInfoFragment(){
		if(attached_fragments.size() == 2)
			return attached_fragments.get(0);
		return null;
	}

}
