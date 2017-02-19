package ch.fhnw.locactitrackermobile.activity;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import ch.fhnw.locactitrackermobile.api.RestApi;
import ch.fhnw.locactitrackermobile.model.ActivityTrace;
import ch.fhnw.locactitrackermobile.model.TrainingTrace;
import ch.fhnw.locactitrackermobile.utils.Preferences;
import retrofit.RestAdapter;


/**
 * Abstract activity implementing data transfer functionality
 * Allow communication with smartphone app or Rest API
 */
public abstract class AbstractDataTransferActivity extends AbstractLocationAwareActivity
        implements DataApi.DataListener, SharedPreferences.OnSharedPreferenceChangeListener {

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

    private final String CONFIGURATION_URL = "/configuration";

    protected String configUrl;
    protected String configUser;
    protected boolean configDirectConnection;
    protected boolean configDominantHand;

    RestApi restAPI;

    protected Preferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initPreferences();
        initRestAPI();
    }

    /**
     * Initialize the rest API
     */
    protected void initRestAPI() {
        Bundle extras = getIntent().getExtras();
        if (configUrl == null || configUrl == "") {
            configUrl = preferences.getURL();
        }

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(configUrl)
                .build();
        restAPI = restAdapter.create(RestApi.class);
    }

    protected void initPreferences() {
        preferences = new Preferences(this);

        configUrl = preferences.getURL();
        configDirectConnection = preferences.isDirectlyConnected();
        configDominantHand = preferences.isDominantHand();
        configUser = preferences.getUser();

        preferences.registerChangeListener(this);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        super.onConnected(bundle);

        // Listen to data API events
        Wearable.DataApi.addListener(mApiClient, this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {

        if (Preferences.CONNECTION_ID.equals(key)) {
            configDirectConnection = preferences.isDirectlyConnected();
        }

        if (Preferences.HANDEDNESS_ID.equals(key)) {
            configDominantHand = preferences.isDominantHand();
        }

        if (Preferences.URL_ID.equals(key)) {
            configUrl = preferences.getURL();
            initRestAPI();
        }

        if (Preferences.USER_ID.equals(key)) {
            configUser = preferences.getUser();
        }

        updateStatusView();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        super.onConnectionSuspended(cause);
        Wearable.DataApi.removeListener(mApiClient, this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause(){
        super.onPause();
        Wearable.DataApi.removeListener(mApiClient, this);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        super.onConnectionFailed(result);
        Log.d(getTag(), "DATA LAYER SYNC: Connection failed");
        Wearable.DataApi.removeListener(mApiClient, this);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {

        for(DataEvent event: dataEventBuffer) {

            if (event.getType() == DataEvent.TYPE_CHANGED) {

                DataItem item = event.getDataItem();
                DataMapItem dataMapItem = DataMapItem.fromDataItem(item);
                String path = item.getUri().getPath();

                if (CONFIGURATION_URL.equals(path)) {
                    DataMap dataMap = dataMapItem.getDataMap();

                    if (dataMap.containsKey(PROP_CONFIG_WATCH_POSITION)) {
                        boolean dominantHand = dataMap.getBoolean(PROP_CONFIG_WATCH_POSITION);
                        preferences.setDominantHand(dominantHand);
                    }

                    if (dataMap.containsKey(PROP_CONFIG_URL)) {
                        String url = dataMap.getString(PROP_CONFIG_URL);
                        preferences.setURL(url);
                    }

                    if (dataMap.containsKey(PROP_CONFIG_USERNAME)) {
                        String username = dataMap.getString(PROP_CONFIG_USERNAME);
                        preferences.setUser(username);
                    }

                    if (dataMap.containsKey(PROP_CONFIG_DIRECT_CONN)) {
                        boolean connection = dataMap.getBoolean(PROP_CONFIG_DIRECT_CONN);
                        preferences.setDirectConnection(connection);
                    }

                    updateStatusView();
                }
            }
        }

        Log.d(getTag(), "DATACHANGED");
    }

    protected abstract void postTrace(ActivityTrace trace);

    protected void sendData(ActivityTrace trace) {
        Log.d(getTag(), "START DATA SYNC: " + trace.toString());
        new SendAccelerationAsyncTask().execute(trace);
    }


    /**
     * Asyncronous task to post data on DATA layer
     */
    private class SendAccelerationAsyncTask extends AsyncTask<ActivityTrace, Void, Void> {

        @Override
        protected Void doInBackground(ActivityTrace... params) {
            try {
                ActivityTrace trace = params[0];

                if (configDirectConnection) {
                    if (trace instanceof TrainingTrace) {
                        restAPI.sendTrainingValues((TrainingTrace) trace);
                    } else {
                        restAPI.sendRecognitionValues(trace);
                    }
                } else {
                    postTrace(params[0]);
                }
            } catch(Exception e) {
                Log.d(getTag(), "ERROR DATA SYNC: " + e);
                e.printStackTrace();
            }
            return null;
        }
    }
}
