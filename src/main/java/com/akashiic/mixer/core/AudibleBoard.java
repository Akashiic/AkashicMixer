package com.akashiic.mixer.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Latest audible level per reporter, with the time it arrived. A report older than {@link #STALE_MS} is silence. */
public final class AudibleBoard {

    public static final long STALE_MS = 1500;

    private final Map<String, long[]> reports = new ConcurrentHashMap<>();

    public void report(String reporter, float level, long nowMs) {
        if (reporter == null) return;
        float l = Float.isNaN(level) ? 0f : Math.max(0f, Math.min(1f, level));
        reports.put(reporter, new long[] { Float.floatToIntBits(l), nowMs });
    }

    public float loudest(long nowMs) {
        float max = 0f;
        for (long[] r : reports.values()) {
            long age = nowMs - r[1];
            if (age < 0 || age > STALE_MS) continue;
            max = Math.max(max, Float.intBitsToFloat((int) r[0]));
        }
        return max;
    }
}
