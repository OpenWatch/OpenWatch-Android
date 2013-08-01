package org.ale.openwatch;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * This class intercepts touch events to prevent
 * swiping pages of the View Pager when on the map tab.
 * This class assumes the map is on the first tab.
 * See onPageScrolled(..) to alter this behavior
 * 
 * @author davidbrodsky
 *
 */
public class MapViewPager extends ViewPager {
	private static final String TAG = "MapViewPager";
	
	private float lastX, maxX = 0;
	// threshold in pixels from screen border to allow scroll in mapview
	private static final float cutoff_threshold = 20; 
	
	// Different scroll behavior on map view
	private boolean onMap = false;
	
	
    public MapViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        // for custom behavior on right view side
        //if(FeedFragmentActivity.display_width != -1)
        //	maxX = FeedFragmentActivity.display_width - cutoff_threshold;
        maxX = cutoff_threshold;	
        //maxX = InputDevice.getMotionRange(MotionEvent.AXIS_X).getMax() - cutoff_threshold ;
        //InputDevice.getDevice(id)
        //Log.d("ViewPager-Maxwidth:",String.valueOf(this.getMeasuredWidth()));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
    
    	//return true; // signal touch event is handled
       return super.onTouchEvent(event);

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
    	if(onMap){
	    	switch (event.getAction()) {
	        case MotionEvent.ACTION_DOWN:
	            lastX = event.getX();
	            //Log.d("DOWN","X: " + String.valueOf(lastX) + " MaxX: " + String.valueOf(maxX));
	            break;
	        case MotionEvent.ACTION_MOVE:
	            if(lastX > maxX){
	            	//Log.i(TAG, "aborting pager scroll");
	                return false;
	            }
    	}
    }
    	return super.onInterceptTouchEvent(event);
    }
    
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    	if(positionOffset == 0){
    		if(position == 1)
    			onMap = true;
    		else
    			onMap = false;
    	}
    	super.onPageScrolled(position, positionOffset, positionOffsetPixels);
    }

}
