package com.akashiic.mixer.mixins.client;

import net.minecraft.client.audio.SoundManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** False between {@code unloadSoundSystem} and the end of the asynchronous {@code loadSoundSystem}. */
@Mixin(SoundManager.class)
public interface AccessorSoundManager {

    @Accessor("loaded")
    boolean akashicmixer$isLoaded();
}
