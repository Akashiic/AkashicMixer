# Project state

Last update: 2026-11-03. Status: **first version, not released, not observed in the Forbidden pack.**

## Evidence

| Claim | Grade | Evidence |
|---|---|---|
| Builds with the GTNH buildscript 2.0.33 (last on 2026-11-02, with the routing change); every class is major 52; the mixin refmap maps `getNormalizedVolume` to `func_148594_a` | BUILD (Linux) | `./gradlew build`, 2026-10-18 |
| Fade curve, report freshness, button placement, channel levels, sound routes, screen layout | AUTOMATED (Linux, JUnit, 28 tests) | `src/test` |
| Vanilla 1.7.10 behaviour the design relies on: `setSoundCategoryVolume` re-reads `getNormalizedVolume` for each playing sound of the category and stops it at 0; `playSound` skips a sound computed at 0; the Music and Sounds screen ends its sliders at `height/6 + 104` and puts Done at `height/6 + 168` | STATIC | decompiled Forge 10.13.4.1614 sources (MCP stable 12) |
| In a vanilla dev client: the button lands in the first free slot, opens the mixer, the mixin fades the vanilla music to 0.0001 in ~3.5 s without stopping it, and the music comes back after the reports stop | AUTOMATED (Linux dev client, Xvfb, Java 8, OpenAL null backend) | [`evidence/2026-10-18-dev-client-smoke.md`](evidence/2026-10-18-dev-client-smoke.md) |
| AkashicFM and AkashicWeb build against the API (`bab631dd8d` from JitPack) and their unit tests pass | BUILD / AUTOMATED (Linux) | their `claude/practical-newton-5y195a` branches, 2026-10-18 |
| Audio device loss: the two audio crash signatures of the owner's reports (`UnsatisfiedLinkError` in `alListenerf`, `NullPointerException` in `setListenerPosition`, both through Dynamic Surroundings' client-thread `flushSound`) come from an OpenAL library without a usable context; overlapping sound reloads (ArchaicFix, Akashic Voice, Refresh Sounds) produce one | STATIC | [`evidence/2026-10-25-audio-device-loss.md`](evidence/2026-10-25-audio-device-loss.md) |
| Reload gate, crash guard and engine recovery: two reloads in one tick no longer collide; a client-thread flush on a library without a context no longer throws; a silent engine restarts by itself once the device is back (33 ticks after) | AUTOMATED (Linux dev client, Java 8, LWJGL 2.9.4, OpenAL Soft with the device switched off and on through its config); `EngineRecovery` and `ReloadGate` unit-tested (12 tests; 30 in total) | same file |
| Following Windows' default output: OpenAL Soft 1.15.1 (LWJGL 2.9.1) lists the default endpoint first but caches the list, and its `ALC_DEFAULT_DEVICE_SPECIFIER` is the constant "OpenAL Soft" | STATIC | [`evidence/2026-10-25-follow-default-device.md`](evidence/2026-10-25-follow-default-device.md) |
| Default-device following and music resume: no restart in 30 s of normal play (device check ≤ 0.64 ms); an injected "default moved" restarts once and settles; music back within ~5 s after a restart; `DefaultDeviceFollower` cannot loop | AUTOMATED (Linux dev client, the move injected; JUnit: 38 tests in total) | same file |
| Sound routing (`routeSounds`), the `menu_music`/`menu_sfx` channels and the adaptive mixer screen layout: route table, routed volume and layout fit unit-tested | AUTOMATED (Linux, JUnit; 49 tests in total after the 2026-11-03 merge) | `SoundRoutesTest`, `MixerLayoutTest`, `AkashicMixerApiTest` |
| Vanilla 1.7.10 behaviour routing relies on: tickable sounds re-read `getNormalizedVolume` every tick; `setSoundCategoryVolume` stops a sound only when that value is 0; `playSound` skips a sound computed at 0 | STATIC | decompiled `SoundManager` (MCP stable 12) |
| In a dev client: routed sounds play at the channel level with Minecraft's Music slider at 0, follow the channel and Akashic (all) live, survive the Music slider being re-applied, go silent when muted; an unrouted control stays under the vanilla slider; six rows fit the mixer screen at 427x240 | AUTOMATED (Linux dev client, Xvfb, Java 8, OpenAL null backend) | [`evidence/2026-11-02-sound-routing-smoke.md`](evidence/2026-11-02-sound-routing-smoke.md) |
| The real Akashic Cinematic Menu (0.7.0-alpha.5) with the mixer, on Windows, in the pack | **not observed** | its OI-17 |
| Why sounds are left behind in 1.7.10 and what current Minecraft (26.3) does instead: one-shot sounds are fixed points in both; entity-bound sounds (`EntityBoundSoundInstance`, `ClientboundSoundEntityPacket`) follow their entity | STATIC | [`research/2026-11-03-sounds-left-behind.md`](research/2026-11-03-sounds-left-behind.md) |
| Entity sounds: a server sound of a moving pig, of the flying player (22 blocks/s) and the player's own client sound stay on the entity (0.00 block at every frame with `smoothEveryFrame`); a sound next to an entity stays vanilla; a dying entity's sound finishes where it died; without the hello the server sends vanilla; `SoundAnchor` unit-tested (5 tests; 54 in total) | AUTOMATED (Linux dev client, integrated server, Java 8, OpenAL null backend) | [`evidence/2026-11-03-entity-sounds-smoke.md`](evidence/2026-11-03-entity-sounds-smoke.md) |
| Next to other mods' buttons (Enhanced Sounds, Refresh Sounds), with Hodgepodge, with real AkashicFM/AkashicWeb sources, on Windows | **not observed** | needs an in-game test in the Forbidden pack |

## Open points

- In-game test on the Forbidden pack (client with Hodgepodge, Enhanced Sounds and Refresh Sounds) on Java 8 and on
  Java 17+ with lwjgl3ify.
- Hodgepodge also mixes into `SoundManager`; our `@Inject` at RETURN is designed to coexist, not yet observed.
- Akashic-Voice is not wired yet (`voice` channel reserved). Its output recovery waits for the mixer's engine
  recovery (`recoversSoundEngine()`) once its own branch with that change is built.
- Device loss on Windows with the real headset in the Forbidden pack (Dynamic Surroundings, ArchaicFix, Hodgepodge,
  Akashic Voice): the dev client only simulates it. On Java 17+ with lwjgl3ify: not run.
- `ALC_CONNECTED = false` with a live context is unit-tested only (OpenAL Soft's null backend never disconnects).
- A real Windows default-device switch (and how long device enumeration takes with Bluetooth devices) is not
  observed; the dev client has a single device.
- Cinematic Menu routing (2026-11-02): in-game check with AkashicCinematicMenu 0.7.0-alpha.5 (its OI-17).
- Entity sounds (2026-11-03): in-game check on a dedicated server with real ping, on Windows, in the Forbidden pack
  (Hodgepodge, Dynamic Surroundings and Sound Physics also touch sounds), with many mobs (cost of the per-frame
  position commands).
