package com.akashiic.mixer.client;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;

import com.akashiic.mixer.AkashicMixer;
import com.akashiic.mixer.core.SoundAnchor;
import com.akashiic.mixer.mixins.client.AccessorSoundHandler;
import com.akashiic.mixer.mixins.client.AccessorSoundManager;
import com.akashiic.mixer.net.EntitySoundMessage;
import com.akashiic.mixer.net.HelloMessage;
import com.akashiic.mixer.net.MixerNetwork;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import paulscode.sound.SoundSystem;

/**
 * Client half of entity-bound sounds. Plays the server's "sound at entity N" ({@link EntitySoundMessage}) and the
 * client's own sounds played exactly at an entity ({@code MixinWorldClient}) as {@link EntityBoundSound}s, says hello
 * to each server so it sends them, and
 * (with {@code smoothEveryFrame}) moves every playing bound sound each frame to where its entity is drawn: Minecraft
 * moves sounds 20 times a second but the listener every frame, which at flying speed leaves a bound sound jumping up
 * to a block behind or ahead. Client thread.
 */
public final class EntitySounds {

    /** Bound sounds moved each frame; older ones still move once per tick. */
    static final int MAX_SMOOTHED = 64;

    public static boolean enabled = true;
    public static boolean smoothEveryFrame = true;

    private static final List<EntityBoundSound> PLAYING = new ArrayList<>();
    private static WeakReference<NetHandlerPlayClient> greeted = new WeakReference<>(null);
    private static boolean smoothingFailed;

    /** A server sound bound to an entity; the vanilla point when this client does not know the entity. */
    public static void play(EntitySoundMessage message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || message.sound == null) return;
        ResourceLocation sound = new ResourceLocation(message.sound);
        Entity entity = mc.theWorld.getEntityByID(message.entityId);
        if (entity == null) {
            mc.getSoundHandler()
                .playSound(
                    new PositionedSoundRecord(sound, message.volume, message.pitch, message.x, message.y, message.z));
        } else {
            play(new EntityBoundSound(sound, entity, message.volume, message.pitch));
        }
    }

    /**
     * A sound the client plays by itself at a point ({@code WorldClient.playSound}): when the point is exactly an
     * entity's feet, position or eyes, plays it on that entity instead and returns true.
     */
    public static boolean playAtEntity(WorldClient world, double x, double y, double z, String name, float volume,
        float pitch) {
        if (!enabled || name == null || world == null) return false;
        Entity entity = null;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null && mc.thePlayer.worldObj == world && at(mc.thePlayer, x, y, z)) entity = mc.thePlayer;
        if (entity == null) {
            double r = SoundAnchor.SAME_SPOT * 2;
            List<?> near = world.getEntitiesWithinAABBExcludingEntity(
                null,
                AxisAlignedBB.getBoundingBox(x - r, y - 2.0, z - r, x + r, y + r, z + r));
            for (int i = 0; i < near.size() && entity == null; i++) {
                Entity candidate = (Entity) near.get(i);
                if (at(candidate, x, y, z)) entity = candidate;
            }
        }
        if (entity == null) return false;
        play(new EntityBoundSound(new ResourceLocation(name), entity, volume, pitch));
        return true;
    }

    private static boolean at(Entity e, double x, double y, double z) {
        return SoundAnchor.atEntity(x, y, z, e.posX, e.posY, e.boundingBox.minY, e.posY + e.getEyeHeight(), e.posZ);
    }

    private static void play(EntityBoundSound sound) {
        Minecraft.getMinecraft()
            .getSoundHandler()
            .playSound(sound);
        if (!smoothEveryFrame) return;
        if (PLAYING.size() >= MAX_SMOOTHED) PLAYING.remove(0);
        PLAYING.add(sound);
    }

    /** Once per connection, as soon as the player is in a world. */
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !enabled) return;
        Minecraft mc = Minecraft.getMinecraft();
        NetHandlerPlayClient connection = mc.getNetHandler();
        if (mc.thePlayer == null || connection == null) {
            PLAYING.clear();
            return;
        }
        if (greeted.get() == connection) return;
        greeted = new WeakReference<>(connection);
        MixerNetwork.channel.sendToServer(new HelloMessage(MixerNetwork.PROTOCOL));
    }

    /** Every frame, before the world is drawn: bound sounds to where their entities are drawn. */
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || PLAYING.isEmpty() || smoothingFailed) return;
        Minecraft mc = Minecraft.getMinecraft();
        SoundManager manager = ((AccessorSoundHandler) mc.getSoundHandler()).akashicmixer$getSndManager();
        if (manager == null || !((AccessorSoundManager) manager).akashicmixer$isLoaded()) return;
        Map<ISound, String> channels = ((AccessorSoundManager) manager).akashicmixer$getInvPlayingSounds();
        SoundSystem system;
        try {
            system = OpenAlEngine.system(manager);
        } catch (ReflectiveOperationException | RuntimeException e) {
            smoothingFailed = true;
            AkashicMixer.LOG.warn("AkashicMixer: entity sounds move once per tick only: {}", e.toString());
            return;
        }
        if (system == null) return;
        float partialTicks = event.renderTickTime;
        for (Iterator<EntityBoundSound> it = PLAYING.iterator(); it.hasNext();) {
            EntityBoundSound sound = it.next();
            String channel = channels.get(sound);
            // Not in the map: finished, or never started (volume 0). Gone entity: stays put, nothing to move.
            if (channel == null || !sound.following()) {
                it.remove();
                continue;
            }
            // Standing still: the tick already put it there; skip the command.
            if (!sound.moving()) continue;
            system.setPosition(
                channel,
                (float) sound.x(partialTicks),
                (float) sound.y(partialTicks),
                (float) sound.z(partialTicks));
        }
    }
}
