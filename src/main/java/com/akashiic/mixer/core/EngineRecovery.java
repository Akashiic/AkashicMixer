package com.akashiic.mixer.core;

/**
 * When to restart Minecraft's sound engine after the audio device is lost. Pure; tested in EngineRecoveryTest.
 * <p>
 * A <em>dead</em> engine is one whose OpenAL library failed to open a device: paulscode keeps it as the active library
 * and Minecraft keeps feeding it, so nothing plays until the engine is restarted (and any OpenAL call into it raises
 * {@code UnsatisfiedLinkError}). It is restarted after {@link #FIRST_DELAY_NANOS}, then with a doubling delay capped at
 * {@link #MAX_DELAY_NANOS}, so the sound comes back within that cap once a device is plugged in again. A
 * <em>disconnected</em> engine (OpenAL alive, the device reports {@code ALC_CONNECTED = false}) is restarted once it
 * has stayed so for {@link #DISCONNECT_GRACE_NANOS}, which leaves the first move to other mods' device checkers.
 */
public final class EngineRecovery {

    public enum Health {
        /** Playing on a connected device (or nothing we can judge): nothing to do. */
        HEALTHY,
        /** Not loaded yet, or a restart is still loading: wait. */
        LOADING,
        /** OpenAL is the active library but has no device. */
        DEAD,
        /** OpenAL alive, its device unplugged. */
        DISCONNECTED
    }

    public static final long FIRST_DELAY_NANOS = 1_000_000_000L;
    public static final long MAX_DELAY_NANOS = 10_000_000_000L;
    public static final long DISCONNECT_GRACE_NANOS = 3_000_000_000L;

    private boolean failing;
    private long nextNanos;
    private long delayNanos;
    private boolean disconnected;
    private long disconnectedSinceNanos;
    private int restarts;

    /**
     * Feeds the latest health; true when the engine should be restarted now (the caller restarts it). A HEALTHY
     * report ends the episode; LOADING keeps the back-off running, so a restart that fails again waits longer.
     */
    public boolean update(Health health, long nowNanos) {
        if (health == null || health == Health.HEALTHY) {
            failing = false;
            disconnected = false;
            restarts = 0;
            return false;
        }
        if (health == Health.LOADING) {
            disconnected = false;
            return false;
        }
        if (health == Health.DISCONNECTED) {
            if (!disconnected) {
                disconnected = true;
                disconnectedSinceNanos = nowNanos;
            }
            if (nowNanos - disconnectedSinceNanos < DISCONNECT_GRACE_NANOS) return false;
        } else {
            disconnected = false;
        }
        if (!failing) {
            failing = true;
            delayNanos = FIRST_DELAY_NANOS;
            nextNanos = health == Health.DEAD ? nowNanos + FIRST_DELAY_NANOS : nowNanos;
        }
        if (nowNanos - nextNanos < 0) return false;
        delayNanos = Math.min(delayNanos * 2, MAX_DELAY_NANOS);
        nextNanos = nowNanos + delayNanos;
        restarts++;
        return true;
    }

    /** Restarts asked for in the current episode (0 when healthy). */
    public int restarts() {
        return restarts;
    }

    /** True between the first unhealthy report and the next HEALTHY one. */
    public boolean failing() {
        return failing;
    }
}
