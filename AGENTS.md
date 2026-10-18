# AkashicMixer: engineering rules

Scope: this repository. Changes needed in AkashicFM or AkashicWeb are made in those repositories, with their own gates.

- Minecraft 1.7.10, Forge 10.13.4.1614, Java 8 source compatibility via Jabel, **class major 52** for every shipped
  class.
- The `api` and `core` packages are pure Java: no Minecraft client classes, safe to load on a dedicated server, unit
  tested headless. Only `client` and `mixins.client` touch the client.
- Mixins only where Forge has no hook, each narrow, namespaced (`akashicmixer$`) and documented with the reason.
  Today: one `@Inject` at RETURN of `SoundManager.getNormalizedVolume`, category *Music* only.
- Never touch vanilla categories other than *Music*; never stop a vanilla sound (keep the factor above 0).
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
