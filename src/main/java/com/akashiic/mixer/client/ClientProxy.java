package com.akashiic.mixer.client;

import net.minecraftforge.common.MinecraftForge;

import com.akashiic.mixer.CommonConfig;
import com.akashiic.mixer.CommonProxy;
import com.akashiic.mixer.api.AkashicMixerApi;
import com.akashiic.mixer.net.EntitySoundMessage;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        // Before the consumers' pre-init (they depend on us), so their channels bind to the stored values.
        MixerConfig.load(CommonConfig.config());
        EntitySounds.enabled = CommonConfig.entitySounds;
        EntitySounds.smoothEveryFrame = CommonConfig.smoothEveryFrame;
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        FMLCommonHandler.instance()
            .bus()
            .register(new MusicDucker());
        FMLCommonHandler.instance()
            .bus()
            .register(new SoundEngineWatchdog(MixerConfig.recoverEngine, MixerConfig.followDefaultDevice));
        AkashicMixerApi.setRecoversSoundEngine(MixerConfig.recoverEngine);
        AkashicMixerApi.setFollowsDefaultDevice(MixerConfig.recoverEngine && MixerConfig.followDefaultDevice);
        MinecraftForge.EVENT_BUS.register(new SoundOptionsButton());
        FMLCommonHandler.instance()
            .bus()
            .register(new EntitySounds());
    }

    @Override
    public void playEntitySound(EntitySoundMessage message) {
        if (EntitySounds.enabled) EntitySounds.play(message);
    }
}
