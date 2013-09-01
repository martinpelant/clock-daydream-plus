
package cz.mpelant.deskclock;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import cz.mpelant.deskclock.notification.NotifCompact;
import cz.mpelant.deskclock.notification.NotificationInfo;
import cz.mpelant.deskclock.notification.NotificationLayout;

import java.util.List;

/**
 * Runnable for use with screensaver and dream, to move the clock every minute.
 * registerViews() must be called prior to posting.
 */
public class ScreensaverMoveSaverRunnable implements Runnable {
    static final long MOVE_DELAY = 60000; // DeskClock.SCREEN_SAVER_MOVE_DELAY;
    static final long SLIDE_TIME = 10000;
    static final long FADE_TIME = 3000;

    static final boolean SLIDE = false;

    private View mContentView, mSaverView;
    private TextView mDate;
    private TextView mBattery;
    private View mBatteryContainer;
    private NotificationLayout mNotifLayout;
    private View mTest;
    private TextView mNextAlarm;
    private final Handler mHandler;
    private NotifCompact mNotifCompact;

    private static TimeInterpolator mSlowStartWithBrakes;

    public ScreensaverMoveSaverRunnable(Handler handler) {
        mHandler = handler;
        mSlowStartWithBrakes = new TimeInterpolator() {
            @Override
            public float getInterpolation(float x) {
                return (float) (Math.cos((Math.pow(x, 3) + 1) * Math.PI) / 2.0f) + 0.5f;
            }
        };
    }

    public void registerViews(View contentView, View saverView) {
        mContentView = contentView;
        mDate = (TextView) contentView.findViewById(R.id.date);
        mBattery = (TextView) contentView.findViewById(R.id.battery);
        mBatteryContainer = contentView.findViewById(R.id.batteryContainer);
        mNotifLayout = (NotificationLayout) contentView.findViewById(R.id.notifLayout);
        mNextAlarm = (TextView) contentView.findViewById(R.id.nextAlarm);
        mSaverView = saverView;
        handleUpdate();
    }

