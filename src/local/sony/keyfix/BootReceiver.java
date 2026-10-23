package local.sony.keyfix;
import android.content.*;
public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("config", Context.MODE_PRIVATE);
        if (StartupPolicy.shouldStart(intent.getAction(), prefs.getBoolean("enabled", false),
            prefs.getBoolean("boot", false))) {
            context.startForegroundService(new Intent(context, RemapService.class));
        }
    }
}
