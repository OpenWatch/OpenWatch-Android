package org.ale.openwatch;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockActivity;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.http.OWServiceRequests;
import org.ale.openwatch.model.OWMission;
import org.ale.openwatch.model.OWServerObject;
import org.ale.openwatch.model.OWUser;
import org.json.JSONObject;

import java.text.ParseException;

/**
 * Created by davidbrodsky on 6/10/13.
 */
public class MissionViewActivity extends SherlockActivity implements OWMediaObjectBackedEntity{
    private static final String TAG = "MissionViewActivity";

    int model_id = -1;
    int server_id = -1;

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

                        if (OWServerObject.objects(c, OWServerObject.class).get(model_id).story.get(c).body.get() != null) {
                            MissionViewActivity.this.populateViews(OWServerObject.objects(c, OWServerObject.class).get(model_id), c);
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
                OWServiceRequests.getStory(c, story_server_id, new OWServiceRequests.RequestCallback() {

                    @Override
                    public void onFailure() {

                    }

                    @Override
                    public void onSuccess() {
                        if (OWServerObject.objects(c, OWServerObject.class).get(model_id).story.get(c).body.get() != null) {
                            MissionViewActivity.this.populateViews(OWServerObject.objects(c, OWServerObject.class).get(model_id), c);
                        }
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
}