package ch.fhnw.locactitrackermobile.activity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import ch.fhnw.locactitrackermobile.model.ActivityTrace;


/**
 * Abstract activity to handle sensors
 */
public abstract class AbstractSensorsActivity extends AbstractDataTransferActivity implements SensorEventListener {

    protected Sensor accelerometer;
    protected SensorManager sm;

    protected boolean sensorRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void init(boolean receivedPermissions) {
        //Init accelerometer sensor
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        super.init(receivedPermissions);
    }

    /**
     * ------------------------------------
     * Accelerometer related functions
     * ------------------------------------
     */

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.d(getTag(), "Accuracy for " + sensor.getName() + " changed to " + i);
    }

    protected void startSensor() {
        sensorRunning = true;
        sm.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void stopSensor() {
        sensorRunning = false;
        sm.unregisterListener(this);
    }

    /**
     * Get accelerometer sensor values and map it into an activity trace.
     * @param event
     * @return an activity trace.
     */
    ActivityTrace getAccelerationFromSensor(SensorEvent event) {
        long timestamp = System.currentTimeMillis();
        Location loc = getLocation();
        double latitude = loc == null ? 0 : loc.getLatitude();
        double longitude = loc == null ? 0: loc.getLongitude();
        return new ActivityTrace(event.values[0], event.values[1], event.values[2], timestamp, latitude, longitude,
                configDominantHand, configUser);
    }
}
