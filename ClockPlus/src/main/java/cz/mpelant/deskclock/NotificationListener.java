package cz.mpelant.deskclock;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.pm.PackageManager;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import cz.mpelant.deskclock.notification.IconNotFoundException;
import cz.mpelant.deskclock.notification.NotificationInfo;

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

    public List<NotificationInfo> getNotifications() {
        List<NotificationInfo> notifications = new ArrayList<>();
        StatusBarNotification[] notifs = getActiveNotifications();
        for (StatusBarNotification notif : notifs) {

            try {
                if (notif.getNotification().priority > Notification.PRIORITY_MIN && (notif.getNotification().flags & Notification.FLAG_ONGOING_EVENT) == 0) {
                    notifications.add(new NotificationInfo(this, notif.getPackageName(), notif.getNotification()));
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            } catch (IconNotFoundException e) {
                e.printStackTrace();
            }

        }
        return notifications;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }
}
