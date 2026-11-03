package com.akashiic.mixer.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReloadGateTest {

    private static final long S = 1_000_000_000L;

    @Test
    void aReloadOnALoadedEngineRuns() {
        ReloadGate g = new ReloadGate();
        assertTrue(g.tryStart(true, 0));
        assertFalse(g.tick(true, S)); // that load is done, nothing deferred
        assertTrue(g.tryStart(true, 2 * S));
    }

    @Test
    void theFirstLoadOfTheGameRuns() {
        assertTrue(new ReloadGate().tryStart(false, 0));
    }

    @Test
    void aSecondReloadWhileLoadingIsDeferredUntilTheLoadIsDone() {
        ReloadGate g = new ReloadGate();
        assertTrue(g.tryStart(true, 0));
        assertFalse(g.tryStart(false, S / 10)); // same moment: ArchaicFix and Voice both react
        assertFalse(g.tryStart(false, S / 5)); // and the Refresh Sounds button: still one deferred reload
        assertTrue(g.pending());
        assertFalse(g.tick(false, S / 2));
        assertTrue(g.tick(true, S)); // loaded: run the deferred one now
        assertFalse(g.pending());
        assertTrue(g.tryStart(true, S)); // which is let through
        assertFalse(g.tick(true, 2 * S));
    }

    @Test
    void aLoadThatNeverFinishesStopsBlockingAfterTheLimit() {
        ReloadGate g = new ReloadGate();
        assertTrue(g.tryStart(true, 0));
        assertFalse(g.tryStart(false, S));
        assertFalse(g.tick(false, ReloadGate.MAX_LOAD_NANOS - 1));
        assertTrue(g.tick(false, ReloadGate.MAX_LOAD_NANOS));
        assertTrue(g.tryStart(false, ReloadGate.MAX_LOAD_NANOS));
    }

    @Test
    void aReloadAfterTheLimitIsNotDeferred() {
        ReloadGate g = new ReloadGate();
        assertTrue(g.tryStart(true, 0));
        assertTrue(g.tryStart(false, ReloadGate.MAX_LOAD_NANOS));
    }
}
