
package cz.mpelant.deskclock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.TextView;

public class ClockActivity extends Activity {
    public static final String TAG = "ClockActivity";
    private View mDigitalClock;
    private View mAnalogClock;
    private TextView mDate;
    private TextView mNextAlarm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_clock_frame);
        mDigitalClock = findViewById(R.id.digital_clock);
        mAnalogClock = findViewById(R.id.analog_clock);
        mDate = (TextView) findViewById(R.id.date);
        mNextAlarm = (TextView) findViewById(R.id.nextAlarm);
        setClockStyle();
    }

    private void setClockStyle() {
        Utils.setClockStyle(this, mDigitalClock, mAnalogClock, ScreensaverSettingsActivity.KEY_CLOCK_STYLE);
    }

    @Override
    protected void onResume() {
        setClockStyle();
        updateViews();
        super.onResume();
    }

    private void updateViews() {
        Utils.setAlarmTextView(this, mNextAlarm);
        Utils.setDateTextView(this, mDate);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add("Settings").setIcon(R.drawable.ic_action_settings);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                startActivity(new Intent(ClockActivity.this, ScreensaverSettingsActivity.class));
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }
}
