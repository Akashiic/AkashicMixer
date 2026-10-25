package com.akashiic.mixer.mixins.client;

import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Minecraft's SoundManager, to restart only the sound engine (not the whole sound registry). */
@Mixin(SoundHandler.class)
public interface AccessorSoundHandler {

    @Accessor("sndManager")
    SoundManager akashicmixer$getSndManager();
}
