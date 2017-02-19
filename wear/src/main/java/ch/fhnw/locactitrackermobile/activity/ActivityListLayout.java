package ch.fhnw.locactitrackermobile.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import ch.fhnw.locactitrackermobile.R;
import ch.fhnw.locactitrackermobile.model.ActivityType;

/** This class sets a timer. */
public class ActivityListLayout extends Activity implements WearableListView.ClickListener {

    public static final int NUMBER_OF_ACTIVITIES = ActivityType.values().length;
    public static final String TAG = "ActivityListLayout";

    private ListViewItem[] mActivityItem = new ListViewItem[NUMBER_OF_ACTIVITIES];
    private WearableListView mWearableListView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Resources res = getResources();
        int i = 0;
        for (ActivityType a : ActivityType.values()) {
            mActivityItem[i] = new ListViewItem(a);
            i++;
        }

        setContentView(R.layout.activity_list);

        // Initialize a simple list of countdown time options.
        mWearableListView = (WearableListView) findViewById(R.id.activity_list);
        mWearableListView.setAdapter(new ActivityWearableListViewAdapter(this));
        mWearableListView.setClickListener(this);
    }

    @Override
    public void onClick(WearableListView.ViewHolder holder) {
        ActivityType a = mActivityItem[holder.getAdapterPosition()].activity;
        Intent result = new Intent();
        result.putExtra(TrainingDataActivity.ACTIVITY, a.getLabel());
        setResult(RESULT_OK, result);
        finishActivity(TrainingDataActivity.ACTIVITY_SELECTION_TASK);
        finish();
    }

    @Override
    public void onTopEmptyRegionClick() {
    }



    /** Model class for the listview. */
    private static class ListViewItem {

        // Label to display.
        private ActivityType activity;

        public ListViewItem(ActivityType activity) {
            this.activity = activity;
        }

        @Override
        public String toString() {
            return activity.getLabel();
        }
    }

    private final class ActivityWearableListViewAdapter extends WearableListView.Adapter {
        private final Context mContext;
        private final LayoutInflater mInflater;

        private ActivityWearableListViewAdapter(Context context) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new WearableListView.ViewHolder(
                    mInflater.inflate(R.layout.activity_item, null));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
            TextView view = (TextView) holder.itemView.findViewById(R.id.name);
            view.setText(mActivityItem[position].activity.getLabel());
            holder.itemView.setTag(position);
        }

        @Override
        public int getItemCount() {
            return NUMBER_OF_ACTIVITIES;
        }
    }

}