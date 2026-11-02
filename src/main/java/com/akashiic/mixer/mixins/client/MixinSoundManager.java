package com.akashiic.mixer.mixins.client;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.audio.SoundPoolEntry;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.akashiic.mixer.api.AkashicMixerApi;
import com.akashiic.mixer.api.MixerChannel;
import com.akashiic.mixer.client.MusicDucker;
import com.akashiic.mixer.core.SoundRoutes;

/**
 * Forge has no hook for the volume of a sound that is already playing, so the mixer works in
 * {@code SoundManager.getNormalizedVolume}, the one place vanilla computes every sound's volume (when it starts, every
 * tick for tickable sounds, and whenever a category level is re-applied). Two jobs, in this order:
 * <ol>
 * <li><b>Routed sounds</b> (an Akashic mod's own {@code sounds.json} events handed to a mixer channel with
 * {@link AkashicMixerApi#routeSounds}): the volume becomes sound x entry x channel level, so the Minecraft category
 * slider no longer applies to them. Never a {@code minecraft:} sound (the route table refuses that domain).</li>
 * <li><b>Music ducking</b>: category MUSIC only; a volume vanilla computed as 0 (music slider off) stays 0.</li>
 * </ol>
 * An {@code @Inject} at RETURN, so other mods' changes to the same method (Hodgepodge's logarithmic volume) still
 * apply to every sound that is not routed.
 */
@Mixin(SoundManager.class)
public abstract class MixinSoundManager {

    @Inject(method = "getNormalizedVolume", at = @At("RETURN"), cancellable = true)
    private void akashicmixer$volume(ISound sound, SoundPoolEntry entry, SoundCategory category,
        CallbackInfoReturnable<Float> cir) {
        ResourceLocation id = sound == null ? null : sound.getPositionedSoundLocation();
        if (id != null && entry != null) {
            MixerChannel routed = AkashicMixerApi.routedChannel(id.getResourceDomain(), id.getResourcePath());
            if (routed != null) {
                cir.setReturnValue(SoundRoutes.volume(sound.getVolume(), entry.getVolume(), routed.level()));
                return;
            }
        }
        if (category != SoundCategory.MUSIC) return;
        float factor = MusicDucker.factor();
        if (factor >= 1f) return;
        float volume = cir.getReturnValueF();
        if (volume > 0f) cir.setReturnValue(volume * factor);
    }
}
