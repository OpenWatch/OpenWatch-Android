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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bugsense.trace.BugSenseHandler;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.orm.androrm.QuerySet;
import com.viewpagerindicator.TitlePageIndicator;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.constants.Constants.OWFeedType;
import org.ale.openwatch.feeds.RemoteRecordingsListFragment;
import org.ale.openwatch.gcm.GCMUtils;
import org.ale.openwatch.model.OWTag;
import org.ale.openwatch.model.OWUser;
import org.json.JSONException;
import org.json.JSONObject;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.ale.openwatch.OWUtils.checkUserStatus;

/**
 * Demonstrates combining a TabHost with a ViewPager to implement a tab UI
 * that switches between tabs and also allows the user to perform horizontal
 * flicks to move between the tabs.
 */
public class FeedFragmentActivity extends SherlockFragmentActivity {
    private static String TAG = "FeedFragmentActivity";
    TabHost mTabHost;
    ViewPager  mViewPager;
    TabsAdapter mTabsAdapter;
    TitlePageIndicator mTitleIndicator;
    int nextPagerViewId;

    public VideoView videoView;
    public TextureView textureView;
    public ProgressBar progressBar;
    public ViewGroup videoViewParent;
    public ViewGroup videoViewHostCell;
    public int videoViewListIndex;
    
    HashMap<String, Integer> mTitleToTabId = new HashMap<String, Integer>();
    
    LayoutInflater inflater;
    
    public static int display_width = -1;
    
    int internal_user_id = -1;
    
    ArrayList<String> tags = new ArrayList<String>();
    ArrayList<String> feeds = new ArrayList<String>(); // tags and feeds are lowercase
    int nextDirectoryMenuId = 1;
    
    boolean onCreateWon = false;
    public boolean forceUserFeedRefresh = false;

    // Temporary drawer items
    private ArrayList<String> mDrawerItems = new ArrayList<String>() {{ add("Profile"); add("Settings");  add("Send Feedback");}};
    private HashMap<String, Integer> drawerTitleToIcon = new HashMap<String, Integer>() {{put("Settings", R.drawable.settings); put("Profile", R.drawable.user_placeholder); put("Send Feedback", R.drawable.heart);}};
    DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ListView mDrawerList;

    public PullToRefreshAttacher mPullToRefreshAttacher;
    int lastPagePosition = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");

        mPullToRefreshAttacher =  PullToRefreshAttacher.get(this);
        //mTabHost.setOn
        setContentView(R.layout.fragment_tabs_pager);
        this.getSupportActionBar().setDisplayShowTitleEnabled(false);
        BugSenseHandler.initAndStartSession(getApplicationContext(), SECRETS.BUGSENSE_API_KEY);

        checkUserStatus(this);
        GCMUtils.setUpGCM(this);

