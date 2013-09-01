package cz.mpelant.deskclock.notification;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.LinkedList;
import java.util.List;

/**
 * NotificationLayout.java
 *
 * @author eMan s.r.o.
 * @project clock-daydream-plus
 * @package cz.mpelant.deskclock
 * @since 9/1/13
 */
public class NotificationLayout extends LinearLayout {
    private List<Notification> mNotifications;
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
        mNotifications = new LinkedList<Notification>();
        setGravity(Gravity.CENTER_HORIZONTAL);
    }

    public void addNotification(Notification notif){
        mNotifications.add(notif);
    }

    public void notifyDatasetChanged() {
        removeAllViews();
        int itemsInRow = 0;
        LinearLayout linearLayout = new LinearLayout(getContext());

        for (Notification notif : mNotifications) {
            linearLayout.addView(notif.generateView(getContext()));
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
