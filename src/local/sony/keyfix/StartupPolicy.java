package local.sony.keyfix;

public final class StartupPolicy {
    private StartupPolicy() {}
    public static boolean shouldStart(String action, boolean enabled, boolean boot) {
        return enabled && ("android.intent.action.MY_PACKAGE_REPLACED".equals(action) ||
            (boot && "android.intent.action.BOOT_COMPLETED".equals(action)));
    }
}
