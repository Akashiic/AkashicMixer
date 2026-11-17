package com.akashiic.mixer.mixins.client;

import net.minecraft.client.multiplayer.WorldClient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.akashiic.mixer.client.EntitySounds;

/**
 * Sounds the client plays by itself at an entity's exact coordinates, as a fixed point: the local player's own sounds
 * ({@code EntityPlayerSP.playSound}: steps, chewing), the ender dragon's wings, the arrow-hit sound, and any mod that
 * plays a client sound at {@code entity.posX}. They stay on that entity instead. Distance-delayed sounds (far
 * explosions, fireworks) are left alone. Forge has no hook that both sees the position and replaces the play.
 */
@Mixin(WorldClient.class)
public abstract class MixinWorldClient {

    @Inject(method = "playSound", at = @At("HEAD"), cancellable = true)
    private void akashicmixer$soundStaysOnEntity(double x, double y, double z, String sound, float volume, float pitch,
        boolean distanceDelay, CallbackInfo ci) {
        if (!distanceDelay && EntitySounds.playAtEntity((WorldClient) (Object) this, x, y, z, sound, volume, pitch)) {
            ci.cancel();
        }
    }
}
