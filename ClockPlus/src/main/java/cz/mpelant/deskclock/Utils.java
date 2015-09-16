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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

    public static final String CLOCK_SIZE_SMALL = "small";
    public static final String CLOCK_SIZE_MEDIUM = "medium";
    public static final String CLOCK_SIZE_LARGE = "large";

    public enum eTextSize {
        Small,
        Medium,
        Large,
    }

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


    public static void cancelAlarmOnQuarterHour(Context context, PendingIntent quarterlyIntent) {
        if (quarterlyIntent != null && context != null) {
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).cancel(
                    quarterlyIntent);
        }
    }


    /**
     * Setup alarm refresh when the quarter-hour changes *
     */
    public static PendingIntent startAlarmOnQuarterHour(Context context) {
        if (context != null) {
            PendingIntent quarterlyIntent = PendingIntent.getBroadcast(
                    context, 0, new Intent(Utils.ACTION_ON_QUARTER_HOUR), 0);
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setRepeating(
                    AlarmManager.RTC, getAlarmOnQuarterHour(),
                    AlarmManager.INTERVAL_FIFTEEN_MINUTES, quarterlyIntent);
            return quarterlyIntent;
        } else {
            return null;
        }
    }

    public static PendingIntent refreshAlarmOnQuarterHour(
            Context context, PendingIntent quarterlyIntent) {
        cancelAlarmOnQuarterHour(context, quarterlyIntent);
        return startAlarmOnQuarterHour(context);
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
                {"HTC Alarm Clock", "com.htc.android.worldclock",
                        "com.htc.android.worldclock.WorldClockTabControl" },
                {"Standar Alarm Clock", "com.android.deskclock",
                        "com.android.deskclock.AlarmClock"},
                {"Froyo Nexus Alarm Clock", "com.google.android.deskclock",
                        "com.android.deskclock.DeskClock"},
                {"Moto Blur Alarm Clock", "com.motorola.blur.alarmclock",
                        "com.motorola.blur.alarmclock.AlarmClock"},
                {"Samsung Galaxy Clock", "com.sec.android.app.clockpackage",
                        "com.sec.android.app.clockpackage.ClockPackage"} ,
                {"Sony Ericsson Xperia Z", "com.sonyericsson.organizer",
                        "com.sonyericsson.organizer.Organizer_WorldClock" },
                {"ASUS Tablets", "com.asus.deskclock",
                        "com.asus.deskclock.DeskClock"},
        };

        PackageManager manager = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);

        for (int i = 0; i < clockImpls.length; i++) {

            ComponentName c = new ComponentName(clockImpls[i][1], clockImpls[i][2]);
            intent.setComponent(c);

            if (isCallable(intent, context))
                return intent;
        }

        return null;
    }

    public static boolean isCallable(Intent intent, Context context) {
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

    /**
     * <p>Set the screen in immersive mode or low profile depending of the Android version.
     * Retry a second time 2 seconds later to fix an issue on Android 5.0 and 5.1.</p>
     */
    public static void hideSystemUiAndRetry(final View view) {
        hideSystemUI(view);

        new CountDownTimer(2000, 2000) {
            public void onTick(long millisUntilFinished) {}
            public void onFinish() { hideSystemUI(view); }
        }.start();
    }

    /**
     * <p>Set the screen in immersive mode or low profile depending of the Android version.</p>
     */
    public static void hideSystemUI(View view) {
        if(Build.VERSION.SDK_INT>=19){
            view.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }else{
            view.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }


    /**
     * <p>Resize the TextView fields to requested size.
     * There is no mechanism to prevent overflow off the screen.
     * Size can be small, medium or large.</p>
     */
    public static void resizeContent(ViewGroup parent, String size) {
        float resizeRatio;

        switch (size) {
            case CLOCK_SIZE_SMALL:
                resizeRatio = (float)0.85;
                break;
            case CLOCK_SIZE_LARGE:
                resizeRatio = (float)1.15;
                break;
            default:
                resizeRatio = 1;
        }

        // First adjustment in size
        resizeContent(parent, resizeRatio);
    }

    /**
     * <p>Resize the TextView fields to requested size.
     * There is no mechanism to prevent overflow off the screen.</p>
     */
    public static void resizeContent(ViewGroup parent, float resizeRatio) {
        for (int i = parent.getChildCount() - 1; i >= 0; i--) {
            final View child = parent.getChildAt(i);
            if (child instanceof ViewGroup) {
                resizeContent((ViewGroup) child, resizeRatio);
                // DO SOMETHING WITH VIEWGROUP, AFTER CHILDREN HAS BEEN LOOPED
            } else {
                if (child != null) {
                    // DO SOMETHING WITH VIEW
                    if (child instanceof TextView)  {
                        float textSize = ((TextView) child).getTextSize();
                        float newTextSize = textSize * resizeRatio;

                        ((TextView) child).setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
                    }
                }
            }
        }
    }




}
