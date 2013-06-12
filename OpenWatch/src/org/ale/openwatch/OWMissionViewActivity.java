package org.ale.openwatch;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockActivity;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.http.OWServiceRequests;
import org.ale.openwatch.location.DeviceLocation;
import org.ale.openwatch.model.OWMission;
import org.ale.openwatch.model.OWPhoto;
import org.ale.openwatch.model.OWServerObject;
import org.ale.openwatch.model.OWUser;
import org.json.JSONObject;

/**
 * Created by davidbrodsky on 6/10/13.
 */
public class OWMissionViewActivity extends SherlockActivity implements OWMediaObjectBackedEntity{
    private static final String TAG = "OWMissionViewActivity";

    int model_id = -1;
    int server_id = -1;

    private static int owphoto_id = -1;
    private static int owphoto_parent_id = -1;

    Context c;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_view);
        c = getApplicationContext();

        try{
            model_id = getIntent().getExtras().getInt(Constants.INTERNAL_DB_ID);
            server_id = OWServerObject.objects(this, OWServerObject.class).get(model_id).server_id.get();
            if( OWServerObject.objects(this, OWServerObject.class).get(model_id).story.get(c) != null && OWServerObject.objects(this, OWServerObject.class).get(model_id).story.get(c).body.get() != null){
                populateViews(OWServerObject.objects(this, OWServerObject.class).get(model_id), c);
            } else if(OWServerObject.objects(this, OWServerObject.class).get(model_id).title.get() != null){
                ((TextView)findViewById(R.id.title)).setText(OWServerObject.objects(this, OWServerObject.class).get(model_id).title.get());
                this.getSupportActionBar().setTitle(OWServerObject.objects(this, OWServerObject.class).get(model_id).title.get());
                final Context c = this.c;
                OWServerObject serverObject = OWServerObject.objects(this, OWServerObject.class).get(model_id);
                int story_server_id = serverObject.getServerId(c);
                OWServiceRequests.getOWServerObjectMeta(c, serverObject, "", new JsonHttpResponseHandler(){
                    @Override
                    public void onSuccess(JSONObject response) {
                        Log.i(TAG, " success : " + response.toString());

                        if (OWServerObject.objects(c, OWServerObject.class).get(model_id).mission.get(c).body.get() != null) {
                            OWMissionViewActivity.this.populateViews(OWServerObject.objects(c, OWServerObject.class).get(model_id), c);
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
            OWServiceRequests.increaseHitCount(c, server_id , model_id, server_object.getContentType(c), server_object.getMediaType(c), Constants.HIT_TYPE.VIEW);
        }catch(Exception e){
            Log.e(TAG, "Error retrieving model");
            e.printStackTrace();
        }
    }

    @Override
    public void populateViews(OWServerObject serverObject, Context c) {
        OWMission mission = serverObject.mission.get(c);
        this.getSupportActionBar().setTitle(serverObject.getTitle(c));
        ((TextView) this.findViewById(R.id.title)).setText(serverObject.getTitle(c));
        if(mission.media_url.get() != null)
            ImageLoader.getInstance().displayImage(mission.media_url.get(), (ImageView) findViewById(R.id.missionImage));

        if(mission.usd.get() != null && mission.usd.get() != 0.0)
            ((TextView) this.findViewById(R.id.bounty)).setText(Constants.USD + mission.usd.get().toString());
        else if(mission.usd.get() != null && mission.karma.get() != 0.0)
            ((TextView) this.findViewById(R.id.bounty)).setText(mission.karma.get().toString() + getString(R.string.karma));

        OWUser user = serverObject.user.get(c);
        if(user != null){
            ((TextView) this.findViewById(R.id.userTitle)).setText(user.username.get());
            if(user.thumbnail_url.get() != null && user.thumbnail_url.get().compareTo("") != 0)
                ImageLoader.getInstance().displayImage(user.thumbnail_url.get(), (ImageView) findViewById(R.id.userImage));
        }
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

    public void camcorderButtonClick(View v){
        Intent i = new Intent(this, RecorderActivity.class);
        // TODO: Bundle tag to add
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
}