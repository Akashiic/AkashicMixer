package com.akashiic.mixer.api;

/**
 * Well-known channel ids. The mixer ships the translations for these ({@code akashicmixer.channel.<id>}), so a mod
 * registering one of them only passes the id and its sort order.
 */
public final class Channels {

    /** AkashicWeb: in-world web screens. */
    public static final String SCREENS = "screens";
    /** AkashicFM: radios, portable radios and the speakers wired to them. */
    public static final String RADIOS = "radios";
    /** AkashicFM: the iPod (item and iPod Player block) and its speakers. */
    public static final String MUSIC = "music";
    /** Akashic Cinematic Menu: the main menu's background music (routed with {@code routeSounds}). */
    public static final String MENU_MUSIC = "menu_music";
    /** Akashic Cinematic Menu: the main menu's interface sounds (focus, select, back, ...). */
    public static final String MENU_SFX = "menu_sfx";
    /** Reserved for Akashic-Voice. */
    public static final String VOICE = "voice";

    private Channels() {}

    /** The translation key the mixer ships for a well-known id. */
    public static String translationKey(String id) {
        return "akashicmixer.channel." + id;
    }
}
