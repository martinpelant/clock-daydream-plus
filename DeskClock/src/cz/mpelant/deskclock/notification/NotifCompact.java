package cz.mpelant.deskclock.notification;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.CallLog;
import android.view.View;
import cz.mpelant.deskclock.GmailContract;
import cz.mpelant.deskclock.Log;

/**
 * NotifCompact.java
 *
 * @author eMan s.r.o.
 * @project clock-daydream-plus
 * @package cz.mpelant.deskclock
 * @since 9/1/13
 */
public class NotifCompact {
    private Handler mHandler;

    public NotifCompact() {
        mHandler = new Handler();
    }

    public static final class LabelColumns {
        public static final String CANONICAL_NAME = "canonicalName";
        public static final String NAME = "name";
        public static final String NUM_CONVERSATIONS = "numConversations";
        public static final String NUM_UNREAD_CONVERSATIONS = "numUnreadConversations";
    }

    public void checkGmail(final Context context, final View image) {
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
                    setViewVisibility(image, View.GONE);
                    accounts = future.getResult();
                    if (accounts != null && accounts.length > 0) {
                        for (Account account : accounts) {
                            String selectedAccount = account.name;
                            queryLabels(selectedAccount, context);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            private void queryLabels(String selectedAccount, Context context) {
                Log.d("Gmail - " + selectedAccount);
                Cursor labelsCursor = context.getContentResolver().query(GmailContract.Labels.getLabelsUri(selectedAccount), null, null, null, null);
                labelsCursor.moveToFirst();
                do {
                    String name = labelsCursor.getString(labelsCursor.getColumnIndex(GmailContract.Labels.CANONICAL_NAME));
                    int unread = labelsCursor.getInt(labelsCursor.getColumnIndex(GmailContract.Labels.NUM_UNREAD_CONVERSATIONS));// here's the value you need
                    if (name.equals(GmailContract.Labels.LabelCanonicalNames.CANONICAL_NAME_INBOX) && unread > 0) {
                        Log.d("Gmail - " + name + "-" + unread);
                        setViewVisibility(image, View.VISIBLE);
                        return;
                    }
                } while (labelsCursor.moveToNext());
            }
        }, null /* handler */);
    }


    private void setViewVisibility(final View view, final int visibility) {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                view.setVisibility(visibility);
            }
        });
    }

    public void checkSMS(Context context, View image) {
        try {
            Uri uriSMSURI = Uri.parse("content://sms/inbox");
            Cursor cur = context.getContentResolver().query(uriSMSURI, null, "read = 0", null, null);
            Log.d("SMS - " + cur.getCount());
            if (cur.getCount() > 0) {
                setViewVisibility(image, View.VISIBLE);
            } else {
                setViewVisibility(image, View.GONE);
            }
            cur.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void checkMissedCalls(Context context, View image) {
        final String[] projection = null;
        final String selection = CallLog.Calls.TYPE + "=" + CallLog.Calls.MISSED_TYPE + " AND " + CallLog.Calls.IS_READ + "=0";
        final String[] selectionArgs = null;
        final String sortOrder = null;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, selection, selectionArgs, sortOrder);
            if (cursor.getCount() > 0) {
                setViewVisibility(image, View.VISIBLE);
            } else {
                setViewVisibility(image, View.GONE);
            }
        } catch (Exception ex) {
            Log.e("ERROR: " + ex.toString());
        } finally {
            cursor.close();
        }
    }
}
