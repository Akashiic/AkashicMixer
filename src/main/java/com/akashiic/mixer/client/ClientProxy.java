package com.akashiic.mixer.client;

import net.minecraftforge.common.MinecraftForge;

import com.akashiic.mixer.CommonProxy;
import com.akashiic.mixer.api.AkashicMixerApi;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        // Before the consumers' pre-init (they depend on us), so their channels bind to the stored values.
        MixerConfig.load(event.getSuggestedConfigurationFile());
    }

    @Override
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(new MusicDucker());
        FMLCommonHandler.instance()
            .bus()
            .register(new SoundEngineWatchdog(MixerConfig.recoverEngine));
        AkashicMixerApi.setRecoversSoundEngine(MixerConfig.recoverEngine);
        MinecraftForge.EVENT_BUS.register(new SoundOptionsButton());
    }
}
