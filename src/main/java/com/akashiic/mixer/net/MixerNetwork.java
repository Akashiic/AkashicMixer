package com.akashiic.mixer.net;

import com.akashiic.mixer.AkashicMixer;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

/**
 * The mixer's own channel, used only for entity-bound sounds. A client with the mixer says {@link HelloMessage} once
 * per connection; only then does the server send it {@link EntitySoundMessage}. Clients and servers without the
 * mixer never see the channel: they keep vanilla's sound packets.
 */
public final class MixerNetwork {

    /** Bumped when a message changes; a server ignores a hello with another protocol. */
    public static final int PROTOCOL = 1;

    public static SimpleNetworkWrapper channel;

    private MixerNetwork() {}

    /** Pre-init, both sides. */
    public static void register() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(AkashicMixer.MODID);
        channel.registerMessage(HelloMessage.Handler.class, HelloMessage.class, 0, Side.SERVER);
        channel.registerMessage(EntitySoundMessage.Handler.class, EntitySoundMessage.class, 1, Side.CLIENT);
    }
}
