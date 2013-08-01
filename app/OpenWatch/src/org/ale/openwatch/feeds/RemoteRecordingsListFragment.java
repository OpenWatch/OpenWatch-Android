/*
 * Copyright (C) 2010 The Android Open Source Project
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

package org.ale.openwatch.feeds;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SearchViewCompat.OnQueryTextListenerCompat;
import android.util.Log;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import android.widget.AbsListView.OnScrollListener;
import org.ale.openwatch.*;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.constants.DBConstants;
import org.ale.openwatch.contentprovider.OWContentProvider;
import org.ale.openwatch.http.OWServiceRequests;
import org.ale.openwatch.http.OWServiceRequests.PaginatedRequestCallback;
import org.ale.openwatch.location.DeviceLocation;
import org.ale.openwatch.location.DeviceLocation.GPSRequestCallback;
import org.ale.openwatch.model.OWServerObject;
import org.ale.openwatch.model.OWServerObjectInterface;
import org.ale.openwatch.model.OWVideoRecording;
import org.ale.openwatch.share.Share;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;


public  class RemoteRecordingsListFragment extends ListFragment
            implements LoaderManager.LoaderCallbacks<Cursor>,
            PullToRefreshAttacher.OnRefreshListener, TabHost.OnTabChangeListener{
    	
    	static String TAG = "RemoteFeedFragment";
    	public boolean didRefreshFeed = false;
    	int page = 0;
    	boolean has_next_page = false;
        boolean fetching_next_page = false;

        int internal_user_id = -1;

        View loading_footer;
    	
    	String feed;
    	Location device_location;

        // This is the Adapter being used to display the list's data.
        //AppListAdapter mAdapter;
    	OWMediaObjectAdapter mAdapter;

        // If non-null, this is the current filter the user has provided.
        String mCurFilter;

        OnQueryTextListenerCompat mOnQueryTextListenerCompat;

        FeedFragmentActivity parentActivity;

        public PullToRefreshAttacher mPullToRefreshAttacher;
        
        PaginatedRequestCallback cb = new PaginatedRequestCallback(){

			@Override
			public void onSuccess(int page, int object_count, int total_pages) {
                fetching_next_page = false;
				if(RemoteRecordingsListFragment.this.isAdded()){
					RemoteRecordingsListFragment.this.page = page;
					if(total_pages <= page)
						RemoteRecordingsListFragment.this.has_next_page = false;
					else
						RemoteRecordingsListFragment.this.has_next_page = true;
					didRefreshFeed = true;
                    showLoadingMore(false);
					restartLoader();
                    if(mPullToRefreshAttacher != null)
                        mPullToRefreshAttacher.setRefreshComplete();
				}
			}

			@Override
			public void onFailure(int page) {
                if(mPullToRefreshAttacher != null)
                    mPullToRefreshAttacher.setRefreshComplete();
                fetching_next_page = false;
                showLoadingMore(false);
            }
        	
        };

    @Override
    public void onDestroy (){
        super.onDestroy();
        Log.i(feed.toString(), "onDestroy");
    }

    public void onResume (){
        super.onResume();
        Log.i(feed.toString(), "onResume");
        if(feed.compareTo("user") == 0 && ((FeedFragmentActivity) getActivity()).forceUserFeedRefresh ){
            fetchFeedPage(1);
            ((FeedFragmentActivity) getActivity()).forceUserFeedRefresh = false;
        }
    }

        @Override public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            parentActivity = (FeedFragmentActivity) getActivity();

            // Give some text to display if there is no data.  In a real
            // application this would come from a resource.
            setEmptyText(getString(R.string.feed_empty));

            // We have a menu item to show in action bar.
            setHasOptionsMenu(true);

            feed = this.getArguments().getString(Constants.OW_FEED);
            Log.i(feed.toString(), "onActivityCreated");
            // Initialize adapter without cursor. Let loader provide it when ready
            mAdapter = new OWMediaObjectAdapter(getActivity(), null);
            // Add footer loading view
            LayoutInflater layoutInflater = (LayoutInflater) parentActivity.getSystemService(parentActivity.LAYOUT_INFLATER_SERVICE);
            loading_footer = layoutInflater.inflate(R.layout.list_view_loading_footer, (ViewGroup) getActivity().findViewById(android.R.id.list), false);
            loading_footer.setVisibility(View.GONE);
            getListView().addFooterView(loading_footer);
            SharedPreferences profile = getActivity().getSharedPreferences(Constants.PROFILE_PREFS, 0);
            if(!profile.getBoolean(Constants.MISSION_TIP, false))
                addListViewHeader();
            setListAdapter(mAdapter);
            getListView().setDivider(null);
            getListView().setDividerHeight(0);

            this.getListView().setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(mPullToRefreshAttacher == null)
                        attachPullToRefresh();
                    return false;
                }
            });

            this.getListView().setOnScrollListener(new OnScrollListener(){

				@Override
				public void onScrollStateChanged(AbsListView view,
						int scrollState) {}

				@Override
				public void onScroll(AbsListView view, int firstVisibleItem,
						int visibleItemCount, int totalItemCount) {

                    if(parentActivity.videoViewListIndex != -1 && !(parentActivity.videoViewListIndex >= firstVisibleItem && parentActivity.videoViewListIndex <= firstVisibleItem + visibleItemCount)){
                        parentActivity.removeVideoView();
                    }

					if(!RemoteRecordingsListFragment.this.has_next_page)
						return;
					
					boolean loadMore = firstVisibleItem + visibleItemCount >= totalItemCount;

				        if(loadMore) {
				            RemoteRecordingsListFragment.this.fetchNextFeedPage();
				        }

				}
            	
            });

            // Start out with a progress indicator.
            setListShown(false);

            // Refresh the feed view
            if(!didRefreshFeed){
	            // If our feed demands device location and we haven't cached it
	            if(Constants.isOWFeedTypeGeoSensitive(feed) && device_location == null){
	            	GPSRequestCallback gps_callback = new GPSRequestCallback(){
	
						@Override
						public void onSuccess(Location result) {
							device_location = result;
							fetchNextFeedPage();
                        }
	            	};
	            	DeviceLocation.getLastKnownLocation(getActivity().getApplicationContext(), false, gps_callback);
	            }else{
	            	fetchNextFeedPage();
	            }
        	}

        }


        public void attachPullToRefresh() {
            // Now get the PullToRefresh attacher from the Activity. An exercise to the reader
            // is to create an implicit interface instead of casting to the concrete Activity
            if(parentActivity != null){
                Log.i("PTR", "Attempting to set PullToRefreshAttacher");
                mPullToRefreshAttacher = parentActivity.mPullToRefreshAttacher;

                // Now set the ScrollView as the refreshable view, and the refresh listener (this)
                mPullToRefreshAttacher.addRefreshableView(getListView(), this);
                //mPullToRefreshAttacher.setRefreshableView(getListView(), this);
            }else
                Log.i("PTR", "parentActivity is null on attachPullToRefresh");

        }

        public void detachPullToRefresh() {
            mPullToRefreshAttacher = null;
        }


        private void addListViewHeader(){
            if(feed.compareTo(Constants.OWFeedType.MISSION.toString().toLowerCase()) == 0){
                if(getActivity() == null)
                    return;
                LayoutInflater inflater = (LayoutInflater)
                        parentActivity.getSystemService(parentActivity.LAYOUT_INFLATER_SERVICE);
                View missionHeader = inflater.inflate(R.layout.mission_header,
                        (ViewGroup) getListView(), false);
                if(missionHeader != null){
                    missionHeader.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final View missionHeader = v;
                            SharedPreferences.Editor profile = getActivity().getSharedPreferences(Constants.PROFILE_PREFS, 0).edit();
                            profile.putBoolean(Constants.MISSION_TIP, true);
                            profile.commit();

                            Animation fadeOut = AnimationUtils.loadAnimation(v.getContext(), R.anim.fadeout);
                            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {

                                }

                                @Override
                                public void onAnimationEnd(Animation animation) {
                                    missionHeader.findViewById(R.id.missionBadge).setVisibility(View.GONE);
                                    missionHeader.findViewById(R.id.missionText).setVisibility(View.GONE);
                                    missionHeader.findViewById(R.id.tapToDismiss).setVisibility(View.GONE);
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {

                                }
                            });

                            v.startAnimation(fadeOut);
                        }
                    });
                }
                this.getListView().addHeaderView(missionHeader);
            }
        }
        
        private void fetchNextFeedPage(){
            fetchFeedPage(page+1);
        }

        private void fetchFeedPage(int page){
            if(!fetching_next_page){
                if(Constants.isOWFeedTypeGeoSensitive(feed) && device_location != null){
                    try{
                        OWServiceRequests.getGeoFeed(this.getActivity().getApplicationContext(), device_location, feed, page, cb);	 // NPE HERE
                        fetching_next_page = true;
                        showLoadingMore(true);
                    }catch(NullPointerException e){
                        Log.e(TAG, "NPE getting GeoFeed");
                        e.printStackTrace();
                    }
                }
                else{
                    try{
                        OWServiceRequests.getFeed(this.getActivity().getApplicationContext(), feed, page, cb);
                        fetching_next_page = true;
                        showLoadingMore(true);
                    }catch(NullPointerException e){
                        Log.e(TAG, "NPE getting GeoFeed");
                        e.printStackTrace();
                    }
                }
            }
        }
        
        private void restartLoader(){
        	this.getLoaderManager().restartLoader(0, null, this);
        }

        @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            // Place an action bar item for searching.
        	/*
            MenuItem item = menu.add("Search");
            item.setIcon(android.R.drawable.ic_menu_search);
            MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM
                    | MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
            View searchView = SearchViewCompat.newSearchView(getActivity());
            if (searchView != null) {
                SearchViewCompat.setOnQueryTextListener(searchView,
                        new OnQueryTextListenerCompat() {
                    @Override
                    public boolean onQueryTextChange(String newText) {
                        // Called when the action bar search text has changed.  Since this
                        // is a simple array adapter, we can just have it do the filtering.
                        mCurFilter = !TextUtils.isEmpty(newText) ? newText : null;
                        mAdapter.getFilter().filter(mCurFilter);
                        return true;
                    }
                });
                MenuItemCompat.setActionView(item, searchView);
            }
            */
        }

        OWUtils.VideoViewCallback videoViewCallback = new OWUtils.VideoViewCallback() {
            @Override
            public void onPlaybackComplete(ViewGroup parent) {
                Log.i(TAG, "playbackComplete");
                parentActivity.removeVideoView();
            }

            @Override
            public void onPrepared(ViewGroup parent) {
                Log.i(TAG, "onPrepared");
                //parent.findViewById(R.id.videoProgress).setVisibility(View.GONE);
                //progressBar.setVisibility(View.GONE);
                parent.removeView(parent.findViewById(R.id.videoProgress));
                if(Build.VERSION.SDK_INT >= 11)
                    parentActivity.videoView.setAlpha(1);
            }

            @Override
            public void onError(ViewGroup parent) {
                this.onPlaybackComplete(parent);
            }
        };

        @Override public void onListItemClick(ListView l, View v, int position, long id) {
            Log.i(TAG, "Item clicked: " + id);
        	try{
        		final int model_id = (Integer)v.getTag(R.id.list_item_model);
        		final OWServerObject server_object = OWServerObject.objects(getActivity().getApplicationContext(), OWServerObject.class).get(model_id);

                if(v.getTag(R.id.subView) != null && v.getTag(R.id.subView).toString().compareTo("menu") == 0){
                    Log.i(TAG, "menu click!");
                    final Context c = getActivity();
                    LayoutInflater inflater = (LayoutInflater)
                           parentActivity.getSystemService(parentActivity.LAYOUT_INFLATER_SERVICE);
                    View layout = inflater.inflate(R.layout.media_menu_popup,
                            (ViewGroup) getActivity().findViewById(R.id.content_frame), false);
                    final AlertDialog dialog =  new AlertDialog.Builder(getActivity()).setView(layout).create();
                    layout.findViewById(R.id.shareButton).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                            Share.showShareDialogWithInfo(c, getString(R.string.share_video), server_object.getTitle(c), OWUtils.urlForOWServerObject(server_object, c));
                            OWServiceRequests.increaseHitCount(c, server_object.getServerId(c), model_id, server_object.getContentType(c), Constants.HIT_TYPE.CLICK);
                        }
                    });
                    if(((OWServerObjectInterface) server_object.getChildObject(c)).getLat(c) != 0.0 ){
                        layout.findViewById(R.id.mapButton).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                dialog.dismiss();
                                Intent i = new Intent(getActivity(), MapActivity.class);
                                i.putExtra(Constants.INTERNAL_DB_ID, model_id);
                                startActivity(i);
                            }
                        });
                    }else
                        layout.findViewById(R.id.mapButton).setVisibility(View.GONE);
                    layout.findViewById(R.id.reportButton).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                            OWServiceRequests.flagOWServerObjet(getActivity().getApplicationContext(), server_object);
                        }
                    });
                    dialog.show();
                    return;
                }else
                    Log.i(TAG, "non menu click!");
        		
        		Intent i = null;
        		switch(server_object.getContentType(getActivity().getApplicationContext())){
        		case INVESTIGATION:
        			//TODO: InvestigationViewActivity
        			i = new Intent(this.getActivity(), OWInvestigationViewActivity.class);
        			break;
        		case VIDEO:
                    // play video inline
                    v.findViewById(R.id.playButton).setVisibility(View.GONE);

                    parentActivity.removeVideoView(); // remove prior VideoView if it exists

                    LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(parentActivity.LAYOUT_INFLATER_SERVICE);
                    //videoViewParent = (ViewGroup) v;
                    parentActivity.videoViewHostCell = (ViewGroup) v;
                    parentActivity.videoViewParent = (ViewGroup) layoutInflater.inflate(R.layout.feed_video_view, (ViewGroup) v, true);
                    parentActivity.videoView = (VideoView) parentActivity.videoViewParent.findViewById(R.id.videoView);

                    if(Build.VERSION.SDK_INT >= 11)
                        parentActivity.videoView.setAlpha(0);
                    parentActivity.progressBar = (ProgressBar) parentActivity.videoViewParent.findViewById(R.id.videoProgress);
                    parentActivity.progressBar.setVisibility(View.VISIBLE);

                    //Log.i(TAG, progressBar.toString());
                    //RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)videoView.getLayoutParams();
                    String url = ((OWVideoRecording) server_object.getChildObject(getActivity().getApplicationContext())).getMediaFilepath(getActivity().getApplicationContext());
                    OWUtils.setupVideoView(getActivity(), parentActivity.videoView, url, videoViewCallback, parentActivity.progressBar);
                    Log.i(TAG, "created VideoView");
                    parentActivity.videoViewListIndex = position;
        			break;
                case AUDIO:
                case PHOTO:
                    i = new Intent(this.getActivity(), OWMediaObjectViewActivity.class);
                    break;
                case MISSION:
                    i = new Intent(this.getActivity(), OWMissionViewActivity.class);
        		}

        		if(i != null){
                    i.putExtra(Constants.INTERNAL_DB_ID, (Integer)v.getTag(R.id.list_item_model));
        			startActivity(i);
                }
        	}catch(Exception e){
        		Log.e(TAG, "failed to load list item model tag");
                e.printStackTrace();
        		return;
        	}
        	
        }


        private void showLoadingMore(boolean show){
            if(show){
                loading_footer.setVisibility(View.VISIBLE);
            }else{
                loading_footer.setVisibility(View.GONE);
            }
        }

        
		@Override
		public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
			mAdapter.swapCursor(cursor);
			if(!isAdded())
				return;

			// The list should now be shown.
            if (isResumed()) {
                setListShown(true);
            } else {
                setListShownNoAnimation(true);
            }
            
           if(cursor != null && cursor.getCount() == 0){
        		setEmptyText(getString(R.string.feed_empty));
           }
			
		}

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			// TODO Auto-generated method stub
			Log.i("URI", "onLoaderReset on " + feed.toString());
			mAdapter.swapCursor(null);
		}
		
		static final String[] PROJECTION = new String[] {
			DBConstants.ID,
			DBConstants.RECORDINGS_TABLE_TITLE,
			DBConstants.VIEWS,
			DBConstants.ACTIONS,
			DBConstants.RECORDINGS_TABLE_THUMB_URL,
			DBConstants.RECORDINGS_TABLE_USERNAME,
            DBConstants.MEDIA_OBJECT_STORY,
            DBConstants.MEDIA_OBJECT_AUDIO,
            DBConstants.MEDIA_OBJECT_VIDEO,
            DBConstants.MEDIA_OBJECT_PHOTO,
            DBConstants.MEDIA_OBJECT_INVESTIGATION,
            DBConstants.MEDIA_OBJECT_MISSION,
            DBConstants.MEDIA_OBJECT_USER_THUMBNAIL,
            DBConstants.LAST_EDITED,
            DBConstants.MEDIA_OBJECT_METRO_CODE

	    };

		@Override
		public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
            Uri baseUri = OWContentProvider.getFeedUri(feed);
			String selection = null;
            String[] selectionArgs = null;
            String order = null;
			Log.i("URI"+feed.toString(), "createLoader on uri: " + baseUri.toString());
			return new CursorLoader(getActivity(), baseUri, PROJECTION, selection, selectionArgs, order);
		}


        @Override
        public void onRefreshStarted(View view) {
            Log.i(TAG, "refresh!");
            fetchFeedPage(1);
        }

        @Override
        public void onTabChanged(String tabId) {

        }
}
