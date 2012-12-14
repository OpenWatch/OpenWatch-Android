package net.openwatch.reporter;

import net.openwatch.reporter.constants.DBConstants;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class LocalRecordingInfoFragment extends Fragment {
	
	static final String[] PROJECTION = new String[] {
		DBConstants.ID,
		DBConstants.RECORDINGS_TABLE_TITLE,
		DBConstants.RECORDINGS_TABLE_DESC,
		DBConstants.RECORDINGS_TABLE_VIDEO_URL,
		DBConstants.RECORDINGS_TABLE_CREATION_TIME,
		DBConstants.RECORDINGS_TABLE_THUMB_URL

    };
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.local_recording_info_view, container, false);
       ((TextView)v.findViewById(R.id.editTitle)).setText("Injected title");
       ((TextView)v.findViewById(R.id.editDescription)).setText("Injected description");
        return v;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

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

}