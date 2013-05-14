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

package net.openwatch.reporter.feeds;

import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SearchViewCompat.OnQueryTextListenerCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import net.openwatch.reporter.OWMediaObjectViewActivity;
import net.openwatch.reporter.R;
import net.openwatch.reporter.StoryViewActivity;
import net.openwatch.reporter.constants.Constants.CONTENT_TYPE;
import net.openwatch.reporter.constants.Constants.OWFeedType;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.contentprovider.OWContentProvider;
import net.openwatch.reporter.http.OWServiceRequests;
import net.openwatch.reporter.http.OWServiceRequests.PaginatedRequestCallback;
import net.openwatch.reporter.http.OWServiceRequests.RequestCallback;
import net.openwatch.reporter.location.DeviceLocation;
import net.openwatch.reporter.location.DeviceLocation.GPSRequestCallback;

/**
 * Demonstration of the implementation of a custom Loader.
 */
public class RemoteFeedFragmentActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();

        // Create the list fragment and add it as our sole content.
        if (fm.findFragmentById(android.R.id.content) == null) {
            RemoteRecordingsListFragment list = new RemoteRecordingsListFragment();
            fm.beginTransaction().add(android.R.id.content, list).commit();
        }
        
    }


    public static class RemoteRecordingsListFragment extends ListFragment
            implements LoaderManager.LoaderCallbacks<Cursor> {
    	
    	static String TAG = "RemoteFeedFragment";
    	boolean didRefreshFeed = false;
    	int page = 0;
    	boolean has_next_page = false;
    	
    	String feed;
    	Location device_location;
    	Uri this_uri; // TESTING

        // This is the Adapter being used to display the list's data.
        //AppListAdapter mAdapter;
    	OWMediaObjectAdapter mAdapter;

        // If non-null, this is the current filter the user has provided.
        String mCurFilter;

        OnQueryTextListenerCompat mOnQueryTextListenerCompat;
        
        PaginatedRequestCallback cb = new PaginatedRequestCallback(){

			@Override
			public void onSuccess(int page, int object_count, int total_pages) {
				if(RemoteRecordingsListFragment.this.isAdded()){
					RemoteRecordingsListFragment.this.page = page;
					if(total_pages <= page)
						RemoteRecordingsListFragment.this.has_next_page = false;
					else
						RemoteRecordingsListFragment.this.has_next_page = true;
					didRefreshFeed = true;
					restartLoader();
				}
			}

			@Override
			public void onFailure(int page) {}
        	
        };

        @Override public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            // Give some text to display if there is no data.  In a real
            // application this would come from a resource.
            setEmptyText(getString(R.string.loading_feed));

            // We have a menu item to show in action bar.
            setHasOptionsMenu(true);

            // Initialize adapter without cursor. Let loader provide it when ready
            mAdapter = new OWMediaObjectAdapter(getActivity(), null); 
            setListAdapter(mAdapter);
            
            this.getListView().setOnScrollListener(new OnScrollListener(){

				@Override
				public void onScrollStateChanged(AbsListView view,
						int scrollState) {}

				@Override
				public void onScroll(AbsListView view, int firstVisibleItem,
						int visibleItemCount, int totalItemCount) {
					
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
            
            feed = this.getArguments().getString(Constants.OW_FEED);
            Log.i(TAG, "got feed name: " +  feed.toString() );
            
            // Prepare the loader.  Either re-connect with an existing one,
            // or start a new one.
           
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
	            	DeviceLocation.getLocation(getActivity().getApplicationContext(), false, gps_callback);
	            }else{
	            	fetchNextFeedPage();
	            }
        	}

        }
        
        private void fetchNextFeedPage(){
        	if(Constants.isOWFeedTypeGeoSensitive(feed) && device_location != null)
        		OWServiceRequests.getGeoFeed(this.getActivity().getApplicationContext(), device_location, feed, page+1, cb);	
        	else
        		OWServiceRequests.getFeed(this.getActivity().getApplicationContext(), feed, page+1, cb);	
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

        @Override public void onListItemClick(ListView l, View v, int position, long id) {
            Log.i("LoaderCustom", "Item clicked: " + id);
        	try{
        		Intent i = null;
        		switch((CONTENT_TYPE)v.getTag(R.id.list_item_model_type)){
        		case VIDEO:
        			i = new Intent(this.getActivity(), OWMediaObjectViewActivity.class);
        			break;
        		case STORY:
        			i = new Intent(this.getActivity(), StoryViewActivity.class);
        			break;
        		}
        		i.putExtra(Constants.INTERNAL_DB_ID, (Integer)v.getTag(R.id.list_item_model));
        		if(i != null)
        			startActivity(i);
        	}catch(Exception e){
        		Log.e(TAG, "failed to load list item model tag");
        		return;
        	}
        	
        }

        
		@Override
		public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
			mAdapter.swapCursor(cursor);
			if(!isAdded())
				return;
			/*
			if(cursor == null || cursor.getCount() == 0)
				Log.i("URI" + feed.toString(), "onLoadFinished empty cursor on uri " + this_uri.toString());
			else
				Log.i("URI" + feed.toString(), String.format("onLoadFinished %d rows on uri %s ",cursor.getCount(), this_uri.toString()));
			*/
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
			DBConstants.RECORDINGS_TABLE_VIEWS,
			DBConstants.RECORDINGS_TABLE_ACTIONS,
			DBConstants.RECORDINGS_TABLE_THUMB_URL,
			DBConstants.RECORDINGS_TABLE_USERNAME,
			DBConstants.MEDIA_OBJECT_STORY,
			DBConstants.MEDIA_OBJECT_VIDEO

	    };

		@Override
		public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
			Uri baseUri = OWContentProvider.getFeedUri(feed);
			this_uri = baseUri;
			String selection = null;
            String[] selectionArgs = null;
            String order = null;
			Log.i("URI"+feed.toString(), "createLoader on uri: " + baseUri.toString());
			return new CursorLoader(getActivity(), baseUri, PROJECTION, selection, selectionArgs, order);
		}
    }

}
