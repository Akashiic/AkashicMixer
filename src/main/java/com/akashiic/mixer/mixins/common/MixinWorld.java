package com.akashiic.mixer.mixins.common;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.akashiic.mixer.server.EntitySoundServer;

/**
 * Remembers which entity a server sound belongs to while {@code playSoundAtEntity} / {@code playSoundToNearExcept}
 * runs, so {@link MixinWorldManager} can bind it. Forge's {@code PlaySoundAtEntityEvent} carries the entity too, but
 * it is posted before the sound is sent and the only way to change the send would be to cancel the event, which also
 * skips every other {@code IWorldAccess} and cannot tell the two methods apart (one excludes the player). Server worlds
 * only; nothing changes on the client.
 */
@Mixin(World.class)
public abstract class MixinWorld {

    @Shadow
    public boolean isRemote;

    @Inject(method = "playSoundAtEntity", at = @At("HEAD"))
    private void akashicmixer$enterEntitySound(Entity entity, String sound, float volume, float pitch,
        CallbackInfo ci) {
        if (!isRemote && entity != null) EntitySoundServer.enter(entity);
    }

    @Inject(method = "playSoundAtEntity", at = @At("RETURN"))
    private void akashicmixer$exitEntitySound(Entity entity, String sound, float volume, float pitch, CallbackInfo ci) {
        if (!isRemote && entity != null) EntitySoundServer.exit();
    }

    @Inject(method = "playSoundToNearExcept", at = @At("HEAD"))
    private void akashicmixer$enterPlayerSound(EntityPlayer player, String sound, float volume, float pitch,
        CallbackInfo ci) {
        if (!isRemote && player != null) EntitySoundServer.enter(player);
    }

    @Inject(method = "playSoundToNearExcept", at = @At("RETURN"))
    private void akashicmixer$exitPlayerSound(EntityPlayer player, String sound, float volume, float pitch,
        CallbackInfo ci) {
        if (!isRemote && player != null) EntitySoundServer.exit();
    }
}
