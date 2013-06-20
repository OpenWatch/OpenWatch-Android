package org.ale.openwatch.view;

/**
 * Created by davidbrodsky on 6/19/13.
 */

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import org.ale.openwatch.R;

public class TouchableSubviewRelativeLayout extends RelativeLayout {

    public TouchableSubviewRelativeLayout(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    public TouchableSubviewRelativeLayout(Context context, AttributeSet as){
        super(context, as);
    }

    public TouchableSubviewRelativeLayout(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    // On any screen touch outside input
    public boolean onInterceptTouchEvent(MotionEvent me){
        if(me.getAction() == me.ACTION_DOWN ){
            // TouchableSubviewRelativeLayout tags:
            // 0 : timelapse id corresponding to this view
            // 1 : "camera" or "view", indicating the behavior for ListView item select

            //Log.d("TouchableSubviewRelativeLayout",this.getTag(R.id.view_related_timelapse).toString());
            // If the touch occurs in the area of the camera icon, go to picture

            if(isPointInsideView(me.getRawX(), me.getRawY(), this.findViewById(R.id.menu))){
                //Log.d("TouchableSubviewRelativeLayout","camera");
                this.setTag(R.id.subView,"menu");
            }
            else{
                //Log.d("TouchableSubviewRelativeLayout","view");
                this.setTag(R.id.subView,"other");
            }

        }

        // pass touch onward
        return false;
    }

    public boolean onTouchEvent(MotionEvent me){
        return false;

    }

    /**
     * Determines if given points are inside view
     * @param x - x coordinate of point
     * @param y - y coordinate of point
     * @param view - view object to compare
     * @return true if the points are within view bounds, false otherwise
     */
    private boolean isPointInsideView(float x, float y, View view){
        int location[] = new int[2];
        view.getLocationOnScreen(location);
        int viewX = location[0];
        int viewY = location[1];

        //point is inside view bounds
        if(( x > viewX && x < (viewX + view.getWidth())) &&
                ( y > viewY && y < (viewY + view.getHeight()))){
            return true;
        } else {
            return false;
        }
    }

}