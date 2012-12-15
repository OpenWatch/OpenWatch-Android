package net.openwatch.reporter;

import com.orm.androrm.QuerySet;

import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.contentprovider.OWContentProvider;
import net.openwatch.reporter.model.OWLocalRecording;
import net.openwatch.reporter.model.OWRecordingTag;
import net.openwatch.reporter.view.TagPoolLayout;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.CursorToStringConverter;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.VideoView;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

public class LocalRecordingInfoFragment extends Fragment implements LoaderCallbacks<Cursor>{
	
	private static final String TAG = "LocalRecordingInfoFragment";
	
	EditText title;
	EditText description;
	AutoCompleteTextView tags;
	
	private static boolean boundData = false;
	String mSelection = ""; // autocomplete tag input

	
	static final String[] PROJECTION = new String[] {
		DBConstants.ID,
		DBConstants.RECORDINGS_TABLE_TITLE,
		DBConstants.RECORDINGS_TABLE_DESC,
		DBConstants.RECORDINGS_TABLE_VIDEO_URL,
		DBConstants.RECORDINGS_TABLE_CREATION_TIME,
		DBConstants.RECORDINGS_TABLE_THUMB_URL

    };
	
	private SimpleCursorAdapter mAdapter;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		
		final LocalRecordingViewActivity parent = (LocalRecordingViewActivity) this.getActivity();
		View v = inflater.inflate(R.layout.local_recording_info_view, container, false);
		
		title = ((EditText)v.findViewById(R.id.editTitle));
		description = ((EditText)v.findViewById(R.id.editDescription));
		tags = ((AutoCompleteTextView)v.findViewById(R.id.editTags));
		tags.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int arg2,
					long arg3) {
				String selection = ((TextView)view).getText().toString();
				TagPoolLayout tagGroup = ((TagPoolLayout) getActivity().findViewById(R.id.tagGroup));
				tagGroup.addTag(selection);
				Log.i("TagPool", String.valueOf(tagGroup.getChildCount()));
				//((ViewGroup) getActivity().findViewById(R.id.tagGroup)).getChild
				tags.setText("");
			}
			
		});
		
		/*
		 * public SimpleCursorAdapter (Context context, int layout, Cursor c, String[] from, int[] to, int flags)
		 */
		tags.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				mSelection = s.toString();
				getLoaderManager().restartLoader(0, null, LocalRecordingInfoFragment.this);
				
			}
			
		});
		mAdapter = new SimpleCursorAdapter(getActivity().getApplicationContext(), R.layout.autocomplete_tag_item, null, new String[]{DBConstants.TAG_TABLE_NAME}, new int[]{R.id.name}, 0);
		mAdapter.setCursorToStringConverter(new CursorToStringConverter(){

			@Override
			public CharSequence convertToString(Cursor cursor) {
				return cursor.getString(cursor.getColumnIndexOrThrow(DBConstants.TAG_TABLE_NAME)); 
			}
			
		});
		tags.setAdapter(mAdapter);
		
		getLoaderManager().initLoader(0, null, this);
                
        tags.setOnFocusChangeListener(new OnFocusChangeListener(){

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(hasFocus){
					parent.setVideoViewVisible(false);
				} else{
					parent.setVideoViewVisible(true);
				}
				
			}
        	
        });
		
        populateViews(this.getActivity().getApplicationContext());
       
        return v;
    }
	
	private void populateViews(Context app_context){
		if(boundData)
			return;
		try{
			OWLocalRecording recording = LocalRecordingViewActivity.recording;
			if(recording.title.get() != null)
				title.setText(recording.title.get());
			if(recording.description.get() != null)
				description.setText(recording.description.get());
			if(recording.hq_filepath.get() != null)
				setupVideoView(R.id.videoview, recording.hq_filepath.get());
			if(recording.tags.get(app_context, recording) != null)
				populateTagPool(recording, app_context);
		} catch(Exception e){
			e.printStackTrace();
			Log.e(TAG, "Error retrieving recording");
		}
		
		boundData = true;
	}
	
	public void populateTagPool(OWLocalRecording recording, Context app_context){
		TagPoolLayout tagGroup = ((TagPoolLayout) getActivity().findViewById(R.id.tagGroup));
		QuerySet<OWRecordingTag> tags = recording.tags.get(app_context, recording);
		for(OWRecordingTag tag : tags){
			tagGroup.addTag(tag.name.get());
		}
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
    
    static final String[] TAG_PROJECTION = new String[] {
		DBConstants.ID,
		DBConstants.TAG_TABLE_NAME
    };

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		Uri baseUri = OWContentProvider.getTagSearchUri(mSelection);
		String selection = null;
        String[] selectionArgs = null;
        String order = null;
        
		return new CursorLoader(getActivity(), baseUri, TAG_PROJECTION, selection, selectionArgs, order);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
		Log.i(TAG, "onLoadFinished");
		mAdapter.swapCursor(cursor);
		// TODO: Check if no tags found and say something nice
		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.swapCursor(null);
		
	}

}