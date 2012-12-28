package net.openwatch.reporter;

import com.orm.androrm.Filter;
import com.orm.androrm.QuerySet;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.model.OWLocalRecording;
import net.openwatch.reporter.model.OWRecording;
import net.openwatch.reporter.model.OWRecordingTag;
import net.openwatch.reporter.view.TagPoolLayout;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class LocalRecordingInfoFragment extends Fragment{
	
	private static final String TAG = "LocalRecordingInfoFragment";
	
	protected EditText title;
	protected EditText description;
	protected AutoCompleteTextView tags;
	protected OWRecording recording = null;
	protected TagPoolLayout tagGroup;
	
	protected static boolean watch_tag_text = true;
	protected String tag_query = ""; // autocomplete tag input
	protected OWTagArrayAdapter mAdapter;
	
	boolean doSave = false; // if recording is mutated during this fragments life, saveAndSync onPause
		
	/**
	 * This fragment assumes a database_id will be passed in the 
	 * starting Intent with key: Constants.INTERNAL_DB_ID
	 * 
	 * This fragment automatically populates it's fields with 
	 * the recording with id specified on launch and 
	 * saves any changes when the fragment is paused
	 */
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.local_recording_info_view, container, false);
		
		tagGroup = ((TagPoolLayout) v.findViewById(R.id.tagGroup));
		title = ((EditText)v.findViewById(R.id.editTitle));
		description = ((EditText)v.findViewById(R.id.editDescription));
		tags = ((AutoCompleteTextView)v.findViewById(R.id.editTags));
		
		recording = OWRecording.objects(getActivity().getApplicationContext(), OWRecording.class)
				.get(this.getActivity().getIntent().getExtras().getInt(Constants.INTERNAL_DB_ID));
		tagGroup.setRecording((OWRecording)recording);
		
		setTagAutoCompleteListeners();
	
		mAdapter = new OWTagArrayAdapter(getActivity().getApplicationContext(), R.layout.autocomplete_tag_item, OWRecordingTag.objects(getActivity(), OWRecordingTag.class).all().toList());
		
		/*
		mAdapter = new SimpleCursorAdapter(getActivity().getApplicationContext(), R.layout.autocomplete_tag_item, null, new String[]{DBConstants.TAG_TABLE_NAME}, new int[]{R.id.name}, 0);
		
		mAdapter.setViewBinder(new ViewBinder(){

			@Override
			public boolean setViewValue(View arg0, Cursor arg1, int arg2) {
				((TextView)arg0.findViewById(R.id.name)).setText(arg1.getString(arg2));
				((TextView)arg0.findViewById(R.id.name)).setTag(R.id.list_item_model, arg1.getInt(arg1.getColumnIndex(DBConstants.ID)));
				return false;
			}
			
		});
		
		mAdapter.setCursorToStringConverter(new CursorToStringConverter(){

			@Override
			public CharSequence convertToString(Cursor cursor) {
				return cursor.getString(cursor.getColumnIndexOrThrow(DBConstants.TAG_TABLE_NAME)); 
			}
			
		});
		*/
		tags.setAdapter(mAdapter);
		tags.setThreshold(1);
		
		
		//getLoaderManager().initLoader(0, null, this);
		
        Log.i(TAG, "onCreateView. tag adapter size:" + String.valueOf(mAdapter.getCount()));
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
	
	protected void populateViews(OWRecording recording, Context app_context){
		try{
			if(recording.title.get() != null)
				title.setText(recording.title.get());
			if(recording.description.get() != null)
				description.setText(recording.description.get());
			if(recording.tags.get(app_context, recording) != null)
				populateTagPool(recording, app_context);
		} catch(Exception e){
			e.printStackTrace();
			Log.e(TAG, "Error retrieving recording");
		}

	}
	
	public void addTagToTagPool(OWRecordingTag tag){
		tagGroup.addTag(tag);
	}
	
	public void populateTagPool(OWRecording recording, Context app_context){
		Log.i(TAG, "populateTagPool");
		//TagPoolLayout tagGroup = ((TagPoolLayout) getActivity().findViewById(R.id.tagGroup));
		//TagPoolLayout tagGroup = ((TagPoolLayout) getView().findViewById(R.id.tagGroup));
		QuerySet<OWRecordingTag> tags = recording.tags.get(app_context, recording);
		for(OWRecordingTag tag : tags){
			//addTagToTagPool(tag);
			tagGroup.addTag(tag);
			//tagGroup.addTag(tag.name.get());
		}
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
    	if(recording == null)
    		return;
   
    	// Title may not be set to empty string
    	if(recording.title.get() != null && recording.title.get().compareTo(title.getText().toString()) != 0 && recording.title.get().compareTo("") != 0){
    		doSave = true;
    		recording.title.set(title.getText().toString());
    	}
    	// if recording has a description, make sure it has changed before saving
    	if(recording.description.get() != null && recording.description.get().compareTo(description.getText().toString()) != 0){
    		doSave = true;
    		recording.description.set(description.getText().toString());
    	}
    	// If recording does not have a description, make sure the EditText description is not blank
    	else if(recording.description.get() == null && "".compareTo(description.getText().toString()) != 0){
    		doSave = true;
    		recording.description.set(description.getText().toString());
    	}
    	if(doSave){
    		Log.i(TAG, "Saving recording. " + title.getText().toString() + " : " + description.getText().toString());
    		recording.saveAndSync(getActivity().getApplicationContext());
    	}
    }
    
    static final String[] TAG_PROJECTION = new String[] {
		DBConstants.ID,
		DBConstants.TAG_TABLE_NAME
    };
	
	/*
	 * This must be set after recording has been initialized and set
	 */
	protected void setTagAutoCompleteListeners(){
		if(recording == null)
			return;
		// On autocomplete tag selection
		tags.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int arg2,
					long arg3) {
				Log.i(TAG, "Autocomplete select");
				String tag_name = ((TextView)view).getText().toString();
				if(!recording.hasTag(getActivity().getApplicationContext(), tag_name)){
					OWRecordingTag tag = OWRecordingTag.objects(getActivity().getApplicationContext(), OWRecordingTag.class).get((Integer)view.getTag(R.id.list_item_model));
					addTagToRecording(tag, recording);
				}
				//watch_tag_text = false;
				tags.setText("");
				//watch_tag_text = true;
				//tags.setAdapter(null);
			}
			
		});
		
		// On keyboard enter
		
		tags.setOnEditorActionListener(new OnEditorActionListener(){

			@Override
			public boolean onEditorAction(TextView view, int actionId,
					KeyEvent event) {
				
				String[] tag_name_array = view.getText().toString().split(",");
				
				for(int x=0;x < tag_name_array.length; x++){
					String tag_name = tag_name_array[x].trim();
					
					if(!recording.hasTag(getActivity().getApplicationContext(), tag_name)){
						Filter filter = new Filter();
						filter.is(DBConstants.TAG_TABLE_NAME, tag_name);
						QuerySet<OWRecordingTag> tags = OWRecordingTag.objects(getActivity().getApplicationContext(), OWRecordingTag.class).filter(filter);
						OWRecordingTag selected_tag = null;
						for(OWRecordingTag tag : tags){
							selected_tag = tag;
							break;
						}
						if(selected_tag == null){
							selected_tag = new OWRecordingTag();
							selected_tag.name.set(tag_name);
							selected_tag.is_featured.set(false);
							selected_tag.save(getActivity().getApplicationContext());
						}
						
						addTagToRecording(selected_tag, recording);
						//watch_tag_text = false;
						view.setText("");
						//watch_tag_text = true;
						//((AutoCompleteTextView)view).setAdapter(null);
					}
				} // end for
				
				return true;
			}
			
		});
		
		/*
		tags.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable s) {				
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				Log.i(TAG, "onTextChanged. s: " + s.toString() + " start: " + String.valueOf(start) + " before: " + String.valueOf(before));
				if(watch_tag_text){
					//Log.i(TAG, "onTextChanged");
					tag_query = s.toString();
				}
				
			}
			
		});
		*/
	}
	
	private void addTagToRecording(OWRecordingTag tag, OWRecording recording){
		tagGroup.addTagPostLayout(tag);
		recording.tags.add(tag);
		recording.save(getActivity().getApplicationContext());
		doSave = true;
	}

}