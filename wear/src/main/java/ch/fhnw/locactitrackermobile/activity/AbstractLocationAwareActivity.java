package ch.fhnw.locactitrackermobile.activity;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.Wearable;


/**
 * Abstract activity implementing location awareness
 * Request location update using the Google Location Service
 */
public abstract class AbstractLocationAwareActivity extends WearableActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, ResultCallback<Status> {

    private static final long LOC_UPDATE_INTERVAL_MS = 500;
    private static final long LOC_FASTEST_INTERVAL_MS = 200;
    private static final int LOC_PRIORITY = LocationRequest.PRIORITY_HIGH_ACCURACY;
    private static final float LOC_SMALLEST_DISPLACEMENT = 0f;

    GoogleApiClient mApiClient;

    Location lastLocation;
    long timeLocationUpdate;

    boolean initialized = false;
    private LocationRequest locationRequest;

    protected boolean mWaitingForGpsSignal = false;
    protected boolean permissionApproved = false;
    protected boolean locationStatus = false;
    protected boolean apiClientStatus = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setAmbientEnabled();

        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    /**
     * initialize the activity when the permissions is received
     * @param receivedPermissions
     */
    protected void init(boolean receivedPermissions) {
        initialized = true;
    }

    /**
     * ------------------------------------
     * Location related functions
     * ------------------------------------
     */

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(getTag(), "onConnected()");
        apiClientStatus = true;
        requestLocation();
    }

    @Override
    public void onResult(@NonNull Status status) {
            if (status.getStatus().isSuccess()) {
                if (Log.isLoggable(getTag(), Log.DEBUG)) {
                    Log.d(getTag(), "Successfully requested location updates");
                }
            } else {
                Log.e(getTag(),
                        "Failed in requesting location updates, "
                                + "status code: "
                                + status.getStatusCode()
                                + ", message: "
                                + status.getStatusMessage());
            }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setAmbientEnabled();
        mApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mApiClient.isConnected()) {
            LocationServices.FusedLocationApi
                    .removeLocationUpdates(mApiClient, this);
            mApiClient.disconnect();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (Log.isLoggable(getTag(), Log.DEBUG)) {
            Log.d(getTag(), "connection to location client suspended");
        }
        apiClientStatus = false;
        updateStatusView();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(getTag(), "onLocationChanged() : " + location);

        mWaitingForGpsSignal = false;
        updateLocation(location);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        apiClientStatus = false;
        if (mApiClient.isConnected()) {
            LocationServices.FusedLocationApi
                    .removeLocationUpdates(mApiClient, this);
        }
        mApiClient.disconnect();
        Log.d(getTag(), "connection to location client failed");
    }

    /**
     * Update the current location with the given element
     * @param location the location object
     * @return true if location updated, otherwise false
     */
    protected boolean updateLocation(Location location) {
        if (location != null) {
            timeLocationUpdate = System.currentTimeMillis();
            lastLocation = location;
            if (!locationStatus) {
                locationStatus = true;
                updateStatusView();
            }
            return true;
        } else {
            if (locationStatus) {
                locationStatus = false;
                updateStatusView();
            }
            return false;
        }
    }

    /**
     * Retrieve the current location if available or null
     * @return Location
     * @throws SecurityException
     */
    protected Location getLocation() throws SecurityException {

        // Return the last location retrieved if still valid
        if (lastLocation != null && lastLocation.getLongitude() > 0 && (timeLocationUpdate - System.currentTimeMillis() < 1000)) {
            return lastLocation;
        }

        // Else try to retrieve the current location
        final LocationAvailability locationAvailability = LocationServices.FusedLocationApi.getLocationAvailability(mApiClient);

        if (locationAvailability != null && locationAvailability.isLocationAvailable()) {
            Location location = LocationServices.FusedLocationApi.getLastLocation(mApiClient);
            if (updateLocation(location)) return location;
        } else {
            Log.d(getTag(), "Location service not available yet");
            locationStatus = false;
            updateStatusView();
        }

        return null;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        Log.d(getTag(), "onRequestPermissionsResult(): " + permissions);

        if (requestCode == getPermissionRequestId()) {
            Log.i(getTag(), "Received response for GPS permission request.");

            if ((grantResults.length == 1)
                    && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.i(getTag(), "GPS permission granted.");
                permissionApproved = true;

                if(mApiClient != null && mApiClient.isConnected()) {
                    requestLocation();
                }

            } else {
                Log.i(getTag(), "GPS permission NOT granted.");
                permissionApproved = false;
            }

            updateStatusView();
        }
    }

    /**
     * Generate a location request to the Google Location Service
     */
    protected void requestLocation() throws SecurityException {
        Log.d(getTag(), "Location requested");

        if (permissionApproved && mApiClient.isConnected()) {

            locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setSmallestDisplacement(LOC_SMALLEST_DISPLACEMENT)
                    .setPriority(LOC_PRIORITY)
                    .setInterval(LOC_UPDATE_INTERVAL_MS)
                    .setFastestInterval(LOC_FASTEST_INTERVAL_MS);

            LocationServices.FusedLocationApi
                    .requestLocationUpdates(mApiClient, locationRequest, this)
                    .setResultCallback(new ResultCallback<Status>() {

                        @Override
                        public void onResult(Status status) {
                            if (status.getStatus().isSuccess()) {

                                if (Log.isLoggable(getTag(), Log.DEBUG)) {
                                    Log.d(getTag(), "Successfully requested location updates");
                                }
                            } else {
                                Log.e(getTag(),
                                        "Failed in requesting location updates, "
                                                + "status code: "
                                                + status.getStatusCode() + ", message: " + status
                                                .getStatusMessage());
                            }
                        }
                    });
        }
    }

    /**
     * Returns {@code true} if this device has the GPS capabilities.
     */
    protected boolean hasGps() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }

    /**
     * Return the activity Tag for logging purpose
     */
    protected abstract String getTag();

    /**
     * Return the permission request Id related to this activity
     */
    protected abstract int getPermissionRequestId();

    /**
     * Update the status information
     */
    protected abstract void updateStatusView();
}
