/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.preference.*;
import android.util.Log;
import android.widget.Toast;

/**
 * Settings for the Alarm Clock Dream (cz.mpelant.cz.mpelant.deskclock.Screensaver).
 */
public class ScreensaverSettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    static final String KEY_CLOCK_STYLE = "screensaver_clock_style";
    static final String KEY_CLOCK_SIZE = "screensaver_clock_size";
    //    static final String KEY_NIGHT_MODE = "screensaver_night_mode";
    static final String KEY_BRIGHTNESS = "brightness";
    static final int BRIGHTNESS_DEFAULT = 192;
    static final int BRIGHTNESS_NIGHT = 96;
    static final String SIZE_DEFAULT = "medium";
    static final String KEY_NOTIF_LISTENER = "notif_listener";
    static final String KEY_NOTIF_GMAIL = "notif_gmail";
    static final String KEY_NOTIF_SMS = "notif_sms";
    static final String KEY_ORIENTATION = "orientation";
    static final String KEY_NOTIF_MISSED_CALLS = "notif_missed_calls";
    static final String KEY_HIDE_ACTIVITY = "hide_activity";
    static final String KEY_BATTERY = "battery";
    static final String KEY_ABOUT = "about";
    static final long TIP_DELAY = 1000 * 3600 * 24; // 24h
    public static final int REQUEST_CODE_NOTIF = 1;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.dream_settings);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (System.currentTimeMillis() - sp.getLong("tip", 0) > TIP_DELAY) {
            sp.edit().putLong("tip", System.currentTimeMillis()).commit();
//            Toast.makeText(this, R.string.tip_unread_gmail, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        if (KEY_CLOCK_STYLE.equals(pref.getKey()) || KEY_CLOCK_SIZE.equals(pref.getKey())) {
            final ListPreference listPref = (ListPreference) pref;
            final int idx = listPref.findIndexOfValue((String) newValue);
            listPref.setSummary(listPref.getEntries()[idx]);
        } else if (KEY_HIDE_ACTIVITY.equals(pref.getKey())) {
            int state = !((CheckBoxPreference) pref).isChecked() ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
            if (Build.VERSION.SDK_INT >= 17) {
                PackageManager p = getPackageManager();
                p.setComponentEnabledSetting(new ComponentName(ClockActivity.class.getPackage().getName(), ClockActivity.class.getName()), state, PackageManager.DONT_KILL_APP);
                Toast.makeText(this, R.string.restart_required, Toast.LENGTH_LONG).show();
            }

        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private void refresh() {
        ListPreference listPref;
        Preference pref;

        listPref = (ListPreference) findPreference(KEY_CLOCK_STYLE);
        listPref.setSummary(listPref.getEntry());
        listPref.setOnPreferenceChangeListener(this);

        listPref = (ListPreference) findPreference(KEY_CLOCK_SIZE);
        listPref.setSummary(listPref.getEntry());
        listPref.setOnPreferenceChangeListener(this);

        pref = findPreference(KEY_BRIGHTNESS);
        pref.setOnPreferenceChangeListener(this);

        pref = findPreference(KEY_NOTIF_GMAIL);
        if (pref != null)
            pref.setOnPreferenceChangeListener(this);

        pref = findPreference(KEY_NOTIF_SMS);
        if (pref != null)
            pref.setOnPreferenceChangeListener(this);


        pref = findPreference(KEY_NOTIF_MISSED_CALLS);
        if (pref != null)
            pref.setOnPreferenceChangeListener(this);


        pref = findPreference(KEY_NOTIF_LISTENER);
        if (pref != null) {
            try {
                ((CheckBoxPreference) pref).setChecked(NotificationListener.instance != null);
                pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent i = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                        startActivityForResult(i, REQUEST_CODE_NOTIF);
                        Toast.makeText(ScreensaverSettingsActivity.this, getString(R.string.enable_loc_listener_tip), Toast.LENGTH_SHORT).show();
                        return true;
                    }
                });
            }catch (NoClassDefFoundError error){//weird SGSIII error - pref should be null because it is only in xml-v18
                error.printStackTrace();
            }

        }

        pref = findPreference(KEY_BATTERY);
        pref.setOnPreferenceChangeListener(this);

        pref = findPreference(KEY_ORIENTATION);
        pref.setOnPreferenceChangeListener(this);

        pref = findPreference(KEY_HIDE_ACTIVITY);
        if (Build.VERSION.SDK_INT < 17) {
            pref.setEnabled(false);
            pref.setSelectable(false);
            pref.setSummary(R.string.action_not_available_in_this_android);
        } else {
            pref.setOnPreferenceChangeListener(this);
        }

        pref = findPreference(KEY_ABOUT);

        String versionName = getVersionName(this);
        int versionNumber = getVersionCode(this);
        pref.setSummary("Version" + " " + versionName + " (" + String.valueOf(versionNumber) + ")");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_NOTIF) {
            refresh();
        }
    }

    /**
     * Gets version code of given application.
     *
     * @param context
     * @return
     */
    public static int getVersionCode(Context context) {
        PackageInfo pinfo;
        try {
            pinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            int versionNumber = pinfo.versionCode;
            return versionNumber;
        } catch (NameNotFoundException e) {
            Log.e(context.getApplicationInfo().name, "Version code not available.");
        }
        return 0;
    }

    /**
     * Gets version name of given application.
     *
     * @param context
     * @return
     */
    public static String getVersionName(Context context) {
        PackageInfo pinfo;
        try {
            pinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String versionName = pinfo.versionName;
            return versionName;
        } catch (NameNotFoundException e) {
            Log.e(context.getApplicationInfo().name, "Version name not available.");
        }
        return null;
    }

}
