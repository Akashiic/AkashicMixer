# Changelog

## Unreleased

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
