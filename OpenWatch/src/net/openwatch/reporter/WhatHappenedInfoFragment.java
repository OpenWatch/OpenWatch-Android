package net.openwatch.reporter;

import net.openwatch.reporter.constants.Constants;
import net.openwatch.reporter.model.OWLocalRecording;
import net.openwatch.reporter.model.OWRecordingTag;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;

public class WhatHappenedInfoFragment extends RecordingInfoFragment {
	private static final String TAG = "WhatHappenedInfoFragment";
	int model_id = -1;
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, container, savedInstanceState);
		
		((EditText)v.findViewById(R.id.editDescription)).setHeight(300);
		Log.i(TAG, "onCreateView");
		try{
			model_id = this.getActivity().getIntent().getExtras().getInt(Constants.INTERNAL_DB_ID);
			recording = OWLocalRecording.objects(getActivity().getApplicationContext(), OWLocalRecording.class)
				.get(model_id);
		}catch(Exception e){
			Log.e(TAG, "Failed to get recording id from intent");
			e.printStackTrace();
		}

		tags.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int arg2,
					long arg3) {
				Log.i(TAG, "Autocomplete select");
				String tag_name = ((TextView)view).getText().toString();
				//TagPoolLayout tagGroup = ((TagPoolLayout) getActivity().findViewById(R.id.tagGroup));
				OWLocalRecording recording = OWLocalRecording.objects(getActivity().getApplicationContext(), OWLocalRecording.class)
						.get(model_id);
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
		
        Log.i(TAG, "onCreateView");
        return v;
    }

}
