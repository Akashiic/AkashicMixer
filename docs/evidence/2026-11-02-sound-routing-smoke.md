# 2026-11-02: sound routing in the dev client

Grade: **AUTOMATED (Linux dev client)**. Not the Forbidden pack, not Windows, no Hodgepodge, no other sound mods, not
the real Akashic Cinematic Menu (a stand-in harness played its own sounds), nobody listened to the audio.

## Setup

- Tree: the sound routing change (uncommitted at the time, same content as the commit that adds this file) plus a
  temporary harness: an extra `@Mod` (`devsmoke.SmokeMod`, enabled by `AKASHICMIXER_SMOKE=1`) and an
  `assets/akmsmoke/sounds.json` with three streamed events pointing at vanilla menu tracks:
  `smoke.music` (category music), `smoke.ui` (category master), `plain.music` (category music, **not** routed).
  Both were deleted after the run and never committed.
- The harness registered `menu_music` and `menu_sfx` and routed `akmsmoke:smoke.music` and `akmsmoke:smoke.ui` to
  them, as the Cinematic Menu does for `akashiccinematic:bgm.*` and `akashiccinematic:ui.*`.
- `./gradlew runClient` (GTNH buildscript 2.0.33, RetroFuturaGradle dev client): Java 1.8.0_504, LWJGL 2.9.4,
  FML 7.99.40.1614, UniMixins. Xvfb 1280x720, Mesa software GL, OpenAL Soft `null` backend, an `xrandr` stand-in on
  `PATH` (as on 2026-10-18). Window 854x480, GUI scale auto: 427x240 scaled.
- Volumes read from paulscode (`SoundSystem.getVolume(source)`) one second after each change.

## Results

| Check | Observed | Result |
|---|---|---|
| Minecraft *Music* slider at 0, *Master* at 1, channels at 100 %: routed music (category music, sound volume 0.4) plays at the channel level | `playing=true source=0.4000` | pass |
| Routed UI sound (category master, 0.45) | `playing=true source=0.4500` | pass |
| Control: unrouted category-music sound with the *Music* slider at 0 is skipped by vanilla | `playing=false` | pass |
| Menu Music 50 %, Menu Sounds 20 %: live, without restarting the sounds | `0.2000`, `0.0900` | pass |
| Minecraft *Music* slider moved 0 → 1 → 0 (`setSoundCategoryVolume` re-applies the category): routed music neither stopped nor changed | `playing=true source=0.2000` | pass |
| Akashic (all) 50 % multiplies in | `0.1000` | pass |
| Menu Music muted: a playing tickable sound goes to 0 (the consumer stops it); a new routed sound is skipped | `source=0.0000`; new: `playing=false` | pass |
| Mixer screen with six rows (Akashic (all), Web Screens, Radios, Music (iPod), Menu Music, Menu Sounds) at 427x240: tighter rows, Done at y=210..230, nothing overlaps or leaves the screen | button log below, screenshot | pass |

Screenshot (not committed): `mixer-6rows.png` `b87570efb1836a4714dd2251a80accf7780f18aacf607eb41fb3f167676fcfd5`.

## Harness log

```
Minecraft Music slider = 0, Master = 1; Menu Music / Menu Sounds = 100%
routed music (cat MUSIC, vol 0.4): playing=true source=0.4000
routed ui (cat MASTER, vol 0.45): playing=true source=0.4500
unrouted control (cat MUSIC, vol 0.4): playing=false source=none
Menu Music -> 50%, Menu Sounds -> 20%
routed music: playing=true source=0.2000
routed ui: playing=true source=0.0900
Minecraft Music slider moved 0 -> 1 -> 0 (setSoundCategoryVolume)
routed music after slider re-apply: playing=true source=0.2000
Akashic (all) -> 50%
routed music: playing=true source=0.1000
Menu Music muted
routed music while muted: playing=true source=0.0000
new routed music started while muted: playing=false source=none
screen 854x480 scaled 427x240
button x=58 y=26 w=250 'Akashic (all): 100%'
button x=313 y=26 w=55 'Mute'
button x=58 y=48 w=250 'Web Screens: 100%'
button x=313 y=48 w=55 'Mute'
button x=58 y=70 w=250 'Radios: 100%'
button x=313 y=70 w=55 'Mute'
button x=58 y=92 w=250 'Music (iPod): 100%'
button x=313 y=92 w=55 'Mute'
button x=58 y=114 w=250 'Menu Music: 50%'
button x=313 y=114 w=55 'Mute'
button x=58 y=136 w=250 'Menu Sounds: 20%'
button x=313 y=136 w=55 'Mute'
button x=58 y=162 w=310 'Fade Minecraft music near Akashic sounds: ON'
button x=58 y=184 w=310 'Minecraft music near Akashic sounds: Silent'
button x=113 y=210 w=200 'Done'
Saved screenshot as mixer-6rows.png
DONE
```