    @Override
    public void run() {
        long delay = MOVE_DELAY;
        if (mContentView == null || mSaverView == null) {
            mHandler.removeCallbacks(this);
            mHandler.postDelayed(this, delay);
            return;
        }

        final float xrange = mContentView.getWidth() - mSaverView.getWidth();
        final float yrange = mContentView.getHeight() - mSaverView.getHeight();
        Log.v("xrange: " + xrange + " yrange: " + yrange);

        if (xrange == 0 && yrange == 0) {
            delay = 500; // back in a split second
        } else {
            final int nextx = (int) (Math.random() * xrange);
            final int nexty = (int) (Math.random() * yrange);

            if (mSaverView.getAlpha() == 0f) {
                // jump right there
                mSaverView.setX(nextx);
                mSaverView.setY(nexty);
                ObjectAnimator.ofFloat(mSaverView, "alpha", 0f, 1f).setDuration(FADE_TIME).start();
            } else {
                AnimatorSet s = new AnimatorSet();
                Animator xMove = ObjectAnimator.ofFloat(mSaverView, "x", mSaverView.getX(), nextx);
                Animator yMove = ObjectAnimator.ofFloat(mSaverView, "y", mSaverView.getY(), nexty);

                Animator xShrink = ObjectAnimator.ofFloat(mSaverView, "scaleX", 1f, 0.85f);
                Animator xGrow = ObjectAnimator.ofFloat(mSaverView, "scaleX", 0.85f, 1f);

                Animator yShrink = ObjectAnimator.ofFloat(mSaverView, "scaleY", 1f, 0.85f);
                Animator yGrow = ObjectAnimator.ofFloat(mSaverView, "scaleY", 0.85f, 1f);
                AnimatorSet shrink = new AnimatorSet();
                shrink.play(xShrink).with(yShrink);
                AnimatorSet grow = new AnimatorSet();
                grow.play(xGrow).with(yGrow);

                Animator fadeout = ObjectAnimator.ofFloat(mSaverView, "alpha", 1f, 0f);
                Animator fadein = ObjectAnimator.ofFloat(mSaverView, "alpha", 0f, 1f);

                if (SLIDE) {
                    s.play(xMove).with(yMove);
                    s.setDuration(SLIDE_TIME);

                    s.play(shrink.setDuration(SLIDE_TIME / 2));
                    s.play(grow.setDuration(SLIDE_TIME / 2)).after(shrink);
                    s.setInterpolator(mSlowStartWithBrakes);
                } else {
                    AccelerateInterpolator accel = new AccelerateInterpolator();
                    DecelerateInterpolator decel = new DecelerateInterpolator();

                    shrink.setDuration(FADE_TIME).setInterpolator(accel);
                    fadeout.setDuration(FADE_TIME).setInterpolator(accel);
                    grow.setDuration(FADE_TIME).setInterpolator(decel);
                    fadein.setDuration(FADE_TIME).setInterpolator(decel);
                    s.play(shrink);
                    s.play(fadeout);
                    s.play(xMove.setDuration(0)).after(FADE_TIME);
                    s.play(yMove.setDuration(0)).after(FADE_TIME);
                    mHandler.postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            handleUpdate();
                        }
                    }, FADE_TIME);

                    s.play(fadein).after(FADE_TIME);
                    s.play(grow).after(FADE_TIME);
                }
                s.start();
            }

            long now = System.currentTimeMillis();
            long adjust = (now % MOVE_DELAY);
            delay = delay + (MOVE_DELAY - adjust) // minute aligned
                    - (SLIDE ? 0 : FADE_TIME) // start moving before the fade
            ;
        }

        mHandler.removeCallbacks(this);
        mHandler.postDelayed(this, delay);
    }

    private void handleUpdate() {
        try {
            Utils.setAlarmTextView(mDate.getContext(), mNextAlarm);
            Utils.setDateTextView(mDate.getContext(), mDate);

            if (isPrefEnabled(ScreensaverSettingsActivity.KEY_BATTERY, true)) {
                mBatteryContainer.setVisibility(View.VISIBLE);
                Utils.setBatteryStatus(mDate.getContext(), mBattery);
            } else {
                mBatteryContainer.setVisibility(View.GONE);
            }

            if (Build.VERSION.SDK_INT >= 18) {
                notifChange();
            } else {
                compatNotifCheck();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void notifChange() {
        if (NotificationListener.instance != null) {
            Log.d("notif listener is running");
            List<NotificationInfo> notifications = NotificationListener.instance.getNotifications();
            Log.d("got " + notifications.size() + " icons");
            mNotifLayout.clear();
            for (NotificationInfo notificationInfo : notifications) {
                mNotifLayout.addNotification(notificationInfo);
            }
            mNotifLayout.notifyDatasetChanged();
        }
    }

    private void compatNotifCheck() {
        if (mNotifCompact == null) {
            mNotifCompact = new NotifCompact();
        }

        mNotifLayout.clear();

        new Thread() {
            @Override
            public void run() {
                try {
                    if (isPrefEnabled(ScreensaverSettingsActivity.KEY_NOTIF_GMAIL, true)) {
                        mNotifLayout.addNotification(mNotifCompact.checkGmail(mDate.getContext()));
                    }
                    if (isPrefEnabled(ScreensaverSettingsActivity.KEY_NOTIF_SMS, true)) {
                        mNotifLayout.addNotification(mNotifCompact.checkSMS(mDate.getContext()));
                    }
                    if (isPrefEnabled(ScreensaverSettingsActivity.KEY_NOTIF_MISSED_CALLS, true)) {
                        mNotifLayout.addNotification(mNotifCompact.checkMissedCalls(mDate.getContext()));
                    }
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mNotifLayout.notifyDatasetChanged();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            ;
        }.start();
    }

    public boolean isPrefEnabled(String prefName, boolean defValue) {
        return PreferenceManager.getDefaultSharedPreferences(mDate.getContext()).getBoolean(prefName, defValue);
    }


}
