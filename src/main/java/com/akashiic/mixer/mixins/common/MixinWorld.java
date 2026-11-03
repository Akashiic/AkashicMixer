package com.akashiic.mixer.mixins.common;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.akashiic.mixer.server.EntitySoundServer;

/**
 * Records whose sound a server world is sending while {@code playSoundAtEntity}, {@code playSoundToNearExcept} or
 * {@code playSoundEffect} runs, so the final per-player send ({@link MixinNetHandlerPlayServer}) can bind it to its
 * entity. Nothing about the sound or its listeners changes here. Forge's {@code PlaySoundAtEntityEvent} carries the
 * entity too, but only for the first two, and it is posted before the send. Server worlds only (Bukkit's
 * {@code World.playSound} also ends in {@code playSoundEffect}).
 */
@Mixin(World.class)
public abstract class MixinWorld {

    @Unique
    private boolean akashicmixer$server() {
        return !((World) (Object) this).isRemote;
    }

    @Inject(method = "playSoundAtEntity", at = @At("HEAD"))
    private void akashicmixer$enterEntitySound(Entity entity, String sound, float volume, float pitch,
        CallbackInfo ci) {
        if (akashicmixer$server()) EntitySoundServer.enterEntitySound(entity);
    }

    @Inject(method = "playSoundAtEntity", at = @At("RETURN"))
    private void akashicmixer$exitEntitySound(Entity entity, String sound, float volume, float pitch, CallbackInfo ci) {
        if (akashicmixer$server()) EntitySoundServer.exitWorldSound();
    }

    @Inject(method = "playSoundToNearExcept", at = @At("HEAD"))
    private void akashicmixer$enterPlayerSound(EntityPlayer player, String sound, float volume, float pitch,
        CallbackInfo ci) {
        if (akashicmixer$server()) EntitySoundServer.enterEntitySound(player);
    }

    @Inject(method = "playSoundToNearExcept", at = @At("RETURN"))
    private void akashicmixer$exitPlayerSound(EntityPlayer player, String sound, float volume, float pitch,
        CallbackInfo ci) {
        if (akashicmixer$server()) EntitySoundServer.exitWorldSound();
    }

    @Inject(method = "playSoundEffect", at = @At("HEAD"))
    private void akashicmixer$enterPointSound(double x, double y, double z, String sound, float volume, float pitch,
        CallbackInfo ci) {
        if (akashicmixer$server()) EntitySoundServer.enterPointSound((World) (Object) this, x, y, z);
    }

    @Inject(method = "playSoundEffect", at = @At("RETURN"))
    private void akashicmixer$exitPointSound(double x, double y, double z, String sound, float volume, float pitch,
        CallbackInfo ci) {
        if (akashicmixer$server()) EntitySoundServer.exitWorldSound();
    }
}
