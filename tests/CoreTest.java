import local.sony.keyfix.*;
import java.io.IOException;

public final class CoreTest {
    private static void check(boolean condition) { if (!condition) throw new AssertionError(); }
    private static String device(int id, String path, String product) {
        return "    " + id + ": Remote\n      Path: " + path + "\n      Identifier: bus=0x0005, vendor=0x054c, product=" + product + ", version=0x0011\n";
    }
    public static void main(String[] args) throws Exception {
        check(KeyMapping.androidKey(0xc0533) == 178);
        check(KeyMapping.androidKey(0xc03c3) == 189);
        check(KeyMapping.androidKey(0xc051f) == 82 && KeyMapping.androidKey(0xc038b) == 82);
        check(KeyMapping.androidKey(0) == 0 && KeyMapping.androidKey(0xc0040) == 0);
        String prefix = "Event Hub State:\n  Devices:\n";
        check("/dev/input/event3".equals(RemoteDevice.find(prefix + device(2,"/dev/input/event2","0x0001") + device(3,"/dev/input/event3","0x0eae"))));
        check(RemoteDevice.find(prefix + device(2,"/dev/input/event2","0x0001") + device(3,"<virtual>","0x0eae")) == null);
        check(RemoteDevice.find(prefix + device(3,"/dev/input/event3; reboot","0x0eae")) == null);
        check(RemoteDevice.find(prefix + device(3,"/dev/input/event3","0x0eae1")) == null);
        check(RemoteDevice.find("Input Reader State:\n" + device(3,"/dev/input/event3","0x0eae")) == null);
        for (boolean enabled : new boolean[]{true,false}) {
            for (boolean boot : new boolean[]{true,false}) {
                check(StartupPolicy.shouldStart("android.intent.action.BOOT_COMPLETED",enabled,boot) == (enabled && boot));
                check(StartupPolicy.shouldStart("android.intent.action.MY_PACKAGE_REPLACED",enabled,boot) == enabled);
                check(!StartupPolicy.shouldStart("arbitrary",enabled,boot));
            }
        }
        check(ShellResult.output("device\n\n__KEYFIX_EXIT__0\n").equals("device\n"));
        for (String bad : new String[]{"", "partial", "\n__KEYFIX_EXIT__1\n", "\n__KEYFIX_EXIT__0\nextra"}) {
            try { ShellResult.output(bad); throw new AssertionError("Accepted failed shell"); }
            catch (IOException expected) {}
        }
        System.out.println("PASS: four mappings, strict device discovery, restart/update policy, shell failure detection");
    }
}
