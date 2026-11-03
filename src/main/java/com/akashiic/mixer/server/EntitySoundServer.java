package com.akashiic.mixer.server;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldServer;

import com.akashiic.mixer.AkashicMixer;
import com.akashiic.mixer.core.SoundAnchor;
import com.akashiic.mixer.net.EntitySoundMessage;
import com.akashiic.mixer.net.MixerNetwork;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

/**
 * Server half of entity-bound sounds (current Minecraft's {@code ClientboundSoundEntityPacket}, done for every sound
 * that belongs to an entity).
 * <p>
 * Vanilla 1.7.10 sends every server sound as a fixed point ({@code S29PacketSoundEffect}): the entity's position when
 * the server played it. The client plays it there and it never moves, so a sound about a moving entity, or about the
 * player who flies on, is left behind. Here, when {@code WorldManager} is about to send a sound that belongs to an
 * entity, players whose client has the mixer get "sound at entity N" instead and their client keeps the sound on the
 * entity; every other player gets the vanilla packet, to the same players vanilla would have chosen.
 * <p>
 * A sound belongs to an entity when it comes from {@code World.playSoundAtEntity} or {@code playSoundToNearExcept}
 * (the entity is known), or, with {@code matchByPosition}, when a mod passed an entity's exact coordinates to
 * {@code playSoundEffect}. Server thread only, except the capable set.
 */
public final class EntitySoundServer {

    /** Players whose client said hello on this connection. */
    private static final Set<UUID> CAPABLE = ConcurrentHashMap.newKeySet();

    public static volatile boolean enabled = true;
    public static volatile boolean matchByPosition = true;

    /** Entities of the {@code playSoundAtEntity} calls in progress on the server thread, innermost first. */
    private static final ArrayDeque<Entity> CURRENT = new ArrayDeque<>();

    private EntitySoundServer() {}

    /** {@code World.playSoundAtEntity} / {@code playSoundToNearExcept} start (server worlds). */
    public static void enter(Entity entity) {
        // An exception between enter and exit would leave an entry behind; the coordinate check in ownerOf makes a
        // stale entry harmless, and this keeps the stack from growing.
        if (CURRENT.size() > 8) CURRENT.clear();
        CURRENT.push(entity);
    }

    /** ...and end. */
    public static void exit() {
        CURRENT.poll();
    }

    public static void hello(EntityPlayerMP player, int protocol) {
        if (player == null) return;
        if (protocol != MixerNetwork.PROTOCOL) {
            AkashicMixer.LOG.info(
                "AkashicMixer: {} has entity sound protocol {} (server {}); sending vanilla sounds",
                player.getCommandSenderName(),
                protocol,
                MixerNetwork.PROTOCOL);
            return;
        }
        CAPABLE.add(player.getUniqueID());
    }

    /**
     * Instead of {@code WorldManager.playSound} / {@code playSoundToNearExcept}: true when the sound was sent here
     * (vanilla's send must be skipped), false when it does not belong to an entity or nobody can bind it.
     */
    public static boolean send(MinecraftServer server, WorldServer world, EntityPlayer except, String sound, double x,
        double y, double z, float volume, float pitch) {
        if (!enabled || CAPABLE.isEmpty() || server == null || world == null || sound == null) return false;
        Entity entity = ownerOf(world, x, y, z);
        if (entity == null) return false;
        EntitySoundMessage bound = null;
        S29PacketSoundEffect vanilla = null;
        int dimension = world.provider.dimensionId;
        List<?> players = server.getConfigurationManager().playerEntityList;
        for (int i = 0; i < players.size(); i++) {
            EntityPlayerMP player = (EntityPlayerMP) players.get(i);
            if (player == except || player.dimension != dimension) continue;
            if (!SoundAnchor.inRange(x - player.posX, y - player.posY, z - player.posZ, volume)) continue;
            if (CAPABLE.contains(player.getUniqueID())) {
                if (bound == null) bound = new EntitySoundMessage(entity.getEntityId(), sound, x, y, z, volume, pitch);
                MixerNetwork.channel.sendTo(bound, player);
            } else {
                if (vanilla == null) vanilla = new S29PacketSoundEffect(sound, x, y, z, volume, pitch);
                player.playerNetServerHandler.sendPacket(vanilla);
            }
        }
        return true;
    }

    private static Entity ownerOf(WorldServer world, double x, double y, double z) {
        Entity e = CURRENT.peek();
        if (e != null && e.worldObj == world && at(e, x, y, z)) return e;
        if (!matchByPosition) return null;
        double r = SoundAnchor.SAME_SPOT * 2;
        List<?> near = world.getEntitiesWithinAABBExcludingEntity(
            null,
            AxisAlignedBB.getBoundingBox(x - r, y - r, z - r, x + r, y + r, z + r));
        for (int i = 0; i < near.size(); i++) {
            Entity candidate = (Entity) near.get(i);
            if (at(candidate, x, y, z)) return candidate;
        }
        return null;
    }

    private static boolean at(Entity e, double x, double y, double z) {
        return SoundAnchor.atEntity(x, y, z, e.posX, e.posY, e.posY - e.yOffset, e.posZ);
    }

    /** Forgets a player's hello when they leave (FML bus). */
    public static final class Logouts {

        @SubscribeEvent
        public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.player != null) CAPABLE.remove(event.player.getUniqueID());
        }
    }
}
