package com.akashiic.mixer.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AudibleBoardTest {

    @Test
    void loudestFreshReportWins() {
        AudibleBoard b = new AudibleBoard();
        b.report("akashicfm", 0.3f, 1000);
        b.report("akashicweb", 0.6f, 1000);
        assertEquals(0.6f, b.loudest(1100));
    }

    @Test
    void aReporterThatStopsCallingCountsAsSilent() {
        AudibleBoard b = new AudibleBoard();
        b.report("akashicweb", 0.6f, 1000);
        assertEquals(0f, b.loudest(1000 + AudibleBoard.STALE_MS + 1));
    }

    @Test
    void levelsAreClamped() {
        AudibleBoard b = new AudibleBoard();
        b.report("x", 7f, 0);
        assertEquals(1f, b.loudest(0));
        b.report("x", Float.NaN, 0);
        assertEquals(0f, b.loudest(0));
    }
}
