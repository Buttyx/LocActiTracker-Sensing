package ch.fhnw.locactitrackermobile.model;

/**
 * Activity trace containing
 * - the user
 * - timestamp
 * - values of the 3 accelerometer axis
 * - location coordinates
 * - if the data have been collected on dominant hand
 */
public class ActivityTrace {

    private long timestamp;
    private String user;
    private double x;
    private double y;
    private double z;
    private double longitude;
    private double latitude;
    private boolean dominantHand;

    public ActivityTrace(float x_value, float y_value, float z_value, long timestamp,
                         double latitude, double longitude, boolean dominantHand, String user) {
        this(new Double(""+x_value), new Double(""+y_value), new Double(""+z_value), timestamp, latitude, longitude, dominantHand, user);
    }

    public ActivityTrace(double x_value, double y_value, double z_value, long timestamp,
                         double latitude, double longitude, boolean dominantHand, String user) {
        x= x_value;
        y= y_value;
        z= z_value;
        this.user = user;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.dominantHand = dominantHand;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getUser() { return user; }

    public boolean isDominantHand() { return dominantHand; }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public double getLongitude() { return longitude; };

    public double getLatitude() { return latitude; };
    
}
