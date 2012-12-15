package net.openwatch.reporter.view;

import java.util.LinkedList;
import net.openwatch.reporter.R;
import net.openwatch.reporter.model.OWRecordingTag;
import android.content.Context;
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
	boolean delete_dummy = true;
	boolean first_tag = true;

	LayoutInflater inflater = (LayoutInflater)getContext().getSystemService
    (Context.LAYOUT_INFLATER_SERVICE);
	
	public TagPoolLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public TagPoolLayout(Context context) {
		super(context);
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
			
			Log.i(TAG, "new tag view width: " + String.valueOf(new_tag.getRight()) + " right: " + String.valueOf(new_tag.getRight()));
			return;
		}
		else{ // this tag will be appended to an existing row
			last_row.addView(new_tag);
			last_tag = new_tag;
			Log.i(TAG, "new tag view width: " + String.valueOf(new_tag.getRight()) + " right: " + String.valueOf(new_tag.getRight()));
		}
	}
	
	private void addTagToNewRow(View new_tag){
		last_row = (LinearLayout) inflater.inflate(R.layout.tag_pool_row, this, false);
		this.addView(last_row);
		last_row.addView(new_tag);
		last_tag = new_tag;
		
	}

	
	public void addTag(OWRecordingTag tag){
		TextView new_tag = (TextView) inflater.inflate( R.layout.tag_pool_item, last_row, false);
		new_tag.setText(tag.name.get());
		new_tag.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener(){

			@Override
			public void onGlobalLayout() {
				drawTag();
			}
			
		});
		
		if(first_tag){
			Log.i(TAG, "adding first tag");
			addTagToNewRow(new_tag);
			first_tag = false;
		} else{
			Log.i(TAG, "queueing subsequent tag");
			view_buffer.add(new_tag);
		}
		
	}
	
	@Override
	protected void onLayout (boolean changed, int l, int t, int r, int b){
		super.onLayout(changed, l, t, r, b);

	}

}
