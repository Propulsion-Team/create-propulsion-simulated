package dev.propulsionteam.propulsionsimulated.content.lidar;

public enum LidarDetection {
    BLOCKS("blocks", true),
    FLUIDS("fluids", false),
    NON_SOLID("non_solid", false),
    SEE_THROUGH("see_through", true),
    ENTITIES("entities", true),
    PLAYERS("players", true),
    SHIPS("ships", true),
    OWN_SHIP("own_ship", false),
    PRECISE("precise", true);

    public static final int ALL = (1 << values().length) - 1;
    public static final int DEFAULTS = defaults();

    public final String id;
    private final boolean enabledByDefault;

    LidarDetection(String id, boolean enabledByDefault) {
        this.id = id;
        this.enabledByDefault = enabledByDefault;
    }

    public int bit() {
        return 1 << ordinal();
    }

    public boolean in(int flags) {
        return (flags & bit()) != 0;
    }

    private static int defaults() {
        int flags = 0;
        for (LidarDetection detection : values()) if (detection.enabledByDefault) flags |= detection.bit();
        return flags;
    }
}
