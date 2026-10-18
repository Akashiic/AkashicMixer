package com.akashiic.mixer.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DuckModelTest {

    @Test
    void nothingAudibleLeavesMusicAlone() {
        assertEquals(1f, DuckModel.target(0f, 0.2f, 0f));
        assertEquals(1f, DuckModel.target(Float.NaN, 0.2f, 0f));
    }

    @Test
    void nextToASourceTheMusicGoesToTheFloor() {
        assertEquals(0f, DuckModel.target(0.2f, 0.2f, 0f));
        assertEquals(0f, DuckModel.target(1f, 0.2f, 0f));
        assertEquals(0.3f, DuckModel.target(0.9f, 0.2f, 0.3f), 1e-6);
    }

    @Test
    void walkingAwayFadesTheMusicBackLinearly() {
        assertEquals(0.5f, DuckModel.target(0.1f, 0.2f, 0f), 1e-6);
        assertEquals(0.75f, DuckModel.target(0.05f, 0.2f, 0f), 1e-6);
    }

    @Test
    void stepFadesOutFasterThanItFadesIn() {
        float down = DuckModel.step(1f, 0f, 0.5, 1.5, 4.0);
        float up = DuckModel.step(0f, 1f, 0.5, 1.5, 4.0);
        assertTrue(1f - down > up, "fade out should cover more ground than fade in in the same time");
    }

    @Test
    void stepReachesTheTargetWithinTheFadeTime() {
        float v = 1f;
        for (int i = 0; i < 30; i++) v = DuckModel.step(v, 0f, 0.05, 1.5, 4.0); // the 1.5 s fade time
        assertTrue(v < 0.06f, "~95% of the way after the fade time, was " + v);
        for (int i = 0; i < 50; i++) v = DuckModel.step(v, 0f, 0.05, 1.5, 4.0); // 4 s in total
        assertEquals(0f, v, "snaps to the target instead of creeping forever");
    }

    @Test
    void stepWithZeroFadeJumps() {
        assertEquals(0f, DuckModel.step(1f, 0f, 0.05, 0, 0));
    }

    @Test
    void appliedNeverReachesZeroSoMinecraftDoesNotStopTheTrack() {
        assertEquals(DuckModel.SILENT, DuckModel.applied(0f));
        assertEquals(1f, DuckModel.applied(2f));
    }
}
