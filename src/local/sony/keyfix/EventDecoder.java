package local.sony.keyfix;

/** Decodes 32-bit Linux input_event records, including fragmented ADB packets. */
public final class EventDecoder {
    public interface Listener { void onPress(int usage); }
    private final byte[] record = new byte[16];
    private final Listener listener;
    private int count, usage, held;
    private boolean dropped;
    public EventDecoder(Listener listener) { this.listener = listener; }
    public void accept(byte[] data) {
        for (byte b : data) {
            record[count++] = b;
            if (count == 16) { count = 0; decode(); }
        }
    }
    private int u16(int offset) {
        return (record[offset] & 255) | ((record[offset + 1] & 255) << 8);
    }
    private int i32(int offset) { return u16(offset) | (u16(offset + 2) << 16); }
    private void decode() {
        int type = u16(8), code = u16(10), value = i32(12);
        // After an overflow, discard through the next SYN_REPORT. Only new DOWNs
        // are mapped; reconstructing a held button must never inject an action.
        if (type == 0 && code == 3) { dropped = true; usage = held = 0; return; }
        if (dropped) {
            if (type == 0 && code == 0) dropped = false;
            return;
        }
        if (type == 4 && code == 4) usage = value;
        else if (type == 1 && code == 240) {
            if (value == 0) held = 0;
            else if (value == 1 && usage != 0 && held != usage) {
                held = usage;
                listener.onPress(usage);
            }
        } else if (type == 0) {
            usage = 0;
        }
    }
}
