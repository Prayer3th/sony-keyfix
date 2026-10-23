package local.sony.keyfix;

import java.io.IOException;

/** Legacy shell streams have no exit packet, so fixed commands append a marker. */
public final class ShellResult {
    private static final String MARKER = "\n__KEYFIX_EXIT__";
    private ShellResult() {}
    public static String wrap(String command) {
        return command + "; printf '\\n__KEYFIX_EXIT__%s\\n' \"$?\"";
    }
    public static String output(String text) throws IOException {
        int end = text.lastIndexOf(MARKER);
        if (end < 0 || !text.substring(end + MARKER.length()).trim().equals("0")) {
            throw new IOException("ADB command failed or was interrupted");
        }
        return text.substring(0, end);
    }
}
