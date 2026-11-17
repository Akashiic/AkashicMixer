package com.akashiic.mixer.client;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MusicTicker;

import com.akashiic.mixer.AkashicMixer;

/**
 * Brings Minecraft's background music back soon after the mixer restarted the sound engine. A restart stops every
 * sound; vanilla's {@code MusicTicker} then waits its full pause (10 to 20 minutes in game) before the next track, so
 * without this the music would seem gone. After a restart the mixer shortens that pause to {@link #DELAY_TICKS}, and
 * only if music was playing before. Fields are found by type (the {@code MusicTicker} of {@code Minecraft}, the
 * {@code ISound} and the {@code int} of {@code MusicTicker}): both classes load before mixins could reach them.
 * Client thread.
 */
final class MusicResume {

    static final int DELAY_TICKS = 100;

    private static Field ticker;
    private static Field current;
    private static Field delay;
    private static boolean broken;

    private MusicResume() {}

    /** Background music is playing (or about to be noticed as stopped). */
    static boolean playing(Minecraft mc) {
        MusicTicker t = ticker(mc);
        try {
            return t != null && current.get(t) != null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail(e);
            return false;
        }
    }

    /** True once the ticker has noticed the stop and its pause is shortened; false: try again next tick. */
    static boolean resumeSoon(Minecraft mc) {
        MusicTicker t = ticker(mc);
        if (t == null) return true;
        try {
            ISound playingNow = (ISound) current.get(t);
            if (playingNow != null) return mc.getSoundHandler()
                .isSoundPlaying(playingNow); // not noticed yet: wait (true only if it somehow survived)
            if (delay.getInt(t) > DELAY_TICKS) delay.setInt(t, DELAY_TICKS);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail(e);
            return true;
        }
    }

    private static MusicTicker ticker(Minecraft mc) {
        if (broken || mc == null) return null;
        try {
            if (ticker == null) {
                ticker = byType(Minecraft.class, MusicTicker.class);
                current = byType(MusicTicker.class, ISound.class);
                delay = byType(MusicTicker.class, int.class);
            }
            return (MusicTicker) ticker.get(mc);
        } catch (ReflectiveOperationException | RuntimeException e) {
            fail(e);
            return null;
        }
    }

    private static Field byType(Class<?> owner, Class<?> type) throws NoSuchFieldException {
        Field found = null;
        for (Field f : owner.getDeclaredFields()) {
            if (f.getType() != type || java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
            if (found != null) throw new NoSuchFieldException(owner.getName() + " has two " + type.getName());
            found = f;
        }
        if (found == null) throw new NoSuchFieldException(owner.getName() + " has no " + type.getName());
        found.setAccessible(true);
        return found;
    }

    private static void fail(Exception e) {
        if (broken) return;
        broken = true;
        AkashicMixer.LOG
            .warn("AkashicMixer: cannot reach Minecraft's music ticker ({}); music resumes as vanilla", e.toString());
    }
}
