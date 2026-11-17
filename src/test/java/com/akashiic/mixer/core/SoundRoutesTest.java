package com.akashiic.mixer.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SoundRoutesTest {

    @Test
    void routesByDomainAndPrefix() {
        SoundRoutes<String> routes = new SoundRoutes<>();
        routes.add("akashiccinematic", "bgm.", "menu_music");
        routes.add("akashiccinematic", "ui.", "menu_sfx");
        assertEquals("menu_music", routes.find("akashiccinematic", "bgm.menu"));
        assertEquals("menu_sfx", routes.find("akashiccinematic", "ui.focus"));
        assertNull(routes.find("akashiccinematic", "other"));
        assertNull(routes.find("otherdomain", "bgm.menu"));
        assertNull(routes.find(null, "bgm.menu"));
        assertNull(routes.find("akashiccinematic", null));
    }

    @Test
    void longestPrefixWinsAndEmptyPrefixTakesTheDomain() {
        SoundRoutes<String> routes = new SoundRoutes<>();
        routes.add("mod", "", "all");
        routes.add("mod", "ui.", "ui");
        routes.add("mod", "ui.loud", "loud");
        assertEquals("all", routes.find("mod", "bgm"));
        assertEquals("ui", routes.find("mod", "ui.focus"));
        assertEquals("loud", routes.find("mod", "ui.loudest"));
    }

    @Test
    void firstRegistrationOfARouteWins() {
        SoundRoutes<String> routes = new SoundRoutes<>();
        assertTrue(routes.add("mod", "ui.", "first"));
        assertFalse(routes.add("mod", "ui.", "second"));
        assertEquals("first", routes.find("mod", "ui.x"));
    }

    @Test
    void vanillaSoundsCannotBeRouted() {
        SoundRoutes<String> routes = new SoundRoutes<>();
        assertThrows(IllegalArgumentException.class, () -> routes.add("minecraft", "music.", "x"));
        assertThrows(IllegalArgumentException.class, () -> routes.add("", "a", "x"));
        assertThrows(IllegalArgumentException.class, () -> routes.add(null, "a", "x"));
        assertThrows(IllegalArgumentException.class, () -> routes.add("mod", null, "x"));
        assertThrows(IllegalArgumentException.class, () -> routes.add("mod", "a", null));
        assertTrue(routes.isEmpty());
        assertNull(routes.find("minecraft", "music.menu"));
    }

    @Test
    void volumeReplacesTheCategorySliderWithTheChannelLevel() {
        assertEquals(0.4f * 0.5f, SoundRoutes.volume(0.4f, 1.0, 0.5f), 1e-6);
        assertEquals(0f, SoundRoutes.volume(0.4f, 1.0, 0f));
        assertEquals(1f, SoundRoutes.volume(2f, 1.0, 1f));
        assertEquals(0f, SoundRoutes.volume(-1f, 1.0, 1f));
        assertEquals(0f, SoundRoutes.volume(Float.NaN, 1.0, 1f));
    }
}
