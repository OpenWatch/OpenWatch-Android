package net.openwatch.reporter;

import net.openwatch.reporter.FeedActivity.TabsAdapter;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.model.OWRecording;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.google.android.gms.maps.GoogleMap;

import android.content.Context;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.VideoView;

public class LocalRecordingViewActivity extends SherlockFragmentActivity {

	private static final String TAG = "LocalRecordingViewActivity";

	private GoogleMap mMap;

	TabHost mTabHost;
	ViewPager mViewPager;
	TabsAdapter mTabsAdapter;

	public static int model_id = -1;
	
	LayoutInflater inflater;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.activity_local_recording_view);
		
		inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		try {
			model_id = getIntent().getExtras().getInt(Constants.INTERNAL_DB_ID);
			Log.i(TAG, "HQ_filepath: " + OWRecording.objects(this, OWRecording.class).get(
					model_id).local.get(getApplicationContext()).hq_filepath.get());
			setupVideoView(
					R.id.videoview,
					OWRecording.objects(this, OWRecording.class).get(
							model_id).local.get(getApplicationContext()).hq_filepath.get());
			

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
					
			mTabsAdapter.addTab(mTabHost.newTabSpec(getString(R.string.tab_map))
					.setIndicator(inflateCustomTab(getString(R.string.tab_map))), MapFragment.class,
					null);
			mTabsAdapter.addTab(mTabHost.newTabSpec(getString(R.string.tab_info))
					.setIndicator(inflateCustomTab(getString(R.string.tab_info))),
					LocalRecordingInfoFragment.class, null);
	
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
						MediaController mc = new MediaController(
								LocalRecordingViewActivity.this);
						VideoView video_view = (VideoView) findViewById(R.id.videoview);
						video_view.setMediaController(mc);
						mc.setAnchorView(video_view);
						video_view.requestFocus();
						video_view.start();
					}
				});
			}
		});
		video_view.start();
	}

}
