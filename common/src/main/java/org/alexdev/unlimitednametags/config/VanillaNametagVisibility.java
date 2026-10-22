package org.alexdev.unlimitednametags.config;

/** Vanilla Java's ordinary player-name distance limits, measured from the owner. */
public final class VanillaNametagVisibility {
    public static final double NORMAL_RANGE = 64.0;
    private static final double NORMAL_RANGE_SQUARED = NORMAL_RANGE * NORMAL_RANGE;
    private static final double SNEAK_RANGE_SQUARED = 32.0 * 32.0;

    private VanillaNametagVisibility() {
    }

    public static boolean isWithinRange(double distanceSquared, boolean sneaking) {
        return Double.isFinite(distanceSquared) && distanceSquared >= 0
                && (sneaking ? distanceSquared < SNEAK_RANGE_SQUARED
                            : distanceSquared <= NORMAL_RANGE_SQUARED);
    }
}
