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
