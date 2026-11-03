package com.akashiic.mixer.net;

import com.akashiic.mixer.AkashicMixer;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Server to client: play {@code sound} bound to entity {@code entityId}, the 1.7.10 counterpart of current Minecraft's
 * {@code ClientboundSoundEntityPacket}. Also carries the point vanilla would have used, for a client that does not
 * know the entity.
 */
public final class EntitySoundMessage implements IMessage {

    public int entityId;
    public String sound;
    public float x, y, z;
    public float volume, pitch;

    public EntitySoundMessage() {}

    public EntitySoundMessage(int entityId, String sound, double x, double y, double z, float volume, float pitch) {
        this.entityId = entityId;
        this.sound = sound;
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        sound = ByteBufUtils.readUTF8String(buf);
        x = buf.readFloat();
        y = buf.readFloat();
        z = buf.readFloat();
        volume = buf.readFloat();
        pitch = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        ByteBufUtils.writeUTF8String(buf, sound);
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeFloat(z);
        buf.writeFloat(volume);
        buf.writeFloat(pitch);
    }

    public static final class Handler implements IMessageHandler<EntitySoundMessage, IMessage> {

        @Override
        public IMessage onMessage(EntitySoundMessage message, MessageContext ctx) {
            // Client thread (FMLProxyPacket.processPacket). The proxy keeps client classes out of this class.
            AkashicMixer.proxy.playEntitySound(message);
            return null;
        }
    }
}
