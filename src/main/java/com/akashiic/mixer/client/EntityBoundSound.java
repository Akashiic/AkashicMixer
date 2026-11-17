package com.akashiic.mixer.client;

import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import com.akashiic.mixer.core.SoundAnchor;

/**
 * A sound that stays on its entity: the 1.7.10 counterpart of current Minecraft's {@code EntityBoundSoundInstance}.
 * Minecraft moves it once per tick ({@link #update}); {@link EntitySounds} also moves it every frame, where the
 * listener moves. Placed at the entity's feet, where vanilla plays an entity's sounds ({@code posY - yOffset}).
 * <p>
 * Unlike current Minecraft, a sound whose entity dies or leaves is not cut: it finishes where the entity was last
 * (death sounds play while the body is removed).
 */
final class EntityBoundSound extends MovingSound {

    final Entity entity;
    private boolean left;

    EntityBoundSound(ResourceLocation sound, Entity entity, float volume, float pitch) {
        super(sound);
        this.entity = entity;
        this.volume = volume;
        this.field_147663_c = pitch;
        place(1.0F);
    }

    @Override
    public void update() {
        if (!left && (entity.isDead || entity.worldObj == null)) left = true;
        if (!left) place(1.0F);
    }

    /** False once the entity is gone; the sound then stays where it was. */
    boolean following() {
        return !left;
    }

    /** True when the entity moved during the last tick (its drawn position changes between frames). */
    boolean moving() {
        return entity.prevPosX != entity.posX || entity.prevPosY != entity.posY || entity.prevPosZ != entity.posZ;
    }

    /** Moves the sound to the entity as drawn {@code partialTicks} into the current tick. */
    void place(float partialTicks) {
        xPosF = (float) x(partialTicks);
        yPosF = (float) y(partialTicks);
        zPosF = (float) z(partialTicks);
    }

    double x(float partialTicks) {
        return SoundAnchor.lerp(entity.prevPosX, entity.posX, partialTicks);
    }

    double y(float partialTicks) {
        // Feet: posY minus the current offset to the bounding box (the client's own player has posY at the eyes).
        double feetOffset = entity.posY - entity.boundingBox.minY;
        return SoundAnchor.lerp(entity.prevPosY, entity.posY, partialTicks) - feetOffset;
    }

    double z(float partialTicks) {
        return SoundAnchor.lerp(entity.prevPosZ, entity.posZ, partialTicks);
    }
}
