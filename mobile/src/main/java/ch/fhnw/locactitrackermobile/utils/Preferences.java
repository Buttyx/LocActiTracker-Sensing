package ch.fhnw.locactitrackermobile.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Preferences class to persist the configuration
 */
public class Preferences {

    private static String PREF_FILE = "locactitracker";

    public static String USER_ID = "user";
    public static String HANDEDNESS_ID = "handedness";
    public static String URL_ID = "url";
    public static String CONNECTION_ID = "connexion";

    public static String USER_DEFAULT = "xa";
    public static boolean HANDEDNESS_DEFAULT = false;
    public static boolean CONNECTION_DEFAULT = false;
    public static String URL_DEFAULT = "http://locactitracker.butty.me:8080/api/";

    static SharedPreferences preferences;

    public Preferences(Context ctx) {
        preferences = ctx.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    public boolean isDominantHand() {
        return preferences.getBoolean(HANDEDNESS_ID, HANDEDNESS_DEFAULT);
    }

    public String getUser() {
        return preferences.getString(USER_ID, USER_DEFAULT);
    }

    public String getURL() {
        return preferences.getString(URL_ID, URL_DEFAULT);
    }

    public boolean isDirectlyConnected() {
        return preferences.getBoolean(CONNECTION_ID, CONNECTION_DEFAULT);
    }

    public void setURL(String url) {
        preferences.edit().putString(URL_ID, url).commit();
    }

    public void setUser(String user) {
        preferences.edit().putString(USER_ID, user).commit();
    }

    public void setDominantHand(boolean dominantHand) {
        preferences.edit().putBoolean(HANDEDNESS_ID, dominantHand).commit();
    }

    public void setDirectConnection(boolean connection) {
        preferences.edit().putBoolean(CONNECTION_ID, connection).commit();
    }

    public void registerChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        preferences.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        preferences.registerOnSharedPreferenceChangeListener(listener);
    }

}
