# AkashicMixer: engineering rules

Scope: this repository. Changes needed in AkashicFM or AkashicWeb are made in those repositories, with their own gates.

- Minecraft 1.7.10, Forge 10.13.4.1614, Java 8 source compatibility via Jabel, **class major 52** for every shipped
  class.
- The `api` and `core` packages are pure Java: no Minecraft client classes, safe to load on a dedicated server, unit
  tested headless. Only `client` and `mixins.client` touch the client. `net`, `server` and `mixins.common` run on both
  sides (dedicated and integrated server) and never reference a client class.
- Mixins only where Forge has no hook, each narrow, namespaced (`akashicmixer$`) and documented with the reason.
  Today: `SoundManager.getNormalizedVolume` (`@Inject` at RETURN: the music fade, category *Music* only, and the
  sounds an Akashic mod routed to a channel with `AkashicMixerApi.routeSounds`, by sound event id),
  `SoundManager.reloadSoundSystem` (`@Inject` at HEAD: defers a reload while the previous one still loads),
  paulscode `SoundSystem.CommandQueue` (`@Inject` at HEAD: drops commands for an OpenAL library without a context),
  `EntityPlayerSP.playSound` (`@Inject` at its `World.playSound` call: the local player's own sound stays on the
  player), and accessors on `SoundHandler`, `SoundManager`, `SoundSystem` and `LibraryLWJGLOpenAL`. Server side:
  `World.playSoundAtEntity` / `playSoundToNearExcept` (`@Inject` at HEAD and RETURN: remember the entity) and
  `WorldManager.playSound` / `playSoundToNearExcept` (`@Inject` at HEAD, cancellable: send entity sounds).
- Entity sounds change only where a sound plays, never its volume, and only between a server and a client that both
  have the mixer (hello on the `akashicmixer` channel); everyone else gets the exact vanilla packet, to the players
  vanilla would choose. Bump `MixerNetwork.PROTOCOL` when a message changes.
- Never call `SoundHandler.onResourceManagerReload` to recover audio: restart the engine with
  `SoundManager.reloadSoundSystem()` (a full resource reload freezes a large pack for seconds).
- Never touch vanilla categories other than *Music*; never stop a vanilla sound (keep the factor above 0). Sound
  routing only ever applies to an Akashic mod's own `sounds.json` events; the `minecraft` domain cannot be routed.
- Keep the API backward compatible; bump `AkashicMixerApi.API_VERSION` on an incompatible change and update the
  consumers in the same series.
- Evidence grades: AUTHORED, STATIC, BUILD, AUTOMATED, LIVE-OBSERVED, LIVE-APPROVED. State the OS for AUTOMATED
  results. A unit test never substitutes for an in-game observation.
- No release tag or "stable" wording without in-game evidence. Do not commit JARs, logs or captures; record SHA-256.
- Never reset, clean or stash changes you did not make. State plainly what you did not run.

## Build delivery protocol (owner request, 2026-10-18)

Builds are delivered as **plain files, never zipped**:

- **GitHub Actions** publishes each file as its own artifact with `actions/upload-artifact@v7` and `archive: false`:
  the mod jar (`<mod>-<sha>.jar`, the one players install), `-dev.jar`, `-sources.jar` (AkashicFM also
  `-dev-preshadow.jar`) and `SHA256SUMS-<sha>.txt`. Do not go back to one zipped bundle.
- **In a chat session**, after building, send the same files to the owner individually (mod jar, dev, sources, and
  for AkashicFM the pre-shadow dev jar) together with a `SHA256SUMS.txt`; say which jar goes in `mods` and whether the
  jars were built from a clean, committed tree.
- JARs are still never committed to the repository; their SHA-256 goes in the evidence instead.
