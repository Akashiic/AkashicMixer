package com.akashiic.mixer.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SoundAnchorTest {

    /** What a vanilla sound packet keeps of a coordinate: eighths, rounded toward zero. */
    private static double packet(double v) {
        return (int) (v * 8.0) / 8.0;
    }

    @Test
    void soundAtTheFeetPositionOrEyesIsTheEntitys() {
        // Server-side player: posY = feet, eyes 1.62 above. Client-side local player: posY = eyes, feet 1.62 below.
        assertTrue(SoundAnchor.atEntity(10.25, 64.0, -3.5, 10.25, 64.0, 64.0, 65.62, -3.5));
        assertTrue(SoundAnchor.atEntity(10.25, 65.62, -3.5, 10.25, 64.0, 64.0, 65.62, -3.5));
        assertTrue(SoundAnchor.atEntity(10.25, 64.0, -3.5, 10.25, 65.62, 64.0, 65.74, -3.5));
        assertTrue(SoundAnchor.atEntity(10.2504, 64.0, -3.5, 10.25, 64.0, 64.0, 65.62, -3.5));
    }

    @Test
    void aSoundNextToTheEntityIsNotItsSound() {
        // A block sound at the centre of the block the entity stands on, half a block away.
        assertFalse(SoundAnchor.atEntity(10.5, 64.5, -3.5, 10.25, 64.0, 64.0, 65.62, -3.5));
        // Right above the head.
        assertFalse(SoundAnchor.atEntity(10.25, 66.0, -3.5, 10.25, 64.0, 64.0, 65.62, -3.5));
        assertFalse(SoundAnchor.atEntity(10.25, 64.0, -3.49, 10.25, 64.0, 64.0, 65.62, -3.5));
    }

    @Test
    void directSoundsMeantForThePlayer() {
        double x = 100.73, y = 64.0, z = -20.31;
        // Exact position, through the packet's eighths (negative z rounds toward zero).
        assertTrue(SoundAnchor.meantForPlayer(packet(x), packet(y), packet(z), x, y, z, 1.62));
        assertTrue(SoundAnchor.meantForPlayer(packet(x), packet(y + 1.62), packet(z), x, y, z, 1.62));
        // Vanilla /playsound without coordinates: corner of the block, y rounded.
        assertTrue(SoundAnchor.meantForPlayer(100, 64, -21, x, y, z, 1.62));
        // Bukkit Player.playSound(player.getLocation()): block centre.
        assertTrue(SoundAnchor.meantForPlayer(100.5, 64.5, -20.5, x, y, z, 1.62));
        // Bukkit /playsound: corner with rounded y, then block centre.
        assertTrue(SoundAnchor.meantForPlayer(100.5, 64.5, -20.5, x, 63.6, z, 1.62));
        // ... and vanilla's corner for the same player (y rounds up to 64).
        assertTrue(SoundAnchor.meantForPlayer(100, 64, -21, x, 63.6, z, 1.62));
    }

    @Test
    void directSoundsElsewhereAreNotThePlayers() {
        double x = 100.73, y = 64.0, z = -20.31;
        assertFalse(SoundAnchor.meantForPlayer(101.5, 64.5, -20.5, x, y, z, 1.62)); // next block
        assertFalse(SoundAnchor.meantForPlayer(100.5, 66.5, -20.5, x, y, z, 1.62)); // two blocks up
        assertFalse(SoundAnchor.meantForPlayer(200, 64, -20, x, y, z, 1.62)); // command with coordinates
        assertFalse(SoundAnchor.meantForPlayer(100.25, 64.0, -20.31, x, y, z, 1.62)); // half a block off
    }

    @Test
    void lerpPlacesBetweenTicks() {
        assertEquals(10.0, SoundAnchor.lerp(10.0, 12.0, 0.0));
        assertEquals(11.0, SoundAnchor.lerp(10.0, 12.0, 0.5));
        assertEquals(12.0, SoundAnchor.lerp(10.0, 12.0, 1.0));
    }
}
