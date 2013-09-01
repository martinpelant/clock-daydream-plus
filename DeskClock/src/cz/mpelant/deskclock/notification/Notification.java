package cz.mpelant.deskclock.notification;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

/**
 * Notification.java
 *
 * @author eMan s.r.o.
 * @project clock-daydream-plus
 * @package cz.mpelant.deskclock.notification
 * @since 9/1/13
 */
public class Notification {
    Drawable mDrawable;

    public Notification(Drawable drawable) {
        mDrawable = drawable;
    }

    public Drawable getDrawable() {
        return mDrawable;
    }

    public View generateView(Context context){
        ImageView img = new ImageView(context);
        img.setImageDrawable(getDrawable());
        return img;
    }
}
