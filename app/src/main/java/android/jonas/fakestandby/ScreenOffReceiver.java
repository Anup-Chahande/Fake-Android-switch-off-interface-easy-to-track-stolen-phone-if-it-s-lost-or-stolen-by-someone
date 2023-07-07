package android.jonas.fakestandby;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.jonas.fakestandby.settings.SettingsActivity;

public class ScreenOffReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            Intent i = new Intent(context, SettingsActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}