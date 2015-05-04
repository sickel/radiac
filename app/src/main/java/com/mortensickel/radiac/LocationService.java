package com.mortensickel.radiac;
// http://stackoverflow.com/questions/7759504/access-locationmanager-locationlistener-from-class
import java.util.Timer;
import java.util.TimerTask;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.app.Service;
import android.os.IBinder;

public class LocationService extends Service implements LocationListener {

    private LocationManager locationManager;

    private Location location;
	private IBinder lBinder;
    private final IBinder mBinder = new LocalBinder();
	private static String LOGTAG="Locservice";
	private double lat;
	private double lon;
	private int n;
	
	
    public class LocalBinder extends Binder {
        LocationService getService(){
            return LocationService.this;
        }
    }



    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
		if(intent!=null){
            if (intent.getAction().equals("startListening")) {
                locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                Log.i(LOGTAG,"Locationservice started");
            }
            else {
                if (intent.getAction().equals("stopListening")) {
                    locationManager.removeUpdates(this);
                    locationManager = null;
                    Log.i(LOGTAG,"Locationservice stopped");
                }
            }
        }
		lat=0;
		lon=0;
		n=0;
        return START_STICKY;

    }

	@Override
	public boolean onUnbind(Intent intent)
	{
		// TODO: Implement this method
		return super.onUnbind(intent);
	}

	
	
	
    @Override
    public IBinder onBind(final Intent   intent) {
        return mBinder;
    }

    public void onLocationChanged(final Location location) {
        this.location = location;   
        // TODO this is where you'd do something like context.sendBroadcast()
        Log.i(LOGTAG,location.toString());
		n++;
		lat=lat+location.getLatitude();
		lon=lon+location.getLongitude();
    }

    public Location getLocation(){
        return this.location;
    }

    public void onProviderDisabled(final String provider) {
    }

    public void onProviderEnabled(final String provider) {
    }

    public void onStatusChanged(final String arg0, final int arg1, final Bundle arg2) {

    }

	}
	
