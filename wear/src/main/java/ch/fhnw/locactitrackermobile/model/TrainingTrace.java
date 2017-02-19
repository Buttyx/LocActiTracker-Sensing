package ch.fhnw.locactitrackermobile.model;

/**
 * Training trace
 * Extend activity trace with the activity label
 */
public class TrainingTrace extends ActivityTrace{

    private String activity;

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
