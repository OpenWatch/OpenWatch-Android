package org.ale.openwatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.MemoryCacheUtil;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.database.DatabaseManager;
import org.ale.openwatch.fb.FBUtils;
import org.ale.openwatch.file.FileUtils;
import org.ale.openwatch.http.OWServiceRequests;
import org.ale.openwatch.model.OWServerObject;
import org.ale.openwatch.model.OWUser;
import org.ale.openwatch.twitter.TwitterUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class OWUtils {
    private static final String TAG = "OWUtils";

    public static final int SELECT_PHOTO = 100;
    public static final int TAKE_PHOTO = 101;
	
	public static String generateRecordingIdentifier()
	{
		return UUID.randomUUID().toString();
	}
	
	public static void loadScaledPicture(String image_path, ImageView target) {
	    // Get the dimensions of the View
	    int targetW = target.getWidth();
	    int targetH = target.getHeight();
	  
	    // Get the dimensions of the bitmap
	    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
	    bmOptions.inJustDecodeBounds = true;
	    BitmapFactory.decodeFile(image_path, bmOptions);
	    int photoW = bmOptions.outWidth;
	    int photoH = bmOptions.outHeight;
	  
	    // Determine how much to scale down the image
	    int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        //Log.i("loadScaledPicture", String.format("imageview is %dx%d. image is %dx%d. Scale factor is %d", targetW, targetH, photoW, photoH, scaleFactor));
	    // Decode the image file into a Bitmap sized to fill the View
	    bmOptions.inJustDecodeBounds = false;
	    bmOptions.inSampleSize = scaleFactor;
	    bmOptions.inPurgeable = true;
	  
	    Bitmap bitmap = BitmapFactory.decodeFile(image_path, bmOptions);
	    target.setImageBitmap(bitmap);
	}
	
	public static String urlForOWServerObject(OWServerObject obj, Context c){
		String url = Constants.OW_URL;
		if(obj.getContentType(c) != null){
            if(obj.getContentType(c) == Constants.CONTENT_TYPE.MISSION)
                url += "missions"; // api/mission/10/ BUT /mission/10/ . Whoooops
            else
			    url += Constants.API_ENDPOINT_BY_CONTENT_TYPE.get(obj.getContentType(c));
        }else
            Log.e(TAG, String.format("Unable to determine contentType for owserverobject %d", obj.getId()));
		url += "/" + String.valueOf(obj.getServerId(c)) + "/";
		return url;
	}

    public static boolean checkEmail(String email) {
        return Constants.EMAIL_ADDRESS_PATTERN.matcher(email).matches();
    }

    public static String getPackageVersion(Context c){
        String packageVersion = "";
        try {
            PackageInfo pInfo = c.getPackageManager().getPackageInfo(
                    c.getPackageName(), 0);
            packageVersion += "I have OpenWatch version " + pInfo.versionName;
            packageVersion += " running on Android API " + String.valueOf(Build.VERSION.SDK_INT) + ".";
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("getPackageVersion", "Unable to read PackageName in RegisterApp");
            e.printStackTrace();
        }
        return packageVersion;
            //USER_AGENT += " (Android API " + Build.VERSION.RELEASE + ")";
    }

    public static int getPackageVersionAsInt(Context c){
        int packageVersion = 0;
        try {
            PackageInfo pInfo = c.getPackageManager().getPackageInfo(
                    c.getPackageName(), 0);
            packageVersion = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("getPackageVersion", "Unable to read PackageName in RegisterApp");
            e.printStackTrace();
        }
        return packageVersion;
    }

    public static void showConnectionErrorDialog(Context c){
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle("Uh oh")
                .setMessage("We were unable to reach openwatch.net. Please check your network connection and try again.")
                .setPositiveButton("Bummer", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    public static boolean isCallable(Context c, Intent intent) {
        List<ResolveInfo> list = c.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    public interface VideoViewCallback{
        public void onPlaybackComplete(ViewGroup parent);
        public void onPrepared(ViewGroup parent);
        public void onError(ViewGroup parent);
    }

    public static View.OnTouchListener videoOnClickListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            if(event.getAction() == MotionEvent.ACTION_DOWN){
                Log.i(TAG, "videoView touched");
                if( ((VideoView)v).isPlaying() ){
                    ((ViewGroup) v.getParent()).findViewById(R.id.playButton).setVisibility(View.VISIBLE);
                    ((ViewGroup) v.getParent()).findViewById(R.id.playButton).bringToFront();
                    ((VideoView)v).pause();
                }else{
                    ((VideoView)v).start();
                    ((ViewGroup) v.getParent()).findViewById(R.id.playButton).setVisibility(View.GONE);
                }
            }
            return true;
        }
    };

    /**
     * Setup a VideoView. Context c should not be an Activity Context for showing AlertDialogs
     * @param c
     * @param videoView
     * @param filepath
     */
    public static void setupVideoView(final Context c, final VideoView videoView, final String filepath, final VideoViewCallback cb, final ProgressBar progressBar) {
        final String TAG = "setupVideoView";
        if(filepath == null){
            Log.e(TAG, "setupVideoView uri is null");
            return;
        }
        if(videoView.isPlaying())
            videoView.stopPlayback();

        videoView.setVideoURI(Uri.parse(filepath));
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                if (extra == -2147483648) { // General error
                    String message = c.getString(R.string.video_error_intro);
                    boolean canPlayExternally = false;
                    //final Intent playVideoExternally = new Intent(Intent.ACTION_VIEW, Uri.parse(filepath));
                    final Intent playVideoExternally = new Intent(Intent.ACTION_VIEW);
                    playVideoExternally.setDataAndType(Uri.parse(filepath), "video/mp4");
                    if (OWUtils.isCallable(c, playVideoExternally)) {
                        canPlayExternally = true;
                        message += " " + c.getString(R.string.device_has_external_video_player);
                    } else {
                        message = " " + c.getString(R.string.device_lacks_external_video_player);
                    }
                    AlertDialog.Builder mediaErrorDialog = new AlertDialog.Builder(c)
                            .setTitle(c.getString(R.string.cannot_play_video))
                            .setMessage(message)
                            .setNegativeButton(c.getString(R.string.dialog_bummer), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    if (canPlayExternally) {
                        mediaErrorDialog.setPositiveButton(c.getString(R.string.play_video_externally), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                c.startActivity(playVideoExternally);
                            }
                        });
                    } else {
                        final Intent goToMarket = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://search?q=" + c.getString(R.string.video_player_search_query)));
                        if (OWUtils.isCallable(c, goToMarket)) {
                            mediaErrorDialog.setPositiveButton(c.getString(R.string.search_for_video_player), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    c.startActivity(goToMarket);
                                }

                            });
                        }
                    }
                    // TODO make this safe
                    //if (is_resumed)
                        mediaErrorDialog.show();
                        progressBar.setVisibility(View.GONE);
                        if(cb != null)
                            cb.onError((ViewGroup) videoView.getParent());
                }
                Log.i(TAG, String.format("what %d extra %d", what, extra));
                return true;
            }
        });
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                //showProgress(false);
                /*
                MediaController mc = new MediaController(
                        c);
                videoView.setMediaController(mc);
                mc.setAnchorView((View) videoView.getParent());
                */

                videoView.requestFocus();
                videoView.setOnTouchListener(videoOnClickListener);
                videoView.start();
                //((ViewGroup) videoView.getParent()).findViewById(R.id.videoProgress).setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                if(cb != null)
                    cb.onPrepared((ViewGroup) videoView.getParent());
                mp.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                    @Override
                    public void onVideoSizeChanged(MediaPlayer mp, int width,
                                                   int height) {
                        //video_view.setVisibility(View.VISIBLE);

                        //video_view.setLayoutParams( new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

                        videoView.requestFocus();
                        videoView.setOnTouchListener(videoOnClickListener);
                        videoView.start();
                        progressBar.setVisibility(View.GONE);
                        //((ViewGroup) videoView.getParent()).findViewById(R.id.videoProgress).setVisibility(View.GONE);
                        if(cb != null)
                            cb.onPrepared((ViewGroup) videoView.getParent());
                        //video_playing = true;
                    }
                });
            }
        });
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                //video_playing = false;
                if(cb != null)
                    cb.onPlaybackComplete((ViewGroup)videoView.getParent());

            }
        });
        //video_view.start();
        //((ViewGroup) videoView.getParent()).requestFocus();
    }

    public static void setReadingFontOnChildren(ViewGroup container){
        Typeface font = Typeface.createFromAsset(container.getContext().getAssets(), "Palatino.ttc");
        View this_view;
        for (int x = 0; x < container.getChildCount(); x++) {
            this_view = container.getChildAt(x);
            if(this_view.getTag() != null){
                if(this_view.getTag().toString().compareTo("custom_font") == 0){
                    ((TextView)this_view).setTypeface(font, Typeface.NORMAL);
                }else if(this_view.getTag().toString().compareTo("custom_font_bold") == 0){
                    ((TextView)this_view).setTypeface(font, Typeface.BOLD);
                }else if(this_view.getTag().toString().compareTo("custom_font_italic") == 0){
                    ((TextView)this_view).setTypeface(font, Typeface.ITALIC);
                }
            }
        }
    }

    /**
     * Determines if given points are inside view
     * @param x - x coordinate of point
     * @param y - y coordinate of point
     * @param view - view object to compare
     * @return true if the points are within view bounds, false otherwise
     */
    public static boolean isPointInsideView(float x, float y, View view){
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

    public static enum SOCIAL_TYPE {FB, TWITTER};

    public static void postSocial(Activity act, final SOCIAL_TYPE type, int model_id){
        switch(type){
            case FB:
                FBUtils.authenticateAndPostVideoAction((FBUtils.FaceBookSessionActivity) act, model_id);
                break;
            case TWITTER:
                TwitterUtils.tweet(act, model_id);
                TwitterUtils.authenticateAndTweet(act, model_id);
                break;
        }
    }

    public interface TakePictureCallback{
        public void gotPotentialPictureLocation(File imageLocation);
    }

    public static void setUserAvatar(final Activity act, View v, final int SELECT_PHOTO_CODE, final int TAKE_PHOTO_CODE, final TakePictureCallback cb){
        AlertDialog.Builder builder = new AlertDialog.Builder(act);

        builder.setTitle(act.getString(R.string.take_choose_picture_title))
                .setPositiveButton(act.getString(R.string.take_picture), new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        File storageDir = Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_PICTURES);
                        File image = FileUtils.prepareOutputLocation(act.getApplicationContext(), storageDir, "ow_avatar_" + String.valueOf(new Date().getTime()),".jpg");
                        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        //takePicture.putExtra("crop", "true");
                        takePicture.putExtra("outputX", 640);
                        takePicture.putExtra("outputY", 480);
                        takePicture.putExtra("aspectX", 1);
                        takePicture.putExtra("aspectY", 1);
                        takePicture.putExtra("scale", true);
                        takePicture.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(image));
                        if(cb != null)
                            cb.gotPotentialPictureLocation(image);
                        act.startActivityForResult(takePicture, TAKE_PHOTO_CODE);
                    }

                }).setNegativeButton(act.getString(R.string.choose_picture), new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                //photoPickerIntent.putExtra("crop", "true");
                photoPickerIntent.putExtra("outputX", 640);
                photoPickerIntent.putExtra("outputY", 480);
                photoPickerIntent.putExtra("aspectX", 1);
                photoPickerIntent.putExtra("aspectY", 1);
                photoPickerIntent.putExtra("scale", true);
                act.startActivityForResult(photoPickerIntent, SELECT_PHOTO_CODE);
            }

        }).show();

    }

    public static void handleSetUserAvatarResult(int requestCode, int resultCode, Intent data, ImageView imageView, Activity act, OWUser u, String photoPath){
        switch(requestCode){
            case SELECT_PHOTO:
                if(resultCode == Activity.RESULT_OK){
                    Uri selectedImageUri = data.getData();
                    //Log.i("selected_image_uri", selectedImageUri.toString());
                    String path = FileUtils.getRealPathFromURI(act.getApplicationContext(), selectedImageUri);
                    try{
                        File imageFile = new File(path);
                        OWServiceRequests.sendOWUserAvatar(act.getApplicationContext(), u, imageFile, null);
                        OWUtils.removeUriFromImageCache(selectedImageUri.toString());
                        Bitmap selectedImage = FileUtils.decodeUri(act.getApplicationContext(), selectedImageUri, 100);
                        imageView.setImageBitmap(selectedImage);
                    }catch(NullPointerException e){
                        new AlertDialog.Builder(act)
                                .setTitle(act.getString(R.string.cant_get_image))
                                .setMessage(act.getString(R.string.cant_get_image_message))
                                .setPositiveButton(act.getString(R.string.dialog_bummer), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .show();
                    }catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            case TAKE_PHOTO:
                if(resultCode == Activity.RESULT_OK){
                    String imageFilePath = photoPath;
                    if(!imageFilePath.contains("file://"))
                        imageFilePath = "file://" + imageFilePath;
                    OWUtils.removeUriFromImageCache(imageFilePath);
                    ImageLoader.getInstance().displayImage(imageFilePath, imageView);
                    OWServiceRequests.sendOWUserAvatar(act.getApplicationContext(), u, new File(photoPath), null);
                    break;
                }
        }
    }

    public static void goToLoginActivityWithMessage(Context c, String message){
        Intent i = new Intent(c, LoginActivity.class);
        i.putExtra("message", message);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(i);
    }

    public static void removeUriFromImageCache(String imageUri){
        ImageLoader imageLoader = ImageLoader.getInstance();
        File imageFile = imageLoader.getDiscCache().get(imageUri);
        if (imageFile.exists()) {
            imageFile.delete();
        }
        MemoryCacheUtil.removeFromCache(imageUri, imageLoader.getMemoryCache());
    }

    public static void checkUserStatus(Activity act){
        Context c = act.getApplicationContext();

        boolean debug_fancy = false;

        SharedPreferences profile = c.getSharedPreferences(Constants.PROFILE_PREFS, 0);
        boolean authenticated = profile.getBoolean(Constants.AUTHENTICATED, false);
        //boolean db_initialized = profile.getBoolean(Constants.DB_READY, false);
        /*
        if(!db_initialized){
            //DatabaseManager.setupDB(getApplicationContext()); // do this every time to auto handle migrations
            //DatabaseManager.testDB(this);
        }else{
            DatabaseManager.registerModels(getApplicationContext()); // ensure androrm is set to our custom Database name.
        }
        */
        DatabaseManager.registerModels(c); // ensure migrations are run.

        if(authenticated /*&& db_initialized*/ && !((OWApplication) c).per_launch_sync){
            // TODO: Attempt to login with stored credentials and report back if error

            OWMediaSyncer.syncMedia(c);
            ((OWApplication) c).per_launch_sync = true;
            // If we have a User object for the current user, and they've applied as an agent, send their current location
            if(profile.getInt(Constants.INTERNAL_USER_ID, 0) != 0){
                OWUser user = OWUser.objects(c, OWUser.class).get(profile.getInt(Constants.INTERNAL_USER_ID,0));
                if(user.agent_applicant.get() == true){
                    Log.i("MainActivity", "Sending agent location");
                    OWServiceRequests.syncOWUser(c, user, null);
                }
            }
        }
        if(debug_fancy || (!authenticated && !act.getIntent().hasExtra(Constants.AUTHENTICATED) ) ){
            Intent i = new Intent(c, FancyLoginActivity.class	);
            //Intent i = new Intent(this, LoginActivity.class	);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            String email = profile.getString(Constants.EMAIL, null);
            if(email != null)
                i.putExtra(Constants.EMAIL, email);
            act.startActivity(i);
            // This will set OWApplication.user_data
        }
        else if(authenticated){
            // If the user state is stored, load user_data into memory
            OWApplication.user_data = c.getSharedPreferences(Constants.PROFILE_PREFS, c.MODE_PRIVATE).getAll();
        }

    }

    public static void loadProfilePicture(final Context c, OWUser user, final ImageView profileImage){
        final int userId = user.getId();
        if(user == null)
            return;
        if(user.thumbnail_url.get() != null && user.thumbnail_url.get().compareTo("") != 0){
            ImageLoader.getInstance().displayImage(user.thumbnail_url.get(), profileImage);
        }

        OWServiceRequests.syncOWUser(c, user, new OWServiceRequests.RequestCallback() {
            @Override
            public void onFailure() {

            }

            @Override
            public void onSuccess() {
                String url = OWUser.objects(c, OWUser.class).get(userId).thumbnail_url.get();
                if(url != null && url.compareTo("") != 0){
                    ImageLoader.getInstance().displayImage(url, profileImage);
                }else
                    Log.i(TAG, "User has no thumbnail set");
            }
        });
    }

}
