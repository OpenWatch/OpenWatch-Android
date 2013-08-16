package org.ale.openwatch;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.facebook.model.GraphUser;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.fb.FBUtils;
import org.ale.openwatch.fb.FaceBookSherlockActivity;
import org.ale.openwatch.file.FileUtils;
import org.ale.openwatch.http.OWServiceRequests;
import org.ale.openwatch.model.OWUser;
import org.ale.openwatch.twitter.TwitterUtils;
import twitter4j.User;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created by davidbrodsky on 7/10/13.
 */
public class OWProfileActivity extends FaceBookSherlockActivity {

    private static final String TAG = "FaceBookSherlockActivity";

    ImageView profileImageView;
    TextView firstName;
    TextView lastName;
    TextView blurb;
    Button twitterButton;
    Button fbButton;
    CheckBox agentToggle;

    public File cameraPhotoLocation;

    boolean profileImageSet = false;
    boolean firstNameSet = false;
    boolean lastNameSet = false;
    boolean blurbSet = false;
    String lastProfileImageUrl;

    int model_id = -1;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_profile);
        firstName = (TextView) findViewById(R.id.firstName);
        lastName = (TextView) findViewById(R.id.lastName);
        blurb = (TextView) findViewById(R.id.bio);
        profileImageView = (ImageView) findViewById(R.id.profileImage);
        twitterButton = (Button) findViewById(R.id.twitterButton);
        fbButton = (Button) findViewById(R.id.fbButton);
        agentToggle = (CheckBox) findViewById(R.id.agentToggle);
        profileImageSet = false;
        firstNameSet = false;
        lastNameSet = false;
        blurbSet = false;

        if(TwitterUtils.isAuthenticated(this)){
            twitterButton.setText(getString(R.string.disconnect));
            //findViewById(R.id.socialTip).setVisibility(View.INVISIBLE);
        }

        twitterButton.setOnClickListener(twitterButtonClickListener);

        SharedPreferences prefs = getSharedPreferences(Constants.PROFILE_PREFS, MODE_PRIVATE);
        model_id = prefs.getInt(Constants.INTERNAL_USER_ID, 0);

        if(model_id > 0)
            populateViews(null);
        else
            Log.e(TAG, "Unknown user id!");

        fetchOWUser();
        agentToggle.setOnCheckedChangeListener(agentCheckedChangeListener);
    }

    private void fetchOWUser(){
        if(model_id <= 0)
            return;

        OWServiceRequests.syncOWUser(getApplicationContext(), getUser(), new OWServiceRequests.RequestCallback() {
            @Override
            public void onFailure() {

            }

            @Override
            public void onSuccess() {
                OWUtils.removeUriFromImageCache(getUser().thumbnail_url.get());
                populateViews(getUser());
            }
        });
    }

    private OWUser getUser(){
        if(model_id > 0)
            return OWUser.objects(getApplicationContext(), OWUser.class).get(model_id);
        else
            return null;
    }

    public void getUserAvatarFromDevice(View v){
        OWUtils.setUserAvatar(this, v, OWUtils.SELECT_PHOTO, OWUtils.TAKE_PHOTO, new OWUtils.TakePictureCallback() {
            @Override
            public void gotPotentialPictureLocation(File image) {
                cameraPhotoLocation = image;
            }
        });
    }

    public void populateViews(OWUser user) {
        if(user == null)
            user = getUser();
        if(user.first_name.get() != null && user.first_name.get().compareTo("") != 0)
            firstName.setText(user.first_name.get());
        if(user.last_name.get() != null && user.last_name.get().compareTo("") != 0)
            lastName.setText(user.last_name.get());
        if(user.blurb.get() != null && user.blurb.get().compareTo("") != 0)
            blurb.setText(user.blurb.get());
        agentToggle.setChecked(user.agent_applicant.get());

        if(lastProfileImageUrl == null || lastProfileImageUrl.compareTo(user.thumbnail_url.get()) != 0){
            OWUtils.loadProfilePicture(getApplicationContext(), user, profileImageView);
            lastProfileImageUrl = user.thumbnail_url.get();
        }
    }

    @Override
    protected void onSessionStateChanged(boolean isOpened){
        if(isOpened){
            FBUtils.getProfile(FBUtils.createSession(this, getString(R.string.fb_app_id)), new FBUtils.FacebookUserCallback() {
                @Override
                public void gotProfile(GraphUser user) {
                    Log.i(TAG, "Got user");
                    OWUser owUser = getUser();
                    if(owUser.thumbnail_url.get() == null || owUser.thumbnail_url.get().compareTo("") == 0){
                        String url = String.format("https://graph.facebook.com/%s/picture", user.getId());
                        // TODO: Download image, upload to OW
                        owUser.thumbnail_url.set(url);

                        ImageLoader.getInstance().displayImage(url, profileImageView);
                        profileImageSet = true;
                    }
                    if((owUser.first_name.get() == null || owUser.first_name.get().compareTo("") == 0) && user.getFirstName() != null){
                        firstName.setText(user.getFirstName());
                        owUser.first_name.set(user.getFirstName());
                    }
                    if((owUser.last_name.get() == null || owUser.last_name.get().compareTo("") == 0) && user.getLastName() != null){
                        lastName.setText(user.getLastName());
                        owUser.last_name.set(user.getLastName());
                    }

                    owUser.save(OWProfileActivity.this);
                }
            });
        }
    }

    View.OnClickListener twitterButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(TwitterUtils.isAuthenticated(OWProfileActivity.this)){
                Log.i(TAG, "Logging out of Twitter");
                TwitterUtils.disconnect(OWProfileActivity.this);
                twitterButton.setText(getString(R.string.connect));
            }else{
                Log.i(TAG, "Logging in to Twitter");
                TwitterUtils.authenticate(OWProfileActivity.this, new TwitterUtils.TwitterAuthCallback() {
                    @Override
                    public void onAuth() {
                        // This shouldn't happen, but JIC
                        // This will only be called if the user is already authed with twitter
                        // Else we'll request getUser onActivityResult
                        TwitterUtils.getUser(OWProfileActivity.this, updateViewsWithTwitterUserCallback);
                    }
                });
            }
        }
    };

    TwitterUtils.TwitterUserCallback updateViewsWithTwitterUserCallback =  new TwitterUtils.TwitterUserCallback() {
                @Override
                public void gotUser(User twitterUser) {
                    Log.i(TAG, "Twitter got User");

                    OWUser user = getUser();
                    if (user == null)
                        return;
                    if (twitterUser.getDescription() != null && (user.blurb.get() == null || user.blurb.get().compareTo("") == 0)) {
                        user.blurb.set(twitterUser.getDescription());
                        blurb.setText(twitterUser.getDescription());
                    }
                    if (user.thumbnail_url.get() == null || user.thumbnail_url.get().compareTo("") == 0) {
                        user.thumbnail_url.set(twitterUser.getProfileImageURLHttps());
                        ImageLoader.getInstance().displayImage(twitterUser.getProfileImageURLHttps(), profileImageView);
                        // TODO: Download Image, upload to OW
                    }
                    user.save(OWProfileActivity.this);
                    twitterButton.setText(getString(R.string.disconnect));
                }
            };


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case TwitterUtils.TWITTER_RESULT:
                TwitterUtils.TwitterAuthCallback cb = new TwitterUtils.TwitterAuthCallback() {
                    @Override
                    public void onAuth() {
                        TwitterUtils.getUser(OWProfileActivity.this, updateViewsWithTwitterUserCallback);
                    }
                };

                if(data != null && data.hasExtra("oauth_callback_url")){
                    String oauthCallbackUrl = data.getExtras().getString("oauth_callback_url");
                    TwitterUtils.twitterLoginConfirmation(OWProfileActivity.this, oauthCallbackUrl, cb);
                }else{
                    Log.e(TAG, "onActivityResult did not provide Intent data with twitter oauth callback url");
                }
                break;
        }
        OWUtils.handleSetUserAvatarResult(requestCode, resultCode, data, profileImageView, this, getUser(), cameraPhotoLocation.getAbsolutePath());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_profile, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_save:
                this.finish();
                break;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return true;
    }

    CompoundButton.OnCheckedChangeListener agentCheckedChangeListener = new CompoundButton.OnCheckedChangeListener(){

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            OWUser user = getUser();
            user.agent_applicant.set(isChecked);
            user.save(getApplicationContext());
            if(isChecked)
                OWServiceRequests.syncOWUser(getApplicationContext(), user, null);
        }
    };

}