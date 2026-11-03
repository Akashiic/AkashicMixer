package com.akashiic.mixer;

import com.akashiic.mixer.net.EntitySoundMessage;
import com.akashiic.mixer.net.MixerNetwork;
import com.akashiic.mixer.server.EntitySoundServer;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * Both sides: the config, the mixer's channel and the server half of entity sounds (a dedicated server and the
 * integrated one). Channels registered on a dedicated server keep their defaults and are never played.
 */
public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        CommonConfig.load(event.getSuggestedConfigurationFile());
        MixerNetwork.register();
    }

    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(new EntitySoundServer.Logouts());
    }

    /** Client only: a server sound bound to an entity. */
    public void playEntitySound(EntitySoundMessage message) {}
}
