package cz.mpelant.deskclock;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationListener.java
 *
 * @author eMan s.r.o.
 * @project clock-daydream-plus
 * @package cz.mpelant.deskclock
 * @since 9/1/13
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationListener extends NotificationListenerService {
    public static NotificationListener instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {

    }


    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {

    }

    public List<Drawable> getIcons() {
        List<Drawable> icons = new ArrayList<Drawable>();
        StatusBarNotification[] notifs = getActiveNotifications();
        for (StatusBarNotification notif : notifs) {

            try {
                Context ctx = createPackageContext(notif.getPackageName(), 0);
                Drawable d = ctx.getResources().getDrawable(notif.getNotification().icon);
                if (d != null) {
                    icons.add(d);
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

        }
        return icons;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }
}
