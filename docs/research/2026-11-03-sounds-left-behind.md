# Sounds left behind when flying fast: 1.7.10 versus current Minecraft

Date: 2026-11-03. Owner question: "in 1.7.10 you hear a sound, fly away fast and the sound stays behind; recent
versions are not like that. Can the mixer backport the new sound handling?"

Grades as in `AGENTS.md`. Everything here is STATIC (code reading or published sources); nothing was observed in game.

## Sources

- 1.7.10: the MCP-decompiled Minecraft/Forge 10.13.4.1614 sources produced by this build (`build/rfg/minecraft-src`)
  and paulscode `soundsystem-20120107` + `librarylwjglopenal-20100824` from the Gradle cache, decompiled with MCP's
  fernflower.
- Current: Mojang's official 26.3 client jar (released 2026-09-15, SHA-1
  `e877b6a07acd633fb3bb475002175cec036e7b87`, matching the launcher manifest; shipped without obfuscation),
  selected classes decompiled with Vineflower 1.11.1, plus a constant-pool scan of every class for sound call sites.
  Not committed.
- Mojang bug tracker and the Minecraft wiki (links below).

## How 1.7.10 places a sound (STATIC)

1. **Listener**: moved every frame. `Minecraft.runGameLoop` calls `SoundHandler.setListener(thePlayer,
   renderPartialTicks)` with the interpolated position and look.
2. **A one-shot sound is a point.** `PositionedSoundRecord` keeps the x, y, z it was created with. Only an
   `ITickableSound` (vanilla: `MovingSoundMinecart`, `MovingSoundMinecartRiding`) gets `setPosition` again, once per
   tick, in `SoundManager.updateAllSounds`.
3. **Attenuation**: paulscode linear model, computed in Java (`SourceLWJGLOpenAL.calculateGain`:
   `gain = 1 - distance / distOrRoll`, `distOrRoll = 16 x max(1, volume)`), re-applied on every listener or source
   move. Doppler factor defaults to 0 and no velocities are ever set.
