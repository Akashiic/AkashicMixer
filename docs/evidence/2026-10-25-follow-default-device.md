# 2026-10-25: following Windows' default output device

Owner request (2026-10-25): when Windows switches its default output (the JBL headset turned on while the speakers
play, or back), Minecraft should move to it, every Minecraft sound should be heard there, and nothing should break.

Grades: OpenAL behaviour **STATIC** (OpenAL Soft 1.15.1 source, the version in LWJGL 2.9.1's natives); the logic
**AUTOMATED** (JUnit, Linux); the watchdog path **AUTOMATED (Linux dev client)** with the "default moved" signal
injected, because OpenAL Soft's null backend has a single device. **Not observed on Windows or with a real device
switch.**

## What OpenAL Soft 1.15.1 reports (STATIC)

LWJGL 2.9.1's `OpenAL64.dll` (`9261b66010a845ddef9f61d5e4266fe2f08a53f3605da002e9e8f8d202bdbc5e`, from
`lwjgl-platform-2.9.1-natives-windows.jar`) identifies itself as `ALSOFT 1.15.1`, has the `mmdevapi` (WASAPI),
`dsound` and `winmm` backends and `ALC_ENUMERATE_ALL_EXT`. In that version's `Alc/ALc.c` and
`Alc/backends/mmdevapi.c`:

- `ALC_DEFAULT_DEVICE_SPECIFIER` and `ALC_DEVICE_SPECIFIER` are the constant `"OpenAL Soft"`. Any check built on them
  (ArchaicFix 0.8.0's `SoundDeviceThread.getDefault`, Akashic Voice's bound-device name) can never see a device change.
- `ALC_DEFAULT_ALL_DEVICES_SPECIFIER` is the first entry of the device list, and the list is only enumerated again
  when `ALC_ALL_DEVICES_SPECIFIER` is asked for without a device. Nothing in the Akashic mods or ArchaicFix asks for
  it, so today the "default" never changes after the first query.
- `mmdevapi` enumerates Windows' default endpoint (`GetDefaultAudioEndpoint`, eMultimedia) first, and opens it when no
  device name is given (Minecraft passes none). `ALC_ALL_DEVICES_SPECIFIER` on an open device is the same friendly
  name.

So the mixer re-enumerates (`alcGetString(null, ALC_ALL_DEVICES_SPECIFIER)`), reads the first entry as Windows'
default, and compares it with the engine's device name.

## The change

- `core/DefaultDeviceFollower`: only a change of the default since the current engine started counts; a name that
  merely differs never does, so a restart cannot loop. Each new engine starts a new baseline.
- `core/EngineRecovery`: `DEFAULT_CHANGED`, restarted after 2.5 s (two device checks agree), so a headset switching
  profiles while it connects gives one restart.
- `SoundEngineWatchdog`: the device check runs every 2 s on the client thread under paulscode's `THREAD_SYNC` (where
  Minecraft itself creates and destroys the context); if two checks in a row take over 8 ms it drops to every 10 s. A new
  engine is checked at once. Config `sound_engine.followDefaultDevice` (on; needs `recoverEngine`).
- `MusicResume`: a restart stops every sound and vanilla's `MusicTicker` then waits 10-20 minutes in game for the next
  track. After the mixer's own restart, if background music was playing, that wait is cut to 100 ticks (5 s).
- `AkashicMixerApi.followsDefaultDevice()` (additive), so Akashic Voice does not reload the engine itself on the same
  change.

## Dev client run (AUTOMATED, Linux)

Same setup as [`2026-10-18-dev-client-smoke.md`](2026-10-18-dev-client-smoke.md) (Java 1.8.0_504, LWJGL 2.9.4 with
OpenAL Soft's null backend, no other mods). A temporary harness, deleted after the run and never committed:

| Check | Result |
|---|---|
| The device check reads names on the real native library | default `No Output`, engine `No Output` |
| 30 s of normal play with the watchdog checking every 2 s, plus 600 extra checks from the harness | same engine throughout, no restart; slowest check 636 µs |
| "Windows default moved" (harness sets the watchdog's moved flag while music plays; the null backend has one device) | restart 2.5 s later, logged as `sound engine is on 'Speakers' but Windows now plays on 'Headphones (JBL)'`; the new engine is checked at once and settles (78 ticks after the move) |
| Music after that restart | the ticker's pause (293 ticks) cut to 100; music playing again 102 ticks after the restart; no second restart in the next 5 s |
| Device lost with music playing, back after 2 s | engine dead → restart → healthy; music playing again 135 ticks after the device came back |

Log not committed: `run5-follow-default.log` `f5758d8699eee33fe644b9a4f3971bc57a26a4b881c9c055d8232cf0f5404206`.

## What a restart does and does not keep

All of Minecraft's sounds go through one paulscode engine, so after the restart every one of them (blocks, mobs,
weather, records, music, mods using Minecraft's sound system) plays on the new device. A restart stops the sounds that
were playing at that moment: short ones are simply lost, music comes back within 5 s, sources other mods keep
re-playing (AkashicFM rebuilds its sources on the new context) come back; a mod's looping sound that is only started
once may stay silent until it is started again. That is what F3+T and Refresh Sounds already do.

## Not covered

Windows, a real device switch, how long the enumeration takes with Bluetooth devices (hence the 8 ms fallback),
multi-output setups that Windows switches on its own, lwjgl3ify (a newer OpenAL Soft names devices differently; the
follower still cannot loop).
