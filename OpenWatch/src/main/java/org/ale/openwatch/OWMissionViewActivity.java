package org.ale.openwatch;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.http.OWServiceRequests;
import org.ale.openwatch.model.OWMission;
import org.ale.openwatch.model.OWServerObject;
import org.ale.openwatch.share.Share;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;

/**
 * Created by davidbrodsky on 6/10/13.
 */
public class OWMissionViewActivity extends SherlockActivity implements OWObjectBackedEntity {
    private static final String TAG = "OWMissionViewActivity";

    int model_id = -1;
    int server_id = -1;

    private static int owphoto_id = -1;
    private static int owphoto_parent_id = -1;

    String missionTag;
    String lastBounty; // only animate bounty view if value changes

    Context c;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_view);
        c = getApplicationContext();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        OWUtils.setupAB(this);
        OWUtils.setReadingFontOnChildren((ViewGroup) findViewById(R.id.relativeLayout));
        this.getSupportActionBar().setTitle("");

        processMissionFromIntent(getIntent());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_mission_view, menu);
        return true;
    }


    public void camcorderButtonClick(View v){
        Intent i = new Intent(this, RecorderActivity.class);
        i.putExtra(Constants.OBLIGATORY_TAG, missionTag);
        startActivity(i);

    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data){
        Log.i("OWMissionViewActivity-onActivityResult","got it");
        if(data == null)
            Log.i("OWMissionViewActivity-onActivityResult", "data null");
        if(requestCode == Constants.CAMERA_ACTION_CODE && resultCode == RESULT_OK){
            Intent i = new Intent(this, OWPhotoReviewActivity.class);
            i.putExtra("owphoto_id", owphoto_id);
            i.putExtra(Constants.INTERNAL_DB_ID, owphoto_parent_id); // server object id
            // TODO: Bundle tag to add
            Log.i("OWMissionViewActivity-onActivityResult", String.format("bundling owphoto_id: %d, owserverobject_id: %d",owphoto_id, owphoto_parent_id));
            startActivity(i);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.tab_share:
                if(server_id > 0){
                    OWServerObject object = OWServerObject.objects(getApplicationContext(), OWServerObject.class).get(model_id);
                    Share.showShareDialogWithInfo(this, getString(R.string.share_mission), object.getTitle(getApplicationContext()), OWUtils.urlForOWServerObject(object, getApplicationContext()));
                    OWServiceRequests.increaseHitCount(getApplicationContext(), server_id, model_id, Constants.CONTENT_TYPE.INVESTIGATION, Constants.HIT_TYPE.CLICK);
                }
                break;
            case R.id.tab_record:
                Intent i = new Intent(this, RecorderActivity.class);
                i.putExtra(Constants.MISSION_SERVER_OBJ_ID, model_id);
                startActivity(i);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public final boolean testJoinDialog = false;
    public void onJoinButtonClick(View v){
        OWServerObject serverObject = OWServerObject.objects(getApplicationContext(), OWServerObject.class).get(model_id);
        OWMission mission = serverObject.mission.get(getApplicationContext());
        if(mission.joined.get() != null && mission.joined.get().length() > 0){
            mission.joined.set(null);
        }else{
            mission.joined.set(Constants.utc_formatter.format(new Date()));
            SharedPreferences prefs = getSharedPreferences(Constants.PROFILE_PREFS, MODE_PRIVATE);
            if(testJoinDialog || !prefs.getBoolean(Constants.JOINED_FIRST_MISSION, false)){
                showJoinedFirstMissionDialog();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(Constants.JOINED_FIRST_MISSION, true);
                editor.commit();
            }
        }

        mission.save(getApplicationContext());
        OWMission.ACTION action = setJoinMissionButtonWithMission(mission);
        mission.save(getApplicationContext());
        OWServiceRequests.postMissionAction(getApplicationContext(), serverObject, action);
    }

    private OWMission.ACTION setJoinMissionButtonWithMission(OWMission mission){
        int missionMembers = 0;
        try{
            missionMembers = Integer.parseInt(((TextView) findViewById(R.id.members)).getText().toString());
        }catch(NumberFormatException e){}

        OWMission.ACTION action;
        String analyticsEvent = null;
        if(mission.joined.get() != null && mission.joined.get().length() > 0){
            action = OWMission.ACTION.JOINED;
            findViewById(R.id.join_button).setBackgroundResource(R.drawable.red_button_bg);
            ((TextView) findViewById(R.id.join_button)).setText(getString(R.string.leave_mission));
            ((TextView) findViewById(R.id.members)).setText(String.valueOf(missionMembers+1));
            analyticsEvent = Analytics.JOINED_MISSION;
        }else{
            action = OWMission.ACTION.LEFT;
            findViewById(R.id.join_button).setBackgroundResource(R.drawable.button_bg);
            ((TextView) findViewById(R.id.join_button)).setText(getString(R.string.join_mission));
            if(missionMembers != 0)
                ((TextView) findViewById(R.id.members)).setText(String.valueOf(missionMembers-1));
            analyticsEvent = Analytics.LEFT_MISSION;
        }
        try {
            JSONObject analyticsPayload = new JSONObject().put(Analytics.mission, server_id);
            Analytics.trackEvent(getApplicationContext(), analyticsEvent, analyticsPayload);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return action;
    }

    public void onMapButtonClick(View v){
        OWServerObject serverObject = OWServerObject
                .objects(getApplicationContext(),
                        OWServerObject.class).get(model_id);

        Intent i = new Intent(this, MapActivity.class);
        i.putExtra(Constants.INTERNAL_DB_ID, model_id);
        this.startActivity(i);
    }

    public void onMediaButtonClick(View v){
        OWServerObject serverObject = OWServerObject
                .objects(getApplicationContext(),
                        OWServerObject.class).get(model_id);

        OWMission mission = serverObject.mission.get(c);

        Intent i = new Intent(this, FeedFragmentActivity.class);
        i.setData(Uri.parse("https://openwatch.net/w/" + mission.tag.get()));
        startActivity(i);

    }

    @Override
    public void populateViews(int model_id, Context app_context) {
        OWServerObject serverObject = OWServerObject
                .objects(getApplicationContext(),
                        OWServerObject.class).get(model_id);

        OWMission mission = serverObject.mission.get(c);
        missionTag = mission.tag.get();
        //this.getSupportActionBar().setTitle(serverObject.getTitle(c));
        ((TextView) this.findViewById(R.id.title)).setText(serverObject.getTitle(c));
        if(mission.media_url.get() != null)
            ImageLoader.getInstance().displayImage(mission.media_url.get(), (ImageView) findViewById(R.id.missionImage));

        lastBounty =  ((TextView) this.findViewById(R.id.bounty)).getText().toString();
        String thisBounty = null;
        if(mission.usd.get() != null && mission.usd.get() != 0.0){
            thisBounty = Constants.USD + String.format("%.2f",mission.usd.get());
        } else if(mission.usd.get() != null && mission.karma.get() != 0.0){
            thisBounty = String.format("%.0f",mission.karma.get()) + " " + getString(R.string.karma);
        }
        if(thisBounty != null && (lastBounty == null || (lastBounty.compareTo(thisBounty) != 0) ) ){
            this.findViewById(R.id.bounty).startAnimation(AnimationUtils.loadAnimation(c, R.anim.slide_left));
            this.findViewById(R.id.bounty).setVisibility(View.VISIBLE);
            ((TextView) this.findViewById(R.id.bounty)).setText(thisBounty);
            lastBounty = thisBounty;
        }

        if(mission.lat.get() != null && mission.lat.get() != 0){
            findViewById(R.id.map_button).setEnabled(true);
        }else{
            findViewById(R.id.map_button).setEnabled(false);
        }
        if(mission.members.get() != null && mission.members.get() != 0){
            ((TextView)findViewById(R.id.members)).setText(String.valueOf(mission.members.get()));
        }else{
            ((TextView)findViewById(R.id.members)).setText("0");
        }

        if(mission.submissions.get() != null && mission.submissions.get() != 0){
            ((TextView)findViewById(R.id.submissions)).setText(String.valueOf(mission.submissions.get()));
        }else{
            ((TextView)findViewById(R.id.submissions)).setText("0");
        }

        if(mission.expires.get() != null && mission.expires.get().length() > 0){
            try {
                ((TextView)findViewById(R.id.expiry)).setText(getString(R.string.expires) + " " + DateUtils.getRelativeTimeSpanString(Constants.utc_formatter.parse(mission.expires.get()).getTime()));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        /*
        OWUser user = serverObject.user.get(c);
        if(user != null){
            ((TextView) this.findViewById(R.id.userTitle)).setText(user.username.get());
            if(user.thumbnail_url.get() != null && user.thumbnail_url.get().compareTo("") != 0)
                ImageLoader.getInstance().displayImage(user.thumbnail_url.get(), (ImageView) findViewById(R.id.userImage));
        }
        */
        ((TextView) this.findViewById(R.id.description)).setText(mission.body.get());

        ((TextView) this.findViewById(R.id.description)).getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener(){

            @Override
            public void onGlobalLayout() {
                if(findViewById(R.id.scrollView) != null){
                    ((ScrollView)findViewById(R.id.scrollView)).scrollTo(0, 0);
                }
            }
        });

        setJoinMissionButtonWithMission(mission);

    }

    private void showJoinedFirstMissionDialog(){
        View v = getLayoutInflater().inflate(R.layout.dialog_join_mission, null);
        new AlertDialog.Builder(this)
                .setView(v)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    @Override
    protected void onNewIntent (Intent intent){
        processMissionFromIntent(intent);
    }

    private void processMissionFromIntent(Intent intent){
        model_id = 0;
        server_id = 0;
        Bundle extras = intent.getExtras();
        OWServerObject serverObject = null;
        final boolean viewedPush = extras.getBoolean("viewed_push", false);
        boolean beganMissionUpdate = false;
        if(extras.containsKey(Constants.INTERNAL_DB_ID)){
            model_id = extras.getInt(Constants.INTERNAL_DB_ID);
            serverObject = OWServerObject.objects(this, OWServerObject.class).get(model_id);
            server_id = serverObject.server_id.get();
        }else if(extras.containsKey(Constants.SERVER_ID)){
            server_id = extras.getInt(Constants.SERVER_ID);
            Log.i("PUSH", "Got bundled mission server_id " + String.valueOf(server_id));
            serverObject = OWMission.getOWServerObjectByOWMissionServerId(getApplicationContext(), server_id);
            if(serverObject != null){
                model_id = serverObject.getId();
                if(viewedPush)
                    onViewedMissionFromPushAlert();
            }else{
                OWServiceRequests.updateOrCreateOWServerObject(getApplicationContext(), Constants.CONTENT_TYPE.MISSION, String.valueOf(server_id), "", new OWServiceRequests.RequestCallback() {
                    @Override
                    public void onFailure() {}

                    @Override
                    public void onSuccess() {
                        OWServerObject serverObject = OWMission.getOWServerObjectByOWMissionServerId(getApplicationContext(), server_id);
                        if(serverObject != null){
                            populateViews(serverObject.getId(), getApplicationContext());
                            if(viewedPush)
                                onViewedMissionFromPushAlert();
                        }
                    }
                });
                beganMissionUpdate = true;
            }

        }

        if(serverObject == null)
            return;

        if( serverObject.mission.get(c) != null && serverObject.mission.get(c).body.get() != null){
            populateViews(model_id, c);
        } else if(serverObject.title.get() != null){
            ((TextView)findViewById(R.id.title)).setText(serverObject.title.get());
        }

        if(!beganMissionUpdate){
            final Context c = this.c;
            OWServiceRequests.getOWServerObjectMeta(c, serverObject, "", new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(JSONObject response) {
                    Log.i(TAG, " success : " + response.toString());

                    if (OWServerObject.objects(c, OWServerObject.class).get(model_id).mission.get(c).body.get() != null) {
                        OWMissionViewActivity.this.populateViews(model_id, c);
                    }
                }

                @Override
                public void onFailure(Throwable e, String response) {Log.i(TAG, " failure: " + response);}

                @Override
                public void onFinish() {}

            });
        }
        onViewedMission();

    }

    private void onViewedMissionFromPushAlert(){
        OWServerObject serverObject = OWServerObject.objects(getApplicationContext(), OWServerObject.class).get(model_id);
        if(serverObject != null)
            OWServiceRequests.postMissionAction(getApplicationContext(), serverObject, OWMission.ACTION.VIEWED_PUSH);
        try {
            JSONObject analyticsPayload = new JSONObject().put(Analytics.mission, server_id);
            Analytics.trackEvent(getApplicationContext(), Analytics.VIEWED_MISSION_PUSH, analyticsPayload);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void onViewedMission(){
        OWServerObject serverObject = OWServerObject.objects(getApplicationContext(), OWServerObject.class).get(model_id);
        OWServiceRequests.increaseHitCount(c, server_id , model_id, serverObject.getContentType(c), Constants.HIT_TYPE.VIEW);
        OWServiceRequests.postMissionAction(getApplicationContext(), serverObject, OWMission.ACTION.VIEWED_MISSION);
        try {
            JSONObject analyticsPayload = new JSONObject().put(Analytics.mission, server_id);
            Analytics.trackEvent(getApplicationContext(), Analytics.VIEWED_MISSION, analyticsPayload);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}