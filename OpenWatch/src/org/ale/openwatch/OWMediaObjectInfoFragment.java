package org.ale.openwatch;

import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.model.OWServerObject;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.nostra13.universalimageloader.core.ImageLoader;

public class OWMediaObjectInfoFragment extends SherlockFragment implements
        OWObjectBackedEntity {

	private static final String TAG = "RecordingInfoFragment";

	protected TextView title;
	protected TextView edit_title;
	protected TextView title_length_warning;
	protected OWServerObject media_obj = null;

	private boolean is_user_owner = false;

	boolean doSave = false; // if recording is mutated during this fragments
							// life, saveAndSync onPause
	
	private final int MAX_TITLE_LENGTH = 254;
	private final int TITLE_WARNING_LENGTH = 200;

	/**
	 * This fragment assumes a database_id will be passed in the starting Intent
	 * with key: Constants.INTERNAL_DB_ID
	 * 
	 * This fragment automatically populates it's fields with the media object with
	 * id specified on launch and saves/transmits any changes when the fragment is paused
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View v;
		if (getArguments() != null
				&& this.getArguments().getBoolean(Constants.IS_USER_RECORDING,
						false)) {
			v = inflater.inflate(R.layout.user_media_object_info_view, container,
					false);
			is_user_owner = true;
		} else if (getArguments() == null) {
			v = inflater.inflate(R.layout.user_media_object_info_view, container,
					false);
			is_user_owner = true;
		} else {
			v = inflater.inflate(R.layout.remote_media_object_info_view,
					container, false);
			is_user_owner = false;
		}

		title = ((TextView) v.findViewById(R.id.editTitle));

		if (!is_user_owner)
			setupViewsForRemoteRecording(v);
		else
			setupViewsForUserRecording(v);

		int model_id = this.getActivity().getIntent().getExtras()
				.getInt(Constants.INTERNAL_DB_ID);

		if (model_id == 0) {
			Log.e(TAG, "Error getting bundled internal db id");
		} else {
			media_obj = OWServerObject
					.objects(getActivity().getApplicationContext(),
							OWServerObject.class).get(
							this.getActivity().getIntent().getExtras()
									.getInt(Constants.INTERNAL_DB_ID));
		}

		return v;
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.i(TAG, "onResume");
		// populateViews(recording, getActivity().getApplicationContext());
	}

	@Override
	public void onViewCreated(View view_arg, Bundle savedInstanceState) {
		populateViews(media_obj, getActivity().getApplicationContext());
	}

	public void populateViews(OWServerObject media_obj, Context app_context) {
		try {
			if (!is_user_owner) {
				if (media_obj.getUser(app_context).thumbnail_url.get() != null) {
					ImageView user_thumb = (ImageView) this.getView()
							.findViewById(R.id.user_thumbnail);

					ImageLoader.getInstance().displayImage(
							media_obj.getUser(app_context).thumbnail_url.get(),
							user_thumb);
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
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "Error retrieving recording");
		}

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
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
            Log.i(TAG, String.format("setting media_obj title: %s", title.getText().toString()));
		}
		if (doSave) {
			media_obj.save(getActivity().getApplicationContext(), true);
			Log.i(TAG, "Saving recording. " + title.getText().toString());
			media_obj.saveAndSync(getActivity().getApplicationContext());
		}
	}
	
	private void setupViewsForRemoteRecording(View root) {
	}

	private void setupViewsForUserRecording(View root) {
		title_length_warning = (TextView) root.findViewById(R.id.editTitleLengthWarning);
		edit_title = (EditText) root.findViewById(R.id.editTitle);
		edit_title.setFilters( new InputFilter[] { new InputFilter.LengthFilter(MAX_TITLE_LENGTH) } );
		edit_title.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {
				if( arg0.length() > TITLE_WARNING_LENGTH){
					title_length_warning.setVisibility(View.VISIBLE);
					title_length_warning.setText(String.valueOf(MAX_TITLE_LENGTH - arg0.length()) + " characters remaining.");
				}
				else
					title_length_warning.setVisibility(View.GONE);
			}
			
		});
	}
	
	
}