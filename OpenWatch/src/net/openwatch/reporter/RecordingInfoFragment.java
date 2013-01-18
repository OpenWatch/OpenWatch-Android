package net.openwatch.reporter;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.orm.androrm.Filter;
import com.orm.androrm.QuerySet;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.constants.Constants.OWFeedType;
import net.openwatch.reporter.model.OWMediaObject;
import net.openwatch.reporter.model.OWVideoRecording;
import net.openwatch.reporter.model.OWTag;
import net.openwatch.reporter.view.TagPoolLayout;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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

public class RecordingInfoFragment extends SherlockFragment implements OWMediaObjectBackedEntity{
	
	private static final String TAG = "RecordingInfoFragment";
	
	protected EditText title;
	protected EditText description;
	protected AutoCompleteTextView tags;
	protected OWMediaObject media_obj = null;
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
		
		media_obj = OWMediaObject.objects(getActivity().getApplicationContext(), OWMediaObject.class)
				.get(this.getActivity().getIntent().getExtras().getInt(Constants.INTERNAL_DB_ID));
		
		if((OWVideoRecording)media_obj.video_recording.get(getActivity().getApplicationContext()) != null)
			tagGroup.setRecording((OWVideoRecording)media_obj.video_recording.get(getActivity().getApplicationContext()));
		
		setTagAutoCompleteListeners();
	
		mAdapter = new OWTagArrayAdapter(getActivity().getApplicationContext(), R.layout.autocomplete_tag_item, OWTag.objects(getActivity(), OWTag.class).all().toList());
		
		tags.setAdapter(mAdapter);
		tags.setThreshold(1);

		//getLoaderManager().initLoader(0, null, this);
		if(getArguments() != null)
			setInfoFieldsEnabled(this.getArguments().getBoolean(Constants.IS_LOCAL_RECORDING, true));
		
        Log.i(TAG, "onCreateView. tag adapter size:" + String.valueOf(mAdapter.getCount()));
        return v;
    }
	
	@Override
	public void onResume(){
		super.onResume();
		Log.i(TAG, "onResume");
		//populateViews(recording, getActivity().getApplicationContext());
	}
	
	/**
	 * This method prepares the fragment for read-only display
	 * @param doEnable
	 */
	private void setInfoFieldsEnabled(boolean doEnable){
		title.setEnabled(doEnable);
		description.setEnabled(doEnable);
		tags.setEnabled(doEnable);
		tagGroup.setTagRemovalAllowed(false);
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
			      populateViews(media_obj, getActivity().getApplicationContext());
			    }
			  });
			}
	}
	
	public void populateViews(OWMediaObject media_obj, Context app_context){
		try{
			if(media_obj.getTitle(app_context) != null)
				title.setText(media_obj.getTitle(app_context));
			if(media_obj.getDescription(app_context) != null)
				description.setText(media_obj.getDescription(app_context));
			if(media_obj.getTags(app_context) != null)
				populateTagPool(media_obj, app_context);
		} catch(Exception e){
			e.printStackTrace();
			Log.e(TAG, "Error retrieving recording");
		}

	}
	
	public void addTagToTagPool(OWTag tag){
		tagGroup.addTag(tag);
	}
	
	public void populateTagPool(OWMediaObject media_obj, Context app_context){
		Log.i(TAG, "populateTagPool");
		//TagPoolLayout tagGroup = ((TagPoolLayout) getActivity().findViewById(R.id.tagGroup));
		//TagPoolLayout tagGroup = ((TagPoolLayout) getView().findViewById(R.id.tagGroup));
		QuerySet<OWTag> tags = media_obj.getTags(app_context);
		for(OWTag tag : tags){
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
    	if(media_obj == null)
    		return;
    	//String rec_title = media_obj.getTitle(getActivity().getApplicationContext());
    	//String title_input = title.getText().toString();
    	// Title may not be set to empty string
    	if(title.getText().toString().compareTo("") != 0){
    		doSave = true;
    		media_obj.setTitle(getActivity().getApplicationContext(), title.getText().toString());
    	}
    	// if recording has a description, make sure it has changed before saving
    	if(description.getText().toString().compareTo("") != 0){
	    	if(media_obj.getDescription(getActivity().getApplicationContext()) != null && media_obj.getDescription(getActivity().getApplicationContext()).compareTo(description.getText().toString()) != 0){
	    		doSave = true;
	    		media_obj.setDescription(getActivity().getApplicationContext(), description.getText().toString());
	    	}else if(media_obj.getDescription(getActivity().getApplicationContext()) == null ){
	    		doSave = true;
	    		media_obj.setDescription(getActivity().getApplicationContext(), description.getText().toString());
	    	}
    	}
    	
    	if(doSave){
    		media_obj.save(getActivity().getApplicationContext());
    		Log.i(TAG, "Saving recording. " + title.getText().toString() + " : " + description.getText().toString());
    		media_obj.video_recording.get(getActivity().getApplicationContext()).saveAndSync(getActivity().getApplicationContext());
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
		if(media_obj == null)
			return;
		// On autocomplete tag selection
		tags.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int arg2,
					long arg3) {
				Log.i(TAG, "Autocomplete select");
				String tag_name = ((TextView)view).getText().toString();
				if(!media_obj.hasTag(getActivity().getApplicationContext(), tag_name)){
					OWTag tag = OWTag.objects(getActivity().getApplicationContext(), OWTag.class).get((Integer)view.getTag(R.id.list_item_model));
					addTagToOWMediaObject(tag, media_obj);
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
					
					if(!media_obj.hasTag(getActivity().getApplicationContext(), tag_name)){
						Filter filter = new Filter();
						filter.is(DBConstants.TAG_TABLE_NAME, tag_name);
						QuerySet<OWTag> tags = OWTag.objects(getActivity().getApplicationContext(), OWTag.class).filter(filter);
						OWTag selected_tag = null;
						for(OWTag tag : tags){
							selected_tag = tag;
							break;
						}
						if(selected_tag == null){
							selected_tag = new OWTag();
							selected_tag.name.set(tag_name);
							selected_tag.is_featured.set(false);
							selected_tag.save(getActivity().getApplicationContext());
						}
						
						addTagToOWMediaObject(selected_tag, media_obj);
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
	
	private void addTagToOWMediaObject(OWTag tag, OWMediaObject recording){
		tagGroup.addTagPostLayout(tag);
		recording.addTag(getActivity().getApplicationContext(), tag);
		recording.save(getActivity().getApplicationContext());
		doSave = true;
	}


}