        getDisplayWidth();
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(view, position);
            }
        });
        mTabHost = (TabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup();
        mViewPager = (ViewPager)findViewById(R.id.pager);
        mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);

        mTitleIndicator = (TitlePageIndicator)findViewById(R.id.titles);
        mTitleIndicator.setViewPager(mViewPager);

        inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        SharedPreferences profile = getSharedPreferences(Constants.PROFILE_PREFS, 0);
	    internal_user_id = profile.getInt(Constants.INTERNAL_USER_ID, 0);
        String userThumbnailUrl = null;
        if(internal_user_id > 0)
            userThumbnailUrl = OWUser.objects(getApplicationContext(), OWUser.class).get(internal_user_id).thumbnail_url.get();
        
        setupFeedAndTagMaps();
        
        populateTabsFromMaps();
        
        // See if initiating intent specified a tab via url, feed type
        if(checkIntentForUri(getIntent())){
        }else if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }else{
            // set default tab to Top Stories
            mTitleIndicator.setCurrentItem(2);
        }
        
        onCreateWon = true;

        ListView mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        mDrawerItems.add("divider");
        mDrawerItems.addAll(feeds);
        mDrawerItems.add("divider");
        mDrawerItems.addAll(tags);
        mDrawerList.setAdapter(new DrawerItemAdapter(this,
                mDrawerItems, drawerTitleToIcon, feeds, userThumbnailUrl));
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle("");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle("");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mTitleIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                try {
                    JSONObject analyticsPayload = new JSONObject().put(Analytics.feed,FeedFragmentActivity.this.mTabsAdapter.getPageTitle(i));
                    Analytics.trackEvent(getApplicationContext(), Analytics.SELECTED_FEED, analyticsPayload);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if(mPullToRefreshAttacher != null)
                    mPullToRefreshAttacher.setRefreshComplete();
                Log.i(TAG, String.format("page %d selected", i));
                removeVideoView();
                if(lastPagePosition != -1){
                    RemoteRecordingsListFragment frag = ((RemoteRecordingsListFragment) FeedFragmentActivity.this.mTabsAdapter.getItem(lastPagePosition));
                    if( frag != null){
                        frag.detachPullToRefresh();

                    } else
                        Log.i("PTR", String.format("fragment at page %d is null",i));
                }
                lastPagePosition = i;
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

    }

    @Override
    public void onResume(){
        super.onResume();
        Log.i(TAG, "onResume");
        if(internal_user_id > 0){
            RelativeLayout profileDrawerItem = (RelativeLayout) findViewById(R.id.profileRow);
            if(profileDrawerItem != null){
                String userThumbnailUrl = OWUser.objects(getApplicationContext(), OWUser.class).get(internal_user_id).thumbnail_url.get();
                if(userThumbnailUrl != null){
                    ImageLoader.getInstance().displayImage(userThumbnailUrl, (ImageView) profileDrawerItem.findViewById(R.id.icon));
                }
            }
        }

    }

    @Override
    public void onPause(){
        removeVideoView();
        super.onPause();
    }

    @Override
    protected void onNewIntent (Intent intent){
        Log.i(TAG, "onNewIntent");
        checkIntentForUri(intent);
    }

    private boolean checkIntentForUri(Intent intent){
        Uri data = intent.getData();
        if(data != null){
            //String scheme = data.getScheme(); // "openwatch"
            List<String> params = data.getPathSegments();
            String tag = params.get(1); // "police"
            Log.i(TAG, "got tag from url: " + tag);
            if(!mTitleToTabId.containsKey(tag)){
                addTagOrFeed(tag);
            }
            mTitleIndicator.setCurrentItem(mTitleToTabId.get(tag.toLowerCase()));
            return true;

        }else if(intent.getExtras() != null && intent.getExtras().containsKey(Constants.FEED_TYPE) ){
            OWFeedType feedType = ((OWFeedType)intent.getExtras().getSerializable(Constants.FEED_TYPE));
            if(feedType == OWFeedType.USER){
                // Force the user feed to refresh
                RemoteRecordingsListFragment userFrag = ((RemoteRecordingsListFragment) FeedFragmentActivity.this.mTabsAdapter.getItem(mTitleToTabId.get(feedType.toString().toLowerCase())));
                FeedFragmentActivity.this.forceUserFeedRefresh = true;
                //userFrag.didRefreshFeed = false;
                //userFrag.forceRefresh = true;
                Log.i(feedType.toString(), "force refresh feed now!");
            }
            mTitleIndicator.setCurrentItem(mTitleToTabId.get(feedType.toString().toLowerCase() ));
            return true;
        }

        if(intent.getBooleanExtra(Constants.VICTORY, false)){
            View v = getLayoutInflater().inflate(R.layout.dialog_victory, null);
            new AlertDialog.Builder(this)
                    .setView(v)
                    .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
        }

        return false;
    }
    private void populateTabsFromMaps(){
    	nextPagerViewId = 0;
    	Bundle feedBundle;
    	    		
    	for(String feed : feeds){
            /*
    		if(feed.compareTo(OWFeedType.USER.toString().toLowerCase()) == 0){
    			//TODO: Merge MyFeedFragmentActivity and RemoteFeedFragment
    			mTabsAdapter.addTab(mTabHost.newTabSpec(getString(Constants.FEED_TO_TITLE.get(feed))).setIndicator(inflateCustomTab(getString(Constants.FEED_TO_TITLE.get(feed)))),
    	                MyFeedFragmentActivity.LocalRecordingsListFragment.class, null);
    			mTitleToTabId.put(feed, nextPagerViewId);
    		}else{
    		*/
            feedBundle = new Bundle(1);
            feedBundle.putString(Constants.OW_FEED, feed);
            mTabsAdapter.addTab(mTabHost.newTabSpec(getString(Constants.FEED_TO_TITLE.get(feed))).setIndicator(inflateCustomTab(getString(Constants.FEED_TO_TITLE.get(feed)))),
                    RemoteRecordingsListFragment.class, feedBundle);
            mTitleToTabId.put(feed, nextPagerViewId);
    		//}
    		nextPagerViewId ++;
    	}

    	for(String tag : tags){
    		feedBundle = new Bundle(1);
			feedBundle.putString(Constants.OW_FEED, tag);
	        mTabsAdapter.addTab(mTabHost.newTabSpec("#"+tag).setIndicator(inflateCustomTab("#"+tag)),
	                RemoteRecordingsListFragment.class, feedBundle);
	        mTitleToTabId.put(tag, nextPagerViewId);
	        nextPagerViewId ++;
    	}

    }

    private void addTagOrFeed(String name){
        name = name.toLowerCase();
        Bundle feedBundle = new Bundle(1);
        feedBundle.putString(Constants.OW_FEED, name);
        String tabTitle;
        if(Constants.OW_FEEDS.contains(name))
            tabTitle = getString(Constants.FEED_TO_TITLE.get(name));
        else
            tabTitle = "#" + name;
        mTabsAdapter.addTab(mTabHost.newTabSpec(tabTitle).setIndicator(inflateCustomTab(tabTitle)),
                RemoteRecordingsListFragment.class, feedBundle);
        mTitleToTabId.put(name, nextPagerViewId);
        nextPagerViewId ++;
    }
    
    private void setupFeedAndTagMaps(){
    	feeds.clear();
    	tags.clear();

        feeds.add(OWFeedType.USER.toString().toLowerCase());
        feeds.add(OWFeedType.TOP.toString().toLowerCase());
        feeds.add(OWFeedType.MISSION.toString().toLowerCase());
        feeds.add(OWFeedType.FEATURED_MEDIA.toString().toLowerCase());
        feeds.add(OWFeedType.LOCAL.toString().toLowerCase());
        feeds.add(OWFeedType.RAW.toString().toLowerCase());

		if(internal_user_id > 0){
            OWUser user = OWUser.objects(getApplicationContext(), OWUser.class).get(internal_user_id);
            QuerySet<OWTag> tag_set = user.tags.get(getApplicationContext(), user);
			//QuerySet<OWTag> tag_set = OWUser.objects(getApplicationContext(), OWUser.class).get(internal_user_id).tags.get(getApplicationContext(), OWUser.objects(getApplicationContext(), OWUser.class).get(internal_user_id));
			for(OWTag tag : tag_set){
				if(!tags.contains(tag.name.get())){
	    			tags.add(tag.name.get());
				}
			}
			Collections.sort(tags);
		}

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
    public static class TabsAdapter extends FragmentStatePagerAdapter
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



    private void selectItem(View v, int position) {
        // Highlight the selected item, update the title, and close the drawer
        mDrawerList.setItemChecked(position, true);
        String tag = ((TextView) v.findViewById(R.id.title)).getText().toString().replace("# ","");
        if(mTitleToTabId.containsKey(tag)){
            mTitleIndicator.setCurrentItem(mTitleToTabId.get(tag));
        }else if(v.getTag(R.id.list_item_model) != null && ((String)v.getTag(R.id.list_item_model)).compareTo("divider")==0){
            return;
        }else if(mTitleToTabId.containsKey(v.getTag(R.id.list_item_model))){
            mTitleIndicator.setCurrentItem(mTitleToTabId.get((String) v.getTag(R.id.list_item_model)));
        }else{
            if(tag.compareTo("Settings")==0){
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
            }else if(tag.compareTo("Send Feedback")==0){
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto",Constants.SUPPORT_EMAIL, null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT,getString(R.string.share_email_subject));
                emailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_email_text) + OWUtils.getPackageVersion(getApplicationContext()));
                startActivity(Intent.createChooser(emailIntent, getString(R.string.share_chooser_title)));
            }else if(tag.compareTo("Profile") == 0){
                Intent profileIntent = new Intent(this, OWProfileActivity.class);
                startActivity(profileIntent);
            }
        }

        mDrawerLayout.closeDrawer(mDrawerList);
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

    public void removeVideoView(){
        if(videoViewParent != null && videoViewHostCell != null && (videoView != null || textureView != null)){
            if(videoView != null && videoView.isPlaying()){
                videoView.stopPlayback();
                videoViewParent.removeView(videoView);
                videoView = null;
            }else if(textureView != null && OWUtils.mediaPlayer.isPlaying()){
                OWUtils.mediaPlayer.stop();
                videoViewParent.removeView(textureView);
                textureView = null;
            }
            Log.i(TAG, "removing videoView");


            videoViewHostCell.removeView(videoViewParent);
            videoViewHostCell.findViewById(R.id.thumbnail).setVisibility(View.VISIBLE);
            videoViewHostCell.findViewById(R.id.playButton).setVisibility(View.VISIBLE);
            videoViewHostCell.findViewById(R.id.playButton).bringToFront();
            videoViewParent = null;
            videoViewHostCell = null;
        }

    }

    @Override
    protected void onDestroy() {
        Analytics.cleanUp();
        super.onDestroy();
    }

}
