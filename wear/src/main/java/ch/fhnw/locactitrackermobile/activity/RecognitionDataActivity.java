package ch.fhnw.locactitrackermobile.activity;

import android.Manifest;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import ch.fhnw.locactitrackermobile.MainActivity;
import ch.fhnw.locactitrackermobile.R;
import ch.fhnw.locactitrackermobile.model.ActivityTrace;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * Activity for the Recognition process
 */
public class RecognitionDataActivity extends AbstractSensorsActivity {

    public static final int PERMISSIONS_REQUEST = 12;

    private final String TAG = "RecognitionDataActivity";

    private final String PREDICTION_URL = "/recognition";

    private TextView activityTraceText;
    private Button myStartButton;
    private Button myStopButton;

    public ActivityTrace lastTrace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.prediction_data);
        activityTraceText = (TextView) findViewById(R.id.activityTrace);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, MainActivity.PERMISSIONS, MainActivity.PERMISSIONS_REQUEST);
        } else {
            init(true);
        }
    }

    @Override
    protected void init(boolean receivedPermission) {
        super.init(receivedPermission);

        if (receivedPermission) {
            initActionButtons();
        } else {
            activityTraceText.setText("No permission to access the location");
        }

    }

    @Override
    protected void updateStatusView() {
        if (lastTrace != null) {
            activityTraceText.setText("X:" + lastTrace.getX() +
                    "\nY:" + lastTrace.getY() +
                    "\nZ:" + lastTrace.getZ() +
                    "\nlat:" + lastTrace.getLatitude() +
                    "\nlong:" + lastTrace.getLongitude() +
                    "\nTimestamp:" + lastTrace.getTimestamp());
        } else {
            activityTraceText.setText(sensorRunning ? "Started" : "Ready to start");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MainActivity.PERMISSIONS_REQUEST) {
            boolean arePermissionsOk = false;
            for (int i:grantResults)
                arePermissionsOk &= (i == PERMISSION_GRANTED);

            if (arePermissionsOk) {
                Log.d(getTag(), "Permission received");
            }

            init(arePermissionsOk);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        ActivityTrace capturedActivityTrace = getAccelerationFromSensor(event);
        lastTrace = capturedActivityTrace;
        updateStatusView();
        sendData(capturedActivityTrace);
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    /**
     * Init start and stop buttons actions.
     */
    private void initActionButtons() {
        myStartButton = (Button) findViewById(R.id.button_start);
        myStopButton = (Button) findViewById(R.id.button_stop);

        myStartButton.setVisibility(View.VISIBLE);
        myStopButton.setVisibility(View.GONE);

        //Start button action on click
        myStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSensor();
                myStartButton.setVisibility(View.GONE);
                myStopButton.setVisibility(View.VISIBLE);
            }
        });

        //Stop button action on click
        myStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSensor();
                myStartButton.setVisibility(View.VISIBLE);
                myStopButton.setVisibility(View.GONE);
                finish();
            }
        });
    }

    @Override
    protected int getPermissionRequestId(){
        return this.PERMISSIONS_REQUEST;
    }

    @Override
    protected void postTrace(ActivityTrace trace) {
        Log.d(getTag(), "RECOGNITION: POSTDATA  " + trace.toString());

        //start API request to phone
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PREDICTION_URL);
        DataMap dataMap = putDataMapRequest.getDataMap();

        dataMap.putDouble(PROP_X, trace.getX());
        dataMap.putDouble(PROP_Y, trace.getY());
        dataMap.putDouble(PROP_Z, trace.getZ());

        dataMap.putDouble(PROP_LATITUDE, trace.getLatitude());
        dataMap.putDouble(PROP_LONGITUDE, trace.getLongitude());

        dataMap.putLong(PROP_TIMESTAMP, trace.getTimestamp());

        dataMap.putString(PROP_USER, trace.getUser());

        PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
        putDataRequest.setUrgent();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mApiClient, putDataRequest);
    }
}
