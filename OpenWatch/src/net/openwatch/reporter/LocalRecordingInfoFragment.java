package net.openwatch.reporter;

import net.openwatch.reporter.model.OWLocalRecording;
import net.openwatch.reporter.model.OWRecordingTag;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

public class LocalRecordingInfoFragment extends RecordingInfoFragment implements LoaderCallbacks<Cursor>{
	
	private static final String TAG = "LocalRecordingInfoFragment";
			
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, container, savedInstanceState);
		
		final LocalRecordingViewActivity parent = (LocalRecordingViewActivity) this.getActivity();
		
		recording = OWLocalRecording.objects(getActivity().getApplicationContext(), OWLocalRecording.class)
				.get(LocalRecordingViewActivity.model_id);

		tags.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int arg2,
					long arg3) {
				Log.i(TAG, "Autocomplete select");
				String tag_name = ((TextView)view).getText().toString();
				//TagPoolLayout tagGroup = ((TagPoolLayout) getActivity().findViewById(R.id.tagGroup));
				OWLocalRecording recording = OWLocalRecording.objects(getActivity().getApplicationContext(), OWLocalRecording.class)
						.get(LocalRecordingViewActivity.model_id);
				if(!recording.hasTag(getActivity().getApplicationContext(), tag_name)){
					OWRecordingTag tag = OWRecordingTag.objects(getActivity().getApplicationContext(), OWRecordingTag.class).get((Integer)view.getTag(R.id.list_item_model));
					tagGroup.addTagPostLayout(tag);
					//addTagToTagPool(tag);
					recording.tags.add(tag);
					recording.save(getActivity().getApplicationContext());
				}
				watch_tag_text = false;
				tags.setText("");
				watch_tag_text = true;
				tags.setAdapter(null);
			}
			
		});
                
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
		
        Log.i(TAG, "onCreateView");
        return v;
    }
	
	@Override
	public void onResume(){
		super.onResume();
		Log.i(TAG, "onResume");
		//populateViews(recording, getActivity().getApplicationContext());
	}
	
	@Override
	public void onViewCreated (View view_arg, Bundle savedInstanceState){
		final View view = view_arg.findViewById(R.id.tagGroup);
		
		ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
		if (viewTreeObserver.isAlive()) {

			  viewTreeObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			    @Override
			    public void onGlobalLayout() {
			      view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			      //view.getWidth();
			      Log.i("onGlobalLayout", "width: " + String.valueOf(view.getWidth()) + " : " + String.valueOf(view.getHeight()));
			      populateViews(recording, getActivity().getApplicationContext());
			    }
			  });
			}
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