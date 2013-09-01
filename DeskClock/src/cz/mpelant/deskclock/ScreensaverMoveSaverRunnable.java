
package cz.mpelant.deskclock;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

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
    private View mNotifGmail;
    private View mNotifMessage;
    private View mMissedCall;
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
        mNotifGmail = contentView.findViewById(R.id.gmail);
        mNotifMessage = contentView.findViewById(R.id.messages);
        mMissedCall = contentView.findViewById(R.id.missedCalls);
        mTest = contentView.findViewById(R.id.test);
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

    }

    private void compatNotifCheck() {
        if (mNotifCompact == null) {
            mNotifCompact = new NotifCompact();
        }

        new Thread() {
            @Override
            public void run() {
                try {
                    if (isPrefEnabled(ScreensaverSettingsActivity.KEY_NOTIF_GMAIL, true)) {
                        mNotifCompact.checkGmail(mDate.getContext(), mNotifGmail);
                    }
                    if (isPrefEnabled(ScreensaverSettingsActivity.KEY_NOTIF_SMS, true)) {
                        mNotifCompact.checkSMS(mDate.getContext(), mNotifMessage);
                    }
                    if (isPrefEnabled(ScreensaverSettingsActivity.KEY_NOTIF_MISSED_CALLS, true)) {
                        mNotifCompact.checkMissedCalls(mDate.getContext(), mMissedCall);
                    }
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


    private void checkOther(Context context, final View image) {
        try {
            if (NotificationListener.instance != null) {
                Log.d("notif listener is running");
                final List<Drawable> icons = NotificationListener.instance.getIcons();
                Log.d("got " + icons.size() + " icons");
                if (!icons.isEmpty()) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            ((ImageView) image).setImageDrawable(icons.get(0));
                        }
                    });

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
