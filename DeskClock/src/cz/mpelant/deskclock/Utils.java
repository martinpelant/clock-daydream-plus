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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class Utils {
    private final static String TAG = Utils.class.getName();

    private final static String PARAM_LANGUAGE_CODE = "hl";

    /**
     * Help URL query parameter key for the app version.
     */
    private final static String PARAM_VERSION = "version";

    /**
     * Cached version code to prevent repeated calls to the package manager.
     */
    private static String sCachedVersionCode = null;

    /**
     * Intent to be used for checking if a clock's date has changed. Must be every fifteen
     * minutes because not all time zones are hour-locked.
     **/
    public static final String ACTION_ON_QUARTER_HOUR = "cz.mpelant.deskclock.ON_QUARTER_HOUR";

    /** Types that may be used for clock displays. **/
    public static final String CLOCK_TYPE_DIGITAL2 = "digital2";
    public static final String CLOCK_TYPE_DIGITAL = "digital";
    public static final String CLOCK_TYPE_ANALOG = "analog";

    /**
     * time format constants
     */
    public final static String HOURS_24 = "kk";
    public final static String HOURS = "h";
    public final static String MINUTES = ":mm";

    public static void prepareHelpMenuItem(Context context, MenuItem helpMenuItem) {
        String helpUrlString = context.getResources().getString(R.string.desk_clock_help_url);
        if (TextUtils.isEmpty(helpUrlString)) {
            // The help url string is empty or null, so set the help menu item to be invisible.
            helpMenuItem.setVisible(false);
            return;
        }
        // The help url string exists, so first add in some extra query parameters. 87
        final Uri fullUri = uriWithAddedParameters(context, Uri.parse(helpUrlString));

        // Then, create an intent that will be fired when the user
        // selects this help menu item.
        Intent intent = new Intent(Intent.ACTION_VIEW, fullUri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        // Set the intent to the help menu item, show the help menu item in the overflow
        // menu, and make it visible.
        helpMenuItem.setIntent(intent);
        helpMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        helpMenuItem.setVisible(true);
    }

    /**
     * Adds two query parameters into the Uri, namely the language code and the version code
     * of the app's package as gotten via the context.
     * 
     * @return the uri with added query parameters
     */
    private static Uri uriWithAddedParameters(Context context, Uri baseUri) {
        Uri.Builder builder = baseUri.buildUpon();

        // Add in the preferred language
        builder.appendQueryParameter(PARAM_LANGUAGE_CODE, Locale.getDefault().toString());

        // Add in the package version code
        if (sCachedVersionCode == null) {
            // There is no cached version code, so try to get it from the package manager.
            try {
                // cache the version code
                PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                sCachedVersionCode = Integer.toString(info.versionCode);

                // append the version code to the uri
                builder.appendQueryParameter(PARAM_VERSION, sCachedVersionCode);
            } catch (NameNotFoundException e) {
                // Cannot find the package name, so don't add in the version parameter
                // This shouldn't happen.
                Log.wtf("Invalid package name for context " + e);
            }
        } else {
            builder.appendQueryParameter(PARAM_VERSION, sCachedVersionCode);
        }

        // Build the full uri and return it
        return builder.build();
    }

    public static long getTimeNow() {
        return SystemClock.elapsedRealtime();
    }

    /**
     * Calculate the amount by which the radius of a CircleTimerView should be offset by the any
     * of the extra painted objects.
     */
    public static float calculateRadiusOffset(float strokeSize, float diamondStrokeSize, float markerStrokeSize) {
        return Math.max(strokeSize, Math.max(diamondStrokeSize, markerStrokeSize));
    }

    /**
     * The pressed color used throughout the app. If this method is changed, it will not have
     * any effect on the button press states, and those must be changed separately.
     **/
    public static int getPressedColorId() {
        return R.color.clock_red;
    }

    /**
     * The un-pressed color used throughout the app. If this method is changed, it will not have
     * any effect on the button press states, and those must be changed separately.
     **/
    public static int getGrayColorId() {
        return R.color.clock_gray;
    }

    /** Setup to find out when the quarter-hour changes (e.g. Kathmandu is GMT+5:45) **/
    public static long getAlarmOnQuarterHour() {
        Calendar nextQuarter = Calendar.getInstance();
        // Set 1 second to ensure quarter-hour threshold passed.
        nextQuarter.set(Calendar.SECOND, 1);
        int minute = nextQuarter.get(Calendar.MINUTE);
        nextQuarter.add(Calendar.MINUTE, 15 - (minute % 15));
        long alarmOnQuarterHour = nextQuarter.getTimeInMillis();
        if (0 >= (alarmOnQuarterHour - System.currentTimeMillis()) || (alarmOnQuarterHour - System.currentTimeMillis()) > 901000) {
            Log.wtf("quarterly alarm calculation error");
        }
        return alarmOnQuarterHour;
    }

    /**
     * For screensavers to set whether the digital or analog clock should be displayed.
     * Returns the view to be displayed.
     */
    public static View setClockStyle(Context context, View digitalClock, View analogClock, String clockStyleKey) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String defaultClockStyle = context.getResources().getString(R.string.default_clock_style);
        String style = sharedPref.getString(clockStyleKey, defaultClockStyle);
        View returnView;
        if (style.equals(CLOCK_TYPE_ANALOG)) {
            digitalClock.setVisibility(View.GONE);
            analogClock.setVisibility(View.VISIBLE);
            returnView = analogClock;
        } else {
            digitalClock.setVisibility(View.VISIBLE);
            analogClock.setVisibility(View.GONE);
            returnView = digitalClock;
            
            if(style.equals(CLOCK_TYPE_DIGITAL)){
                digitalClock.findViewById(R.id.timeDisplayHoursThin).setVisibility(View.GONE);
                digitalClock.findViewById(R.id.timeDisplayHours).setVisibility(View.VISIBLE);
            }else {
                digitalClock.findViewById(R.id.timeDisplayHoursThin).setVisibility(View.VISIBLE);
                digitalClock.findViewById(R.id.timeDisplayHours).setVisibility(View.GONE);
            }
        }

        return returnView;
    }

    /**
     * For screensavers to dim the lights if necessary.
     */
    public static void dimClockView(boolean dim, View clockView) {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setColorFilter(new PorterDuffColorFilter((dim ? 0x60FFFFFF : 0xC0FFFFFF), PorterDuff.Mode.MULTIPLY));
        clockView.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }
    
    
    /**
     * For screensavers to dim the lights if necessary.
     */
    public static void dimView(int dim, View view) {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        dim=dim<<24;
        dim|=0x00FFFFFF;
        paint.setColorFilter(new PorterDuffColorFilter(dim, PorterDuff.Mode.MULTIPLY));
        view.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
    }

    /** Clock views can call this to refresh their alarm to the next upcoming value. **/
    public static void refreshAlarm(Context context, View clock) {
        String nextAlarm = Settings.System.getString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
        TextView nextAlarmView;
        nextAlarmView = (TextView) clock.findViewById(R.id.nextAlarm);
        if (!TextUtils.isEmpty(nextAlarm) && nextAlarmView != null) {
            nextAlarmView.setText(context.getString(R.string.control_set_alarm_with_existing, nextAlarm));
            nextAlarmView.setContentDescription(context.getResources().getString(R.string.next_alarm_description, nextAlarm));
            nextAlarmView.setVisibility(View.VISIBLE);
        } else {
            nextAlarmView.setVisibility(View.GONE);
        }
    }

    /** Clock views can call this to refresh their date. **/
    public static void updateDate(String dateFormat, String dateFormatForAccessibility, View clock) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());

        CharSequence newDate = DateFormat.format(dateFormat, cal);
        TextView dateDisplay;
        dateDisplay = (TextView) clock.findViewById(R.id.date);
        if (dateDisplay != null) {
            dateDisplay.setVisibility(View.VISIBLE);
            dateDisplay.setText(newDate);
            dateDisplay.setContentDescription(DateFormat.format(dateFormatForAccessibility, cal));
        }
    }

    public static void setAlarmTextView(Context context, TextView alarm) {
        String nextAlarm = Settings.System.getString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED);
        if (nextAlarm==null || nextAlarm.isEmpty()) {
            alarm.setVisibility(View.GONE);
        } else {
            alarm.setVisibility(View.VISIBLE);
            alarm.setText(nextAlarm);
        }
    }

    public static void setDateTextView(Context context, TextView dateView) {
        dateView.setText(new SimpleDateFormat(context.getString(R.string.abbrev_wday_month_day_no_year)).format(new Date()));
    }

    public static void setBatteryStatus(Context context, TextView batteryView) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        // Are we charging / charged?
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;

        // How are we charging?
        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level / (float) scale*100;

        String text = "";
        if (status == BatteryManager.BATTERY_STATUS_FULL) {
            text += context.getString(R.string.battery_full);
        } else {
            if (isCharging) {
                text += context.getString(R.string.battery_charging);
                if (usbCharge)
                    text += context.getString(R.string._usb_);
                if (acCharge)
                    text += context.getString(R.string._ac_);
                text += ", ";
            }
            text += (int)batteryPct + "%";
        }
        batteryView.setText(text);

    }

    public static Intent getAlarmPackage(Context context) {
        // Verify clock implementation
        String clockImpls[][] = {
                {
                        "JellyBean Alarm Clock", "com.google.android.deskclock", "com.android.deskclock.AlarmClock"
                }, {
                        "HTC Alarm Clock", "com.htc.android.worldclock", "com.htc.android.worldclock.WorldClockTabControl"
                }, {
                        "Standard Alarm Clock", "com.android.deskclock", "com.android.deskclock.AlarmClock"
                }, {
                        "Froyo Nexus Alarm Clock", "com.google.android.deskclock", "com.android.deskclock.DeskClock"
                }, {
                        "Moto Blur Alarm Clock", "com.motorola.blur.alarmclock", "com.motorola.blur.alarmclock.AlarmClock"
                }, {
                        "Samsung Galaxy Clock", "com.sec.android.app.clockpackage", "com.sec.android.app.clockpackage.ClockPackage"
                }
        };

        PackageManager manager = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);

        for (int i = 0; i < clockImpls.length; i++) {
            try {
                ComponentName c = new ComponentName(clockImpls[i][1], clockImpls[i][2]);
                manager.getActivityInfo(c, PackageManager.GET_META_DATA);
                intent.setComponent(c);
                return intent;
            } catch (NameNotFoundException nf) {
                Log.e("Caught name not found exception!" + clockImpls[i][0]);
                continue;
            }
        }

        return null;
    }

}