4. **Where sounds come from**:
   - `EntityPlayerSP.playSound` (the local player's steps, eating while chewing, hurt, ...) plays locally at once;
     the server side `EntityPlayer.playSound` uses `playSoundToNearExcept`, so the player is not sent a copy.
   - `World.playSoundAtEntity(entity, ...)` (21 vanilla call sites: bow, food burp, potions, bottles, saddles,
     mobs, ...) and `World.playSoundEffect(x, y, z, ...)` run on the server only. `WorldManager` broadcasts an
     `S29PacketSoundEffect` with the position (1/8 block precision) to everyone in range, **including the player who
     caused it**. The client side `RenderGlobal.playSound` is empty, so the client never predicts these.
   - `NetHandlerPlayClient.handleSoundEffect` → `WorldClient.playSound` → a fixed `PositionedSoundRecord`.

So a sound the server sends about **you** starts where the server last saw you, one network round trip plus up to
two ticks late, and then stays there. Flying at 22 blocks/s (sprint creative flight; jetpacks and GraviSuite are
faster) with 150 ms ping, that is about 5 blocks behind at the start, and the 16-block linear range is crossed in well
under a second. A sound about another entity starts where the server had that entity (ahead of what the client
draws, which interpolates 3 ticks) and also stays there.

## What current Minecraft does (STATIC, 26.3)

- **Same core model.** The listener is set every frame from the camera (`Minecraft` → `SoundManager.updateSource`);
  tickable sounds are moved once per tick (`SoundEngine.tickInGameSound`); everything else is a fixed point.
  Attenuation is the same 1 − d/16 line, now computed by OpenAL (`Channel.linearAttenuation`: per-source
  `AL_LINEAR_DISTANCE`, rolloff 1, reference 0, max = attenuation distance). No Doppler.
- **`Entity.playSound` is still a point**: `level().playSound(null, getX(), getY(), getZ(), ...)`. Mob steps, hurt
  and ambient sounds do not follow their mob. Mojang closed "origin points of entity sounds don't move with the
  entity" as **Won't Fix** ([MC-259198](https://mojira.dev/MC-259198), 1.19.3; older duplicates such as
  [MC-105422](https://mojira.dev/MC-105422)).
- **Entity-bound sounds exist but are rare.** `EntityBoundSoundInstance` (a tickable sound whose position is copied
  from the entity every tick, stopped when the entity is removed), sent by the server as `ClientboundSoundEntityPacket`
  (since 1.14). In 26.3 about 30 classes use it: firework rocket, trident, goat horn, allay, phantom, breeze, frog,
  goat, sniffer, snow golem, mooshroom, sheep, ...
- **More of the player's own sounds are predicted.** Since 1.9 `Level.playSound(except, ...)` skips `except` on the
  server and plays only for `except` on the client. Eating/drinking (`Consumable` → `Player.playSound`), buckets and
  flint and steel use it. Throws and shots still come from the server for everyone, the shooter included (snowball,
  egg, ender pearl, experience bottle, eye of ender, fishing rod, bow, crossbow: `playSound(null, ...)`).
- **Continuous movement sounds are tickable**: elytra wind (`ElytraOnPlayerSoundInstance`), minecart, riding, bees,
  guardian beam, underwater ambience, sniffer.
- The 1.14 rewrite (19w12a) replaced paulscode ("reduced stuttering"); 1.19 added the Directional Audio (HRTF) option.

**Conclusion.** There is no new "master" sound engine to backport: current Minecraft also leaves a one-shot sound
where it started. What makes it feel different is narrower: (a) part of the player's own sounds are played by the
client at once instead of coming back late from the server, (b) a few sounds and every continuous movement sound
follow their entity, (c) the vanilla flight sound (elytra wind) follows the player. In the Forbidden pack the
sounds that matter while flying are most likely modded and server-sent, which neither version fixes.

## Existing 1.7.10 mods (published sources, see the web notes)

- Hodgepodge: replaces paulscode's OpenAL library (channels, HRTF, reverb, limiter, device handling); has a
  `dopplerFactor` setting but nothing sets velocities. No sound following. Client only.
- Sound Physics (1.7.10 ports): reverb and occlusion; does not move sources.
- Dynamic Surroundings 1.7.10: footsteps and ambience; no following found.
- Doppler mods exist for modern versions only.

## What the mixer could do

1. **Own sounds catch up (client only).** Keep the local player's positions of the last ~1 s. When an
   `S29PacketSoundEffect` lands within ~1.5 blocks of one of them while the player moves fast, play it as a tickable
   sound bound to the player (modern `EntityBoundSoundInstance`). Fixes the late, left-behind echo of your own
   actions on any server. Risk: a sound from something touching your recent path is bound to you; limits by speed,
   distance and time keep it rare.
2. **Entity sounds follow their entity (server + client).** The mixer on the server hooks `playSoundAtEntity`
   (Forge `PlaySoundAtEntityEvent` carries the entity) and sends clients that have the mixer "sound S bound to entity
   N"; they play it bound to the entity, as `ClientboundSoundEntityPacket` does today. Clients without the mixer get
   the vanilla packet. Goes beyond vanilla (Mojang keeps mob sounds as points), so it should be a config choice.
3. **Per-mod fixes**: a looping modded sound (jetpack, machine on a vehicle) that is re-sent as one-shots from the
   server can only be fixed per mod; find which mods do this in the pack first.

**Built (2026-11-03), owner's choice "the sound stays on the entity, as in recent versions":** option 2 for every
sound that belongs to an entity, plus the local player's own client sounds and per-frame placement. Option 1's
heuristic was not needed for the player's own server sounds, since the server knows they belong to the player. Option
3 remains for mods that send sounds through their own packets. Evidence:
[`../evidence/2026-11-03-entity-sounds-smoke.md`](../evidence/2026-11-03-entity-sounds-smoke.md).
