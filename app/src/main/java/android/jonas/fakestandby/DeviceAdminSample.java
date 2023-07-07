package android.jonas.fakestandby;

import android.app.Activity;
import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

public class DeviceAdminSample extends DeviceAdminReceiver {
    public static CharSequence getComponentName(Activity activity) {
        return "XYZ";
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent customScreenIntent = new Intent(context, CustomActivity.class);
                    customScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(customScreenIntent);
                }
            }, 1000);
        }
        super.onReceive(context, intent);
    }

    public CharSequence onDisableRequested(Context context, Intent intent) {
        // Show a warning message to the user
        return "Disabling this app as a device admin will prevent it from functioning properly. Are you sure you want to proceed?";
    }
}
