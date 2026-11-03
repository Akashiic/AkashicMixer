# 2026-11-03: entity sounds in the dev client

Grade: **AUTOMATED (Linux dev client, integrated server)**. Not a dedicated server, not Windows, not the Forbidden
pack, no other sound mods (Hodgepodge, Dynamic Surroundings, Sound Physics), nobody listened to the audio. Positions
were read from paulscode (`Library.getSources().get(channel).position`), which is what OpenAL is given.

## Setup

- Tree: the entity sounds commit plus the "standing still" check of the following commit, and a temporary harness
  (`client/SmokeEntitySounds`, enabled by `AKASHICMIXER_SMOKE=1`, registered from `ClientProxy.init`) with temporary
  log lines in `EntitySoundServer.send` and `EntitySounds.play`. Harness and log lines were deleted after the run and
  never committed.
- `./gradlew runClient` (GTNH buildscript 2.0.33, RetroFuturaGradle): Java 1.8.0_504 (Temurin), LWJGL 2.9.4,
  Forge 10.13.4.1614, UniMixins. Xvfb 1280x720, Mesa software GL (about 30 frames per second), OpenAL Soft `null`
  backend, `xrandr` stand-in on `PATH`, as on 2026-10-18.
- `pauseOnLostFocus` turned off by the harness: under Xvfb the window has no focus, the pause menu opens and the
  integrated server stops ticking (the first two runs measured nothing because of this).
- Flat creative world. Test sounds are vanilla records (long, so they are still playing while measured). A pig is
  moved by the server 0.5 block per tick (10 blocks/s); the player is moved 1.1 blocks per tick (22 blocks/s, sprint
  creative flight).

## Results (final run)

| Scenario | Observed | Result |
|---|---|---|
| Hello: the client said hello on joining | server `CAPABLE` = 1 | pass |
| A. Server `playSoundAtEntity(pig)` while the pig moves 31 blocks | played as `EntityBoundSound`; sound to pig 0.00 (one sample 0.57, between two tracker updates); sound moved 31.17 blocks with it | pass |
| B. Server `playSoundEffect` at the pig's exact coordinates (`matchByPosition`) | bound to the pig; followed it 19.83 blocks, sound to pig 0.00 | pass |
| C. Control: `playSoundEffect` 0.37 / 0.81 / 0.29 blocks off the pig | vanilla `PositionedSoundRecord`, did not move | pass |
| D. Server `playSoundAtEntity(player)`, player flying 22 blocks/s, `smoothEveryFrame` on | bound to the local player; sound to player 0.00 at every tick sample over 66 blocks; per frame (98 frames) mean 0.000, max 0.000 blocks | pass |
| E. Same with `smoothEveryFrame` off | follows at each tick; per frame (106 frames) mean 0.549, max 1.084 blocks: the 20 Hz jump that per-frame placement removes | pass (shows why the option exists) |
| F. The local player's own client sound (`EntityPlayerSP.playSound`) | bound to the player; per frame (129 frames) mean 0.000, max 0.000 | pass |
| G. The pig is killed while its sound plays | the sound followed it until death (9.67 blocks), then stayed where the pig died and kept playing | pass |
| H. Fallback: the server forgets the hello | vanilla `PositionedSoundRecord` at the start point; the pig walked 19.59 blocks away from its own sound (the vanilla behaviour the change removes) | pass |
| Natural vanilla sounds during the run | slime and pig sounds and `step.grass` arrived as entity sounds and were bound to their mob | observed |

Harness artefacts seen on the way and fixed in the harness, not in the mod: a pig teleported in the same tick as its
sound is still registered in its old chunk, so the position match misses it; a pig teleported back into tracking
range is not resent to the client for 30+ ticks, so its sound arrives before the entity and the client uses the
vanilla point (the designed fallback).

## Log of the final run (harness lines)

```
in world; config enabled=true smooth=true server capable players=1
A playSoundAtEntity(pig) t=22 sound=EntityBoundSound source=(511.50, 4.00, 15.50) clientPig=(511.50, 4.00, 15.50) sourceToPig=0.00 sourceFromStart=0.00
A playSoundAtEntity(pig) t=82 sound=EntityBoundSound source=(542.67, 4.00, 15.50) clientPig=(542.67, 4.00, 15.50) sourceToPig=0.00 sourceFromStart=31.17
B playSoundEffect(at pig) t=27 sound=EntityBoundSound source=(511.33, 4.00, 15.50) clientPig=(511.33, 4.00, 15.50) sourceToPig=0.00 sourceFromStart=0.00
B playSoundEffect(at pig) t=67 sound=EntityBoundSound source=(491.50, 4.00, 15.50) clientPig=(491.50, 4.00, 15.50) sourceToPig=0.00 sourceFromStart=19.83
C playSoundEffect(next to pig) t=43 sound=PositionedSoundRecord source=(511.75, 4.75, 15.13) clientPig=(511.50, 4.00, 15.50) sourceToPig=0.88 sourceFromStart=0.00
D server playSoundAtEntity(player), smooth on t=72 sound=EntityBoundSound source=(586.60, 11.81, 12.50) player=(586.60, 11.81, 12.50) sourceToPlayer=0.00 sourceFromStart=66.00
D server playSoundAtEntity(player), smooth on: per-frame distance source to interpolated player feet over 98 frames: mean 0.000 max 0.000
E server playSoundAtEntity(player), smooth off: per-frame distance source to interpolated player feet over 106 frames: mean 0.549 max 1.084
F client thePlayer.playSound, smooth on t=72 sound=EntityBoundSound source=(749.31, 11.81, 12.50) player=(749.40, 11.81, 12.50) sourceToPlayer=0.09 sourceFromStart=65.91
F client thePlayer.playSound, smooth on: per-frame distance source to interpolated player feet over 129 frames: mean 0.000 max 0.000
G pig dies at t=51 t=43 sound=EntityBoundSound source=(760.71, 4.00, 15.50) clientPig=(760.71, 4.00, 15.50) sourceToPig=0.00 sourceFromStart=4.69
G pig set dead on the server
G pig dies at t=51 t=53 sound=EntityBoundSound source=(765.69, 4.00, 15.50) clientPig=(765.69, 4.00, 15.50) sourceToPig=0.00 sourceFromStart=9.67
G pig dies at t=51 t=93 sound=EntityBoundSound source=(765.69, 4.00, 15.50) clientPig=gone sourceToPig=- sourceFromStart=9.67
H no hello: vanilla t=62 sound=PositionedSoundRecord source=(755.63, 4.00, 15.50) clientPig=(775.22, 4.00, 15.50) sourceToPig=19.59 sourceFromStart=0.00
DONE
```

## Not run

A dedicated server with several players (the integrated server has one, and its latency is near zero, so the "late
by one ping" part of the owner's case is not reproduced here); Windows; the Forbidden pack and its sound mods; how
the added per-frame position commands cost with many mobs (capped at 64 bound sounds, skipped while the entity stands
still); a player listening.
