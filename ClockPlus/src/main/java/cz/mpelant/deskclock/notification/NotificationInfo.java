package cz.mpelant.deskclock.notification;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

/**
 * NotificationInfo.java
 *
 * @author eMan s.r.o.
 * @project clock-daydream-plus
 * @package cz.mpelant.cz.mpelant.deskclock.notification
 * @since 9/1/13
 */
public class NotificationInfo {
    Drawable mDrawable;
    String mId;

    public NotificationInfo(Context ctx, String pkg, Notification notification) throws PackageManager.NameNotFoundException, IconNotFoundException {

        Context remoteCtx = ctx.createPackageContext(pkg, 0);
        try {
            mDrawable = remoteCtx.getResources().getDrawable(notification.icon);
        }catch (Resources.NotFoundException ignored){

        }

        if(mDrawable==null){
            throw new IconNotFoundException();
        }
        mId=pkg;
    }
    public NotificationInfo(Context ctx, int iconResId){
        mId="notificationInternal"+iconResId;
        mDrawable=ctx.getResources().getDrawable(iconResId);
    }

    public Drawable getDrawable() {
        return mDrawable;
    }

    public String getId() {
        return mId;
    }

    public View generateView(Context context){
        ImageView img = new ImageView(context);
        img.setImageDrawable(getDrawable());
        return img;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NotificationInfo that = (NotificationInfo) o;

        if (!mId.equals(that.mId)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return mId.hashCode();
    }
}
