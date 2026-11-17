package com.akashiic.mixer.core;

/**
 * Pure maths for sounds that stay on their entity (current Minecraft's {@code EntityBoundSoundInstance}): the
 * "this sound was played at that entity" tests, and the per-frame interpolation of the sound's position. Safe on a
 * dedicated server.
 */
public final class SoundAnchor {

    /**
     * How close a sound's coordinates must be to an entity's to count as "played at the entity" when the coordinates
     * are copied from the entity's own fields ({@code posX}, {@code posY - yOffset}, {@code posZ}): a real match is
     * exact; this only absorbs float rounding.
     */
    public static final double SAME_SPOT = 1.0E-3;

    /**
     * The same for a position read back from a vanilla sound packet, which keeps eighths of a block
     * ({@code (int) (x * 8)}, rounded toward zero): up to 1/8 block off.
     */
    public static final double PACKET_SPOT = 0.13;

    private SoundAnchor() {}

    /**
     * True when a sound at (x, y, z) was played at an entity standing at (entityX, entityZ): at its feet (vanilla's
     * {@code playSoundAtEntity}), at its position ({@code posY}, which mods often pass) or at its eyes.
     */
    public static boolean atEntity(double x, double y, double z, double entityX, double entityY, double feetY,
        double eyeY, double entityZ) {
        if (Math.abs(x - entityX) > SAME_SPOT || Math.abs(z - entityZ) > SAME_SPOT) return false;
        return Math.abs(y - feetY) <= SAME_SPOT || Math.abs(y - entityY) <= SAME_SPOT
            || Math.abs(y - eyeY) <= SAME_SPOT;
    }

    /**
     * True when a sound packet sent straight to one player (not through the world: {@code /playsound}, a plugin's
     * {@code Player.playSound}) was meant to be heard at that player, whose feet are at (x, y, z). Matches where those
     * senders put "at the player":
     * <ul>
     * <li>the player's position or eyes (mods, ProtocolLib);</li>
     * <li>the corner of the player's block, {@code (floor x, floor(y + 0.5), floor z)}: vanilla {@code /playsound}
     * without coordinates or with {@code ~ ~ ~} ({@code getPlayerCoordinates});</li>
     * <li>the centre of the player's block, {@code (floor x + 0.5, floor y + 0.5, floor z + 0.5)}: Bukkit's
     * {@code Player.playSound(player.getLocation(), ...)}, which rounds to the block centre;</li>
     * <li>{@code (floor x + 0.5, floor(y + 0.5) + 0.5, floor z + 0.5)}: Bukkit's own {@code /playsound} without
     * coordinates (its corner, then rounded to the centre by {@code Player.playSound}).</li>
     * </ul>
     */
    public static boolean meantForPlayer(double soundX, double soundY, double soundZ, double x, double y, double z,
        double eyeHeight) {
        double bx = Math.floor(x), bz = Math.floor(z);
        double feetBlock = Math.floor(y), roundedBlock = Math.floor(y + 0.5);
        return near(soundX, soundY, soundZ, x, y, z) || near(soundX, soundY, soundZ, x, y + eyeHeight, z)
            || near(soundX, soundY, soundZ, bx, roundedBlock, bz)
            || near(soundX, soundY, soundZ, bx + 0.5, feetBlock + 0.5, bz + 0.5)
            || near(soundX, soundY, soundZ, bx + 0.5, roundedBlock + 0.5, bz + 0.5);
    }

    private static boolean near(double ax, double ay, double az, double bx, double by, double bz) {
        return Math.abs(ax - bx) <= PACKET_SPOT && Math.abs(ay - by) <= PACKET_SPOT && Math.abs(az - bz) <= PACKET_SPOT;
    }

    /** Linear interpolation, as the renderer places an entity between two ticks. */
    public static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }
}
