package com.akashiic.mixer.mixins.client;

import java.util.Map;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundManager.class)
public interface AccessorSoundManager {

    /** False between {@code unloadSoundSystem} and the end of the asynchronous {@code loadSoundSystem}. */
    @Accessor("loaded")
    boolean akashicmixer$isLoaded();

    /** Playing sound to its paulscode source name, to move entity-bound sounds every frame. */
    @Accessor("invPlayingSounds")
    Map<ISound, String> akashicmixer$getInvPlayingSounds();
}
