package org.ale.openwatch;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.orm.androrm.Filter;
import com.orm.androrm.Model;
import org.ale.openwatch.constants.Constants;
import org.ale.openwatch.http.OWServiceRequests;
import org.ale.openwatch.model.OWPhoto;
import org.ale.openwatch.model.OWServerObject;
import org.ale.openwatch.model.OWServerObjectInterface;
import org.ale.openwatch.model.OWVideoRecording;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by davidbrodsky on 6/9/13.
 */
public class DashboardActivity extends SherlockFragmentActivity {
    private static final String TAG = "DashboardActivity";

    TextView titleLabel;
    TextView userLabel;
    ImageView userThumb;

    GoogleMap map;
    Context c;

    ImageView imageView;
    VideoView videoView;

    ArrayList<OWPhoto> photos;
    ArrayList<OWVideoRecording> videos;

    boolean isResumed;
    boolean tick = false;
    boolean demoRunning = false;

    Timer timer;

    boolean isLocal;

    OWServiceRequests.PaginatedRequestCallback cb = new OWServiceRequests.PaginatedRequestCallback(){

        @Override
        public void onSuccess(int page, int object_count, int total_pages) {
            beginDemo();
        }

        @Override
        public void onFailure(int page) {}

    };


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        requestWindowFeature(com.actionbarsherlock.view.Window.FEATURE_NO_TITLE);
        this.setContentView(R.layout.activity_dashboard);
        titleLabel = (TextView) findViewById(R.id.titleLabel);
        userLabel = (TextView) findViewById(R.id.userLabel);
        userThumb = (ImageView) findViewById(R.id.userThumbnail);
        imageView = (ImageView) findViewById(R.id.imageView);
        videoView = (VideoView) findViewById(R.id.videoView);
        videoView.setZOrderOnTop(true);
        c = getApplicationContext();
        timer = new Timer();
    }

    @Override
    public void onResume(){
        super.onResume();

        OWServiceRequests.getFeed(this.getApplicationContext(), "raw", 1, cb);
    }

    private void beginDemo(){
        if(demoRunning)
            return;
        demoRunning = true;
        map = ((SupportMapFragment) this.getSupportFragmentManager().findFragmentById(R.id.mapFragment)).getMap()
                ;
        Filter photoFilter = new Filter();
        photoFilter.is("lat", "!=", 0);
        photos = (ArrayList<OWPhoto>) OWPhoto.objects(this, OWPhoto.class).filter(photoFilter).toList();
        for(OWPhoto photo : photos){
            mapObject(photo);
        }
        Filter videoFilter = new Filter();
        videoFilter.is("end_lat", "!=", 0);
        videos = (ArrayList<OWVideoRecording>) OWVideoRecording.objects(this, OWVideoRecording.class).filter(videoFilter).toList();
        for(OWVideoRecording video: videos){
            mapObject(video);
        }
        processNextObject();

    }

    private void processNextObject(){
        OWServerObjectInterface object = getNextObject();
        if(object != null){
            Log.i(TAG, String.format("processing %s, with id %d", object.getMediaType(c), ((Model)object).getId()));
            animateToObject(object);
            populateMeta(object);
        }else{
            Log.e(TAG, "getNextObject null. demo finished");
            demoRunning = false;
        }
    }

    private OWServerObjectInterface getNextObject(){
        tick = !tick;
        if(tick){
            if(photos.size() > 0){
               return photos.remove(0);
            }
        }else{
            if(videos.size() > 0){
                return videos.remove(0);
            }
        }
        return null;
    }

    private void animateToObject(final OWServerObjectInterface object){
        LatLng latLng = new LatLng(object.getLat(c), object.getLon(c));
        Log.i(TAG, String.format("animating %s, with id %d to %f, %f", object.getMediaType(c), ((Model)object).getId(), latLng.latitude, latLng.longitude));
        map.addMarker(
                new MarkerOptions().position(latLng)
                        .icon(BitmapDescriptorFactory
                                .fromResource(R.drawable.marker_stop)));
        GoogleMap.CancelableCallback cb = new GoogleMap.CancelableCallback(){

            @Override
            public void onFinish() {
                if(object.getMediaType(c) == Constants.MEDIA_TYPE.VIDEO){
                    Log.i(TAG, String.format("animation complete. loading media for %s, with id %d", object.getMediaType(c), ((Model)object).getId()));
                    setupMediaViewForOWServerObject( ((OWVideoRecording)object).media_object.get(c) );

                }
                else if(object.getMediaType(c) == Constants.MEDIA_TYPE.PHOTO){
                    Log.i(TAG, String.format("animation complete. loading media for %s, with id %d", object.getMediaType(c), ((Model)object).getId()));
                    setupMediaViewForOWServerObject( ((OWPhoto)object).media_object.get(c) );

                }
            }

            @Override
            public void onCancel() {
                Log.i(TAG, "animation cancelled");
            }
        };

        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16), 6*1000, cb);

    }

    private void mapObject(final OWServerObjectInterface object){
        LatLng latLng = new LatLng(object.getLat(c), object.getLon(c));
        map.addMarker(
                new MarkerOptions().position(latLng)
                        .icon(BitmapDescriptorFactory
                                .fromResource(R.drawable.marker_stop)));
    }

    private void populateMeta(OWServerObjectInterface object){
        if(object.getUser(c) != null){
            ImageLoader.getInstance().displayImage(object.getUser(c).thumbnail_url.get(), userThumb);
            userLabel.setText(object.getUser(c).username.get());
        }
        titleLabel.setText(object.getTitle(c));
    }

    private class DashTimerTask extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "DashTimerTask run");
                    //findViewById(R.id.mapContainer).setVisibility(View.VISIBLE);
                    videoView.setVisibility(View.GONE);
                    imageView.setVisibility(View.GONE);
                    processNextObject();
                }
            });
        }
    }

    public void setupMediaViewForOWServerObject(OWServerObject object){
        String media_path = "";
        //Log.i(TAG, String.format("setupMediaView. lat:%f, lon:%f", object.getLat(getApplicationContext()), object.getLon(getApplicationContext())));
        switch(object.getMediaType(getApplicationContext())){
            case VIDEO:
                /*
                if( object.local_video_recording.get(getApplicationContext()) != null ){
                    // This is a local recording, attempt to play HQ file
                    isLocal = true;
                    media_path = object.local_video_recording.get(getApplicationContext()).hq_filepath.get();

                } else if( object.video_recording.get(getApplicationContext()) != null && object.video_recording.get(getApplicationContext()).media_url.get() != null){
                    isLocal = false;
                    showProgress(true);
                    // remote recording, and video_url present
                    media_path = object.video_recording.get(getApplicationContext()).media_url.get();
                }
                Log.i(TAG, String.format("setupMediaView. media_url: %s", media_path));
                this.setupVideoView(media_path);
                break;
                */
                Log.i(TAG, String.format("Setting up imageView for video %d thumbnail with path %s", ((Model)object).getId(), object.thumbnail_url.get()));
                this.setupImageView(object.thumbnail_url.get());
            case AUDIO:
                /*
                media_path = object.audio.get(getApplicationContext()).getMediaFilepath(getApplicationContext());
                if(media_path == null || media_path.compareTo("") == 0){
                    media_path = object.audio.get(getApplicationContext()).media_url.get();
                    isLocal = false;
                    showProgress(true);
                }else
                    isLocal = true;
                Log.i(TAG, String.format("setupMediaView. media_url: %s", media_path));
                this.setupVideoView(media_path);
                break;
                */
                Log.i(TAG, String.format("skipping media view for %s, with id %d", object.getMediaType(c), ((Model)object).getId()));
                timer.schedule(new DashTimerTask(), 5*1000);
            case PHOTO:
                media_path = object.photo.get(getApplicationContext()).getMediaFilepath(getApplicationContext());
                if(media_path == null || media_path.compareTo("") == 0){
                    media_path = object.photo.get(getApplicationContext()).media_url.get();
                    isLocal = false;
                    showProgress(true);
                } else
                    isLocal = true;
                Log.i(TAG, String.format("animation complete. loading media for %s, with id %d: path: %s", object.getMediaType(c), ((Model)object).getId(), media_path));
                this.setupImageView(media_path);
                break;
        }

    }

    private void showProgress(boolean doShow){

    }

    public void setupImageView(String uri){
        if(uri == null){
            //Log.e(TAG, "setupImageView uri is null");
            showProgress(false);
            imageView.setImageResource(R.drawable.thumbnail_placeholder);
            timer.schedule(new DashTimerTask(), 5*1000);
            Log.i(TAG, "setupImageView passed null uri, scheduling next task");
            return;
        }
        if(isLocal && !uri.contains("file:\\/\\/"))
            uri = "file://" + uri;
        final String absolute_uri = uri;

        ImageSize size = new ImageSize(1920, 1080);
        Log.i("setupImageView", String.format("ImageView dimen: %d x %d ", size.getWidth(), size.getHeight()));
        DisplayImageOptions options = new DisplayImageOptions.Builder().showStubImage(R.drawable.blank).build();
        ImageLoader.getInstance().displayImage(absolute_uri,imageView,options, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {

            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                showProgress(false);
                Log.i(TAG, "setupImageView loading failed");
                ((ImageView)view).setImageResource(R.drawable.thumbnail_placeholder);
                timer.schedule(new DashTimerTask(), 5*1000);
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                Log.i("setupImageView", "onLoadingComplete");
                showProgress(false);
                Log.i(TAG, "setupImageView loading complete");
                imageView.setVisibility(View.VISIBLE);
                timer.schedule(new DashTimerTask(), 5*1000);
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {

            }
        } );



    }

    public void setupVideoView(final String filepath) {
        if(filepath == null || filepath.compareTo("") == 0){
            Log.e(TAG, "setupVideoView uri is null");
            timer.schedule(new DashTimerTask(), 5*1000);
            return;
        }

        videoView.setVideoURI(Uri.parse(filepath));
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                if (extra == -2147483648) { // General error
                    timer.schedule(new DashTimerTask(), 5*1000);
                }
                Log.i("VideoView error", String.format("what %d extra %d", what, extra));
                return true;
            }
        });
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.i(TAG, "videoView onPrepared");
                showProgress(false);
                videoView.setVisibility(View.VISIBLE);
                MediaController mc = new MediaController(
                        DashboardActivity.this);
                videoView.setMediaController(mc);
                mc.setAnchorView(videoView);
                videoView.requestFocus();
                videoView.start();
                findViewById(R.id.mapContainer).setVisibility(View.INVISIBLE);
                mp.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                    @Override
                    public void onVideoSizeChanged(MediaPlayer mp, int width,
                                                   int height) {
                        Log.i(TAG, "videoView onVideoSizeChanged");
                        VideoView video_view = (VideoView) findViewById(R.id.media_object_media_view);
                        //video_view.setVisibility(View.VISIBLE);

                        //video_view.setLayoutParams( new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

                        MediaController mc = new MediaController(
                                DashboardActivity.this);
                        video_view.setMediaController(mc);
                        mc.setAnchorView(video_view);
                        video_view.requestFocus();
                        video_view.start();
                        // video_playing = true;
                    }
                });
            }
        });
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                timer.schedule(new DashTimerTask(), 5*1000);
                // timer task

                //video_playing = false;
            }
        });
        //video_view.start();
    }


}