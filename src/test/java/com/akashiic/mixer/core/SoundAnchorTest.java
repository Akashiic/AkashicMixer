package com.akashiic.mixer.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SoundAnchorTest {

    @Test
    void rangeIsVanillas() {
        assertEquals(16.0, SoundAnchor.range(0.5F));
        assertEquals(16.0, SoundAnchor.range(1.0F));
        assertEquals(64.0, SoundAnchor.range(4.0F));
    }

    @Test
    void inRangeIsStrict() {
        assertTrue(SoundAnchor.inRange(15.99, 0, 0, 1.0F));
        assertFalse(SoundAnchor.inRange(16.0, 0, 0, 1.0F));
        assertTrue(SoundAnchor.inRange(0, 30, 0, 2.0F));
    }

    @Test
    void soundAtTheFeetOrAtThePositionIsTheEntitys() {
        // Server-side player: posY = feet. Client-side local player: posY = eyes, feet = posY - 1.62.
        assertTrue(SoundAnchor.atEntity(10.25, 64.0, -3.5, 10.25, 65.62, 64.0, -3.5));
        assertTrue(SoundAnchor.atEntity(10.25, 65.62, -3.5, 10.25, 65.62, 64.0, -3.5));
        assertTrue(SoundAnchor.atEntity(10.2504, 64.0, -3.5, 10.25, 64.0, 64.0, -3.5));
    }

    @Test
    void aSoundNextToTheEntityIsNotItsSound() {
        // A block sound at the centre of the block the entity stands on, half a block away.
        assertFalse(SoundAnchor.atEntity(10.5, 64.5, -3.5, 10.25, 64.0, 64.0, -3.5));
        // Right above the head.
        assertFalse(SoundAnchor.atEntity(10.25, 66.0, -3.5, 10.25, 64.0, 64.0, -3.5));
        assertFalse(SoundAnchor.atEntity(10.25, 64.0, -3.49, 10.25, 64.0, 64.0, -3.5));
    }

    @Test
    void lerpPlacesBetweenTicks() {
        assertEquals(10.0, SoundAnchor.lerp(10.0, 12.0, 0.0));
        assertEquals(11.0, SoundAnchor.lerp(10.0, 12.0, 0.5));
        assertEquals(12.0, SoundAnchor.lerp(10.0, 12.0, 1.0));
    }
}
