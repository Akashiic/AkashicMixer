# AkashicMixer

Sound core of the Akashic mods for Minecraft 1.7.10 (Forge 10.13.4.1614, GTNH stack). AkashicFM and AkashicWeb
depend on it.

## What it does

- **Akashic Sounds Mixer.** A button in *Options → Music & Sounds* (next to whatever other mods add there, such as
  Enhanced Sounds or Refresh Sounds) opens a screen with:
  - **Akashic (all)**: a master slider for every Akashic channel;
  - one slider and a **Mute** button per channel registered by an installed mod: **Web Screens** (AkashicWeb),
    **Radios** and **Music (iPod)** (AkashicFM), **Menu Music** and **Menu Sounds** (Akashic Cinematic Menu).
    **Voice** is reserved for Akashic-Voice;
  - **Fade Minecraft music near Akashic sounds** (on by default) and how much of it is left next to a source
    (default: silent).
  The same screen opens from the Forge mod list (*Config*).
- **Own channels, outside Minecraft's categories.** The Akashic mods used to read the vanilla *Jukebox/Note Blocks*
  slider. They now read their mixer channel, so that slider only affects vanilla jukeboxes and note blocks again.
  Minecraft's own *Master* slider still applies to everything.
- **Akashic sounds played through Minecraft's sound handler leave the vanilla sliders.** The Akashic Cinematic Menu's
  background music (category *Music* in its `sounds.json`) and interface sounds used to follow Minecraft's *Music*
  slider and nothing else. The menu routes them to its two mixer channels, and the mixer applies the channel level in
  place of the Minecraft category slider: Minecraft's *Music* slider no longer touches the menu, and the menu music is
  left out of the music fade. Only an Akashic mod's own sound events can be routed, never `minecraft:` ones.
- **Minecraft music fades out near Akashic audio.** Minecraft's background music (category *Music*, the vanilla music
  ticker) does not know a radio, speaker or screen is playing and used to play over it. While Akashic audio is
  audible the mixer fades that music out (1.5 s), proportionally to how loud the Akashic source is at the player, and
  fades it back (4 s) when the player walks away or the source stops. The track keeps running at -80 dB instead of
  being stopped, so it resumes where it was instead of waiting 10-20 minutes for the next one.

- **Sound survives the audio device going away.** Unplugging headphones or switching a Bluetooth headset off no
  longer leaves Minecraft silent until *Refresh Sounds*, and no longer crashes it: overlapping sound reloads from
  several mods are run one at a time, commands for an engine without a device are dropped instead of crashing, and
  the engine restarts by itself (engine only, no resource reload) within about 10 s of the device coming back.
- **Sound follows Windows' default output.** Turning a headset on while the speakers play (or back) moves every
  Minecraft sound to the new default device within a few seconds; background music that was playing resumes.

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
boolean mixerFollowsDefault = AkashicMixerApi.followsDefaultDevice(); // same for a Windows default-device change
```

Sounds a mod plays through Minecraft's own `SoundHandler` (events of its `sounds.json`) can be handed to a channel
instead of reading `level()` by hand:

```java
MixerChannel music = AkashicMixerApi.register(Channels.MENU_MUSIC, 80);
// Every "mymod:bgm.*" sound: volume = sound x channel level, whatever its sounds.json category is.
AkashicMixerApi.routeSounds("mymod", "bgm.", music);
```

The mixer applies the level every tick for tickable sounds (sliders move live) and when a sound starts. At level 0
Minecraft skips a starting sound; stop your own long sounds when `level()` is 0. `routeSounds` was added after the
first mixer build (`bab631d`): a mod that must also run with that build probes for the method first (the Cinematic
Menu does).

Dependency: `com.github.Akashiic:AkashicMixer:<commit>:dev` from JitPack (`https://jitpack.io`).

## How it works

| Piece | Where |
|---|---|
| Channel registry and levels (pure Java, safe on a dedicated server) | `api/AkashicMixerApi`, `api/MixerChannel` |
| Fade curve, report freshness, button placement (pure, unit-tested) | `core/` |
| Music fade: a factor applied to category *Music* in `SoundManager.getNormalizedVolume` (one `@Inject` at RETURN, client only), re-applied through `SoundHandler.setSoundLevel` | `mixins/client/MixinSoundManager`, `client/MusicDucker` |
| One reload at a time (`@Inject` at HEAD of `SoundManager.reloadSoundSystem`), crash guard (`@Inject` at HEAD of paulscode `SoundSystem.CommandQueue`), engine watchdog, default-device following, music resume | `mixins/client/MixinSoundManager`, `mixins/client/MixinSoundSystem`, `client/SoundEngineWatchdog`, `client/OpenAlEngine`, `client/MusicResume`, `core/ReloadGate`, `core/EngineRecovery`, `core/DefaultDeviceFollower` |
| Sound routing: route table (pure) and, in the same `@Inject`, the channel level in place of the category slider for routed sounds | `core/SoundRoutes`, `mixins/client/MixinSoundManager` |
| Mixer screen layout: vanilla-like, tighter or two columns so every channel fits a 427x240 screen (pure, unit-tested) | `core/MixerLayout`, `client/GuiAkashicMixer` |
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
