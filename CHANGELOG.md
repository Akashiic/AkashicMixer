# Changelog

## Unreleased

- Sound engine and audio device loss (owner report, 2026-10-25: headset off → silence, back on → no sound until
  Refresh Sounds, freezes and crashes in Dynamic Surroundings' `flushSound`). Evidence:
  `docs/evidence/2026-10-25-audio-device-loss.md`.
  - One sound reload at a time: a `reloadSoundSystem` asked for while the last one is still loading is deferred
    until it is loaded. Two overlapping reloads made the second engine's `AL.create` fail with "Only one OpenAL
    context" and left a library without a context.
  - Crash guard: commands queued for an OpenAL library without a usable context are dropped instead of run
    (`UnsatisfiedLinkError` in `alListenerf`, `NullPointerException` in `setListenerPosition`).
  - Recovery (`sound_engine.recoverEngine`, on by default): the engine restarts by itself (engine only, not the sound
    registry) when it has no device, fell back to silent mode, or its device reports disconnected; so the sound comes
    back within about 10 s of the device returning.
  - API: `AkashicMixerApi.recoversSoundEngine()` (additive, API version stays 1).
- Follow Windows' default output device (owner request, 2026-10-25; `sound_engine.followDefaultDevice`, on): when
  Windows switches its default output, the engine restarts on the new device, so every Minecraft sound plays there;
  background music that was playing comes back within 5 s instead of after vanilla's 10-20 minute pause. API:
  `AkashicMixerApi.followsDefaultDevice()` (additive). Evidence: `docs/evidence/2026-10-25-follow-default-device.md`.
- Sound routing (owner request, 2026-11-02): `AkashicMixerApi.routeSounds(domain, pathPrefix, channel)` hands an
  Akashic mod's own `sounds.json` events to a mixer channel; the mixin applies the channel level in place of the
  Minecraft category slider and leaves them out of the music fade. `minecraft:` sounds cannot be routed. API version
  stays 1 (additive).
- Well-known channels `menu_music` (**Menu Music**) and `menu_sfx` (**Menu Sounds**) for the Akashic Cinematic Menu,
  with English and Brazilian Portuguese names.
- The mixer screen gets tighter rows, then two columns, when the channels do not fit (6 rows overflowed 1280x720 at
  the automatic GUI scale, 427x240).
- Dev client smoke test of the routing and the six-row screen on Linux (AUTOMATED),
  `docs/evidence/2026-11-02-sound-routing-smoke.md`.
- Entity sounds (owner request, 2026-11-03: "the sound has to stay on the entity, as in recent versions"). Vanilla
  1.7.10 sends every server sound as a fixed point, so a sound about a moving mob, vehicle or the player who flies on
  is left behind. Now, between a server and a client that both have the mixer:
  - the server sends a sound that belongs to an entity (`playSoundAtEntity`, `playSoundToNearExcept`, or with
    `matchByPosition` a `playSoundEffect` at an entity's exact coordinates) as "sound at entity N" on the
    `akashicmixer` channel, after the client's hello; players without the mixer get the vanilla packet, to the same
    players;
  - the client plays it on the entity (`EntityBoundSound`, as current Minecraft's `EntityBoundSoundInstance`) and
    moves it every frame to where the entity is drawn (`smoothEveryFrame`); a sound whose entity dies finishes where
    the entity was;
  - the local player's own client sounds (`EntityPlayerSP.playSound`) stay on the player.
  Config `entity_sounds` (`enabled`, `matchByPosition`, `smoothEveryFrame`), read by both sides. Research:
  `docs/research/2026-11-03-sounds-left-behind.md`; dev client check (AUTOMATED, Linux):
  `docs/evidence/2026-11-03-entity-sounds-smoke.md`.
- One branch again (owner report, 2026-11-03): the Cinematic Menu build (`0ff47b7`) had been cut from `c60f79e`, so
  its jar lacked the audio device fixes of `08b49cd` and `c69316d`. Both series merged; the routing and the reload
  gate share `MixinSoundManager`.

- First version (owner request, 2026-10-18): the Akashic Sounds Mixer.
  - Channel API (`AkashicMixerApi`, version 1): master slider, per-channel volume and mute, well-known ids
    `screens`, `radios`, `music`, `voice`.
  - *Akashic Sounds Mixer* button in Options → Music & Sounds (first free slot) and the mixer screen; also opened
    from the Forge mod list.
  - Minecraft background music fades out near audible Akashic sources and back in afterwards (configurable, on by
    default, silent next to a source).
  - `config/akashicmixer.cfg`; English and Brazilian Portuguese translations; JitPack and GitHub Actions builds.
- GitHub Actions publishes each jar and `SHA256SUMS-<sha>.txt` as its own artifact, not zipped
  (`upload-artifact@v7`, `archive: false`); the build delivery protocol is in `AGENTS.md`.
- Dev client smoke test on Linux (AUTOMATED): button placement, mixer screen and the music fade out and back,
  `docs/evidence/2026-10-18-dev-client-smoke.md`.
