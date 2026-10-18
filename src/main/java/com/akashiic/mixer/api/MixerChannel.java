package com.akashiic.mixer.api;

/**
 * One slider of the Akashic Sounds Mixer. Thread-safe: audio threads may read {@link #level()} while the GUI writes.
 */
public final class MixerChannel {

    private final String id;
    private final String translationKey;
    private final int order;
    private final MixerChannel parent;
    private volatile int volume;
    private volatile boolean muted;

    MixerChannel(String id, String translationKey, int order, MixerChannel parent) {
        this.id = id;
        this.translationKey = translationKey;
        this.order = order;
        this.parent = parent;
        this.volume = 100;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return translationKey;
    }

    /** Sort order in the mixer screen (lower first). */
    public int order() {
        return order;
    }

    /** The slider position, 0..100. */
    public int volumePercent() {
        return volume;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setVolumePercent(int percent) {
        volume = percent < 0 ? 0 : (percent > 100 ? 100 : percent);
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    /**
     * The linear level to apply, 0..1: this slider times the Akashic master slider, 0 when either is muted. It
     * replaces the Minecraft "Jukebox/Note Blocks" level the Akashic mods used to read. Minecraft's own Master slider
     * is NOT included: each mod applies it the way its audio path needs (OpenAL listener gain or by hand).
     */
    public float level() {
        if (muted) return 0f;
        float own = volume / 100f;
        return parent == null ? own : own * parent.level();
    }
}
