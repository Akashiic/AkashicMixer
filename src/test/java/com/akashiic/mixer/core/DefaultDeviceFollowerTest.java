package com.akashiic.mixer.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DefaultDeviceFollowerTest {

    private static final String SPEAKERS = "Speakers (Realtek High Definition Audio)";
    private static final String JBL = "Headphones (JBL TUNE)";

    @Test
    void turningTheHeadsetOnWhileOnSpeakersMoves() {
        DefaultDeviceFollower f = new DefaultDeviceFollower();
        Object engine = new Object();
        assertFalse(f.moved(engine, SPEAKERS, SPEAKERS));
        assertFalse(f.moved(engine, SPEAKERS, SPEAKERS));
        assertTrue(f.moved(engine, JBL, SPEAKERS));
        assertTrue(f.moved(engine, JBL, SPEAKERS)); // still moved until the engine is restarted
    }

    @Test
    void aNewEngineOnTheNewDefaultIsSettled() {
        DefaultDeviceFollower f = new DefaultDeviceFollower();
        Object first = new Object();
        f.moved(first, SPEAKERS, SPEAKERS);
        assertTrue(f.moved(first, JBL, SPEAKERS));
        Object second = new Object();
        assertFalse(f.moved(second, JBL, JBL));
        assertEquals(JBL, f.baseline());
    }

    @Test
    void aNameMismatchAloneNeverRestartsSoNothingLoops() {
        DefaultDeviceFollower f = new DefaultDeviceFollower();
        Object engine = new Object();
        // OpenAL opened another device than the one it lists first, or names it differently.
        for (int i = 0; i < 10; i++) assertFalse(f.moved(engine, JBL, "OpenAL Soft"));
        Object restarted = new Object();
        for (int i = 0; i < 10; i++) assertFalse(f.moved(restarted, JBL, "OpenAL Soft"));
    }

    @Test
    void aDefaultThatFlipsBackIsNotAMove() {
        DefaultDeviceFollower f = new DefaultDeviceFollower();
        Object engine = new Object();
        f.moved(engine, SPEAKERS, SPEAKERS);
        assertTrue(f.moved(engine, JBL, SPEAKERS));
        assertFalse(f.moved(engine, SPEAKERS, SPEAKERS));
    }

    @Test
    void unknownNamesAreIgnored() {
        DefaultDeviceFollower f = new DefaultDeviceFollower();
        Object engine = new Object();
        f.moved(engine, SPEAKERS, SPEAKERS);
        assertFalse(f.moved(engine, null, SPEAKERS));
        assertFalse(f.moved(engine, "  ", SPEAKERS));
        assertFalse(f.moved(engine, SPEAKERS, null));
    }

    @Test
    void comparisonIgnoresCaseAndSurroundingBlanks() {
        DefaultDeviceFollower f = new DefaultDeviceFollower();
        Object engine = new Object();
        f.moved(engine, SPEAKERS, SPEAKERS);
        assertFalse(f.moved(engine, " " + SPEAKERS.toUpperCase() + "\0", SPEAKERS));
    }
}
