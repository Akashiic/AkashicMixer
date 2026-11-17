# 2026-10-25: sound engine after an audio device is lost (crash guard, one reload at a time, recovery)

Owner report (2026-10-25): with a Bluetooth headset (JBL), taking it off makes Windows drop the output and
Minecraft goes silent; putting it back brings Windows' sound back but not Minecraft's, which needs *Refresh Sounds*;
around this the game freezes badly or crashes. Four crash reports attached.

Grades: crash analysis **STATIC**; the fix **BUILD** and **AUTOMATED (Linux dev client)**. Not observed on Windows,
with a real Bluetooth device, or in the Forbidden pack. Nobody listened to the audio.

## Field input (owner's crash reports, not committed)

| Report | SHA-256 | Signature | Audio? |
|---|---|---|---|
| `crash-2026-10-20_21.07.50-client.txt` | `d23b22edad82e716c39e3e88c00e4123a958911a9c82dab2b43305b872a6ff28` | `UnsatisfiedLinkError: org.lwjgl.openal.AL10.nalListenerf(IF)V` in `LibraryLWJGLOpenAL.setMasterVolume`, from `SoundSystem.CommandQueue` called by Dynamic Surroundings' `SoundManager.flushSound` on the client thread | yes |
| `crash-2026-10-17_00.21.04-client.txt` | `1af8006f8add1425460ea0de4833cb420265ff231fc9b55e9e641ebeb7389a10` | `NullPointerException` in `LibraryLWJGLOpenAL.setListenerPosition`, same path | yes |
| `crash-2026-10-17_17.55.23-client.txt` | `a0f92b82e4cbc2c5fceaffc7b948a07688414581b43875aa35e484ecca2a0fda` | `IllegalArgumentException: Could not get provider type for dimension -3` on `S07PacketRespawn` (Paper backend) | **no**: a dimension id the client does not know |
| `crash-2026-10-17_17.55.46-client.txt` | `8cde593d1d6d55548326ac33e770793a892488f23b0ab9fd011597b3bfeb3085` | same as above | **no** |

Environment in the reports: Windows 8.1, Java 1.8.0_51, LWJGL 2.9.1, Forge 10.13.4.1614, 285 mods; sound-related:
ArchaicFix 0.8.0, Hodgepodge 2.7.189, Dynamic Surroundings 1.0.6.4, AkashicFM `bffcade`, AkashicMixer `c60f79e`,
Akashic Voice `0.1.0-alpha.5-akashic-voice.11+022dc1f922-dirty`.

## What the code does (STATIC)

Decompiled (CFR 0.152) from the jars the pack uses: paulscode `soundsystem-20120107`
(`2882d645…`), `librarylwjglopenal-20100824` (`107d1150…`), LWJGL 2.9.1 (`c118f8d8…`), ArchaicFix 0.8.0
(`15d0de52…`), Hodgepodge 2.7.189 (`7d7650c7…`); Akashic Voice and AkashicFM from source.

1. LWJGL 2 has **one** global OpenAL context. `AL.create()` throws `IllegalStateException` if one exists; if no
   device opens it calls `AL.destroy()`, which resets the native stubs of AL10/AL11/ALC10/ALC11/EFX10, so every later
   OpenAL call raises `UnsatisfiedLinkError` until the next successful `AL.create()`.
2. `SoundManager.reloadSoundSystem()` unloads the engine on the calling thread and starts the new one on a
   "Sound Library Loader" thread; `loaded` stays false until that thread is done. A second reload in that window skips
   the unload and starts a **second** loader. The second engine's `AL.create()` meets the first one's context and
   throws `IllegalStateException`, which `LibraryLWJGLOpenAL.init` does not catch: that paulscode thread dies with a
   library whose listener buffers were never allocated, and both loaders wait for paulscode's static `initialized`
   flag for up to 40 s.
