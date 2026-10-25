# AkashicMixer

Sound core of the Akashic mods for Minecraft 1.7.10 (Forge 10.13.4.1614, GTNH stack). AkashicFM and AkashicWeb
depend on it.

## What it does

- **Akashic Sounds Mixer.** A button in *Options → Music & Sounds* (next to whatever other mods add there, such as
  Enhanced Sounds or Refresh Sounds) opens a screen with:
  - **Akashic (all)**: a master slider for every Akashic channel;
  - one slider and a **Mute** button per channel registered by an installed mod: **Web Screens** (AkashicWeb),
    **Radios** and **Music (iPod)** (AkashicFM). **Voice** is reserved for Akashic-Voice;
  - **Fade Minecraft music near Akashic sounds** (on by default) and how much of it is left next to a source
    (default: silent).
  The same screen opens from the Forge mod list (*Config*).
- **Own channels, outside Minecraft's categories.** The Akashic mods used to read the vanilla *Jukebox/Note Blocks*
  slider. They now read their mixer channel, so that slider only affects vanilla jukeboxes and note blocks again.
  Minecraft's own *Master* slider still applies to everything.
- **Minecraft music fades out near Akashic audio.** Minecraft's background music (category *Music*, the vanilla music
  ticker) does not know a radio, speaker or screen is playing and used to play over it. While Akashic audio is
  audible the mixer fades that music out (1.5 s), proportionally to how loud the Akashic source is at the player, and
  fades it back (4 s) when the player walks away or the source stops. The track keeps running at -80 dB instead of
  being stopped, so it resumes where it was instead of waiting 10-20 minutes for the next one.

- **Sound survives the audio device going away.** Unplugging headphones or switching a Bluetooth headset off no
  longer leaves Minecraft silent until *Refresh Sounds*, and no longer crashes it: overlapping sound reloads from
  several mods are run one at a time, commands for an engine without a device are dropped instead of crashing, and
  the engine restarts by itself (engine only, no resource reload) within about 10 s of the device coming back.

Settings live in `config/akashicmixer.cfg` (written when the mixer screen closes).

## For mod authors (API, version 1)

```java
// Client pre-init (the mixer pre-inits first: declare "required-after:akashicmixer").
MixerChannel radios = AkashicMixerApi.register(Channels.RADIOS, 20);

// Wherever you used mc.gameSettings.getSoundLevel(SoundCategory.RECORDS):
float level = radios.level(); // 0..1 = channel x Akashic master, 0 when muted

// Every client tick: how loud your audio is at the listener (0..1, after distance and channel level).
AkashicMixerApi.reportAudible("mymod", loudestGain);

// Lost your OpenAL output? If this is true, wait for the mixer to restart Minecraft's engine; do not reload it.
boolean mixerRecovers = AkashicMixerApi.recoversSoundEngine();
```

Dependency: `com.github.Akashiic:AkashicMixer:<commit>:dev` from JitPack (`https://jitpack.io`).

## How it works

| Piece | Where |
|---|---|
| Channel registry and levels (pure Java, safe on a dedicated server) | `api/AkashicMixerApi`, `api/MixerChannel` |
| Fade curve, report freshness, button placement (pure, unit-tested) | `core/` |
| Music fade: a factor applied to category *Music* in `SoundManager.getNormalizedVolume` (one `@Inject` at RETURN, client only), re-applied through `SoundHandler.setSoundLevel` | `mixins/client/MixinSoundManager`, `client/MusicDucker` |
| One reload at a time (`@Inject` at HEAD of `SoundManager.reloadSoundSystem`), crash guard (`@Inject` at HEAD of paulscode `SoundSystem.CommandQueue`), engine watchdog | `mixins/client/MixinSoundManager`, `mixins/client/MixinSoundSystem`, `client/SoundEngineWatchdog`, `client/OpenAlEngine`, `core/ReloadGate`, `core/EngineRecovery` |
| Options button, mixer screen, config | `client/` |

## Build

```
./gradlew build
```

Java 8 bytecode (class major 52). Every push publishes the jars and `SHA256SUMS-<sha>.txt` in GitHub Actions
(*Build* → *Artifacts*), each as a plain file, not a zip. See [`docs/PROJECT_STATE.md`](docs/PROJECT_STATE.md) for what has and has not been
verified.

## License

MIT, see [LICENSE](LICENSE).
