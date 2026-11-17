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
  `WorldClient.playSound` (`@Inject` at HEAD, cancellable: a client sound exactly at an entity stays on it), and
  accessors on `SoundHandler`, `SoundManager`, `SoundSystem` and `LibraryLWJGLOpenAL`. Both sides:
  `World.playSoundAtEntity` / `playSoundToNearExcept` / `playSoundEffect` (`@Inject` at HEAD and RETURN on server
  worlds: remember whose sound is being sent), `NetHandlerPlayServer.sendPacket` (`@Inject` at HEAD, cancellable: the
  final per-player send of `S29PacketSoundEffect`) and an accessor on `S29PacketSoundEffect`.
- Entity sounds never choose who hears a sound: they act on the final per-player send, after vanilla, Forge, Bukkit
  (vanish, `canSee`) and plugins chose the players, so Crucible/Thermos keep their rules. A client with the mixer
  (hello on the `akashicmixer` channel) gets a sound that belongs to an entity bound to it; everyone else gets the
  vanilla packet, except a sound sent straight to one player at that player (`/playsound`, `Player.playSound`), which
  is moved from the block corner/centre to the player (the `/playsound` fix). Do not add `@Shadow`s on classes
  Bukkit patches (Crucible changes `WorldManager.theWorldServer` to public). Bump `MixerNetwork.PROTOCOL` when a
  message changes.
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
