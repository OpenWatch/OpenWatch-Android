package net.openwatch.reporter;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.orm.androrm.Filter;
import com.orm.androrm.QuerySet;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.constants.DBConstants;
import net.openwatch.reporter.constants.Constants.OWFeedType;
import net.openwatch.reporter.http.OWServiceRequests;
import net.openwatch.reporter.model.OWServerObject;
import net.openwatch.reporter.model.OWUser;
import net.openwatch.reporter.model.OWVideoRecording;
import net.openwatch.reporter.model.OWTag;
import net.openwatch.reporter.view.TagPoolLayout;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class OWMediaObjectInfoFragment extends SherlockFragment implements
		OWMediaObjectBackedEntity {

	private static final String TAG = "RecordingInfoFragment";

	protected TextView title;
	protected TextView description;
	protected AutoCompleteTextView tags;
	protected OWServerObject media_obj = null;
	protected TagPoolLayout tagGroup;

	protected static boolean watch_tag_text = true;
	protected String tag_query = ""; // autocomplete tag input
	protected OWTagArrayAdapter mAdapter;
	private boolean is_user_recording = false;
	

	boolean doSave = false; // if recording is mutated during this fragments
							// life, saveAndSync onPause

	/**
	 * This fragment assumes a database_id will be passed in the starting Intent
	 * with key: Constants.INTERNAL_DB_ID
	 * 
	 * This fragment automatically populates it's fields with the recording with
	 * id specified on launch and saves any changes when the fragment is paused
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View v;
		if (getArguments() != null
				&& this.getArguments().getBoolean(Constants.IS_USER_RECORDING,
						false)) {
			v = inflater.inflate(R.layout.user_recording_info_view, container,
					false);
			is_user_recording = true;
		} else if (getArguments() == null) {
			v = inflater.inflate(R.layout.user_recording_info_view, container,
					false);
			is_user_recording = true;
		} else {
			v = inflater.inflate(R.layout.remote_recording_info_view,
					container, false);
			is_user_recording = false;
		}

		tagGroup = ((TagPoolLayout) v.findViewById(R.id.tagGroup));
		title = ((TextView) v.findViewById(R.id.editTitle));
		description = ((TextView) v.findViewById(R.id.editDescription));
		tags = ((AutoCompleteTextView) v.findViewById(R.id.editTags));

		if (!is_user_recording)
			setupViewsForRemoteRecording();
		else
			setupViewsForUserRecording();

		int model_id = this.getActivity().getIntent().getExtras()
				.getInt(Constants.INTERNAL_DB_ID);

		if (model_id == 0) {
			Log.e(TAG, "Error getting bundled internal db id");
		} else {
			media_obj = OWServerObject.objects(
					getActivity().getApplicationContext(), OWServerObject.class)
					.get(this.getActivity().getIntent().getExtras()
							.getInt(Constants.INTERNAL_DB_ID));
		}

		if ((OWVideoRecording) media_obj.video_recording.get(getActivity()
				.getApplicationContext()) != null)
			tagGroup.setRecording((OWVideoRecording) media_obj.video_recording
					.get(getActivity().getApplicationContext()));

		setTagAutoCompleteListeners();

		mAdapter = new OWTagArrayAdapter(getActivity().getApplicationContext(),
				R.layout.autocomplete_tag_item, OWTag
						.objects(getActivity(), OWTag.class).all().toList());

		tags.setAdapter(mAdapter);
		tags.setThreshold(1);

		Log.i(TAG,
				"onCreateView. tag adapter size:"
						+ String.valueOf(mAdapter.getCount()));
		return v;
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.i(TAG, "onResume");
		// populateViews(recording, getActivity().getApplicationContext());
	}

	private void setupViewsForRemoteRecording() {
		tagGroup.setTagRemovalAllowed(false);
	}

	private void setupViewsForUserRecording() {
		tagGroup.setTagRemovalAllowed(true);
	}

	@Override
	public void onViewCreated(View view_arg, Bundle savedInstanceState) {
		final View view = view_arg.findViewById(R.id.tagGroup);

		ViewTreeObserver viewTreeObserver = view.getViewTreeObserver();
		if (viewTreeObserver.isAlive()) {

			viewTreeObserver
					.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
						@Override
						public void onGlobalLayout() {
							view.getViewTreeObserver()
									.removeGlobalOnLayoutListener(this);
							// view.getWidth();
							Log.i("onGlobalLayout",
									"width: " + String.valueOf(view.getWidth())
											+ " : "
											+ String.valueOf(view.getHeight()));
							populateViews(media_obj, getActivity()
									.getApplicationContext());
						}
					});
		}
	}

	public void populateViews(OWServerObject media_obj, Context app_context) {
		try {
			if (!is_user_recording) {
				if (media_obj.getUser(app_context).thumbnail_url.get() != null) {
					ImageView user_thumb = (ImageView) this
							.getView().findViewById(R.id.user_thumbnail);
					
					ImageLoader.getInstance().displayImage(media_obj.getUser(app_context).thumbnail_url
							.get(), user_thumb);
				}
				if (media_obj.username.get() != null)
					((TextView) getView().findViewById(R.id.userLabel))
							.setText(media_obj.username.get());

				Log.i("populateViews", String.format(
						"Description: %s Actions: %d Views: %d",
						media_obj.getDescription(app_context),
						media_obj.getActions(app_context),
						media_obj.getViews(app_context)));
				((TextView) getView().findViewById(R.id.action_count))
						.setText(String.valueOf(media_obj
								.getActions(app_context)));
				((TextView) getView().findViewById(R.id.view_count))
						.setText(String.valueOf(media_obj.getViews(app_context)));
			}
			if (media_obj.getTitle(app_context) != null)
				title.setText(media_obj.getTitle(app_context));
			if (media_obj.getDescription(app_context) != null)
				description.setText(media_obj.getDescription(app_context));
			if (media_obj.getTags(app_context) != null){
				populateTagPool(media_obj, app_context);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "Error retrieving recording");
		}

	}

	public void addTagToTagPool(OWTag tag) {
		tagGroup.addTag(tag);
	}

	public void populateTagPool(OWServerObject media_obj, Context app_context) {
		Log.i(TAG, "populateTagPool");
		// TagPoolLayout tagGroup = ((TagPoolLayout)
		// getActivity().findViewById(R.id.tagGroup));
		// TagPoolLayout tagGroup = ((TagPoolLayout)
		// getView().findViewById(R.id.tagGroup));
		QuerySet<OWTag> tags = media_obj.getTags(app_context);
		for (OWTag tag : tags) {
			// addTagToTagPool(tag);
			tagGroup.addTag(tag);
			// tagGroup.addTag(tag.name.get());
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		// Place an action bar item for searching.
		/*
		 * MenuItem item = menu.add("Search");
		 * item.setIcon(android.R.drawable.ic_menu_search);
		 * MenuItemCompat.setShowAsAction(item,
		 * MenuItemCompat.SHOW_AS_ACTION_IF_ROOM |
		 * MenuItemCompat.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW); View searchView
		 * = SearchViewCompat.newSearchView(getActivity()); if (searchView !=
		 * null) { SearchViewCompat.setOnQueryTextListener(searchView, new
		 * OnQueryTextListenerCompat() {
		 * 
		 * @Override public boolean onQueryTextChange(String newText) { //
		 * Called when the action bar search text has changed. Since this // is
		 * a simple array adapter, we can just have it do the filtering.
		 * mCurFilter = !TextUtils.isEmpty(newText) ? newText : null;
		 * mAdapter.getFilter().filter(mCurFilter); return true; } });
		 * MenuItemCompat.setActionView(item, searchView); }
		 */
	}

	@Override
	public void onPause() {
		super.onPause();
		if (media_obj == null)
			return;
		// String rec_title =
		// media_obj.getTitle(getActivity().getApplicationContext());
		// String title_input = title.getText().toString();
		// Title may not be set to empty string
		if (title.getText().toString().compareTo("") != 0) {
			doSave = true;
			media_obj.setTitle(getActivity().getApplicationContext(), title
					.getText().toString());
		}
		// if recording has a description, make sure it has changed before
		// saving
		if (description.getText().toString().compareTo("") != 0) {
			if (media_obj.getDescription(getActivity().getApplicationContext()) != null
					&& media_obj.getDescription(
							getActivity().getApplicationContext()).compareTo(
							description.getText().toString()) != 0) {
				doSave = true;
				media_obj.setDescription(getActivity().getApplicationContext(),
						description.getText().toString());
			} else if (media_obj.getDescription(getActivity()
					.getApplicationContext()) == null) {
				doSave = true;
				media_obj.setDescription(getActivity().getApplicationContext(),
						description.getText().toString());
			}
		}

		if (doSave) {
			media_obj.save(getActivity().getApplicationContext(), true);
			Log.i(TAG, "Saving recording. " + title.getText().toString()
					+ " : " + description.getText().toString());
			media_obj.video_recording
					.get(getActivity().getApplicationContext()).saveAndSync(
							getActivity().getApplicationContext());
		}
	}

	static final String[] TAG_PROJECTION = new String[] { DBConstants.ID,
			DBConstants.TAG_TABLE_NAME };

	/*
	 * This must be set after recording has been initialized and set
	 */
	protected void setTagAutoCompleteListeners() {
		if (media_obj == null)
			return;
		// On autocomplete tag selection
		tags.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int arg2,
					long arg3) {
				Log.i(TAG, "Autocomplete select");
				String tag_name = ((TextView) view).getText().toString();
				if (!media_obj.hasTag(getActivity().getApplicationContext(),
						tag_name)) {
					OWTag tag = OWTag.objects(
							getActivity().getApplicationContext(), OWTag.class)
							.get((Integer) view.getTag(R.id.list_item_model));
					addTagToOWMediaObject(tag, media_obj);
				}
				// watch_tag_text = false;
				tags.setText("");
				// watch_tag_text = true;
				// tags.setAdapter(null);
			}

		});

		// On keyboard enter

		tags.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView view, int actionId,
					KeyEvent event) {

				String[] tag_name_array = view.getText().toString().trim().split(",");

				for (int x = 0; x < tag_name_array.length; x++) {
					String tag_name = tag_name_array[x];

					if (!media_obj.hasTag(
							getActivity().getApplicationContext(), tag_name)) {
						Filter filter = new Filter();
						filter.is(DBConstants.TAG_TABLE_NAME, tag_name);
						QuerySet<OWTag> tags = OWTag.objects(
								getActivity().getApplicationContext(),
								OWTag.class).filter(filter);
						OWTag selected_tag = null;
						for (OWTag tag : tags) {
							selected_tag = tag;
							break;
						}
						if (selected_tag == null) {
							selected_tag = new OWTag();
							selected_tag.name.set(tag_name);
							selected_tag.is_featured.set(false);
							selected_tag.save(getActivity()
									.getApplicationContext());
						}

						addTagToOWMediaObject(selected_tag, media_obj);
						// watch_tag_text = false;
						view.setText("");
						// watch_tag_text = true;
						// ((AutoCompleteTextView)view).setAdapter(null);
					}
				} // end for

				return true;
			}

		});

		/*
		 * tags.addTextChangedListener(new TextWatcher(){
		 * 
		 * @Override public void afterTextChanged(Editable s) { }
		 * 
		 * @Override public void beforeTextChanged(CharSequence s, int start,
		 * int count, int after) { }
		 * 
		 * @Override public void onTextChanged(CharSequence s, int start, int
		 * before, int count) { Log.i(TAG, "onTextChanged. s: " + s.toString() +
		 * " start: " + String.valueOf(start) + " before: " +
		 * String.valueOf(before)); if(watch_tag_text){ //Log.i(TAG,
		 * "onTextChanged"); tag_query = s.toString(); }
		 * 
		 * }
		 * 
		 * });
		 */
	}

	private void addTagToOWMediaObject(OWTag tag, OWServerObject recording) {
		tagGroup.addTagPostLayout(tag);
		recording.addTag(getActivity().getApplicationContext(), tag);
		recording.save(getActivity().getApplicationContext());
		Log.i("TAGGIN", tag.name.get() + " added to media_obj " + recording.getId());
		if (is_user_recording) {
			//Subscribe user to this new tag
			SharedPreferences profile = getActivity().getSharedPreferences(
					Constants.PROFILE_PREFS, 0);
			int user_id = profile.getInt(DBConstants.USER_SERVER_ID, -1);
			if (user_id != -1) {
				Filter filter = new Filter();
				filter.is(DBConstants.USER_SERVER_ID, user_id);
				QuerySet<OWUser> users = OWUser.objects(
						getActivity().getApplicationContext(), OWUser.class)
						.filter(filter);
				for (OWUser user : users) {
					OWServiceRequests.setTags(getActivity()
							.getApplicationContext(), user.tags.get(
							getActivity().getApplicationContext(), user));
					break;
				}
			}
		}
		doSave = true;
	}

}