package net.openwatch.reporter.location;

import java.util.Timer;
import java.util.TimerTask;

import net.openwatch.reporter.http.OWMediaRequests;
import net.openwatch.reporter.model.OWLocalRecording;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

//	Thanks, Fedor!
public class DeviceLocation {
    Timer timer1;
    LocationManager lm;
    LocationResult locationResult;
    boolean gps_enabled=false;
    boolean network_enabled=false;
    private static final String TAG = "DeviceLocation";
    private Location bestLocation;
    private boolean waitForGpsFix;
    
    private static final float ACCURATE_LOCATION_THRESHOLD_METERS = 20;
    
    /**
     * 
     * @param context
     * @param result
     * @param waitForGpsFix even if a network location is gotten, wait for a gps fix
     * @return
     */
    public boolean getLocation(Context context, LocationResult result, boolean waitForGpsFix)
    {
    	this.waitForGpsFix = waitForGpsFix;
    	
        //I use LocationResult callback class to pass location value from MyLocation to user code.
        locationResult=result;
        if(lm==null)
            lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        //exceptions will be thrown if provider is not permitted.
        try{gps_enabled=lm.isProviderEnabled(LocationManager.GPS_PROVIDER);}catch(Exception ex){}
        try{network_enabled=lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);}catch(Exception ex){}

        //don't start listeners if no provider is enabled
        if(!gps_enabled && !network_enabled)
            return false;

        if(gps_enabled)
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListenerGps);
        if(network_enabled)
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListenerNetwork);
        timer1=new Timer();
        timer1.schedule(new GetBestLocation(), 20000);
        return true;
    }

    LocationListener locationListenerGps = new LocationListener() {
        public void onLocationChanged(Location location) {
        	Log.i(TAG, "got GPS loc accurate to " + String.valueOf(location.getAccuracy()) + "m");
        	if(bestLocation == null || bestLocation.getAccuracy() > location.getAccuracy())
            	bestLocation = location;
        	
        	if(!waitForGpsFix || bestLocation.getAccuracy() < ACCURATE_LOCATION_THRESHOLD_METERS){
        		timer1.cancel();
                locationResult.gotLocation(bestLocation);
                lm.removeUpdates(this);
                lm.removeUpdates(locationListenerNetwork);
        	}
            
        }
        public void onProviderDisabled(String provider) {}
        public void onProviderEnabled(String provider) {}
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    LocationListener locationListenerNetwork = new LocationListener() {
        public void onLocationChanged(Location location) {
        	Log.i(TAG, "got network loc accurate to " + String.valueOf(location.getAccuracy()) + "m");
        	if(bestLocation == null || bestLocation.getAccuracy() > location.getAccuracy())
            	bestLocation = location;
        	
        	if(!waitForGpsFix || bestLocation.getAccuracy() < ACCURATE_LOCATION_THRESHOLD_METERS){
        		timer1.cancel();
                locationResult.gotLocation(bestLocation);
                lm.removeUpdates(this);
                lm.removeUpdates(locationListenerGps);
        	}
      
        }
        public void onProviderDisabled(String provider) {}
        public void onProviderEnabled(String provider) {}
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    class GetBestLocation extends TimerTask {
        @Override
        public void run() {
        	Log.i(TAG, "Timer expired before adequate location acquired");
             lm.removeUpdates(locationListenerGps);
             lm.removeUpdates(locationListenerNetwork);

             Location net_loc=null, gps_loc=null;
             if(gps_enabled)
                 gps_loc=lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
             if(network_enabled)
                 net_loc=lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

             //if there are both values use the latest one
             if(gps_loc!=null && net_loc!=null){
                 if(gps_loc.getTime()>net_loc.getTime())
                     locationResult.gotLocation(gps_loc);
                 else
                     locationResult.gotLocation(net_loc);
                 return;
             }

             if(gps_loc!=null){
                 locationResult.gotLocation(gps_loc);
                 return;
             }
             if(net_loc!=null){
                 locationResult.gotLocation(net_loc);
                 return;
             }
             locationResult.gotLocation(null);
        }
    }

    public static abstract class LocationResult{
        public abstract void gotLocation(Location location);
    }
    
    /**
     * Specific method for interacting with DeviceLocation
     * in the context of media start and stop signals
     * @param app_context
     * @param recording_db_id
     * @param isStart
     */
    public static void setRecordingLocation(final Context app_context, final String upload_token, final int recording_db_id, final boolean isStart){
		DeviceLocation deviceLocation = new DeviceLocation();
		
        LocationResult locationResult = new LocationResult(){
            @Override
            public void gotLocation(final Location location){
                //Got the location!
                OWLocalRecording recording = OWLocalRecording.objects(app_context, OWLocalRecording.class).get(recording_db_id);
	                if (location != null) {
	                	Log.i(TAG, "gotLocation");
	                	if(isStart){
		                	recording.begin_lat.set(location.getLatitude());
		                	recording.begin_lon.set(location.getLongitude());
	                	}else{
	                		recording.end_lat.set(location.getLatitude());
	                		recording.end_lon.set(location.getLongitude());
	                	}
	                	recording.save(app_context);
	                   Log.d("RefreshLocation"," accuracy: "+ String.valueOf(location.getAccuracy())+" meters");
	                   // If this was the end signal, check to see if 
	                   // server_id has been retrieved. If so, send another updateMetadata
	                   // request to ensure geo data available
	                   if(!isStart){
	                	   OWMediaRequests.updateMeta(upload_token, recording);
	                   }
	                }
                };
            };
       Log.i(TAG, "getLocation()...");
       // For start, get location as quickly as possible
       // For end, wait for additional accuracy
       deviceLocation.getLocation(app_context, locationResult, !isStart);
	}
}