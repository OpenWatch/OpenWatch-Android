package net.openwatch.reporter;

import net.openwatch.reporter.model.OWMediaObject;
import net.openwatch.reporter.model.OWVideoRecording;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapFragment extends SupportMapFragment implements OWMediaObjectBackedEntity{
	private static final String TAG = "MapFragment";
	
    private GoogleMap mMap;
    private LatLng mStartLocation;
    private LatLng mStopLocation;

    public MapFragment() {
        super();
    }

    public static MapFragment newInstance(LatLng start, LatLng stop) {
        MapFragment frag = new MapFragment();
        frag.mStartLocation = start;
        frag.mStopLocation = stop;
        return frag;
    }

    @Override
    public GoogleMap getMap() {
        // TODO Auto-generated method stub
        return super.getMap();
    }

    @Override
    public void onCreate(Bundle arg0) {
        // TODO Auto-generated method stub
        super.onCreate(arg0);
    }
    /*
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup view, Bundle savedInstance) {
    View layout = super.onCreateView(inflater, view, savedInstance);

    FrameLayout frameLayout = new FrameLayout(getActivity());
    frameLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
    ((ViewGroup) layout).addView(frameLayout,
        new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
    return layout;
}
*/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if(RecordingViewActivity.model_id != -1){
        	OWVideoRecording recording = OWVideoRecording.objects(getActivity().getApplicationContext(), OWVideoRecording.class)
    				.get(RecordingViewActivity.model_id);
        	
        	if(recording != null)
        		mapRecording(recording);
        	
        }
        // Hack to fix MapFragment causing drawing errors
        // see http://stackoverflow.com/questions/13837697/viewpager-with-google-maps-api-v2-mysterious-black-view/13910364#13910364
        FrameLayout frameLayout = new FrameLayout(getActivity());
        frameLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        ((ViewGroup) view).addView(frameLayout,
            new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        
        return view;
    }
    
    private void mapRecording(OWVideoRecording recording){
    	if(recording.begin_lat.get() != null && recording.end_lat.get() != null)
    		Log.i(TAG, "recording begin point: " + String.valueOf(recording.begin_lat.get()) + ", " + String.valueOf(recording.begin_lon.get()));
    	if(recording.end_lat.get() != null && recording.end_lat.get() != null)
    		Log.i(TAG, "recording end point: " + String.valueOf(recording.end_lat.get()) + ", " + String.valueOf(recording.end_lon.get()));
    	mStartLocation = new LatLng(recording.begin_lat.get(), recording.begin_lon.get());
    	mStopLocation = new LatLng(recording.end_lat.get(), recording.end_lon.get());
    	initMap();
    }

    private void initMap() {
    	if(this.getMap() == null)
    		return;
    	
        UiSettings settings = getMap().getUiSettings();
        //settings.setAllGesturesEnabled(false);
        settings.setMyLocationButtonEnabled(false);

        getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(mStartLocation, 10));
        getMap().clear();
        getMap().addMarker(
                new MarkerOptions().position(mStartLocation)
                        .icon(BitmapDescriptorFactory
                                .fromResource(R.drawable.marker_start)));
        getMap().addMarker(
                new MarkerOptions().position(mStopLocation)
                        .icon(BitmapDescriptorFactory
                                .fromResource(R.drawable.marker_stop)));
        getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(mStartLocation, 18));
    }

	@Override
	public void populateViews(OWMediaObject media_object, Context app_context) {
		mapRecording(media_object.video_recording.get(app_context));
	}

}