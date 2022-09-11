package com.example.roadmapfinal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;


import com.example.roadmapfinal.Model.User;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {
    private static final String TAG = "MapsActivity";
    private GoogleMap mMap;
    private GeofencingClient geofencingClient;
    private Geofencehelper geofencehelper;
    private String GEOFENCE_ID = "SCHOOL";
    private int FINE_LOCATION_CODE = 10001;
    private float GEOFENCE_RADIUS = 250;
DatabaseReference databaseReference;
    private DatabaseReference mDatabase;
    MediaPlayer player;
    TextView tv_speed;
    TextView tv_dynamic_max_speed;
    TextView tv_location;
    static int speedLimit=40;
    static  int totalUserPoints =0;
    static List<Object> locationsList = new ArrayList<>();
    static String uid;


    boolean[] hasAccident = new boolean[]{false,true,false};
    int[] popularityInArea = new int[]{40,60,20};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
      uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        if(uid!=null)
        {
            getUserData(uid);
            //Toast.makeText(geofencehelper, ""+locationsList.get(0).toString(), Toast.LENGTH_SHORT).show();
        }


        geofencingClient = LocationServices.getGeofencingClient(this);
        geofencehelper = new Geofencehelper(this);


        tv_speed = findViewById(R.id.tv_speed);
        tv_location=findViewById(R.id.tv_address);
        tv_dynamic_max_speed=findViewById(R.id.tv_dynamic_max_speed);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        } else {

            doStuff();
        }


    }

    public void getUserData(String uid)
    {
        databaseReference.child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                User user = snapshot.getValue(User.class);
                totalUserPoints= user.points;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // calling on cancelled method when we receive
                // any error or we are not able to get the data.
                Toast.makeText(MapsActivity.this, "Fail to get data.", Toast.LENGTH_SHORT).show();
            }
        });




    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        LatLng school = new LatLng(22.2910942, 73.2365132);

        addCircle(school, GEOFENCE_RADIUS);
        addGeofence(school, GEOFENCE_RADIUS);
        enableUserlocation();
    }

    public static double getDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(Math.abs(lat2 - lat1));
        double lonDistance = Math.toRadians(Math.abs(lon2 - lon1));

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = R * c * 1000; // distance in meter

        distance = Math.pow(distance, 2);
        return Math.sqrt(distance);
    }

    private boolean isInSchool(double x,double y){

        // the lat and long of : Crystal School Waghodia Road
        final double X = 22.2910942;
        final double Y = 73.2365132;

        // radius up to 250 m is checked
        if(getDistance(X,Y,x,y) <= GEOFENCE_RADIUS){
            return true;
        }
        else{
            return false;
        }
    }









    @Override
    public void onLocationChanged(Location location) {
       int FASTEST_INTERVAL = 60000; // use whatever suits you
         Location currentLocation = null;
         long locationUpdatedAt = Long.MIN_VALUE;
        boolean updateLocationandReport = false;
        if(currentLocation == null){
            currentLocation = location;
            locationUpdatedAt = System.currentTimeMillis();
            updateLocationandReport = true;
        } else {
            long secondsElapsed = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - locationUpdatedAt);
            if (secondsElapsed >= TimeUnit.MILLISECONDS.toSeconds(FASTEST_INTERVAL)){
                // check location accuracy here
                currentLocation = location;
                locationUpdatedAt = System.currentTimeMillis();
                updateLocationandReport = true;
            }
        }
        if(updateLocationandReport){
            //  send your location to server

            //checkDistance
            //Toast.makeText(geofencehelper, "Lat : "+location.getLatitude()+"\n Long : "+location.getLongitude(), Toast.LENGTH_LONG).show();

            showCurrentLocation(location.getLatitude(),location.getLongitude());
            if (location == null) {

                tv_speed.setText("Current Speed : 0 km/h");
            } else {
                //int speed=(int) ((location.getSpeed()) is the standard which returns meters per second.
                // In this example i converted it to kilometers per hour

                int speed = (int) ((location.getSpeed() * 3600) / 1000);

                tv_speed.setText("Current Speed : "+speed + " km/h");
                tv_dynamic_max_speed.setText("Dynamic Max Speed : "+speedLimit+" km/h");


                Double latitude = location.getLatitude();
                Double longitude = location.getLongitude();
                boolean playerStart=false;

                if (speed > speedLimit )
                {
                    Toast.makeText(this, " Please Slow Down...", Toast.LENGTH_LONG).show();
                    player = MediaPlayer.create(MapsActivity.this, R.raw.slowdown);
                    if(!playerStart)
                    {
                        player.start();
                        playerStart=true;
                        decreasePoints(uid);
                    }

                }
                else
                {
                    if(speed>speedLimit-10)
                        increasePoints(uid);
                    playerStart=false;
                }


                getLocationData(latitude,longitude);


            }
        }




    }





    void getLocationData(Double latitude,Double longitude)
    {
        DatabaseReference locationsReference = FirebaseDatabase.getInstance().getReference("locations");

        locationsReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                for(int i = 0; i<snapshot.getChildrenCount();i++){
                    double locationLat = (double) snapshot.child(i+"").child("coordinates").child("lat").getValue();
                    double locationLong = (double) snapshot.child(i+"").child("coordinates").child("lng").getValue();
                    //.
                    // getCurrentBusiest(snapshot.child(i+"").child("populartimes").getValue());
                    if(checkDistance(latitude,longitude,locationLat,locationLong))
                    {
                        Toast.makeText(geofencehelper, "We are in \\r\\n"+snapshot.child(i+"").child("address").getValue()+"\\r\\n and this is a busiest area please drive carefully", Toast.LENGTH_SHORT).show();
                       speedLimit = changingTheDynamicSpeed(i);
                        tv_dynamic_max_speed.setText("Dynamic Max Speed : "+20+" km/h" );

                    }


                }
            }
            @Override
            public void onCancelled(DatabaseError firebaseError) {
                Log.e("The read failed: " ,firebaseError.getMessage());
            }
        });


    }



    int changingTheDynamicSpeed(int i)
    {
        int newSpeed;
        newSpeed = speedLimit - speedLimit%((popularityInArea[i]/4));
        if(hasAccident[i]&&newSpeed>=30)
            newSpeed-=20;


        speedLimit = newSpeed;
        return speedLimit;
    }

    void decreasePoints(String uid)
    {
        mDatabase.child("users").child(uid).child("points").setValue(totalUserPoints-2).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });


        new CountDownTimer(30000, 1000) {

            public void onTick(long millisUntilFinished) {

                // logic to set the EditText could go here
            }

            public void onFinish() {
                Toast.makeText(geofencehelper, "Points have been deducted :( Please drive safe", Toast.LENGTH_SHORT).show();
            }

        }.start();
    }

    void increasePoints(String uid)
    {
        mDatabase.child("users").child(uid).child("points").setValue(totalUserPoints+5).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
        new CountDownTimer(30000, 1000) {

            public void onTick(long millisUntilFinished) {

                // logic to set the EditText could go here
            }

            public void onFinish() {
                Toast.makeText(geofencehelper, "Points have been added!!!", Toast.LENGTH_SHORT).show();
            }

        }.start();
    }


    @Override
    public void onProviderDisabled(String s) {

    }

    @SuppressLint("MissingPermission")
    private void doStuff() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }
        Toast.makeText(this, "Waiting for GPS connection", Toast.LENGTH_SHORT).show();
    }


    private void enableUserlocation() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {

            // Ask location
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_CODE);

            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == FINE_LOCATION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mMap.setMyLocationEnabled(true);

            } else {
                // we don't have permission
            }
        }
    }

    private void addGeofence(LatLng school, float radius) {

        Geofence geofence = geofencehelper.getGeofence(GEOFENCE_ID, school, radius, Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT);
        GeofencingRequest geofencingRequest = geofencehelper.getGeofencingRequest(geofence);
        PendingIntent pendingIntent = geofencehelper.getPendingIntent();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "OnSuccess: Geofence_Added");

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        String errorMessage = geofencehelper.getErrorString(e);
                        Log.d(TAG, "OnFail: " + errorMessage);

                    }
                });


    }

    private void addCircle(LatLng school, float radius) {

        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(school);
        circleOptions.radius(radius);
        circleOptions.strokeColor(Color.argb(255, 255, 0, 0));
        circleOptions.fillColor(Color.argb(64, 255, 0, 0));
        circleOptions.strokeWidth(4);
        mMap.addCircle(circleOptions);
    }

    private void showCurrentLocation(Double latitude, Double longitude)
    {
        try{
            Geocoder geo = new Geocoder(MapsActivity.this.getApplicationContext(), Locale.getDefault());
            List<Address> addresses = geo.getFromLocation(latitude, longitude, 1);
            if (addresses.isEmpty()) {
                Toast.makeText(MapsActivity.this, "Waiting for the location", Toast.LENGTH_SHORT).show();
            }
            else {
                if (addresses.size() > 0) {
                    tv_location.setText("Location : "+addresses.get(0).getAddressLine(0).toString());
                    //Toast.makeText(MapsActivity.this, addresses.get(0).getAddressLine(0), Toast.LENGTH_SHORT).show();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    private boolean checkDistance(double currentLocLat, double currentLocLong, double placeLocLat, double placeLocLong) {
        float radius =  500F;
        float[] results = new float[3];
        Location.distanceBetween(currentLocLat, currentLocLong, placeLocLat, placeLocLong, results);
        return (results[0] <= radius);
    }



























    public class GeofenceSampleReceiver extends BroadcastReceiver {

        private static final String TAG = "GeofenceBroadcastReceiver";
        @SuppressLint("LongLogTag")
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO: This method is called when the BroadcastReceiver is receiving
            // an Intent broadcast.
            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

            if (geofencingEvent.hasError()) {
                Log.d(TAG, "onReceive: Error reciving Geofence");
                return;
            }

         List<Geofence> geofenceList = geofencingEvent.getTriggeringGeofences();
            for (Geofence geofence : geofenceList) {
                Log.d(TAG, "onReceive:" + geofence.getRequestId());
            }

            int transistionType = geofencingEvent.getGeofenceTransition();

            switch (transistionType) {

                case Geofence.GEOFENCE_TRANSITION_ENTER:
                    Toast.makeText(context, "Entered School Zone", Toast.LENGTH_SHORT).show();
                    break;

                case Geofence.GEOFENCE_TRANSITION_DWELL:
                    Toast.makeText(context, "Inside School Zone", Toast.LENGTH_SHORT).show();
                    break;

                case Geofence.GEOFENCE_TRANSITION_EXIT:
                    Toast.makeText(context, "School Zone Exited", Toast.LENGTH_SHORT).show();
                    break;
            }
        }



    }
}




