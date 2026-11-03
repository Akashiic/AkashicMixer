package com.akashiic.mixer.mixins.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import paulscode.sound.Library;
import paulscode.sound.SoundSystem;

/** The active library of a SoundSystem, for {@code OpenAlEngine.health}. */
@Mixin(value = SoundSystem.class, remap = false)
public interface AccessorSoundSystem {

    @Accessor("soundLibrary")
    Library akashicmixer$getLibrary();
}
