package com.akashiic.mixer.net;

import com.akashiic.mixer.server.EntitySoundServer;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Client to server, once per connection: "I play entity-bound sounds" (only when the client has them on). */
public final class HelloMessage implements IMessage {

    int protocol;

    public HelloMessage() {}

    public HelloMessage(int protocol) {
        this.protocol = protocol;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        protocol = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(protocol);
    }

    public static final class Handler implements IMessageHandler<HelloMessage, IMessage> {

        @Override
        public IMessage onMessage(HelloMessage message, MessageContext ctx) {
            // Server thread: FMLProxyPacket.processPacket runs from NetworkManager.processReceivedPackets.
            EntitySoundServer.hello(ctx.getServerHandler().playerEntity, message.protocol);
            return null;
        }
    }
}
