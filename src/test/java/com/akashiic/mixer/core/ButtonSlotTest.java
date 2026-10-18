package com.akashiic.mixer.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ButtonSlotTest {

    /** The vanilla 1.7.10 Music and Sounds screen: master + 8 category sliders, Done. */
    static List<int[]> vanilla(int w, int h) {
        List<int[]> b = new ArrayList<>();
        b.add(new int[] { w / 2 - 155, h / 6 - 12, 310, 20 });
        for (int k = 2; k < 10; k++)
            b.add(new int[] { w / 2 - 155 + k % 2 * 160, h / 6 - 12 + 24 * (k >> 1), 150, 20 });
        b.add(new int[] { w / 2 - 100, h / 6 + 168, 200, 20 });
        return b;
    }

    @Test
    void vanillaScreenGetsTheFirstRowBelowTheSliders() {
        int w = 427, h = 240;
        assertArrayEquals(new int[] { w / 2 - 155, h / 6 + 108, 150 }, ButtonSlot.find(vanilla(w, h), w, h));
    }

    @Test
    void otherModsButtonsArePassedOver() {
        int w = 427, h = 240;
        List<int[]> taken = vanilla(w, h);
        taken.add(new int[] { w / 2 - 155, h / 6 + 108, 150, 20 }); // e.g. Enhanced Sounds
        taken.add(new int[] { w / 2 + 5, h / 6 + 108, 150, 20 }); // e.g. Refresh Sounds
        int[] slot = ButtonSlot.find(taken, w, h);
        assertArrayEquals(new int[] { w / 2 - 155, h / 6 + 132, 150 }, slot);
    }

    @Test
    void theSlotNeverOverlapsAnything() {
        int w = 320, h = 240;
        List<int[]> taken = vanilla(w, h);
        taken.add(new int[] { 0, h / 6 + 100, w, 60 }); // a wide panel over both rows
        int[] s = ButtonSlot.find(taken, w, h);
        assertTrue(ButtonSlot.free(taken, s[0], s[1], s[2], ButtonSlot.HEIGHT));
    }
}
