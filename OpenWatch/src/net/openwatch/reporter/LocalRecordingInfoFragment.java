package net.openwatch.reporter;

import com.orm.androrm.DatabaseAdapter;

import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.model.OWLocalRecording;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.VideoView;

public class LocalRecordingInfoFragment extends Fragment {
	
	private static final String TAG = "LocalRecordingInfoFragment";
	
	EditText title;
	EditText description;

	
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
		
		title = ((EditText)v.findViewById(R.id.editTitle));
		description = ((EditText)v.findViewById(R.id.editDescription));
		try{
			OWLocalRecording recording = LocalRecordingViewActivity.recording;
			if(recording.title.get() != null)
				title.setText(recording.title.get());
			if(recording.description.get() != null)
				description.setText(recording.description.get());
			if(recording.hq_filepath.get() != null)
				setupVideoView(R.id.videoview, recording.hq_filepath.get());
		} catch(Exception e){
			e.printStackTrace();
			Log.e(TAG, "Error retrieving recording");
		}
       
        return v;
    }
	
	public void setupVideoView(int view_id, String filepath){
		VideoView myVideoView = (VideoView)getActivity().findViewById(view_id);
       myVideoView.setVideoURI(Uri.parse(filepath));
       myVideoView.setMediaController(new MediaController(getActivity()));
       myVideoView.requestFocus();
       myVideoView.start();
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
    
    @Override
    public void onPause(){
    	super.onPause();
    	Log.i(TAG, "Saving recording. " + title.getText().toString() + " : " + description.getText().toString());
    	//this.getActivity().getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder)
    	OWLocalRecording recording = OWLocalRecording.objects(getActivity().getApplicationContext(), OWLocalRecording.class).get(LocalRecordingViewActivity.model_id);
    	recording.title.set(title.getText().toString());
    	recording.description.set(description.getText().toString());
    	recording.save(getActivity().getApplicationContext());
    }

}