package com.atakmap.android.helloworld.utils;


import static androidx.core.app.ActivityCompat.requestPermissions;
import static androidx.core.content.ContextCompat.checkSelfPermission;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.coords.MGRSCoord;

public final class LocationUtilities {
    private static final String TAG = "KYLE";
    private static final int GPS_ENABLE_REQUEST = 42;

    private static LocationCallback locationCallback;
    private static MGRSCoord mgrsCoord;
    private static LocationRequest locationRequest;
    private static LocationManager locationManager;
    private static PendingIntent locationFailurePendingIntent;
    private static boolean locationUpdatesInProgress = false;

    private LocationUtilities() {
    }


    public static MGRSCoord getMGRSCoord(){
        return mgrsCoord;
    }

    private static LocationCallback getLocationCallback(Activity activity) {
        if (locationCallback == null) {
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        Log.i(TAG, "Locationresult is null");
                    } else {
                        for (Location location : locationResult.getLocations()) {
                            mgrsCoord = MGRSCoord.fromLatLon(Angle.fromDegreesLatitude(location.getLatitude()),
                                    Angle.fromDegreesLongitude(location.getLongitude()));
                        }
                    }
                }

                @Override
                public void onLocationAvailability(LocationAvailability locationAvailability) {
                    if(locationManager == null){
                        locationManager = (LocationManager) activity.getSystemService(activity.getApplicationContext().LOCATION_SERVICE);
                    }
                    if (!locationAvailability.isLocationAvailable()) {
                        Log.i(TAG, "Location is not available");

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            if (!locationManager.isLocationEnabled()) {
                                Log.i(TAG, "location is not enabled");
                            } else Log.i(TAG, "location is enabled");
                        }
                        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            Log.i(TAG, "location GPS Provider is not enabled");
                            //showGPSDisabledDialog();
                        } else Log.i(TAG, "gps provider is enabled");
                        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                            Log.i(TAG, "location Network Provider is not enabled");
                        } else Log.i(TAG, "network provider location is enabled");
                        if (!locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                            Log.i(TAG, "location passive provider is not enabled");
                        } else Log.i(TAG, "location passive provider enabled");
                    } else {
                        Log.i(TAG, "Location is avaiable");
                    }
                }

            };
        }
        return locationCallback;
    }

    private static void startLocationUpdates(Activity activity, FusedLocationProviderClient fusedLocationProviderClient) {
        if(!locationUpdatesInProgress) {
            if (checkSelfPermission(activity.getApplicationContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                String[] locationPermissionArray = {Manifest.permission.ACCESS_FINE_LOCATION};
                requestPermissions(activity, locationPermissionArray, 0);
            }
            if (checkSelfPermission(activity.getApplicationContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                String[] locationPermissionArray = {Manifest.permission.ACCESS_COARSE_LOCATION};
                requestPermissions(activity, locationPermissionArray, 0);
            }

            if (checkSelfPermission(activity.getApplicationContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(activity.getApplicationContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Log.i(TAG, "Location update persmission bad");
            } else {
                fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                        getLocationCallback(activity),
                        null);
                locationUpdatesInProgress = true;
            }
        }
    }

    public static void stopLocationUpdates(Activity activity,FusedLocationProviderClient fusedLocationProviderClient) {
        if (fusedLocationProviderClient == null) return;

        fusedLocationProviderClient.removeLocationUpdates(getLocationCallback(activity));
        locationUpdatesInProgress = false;
    }


    public static void createLocationRequest(){
        if(locationRequest == null) {
            locationRequest = new LocationRequest();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setFastestInterval(5000);
            locationRequest.setInterval(10000);
        }
    }

    public static void verifyLocationSettings(Activity activity, FusedLocationProviderClient fusedLocationProviderClient){
        Log.d(TAG, "top of VLS");
        if (locationRequest == null) {
            Log.d(TAG, "Creating request");
            createLocationRequest();
        } else Log.d(TAG, "Not null location request. How?");

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        SettingsClient settingsClient = LocationServices.getSettingsClient(activity.getApplicationContext());
        Task<LocationSettingsResponse> locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build());
        locationSettingsResponseTask.addOnSuccessListener(activity, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                //Initialize location requests here
                startLocationUpdates(activity, fusedLocationProviderClient);
            }
        });
        locationSettingsResponseTask.addOnFailureListener(activity, new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if(e instanceof ResolvableApiException){
                    try{
                        ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                        locationFailurePendingIntent= resolvableApiException.getResolution();
                        Log.i(TAG, "Got a pending intent"+locationFailurePendingIntent.getCreatorPackage());
                    } catch(Exception e1) {
                        Log.i(TAG, "Ignoring error inside of ResolvableApiException:"+e1.getLocalizedMessage());
                    }
                }
            }
        });
    }

    public static void showGPSDisabledDialog(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("GPS Disabled");
        builder.setMessage("Gps is disabled, in order to mark current coordinates you need to enable GPS of your device");
        builder.setPositiveButton("Enable GPS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                activity.startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                        GPS_ENABLE_REQUEST);
            }
        }).setNegativeButton("No, Just Exit", (dialog, which) -> {

        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public static boolean isLocationEnabled(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return locationManager.isLocationEnabled();
        } else {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
    }
}
