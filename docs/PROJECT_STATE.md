# Project state

Last update: 2026-10-18. Status: **first version, not released, not observed in the Forbidden pack.**

## Evidence

| Claim | Grade | Evidence |
|---|---|---|
| Builds with the GTNH buildscript 2.0.33; every class is major 52; the mixin refmap maps `getNormalizedVolume` to `func_148594_a` | BUILD (Linux) | `./gradlew build`, 2026-10-18 |
| Fade curve, report freshness, button placement, channel levels | AUTOMATED (Linux, JUnit, 17 tests) | `src/test` |
| Vanilla 1.7.10 behaviour the design relies on: `setSoundCategoryVolume` re-reads `getNormalizedVolume` for each playing sound of the category and stops it at 0; `playSound` skips a sound computed at 0; the Music and Sounds screen ends its sliders at `height/6 + 104` and puts Done at `height/6 + 168` | STATIC | decompiled Forge 10.13.4.1614 sources (MCP stable 12) |
| In a vanilla dev client: the button lands in the first free slot, opens the mixer, the mixin fades the vanilla music to 0.0001 in ~3.5 s without stopping it, and the music comes back after the reports stop | AUTOMATED (Linux dev client, Xvfb, Java 8, OpenAL null backend) | [`evidence/2026-10-18-dev-client-smoke.md`](evidence/2026-10-18-dev-client-smoke.md) |
| AkashicFM and AkashicWeb build against the API (`bab631dd8d` from JitPack) and their unit tests pass | BUILD / AUTOMATED (Linux) | their `claude/practical-newton-5y195a` branches, 2026-10-18 |
| Next to other mods' buttons (Enhanced Sounds, Refresh Sounds), with Hodgepodge, with real AkashicFM/AkashicWeb sources, on Windows | **not observed** | needs an in-game test in the Forbidden pack |

## Open points

- In-game test on the Forbidden pack (client with Hodgepodge, Enhanced Sounds and Refresh Sounds) on Java 8 and on
  Java 17+ with lwjgl3ify.
- Hodgepodge also mixes into `SoundManager`; our `@Inject` at RETURN is designed to coexist, not yet observed.
- Akashic-Voice is not wired yet (`voice` channel reserved).
