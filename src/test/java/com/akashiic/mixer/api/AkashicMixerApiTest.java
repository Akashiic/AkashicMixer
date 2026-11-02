package com.akashiic.mixer.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AkashicMixerApiTest {

    @Test
    void registeringTwiceReturnsTheSameChannel() {
        MixerChannel a = AkashicMixerApi.register("test-same", 5);
        MixerChannel b = AkashicMixerApi.register("test-same", "other.key", 99);
        assertSame(a, b);
        assertEquals("akashicmixer.channel.test-same", b.translationKey());
        assertEquals(5, b.order());
    }

    @Test
    void levelIsChannelTimesMasterAndMuteWins() {
        MixerChannel ch = AkashicMixerApi.register("test-level", 1);
        MixerChannel master = AkashicMixerApi.master();
        try {
            ch.setVolumePercent(50);
            master.setVolumePercent(50);
            assertEquals(0.25f, ch.level(), 1e-6);
            ch.setMuted(true);
            assertEquals(0f, ch.level());
            ch.setMuted(false);
            master.setMuted(true);
            assertEquals(0f, ch.level());
        } finally {
            master.setMuted(false);
            master.setVolumePercent(100);
        }
    }

    @Test
    void volumeIsClamped() {
        MixerChannel ch = AkashicMixerApi.register("test-clamp", 1);
        ch.setVolumePercent(250);
        assertEquals(100, ch.volumePercent());
        ch.setVolumePercent(-3);
        assertEquals(0, ch.volumePercent());
    }

    @Test
    void masterIdIsReserved() {
        assertThrows(IllegalArgumentException.class, () -> AkashicMixerApi.register("master", 0));
    }

    @Test
    void routedSoundsFindTheirChannel() {
        MixerChannel ch = AkashicMixerApi.register("test-route", 1);
        AkashicMixerApi.routeSounds("testroutemod", "ui.", ch);
        assertSame(ch, AkashicMixerApi.routedChannel("testroutemod", "ui.focus"));
        assertNull(AkashicMixerApi.routedChannel("testroutemod", "bgm.menu"));
        assertThrows(IllegalArgumentException.class, () -> AkashicMixerApi.routeSounds("minecraft", "", ch));
    }
}
