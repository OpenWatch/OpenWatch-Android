package net.openwatch.reporter;

/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.orm.androrm.QuerySet;
import com.viewpagerindicator.TitlePageIndicator;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.constants.Constants.OWFeedType;
import net.openwatch.reporter.feeds.MyFeedFragmentActivity;
import net.openwatch.reporter.feeds.RemoteFeedFragmentActivity;
import net.openwatch.reporter.model.OWFeed;
import net.openwatch.reporter.model.OWTag;
import net.openwatch.reporter.model.OWUser;

/**
 * Demonstrates combining a TabHost with a ViewPager to implement a tab UI
 * that switches between tabs and also allows the user to perform horizontal
 * flicks to move between the tabs.
 */
public class FeedFragmentActivity extends SherlockFragmentActivity {

    TabHost mTabHost;
    ViewPager  mViewPager;
    TabsAdapter mTabsAdapter;
    TitlePageIndicator mTitleIndicator;
    
    HashMap<String, Integer> mTabMap;
    
    LayoutInflater inflater;
    
    public static int display_width = -1;
    
    int internal_user_id = -1;
    
    HashMap<String, Integer> tag_id_map = new HashMap<String, Integer>();
    HashMap<String, Integer> feed_id_map = new HashMap<String, Integer>();
    int nextDirectoryMenuId = 1;
    
