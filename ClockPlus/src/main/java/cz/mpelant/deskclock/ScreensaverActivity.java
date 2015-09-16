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

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

public class ScreensaverActivity extends BaseScreenOnActivity {
    static final boolean DEBUG = BuildConfig.DEBUG;
    static final String TAG = "DeskClock/ScreensaverAc";

    // This value must match android:defaultValue of
    // android:key="screensaver_clock_style" in dream_settings.xml
    static final String DEFAULT_CLOCK_STYLE = "digital";

    private View mContentView, mSaverView;
    private View mAnalogClock, mDigitalClock;

    private final Handler mHandler = new Handler();
    private final ScreensaverMoveSaverRunnable mMoveSaverRunnable;
    private String mDateFormat;
    private String mDateFormatForAccessibility;

    public ScreensaverActivity() {
        mMoveSaverRunnable = new ScreensaverMoveSaverRunnable(mHandler);
    }

    /**
     * <p>If the charging is over, for the activity to finish.</p>
     */
    public class PowerConnectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isNotCharging = status == BatteryManager.BATTERY_STATUS_NOT_CHARGING;

            if (isNotCharging)
                finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mDateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        mDateFormatForAccessibility = getString(R.string.full_wday_month_day_no_year);

        layoutClockSaver();
        mHandler.post(mMoveSaverRunnable);

    }

    @Override
    public void onPause() {
        mHandler.removeCallbacks(mMoveSaverRunnable);
        super.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (DEBUG)
            Log.d(TAG, "Screensaver config changed");
        super.onConfigurationChanged(newConfig);
        mHandler.removeCallbacks(mMoveSaverRunnable);
        layoutClockSaver();
        mHandler.postDelayed(mMoveSaverRunnable, 250);
    }

    @Override
    public void onUserInteraction() {
        finish();
    }

    private void setClockStyle() {
        Utils.setClockStyle(this, mDigitalClock, mAnalogClock, ScreensaverSettingsActivity.KEY_CLOCK_STYLE);
        mSaverView = findViewById(R.id.main_clock);
        int brightness = PreferenceManager.getDefaultSharedPreferences(this).getInt(
                ScreensaverSettingsActivity.KEY_BRIGHTNESS,
                ScreensaverSettingsActivity.BRIGHTNESS_DEFAULT);
        Utils.dimView(brightness, mSaverView);

        String size = PreferenceManager.getDefaultSharedPreferences(this).getString(
                ScreensaverSettingsActivity.KEY_CLOCK_SIZE,
                ScreensaverSettingsActivity.SIZE_DEFAULT);
        Utils.resizeContent((ViewGroup) mSaverView, size);

        boolean dim = brightness < ScreensaverSettingsActivity.BRIGHTNESS_NIGHT;
        if (dim) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.buttonBrightness = 0;
            lp.screenBrightness = 0.01f;
            getWindow().setAttributes(lp);
        }

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void layoutClockSaver() {
        setContentView(R.layout.desk_clock_saver);
        mDigitalClock = findViewById(R.id.digital_clock);
        mAnalogClock = findViewById(R.id.analog_clock);
        setClockStyle();
        mContentView = (View) mSaverView.getParent();
        mContentView.forceLayout();
        mSaverView.forceLayout();
        mSaverView.setAlpha(0);

        mMoveSaverRunnable.registerViews(mContentView, mSaverView);

        Utils.hideSystemUiAndRetry(mContentView);
        Utils.updateDate(mDateFormat, mDateFormatForAccessibility, mContentView);
        Utils.refreshAlarm(ScreensaverActivity.this, mContentView);
    }

    @Override
    protected void updateViews() {
        Utils.updateDate(mDateFormat, mDateFormatForAccessibility, mContentView);
        Utils.refreshAlarm(ScreensaverActivity.this, mContentView);
    }

    @Override
    protected int getAdditionalFlags() {
        return WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
    }
}
