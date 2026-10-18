package com.akashiic.mixer.core;

/**
 * How much of Minecraft's background music is let through while Akashic audio plays. Pure; tested in DuckModelTest.
 */
public final class DuckModel {

    /**
     * Floor of the factor actually applied. Minecraft stops a sound whose volume reaches 0 and the music ticker then
     * waits 10-20 minutes for the next track; at -80 dB the track keeps running inaudibly and comes back when the
     * player walks away.
     */
    public static final float SILENT = 1.0e-4f;

    private DuckModel() {}

    /**
     * The music factor wanted for a given loudest Akashic level: 1 with nothing audible, {@code floor} once the level
     * reaches {@code fullAt}, linear in between (so the music fades in as the player walks away from a source).
     */
    public static float target(float audible, float fullAt, float floor) {
        float f = clamp01(floor);
        if (!(audible > 0f)) return 1f;
        if (!(fullAt > 0f) || audible >= fullAt) return f;
        float a = audible / fullAt;
        return 1f - a * (1f - f);
    }

    /**
     * Moves {@code current} toward {@code target}: downward with {@code fadeOutSeconds}, upward with
     * {@code fadeInSeconds} (time to cover ~95%, exponential).
     */
    public static float step(float current, float target, double dtSeconds, double fadeOutSeconds,
        double fadeInSeconds) {
        double seconds = target < current ? fadeOutSeconds : fadeInSeconds;
        if (seconds <= 0 || dtSeconds <= 0) return dtSeconds <= 0 ? current : target;
        double k = 1 - Math.exp(-3.0 * dtSeconds / seconds);
        float next = (float) (current + (target - current) * k);
        return Math.abs(next - target) < 1.0e-3f ? target : next;
    }

    /** The factor handed to the sound engine: never exactly 0 (see {@link #SILENT}). */
    public static float applied(float factor) {
        return Math.max(SILENT, clamp01(factor));
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
