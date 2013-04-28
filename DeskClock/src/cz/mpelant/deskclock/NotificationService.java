
package cz.mpelant.deskclock;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RemoteViews;

public class NotificationService extends AccessibilityService {
    public static int icon;
    public static Bitmap largeIcon;
    public static Drawable drawable;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d("onAccessibilityEvent");
        if (event.getEventType() == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            if (event.getParcelableData() instanceof Notification) {
                Notification notification = (Notification) event.getParcelableData();

                Log.d("ticker: " + notification.tickerText);
                Log.d("icon: " + notification.icon);
                Log.d("largeIcon: " + notification.largeIcon);
                largeIcon = notification.largeIcon;
                icon = notification.icon;
                Log.d("contentView: " + notification.contentView);
                extractImage(notification.contentView);

                Log.d("number: " + notification.number);
            }

            Log.d("notification: " + event.getText());
            Log.d("package name: " + event.getPackageName());
        }
    }

    private void extractImage(RemoteViews views) {
        LinearLayout ll = new LinearLayout(getApplicationContext());
        View view = views.apply(getApplicationContext(), ll);
        drawable = searchForBitmap(view);
        Log.d("Drawable: " + drawable);
    }

    private Drawable searchForBitmap(View view) {
        if (view instanceof ImageView) {
            return ((ImageView) view).getDrawable();
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                Drawable result = searchForBitmap(viewGroup.getChildAt(i));
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private void extractImage2(RemoteViews views) {
        Class secretClass = views.getClass();

        try {
            Map<Integer, String> text = new HashMap<Integer, String>();

            Field outerFields[] = secretClass.getDeclaredFields();
            for (int i = 0; i < outerFields.length; i++) {
                if (!outerFields[i].getName().equals("mActions")) {
                    continue;
                }

                outerFields[i].setAccessible(true);

                ArrayList<Object> actions = (ArrayList<Object>) outerFields[i].get(views);
                for (Object action : actions) {
                    Field innerFields[] = action.getClass().getDeclaredFields();

                    Object value = null;
                    Integer type = null;
                    Integer viewId = null;
                    for (Field field : innerFields) {
                        field.setAccessible(true);
                        if (field.getName().equals("value")) {
                            value = field.get(action);
                        } else if (field.getName().equals("type")) {
                            type = field.getInt(action);
                        } else if (field.getName().equals("viewId")) {
                            viewId = field.getInt(action);
                        }
                    }

                    if (type == 9 || type == 10) {
                        text.put(viewId, value.toString());
                    }
                }

                System.out.println("title is: " + text.get(16908310));
                System.out.println("info is: " + text.get(16909082));
                System.out.println("text is: " + text.get(16908358));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onServiceConnected() {
        Log.d("onServiceConnected");
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
        info.notificationTimeout = 100;
        info.feedbackType = AccessibilityEvent.TYPES_ALL_MASK;
        setServiceInfo(info);
    }

    @Override
    public void onInterrupt() {
        Log.d("onInterrupt");
    }
}
