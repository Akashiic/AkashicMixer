package com.akashiic.mixer.core;

/**
 * Has Windows' default output moved away from the device the sound engine is playing on? Pure; tested in
 * DefaultDeviceFollowerTest.
 * <p>
 * Only a <em>change</em> of the default counts, measured from the default seen when the current engine was first
 * probed. A default that merely differs from the engine's device name (another naming, a device OpenAL could not open)
 * never triggers anything, so a restart can never loop: each new engine starts a new baseline.
 */
public final class DefaultDeviceFollower {

    private Object engine;
    private String baseline;

    /**
     * {@code engine} identifies the running engine (a new one resets the baseline); {@code currentDefault} and
     * {@code engineDevice} are the names OpenAL reports, null when it cannot tell. True when the default moved to a
     * device the engine is not on.
     */
    public boolean moved(Object engine, String currentDefault, String engineDevice) {
        if (engine != this.engine) {
            this.engine = engine;
            baseline = null;
        }
        if (!meaningful(currentDefault)) return false;
        if (baseline == null || same(currentDefault, engineDevice)) {
            baseline = currentDefault.trim();
            return false;
        }
        return !same(currentDefault, baseline);
    }

    /** The default seen when the current engine started (or when it last matched the engine's device). */
    public String baseline() {
        return baseline;
    }

    static boolean same(String a, String b) {
        return meaningful(a) && meaningful(b)
            && a.trim()
                .equalsIgnoreCase(b.trim());
    }

    private static boolean meaningful(String s) {
        return s != null && !s.trim()
            .isEmpty();
    }
}
