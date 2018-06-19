package tech.iosd.benefit.Services;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.location.LocationListener;
import com.google.maps.android.SphericalUtil;

import tech.iosd.benefit.Utils.Constants;

/**
 * Created by SAM33R on 31-05-2018.
 */
public class GPSTracker extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private Context mContext;

    private double latitude, longitude;

    private ArrayList<LatLng> points;
    private ArrayList<LatLng> pointsForLastDistance;

    private boolean isPaused = false;
    ProgressDialog progressDialog;



    private static final long INTERVAL = 1000 * 10;
    private static final long FASTEST_INTERVAL = 1000 * 5;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mCurrentLocation, lStart, lEnd;
    static double distance = 0;
    private double lastDistance = 0;
    private double lastLongitude =0;
    private double lastLatitude =0;
    double speed;

    private final IBinder mBinder = new LocalBinder();



    public void stopLocationUpdates() {
        Toast.makeText(this,"stopeed",Toast.LENGTH_LONG).show();

        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
        distance = 0;

    }

    @Override
    public void onCreate(){
        super.onCreate();
        points =  new ArrayList<>();
        pointsForLastDistance =  new ArrayList<>();

    }


    public Context getmContext() {
        return mContext;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setPoints(ArrayList<LatLng> points) {
        this.points = points;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onConnected(Bundle bundle) {

        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
            Toast.makeText(this,"onconnecyed",Toast.LENGTH_LONG).show();
            if (mGoogleApiClient.isConnected()) {
                Toast.makeText(this,"GoogleApiClient  connected",Toast.LENGTH_LONG).show();


                //mGoogleApiClient.connect(); // connect it here..

            }


        } catch (SecurityException e) {
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this,connectionResult.getErrorMessage(),Toast.LENGTH_LONG).show();
        Log.d("error77", connectionResult.getErrorMessage());
    }
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;

        latitude = mCurrentLocation.getLatitude();
        longitude = mCurrentLocation.getLongitude();
        Toast.makeText(this, "Location accuracy: "+String.valueOf(location.getAccuracy()), Toast.LENGTH_SHORT).show();


        if(isPaused){
            progressDialog.hide();
        }
        if(location.getAccuracy()<20){
            progressDialog.hide();
            pointsForLastDistance.clear();
            pointsForLastDistance.add(new LatLng(latitude,longitude));
            pointsForLastDistance.add(new LatLng(lastLatitude,lastLongitude));

            double dist = SphericalUtil.computeLength(pointsForLastDistance);
            if(Math.abs(dist - lastDistance) <1){
                Toast.makeText(this, "user not moving "+ dist +"-"+ lastDistance+"="+(dist - lastDistance), Toast.LENGTH_SHORT).show();
                lastDistance = dist;
                lastLatitude =  latitude;
                lastLongitude =  longitude;

                return;
            }
            lastDistance = dist;
            lastLatitude =  latitude;
            lastLongitude =  longitude;


            if(isPaused()){
                points.add(new LatLng(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude()));

                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(Constants.GPS_UPDATE);
                sendBroadcast(broadcastIntent);
                speed = 0;
            }else {
                points.add(new LatLng(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude()));

                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(Constants.GPS_UPDATE);
                sendBroadcast(broadcastIntent);
                speed = location.getSpeed() * 18 / 5;
            }
        }else {
             progressDialog.show();
        }





    }

    public class LocalBinder extends Binder {

        public GPSTracker getService() {
            return GPSTracker.this;
        }


    }


    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
        alertDialog.setTitle("GPS is settings");
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();

            }
        });
        alertDialog.show();

    }
    public ArrayList getPoints(){
        return points;
    }
    public void stoptacking(){
        points.clear();
        isPaused = true;
    }

    public void setmContext(Context mContext) {

        this.mContext = mContext;
        progressDialog = new ProgressDialog(mContext);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("move to a place with higher GPS accuracy\nPlease use this feature outside.");
        //progressDialog.show();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if(mLocationRequest != null){
            Toast.makeText(this,"connected gps",Toast.LENGTH_LONG).show();
           // latitude = mCurrentLocation.getLatitude();
        //    longitude = mCurrentLocation.getLongitude();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        createLocationRequest();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this,"GoogleApiClient not yet connected",Toast.LENGTH_LONG).show();

            mGoogleApiClient.connect(); // connect it here..

        } else {
            //og.e(TAG,"GoogleApiClient connected");

        }
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopLocationUpdates();

        //if (mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
        lStart = null;
        lEnd = null;
        distance = 0;

        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}

