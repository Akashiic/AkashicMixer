package com.akashiic.mixer.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MixerLayoutTest {

    /** 1280x720 at the automatic GUI scale (3). */
    private static final int W = 427, H = 240;

    @Test
    void fourRowsKeepTheVanillaLikeLayout() {
        MixerLayout l = MixerLayout.of(4, W, H);
        assertEquals(1, l.columns);
        assertEquals(24, l.pitch);
        assertEquals(W / 2 - 155, l.rowX(0));
        assertEquals(H / 6 - 12, l.rowY(0));
        assertEquals(H / 6 - 12 + 3 * 24, l.rowY(3));
        assertEquals(250, l.sliderWidth);
        assertEquals(55, l.muteWidth);
        assertEquals(H / 6 + 168, l.doneY);
        assertTrue(l.fits(H));
    }

    @Test
    void sixRowsGetTighterOnA720pScreen() {
        MixerLayout l = MixerLayout.of(6, W, H);
        assertEquals(1, l.columns);
        assertEquals(MixerLayout.COMPACT_PITCH, l.pitch);
        assertTrue(l.fits(H));
        assertTrue(l.rowY(0) >= MixerLayout.COMPACT_TOP);
        assertTrue(l.rowY(5) + 20 <= l.duckY);
    }

    @Test
    void sixRowsKeepTheNormalLayoutOnTallScreens() {
        MixerLayout l = MixerLayout.of(6, 480, 270);
        assertEquals(24, l.pitch);
        assertTrue(l.fits(270));
    }

    @Test
    void manyRowsGoIntoTwoColumnsThatFit() {
        MixerLayout l = MixerLayout.of(9, W, H);
        assertEquals(2, l.columns);
        assertTrue(l.fits(H));
        assertEquals(l.rowY(0), l.rowY(1));
        assertTrue(l.rowX(1) >= l.rowX(0) + l.cellWidth);
        assertTrue(l.rowX(1) + l.cellWidth <= l.left + MixerLayout.CONTENT_WIDTH);
        assertEquals(l.cellWidth, l.sliderWidth + 5 + l.muteWidth);
        assertTrue(l.rowY(8) + 20 <= l.duckY);
    }

    @Test
    void rowsNeverOverlapTheMusicSettings() {
        for (int rows = 1; rows <= 10; rows++) {
            for (int h : new int[] { 240, 270, 360, 480 }) {
                MixerLayout l = MixerLayout.of(rows, W, h);
                assertTrue(l.rowY(rows - 1) + 20 <= l.duckY, rows + " rows, height " + h);
                assertTrue(l.duckY + 20 <= l.floorY);
                assertTrue(l.floorY + 20 <= l.doneY);
            }
        }
    }
}
