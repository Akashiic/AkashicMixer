package com.akashiic.mixer.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.akashiic.mixer.core.AudibleBoard;

/**
 * Entry point for the Akashic mods. Call {@link #register} from your client pre-init (the mixer pre-inits first, as a
 * required dependency), keep the returned channel, and read {@link MixerChannel#level()} where you used to read the
 * Minecraft Records level. Every client tick, report how loud your audio is at the listener with
 * {@link #reportAudible}: that is what fades Minecraft's music out near radios, speakers and screens.
 * <p>
 * Pure Java, safe to class-load on a dedicated server (nothing is persisted or played there).
 */
public final class AkashicMixerApi {

    /** Bumped on incompatible API changes. */
    public static final int API_VERSION = 1;

    private static final MixerChannel MASTER = new MixerChannel(
        "master",
        Channels.translationKey("master"),
        Integer.MIN_VALUE,
        null);
    private static final Map<String, MixerChannel> CHANNELS = new ConcurrentHashMap<>();
    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final AudibleBoard AUDIBLE = new AudibleBoard();
    private static volatile boolean recoversSoundEngine;

    private AkashicMixerApi() {}

    /** A well-known channel ({@link Channels}), with the mixer's own translation. */
    public static MixerChannel register(String id, int order) {
        return register(id, Channels.translationKey(id), order);
    }

    /**
     * Registers (or returns the already registered) channel {@code id}. Two mods may share a channel: the first
     * registration's translation key and order win.
     */
    public static MixerChannel register(String id, String translationKey, int order) {
        if (id == null || id.isEmpty() || "master".equals(id)) throw new IllegalArgumentException("bad channel id");
        MixerChannel[] created = new MixerChannel[1];
        MixerChannel ch = CHANNELS
            .computeIfAbsent(id, k -> created[0] = new MixerChannel(k, translationKey, order, MASTER));
        if (created[0] != null) for (Listener l : LISTENERS) l.registered(ch);
        return ch;
    }

    /** The registered channel {@code id}, or null. */
    public static MixerChannel channel(String id) {
        return CHANNELS.get(id);
    }

    /** The Akashic master slider: scales every channel. */
    public static MixerChannel master() {
        return MASTER;
    }

    /** Registered channels, in screen order. */
    public static List<MixerChannel> channels() {
        List<MixerChannel> out = new ArrayList<>(CHANNELS.values());
        Collections.sort(
            out,
            Comparator.comparingInt(MixerChannel::order)
                .thenComparing(MixerChannel::id));
        return out;
    }

    /**
     * How loud {@code reporter}'s audio is right now at the listener, 0..1, after distance, occlusion and the channel
     * level (so a muted channel reports 0). Call it every client tick while anything plays; a reporter that stops
     * calling counts as silent after {@link AudibleBoard#STALE_MS}.
     */
    public static void reportAudible(String reporter, float level) {
        AUDIBLE.report(reporter, level, System.currentTimeMillis());
    }

    /** Loudest fresh report (the mixer's music ducking reads this). */
    public static float loudestAudible() {
        return AUDIBLE.loudest(System.currentTimeMillis());
    }

    /**
     * True on a client where the mixer restarts Minecraft's sound engine by itself after the audio device is lost and
     * comes back (config {@code sound_engine.recoverEngine}). A mod that needs the engine back (voice output) should
     * then wait for it instead of reloading the sound system itself: two mods reloading it fight each other, and a
     * full resource reload ({@code SoundHandler.onResourceManagerReload}) freezes the game for seconds.
     */
    public static boolean recoversSoundEngine() {
        return recoversSoundEngine;
    }

    /** Mixer internals. */
    public static void setRecoversSoundEngine(boolean value) {
        recoversSoundEngine = value;
    }

    /** Mixer internals: told about each new channel (config binding). */
    public interface Listener {

        void registered(MixerChannel channel);
    }

    /** Mixer internals. Also replays the channels registered so far. */
    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
        listener.registered(MASTER);
        for (MixerChannel ch : channels()) listener.registered(ch);
    }
}
