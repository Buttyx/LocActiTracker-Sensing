package ch.fhnw.locactitrackermobile;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import ch.fhnw.locactitrackermobile.activity.AbstractDataTransferActivity;
import ch.fhnw.locactitrackermobile.activity.RecognitionDataActivity;
import ch.fhnw.locactitrackermobile.activity.TrainingDataActivity;
import ch.fhnw.locactitrackermobile.model.ActivityTrace;

/**
 * Start point of the application
 */
public class MainActivity extends AbstractDataTransferActivity {

    public static final int PERMISSIONS_REQUEST = 10;
    public static String[] PERMISSIONS = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET};

    private static final String TAG = "MainActivity";
    private TextView textBox;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        textBox = (TextView) findViewById(R.id.text);

        final Button myStartButton = (Button) findViewById(R.id.button_prediction);

        super.init(true);

        myStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, RecognitionDataActivity.class);
                startActivity(intent);
            }
        });

        Button myCollectButton = (Button) findViewById(R.id.button_training);
        myCollectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, TrainingDataActivity.class);
                startActivity(intent);
            }
        });

        mWaitingForGpsSignal = true;

        permissionApproved =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;


        if (!permissionApproved) {
            Log.d(TAG, "Location permission has NOT been granted. Requesting permission.");
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS,
                    PERMISSIONS_REQUEST);
        }


        /*
         * If this hardware doesn't support GPS, we warn the user. Note that when such device is
         * connected to a phone with GPS capabilities, the framework automatically routes the
         * location requests from the phone. However, if the phone becomes disconnected and the
         * wearable doesn't support GPS, no location is recorded until the phone is reconnected.
         */
        if (!hasGps()) {
            Log.w(TAG, "This hardware doesn't have GPS, so we warn user.");
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.gps_not_available))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            dialog.cancel();
                        }
                    })
                    .setCancelable(true)
                    .create()
                    .show();
        }

        updateStatusView();
    }

    @Override
    protected int getPermissionRequestId() {
        return PERMISSIONS_REQUEST;
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);
        updateStatusView();
    }

    @Override
    protected void updateStatusView() {
        textBox.setText("GPS:" + (mWaitingForGpsSignal ? "waiting for signal" : "ok") +
                ", Location:" + locationStatus +
                "\nURL:" + configUrl +
                "\nDirect:" + configDirectConnection + ", D-Hand:" + configDominantHand +
                "\nUser:" + configUser);
    }


    @Override
    public void onConnected(Bundle bundle) {
        super.onConnected(bundle);
        updateStatusView();
    }

    @Override
    protected void postTrace(ActivityTrace trace) {
        // Nothing, this activity is not posting traces
    }
}
