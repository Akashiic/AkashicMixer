# Changelog

## Unreleased

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
