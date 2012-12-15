package net.openwatch.reporter.view;

import net.openwatch.reporter.R;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

public class TagPoolLayout extends TableLayout {
	private static final String TAG = "TagPoolLayout";
	
	int PIX_BUFFER = 200;
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

	
	public void addTag(String new_tag_text){
		int new_tag_width = 0;
		TextView new_tag = null;
		Log.i(TAG, "Tagpool has #rows: " + String.valueOf(this.getChildCount()));
		
		if(last_row == null || last_tag == null || new_tag_width + PIX_BUFFER >= last_row.getRight() - last_tag.getRight()){ // this is the first tag in new row
			Log.i(TAG, "new row");
			last_row = (LinearLayout) inflater.inflate(R.layout.tag_pool_row, this, false);
			new_tag = (TextView) inflater.inflate( R.layout.tag_pool_item, last_row, false);
			new_tag.setText(new_tag_text);
			last_row.addView(new_tag);
			this.addView(last_row);
			last_tag = new_tag;
			return;
		}
		else{ // this tag will be appended to an existing row
			Log.i(TAG," row/tag right: " + String.valueOf(last_row.getRight()) + " / " + String.valueOf(last_tag.getRight()));
			new_tag = (TextView) inflater.inflate( R.layout.tag_pool_item, last_row, false);
			new_tag.setText(new_tag_text);
			// set left margin
			last_row.addView(new_tag);
			last_tag = new_tag;
		}
		
			
	}
	
	@Override
	protected void onLayout (boolean changed, int l, int t, int r, int b){
		super.onLayout(changed, l, t, r, b);
		
	}

}
