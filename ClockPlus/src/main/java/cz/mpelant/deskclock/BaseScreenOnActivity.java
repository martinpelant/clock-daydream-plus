/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cz.mpelant.deskclock;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.view.Window;
import android.view.WindowManager;

public abstract class BaseScreenOnActivity extends Activity {
    static final boolean DEBUG = false;
    static final String TAG = "BaseScreenOnActivity";

    private PendingIntent mQuarterlyIntent;
    private boolean mPluggedIn = true;
    private final int mFlags = (WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean changed = intent.getAction().equals(Intent.ACTION_TIME_CHANGED) || intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED);
            if (intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)) {
                mPluggedIn = true;
                setWakeLock();
            } else if (intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED)) {
                mPluggedIn = false;
                setWakeLock();
            } else if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                finish();
            } else if (intent.getAction().equals(Utils.ACTION_ON_QUARTER_HOUR) || changed) {
                changed = true;
            }

            if (changed) {
                updateViews();
            }

        }
    };

    protected abstract void updateViews();

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Utils.ACTION_ON_QUARTER_HOUR);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(mIntentReceiver, filter);
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent chargingIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = chargingIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        mPluggedIn = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;

        setWakeLock();

        long alarmOnQuarterHour = Utils.getAlarmOnQuarterHour();
        mQuarterlyIntent = PendingIntent.getBroadcast(this, 0, new Intent(Utils.ACTION_ON_QUARTER_HOUR), 0);
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE)).setRepeating(AlarmManager.RTC, alarmOnQuarterHour, AlarmManager.INTERVAL_FIFTEEN_MINUTES, mQuarterlyIntent);
    }

    @Override
    public void onPause() {
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(mQuarterlyIntent);
        super.onPause();
    }

    @Override
    public void onStop() {
        unregisterReceiver(mIntentReceiver);
        super.onStop();
    }

    private void setWakeLock() {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        winParams.flags |= getAdditionalFlags();
        if (mPluggedIn)
            winParams.flags |= mFlags;
        else
            winParams.flags &= (~mFlags);
        win.setAttributes(winParams);
    }

    protected int getAdditionalFlags() {
        return 0;
    }

}
