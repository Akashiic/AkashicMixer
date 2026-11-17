package com.akashiic.mixer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * AkashicMixer: the sound core of the Akashic mods. It owns the "Akashic Sounds Mixer" channels (web screens, radios,
 * music, ...) that live outside Minecraft's sound categories, and it fades Minecraft's own background music out while
 * Akashic audio is audible. AkashicFM and AkashicWeb depend on it ({@code required-after:akashicmixer}).
 * <p>
 * Mostly client side. On a server (dedicated or integrated) it only sends sounds that belong to an entity as entity
 * sounds to clients that have the mixer; the API answers with defaults there. The mod is not needed on the other side
 * of a connection ({@code acceptableRemoteVersions = "*"}): without it on both sides, sounds stay vanilla.
 */
@Mod(
    modid = AkashicMixer.MODID,
    name = AkashicMixer.NAME,
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*",
    guiFactory = "com.akashiic.mixer.client.MixerGuiFactory")
public class AkashicMixer {

    public static final String MODID = "akashicmixer";
    public static final String NAME = "AkashicMixer";
    public static final Logger LOG = LogManager.getLogger(NAME);

    @SidedProxy(clientSide = "com.akashiic.mixer.client.ClientProxy", serverSide = "com.akashiic.mixer.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }
}
