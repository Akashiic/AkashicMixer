package com.akashiic.mixer.server;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import com.akashiic.mixer.AkashicMixer;
import com.akashiic.mixer.core.SoundAnchor;
import com.akashiic.mixer.mixins.common.AccessorS29PacketSoundEffect;
import com.akashiic.mixer.net.EntitySoundMessage;
import com.akashiic.mixer.net.MixerNetwork;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Server half of entity-bound sounds (current Minecraft's {@code ClientboundSoundEntityPacket}, done for every sound
 * that belongs to an entity).
 * <p>
 * Vanilla 1.7.10 sends every sound as a fixed point ({@code S29PacketSoundEffect}); the client plays it there and it
 * never moves. The mixer changes nothing about who gets a sound: it waits for the final, per-player
 * {@code NetHandlerPlayServer.sendPacket} of each sound packet, after vanilla, Forge, Bukkit (vanish, {@code canSee})
 * and plugins have chosen the players, and only then decides what that one player receives:
 * <ul>
 * <li><b>World sounds</b> ({@code World.playSoundAtEntity}, {@code playSoundToNearExcept}, {@code playSoundEffect},
 * which Bukkit's {@code World.playSound} also uses): {@code MixinWorld} records whose sound it is while the world
 * sends it. A sound that belongs to an entity goes to a player whose client has the mixer as "sound at entity N";
 * everyone else gets the vanilla packet unchanged.</li>
 * <li><b>Sounds sent straight to one player</b> ({@code /playsound}, vanilla or Bukkit's; a plugin's
 * {@code Player.playSound}; ProtocolLib): when the packet is at that player ({@link SoundAnchor#meantForPlayer}), a
 * mixer client gets it bound to the player, and a vanilla client gets it at the player's exact position instead of
 * the block corner or centre those senders round to (the {@code /playsound} bug).</li>
 * </ul>
 * Thread safe: world records are per thread, the capable set is concurrent, and the replacement is a plain packet
 * sent through the same {@code sendPacket} (plugins may send from their own threads).
 */
public final class EntitySoundServer {

    /** Players whose client said hello on this connection. */
    private static final Set<UUID> CAPABLE = ConcurrentHashMap.newKeySet();

    /** Owners of the world sounds being sent on this thread, innermost first ({@link #POINT}: no owner). */
    private static final ThreadLocal<ArrayDeque<Object>> WORLD_SOUNDS = ThreadLocal.withInitial(ArrayDeque::new);
    private static final Object POINT = new Object();

    /** Set while this class sends its own replacement, which goes through the same hook. */
    private static final ThreadLocal<Boolean> REPLACING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /** Discriminator of {@link EntitySoundMessage} on {@link MixerNetwork#channel}. */
    private static final int ENTITY_SOUND_ID = 1;

    public static volatile boolean enabled = true;
    public static volatile boolean matchByPosition = true;

    private EntitySoundServer() {}

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

    /** {@code World.playSoundAtEntity} / {@code playSoundToNearExcept} starts on a server world. */
    public static void enterEntitySound(Entity entity) {
        push(entity == null ? POINT : entity);
    }

    /** {@code World.playSoundEffect} starts on a server world: the entity standing exactly there, if any. */
    public static void enterPointSound(World world, double x, double y, double z) {
        Entity owner = enabled && matchByPosition ? ownerAt(world, x, y, z) : null;
        push(owner == null ? POINT : owner);
    }

    /** ...and ends. */
    public static void exitWorldSound() {
        WORLD_SOUNDS.get()
            .poll();
    }

    private static void push(Object owner) {
        ArrayDeque<Object> sounds = WORLD_SOUNDS.get();
        // An exception between enter and exit would leave an entry behind; keep the stack from growing.
        if (sounds.size() > 8) sounds.clear();
        sounds.push(owner);
    }

    /**
     * {@code NetHandlerPlayServer.sendPacket} for {@code player}: true when the packet was replaced here (the vanilla
     * send must be skipped).
     */
    public static boolean onSend(EntityPlayerMP player, Packet packet) {
        if (!enabled || !(packet instanceof S29PacketSoundEffect) || player == null || REPLACING.get()) return false;
        AccessorS29PacketSoundEffect sound = (AccessorS29PacketSoundEffect) packet;
        String name = sound.akashicmixer$getName();
        if (name == null) return false;
        double x = sound.akashicmixer$getX() / 8.0;
        double y = sound.akashicmixer$getY() / 8.0;
        double z = sound.akashicmixer$getZ() / 8.0;
        float volume = sound.akashicmixer$getVolume();
        float pitch = sound.akashicmixer$getPitch() / 63.0F;
        boolean capable = CAPABLE.contains(player.getUniqueID());

        ArrayDeque<Object> world = WORLD_SOUNDS.get();
        if (!world.isEmpty()) {
            Object owner = world.peek();
            if (owner == POINT || !capable) return false;
            replace(player, bound((Entity) owner, name, x, y, z, volume, pitch));
            return true;
        }

        if (!SoundAnchor.meantForPlayer(x, y, z, player.posX, player.posY, player.posZ, player.getEyeHeight())) {
            return false;
        }
        if (capable) {
            replace(player, bound(player, name, x, y, z, volume, pitch));
            return true;
        }
        // A vanilla client: same sound, at the player instead of the block corner or centre (skip if already there).
        if (Math.abs(x - player.posX) <= SoundAnchor.PACKET_SPOT && Math.abs(z - player.posZ) <= SoundAnchor.PACKET_SPOT
            && (Math.abs(y - player.posY) <= SoundAnchor.PACKET_SPOT
                || Math.abs(y - player.posY - player.getEyeHeight()) <= SoundAnchor.PACKET_SPOT))
            return false;
        replace(player, new S29PacketSoundEffect(name, player.posX, player.posY, player.posZ, volume, pitch));
        return true;
    }

    /**
     * {@link EntitySoundMessage} as a packet of the mixer channel, built here instead of through
     * {@code SimpleNetworkWrapper} (whose shared encoder is not safe from a plugin's thread): the indexed codec's
     * discriminator byte, then the message.
     */
    private static Packet bound(Entity owner, String name, double x, double y, double z, float volume, float pitch) {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(ENTITY_SOUND_ID);
        new EntitySoundMessage(owner.getEntityId(), name, x, y, z, volume, pitch).toBytes(buffer);
        return new FMLProxyPacket(buffer, AkashicMixer.MODID);
    }

    private static void replace(EntityPlayerMP player, Packet packet) {
        REPLACING.set(Boolean.TRUE);
        try {
            player.playerNetServerHandler.sendPacket(packet);
        } finally {
            REPLACING.set(Boolean.FALSE);
        }
    }

    private static Entity ownerAt(World world, double x, double y, double z) {
        if (world == null) return null;
        double r = SoundAnchor.SAME_SPOT * 2;
        // Feet, position or eyes: search the column from 2 blocks below the sound (eyes) to just above it.
        List<?> near = world.getEntitiesWithinAABBExcludingEntity(
            null,
            AxisAlignedBB.getBoundingBox(x - r, y - 2.0, z - r, x + r, y + r, z + r));
        for (int i = 0; i < near.size(); i++) {
            Entity e = (Entity) near.get(i);
            if (SoundAnchor.atEntity(x, y, z, e.posX, e.posY, e.posY - e.yOffset, e.posY + e.getEyeHeight(), e.posZ))
                return e;
        }
        return null;
    }

    /** Forgets a player's hello when they leave (FML bus). */
    public static final class Logouts {

        @SubscribeEvent
        public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.player != null) CAPABLE.remove(event.player.getUniqueID());
        }
    }
}
