package net.openwatch.reporter.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import net.openwatch.reporter.R;
import net.openwatch.reporter.model.OWVideoRecording;
import net.openwatch.reporter.model.OWTag;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

public class TagPoolLayout extends TableLayout {
	private static final String TAG = "TagPoolLayout";
	
	int PIX_BUFFER = 200;
	LinkedList<View> view_buffer = new LinkedList<View>();
	HashMap<String, Boolean> tags = new HashMap<String, Boolean>();
	boolean delete_dummy = true;
	boolean first_tag = true;
	
	boolean allow_tag_removal = false;
	
	OWVideoRecording recording;

	LayoutInflater inflater = (LayoutInflater)getContext().getSystemService
    (Context.LAYOUT_INFLATER_SERVICE);
	
	public TagPoolLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public TagPoolLayout(Context context) {
		super(context);
	}
	
	public TagPoolLayout(Context context, OWVideoRecording recording) {
		super(context);
		this.recording = recording;
	}
		
	public void setRecording(OWVideoRecording recording){
		this.recording = recording;
	}
	
	public void setTagRemovalAllowed(boolean isAllowed){
		allow_tag_removal = isAllowed;
	}

	LinearLayout last_row;
	View last_tag;
	
	private void drawTag(){
		TextView new_tag = (TextView) view_buffer.poll();
		if(new_tag == null)
			return;
		if(last_row != null && delete_dummy){
			last_row.getChildAt(0).setLayoutParams(new_tag.getLayoutParams());
			delete_dummy = false;
		}
		
		int new_tag_width = new_tag.getWidth();
		if(last_row == null || last_tag == null || new_tag_width + PIX_BUFFER >= last_row.getRight() - last_tag.getRight()){ // this is the first tag in new row
			if(last_row != null && last_tag != null)
				Log.i(TAG, "new row spacing calc: " + String.valueOf(last_row.getRight()) + " - " + String.valueOf(last_tag.getRight()));
		
			addTagToNewRow(new_tag);
			
			//Log.i(TAG, "new tag view width: " + String.valueOf(new_tag.getRight()) + " right: " + String.valueOf(new_tag.getRight()));
			return;
		}
		else{ // this tag will be appended to an existing row
			last_row.addView(new_tag);
			last_tag = new_tag;
			//Log.i(TAG, "new tag view width: " + String.valueOf(new_tag.getRight()) + " right: " + String.valueOf(new_tag.getRight()));
		}
	}
	
	private void addTagToNewRow(View new_tag){
		last_row = (LinearLayout) inflater.inflate(R.layout.tag_pool_row, this, false);
		this.addView(last_row);
		last_row.addView(new_tag);
		last_tag = new_tag;
		
	}

	
	public void addTag(OWTag tag){
		if(!tags.containsKey(tag.name.get()) ){
			TextView new_tag = (TextView) initTagView(tag);
			new_tag.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener(){
	
				@Override
				public void onGlobalLayout() {
					drawTag();
				}
				
			});
			
			if(first_tag){
				//Log.i(TAG, "adding first tag");
				addTagToNewRow(new_tag);
				first_tag = false;
			} else{
				//Log.i(TAG, "queueing subsequent tag");
				view_buffer.add(new_tag);
			}
			tags.put(tag.name.get(), true);
		}
	}
	
	public void addTagPostLayout(OWTag tag){
		if(!tags.containsKey(tag.name.get()) ){
			TextView new_tag = (TextView) initTagView(tag);
			view_buffer.add(new_tag);
			drawTag();
			tags.put(tag.name.get(), true);
		}
	}
	
	private View initTagView(final OWTag tag){
		final Context c = this.getContext();
		final TagPoolLayout tpl = this;
		TextView new_tag = (TextView) inflater.inflate( R.layout.tag_pool_item, last_row, false);
		new_tag.setText(tag.name.get());
		
		new_tag.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(final View v) {
				if(!allow_tag_removal)
					return;
				
				AlertDialog.Builder builder = new AlertDialog.Builder(c);
				builder.setTitle(c.getString(R.string.remove_tag_dialog_title))
				.setMessage(c.getString(R.string.remove_tag_dialog_msg))
				.setPositiveButton(c.getString(R.string.remove_tag_dialog_yes), new DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface dialog, int which) {
						v.setVisibility(View.GONE);
						recording.removeTag(c, tag);
						//OWRecording.objects(c, OWRecording.class).get(ow_recording_db_id).removeTag(c, tag);
						dialog.dismiss();
					}

				}).setNegativeButton(c.getString(R.string.remove_tag_dialog_cancel), new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				}).show();	
			}
			
		});
		return new_tag;
	}
	
	@Override
	protected void onLayout (boolean changed, int l, int t, int r, int b){
		super.onLayout(changed, l, t, r, b);

	}

}
