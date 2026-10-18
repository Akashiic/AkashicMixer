package com.akashiic.mixer.core;

import java.util.List;

/**
 * Where the "Akashic Sounds Mixer" button goes in Minecraft's Music and Sounds screen. Other mods (Enhanced Sounds,
 * Refresh Sounds, ...) add their own buttons there, so we take the first free slot from a list of candidates below the
 * vanilla sliders instead of a fixed position. Pure; tested in ButtonSlotTest.
 */
public final class ButtonSlot {

    public static final int WIDTH = 150;
    public static final int HEIGHT = 20;

    private ButtonSlot() {}

    /**
     * @param taken  rectangles {x, y, w, h} of the buttons already on the screen
     * @param width  screen width (scaled)
     * @param height screen height (scaled)
     * @return {x, y, w} of the slot
     */
    public static int[] find(List<int[]> taken, int width, int height) {
        int left = width / 2 - 155, right = width / 2 + 5, center = width / 2 - 75, top = height / 6;
        // Vanilla 1.7.10: the sliders end at top + 104, Done sits at top + 168.
        int[][] candidates = { { left, top + 108 }, { right, top + 108 }, { left, top + 132 }, { right, top + 132 },
            { center, top + 144 }, { center, top + 192 }, { 4, 4 } };
        for (int[] c : candidates) {
            if (c[1] + HEIGHT > height && c[1] != 4) continue;
            if (free(taken, c[0], c[1], WIDTH, HEIGHT)) return new int[] { c[0], c[1], WIDTH };
        }
        return new int[] { 4, 4, WIDTH };
    }

    static boolean free(List<int[]> taken, int x, int y, int w, int h) {
        for (int[] r : taken) {
            if (x < r[0] + r[2] && r[0] < x + w && y < r[1] + r[3] && r[1] < y + h) return false;
        }
        return true;
    }
}
