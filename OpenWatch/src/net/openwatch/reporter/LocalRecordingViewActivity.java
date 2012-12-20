package net.openwatch.reporter;

import net.openwatch.reporter.FeedActivity.TabsAdapter;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.model.OWLocalRecording;

import com.google.android.gms.maps.GoogleMap;

import android.app.Activity;
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
import android.view.Menu;
import android.view.View;
import android.widget.MediaController;
import android.widget.TabHost;
import android.widget.VideoView;

public class LocalRecordingViewActivity extends FragmentActivity {

	private static final String TAG = "LocalRecordingViewActivity";

	private GoogleMap mMap;

	TabHost mTabHost;
	ViewPager mViewPager;
	TabsAdapter mTabsAdapter;

	public static int model_id = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_local_recording_view);
		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup();
		mTabHost.requestFocus();
		try {
			model_id = getIntent().getExtras().getInt(Constants.INTERNAL_DB_ID);
			//Log.i(TAG, "HQ_filepath: " + OWLocalRecording.objects(this, OWLocalRecording.class).get(
			//		model_id).hq_filepath.get());
			setupVideoView(
					R.id.videoview,
					OWLocalRecording.objects(this, OWLocalRecording.class).get(
							model_id).hq_filepath.get());
			

			// Log.i(TAG, "got model_id : " + String.valueOf(model_id));
		} catch (Exception e) {
			Log.e(TAG, "Could not load Intent extras");
			e.printStackTrace();
		}

		mViewPager = (ViewPager) findViewById(R.id.pager);

		mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);

		mTabsAdapter.addTab(mTabHost.newTabSpec(getString(R.string.tab_map))
				.setIndicator(getString(R.string.tab_map)), MapFragment.class,
				null);
		mTabsAdapter.addTab(mTabHost.newTabSpec(getString(R.string.tab_info))
				.setIndicator(getString(R.string.tab_info)),
				LocalRecordingInfoFragment.class, null);

		if (savedInstanceState != null) {
			mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_local_recording_view, menu);
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
