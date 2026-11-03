package com.akashiic.mixer;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import com.akashiic.mixer.server.EntitySoundServer;

/**
 * {@code config/akashicmixer.cfg}, opened once on both sides. Holds the settings a dedicated server also reads (entity
 * sounds); the client settings are added by {@code MixerConfig} to the same file.
 */
public final class CommonConfig {

    public static final String ENTITY_SOUNDS = "entity_sounds";

    private static Configuration config;

    /** Sounds that belong to an entity stay on it (both sides must have it on). */
    public static boolean entitySounds = true;
    /** Server: a sound played exactly at an entity's coordinates belongs to that entity. */
    public static boolean matchByPosition = true;
    /** Client: move entity sounds every frame instead of 20 times a second. */
    public static boolean smoothEveryFrame = true;

    private CommonConfig() {}

    public static Configuration config() {
        return config;
    }

    static void load(File file) {
        config = new Configuration(file);
        config.load();
        config.setCategoryComment(
            ENTITY_SOUNDS,
            "Sounds that stay on their entity, as entity-bound sounds do in current Minecraft: a sound made by or at "
                + "a mob, a player or a vehicle moves with it instead of staying where it started. Works when the "
                + "server and the client both have the mixer with this on; anyone else gets vanilla sounds.");
        entitySounds = config.getBoolean(
            "enabled",
            ENTITY_SOUNDS,
            true,
            "Server: send sounds that belong to an entity as entity sounds to clients that have the mixer. "
                + "Client: play them on the entity, and keep the local player's own sounds on the player.");
        matchByPosition = config.getBoolean(
            "matchByPosition",
            ENTITY_SOUNDS,
            true,
            "Server: also treat a sound played exactly at an entity's coordinates (common in mods) as that entity's.");
        smoothEveryFrame = config.getBoolean(
            "smoothEveryFrame",
            ENTITY_SOUNDS,
            true,
            "Client: move entity sounds every frame, as the listener moves, instead of 20 times a second.");
        EntitySoundServer.enabled = entitySounds;
        EntitySoundServer.matchByPosition = matchByPosition;
        if (config.hasChanged()) config.save();
    }
}
