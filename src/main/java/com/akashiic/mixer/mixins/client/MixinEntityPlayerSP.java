package com.akashiic.mixer.mixins.client;

import net.minecraft.client.entity.EntityPlayerSP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.akashiic.mixer.client.EntitySounds;

/**
 * The local player's own sounds (steps, chewing, ...) are played by the client at a fixed point
 * ({@code worldObj.playSound(posX, posY - yOffset, posZ, ...)}); here they stay on the player instead, like the
 * server's entity sounds. Injected at that call, after Forge's {@code PlaySoundAtEntityEvent}, so a cancelled or
 * renamed sound is respected (the arguments are read at the call, after the event changed the name). Forge has no hook
 * that both knows the entity and replaces the play.
 */
@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP {

    @Inject(
        method = "playSound",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(DDDLjava/lang/String;FFZ)V"),
        cancellable = true)
    private void akashicmixer$ownSoundStaysOnPlayer(String name, float volume, float pitch, CallbackInfo ci) {
        if (!EntitySounds.enabled || name == null) return;
        EntitySounds.playOwn((EntityPlayerSP) (Object) this, name, volume, pitch);
        ci.cancel();
    }
}
