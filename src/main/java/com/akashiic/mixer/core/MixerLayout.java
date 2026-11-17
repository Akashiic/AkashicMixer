package com.akashiic.mixer.core;

/**
 * Where the rows of the mixer screen go. The vanilla-like layout (24 px rows from {@code height/6 - 12}, Done at
 * {@code height/6 + 168} or below) is used whenever it fits; with more channels than that on a short screen (1280x720
 * at the automatic GUI scale is 427x240) the rows get tighter, and if that is not enough the channel rows go into two
 * columns. Pure; tested in MixerLayoutTest.
 */
public final class MixerLayout {

    public static final int BUTTON_HEIGHT = 20;
    /** Total width of the content, as in vanilla's option screens. */
    public static final int CONTENT_WIDTH = 310;
    /** Below the title (drawn at y = 15). */
    static final int COMPACT_TOP = 26;
    static final int COMPACT_PITCH = 22;
    static final int BOTTOM_MARGIN = 4;

    public final int columns;
    public final int left;
    public final int top;
    public final int pitch;
    /** Width of one channel cell (slider + gap + mute button). */
    public final int cellWidth;
    public final int sliderWidth;
    public final int muteWidth;
    public final int duckY;
    public final int floorY;
    public final int doneY;

    private MixerLayout(int columns, int left, int top, int pitch, int muteWidth, int duckY, int floorY, int doneY) {
        this.columns = columns;
        this.left = left;
        this.top = top;
        this.pitch = pitch;
        this.cellWidth = columns == 1 ? CONTENT_WIDTH : (CONTENT_WIDTH - 10) / 2;
        this.muteWidth = muteWidth;
        this.sliderWidth = cellWidth - 5 - muteWidth;
        this.duckY = duckY;
        this.floorY = floorY;
        this.doneY = doneY;
    }

    /** X of channel row {@code i}'s slider. */
    public int rowX(int i) {
        return left + (columns == 1 ? 0 : (i % columns) * (cellWidth + 10));
    }

    /** Y of channel row {@code i}. */
    public int rowY(int i) {
        return top + (i / columns) * pitch;
    }

    /** True when every button ends inside a screen of {@code height}. */
    public boolean fits(int height) {
        return doneY + BUTTON_HEIGHT <= height - BOTTOM_MARGIN;
    }

    /**
     * @param rows   channel rows, the Akashic master included
     * @param width  screen width (scaled)
     * @param height screen height (scaled)
     */
    public static MixerLayout of(int rows, int width, int height) {
        int n = Math.max(1, rows);
        int left = width / 2 - CONTENT_WIDTH / 2;
        int sixth = height / 6;

        int top = sixth - 12;
        int duck = top + n * 24 + 6;
        int floor = duck + 24;
        MixerLayout normal = new MixerLayout(1, left, top, 24, 55, duck, floor, Math.max(floor + 30, sixth + 168));
        if (normal.fits(height)) return normal;

        top = Math.min(sixth - 12, COMPACT_TOP);
        duck = top + n * COMPACT_PITCH + 4;
        floor = duck + COMPACT_PITCH;
        MixerLayout compact = new MixerLayout(1, left, top, COMPACT_PITCH, 55, duck, floor, floor + COMPACT_PITCH + 4);
        if (compact.fits(height)) return compact;

        int lines = (n + 1) / 2;
        duck = top + lines * COMPACT_PITCH + 4;
        floor = duck + COMPACT_PITCH;
        return new MixerLayout(2, left, top, COMPACT_PITCH, 40, duck, floor, floor + COMPACT_PITCH + 4);
    }
}
