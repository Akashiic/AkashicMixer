package com.akashiic.mixer.core;

/**
 * One sound engine reload at a time. Pure; tested in ReloadGateTest.
 * <p>
 * {@code SoundManager.reloadSoundSystem} unloads the engine and starts a new one on a "Sound Library Loader" thread;
 * {@code loaded} stays false until that thread is done. A second reload in that window skips the unload (nothing is
 * loaded yet) and starts a second loader, and the two new engines then fight over LWJGL's single OpenAL context: the
 * second {@code AL.create} throws {@code IllegalStateException} and its paulscode thread dies, both loaders then wait
 * for each other for up to 40 seconds, and the engine that wins can be one whose OpenAL library never initialised.
 * Several mods reload when an audio device goes away (ArchaicFix's device checker, Akashic Voice, the Refresh Sounds
 * button), so this happens.
 * <p>
 * A reload asked for while a load is running is deferred and run once that load is done. A load that never reports
 * done stops blocking after {@link #MAX_LOAD_NANOS}.
 */
public final class ReloadGate {

    /** Longer than paulscode's own 40 s wait for a library to initialise. */
    public static final long MAX_LOAD_NANOS = 60_000_000_000L;

    private boolean loading;
    private long loadStartedNanos;
    private boolean pending;

    /** A reload is about to run: true lets it run, false defers it (a load is still running). */
    public synchronized boolean tryStart(boolean loaded, long nowNanos) {
        if (loading && !loaded && nowNanos - loadStartedNanos < MAX_LOAD_NANOS) {
            pending = true;
            return false;
        }
        loading = true;
        loadStartedNanos = nowNanos;
        pending = false;
        return true;
    }

    /** Client tick: true when the deferred reload should run now (the caller runs it). */
    public synchronized boolean tick(boolean loaded, long nowNanos) {
        if (!loading) return false;
        if (!loaded && nowNanos - loadStartedNanos < MAX_LOAD_NANOS) return false;
        loading = false;
        boolean run = pending;
        pending = false;
        return run;
    }

    /** A deferred reload is waiting for the running load. */
    public synchronized boolean pending() {
        return pending;
    }
}
