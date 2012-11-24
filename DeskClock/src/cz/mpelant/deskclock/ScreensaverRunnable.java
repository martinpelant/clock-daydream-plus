
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
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

/**
 * Runnable for use with screensaver and dream, to move the clock every minute.
 * registerViews() must be called prior to posting.
 */
public class ScreensaverRunnable implements Runnable {
    static final long MOVE_DELAY = 60000; // DeskClock.SCREEN_SAVER_MOVE_DELAY;
    static final long SLIDE_TIME = 10000;
    static final long FADE_TIME = 3000;

    static final boolean SLIDE = false;

    private View mContentView, mSaverView;
    private TextView mDate;
    private View mNotifGmail;
    private View mNotifMessage;
    private View mMissedCall;
    private TextView mNextAlarm;
    private final Handler mHandler;

    private static TimeInterpolator mSlowStartWithBrakes;

    public ScreensaverRunnable(Handler handler) {
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
        mNotifGmail = contentView.findViewById(R.id.gmail);
        mNotifMessage = contentView.findViewById(R.id.messages);
        mMissedCall = contentView.findViewById(R.id.missedCalls);
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

            new Thread() {
                public void run() {
                    if (isPrefEnabled(ScreensaverSettingsActivity.KEY_NOTIF_GMAIL, true))
                        checkGmail(mDate.getContext(), mNotifGmail);
                    if (isPrefEnabled(ScreensaverSettingsActivity.KEY_NOTIF_SMS, true))
                        checkSMS(mDate.getContext(), mNotifMessage);
                    if (isPrefEnabled(ScreensaverSettingsActivity.KEY_NOTIF_MISSED_CALLS, true))
                        checkMissedCalls(mDate.getContext(), mMissedCall);
                };
            }.start();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public boolean isPrefEnabled(String prefName, boolean defValue) {
        return PreferenceManager.getDefaultSharedPreferences(mDate.getContext()).getBoolean(prefName, defValue);
    }

    public static final class LabelColumns {
        public static final String CANONICAL_NAME = "canonicalName";
        public static final String NAME = "name";
        public static final String NUM_CONVERSATIONS = "numConversations";
        public static final String NUM_UNREAD_CONVERSATIONS = "numUnreadConversations";
    }

    private static void checkGmail(final Context context, final View image) {
        // Get the account list, and pick the first one
        final String ACCOUNT_TYPE_GOOGLE = "com.google";
        final String[] FEATURES_MAIL = {
            "service_mail"
        };
        AccountManager.get(context).getAccountsByTypeAndFeatures(ACCOUNT_TYPE_GOOGLE, FEATURES_MAIL, new AccountManagerCallback<Account[]>() {
            @Override
            public void run(AccountManagerFuture<Account[]> future) {
                Account[] accounts = null;
                try {
                    accounts = (Account[]) future.getResult();
                    if (accounts != null && accounts.length > 0) {
                        String selectedAccount = accounts[0].name;
                        queryLabels(selectedAccount, context);
                    }

                } catch (Exception e) {
                    // catch (OperationCanceledException oce) {

                    // // TODO: handle exception
                    // } catch (IOException ioe) {
                    // } catch (AuthenticatorException ae) {

                    // }
                    e.printStackTrace();
                }
            }

            private void queryLabels(String selectedAccount, Context context) {
                Log.d("Gmail - " + selectedAccount);
                Cursor labelsCursor = context.getContentResolver().query(GmailContract.Labels.getLabelsUri(selectedAccount), null, null, null, null);
                labelsCursor.moveToFirst();
                do {

                    String name = labelsCursor.getString(labelsCursor.getColumnIndex(GmailContract.Labels.NAME));
                    int unread = labelsCursor.getInt(labelsCursor.getColumnIndex(GmailContract.Labels.NUM_UNREAD_CONVERSATIONS));// here's the value you need
                    if (name.equals("Inbox") && unread > 0) {
                        Log.d("Gmail - " + name + "-" + unread);
                        setViewVisibility(image, View.VISIBLE);
                        return;
                    }
                } while (labelsCursor.moveToNext());
                setViewVisibility(image, View.GONE);
            }
        }, null /* handler */);
    }

    private static void setViewVisibility(final View view, final int visibility) {
        view.post(new Runnable() {

            @Override
            public void run() {
                view.setVisibility(visibility);
            }
        });
    }

    private static void checkSMS(Context context, View image) {
        try {
            Uri uriSMSURI = Uri.parse("content://sms/inbox");
            Cursor cur = context.getContentResolver().query(uriSMSURI, null, "read = 0", null, null);
            Log.d("SMS - " + cur.getCount());
            if (cur.getCount() > 0) {
                image.setVisibility(View.VISIBLE);
            } else {
                image.setVisibility(View.GONE);
            }
            cur.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void checkMissedCalls(Context context, View image) {
        final String[] projection = null;
        final String selection = CallLog.Calls.TYPE + "=" + CallLog.Calls.MISSED_TYPE + " AND " + CallLog.Calls.IS_READ + "=0";
        final String[] selectionArgs = null;
        final String sortOrder = null;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, selection, selectionArgs, sortOrder);
            if (cursor.getCount() > 0) {
                image.setVisibility(View.VISIBLE);
            } else {
                image.setVisibility(View.GONE);
            }
        } catch (Exception ex) {
            Log.e("ERROR: " + ex.toString());
        } finally {
            cursor.close();
        }
    }

}
