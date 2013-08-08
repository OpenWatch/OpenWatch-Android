package org.ale.openwatch;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.orm.androrm.Filter;
import com.orm.androrm.QuerySet;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.constants.DBConstants;
import org.ale.openwatch.http.OWServiceRequests;
import org.ale.openwatch.model.OWMission;
import org.ale.openwatch.model.OWServerObject;
import org.ale.openwatch.share.Share;
import org.json.JSONObject;

/**
 * Created by davidbrodsky on 6/10/13.
 */
public class OWMissionViewActivity extends SherlockActivity implements OWObjectBackedEntity {
    private static final String TAG = "OWMissionViewActivity";

    int model_id = -1;
    int server_id = -1;

    private static int owphoto_id = -1;
    private static int owphoto_parent_id = -1;

    private String missionTag;

    Context c;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_view);
        c = getApplicationContext();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        OWUtils.setReadingFontOnChildren((ViewGroup) findViewById(R.id.relativeLayout));

        try{
            Bundle extras = getIntent().getExtras();
            if(extras.containsKey(Constants.INTERNAL_DB_ID)){
                model_id = extras.getInt(Constants.INTERNAL_DB_ID);
                server_id = OWServerObject.objects(this, OWServerObject.class).get(model_id).server_id.get();
            }else if(extras.containsKey(Constants.SERVER_ID)){
                server_id = extras.getInt(Constants.SERVER_ID);
                Filter filter = new Filter();
                filter.is(DBConstants.SERVER_ID, server_id);
                QuerySet<OWServerObject> serverObjects = OWServerObject.objects(this, OWServerObject.class).filter(filter);
                for(OWServerObject serverObject : serverObjects){
                    model_id = serverObject.getId();
                }
            }

            if( OWServerObject.objects(this, OWServerObject.class).get(model_id).mission.get(c) != null && OWServerObject.objects(this, OWServerObject.class).get(model_id).mission.get(c).body.get() != null){
                populateViews(model_id, c);
            } else if(OWServerObject.objects(this, OWServerObject.class).get(model_id).title.get() != null){
                ((TextView)findViewById(R.id.title)).setText(OWServerObject.objects(this, OWServerObject.class).get(model_id).title.get());
                this.getSupportActionBar().setTitle(OWServerObject.objects(this, OWServerObject.class).get(model_id).title.get());
                final Context c = this.c;
                OWServerObject serverObject = OWServerObject.objects(this, OWServerObject.class).get(model_id);
                OWServiceRequests.getOWServerObjectMeta(c, serverObject, "", new JsonHttpResponseHandler(){
                    @Override
                    public void onSuccess(JSONObject response) {
                        Log.i(TAG, " success : " + response.toString());

                        if (OWServerObject.objects(c, OWServerObject.class).get(model_id).mission.get(c).body.get() != null) {
                            OWMissionViewActivity.this.populateViews(model_id, c);
                        }
                    }

                    @Override
                    public void onFailure(Throwable e, String response) {
                        Log.i(TAG, " failure: " + response);

                    }

                    @Override
                    public void onFinish() {
                        Log.i(TAG, " finish: ");

                    }

                });
            }
            OWServerObject server_object = OWServerObject.objects(this, OWServerObject.class).get(server_id);
            OWServiceRequests.increaseHitCount(c, server_id , model_id, server_object.getContentType(c), Constants.HIT_TYPE.VIEW);
        }catch(Exception e){
            Log.e(TAG, "Error retrieving model");
            e.printStackTrace();
        }
    }

/*
    public void cameraButtonClick(View v){
        String uuid = OWUtils.generateRecordingIdentifier();
        OWPhoto photo  = OWPhoto.initializeOWPhoto(getApplicationContext(), uuid);
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.putExtra("uuid", uuid);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.parse(photo.filepath.get()));
        owphoto_id = photo.getId();
        owphoto_parent_id = photo.media_object.get(getApplicationContext()).getId();
        Log.i("MainActivity-cameraButtonClick", "get owphoto_id: " + String.valueOf(owphoto_id));
        DeviceLocation.setOWServerObjectLocation(getApplicationContext(), owphoto_parent_id, false);
        startActivityForResult(takePictureIntent, Constants.CAMERA_ACTION_CODE);
    }
*/
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
            case R.id.menu_share:
                if(server_id > 0){
                    OWServerObject object = OWServerObject.objects(getApplicationContext(), OWServerObject.class).get(model_id);
                    Share.showShareDialogWithInfo(this, getString(R.string.share_investigation), object.getTitle(getApplicationContext()), OWUtils.urlForOWServerObject(object, getApplicationContext()));
                    OWServiceRequests.increaseHitCount(getApplicationContext(), server_id, model_id, Constants.CONTENT_TYPE.INVESTIGATION, Constants.HIT_TYPE.CLICK);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onJoinButtonClick(View v){
        OWServerObject serverObject = OWServerObject.objects(getApplicationContext(), OWServerObject.class).get(model_id);
        OWMission mission = serverObject.mission.get(getApplicationContext());
        mission.joined.set(!mission.joined.get());
        mission.save(getApplicationContext());
        if(mission.joined.get() == true){
            findViewById(R.id.join_button).setBackgroundResource(R.drawable.red_button_bg);
            ((TextView) findViewById(R.id.join_button)).setText(getString(R.string.leave_mission));
        }else{
            findViewById(R.id.join_button).setBackgroundResource(R.drawable.green_button_bg);
            ((TextView) findViewById(R.id.join_button)).setText(getString(R.string.join_mission));
        }
        mission.save(getApplicationContext());
        OWServiceRequests.syncOWServerObject(getApplicationContext(), serverObject);
    }

    public void onMapButtonClick(View v){
        OWServerObject serverObject = OWServerObject
                .objects(getApplicationContext(),
                        OWServerObject.class).get(model_id);

        OWMission mission = serverObject.mission.get(c);
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
        this.getSupportActionBar().setTitle(serverObject.getTitle(c));
        ((TextView) this.findViewById(R.id.title)).setText(serverObject.getTitle(c));
        if(mission.media_url.get() != null)
            ImageLoader.getInstance().displayImage(mission.media_url.get(), (ImageView) findViewById(R.id.missionImage));

        boolean bounty = false;
        if(mission.usd.get() != null && mission.usd.get() != 0.0){
            ((TextView) this.findViewById(R.id.bounty)).setText(Constants.USD + String.format("%.2f",mission.usd.get()));
            bounty = true;
        } else if(mission.usd.get() != null && mission.karma.get() != 0.0){
            ((TextView) this.findViewById(R.id.bounty)).setText(String.format("%.0f",mission.karma.get()) + " " + getString(R.string.karma));
            bounty = true;
        }
        if(bounty){
            this.findViewById(R.id.bounty).startAnimation(AnimationUtils.loadAnimation(c, R.anim.slide_left));
            this.findViewById(R.id.bounty).setVisibility(View.VISIBLE);
        }

        if(mission.lat.get() != null && mission.lat.get() != 0){
            findViewById(R.id.map_button).setEnabled(true);
        }else{
            findViewById(R.id.map_button).setEnabled(false);
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

    }
}