3. Who reloads when a device goes away: ArchaicFix's *Default Sound Device Checker* (every 1 s; schedules
   `reloadSoundSystem` on the client thread; it calls ALC outside `THREAD_SYNC` and its thread ends at the first
   `Throwable`, so after one `UnsatisfiedLinkError` it never checks again); Akashic Voice's output recovery
   (`SoundHandler.onResourceManagerReload`, a full sound-resource reload, then again every 5 s while the device is
   missing); Hodgepodge's *Refresh Sounds* button (also a full resource reload). Two of them reacting to the same
   unplug within a second is enough for 2.
4. paulscode's `SoundSystem.cleanup()` and a failed library init leave the active `LibraryLWJGLOpenAL` with
   `listenerPositionAL == null` and/or the stubs reset. Commands run against it raise exactly the two audio
   signatures above. Normally they run on the paulscode thread (which then dies: no sound until a reload); Dynamic
   Surroundings runs `CommandQueue(null)` on the client thread, so there the error crashes the game.
5. With no device at all, paulscode ends in its silent fallback (`Library`, "Switching to No Sound") and nothing
   restarts it once the device is back: ArchaicFix only checks while `AL.isCreated()`.
6. Hodgepodge installs `LibraryHodgepodgeOpenAL extends LibraryLWJGLOpenAL` (no `init`/`cleanup` override);
   AkashicFM's mixin on `LibraryLWJGLOpenAL` only observes `init`/`cleanup`. Neither changes the above.

The exact interleaving that produced each of the two field crashes is inferred, not observed.

## The fix (this commit)

- `MixinSoundManager`: `reloadSoundSystem` asked for while the last reload is still loading is deferred and run once
  it is loaded (`core/ReloadGate`, limit 60 s). Always on.
- `MixinSoundSystem`: `CommandQueue(null)` on an OpenAL library without a usable context (`!AL.isCreated()` or
  `listenerPositionAL == null`) drops the queued commands instead of running them; a queued `NEW_LIBRARY` still runs.
  Always on.
- `SoundEngineWatchdog` (config `sound_engine.recoverEngine`, default on): twice a second reads the engine; restarts
  it with `reloadSoundSystem()` only (engine, not the sound registry) when the OpenAL library is dead or paulscode fell
  back to silent mode (after 1 s, then 2, 4, 8, 10, 10… s), or when the device reports `ALC_CONNECTED = false` for 3 s
  (`core/EngineRecovery`).
- `AkashicMixerApi.recoversSoundEngine()`: lets Akashic Voice wait for the engine instead of reloading it.

## Dev client run (AUTOMATED, Linux)

