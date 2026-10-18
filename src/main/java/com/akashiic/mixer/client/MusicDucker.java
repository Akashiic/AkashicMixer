package com.akashiic.mixer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;

import com.akashiic.mixer.api.AkashicMixerApi;
import com.akashiic.mixer.core.DuckModel;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Fades Minecraft's background music (category MUSIC, the vanilla music ticker) out while Akashic audio is audible
 * and back in afterwards. {@code MixinSoundManager} multiplies the music's volume by {@link #factor()}; this handler
 * moves the factor each client tick and asks the sound manager to re-apply the MUSIC volume when it changed.
 * Jukeboxes, note blocks and every other category are untouched.
 */
public final class MusicDucker {

    private static volatile float applied = 1f;
    private float current = 1f;
    private long lastNanos;

    /** The factor the mixin applies to MUSIC sounds (1 = untouched). Read on the sound thread too. */
    public static float factor() {
        return applied;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0.05 : Math.min(1.0, (now - lastNanos) / 1.0e9);
        lastNanos = now;

        float target = MixerConfig.duckEnabled ? DuckModel.target(
            AkashicMixerApi.loudestAudible(),
            MixerConfig.duckFullAtPercent / 100f,
            MixerConfig.duckFloorPercent / 100f) : 1f;
        current = DuckModel.step(current, target, dt, MixerConfig.fadeOutSeconds, MixerConfig.fadeInSeconds);
        float next = current >= 1f ? 1f : DuckModel.applied(current);
        if (Math.abs(next - applied) < 1.0e-4f) return;
        applied = next;
        reapply();
    }

    /** Re-applies the MUSIC volume to the playing music; the mixin then multiplies in the new factor. */
    static void reapply() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getSoundHandler() == null || mc.gameSettings == null) return;
        mc.getSoundHandler()
            .setSoundLevel(SoundCategory.MUSIC, mc.gameSettings.getSoundLevel(SoundCategory.MUSIC));
    }
}
