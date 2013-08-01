package org.ale.openwatch.view;

/**
 * Created by davidbrodsky on 6/19/13.
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import org.ale.openwatch.MapActivity;
import org.ale.openwatch.OWUtils;
import org.ale.openwatch.R;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.http.OWServiceRequests;
import org.ale.openwatch.model.OWServerObject;
import org.ale.openwatch.model.OWServerObjectInterface;
import org.ale.openwatch.share.Share;

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
            //Log.i("TouchableLayout", "Touchdown!");
            // TouchableSubviewRelativeLayout tags:
            // 0 : timelapse id corresponding to this view
            // 1 : "camera" or "view", indicating the behavior for ListView item select

            //Log.d("TouchableSubviewRelativeLayout",this.getTag(R.id.view_related_timelapse).toString());
            // If the touch occurs in the area of the camera icon, go to picture

            if(OWUtils.isPointInsideView(me.getRawX(), me.getRawY(), this.findViewById(R.id.menu))){
                Log.d("TouchableSubviewRelativeLayout","menu");
                showMenuDialog();
                return true;
            }

        }

        // pass touch onward
        return false;
    }

    public boolean onTouchEvent(MotionEvent me){
        return false;

    }

    // Something related to VideoView intercepts touches to other ViewGroup children
    // So touches necessary during VideoView playback must be done at the ViewGroup level until I have better control of VideoView
    private void showMenuDialog(){
        final int model_id = (Integer)this.getTag(R.id.list_item_model);
        final OWServerObject server_object = OWServerObject.objects(getContext().getApplicationContext(), OWServerObject.class).get(model_id);
        final Context c = getContext();
        LayoutInflater inflater = (LayoutInflater)
                c.getSystemService(c.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.media_menu_popup, this, false);
        final AlertDialog dialog =  new AlertDialog.Builder(c).setView(layout).create();
        layout.findViewById(R.id.shareButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Share.showShareDialogWithInfo(c, c.getString(R.string.share_video), server_object.getTitle(c), OWUtils.urlForOWServerObject(server_object, c));
                OWServiceRequests.increaseHitCount(c, server_object.getServerId(c), model_id, server_object.getContentType(c), Constants.HIT_TYPE.CLICK);
            }
        });
        if(((OWServerObjectInterface) server_object.getChildObject(c)).getLat(c) != 0.0 ){
            layout.findViewById(R.id.mapButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    Intent i = new Intent(c, MapActivity.class);
                    i.putExtra(Constants.INTERNAL_DB_ID, model_id);
                    c.startActivity(i);
                }
            });
        }else
            layout.findViewById(R.id.mapButton).setVisibility(View.GONE);
        layout.findViewById(R.id.reportButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                OWServiceRequests.flagOWServerObjet(getContext().getApplicationContext(), server_object);
            }
        });
        dialog.show();
    }

}