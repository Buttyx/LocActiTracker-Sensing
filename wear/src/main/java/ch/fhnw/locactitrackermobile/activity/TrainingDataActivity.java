package ch.fhnw.locactitrackermobile.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.SensorEvent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
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

import java.util.Timer;
import java.util.TimerTask;

import ch.fhnw.locactitrackermobile.model.TrainingTrace;
import ch.fhnw.locactitrackermobile.R;
import ch.fhnw.locactitrackermobile.model.ActivityTrace;
import ch.fhnw.locactitrackermobile.model.ActivityType;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * Activity for the Training process
 */
public class TrainingDataActivity extends AbstractSensorsActivity {

    public static final int PERMISSIONS_REQUEST = 11;
    public static String[] PERMISSIONS = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET};

    private final String TAG = "TrainingDataActivity";

    private final String TRAINING_URL = "/training";

    public static final String ACTIVITY = "ACTIVITY";
    public static final int ACTIVITY_SELECTION_TASK = 10;

    private String selectedActivity = ActivityType.TRANSPORTATION.getLabel();

    private Timer timer;
    private TimerTask startTimerTask;
    private Handler timerHandler;

    private TextView activityTraceText;
    private Button myStartButton;
    private Button myStopButton;
    private Button myActivityButton;
    private Button myBackButton;

    public ActivityTrace lastTrace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.training_data);
        activityTraceText = (TextView) findViewById(R.id.activityTrace);

        super.init(true);

        timerHandler = new Handler() {
            public void handleMessage(NotificationCompat.MessagingStyle.Message msg) {
                updateStatusView();
            }
        };

        permissionApproved =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;


        if (!permissionApproved) {
            Log.d(TAG, "Location permission has NOT been granted. Requesting permission.");

            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS,
                    PERMISSIONS_REQUEST);
        } else {
            init(true);
        }

    }

    @Override
    protected void init(boolean receivedPermission) {
        super.init(receivedPermission);

        if (receivedPermission) {
            initActionButtons();
            updateStatusView();
        } else {
            activityTraceText.setText("No permission to access the location");
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
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
    public void onSensorChanged(SensorEvent event) {
        ActivityTrace capturedActivityTrace = getAccelerationFromSensor(event);
        TrainingTrace trainingTrace = new TrainingTrace(capturedActivityTrace, selectedActivity);
        lastTrace = trainingTrace;
        updateStatusView();
        sendData(trainingTrace);
    }

    /**
     * Initialize the different buttons
     */
    private void initActionButtons() {
        myStartButton = (Button) findViewById(R.id.button_start_training);
        myStopButton = (Button) findViewById(R.id.button_stop_training);
        myActivityButton = (Button) findViewById(R.id.button_choose_activity);
        myBackButton = (Button) findViewById(R.id.button_collect_exit);

        myActivityButton.setText(selectedActivity);

        myStartButton.setVisibility(View.VISIBLE);
        myActivityButton.setVisibility(View.VISIBLE);
        myStopButton.setVisibility(View.GONE);

        myStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                updateStatusView();

                initTimerTask();
                myStartButton.setVisibility(View.GONE);
                myStopButton.setVisibility(View.VISIBLE);
                myStopButton.setActivated(true);

                //Sensor starts after 3 seconds
                timer.schedule(startTimerTask, 1000);
                updateStatusView();
            }
        });


        myStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTimerTask.cancel();
                timer.cancel();
                stopSensor();
                updateStatusView();

                myStopButton.setVisibility(View.GONE);
                myStartButton.setVisibility(View.VISIBLE);
                myStartButton.setActivated(true);
            }
        });

        myActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(TrainingDataActivity.this, ActivityListLayout.class);
                startActivityForResult(intent, ACTIVITY_SELECTION_TASK);
            }
        });

        myBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(startTimerTask != null) {
                    startTimerTask.cancel();
                    timer.cancel();
                }
                stopSensor();
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_SELECTION_TASK && resultCode == RESULT_OK) {
            selectedActivity = data.getStringExtra(ACTIVITY);
            myActivityButton.setText(selectedActivity);
            Log.d(TAG, "Activity changed to " + selectedActivity);
        }
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        updateStatusView();
        activityTraceText.getPaint().setAntiAlias(false);
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();

        updateStatusView();
        myStopButton.setActivated(true);
        activityTraceText.getPaint().setAntiAlias(true);
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateStatusView();
    }

    @Override
    public void onConnected(Bundle bundle) {
        super.onConnected(bundle);
        updateStatusView();
    }


    private void initTimerTask() {
        timer = new Timer();

        startTimerTask = new TimerTask() {
            @Override
            public void run() {
                startSensor();
                timerHandler.obtainMessage(1).sendToTarget();
            }
        };
    }

    @Override
    protected int getPermissionRequestId() {
        return this.PERMISSIONS_REQUEST;
    };

    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);
        updateStatusView();
    }

    @Override
    protected void updateStatusView() {
        if (locationStatus && lastTrace != null ) {
            activityTraceText.setText("X:" + lastTrace.getX() +
                    "\nY:" + lastTrace.getY() +
                    "\nZ:" + lastTrace.getZ() +
                    "\nlat:" + lastTrace.getLatitude() +
                    "\nlong:" + lastTrace.getLongitude());
        } else {
            String message = "";
            if (!sensorRunning)
                message += "Not running\n";
            if (!locationStatus)
                message += "Waiting for connexion";

            activityTraceText.setText(message);
        }
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    @Override
    protected void postTrace(ActivityTrace trace) {
        Log.d(getTag(), "TRAINING: POSTDATA  " + trace.toString());

        TrainingTrace trainingTrace = (TrainingTrace) trace;

        //start API request to phone
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(TRAINING_URL);
        DataMap dataMap = putDataMapRequest.getDataMap();

        dataMap.putDouble(PROP_X, trainingTrace.getX());
        dataMap.putDouble(PROP_Y, trainingTrace.getY());
        dataMap.putDouble(PROP_Z, trainingTrace.getZ());

        dataMap.putDouble(PROP_LATITUDE, trainingTrace.getLatitude());
        dataMap.putDouble(PROP_LONGITUDE, trainingTrace.getLongitude());

        dataMap.putLong(PROP_TIMESTAMP, trainingTrace.getTimestamp());

        dataMap.putString(PROP_USER, trainingTrace.getUser());

        dataMap.putString(PROP_ACTIVITY, trainingTrace.getActivity());

        PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
        putDataRequest.setUrgent();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mApiClient, putDataRequest);
    }
}
