package net.openwatch.reporter;

import net.openwatch.reporter.FeedActivity.TabsAdapter;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.feeds.MyFeedFragmentActivity;
import net.openwatch.reporter.feeds.RemoteFeedFragmentActivity;
import net.openwatch.reporter.model.OWLocalRecording;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.os.Bundle;
import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.widget.TabHost;

public class LocalRecordingViewActivity extends FragmentActivity {

	private static final String TAG = "LocalRecordingViewActivity";
	private GoogleMap mMap;
	
	TabHost mTabHost;
    ViewPager  mViewPager;
    TabsAdapter mTabsAdapter;
    
    public static int model_id = -1;
    public static OWLocalRecording recording;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_local_recording_view);
        mTabHost = (TabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup();
        
        try{
        	model_id = getIntent().getExtras().getInt(Constants.VIEW_TAG_MODEL);
        	recording = OWLocalRecording.objects(this, OWLocalRecording.class).get(model_id);
        	//Log.i(TAG, "got model_id : " + String.valueOf(model_id));
        } catch (Exception e){
        	Log.e(TAG, "Could not load Intent extras");
        }

        mViewPager = (ViewPager)findViewById(R.id.pager);

        mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);

        mTabsAdapter.addTab(mTabHost.newTabSpec(getString(R.string.tab_map)).setIndicator(getString(R.string.tab_map)),
        		MapFragment.class, null);
        mTabsAdapter.addTab(mTabHost.newTabSpec(getString(R.string.tab_info)).setIndicator(getString(R.string.tab_info)),
        		LocalRecordingInfoFragment.class, null);

        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }
	}
	
	@Override
	protected void onResume(){
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_local_recording_view, menu);
		return true;
	}
	

}
