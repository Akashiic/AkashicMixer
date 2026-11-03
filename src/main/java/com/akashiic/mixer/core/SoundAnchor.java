package com.akashiic.mixer.core;

/**
 * Pure maths for sounds that stay on their entity (current Minecraft's {@code EntityBoundSoundInstance}): vanilla's
 * hearing range for a sound, the "this sound was played at that entity" test, and the per-frame interpolation of the
 * sound's position. Safe on a dedicated server.
 */
public final class SoundAnchor {

    /**
     * How close a sound's coordinates must be to an entity's to count as "played at the entity". The coordinates are
     * copied from the entity's own fields ({@code posX}, {@code posY - yOffset}, {@code posZ}), so a real match is
     * exact; this only absorbs float rounding.
     */
    public static final double SAME_SPOT = 1.0E-3;

    private SoundAnchor() {}

    /** The radius vanilla sends a sound to ({@code WorldManager.playSound}): 16 blocks, more for loud sounds. */
    public static double range(float volume) {
        return volume > 1.0F ? 16.0 * volume : 16.0;
    }

    /** {@code ServerConfigurationManager.sendToAllNearExcept}'s test: strictly inside the range. */
    public static boolean inRange(double dx, double dy, double dz, float volume) {
        double r = range(volume);
        return dx * dx + dy * dy + dz * dz < r * r;
    }

    /**
     * True when a sound at (x, y, z) was played at an entity standing at (entityX, entityZ) with its position at
     * {@code entityY} and its feet at {@code feetY}: vanilla's {@code playSoundAtEntity} uses the feet, mods that pass
     * an entity's coordinates to {@code playSoundEffect} usually use {@code posY}.
     */
    public static boolean atEntity(double x, double y, double z, double entityX, double entityY, double feetY,
        double entityZ) {
        if (Math.abs(x - entityX) > SAME_SPOT || Math.abs(z - entityZ) > SAME_SPOT) return false;
        return Math.abs(y - feetY) <= SAME_SPOT || Math.abs(y - entityY) <= SAME_SPOT;
    }

    /** Linear interpolation, as the renderer places an entity between two ticks. */
    public static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }
}
