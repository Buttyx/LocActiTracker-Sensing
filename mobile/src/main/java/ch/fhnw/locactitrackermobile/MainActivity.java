package ch.fhnw.locactitrackermobile;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import ch.fhnw.locactitrackermobile.model.ActivityTrace;
import ch.fhnw.locactitrackermobile.model.TrainingTrace;
import ch.fhnw.locactitrackermobile.service.RestApi;
import ch.fhnw.locactitrackermobile.utils.Preferences;
import retrofit.ErrorHandler;
import retrofit.RestAdapter;
import retrofit.RetrofitError;

/**
 * Start point and single activity of the companion application
 * In charge of data synchronisation
 */
public class MainActivity extends Activity implements  DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ErrorHandler {

    public final String PROP_X = "X";
    public final String PROP_Y = "Y";
    public final String PROP_Z = "Z";
    public final String PROP_USER = "USER";
    public final String PROP_TIMESTAMP = "TIMESTAMP";
    public final String PROP_LATITUDE = "LATITUDE";
    public final String PROP_LONGITUDE = "LONGITUDE";
    public final String PROP_ACTIVITY = "ACTIVITY";

    public final String PROP_CONFIG_USERNAME = "USERNAME";
    public final String PROP_CONFIG_WATCH_POSITION = "WATCH_POSITION";
    public final String PROP_CONFIG_URL = "URL";
    public final String PROP_CONFIG_DIRECT_CONN = "DIRECT_CONNECTION";

    private final String RECOGNITION_URL = "/recognition";
    private final String TRAINING_URL = "/training";
    private final String CONFIGURATION_URL = "/configuration";

    private Activity activity;
    private GoogleApiClient googleClient;

    private TextView counterField;
    private TextView connexionStatusField;

    private EditText usernameField;
    private EditText urlField;

    private CheckBox handField;
    private CheckBox connectionField;

    private int countPrediction = 0;
    private int countTraining = 0;
    private boolean connexionStatus = false;

    private double lastLatitude;
    private double lastLongitude;

    private String configUsername = "username";
    private Boolean configPositionDominantHand = false;
    private Boolean configDirectConnection = false;
    private String configURL = "http://locactitracker.butty.me:8080/api/";

    private RestApi api;

    private Preferences preferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.activity = this;

        initApi(configURL);

        preferences = new Preferences(this);

        configURL = preferences.getURL();
        configDirectConnection = preferences.isDirectlyConnected();
        configPositionDominantHand = preferences.isDominantHand();
        configUsername = preferences.getUser();

        // data layer
        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        counterField = (TextView) findViewById(R.id.counter);
        connexionStatusField = (TextView) findViewById(R.id.status);

        usernameField = (EditText) findViewById(R.id.username);
        usernameField.setText(configUsername);

        handField = (CheckBox) findViewById(R.id.position);
        handField.setChecked(configPositionDominantHand);

        connectionField = (CheckBox) findViewById(R.id.connection);
        connectionField.setChecked(configDirectConnection);

        urlField = (EditText) findViewById(R.id.url);
        urlField.setText(configURL);

        usernameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                configUsername = s.toString();
                preferences.setUser(configUsername);
                updateInfos();
                new SendToWearAsyncTask().doInBackground();
            }
        });

        urlField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                configURL = s.toString();
                preferences.setURL(configURL);
                initApi(configURL);
                new SendToWearAsyncTask().doInBackground();
            }
        });
    }

    public void positionOnClick (View view) {
        configPositionDominantHand = ((CheckBox) view).isChecked();
        preferences.setDominantHand(configPositionDominantHand);
        updateInfos();
        new SendToWearAsyncTask().doInBackground();
    }

    public void connectionOnClick (View view) {
        configDirectConnection = ((CheckBox) view).isChecked();
        preferences.setDirectConnection(configDirectConnection);
        updateInfos();
        new SendToWearAsyncTask().doInBackground();
    }

    public void initApi (String url) {
        // api
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(url)
                .setErrorHandler(this)
                .build();

        api = restAdapter.create(RestApi.class);
    }

    //on successful connection to play services, add data listner
    public void onConnected(Bundle connectionHint) {
        connexionStatus = true;
        Wearable.DataApi.addListener(googleClient, this);
    }

    //on resuming activity, reconnect play services
    public void onResume(){
        super.onResume();
        googleClient.connect();
    }

    //on suspended connection, remove play services
    public void onConnectionSuspended(int cause) {
        connexionStatus = false;
        Wearable.DataApi.removeListener(googleClient, this);
    }

    //pause listener, disconnect play services
    public void onPause(){
        super.onPause();
        Wearable.DataApi.removeListener(googleClient, this);
        googleClient.disconnect();
    }

    //On failed connection to play services, remove the data listener
    public void onConnectionFailed(ConnectionResult result) {
        Wearable.DataApi.removeListener(googleClient, this);
    }

    /**
     * Callback for data updates from data layer API
     * @param dataEvents
     */
    public void onDataChanged(DataEventBuffer dataEvents) {
        for(DataEvent event: dataEvents){

            //data item changed
            if(event.getType() == DataEvent.TYPE_CHANGED){

                DataItem item = event.getDataItem();
                DataMapItem dataMapItem = DataMapItem.fromDataItem(item);
                String path = item.getUri().getPath();
                boolean isTraining = TRAINING_URL.equals(path);

                if(isTraining || RECOGNITION_URL.equals(path)) {

                    Log.d("debug", "caught message passed to me by the wearable");

                    final DataMap dataMap = dataMapItem.getDataMap();

                    double x = dataMap.getDouble(PROP_X);
                    double y = dataMap.getDouble(PROP_Y);
                    double z = dataMap.getDouble(PROP_Z);

                    String user = dataMap.getString(PROP_USER);

                    long timestamp = dataMap.getLong(PROP_TIMESTAMP);

                    double latitude = dataMap.getDouble(PROP_LATITUDE);
                    double longitude = dataMap.getDouble(PROP_LONGITUDE);

                    lastLatitude = latitude;
                    lastLongitude = longitude;

                    if (timestamp != 0) {
                        if (isTraining) {
                            countTraining++;
                            updateInfos();
                            String activity = dataMap.getString(PROP_ACTIVITY);
                            TrainingTrace trace = new TrainingTrace(x, y, z, timestamp, latitude, longitude, configPositionDominantHand, configUsername, activity);
                            new SendTrainingTraceAsyncTask().execute(trace);
                        } else {
                            countPrediction++;
                            updateInfos();
                            ActivityTrace trace = new ActivityTrace(x, y, z, timestamp, latitude, longitude, configPositionDominantHand, configUsername);
                            new SendRecognitionTraceAsyncTask().execute(trace);
                        }
                    }
                }
            }
        }
    }

    /**
     * Update the status information
     */
    private void updateInfos(){
        if (connexionStatus) {
            connexionStatusField.setText("Connected");
        } else {
            connexionStatusField.setText("Not connected");
        }

        counterField.setText("Name: " + configUsername +
                            "\nURL: " + configURL +
                            "\nWatch on dominant hand: " + configPositionDominantHand +
                            "\nRecognition Traces: " + countPrediction +
                            "\nTraining Traces:" + countTraining +
                            "\nLocation: " + lastLatitude + " / " + lastLongitude );
    }

    @Override
    public Throwable handleError(RetrofitError cause) {
        Log.d("LOCACTITRACKER", "Retrofit error: " + cause);
        return null;
    }


    /**
     * Asyncronous task to post recognition request to a Rest API.
     */
    private class SendRecognitionTraceAsyncTask extends AsyncTask<ActivityTrace, Void, Void> {

        @Override
        protected Void doInBackground(ActivityTrace... params) {
            try {

                api.sendRecognitionValues(params[0]);

            } catch(Exception e) {

                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Asyncronous task to post training request to a Rest API.
     */
    private class SendTrainingTraceAsyncTask extends AsyncTask<TrainingTrace, Void, Void> {

        @Override
        protected Void doInBackground(TrainingTrace... params) {
            try {

                api.sendTrainingValues(params[0]);

            } catch(RetrofitError e) {

                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Asyncronous task to post data on DATA layer
     */
    private class SendToWearAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... param) {
            try {
                //start API request to phone
                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(CONFIGURATION_URL);
                DataMap dataMap = putDataMapRequest.getDataMap();

                dataMap.putString(PROP_CONFIG_USERNAME, configUsername);
                dataMap.putBoolean(PROP_CONFIG_WATCH_POSITION, configPositionDominantHand);
                dataMap.putString(PROP_CONFIG_URL, configURL);
                dataMap.putBoolean(PROP_CONFIG_DIRECT_CONN, configDirectConnection);

                PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
                putDataRequest.setUrgent();
                PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleClient, putDataRequest);

            } catch(Exception e) {
                Log.d("LOCACTITRACKER", "ERROR DATA SYNC: " + e);
                e.printStackTrace();
            }
            return null;
        }
    }
}
