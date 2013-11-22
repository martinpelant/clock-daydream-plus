/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.content.Context;
import android.provider.Settings;
import android.text.format.DateFormat;

import java.util.Calendar;

/**
 * The Alarms provider supplies info about Alarm Clock settings
 */
public class Alarms {

    static final String PREFERENCES = "AlarmClock";

    // This action triggers the AlarmReceiver as well as the AlarmKlaxon. It
    // is a public action used in the manifest for receiving Alarm broadcasts
    // from the alarm manager.
    public static final String ALARM_ALERT_ACTION = "cz.mpelant.deskclock.ALARM_ALERT";

    // A public action sent by AlarmKlaxon when the alarm has stopped sounding
    // for any reason (e.g. because it has been dismissed from AlarmAlertFullScreen,
    // or killed due to an incoming phone call, etc).
    public static final String ALARM_DONE_ACTION = "cz.mpelant.deskclock.ALARM_DONE";

    // AlarmAlertFullScreen listens for this broadcast intent, so that other applications
    // can snooze the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
    public static final String ALARM_SNOOZE_ACTION = "cz.mpelant.deskclock.ALARM_SNOOZE";

    // AlarmAlertFullScreen listens for this broadcast intent, so that other applications
    // can dismiss the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
    public static final String ALARM_DISMISS_ACTION = "cz.mpelant.deskclock.ALARM_DISMISS";

    // A public action sent by AlarmAlertFullScreen when a snoozed alarm was dismissed due
    // to it handling ALARM_DISMISS_ACTION cancelled
    public static final String ALARM_SNOOZE_CANCELLED = "cz.mpelant.deskclock.ALARM_SNOOZE_CANCELLED";

    // A broadcast sent every time the next alarm time is set in the system
    public static final String NEXT_ALARM_TIME_SET = "cz.mpelant.deskclock.NEXT_ALARM_TIME_SET";

    // This is a private action used by the AlarmKlaxon to update the UI to
    // show the alarm has been killed.
    public static final String ALARM_KILLED = "alarm_killed";

    // Extra in the ALARM_KILLED intent to indicate to the user how long the
    // alarm played before being killed.
    public static final String ALARM_KILLED_TIMEOUT = "alarm_killed_timeout";

    // This string is used to indicate a silent alarm in the db.
    public static final String ALARM_ALERT_SILENT = "silent";

    // This intent is sent from the notification when the user cancels the
    // snooze alert.
    public static final String CANCEL_SNOOZE = "cancel_snooze";

    // This string is used when passing an Alarm object through an intent.
    public static final String ALARM_INTENT_EXTRA = "intent.extra.alarm";

    // This extra is the raw Alarm object data. It is used in the
    // AlarmManagerService to avoid a ClassNotFoundException when filling in
    // the Intent extras.
    public static final String ALARM_RAW_DATA = "intent.extra.alarm_raw";

    private static final String PREF_SNOOZE_IDS = "snooze_ids";
    private static final String PREF_SNOOZE_TIME = "snooze_time";

    private final static String DM12 = "E h:mm aa";
    private final static String DM24 = "E kk:mm";

    private final static String M12 = "h:mm aa";
    // Shared with DigitalClock
    final static String M24 = "kk:mm";

    final static int INVALID_ALARM_ID = -1;

    /* used by AlarmAlert */
    static String formatTime(final Context context, Calendar c) {
        String format = get24HourMode(context) ? M24 : M12;
        return (c == null) ? "" : (String) DateFormat.format(format, c);
    }

    /**
     * Shows day and time -- used for lock screen
     */
    private static String formatDayAndTime(final Context context, Calendar c) {
        String format = get24HourMode(context) ? DM24 : DM12;
        return (c == null) ? "" : (String) DateFormat.format(format, c);
    }

    /**
     * Save time of the next alarm, as a formatted string, into the system
     * settings so those who care can make use of it.
     */
    static void saveNextAlarm(final Context context, String timeString) {
        Settings.System.putString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED, timeString);
    }

    /**
     * @return true if clock is set to 24-hour mode
     */
    public static boolean get24HourMode(final Context context) {
        return android.text.format.DateFormat.is24HourFormat(context);
    }
}
