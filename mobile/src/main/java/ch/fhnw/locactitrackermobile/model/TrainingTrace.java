package ch.fhnw.locactitrackermobile.model;

/**
 * Training trace
 * Extend activity trace with the activity label
 */
public class TrainingTrace extends ActivityTrace{

    private String userID;
    private String activity;
    private ActivityTrace activityTrace;

    public TrainingTrace(double x_value, double y_value, double z_value, long timestamp,
                         double latitude, double longitude, boolean dominantHand, String user, String activity) {

        super(x_value, y_value, z_value, timestamp, latitude, longitude, dominantHand, user);
        this.activity = activity;
    }

    public TrainingTrace(ActivityTrace trace, String activity) {
        super(trace.getX(), trace.getY(), trace.getZ(), trace.getTimestamp(), trace.getLatitude(), trace.getLongitude(), trace.isDominantHand(), trace.getUser());
        this.activity = activity;
    }

    public String getActivity() {
        return activity;
    }
    public void setActivity(String activity) {
        this.activity = activity;
    }
}
