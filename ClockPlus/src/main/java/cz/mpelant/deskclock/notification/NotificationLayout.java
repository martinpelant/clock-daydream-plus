package cz.mpelant.deskclock.notification;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import cz.mpelant.deskclock.R;

import java.util.HashSet;
import java.util.Set;

/**
 * NotificationLayout.java
 *
 * @author eMan s.r.o.
 * @project clock-daydream-plus
 * @package cz.mpelant.deskclock
 * @since 9/1/13
 */
public class NotificationLayout extends LinearLayout {
    private Set<NotificationInfo> mNotificationInfos;
    private Set<NotificationInfo> mTmpNotificationInfos;
    private static final int MAX_ICONS_PER_ROW = 5;

    public NotificationLayout(Context context) {
        super(context);
        init();

    }

    public NotificationLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        mNotificationInfos = new HashSet<NotificationInfo>();
        mTmpNotificationInfos = new HashSet<NotificationInfo>();
        setGravity(Gravity.CENTER_HORIZONTAL);
    }

    /**
     * removes all notification icons
     * can be called from background thread
     * must call notifyDatasetChanged for the change to take effect
     */
    public void clear() {
        mTmpNotificationInfos.clear();
    }

    /**
     * adds new notification
     * can be called from background thread
     * must call notifyDatasetChanged for the change to take effect
     * @param notif
     */
    public void addNotification(NotificationInfo notif) {

        if (notif != null) {
            Log.d("notif", "Notif add");
            mTmpNotificationInfos.add(notif);
        }
    }

    /**
     * must be called from main thread
     */
    public void notifyDatasetChanged() {
        Log.d("notif", "notifyDatasetChanged");
        mNotificationInfos.clear();//addNotification and clear can be called from background thread
        mNotificationInfos.addAll(mTmpNotificationInfos);
        removeAllViews();
        int itemsInRow = 0;
        LinearLayout linearLayout = new LinearLayout(getContext());

        for (NotificationInfo notif : mNotificationInfos) {
            int size = (int) getContext().getResources().getDimension(R.dimen.notif_size);
            int margin = (int) getContext().getResources().getDimension(R.dimen.notif_margin);
            LinearLayout.LayoutParams lp = new LayoutParams(size, size);
            lp.setMargins(margin, margin, margin, margin);
            linearLayout.addView(notif.generateView(getContext()), lp);
            itemsInRow++;
            if (itemsInRow % MAX_ICONS_PER_ROW == 0) {
                addView(linearLayout);
                linearLayout = new LinearLayout(getContext());
            }

        }

        if (linearLayout.getChildCount() > 0) {
            addView(linearLayout);
        }
    }


}