    boolean onCreateWon = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_tabs_pager);
        this.getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
       
        mTabMap = new HashMap<String, Integer>();
        
        getDisplayWidth();
        mTabHost = (TabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup();
        mViewPager = (ViewPager)findViewById(R.id.pager);
        mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);
        
        mTitleIndicator = (TitlePageIndicator)findViewById(R.id.titles);
        mTitleIndicator.setViewPager(mViewPager);

        inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        SharedPreferences profile = getSharedPreferences(Constants.PROFILE_PREFS, 0);
	    internal_user_id = profile.getInt(Constants.INTERNAL_USER_ID, 0);
        
        setupFeedAndTagMaps();
        
        populateTabsFromMaps();
        
        // See if initiating intent specified a tab
        if(getIntent().getExtras() != null && getIntent().getExtras().containsKey(Constants.FEED_TYPE) )
        	mTitleIndicator.setCurrentItem(mTabMap.get(((OWFeedType)getIntent().getExtras().getSerializable(Constants.FEED_TYPE)).toString().toLowerCase(Locale.US) ));
        // Try to restore last tab state
        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }
        
        onCreateWon = true;

    }
    
    private void populateTabsFromMaps(){
    	int nextPagerViewId = 0;
    	Bundle feedBundle;
    	
    	Set<String> feeds = feed_id_map.keySet();
    	
    	for(String feed_name : feeds){
    		if(feed_name.compareTo(OWFeedType.RECORDINGS.toString().toLowerCase(Locale.US)) == 0){
    			//TODO: Merge MyFeedFragmentActivity and RemoteFeedFragmentActivity
    			mTabsAdapter.addTab(mTabHost.newTabSpec(getString(R.string.tab_local_user_recordings)).setIndicator(inflateCustomTab(getString(R.string.tab_local_user_recordings))),
    	                MyFeedFragmentActivity.LocalRecordingsListFragment.class, null);
    			mTabMap.put(feed_name, nextPagerViewId);
    		}else{
    			feedBundle = new Bundle(1);
    			feedBundle.putString(Constants.OW_FEED, feed_name);
    	        mTabsAdapter.addTab(mTabHost.newTabSpec(feed_name).setIndicator(inflateCustomTab(feed_name)),
    	                RemoteFeedFragmentActivity.RemoteRecordingsListFragment.class, feedBundle);
    	        mTabMap.put(feed_name, nextPagerViewId);
    		}
    		nextPagerViewId ++;
    	}
    	
    	Set<String> tags = tag_id_map.keySet();
    	
    	for(String tag_name : tags){
    		feedBundle = new Bundle(1);
			feedBundle.putString(Constants.OW_FEED, tag_name);
	        mTabsAdapter.addTab(mTabHost.newTabSpec(tag_name).setIndicator(inflateCustomTab(tag_name)),
	                RemoteFeedFragmentActivity.RemoteRecordingsListFragment.class, feedBundle);
	        mTabMap.put(tag_name, nextPagerViewId);
	        nextPagerViewId ++;
    	}

    }
    
    private void setupFeedAndTagMaps(){
    	feed_id_map.clear();
    	tag_id_map.clear();
    	
    	// Currently no way to poll server for list of feeds
    	// so start with hard-coded feeds
		OWFeedType[] feed_types = OWFeedType.values();
		for(int x=0; x < feed_types.length;x++){
			if(!feed_id_map.containsKey(feed_types[x].toString().toLowerCase(Locale.US))){
    			feed_id_map.put(feed_types[x].toString().toLowerCase(Locale.US), nextDirectoryMenuId);
    			nextDirectoryMenuId ++;
			}
		}
		
		// Once we start polling feeds from the server, we'll add them as well
		QuerySet<OWFeed> feeds = OWFeed.objects(getApplicationContext(), OWFeed.class).all();
		for(OWFeed feed : feeds){
			if( !feed_id_map.containsKey(feed.name.get()) ){
    			feed_id_map.put(feed.name.get(), nextDirectoryMenuId);
    			nextDirectoryMenuId ++;
			}
		}
	
		
		if(internal_user_id > 0){
			QuerySet<OWTag> tags = OWUser.objects(getApplicationContext(), OWUser.class).get(internal_user_id).tags.get(getApplicationContext(), OWUser.objects(getApplicationContext(), OWUser.class).get(internal_user_id));
			for(OWTag tag : tags){
				if(!tag_id_map.containsKey(tag.name.get())){
	    			tag_id_map.put(tag.name.get(), nextDirectoryMenuId);
	    			nextDirectoryMenuId ++;
				}
			}
		}
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tab", mTabHost.getCurrentTabTag());
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.

    	//feed_id_map.clear();
    	//tag_id_map.clear();
		getSupportMenuInflater().inflate(R.menu.fragment_tabs_pager, menu);
		
		MenuItem directory = menu.findItem(R.id.feed_directory);
    	if(directory != null){
    		
    		Set<String> feeds = feed_id_map.keySet();
    		
    		for(String feed_name : feeds){
    			directory.getSubMenu().add(R.id.feeds, feed_id_map.get(feed_name), Menu.NONE, feed_name);
    		}
    		
    		Set<String> tags = tag_id_map.keySet();
    		
    		for(String tag_name : tags){
    			directory.getSubMenu().add(R.id.tags, tag_id_map.get(tag_name), Menu.NONE, "#"+tag_name);
    		}
    		
    		/*
    		
    		QuerySet<OWFeed> feeds = OWFeed.objects(getApplicationContext(), OWFeed.class).all();
    		if(feeds.count() == 0){
    			OWFeedType[] feed_types = OWFeedType.values();
    			for(int x=0; x < feed_types.length;x++){
    				if(!feed_id_map.containsKey(feed_types[x].toString().toLowerCase(Locale.US))){
	    				directory.getSubMenu().add(R.id.tags, nextDirectoryMenuId, Menu.NONE, "#"+feed_types[x].toString().toLowerCase(Locale.US));
	        			feed_id_map.put(feed_types[x].toString().toLowerCase(Locale.US), nextDirectoryMenuId);
	        			nextDirectoryMenuId ++;
    				}
    			}
    		}else{
    			for(OWFeed feed : feeds){
    				if( !feed_id_map.containsKey(feed.name.get()) ){
	    				directory.getSubMenu().add(R.id.feeds, nextDirectoryMenuId, Menu.NONE, feed.name.get());
	        			feed_id_map.put(feed.name.get(), nextDirectoryMenuId);
	        			nextDirectoryMenuId ++;
    				}
        		}
    		}
    		QuerySet<OWTag> tags = OWUser.objects(getApplicationContext(), OWUser.class).get(internal_user_id).tags.get(getApplicationContext(), OWUser.objects(getApplicationContext(), OWUser.class).get(internal_user_id));
    		for(OWTag tag : tags){
    			if(!tag_id_map.containsKey(tag.name.get())){
	    			directory.getSubMenu().add(R.id.tags, nextDirectoryMenuId, Menu.NONE, "#"+tag.name.get());
	    			tag_id_map.put(tag.name.get(), nextDirectoryMenuId);
	    			nextDirectoryMenuId ++;
    			}
    		}
    		*/
    	}
		return true;
	}
    
    @Override
	public boolean onPrepareOptionsMenu(Menu menu){
    	
    	
    	
    	return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost.  It relies on a
     * trick.  Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show.  This is not sufficient for switching
     * between pages.  So instead we make the content part of the tab host
     * 0dp high (it is not shown) and the TabsAdapter supplies its own dummy
     * view to show as the tab content.  It listens to changes in tabs, and takes
     * care of switch to the correct paged in the ViewPager whenever the selected
     * tab changes.
     */
    public static class TabsAdapter extends FragmentPagerAdapter
            implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {
        private final Context mContext;
        private final TabHost mTabHost;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

        static final class TabInfo {
            private final String tag;
            private final Class<?> clss;
            private final Bundle args;

            TabInfo(String _tag, Class<?> _class, Bundle _args) {
                tag = _tag;
                clss = _class;
                args = _args;
            }
        }

        static class DummyTabFactory implements TabHost.TabContentFactory {
            private final Context mContext;

            public DummyTabFactory(Context context) {
                mContext = context;
            }

            @Override
            public View createTabContent(String tag) {
                View v = new View(mContext);
                v.setMinimumWidth(0);
                v.setMinimumHeight(0);
                return v;
            }
        }

        public TabsAdapter(FragmentActivity activity, TabHost tabHost, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mTabHost = tabHost;
            mViewPager = pager;
            mTabHost.setOnTabChangedListener(this);
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {
            tabSpec.setContent(new DummyTabFactory(mContext));
            String tag = tabSpec.getTag();

            TabInfo info = new TabInfo(tag, clss, args);
            mTabs.add(info);
            mTabHost.addTab(tabSpec);
            notifyDataSetChanged();
        }
        
        @Override
        public CharSequence getPageTitle (int position){
        	return mTabs.get(position).tag;
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            TabInfo info = mTabs.get(position);
            return Fragment.instantiate(mContext, info.clss.getName(), info.args);
        }

        @Override
        public void onTabChanged(String tabId) {
            int position = mTabHost.getCurrentTab();
            mViewPager.setCurrentItem(position);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            // Unfortunately when TabHost changes the current tab, it kindly
            // also takes care of putting focus on it when not in touch mode.
            // The jerk.
            // This hack tries to prevent this from pulling focus out of our
            // ViewPager.
            TabWidget widget = mTabHost.getTabWidget();
            int oldFocusability = widget.getDescendantFocusability();
            widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            mTabHost.setCurrentTab(position);
            widget.setDescendantFocusability(oldFocusability);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    }
    
    private View inflateCustomTab(String tab_title){
    	ViewGroup tab = (ViewGroup) inflater.inflate(R.layout.tab_indicator_openwatch, (ViewGroup) this.findViewById(android.R.id.tabs), false);
		((TextView)tab.findViewById(R.id.title)).setText(tab_title);
		return tab;
	}
    
    /**
     * Measure display width so the view pager can implement its 
     * custom behavior re: paging on the map view
     */
    private void getDisplayWidth(){
    	Display display = getWindowManager().getDefaultDisplay();
        display_width = display.getWidth();
    }
}
