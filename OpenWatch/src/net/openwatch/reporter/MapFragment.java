package net.openwatch.reporter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapFragment extends SupportMapFragment {
    private GoogleMap mMap;
    private LatLng mLocation;

    public MapFragment() {
        super();
    }

    public static MapFragment newInstance(LatLng posicion) {
        MapFragment frag = new MapFragment();
        frag.mLocation = posicion;
        return frag;
    }
    
    public void centerOnLocation(LatLng location){
    	mLocation = location;
    	initMap();
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if(LocalRecordingViewActivity.recording != null){
        	mLocation = new LatLng(LocalRecordingViewActivity.recording.begin_lat.get(), LocalRecordingViewActivity.recording.end_lat.get());
        	initMap();
        }
        return view;
    }

    private void initMap() {
        UiSettings settings = getMap().getUiSettings();
        //settings.setAllGesturesEnabled(false);
        settings.setMyLocationButtonEnabled(false);

        getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(mLocation, 16));
        getMap().addMarker(
                new MarkerOptions().position(mLocation)
                        .icon(BitmapDescriptorFactory
                                .fromResource(R.drawable.marker)));
    }

}