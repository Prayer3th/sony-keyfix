package local.sony.keyfix;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads only Event Hub device blocks, never trusts a persisted event number. */
public final class RemoteDevice {
    private static final Pattern HEADER = Pattern.compile("^    -?\\d+: .*$");
    private static final Pattern PATH = Pattern.compile("^\\s+Path: (/dev/input/event\\d+)\\s*$");
    private RemoteDevice() {}
    public static String find(String listing) {
        String path = null;
        boolean hub = false;
        for (String line : listing.split("\n")) {
            if (line.equals("Event Hub State:")) { hub = true; continue; }
            if (hub && !line.isEmpty() && !Character.isWhitespace(line.charAt(0))) break;
            if (!hub) continue;
            if (HEADER.matcher(line).matches()) path = null;
            Matcher match = PATH.matcher(line);
            if (match.matches()) path = match.group(1);
            if (line.trim().startsWith("Identifier:") &&
                line.matches(".*\\bbus=0x0005,.*") &&
                line.matches(".*\\bvendor=0x054c,.*") &&
                line.matches(".*\\bproduct=0x0eae,.*")) return path;
        }
        return null;
    }
}
