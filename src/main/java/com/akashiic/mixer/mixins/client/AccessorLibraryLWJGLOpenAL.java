package com.akashiic.mixer.mixins.client;

import java.nio.FloatBuffer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import paulscode.sound.libraries.LibraryLWJGLOpenAL;

/**
 * The listener position buffer: allocated by {@code init} only after {@code AL.create} succeeded, cleared by
 * {@code cleanup}, so null means the library never got (or no longer has) a device.
 */
@Mixin(value = LibraryLWJGLOpenAL.class, remap = false)
public interface AccessorLibraryLWJGLOpenAL {

    @Accessor("listenerPositionAL")
    FloatBuffer akashicmixer$getListenerPosition();
}
