package com.akashiic.mixer.mixins.client;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.audio.SoundPoolEntry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.akashiic.mixer.client.MusicDucker;
import com.akashiic.mixer.client.SoundEngineWatchdog;

/**
 * Two narrow hooks Forge does not offer.
 * <p>
 * Music ducking: the factor goes into {@code SoundManager.getNormalizedVolume}, the one place vanilla computes every
 * sound's volume (when it starts and whenever a category level is re-applied). Only category MUSIC is touched, and a
 * volume vanilla computed as 0 (music slider off) stays 0. An {@code @Inject} at RETURN, so other mods' changes to the
 * same method (Hodgepodge's logarithmic volume) still apply.
 * <p>
 * One reload at a time: {@code reloadSoundSystem} asked for while the previous reload is still loading on its own
 * thread is deferred until that load is done ({@code ReloadGate} has the why); {@link SoundEngineWatchdog} runs it.
 */
@Mixin(SoundManager.class)
public abstract class MixinSoundManager {

    @Shadow
    private boolean loaded;

    @Inject(method = "getNormalizedVolume", at = @At("RETURN"), cancellable = true)
    private void akashicmixer$duckMusic(ISound sound, SoundPoolEntry entry, SoundCategory category,
        CallbackInfoReturnable<Float> cir) {
        if (category != SoundCategory.MUSIC) return;
        float factor = MusicDucker.factor();
        if (factor >= 1f) return;
        float volume = cir.getReturnValueF();
        if (volume > 0f) cir.setReturnValue(volume * factor);
    }

    @Inject(method = "reloadSoundSystem", at = @At("HEAD"), cancellable = true)
    private void akashicmixer$oneReloadAtATime(CallbackInfo ci) {
        if (!SoundEngineWatchdog.tryStartReload(loaded)) ci.cancel();
    }
}
