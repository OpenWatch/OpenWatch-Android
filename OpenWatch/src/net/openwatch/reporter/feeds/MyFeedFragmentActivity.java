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

import com.orm.androrm.Filter;
import com.orm.androrm.QuerySet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
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
import android.widget.ListView;
import android.widget.AbsListView.OnScrollListener;
import net.openwatch.reporter.OWApplication;
import net.openwatch.reporter.RecordingViewActivity;
import net.openwatch.reporter.R;
import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.constants.Constants.OWFeedType;
import net.openwatch.reporter.contentprovider.OWContentProvider;
import net.openwatch.reporter.feeds.RemoteFeedFragmentActivity.RemoteRecordingsListFragment;
import net.openwatch.reporter.http.OWServiceRequests;
import net.openwatch.reporter.http.OWServiceRequests.PaginatedRequestCallback;
import net.openwatch.reporter.http.OWServiceRequests.RequestCallback;
import net.openwatch.reporter.model.OWUser;

/**
 * Demonstration of the implementation of a custom Loader.
 */
public class MyFeedFragmentActivity extends FragmentActivity {
	
	private static final String TAG = "MyFeedFragmentActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();

        // Create the list fragment and add it as our sole content.
        if (fm.findFragmentById(android.R.id.content) == null) {
            LocalRecordingsListFragment list = new LocalRecordingsListFragment();
            fm.beginTransaction().add(android.R.id.content, list).commit();
        }
    }


    public static class LocalRecordingsListFragment extends ListFragment
            implements LoaderManager.LoaderCallbacks<Cursor> {

        // This is the Adapter being used to display the list's data.
    	OWLocalRecordingAdapter mAdapter;

        // If non-null, this is the current filter the user has provided.
        String mCurFilter;

        OnQueryTextListenerCompat mOnQueryTextListenerCompat;
        
        final String feed = Constants.OWFeedType.USER.toString();
        int internal_user_id = -1;
        int page = 0;
        boolean didRefreshFeed = false;
        boolean has_next_page = true;
        
        PaginatedRequestCallback cb = new PaginatedRequestCallback(){
        	
			@Override
			public void onSuccess(int page, int object_count,
					int total_pages) {
				if(LocalRecordingsListFragment.this.isAdded()){
					LocalRecordingsListFragment.this.page = page;
					if(total_pages <= page)
						LocalRecordingsListFragment.this.has_next_page = false;
					else
						LocalRecordingsListFragment.this.has_next_page = true;
					didRefreshFeed = true;
					restartLoader();
				}
			}

			@Override
			public void onFailure(int page) {}
        	
        };

        @Override public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            // We have a menu item to show in action bar.
            setHasOptionsMenu(true);

            // Initialize adapter without cursor. Let loader provide it when ready
            mAdapter = new OWLocalRecordingAdapter(getActivity(), null); 
            setListAdapter(mAdapter);

            this.getListView().setOnScrollListener(new OnScrollListener(){

				@Override
				public void onScrollStateChanged(AbsListView view,
						int scrollState) {}

				@Override
				public void onScroll(AbsListView view, int firstVisibleItem,
						int visibleItemCount, int totalItemCount) {
					
					if(!LocalRecordingsListFragment.this.has_next_page)
						return;
					
					boolean loadMore = firstVisibleItem + visibleItemCount >= totalItemCount;

				        if(loadMore) {
				            LocalRecordingsListFragment.this.fetchNextFeedPage();
				        }

				}
            	
            });
            
            // Refresh the feed view
            if(!didRefreshFeed){
            	fetchNextFeedPage();
            }

            // perhaps do auth checking onStart
            
            //boolean authenticated = false;
            //if( OWApplication.user_data != null && OWApplication.user_data.containsKey(Constants.AUTHENTICATED))
            //		authenticated = true;
            SharedPreferences profile = getActivity().getSharedPreferences(Constants.PROFILE_PREFS, 0);
    		boolean authenticated = profile.getBoolean(Constants.AUTHENTICATED, false);
    		if(authenticated){
    			int user_server_id = profile.getInt(DBConstants.USER_SERVER_ID, 0);
    			//int user_server_id = (Integer) OWApplication.user_data.get(DBConstants.USER_SERVER_ID);
    			Filter filter = new Filter();
    			filter.is(DBConstants.USER_SERVER_ID, user_server_id);
    			QuerySet<OWUser> users = OWUser.objects(getActivity().getApplicationContext(), OWUser.class).filter(filter);
    			for(OWUser user : users){
    				internal_user_id = user.getId();
    				break;
    			}
    			if(internal_user_id > 0){
	                setEmptyText(getString(R.string.loading_recordings));
	    			setListShown(false); // start with a progress indicator
	    			getLoaderManager().initLoader(0, null, this);
    			}
    		}else{
    			setEmptyText(getString(R.string.login_for_local_recordings));
    		}
            
        }
        
        private void fetchNextFeedPage(){
        	OWServiceRequests.getFeed(this.getActivity().getApplicationContext(), feed, page+1, cb);
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
        	Intent i = new Intent(this.getActivity(), RecordingViewActivity.class);
        	try{
        		i.putExtra(Constants.INTERNAL_DB_ID, (Integer)v.getTag(R.id.list_item_model));
        	}catch(Exception e){
        		Log.e(TAG, "failed to load list item model tag");
        		return;
        	}
        	startActivity(i);
        	
        }
        
        private void restartLoader(){
        	this.getLoaderManager().restartLoader(0, null, this);
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
        		setEmptyText(getString(R.string.no_recordings));
           }
			
		}

		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			// TODO Auto-generated method stub
			mAdapter.swapCursor(null);
		}
		
		static final String[] PROJECTION = new String[] {
			DBConstants.ID,
			DBConstants.RECORDINGS_TABLE_TITLE,
			DBConstants.RECORDINGS_TABLE_FIRST_POSTED,
			DBConstants.RECORDINGS_TABLE_THUMB_URL,
			DBConstants.RECORDINGS_TABLE_VIEWS,
			DBConstants.RECORDINGS_TABLE_ACTIONS,
			DBConstants.MEDIA_OBJECT_LOCAL_VIDEO
	    };

		@Override
		public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
			//Uri baseUri = OWContentProvider.getFeedUri(feed);
			Uri baseUri = OWContentProvider.getUserRecordingsUri(internal_user_id);
			String selection = null;
            String[] selectionArgs = null;
            String order = " ORDER BY " + DBConstants.RECORDINGS_TABLE_FIRST_POSTED + " DESC";
            //String order = DBConstants.RECORDINGS_TABLE_FIRST_POSTED + " DESC";
            Log.i("URI"+feed.toString(), "createLoader on uri: " + baseUri.toString());
			return new CursorLoader(getActivity(), baseUri, PROJECTION, selection, selectionArgs, order);
		}
    }

}
