package local.sony.keyfix;

/** The complete command allowlist. Unknown usages never become shell arguments. */
public final class KeyMapping {
    private KeyMapping() {}
    public static int androidKey(int usage) {
        switch (usage) {
            case 0x000c0533: return 178;
            case 0x000c03c3: return 189;
            case 0x000c051f:
            case 0x000c038b: return 82;
            default: return 0;
        }
    }
}
