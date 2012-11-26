
package cz.mpelant.deskclock;

import android.app.Application;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Build;

public class App extends Application {
    public static final String TAG = "App";

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 17) {
            Log.i("hiding activity from launcher " + ClockActivity.class.getPackage().getName() + "  " +  ClockActivity.class.getName());
            PackageManager p = getPackageManager();
            p.setComponentEnabledSetting(new ComponentName(ClockActivity.class.getPackage().getName(), ClockActivity.class.getName()), PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
    }
}
