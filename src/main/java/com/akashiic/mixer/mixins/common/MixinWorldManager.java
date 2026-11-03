package com.akashiic.mixer.mixins.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldManager;
import net.minecraft.world.WorldServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.akashiic.mixer.server.EntitySoundServer;

/**
 * The server's only sound send: {@code WorldManager} turns every world sound into an {@code S29PacketSoundEffect} at a
 * fixed point. Forge has no hook here. When the sound belongs to an entity and a player in range has the mixer,
 * {@link EntitySoundServer#send} sends it instead (bound for mixer clients, the same vanilla packet for the others)
 * and the vanilla send is skipped; otherwise nothing changes.
 */
@Mixin(WorldManager.class)
public abstract class MixinWorldManager {

    @Shadow
    private MinecraftServer mcServer;

    @Shadow
    private WorldServer theWorldServer;

    @Inject(method = "playSound", at = @At("HEAD"), cancellable = true)
    private void akashicmixer$bindSound(String sound, double x, double y, double z, float volume, float pitch,
        CallbackInfo ci) {
        if (EntitySoundServer.send(mcServer, theWorldServer, null, sound, x, y, z, volume, pitch)) ci.cancel();
    }

    @Inject(method = "playSoundToNearExcept", at = @At("HEAD"), cancellable = true)
    private void akashicmixer$bindSoundExcept(EntityPlayer except, String sound, double x, double y, double z,
        float volume, float pitch, CallbackInfo ci) {
        if (EntitySoundServer.send(mcServer, theWorldServer, except, sound, x, y, z, volume, pitch)) ci.cancel();
    }
}
