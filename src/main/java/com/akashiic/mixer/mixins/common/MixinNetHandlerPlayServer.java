package com.akashiic.mixer.mixins.common;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.Packet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.akashiic.mixer.server.EntitySoundServer;

/**
 * The last point every sound packet passes for one player, whoever sent it (the world, {@code /playsound}, Bukkit
 * plugins, ProtocolLib) and after every filter that chose that player. Forge has no outgoing-packet hook. Only
 * {@code S29PacketSoundEffect} is looked at; {@link EntitySoundServer#onSend} decides whether this player gets it bound
 * to an entity or at the player instead, and the vanilla packet is then skipped.
 */
@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServer {

    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void akashicmixer$entitySound(Packet packet, CallbackInfo ci) {
        if (EntitySoundServer.onSend(((NetHandlerPlayServer) (Object) this).playerEntity, packet)) ci.cancel();
    }
}
