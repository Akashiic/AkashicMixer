package com.akashiic.mixer.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.akashiic.mixer.core.EngineRecovery.Health;

class EngineRecoveryTest {

    private static final long S = 1_000_000_000L;

    @Test
    void healthyAndLoadingNeverRestart() {
        EngineRecovery r = new EngineRecovery();
        for (long t = 0; t < 60 * S; t += S / 2) {
            assertFalse(r.update(Health.HEALTHY, t));
            assertFalse(r.update(Health.LOADING, t));
        }
        assertFalse(r.failing());
    }

    @Test
    void aDeadEngineIsRestartedAfterOneSecondThenBacksOffToTenSeconds() {
        EngineRecovery r = new EngineRecovery();
        List<Long> at = new ArrayList<>();
        long t0 = 5 * S; // nanoTime has an arbitrary origin
        for (long t = t0; t < t0 + 60 * S; t += S / 20) if (r.update(Health.DEAD, t)) at.add((t - t0) / (S / 20));
        // in ticks of 50 ms: 1 s, +2 s, +4 s, +8 s, then every 10 s
        assertEquals(Arrays.asList(20L, 60L, 140L, 300L, 500L, 700L, 900L, 1100L), at);
        assertEquals(8, r.restarts());
    }

    @Test
    void loadingBetweenFailedRestartsKeepsTheBackOff() {
        EngineRecovery r = new EngineRecovery();
        assertFalse(r.update(Health.DEAD, 0));
        assertTrue(r.update(Health.DEAD, S));
        assertFalse(r.update(Health.LOADING, S + S / 10));
        assertFalse(r.update(Health.DEAD, 2 * S)); // next one is due at 3 s, not 1 s after the load
        assertTrue(r.update(Health.DEAD, 3 * S));
    }

    @Test
    void aHealthyReportEndsTheEpisode() {
        EngineRecovery r = new EngineRecovery();
        r.update(Health.DEAD, 0);
        assertTrue(r.update(Health.DEAD, S));
        assertFalse(r.update(Health.HEALTHY, 2 * S));
        assertEquals(0, r.restarts());
        assertFalse(r.update(Health.DEAD, 10 * S)); // a new episode waits its first second again
        assertTrue(r.update(Health.DEAD, 11 * S));
    }

    @Test
    void aDisconnectedDeviceGetsAGracePeriodThenOneImmediateRestart() {
        EngineRecovery r = new EngineRecovery();
        assertFalse(r.update(Health.DISCONNECTED, 0));
        assertFalse(r.update(Health.DISCONNECTED, 2 * S));
        assertTrue(r.update(Health.DISCONNECTED, 3 * S));
        assertFalse(r.update(Health.DISCONNECTED, 4 * S));
    }

    @Test
    void aBriefDisconnectThatHealsDoesNothing() {
        EngineRecovery r = new EngineRecovery();
        assertFalse(r.update(Health.DISCONNECTED, 0));
        assertFalse(r.update(Health.HEALTHY, S));
        assertFalse(r.update(Health.DISCONNECTED, 2 * S));
        assertFalse(r.update(Health.DISCONNECTED, 4 * S)); // the grace restarted at 2 s
        assertTrue(r.update(Health.DISCONNECTED, 5 * S));
    }

    @Test
    void aRestartAfterADisconnectThatLeavesTheEngineDeadKeepsBackingOff() {
        EngineRecovery r = new EngineRecovery();
        r.update(Health.DISCONNECTED, 0);
        assertTrue(r.update(Health.DISCONNECTED, 3 * S));
        assertFalse(r.update(Health.LOADING, 3 * S + S / 10));
        assertFalse(r.update(Health.DEAD, 4 * S)); // 2 s after the first restart
        assertTrue(r.update(Health.DEAD, 5 * S));
    }
}