Setup as in [`2026-10-18-dev-client-smoke.md`](2026-10-18-dev-client-smoke.md): `./gradlew runClient`, Java
1.8.0_504 (Temurin), LWJGL 2.9.4, Forge 10.13.4.1614, UniMixins 0.3.1, Xvfb, OpenAL Soft (LWJGL's bundled build).
No other mods. A temporary harness class (registered from `ClientProxy.init` when `AKASHICMIXER_SMOKE` was set) was
deleted after the run and never committed. Device loss was simulated by pointing `ALSOFT_CONF` at a file whose
`drivers` line the harness switched between `null` (a working device) and `nonexistent` (no backend: `alcOpenDevice`
fails, as with no output device); OpenAL Soft re-reads it each time `AL.create()` loads the library.
"DynSurround-style flush": the harness queued `setListenerPosition`, `setMasterVolume` and a sound, then called
`SoundSystem.CommandQueue(null)` on the client thread, as Dynamic Surroundings does.

| Scenario | Without the fix (A: run on the tree before `ReloadGate`; B, C: the library called directly, which is what an unguarded queue runs) | With the fix |
|---|---|---|
| A. Two `reloadSoundSystem()` in the same tick | `IllegalStateException: Only one OpenAL context may be instantiated at any one time.` on the second paulscode thread; engine still not loaded 30 s later | second reload deferred; engine healthy 32 ticks (1.6 s) later; no `IllegalStateException` |
| C. The state A leaves (AL created, an OpenAL library never initialised) | direct `setListenerPosition` → `NullPointerException` (field crash 2's signature) | the client-thread flush drops the commands, no crash; watchdog restart; healthy 44 ticks later |
| B. Device lost + one reload, device back after 7 s | paulscode falls back to silent mode; an OpenAL call with the context gone → `UnsatisfiedLinkError: org.lwjgl.openal.AL10.nalListenerf(IF)V` (field crash 1's signature) | silent engine detected; restarts at 1, 2 and 4 s fail while no device; the first restart after the device is back succeeds (33 ticks after); sounds queue and flush afterwards |

No `Exception in thread` and no "Command thread did not die" in the run with the fix. Logs not committed:
`run2.log` (before, A only) `284a27e2890c320dcbe3beb1bb3e8a7f4bca6b382b9e263b0f56597c2b075711`, `run4-dev-client.log`
(with the fix) `7cb7acc7bac49389cd66bf9fff7c8f6057a734ac473e539b8743aa0370fef2c9`.

Excerpt of the run with the fix (client clock):

```
05:58:20 [EngineSmoke] A: healthy (library=LibraryLWJGLOpenAL listenerPositionAL=allocated AL.isCreated=true); two sound reloads in the same tick
05:58:20 AkashicMixer: sound reload asked for while the last one is still loading; deferred
05:58:21 [EngineSmoke] A: HEALTHY 32 ticks after the double reload: library=LibraryLWJGLOpenAL listenerPositionAL=allocated AL.isCreated=true
05:58:21 [EngineSmoke] C: swapped in an uninitialised library: library=LibraryLWJGLOpenAL listenerPositionAL=null AL.isCreated=true isDead=true
05:58:21 [EngineSmoke] C: direct setListenerPosition -> java.lang.NullPointerException
05:58:21 [Thread-13/WARN] AkashicMixer: Minecraft's sound engine has no usable OpenAL context (audio device lost or overlapping sound reloads); dropping its sound commands until it is restarted
05:58:21 [EngineSmoke] C: DynSurround-style client-thread flush returned false, no crash
05:58:23 AkashicMixer: sound engine has no usable audio device; restarting it (attempt 1)
05:58:24 [EngineSmoke] C: HEALTHY again 44 ticks later: library=LibraryLWJGLOpenAL listenerPositionAL=allocated AL.isCreated=true
05:58:24 AkashicMixer: sound engine is back after 1 restart(s)
05:58:26 [EngineSmoke] B: device lost now (drivers=nonexistent) and one reload
05:58:27 [Thread-20] Switching to No Sound
05:58:27 [EngineSmoke] B: DEAD after 29 ticks: library=Library listenerPositionAL=- AL.isCreated=false
05:58:27 [EngineSmoke] B: setMasterVolume on an OpenAL library with AL destroyed -> java.lang.UnsatisfiedLinkError: org.lwjgl.openal.AL10.nalListenerf(IF)V
05:58:29 AkashicMixer: sound engine has no usable audio device; restarting it (attempt 1)
05:58:31 AkashicMixer: sound engine has no usable audio device; restarting it (attempt 2)
05:58:34 [EngineSmoke] B: device back now (drivers=null)
05:58:35 AkashicMixer: sound engine has no usable audio device; restarting it (attempt 3)
05:58:36 [EngineSmoke] B: HEALTHY again 33 ticks after the device came back: library=LibraryLWJGLOpenAL listenerPositionAL=allocated AL.isCreated=true
05:58:36 AkashicMixer: sound engine is back after 3 restart(s)
05:58:38 [EngineSmoke] PASS: sounds queue and flush after recovery
```

## Not covered

- Windows, a real Bluetooth device, Java 8u51, LWJGL 2.9.1 (the dev client has 2.9.4), the Forbidden pack, Dynamic
  Surroundings itself, ArchaicFix's checker and Akashic Voice running alongside. lwjgl3ify (Java 17+) not run.
- `ALC_CONNECTED = false` with a live context (scenario "disconnected") is unit-tested only: OpenAL Soft's null backend
  never reports a disconnect.
- Akashic Voice creating the OpenAL context itself while Minecraft's engine has none (its `initOpenAl`) would make
  every Minecraft restart fail with `IllegalStateException`; not addressed here.
- The dimension -3 crashes are not audio and are not addressed.
