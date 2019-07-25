package com.parse.starter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.List;

public class DestiantionActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    LocationManager locationManager;
    LocationListener locationListener;
    Button calluberButton;

    Boolean requestActive = false;

    Handler handler = new Handler();
    TextView infoTextView ;

    public void checkForUpdats(){

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Request");
        query.whereEqualTo("username",ParseUser.getCurrentUser().getUsername());
        query.whereExists("driverUsername");

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {

                if (e == null && objects.size() > 0) {

                    ParseQuery<ParseUser> query = ParseUser.getQuery();

                    query.whereEqualTo("username", objects.get(0).getString("driverUsername"));

                    query.findInBackground(new FindCallback<ParseUser>() {
                        @Override
                        public void done(List<ParseUser> objects, ParseException e) {
                            if (e == null && objects.size() >0){

                                ParseGeoPoint driverLocation = objects.get(0).getParseGeoPoint("Location");

                                if (Build.VERSION.SDK_INT < 23 || ContextCompat.checkSelfPermission(DestiantionActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                                    if (lastKnownLocation != null){

                                        ParseGeoPoint userLocation = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());



                                    }




                                }


                            }
                        }
                    });

                    infoTextView.setText("Your driver is on the way");

                    calluberButton.setVisibility(View.INVISIBLE);

                }
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        checkForUpdats();


                    }
                }, 2000);


            }
        });




    }



    public void logout (View view){

        ParseUser.logOut();

        Intent intent = new Intent (getApplicationContext(), MainActivity.class);

        startActivity(intent);

    }

    public void callUber (View view){

        Log.i("Info","call");



        if (requestActive){

            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");

            query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());

            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e==null){

                        if (objects.size() > 0){

                            for (ParseObject object : objects){

                                object.deleteInBackground();


                            }

                            requestActive =false;
                            calluberButton.setText("CALL AN UBER");

                        }


                    }

                }
            });







        } else {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (lastKnownLocation != null) {

                    ParseObject request = new ParseObject("Request");

                    request.put("username", ParseUser.getCurrentUser().getUsername());

                    ParseGeoPoint parseGeoPoint = new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

                    request.put("Location", parseGeoPoint);

                    request.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {

                            if (e == null) {

                                calluberButton.setText("CANCEL UBER");
                                requestActive= true;

                               checkForUpdats();

                            }


                        }
                    });


                } else {

                    Toast.makeText(this, "Could not find location. Pleast try again later", Toast.LENGTH_SHORT).show();

                }


            }
        }



    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1 ){

            if (grantResults.length> 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);

                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    updateMap(lastKnownLocation);
                }

            }

        }
    }

    public void updateMap(Location location) {
        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.clear();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,15));
        mMap.addMarker(new MarkerOptions().position(userLocation).title("Your Location"));

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        calluberButton = (Button) findViewById(R.id.callUberButton);
        infoTextView = (TextView) findViewById(R.id.infoTextView);

        ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");

        query.whereEqualTo("username", ParseUser.getCurrentUser().getUsername());

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e==null){

                    if (objects.size() > 0){

                        requestActive =true;
                        calluberButton.setText("CANCEL UBER");

                        checkForUpdats();

                    }


                }

            }
        });



    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                LatLng userLocation = new LatLng(location.getLatitude(),location.getLongitude());

                updateMap(location);

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        if (Build.VERSION.SDK_INT < 23){
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);

        } else {
         if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){

             ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);


         } else {

             locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);

             Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

             if (lastKnownLocation != null){

                 updateMap(lastKnownLocation);

             }


         }


        }


        }



}

