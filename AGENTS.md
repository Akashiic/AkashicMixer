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
