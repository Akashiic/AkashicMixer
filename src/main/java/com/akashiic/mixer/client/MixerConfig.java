package com.akashiic.mixer.client;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import com.akashiic.mixer.AkashicMixer;
import com.akashiic.mixer.api.AkashicMixerApi;
import com.akashiic.mixer.api.MixerChannel;

/**
 * {@code config/akashicmixer.cfg}: one volume and mute per channel (bound as channels register) and the music ducking
 * settings. Written when the mixer screen closes. Client thread.
 */
public final class MixerConfig {

    static final String CHANNELS = "channels";
    static final String MUSIC = "minecraft_music";

    private static Configuration config;

    /** Fade Minecraft's background music out while Akashic audio is audible. */
    public static boolean duckEnabled = true;
    /** What is left of Minecraft's music next to an Akashic source, 0..100 (0 = silent). */
    public static int duckFloorPercent = 0;
    /** Akashic level (0..100, at the listener) at which the music is fully faded. */
    public static int duckFullAtPercent = 20;
    public static double fadeOutSeconds = 1.5;
    public static double fadeInSeconds = 4.0;

    private MixerConfig() {}

    static void load(File file) {
        config = new Configuration(file);
        config.load();
        config.setCategoryComment(CHANNELS, "Akashic Sounds Mixer channels: volume 0-100 and mute, per channel.");
        config.setCategoryComment(
            MUSIC,
            "Minecraft background music while Akashic audio (radios, speakers, screens) is audible.");
        duckEnabled = config
            .getBoolean("duckEnabled", MUSIC, true, "Fade Minecraft's music out near Akashic sound sources.");
        duckFloorPercent = config.getInt(
            "duckFloorPercent",
            MUSIC,
            0,
            0,
            100,
            "What is left of Minecraft's music next to an Akashic source (0 = silent).");
        duckFullAtPercent = config.getInt(
            "duckFullAtPercent",
            MUSIC,
            20,
            1,
            100,
            "Akashic level at the listener (percent) at which the music is fully faded; below it the fade is partial.");
        fadeOutSeconds = config.get(MUSIC, "fadeOutSeconds", 1.5, "Seconds to fade the music out.", 0.0, 30.0)
            .getDouble();
        fadeInSeconds = config.get(MUSIC, "fadeInSeconds", 4.0, "Seconds to bring the music back.", 0.0, 60.0)
            .getDouble();
        AkashicMixerApi.addListener(MixerConfig::bind);
        if (config.hasChanged()) config.save();
    }

    /** Reads a channel's stored volume and mute (creating the entries with defaults). */
    static void bind(MixerChannel ch) {
        if (config == null) return;
        ch.setVolumePercent(volume(ch).getInt(100));
        ch.setMuted(muted(ch).getBoolean(false));
        if (config.hasChanged()) config.save();
    }

    /** Writes every channel and the music settings back. */
    public static void save() {
        if (config == null) return;
        store(AkashicMixerApi.master());
        for (MixerChannel ch : AkashicMixerApi.channels()) store(ch);
        config.get(MUSIC, "duckEnabled", true)
            .set(duckEnabled);
        config.get(MUSIC, "duckFloorPercent", 0)
            .set(duckFloorPercent);
        try {
            config.save();
        } catch (RuntimeException e) {
            AkashicMixer.LOG.warn("AkashicMixer: could not save {}: {}", config.getConfigFile(), e.toString());
        }
    }

    private static void store(MixerChannel ch) {
        volume(ch).set(ch.volumePercent());
        muted(ch).set(ch.isMuted());
    }

    private static Property volume(MixerChannel ch) {
        Property p = config.get(CHANNELS, ch.id() + ".volume", 100, "Volume 0-100.", 0, 100);
        return p;
    }

    private static Property muted(MixerChannel ch) {
        return config.get(CHANNELS, ch.id() + ".muted", false, "Muted.");
    }
}
