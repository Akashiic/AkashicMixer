# 2026-10-18: dev client smoke test

Grade: **AUTOMATED (Linux dev client)**. Not the Forbidden pack, not Windows, no Hodgepodge, no other sound mods, no
AkashicFM or AkashicWeb loaded, nobody listened to the audio.

## Setup

- Tree: commit `bab631d` plus a temporary harness class (`devsmoke.DevSmoke`, enabled by `AKASHICMIXER_SMOKE=1` and
  registered from `ClientProxy.init`). The harness was deleted after the run and never committed.
- `./gradlew runClient` (GTNH buildscript 2.0.33, RetroFuturaGradle dev client): Java 1.8.0_504 (Temurin), LWJGL
  2.9.4, Forge 10.13.4.1614, UniMixins 0.3.1.
- Xvfb 1280x720, Mesa software GL (`LIBGL_ALWAYS_SOFTWARE=1`), OpenAL Soft with the `null` backend
  (`ALSOFT_DRIVERS=null`, as in AkashicFM's Phase 0). LWJGL 2 needs `xrandr`, missing in the container: a three-line
  stand-in script printing one 1280x720 mode was put on `PATH` for the run.

## What the harness did

1. On the main menu: registered the `screens`, `radios` and `music` channels (as AkashicWeb and AkashicFM do), set
   Master and Music to 100%, opened `GuiScreenOptionsSounds`, logged every button, took a screenshot.
2. Clicked the Akashic button through `GuiScreen.mouseClicked` (Forge's own `ActionPerformedEvent` path), logged the
   open screen and its buttons, took a screenshot.
3. Back on the main menu, waited for the vanilla menu music, then read the paulscode source volume
   (`SoundSystem.getVolume`) every 10 ticks while calling `AkashicMixerApi.reportAudible("smoke", 1f)` each tick for
   4 s, then for 8 s without reporting.

## Results

| Check | Result |
|---|---|
| The button is added to Music & Sound Options in the first free slot (x=58, y=148 = `height/6 + 108`, 150x20) without overlapping the vanilla buttons | pass |
| A click on it opens `GuiAkashicMixer` | pass |
| The mixer lists Akashic (all), Web Screens, Radios, Music (iPod), each with Mute; the music fade toggle (ON) and the floor slider (Silent) | pass |
| The mixin applies: the vanilla music's source volume follows the factor (1.0 → 0.0529 after 1.5 s → 0.0001 after 3.5 s) | pass |
| The music is not stopped while faded (`playing=true` throughout) | pass |
| After the reports stop, the music stays down for the 1.5 s freshness window, then fades back (0.31 after 2 s, 0.93 after 5 s, 0.99 after 8 s) | pass |

Screenshots (not committed; SHA-256):

- `1-sound-options.png` `4375714e0518cc6d96afb8bf0fa538bb588fc2896dc7ad0ed6f30f584c82f34b`
- `2-mixer.png` `c0eed4f4c8a7246e0171e17014be6b73b287e13d86b0215aef16c336f4c0af3f`

## Harness log (times are the client's clock)

```
04:24:13 button id=0 x=58 y=28 w=310 h=20 'Master Volume: 100%'
04:24:13 button id=1 x=58 y=52 w=150 h=20 'Music: 100%'
04:24:13 button id=2 x=218 y=52 w=150 h=20 'Jukebox/Noteblocks: 100%'
04:24:13 button id=3 x=58 y=76 w=150 h=20 'Weather: 100%'
04:24:13 button id=4 x=218 y=76 w=150 h=20 'Blocks: 100%'
04:24:13 button id=5 x=58 y=100 w=150 h=20 'Hostile Creatures: 100%'
04:24:13 button id=6 x=218 y=100 w=150 h=20 'Friendly Creatures: 100%'
04:24:13 button id=7 x=58 y=124 w=150 h=20 'Players: 100%'
04:24:13 button id=8 x=218 y=124 w=150 h=20 'Ambient/Environment: 100%'
04:24:13 button id=200 x=113 y=208 w=200 h=20 'Done'
04:24:13 button id=4279117 x=58 y=148 w=150 h=20 'Akashic Sounds Mixer...'
04:24:13 screen GuiScreenOptionsSounds 427x240 ourButton=true
04:24:13 Saved screenshot as 1-sound-options.png
04:24:13 after click: com.akashiic.mixer.client.GuiAkashicMixer
04:24:14 mixer button id=1100 x=58 y=28 w=250 'Akashic (all): 100%'
04:24:14 mixer button id=100 x=313 y=28 w=55 'Mute'
04:24:14 mixer button id=1101 x=58 y=52 w=250 'Web Screens: 100%'
04:24:14 mixer button id=101 x=313 y=52 w=55 'Mute'
04:24:14 mixer button id=1102 x=58 y=76 w=250 'Radios: 100%'
04:24:14 mixer button id=102 x=313 y=76 w=55 'Mute'
04:24:14 mixer button id=1103 x=58 y=100 w=250 'Music (iPod): 100%'
04:24:14 mixer button id=103 x=313 y=100 w=55 'Mute'
04:24:14 mixer button id=1 x=58 y=130 w=310 'Fade Minecraft music near Akashic sounds: ON'
04:24:14 mixer button id=2 x=58 y=154 w=310 'Minecraft music near Akashic sounds: Silent'
04:24:14 mixer button id=0 x=113 y=208 w=200 'Done'
04:24:15 Saved screenshot as 2-mixer.png
04:24:17 music d0ea25a1-a2bd-47df-af80-d4166528e4ec playing, volume 1.00000, factor 1.00000
04:24:17 ducking t=10 factor=0.39311 vol=0.393111 playing=true
04:24:18 ducking t=20 factor=0.13538 vol=0.154783 playing=true
04:24:18 ducking t=30 factor=0.04980 vol=0.052905 playing=true
04:24:19 ducking t=40 factor=0.01697 vol=0.019571 playing=true
04:24:19 ducking t=50 factor=0.00631 vol=0.006741 playing=true
04:24:20 ducking t=60 factor=0.00217 vol=0.002405 playing=true
04:24:20 ducking t=70 factor=0.00010 vol=0.000100 playing=true
04:24:21 ducking t=80 factor=0.00010 vol=0.000100 playing=true
04:24:21 recover t=10 factor=0.00010 vol=0.000100 playing=true
04:24:22 recover t=20 factor=0.00010 vol=0.000100 playing=true
04:24:22 recover t=30 factor=0.02446 vol=0.000100 playing=true
04:24:23 recover t=40 factor=0.32951 vol=0.312748 playing=true
04:24:23 recover t=50 factor=0.53925 vol=0.527640 playing=true
04:24:24 recover t=60 factor=0.69107 vol=0.675359 playing=true
04:24:24 recover t=70 factor=0.78778 vol=0.776885 playing=true
04:24:25 recover t=80 factor=0.85405 vol=0.846646 playing=true
04:24:25 recover t=90 factor=0.89969 vol=0.897199 playing=true
04:24:26 recover t=100 factor=0.93107 vol=0.929329 playing=true
04:24:26 recover t=110 factor=0.95262 vol=0.951432 playing=true
04:24:27 recover t=120 factor=0.96744 vol=0.966618 playing=true
04:24:27 recover t=130 factor=0.97762 vol=0.977057 playing=true
04:24:28 recover t=140 factor=0.98462 vol=0.984231 playing=true
04:24:28 recover t=150 factor=0.98969 vol=0.989163 playing=true
04:24:29 recover t=160 factor=0.99292 vol=0.992552 playing=true
04:24:29 DONE base=1.00000 final=0.992917
```

## Not covered

Hodgepodge's `SoundManager` mixin and logarithmic volume, Enhanced Sounds / Refresh Sounds buttons, lwjgl3ify on Java
17+, a real audio device, AkashicFM and AkashicWeb reporting real sources, config persistence across restarts.
