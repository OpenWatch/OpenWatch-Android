package net.openwatch.reporter;

import net.openwatch.reporter.FeedActivity.TabsAdapter;
import net.openwatch.reporter.feeds.MyFeedFragmentActivity;
import net.openwatch.reporter.feeds.RemoteFeedFragmentActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.os.Bundle;
import android.app.Activity;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.widget.TabHost;

public class LocalRecordingViewActivity extends FragmentActivity {

	private GoogleMap mMap;
	
	TabHost mTabHost;
    ViewPager  mViewPager;
    TabsAdapter mTabsAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_local_recording_view);
        mTabHost = (TabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup();

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
