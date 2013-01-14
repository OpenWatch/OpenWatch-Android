package net.openwatch.reporter;

import java.util.ArrayList;

import net.openwatch.reporter.FeedFragmentActivity.TabsAdapter;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.http.OWServiceRequests;
import net.openwatch.reporter.http.OWServiceRequests.RequestCallback;
import net.openwatch.reporter.model.OWRecording;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import android.content.Context;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.MediaController;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.VideoView;

public class RecordingViewActivity extends SherlockFragmentActivity {

	private static final String TAG = "RecordingViewActivity";
	
	private ArrayList<Fragment> attached_fragments = new ArrayList<Fragment>();

	TabHost mTabHost;
	ViewPager mViewPager;
	TabsAdapter mTabsAdapter;

	public static int model_id = -1;
	boolean is_local = false;
	
	LayoutInflater inflater;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.activity_local_recording_view);
		
		inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		String video_path = null;
		try {
			model_id = getIntent().getExtras().getInt(Constants.INTERNAL_DB_ID);
			if( OWRecording.objects(this, OWRecording.class).get(model_id).local.get(getApplicationContext()) != null ){
				// This is a local recording, attempt to play HQ file
				is_local = true;
				video_path = OWRecording.objects(this, OWRecording.class).get(model_id).local.get(getApplicationContext()).hq_filepath.get();
				
			} else if( OWRecording.objects(this, OWRecording.class).get(model_id).video_url.get() != null ){
				// remote recording, and video_url present
				video_path = OWRecording.objects(this, OWRecording.class).get(model_id).video_url.get();
			} else if (OWRecording.objects(this, OWRecording.class).get(model_id).video_url.get() == null ){
				// remote recording, and need to get video_url
				final Context c = this.getApplicationContext();
				RequestCallback cb = new RequestCallback(){

					@Override
					public void onFailure() {
					
					}

					@Override
					public void onSuccess() {
						if( OWRecording.objects(c, OWRecording.class).get(model_id).video_url.get() != null ){
							setupVideoView(R.id.videoview, OWRecording.objects(c, OWRecording.class).get(model_id).video_url.get());
							((OWRecordingBackedEntity) RecordingViewActivity.this.getMapFragment() ).populateViews(OWRecording.objects(c, OWRecording.class).get(model_id), c);
							((OWRecordingBackedEntity) RecordingViewActivity.this.getInfoFragment() ).populateViews(OWRecording.objects(c, OWRecording.class).get(model_id), c);
						}
					}
					
				};
				OWServiceRequests.getRecording(getApplicationContext(), OWRecording.objects(this, OWRecording.class).get(model_id).uuid.get(), cb);
			}
			
			if(video_path != null){
				Log.i(TAG, "Video uri: " + video_path);
				setupVideoView(R.id.videoview, video_path);
			} else{
				Log.e(TAG, "Recording has no local or remote video uri specified");
			}
			

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
			Bundle fragBundle = new Bundle(1);
			fragBundle.putBoolean(Constants.IS_LOCAL_RECORDING, is_local);
			mTabsAdapter.addTab(mTabHost.newTabSpec(getString(R.string.tab_info))
					.setIndicator(inflateCustomTab(getString(R.string.tab_info))),
					RecordingInfoFragment.class, fragBundle);
	
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
								RecordingViewActivity.this);
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
	
	public void onAttachFragment (Fragment fragment){
    	if(OWRecordingBackedEntity.class.isInstance(fragment))
    		attached_fragments.add((Fragment)fragment);
    }
	
	public Fragment getMapFragment(){
		if(attached_fragments.size() == 2)
			return attached_fragments.get(0);
		return null;
	}
	
	public Fragment getInfoFragment(){
		if(attached_fragments.size() == 2)
			return attached_fragments.get(1);
		return null;
	}

}
