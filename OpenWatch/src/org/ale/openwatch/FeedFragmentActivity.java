package org.ale.openwatch;

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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionProvider;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.viewpagerindicator.TitlePageIndicator;

import org.ale.openwatch.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;

import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.constants.Constants.OWFeedType;
import org.ale.openwatch.database.DatabaseManager;
import org.ale.openwatch.feeds.MyFeedFragmentActivity;
import org.ale.openwatch.feeds.RemoteFeedFragmentActivity;
import org.ale.openwatch.http.OWServiceRequests;
import org.ale.openwatch.model.OWUser;

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
    
    HashMap<String, Integer> mTitleToTabId = new HashMap<String, Integer>();
    
    LayoutInflater inflater;
    
    public static int display_width = -1;
    
    int internal_user_id = -1;
    
    //ArrayList<String> tags = new ArrayList<String>();
    ArrayList<String> feeds = new ArrayList<String>(); // tags and feeds are lowercase
    int nextDirectoryMenuId = 1;
    
    boolean onCreateWon = false;

    // Temporary drawer items
    private String[] mPlanetTitles = new String[] {"Settings", "Profile"};
    DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_tabs_pager);
        this.getSupportActionBar().setDisplayShowTitleEnabled(false);

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
        	mTitleIndicator.setCurrentItem(mTitleToTabId.get(((OWFeedType)getIntent().getExtras().getSerializable(Constants.FEED_TYPE)).toString() ));
        // Try to restore last tab state
        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }
        
        onCreateWon = true;
        checkUserStatus();

        // Drawer

        ListView mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mPlanetTitles));
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                getActionBar().setTitle("");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle("");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

    }
    
    private void populateTabsFromMaps(){
    	int nextPagerViewId = 0;
    	Bundle feedBundle;
    	    		
    	for(String feed : feeds){
    		if(feed.compareTo(OWFeedType.USER.toString()) == 0){
    			//TODO: Merge MyFeedFragmentActivity and RemoteFeedFragmentActivity
    			mTabsAdapter.addTab(mTabHost.newTabSpec(getString(Constants.FEED_TO_TITLE.get(feed))).setIndicator(inflateCustomTab(getString(Constants.FEED_TO_TITLE.get(feed)))),
    	                MyFeedFragmentActivity.LocalRecordingsListFragment.class, null);
    			mTitleToTabId.put(feed, nextPagerViewId);
    		}else{
    			feedBundle = new Bundle(1);
    			feedBundle.putString(Constants.OW_FEED, feed);
                mTabsAdapter.addTab(mTabHost.newTabSpec(getString(Constants.FEED_TO_TITLE.get(feed))).setIndicator(inflateCustomTab(getString(Constants.FEED_TO_TITLE.get(feed)))),
    	                RemoteFeedFragmentActivity.RemoteRecordingsListFragment.class, feedBundle);
    	        mTitleToTabId.put(feed, nextPagerViewId);
    		}
    		nextPagerViewId ++;
    	}
    	   /* 	
    	for(String tag : tags){
    		feedBundle = new Bundle(1);
			feedBundle.putString(Constants.OW_FEED, tag);
	        mTabsAdapter.addTab(mTabHost.newTabSpec("#"+tag).setIndicator(inflateCustomTab("#"+tag)),
	                RemoteFeedFragmentActivity.RemoteRecordingsListFragment.class, feedBundle);
	        mTitleToTabId.put(tag, nextPagerViewId);
	        nextPagerViewId ++;
    	}
    	    */
    }
    
    private void setupFeedAndTagMaps(){
    	feeds.clear();
    	//tags.clear();
    	
    	// Currently no way to poll server for list of feeds
    	// so start with hard-coded feeds
		OWFeedType[] feed_types = OWFeedType.values();
		for(int x=0; x < feed_types.length;x++){
			if(!feeds.contains(feed_types[x])){
    			feeds.add(feed_types[x].toString());
			}
		}
		Collections.sort(feeds);
		Collections.reverse(feeds);
		
		// Once we start polling feeds from the server, we'll add them as well
		// Will have to resolve the issue where tags are added as feeds
		// to allow caching. i.e: we query each tag view by media object's tag feed relation
		// not the mere presence of a tag, since the server's feed sort is unpredictable
		/*
		QuerySet<OWFeed> feed_set = OWFeed.objects(getApplicationContext(), OWFeed.class).all();
		for(OWFeed feed : feed_set){
			if( !feeds.contains(feed.name.get()) ){
    			feeds.add(feed.name.get());
			}
		}
		*/
		/*
		if(internal_user_id > 0){
			QuerySet<OWTag> tag_set = OWUser.objects(getApplicationContext(), OWUser.class).get(internal_user_id).tags.get(getApplicationContext(), OWUser.objects(getApplicationContext(), OWUser.class).get(internal_user_id));
			for(OWTag tag : tag_set){
				if(!tags.contains(tag.name.get())){
	    			tags.add(tag.name.get());
				}
			}
			Collections.sort(tags);
		}
		*/
    }
    
    private String capitalizeFirstChar(String in){
    	in = in.toLowerCase();
    	return Character.toString(in.charAt(0)).toUpperCase()+in.substring(1);
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

        /*
		MenuItem directory = menu.findItem(R.id.tab_directory);
    	if(directory != null){
    		    		
    		for(String feed_name : feeds){
    		    directory.getSubMenu().add(R.id.feeds, mTitleToTabId.get(feed_name), Menu.NONE, getString(Constants.FEED_TO_TITLE.get(feed_name)));
    		}

    		for(String tag_name : tags){
    			directory.getSubMenu().add(R.id.tags, mTitleToTabId.get(tag_name), Menu.NONE, "#"+tag_name);
    		}

    		
    	}
        */
		return true;
	}
    
    @Override
	public boolean onPrepareOptionsMenu(Menu menu){

    	return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(getMenuItem(item))) {
            return true;
        }
    	Log.i("ItemSelected", String.valueOf(item.getItemId()));
    	if(item.getGroupId() == R.id.feeds || item.getGroupId() == R.id.tags){
    		mTitleIndicator.setCurrentItem(item.getItemId());
    	}
		switch (item.getItemId()) {
            case R.id.tab_record:
                Intent i = new Intent(this, RecorderActivity.class);
                startActivity(i);
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

    public void checkUserStatus(){

        boolean debug_fancy = false;

        SharedPreferences profile = getSharedPreferences(Constants.PROFILE_PREFS, 0);
        boolean authenticated = profile.getBoolean(Constants.AUTHENTICATED, false);
        boolean db_initialized = profile.getBoolean(Constants.DB_READY, false);

        if(!db_initialized){
            DatabaseManager.setupDB(getApplicationContext()); // do this every time to auto handle migrations
            //DatabaseManager.testDB(this);
        }else{
            DatabaseManager.registerModels(getApplicationContext()); // ensure androrm is set to our custom Database name.
        }

        if(authenticated && db_initialized && !((OWApplication) this.getApplicationContext()).per_launch_sync){
            // TODO: Attempt to login with stored credentials and report back if error

            OWMediaSyncer.syncMedia(getApplicationContext());
            ((OWApplication) getApplicationContext()).per_launch_sync = true;
            // If we have a User object for the current user, and they've applied as an agent, send their current location
            if(profile.getInt(Constants.INTERNAL_USER_ID, 0) != 0){
                OWUser user = OWUser.objects(getApplicationContext(), OWUser.class).get(profile.getInt(Constants.INTERNAL_USER_ID,0));
                if(user.agent_applicant.get() == true){
                    Log.i("MainActivity", "Sending agent location");
                    OWServiceRequests.syncOWUser(getApplicationContext(), user);
                }
            }
        }
        if(debug_fancy || (!authenticated && !this.getIntent().hasExtra(Constants.AUTHENTICATED) ) ){
            Intent i = new Intent(this, FancyLoginActivity.class	);
            //Intent i = new Intent(this, LoginActivity.class	);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            String email = profile.getString(Constants.EMAIL, null);
            if(email != null)
                i.putExtra(Constants.EMAIL, email);
            startActivity(i);
            // This will set OWApplication.user_data
        }
        else if(authenticated){
            // If the user state is stored, load user_data into memory
            OWApplication.user_data = getApplicationContext().getSharedPreferences(Constants.PROFILE_PREFS, getApplicationContext().MODE_PRIVATE).getAll();
        }

    }

    private android.view.MenuItem getMenuItem(final MenuItem item) {
        return new android.view.MenuItem() {
            @Override
            public int getItemId() {
                return item.getItemId();
            }

            public boolean isEnabled() {
                return true;
            }

            @Override
            public boolean collapseActionView() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean expandActionView() {
                // TODO Auto-generated method stub
                return false;
            }
            /*
            @Override
            public ActionProvider getActionProvider() {
                // TODO Auto-generated method stub
                return null;
            }*/

            @Override
            public View getActionView() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public android.view.MenuItem setActionProvider(android.view.ActionProvider actionProvider) {
                return null;
            }

            @Override
            public android.view.ActionProvider getActionProvider() {
                return null;
            }

            @Override
            public char getAlphabeticShortcut() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public int getGroupId() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public Drawable getIcon() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Intent getIntent() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public ContextMenu.ContextMenuInfo getMenuInfo() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public char getNumericShortcut() {
                // TODO Auto-generated method stub
                return 0;
            }

            @Override
            public int getOrder() {
                // TODO Auto-generated method stub
                return 0;
            }


            @Override
            public CharSequence getTitle() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public CharSequence getTitleCondensed() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public boolean hasSubMenu() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public android.view.SubMenu getSubMenu() {
                return null;
            }

            @Override
            public boolean isActionViewExpanded() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean isCheckable() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean isChecked() {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public boolean isVisible() {
                // TODO Auto-generated method stub
                return false;
            }


            @Override
            public android.view.MenuItem setActionView(View view) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public android.view.MenuItem setActionView(int resId) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public android.view.MenuItem setAlphabeticShortcut(char alphaChar) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public android.view.MenuItem setCheckable(boolean checkable) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public android.view.MenuItem setChecked(boolean checked) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public android.view.MenuItem setEnabled(boolean enabled) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public android.view.MenuItem setIcon(Drawable icon) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public android.view.MenuItem setIcon(int iconRes) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public android.view.MenuItem setIntent(Intent intent) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public android.view.MenuItem setNumericShortcut(char numericChar) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public android.view.MenuItem setOnActionExpandListener(OnActionExpandListener listener) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public android.view.MenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public android.view.MenuItem setShortcut(char numericChar, char alphaChar) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void setShowAsAction(int actionEnum) {
                // TODO Auto-generated method stub

            }

            @Override
            public android.view.MenuItem setShowAsActionFlags(int actionEnum) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public android.view.MenuItem setTitle(CharSequence title) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public android.view.MenuItem setTitle(int title) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public android.view.MenuItem setTitleCondensed(CharSequence title) {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public android.view.MenuItem setVisible(boolean visible) {
                // TODO Auto-generated method stub
                return null;
            }
        };
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
}
