# 2026-11-03: entity sounds, second pass (`/playsound`, the other sound paths, Crucible, plugins)

Owner request: "`/playsound` must stay on the entity too; find the other places that use the old method and replace
them with ours; fix the buggy `/playsound`; make sure it works on Crucible and for plugins."

## 1. Every way a sound reaches a 1.7.10 client (STATIC, MCP sources of Forge 10.13.4.1614)

| Path | Before | Now |
|---|---|---|
| `World.playSoundAtEntity` (mobs, items, `Entity.playSound`) → `WorldManager.playSound` → `S29PacketSoundEffect` to players in range | fixed point | bound to the entity (mixer clients) |
| `World.playSoundToNearExcept` (`EntityPlayer.playSound`, the player excluded) | fixed point | bound to the player for the others |
| `World.playSoundEffect` (blocks, mods, Bukkit's `World.playSound`) | fixed point | bound when exactly at an entity's feet, position or eyes (`matchByPosition`); otherwise unchanged |
| `CommandPlaySound` (vanilla `/playsound`): `sendPacket(new S29PacketSoundEffect(...))` straight to the target | at `getPlayerCoordinates()`: `floor(x)`, `floor(y + 0.5)`, `floor(z)`, the block **corner** (bug) | at the player, bound for mixer clients |
| Client: `EntityPlayerSP.playSound` (own steps, chewing), `EntityDragon` wings, `NetHandlerPlayClient` arrow-hit (`posY + eyeHeight`), mods' client sounds at `entity.posX` → `WorldClient.playSound` | fixed point | bound to that entity |
| `playAuxSFX` level events (`S28PacketEffect`: doors, ghast warning, potion splash, block break, records) | block position | unchanged: integer block coordinates, and current Minecraft also plays them at a block position (`levelEvent`) |
| Ticking sounds (`MovingSoundMinecart`, mods' `MovingSound`s) | follow already | unchanged |

`new S29PacketSoundEffect` appears in exactly four places in vanilla: two in `WorldManager`, two in
`CommandPlaySound`. Current Minecraft (26.3, STATIC, see the research note) still sends `/playsound` as a fixed point;
binding it is beyond vanilla, as the owner asked.

## 2. Design change: act on the final per-player send

The first pass replaced `WorldManager`'s send with its own loop. On Bukkit servers that loop would have skipped
Bukkit's own rules (`ServerConfigurationManager` checks `canSee` for vanished players) and missed every sound sent
straight to a player. Now the server only records whose sound a world is sending (`MixinWorld`) and decides at
`NetHandlerPlayServer.sendPacket` (`MixinNetHandlerPlayServer`), the last point a packet passes for one player, after
every filter: a mixer client gets the bound sound, any other client the vanilla packet. A sound sent straight to one
player at that player (`SoundAnchor.meantForPlayer`: exact position or eyes, vanilla's block corner, Bukkit's block
centre) is bound for a mixer client and moved to the player for a vanilla client. The replacement is a plain packet
sent through the same `sendPacket`, built without `SimpleNetworkWrapper`'s shared encoder, so a plugin's thread is
safe.

## 3. Crucible (STATIC, source `CrucibleMC/Crucible` at `cd1de3d`, 2026-11-01)

- `/playsound` on Crucible is **Bukkit's**: `SimpleCommandMap.setFallbackCommands()` registers
  `org.bukkit.command.defaults.PlaySoundCommand` before `CraftServer.setVanillaCommands()` registers the vanilla
  command wrapper, and a second `VanillaCommand` with a taken name only gets `minecraft:playsound`. Bukkit's command
  has the same corner bug (`Math.floor(location.getX())`, `Math.floor(location.getY() + 0.5D)`) and sends through
  `Player.playSound`.
- `CraftPlayer.playSound(Location, String, ...)` sends `new S29PacketSoundEffect(sound, loc.getBlockX() + 0.5, ...)`
  through `playerNetServerHandler.sendPacket`: the block **centre**. Both are matched by `meantForPlayer` (unit tests
  cover the corner, the centre and Bukkit's corner-then-centre).
- `CraftWorld.playSound(Location, Sound, ...)` calls `World.playSoundEffect` (covered by `MixinWorld`).
- `NetHandlerPlayServer.sendPacket(Packet)` keeps its signature; Crucible adds a null check and the compass target
  after the start, so an `@Inject` at HEAD applies.
- `World.playSoundAtEntity`, `playSoundToNearExcept`, `playSoundEffect` are not patched.
- Crucible makes `WorldManager.theWorldServer` public: the first pass's `@Shadow private` on it is gone with
  `MixinWorldManager`; no `@Shadow` remains on a class Bukkit patches.
- Mixins: CrucibleMC recommends UniMixins for 1.7.10 mixins (its own Grimoire-Mixins repository is archived in
  favour of the GTNH stack): <https://github.com/CrucibleMC/Grimoire-Mixins-1.7.10>.

## 4. Dev client check (AUTOMATED, Linux dev client, integrated server, Java 8, OpenAL null backend)

Setup as in [`2026-11-03-entity-sounds-smoke.md`](2026-11-03-entity-sounds-smoke.md), final code of this commit plus
a temporary harness (deleted, never committed). Player flying 22 blocks/s, pig moving 10 blocks/s; positions read from
paulscode. Runs 8 and 9 (between them only the harness changed: `/playsound` is run from the server console, since
the test world has cheats off).

| Scenario | Observed | Result |
|---|---|---|
| A. `playSoundAtEntity(pig)` | bound, followed 20.5 blocks at 0.00 | pass |
| B. `playSoundEffect` at the pig's coordinates | bound, followed 20 blocks at 0.00 | pass |
| C. `playSoundEffect` next to the pig | vanilla point, the pig walked 19.3 blocks away | pass |
| D. Server `playSoundAtEntity(player)` | bound to the player over 66 blocks; per frame mean 0.000 | pass |
| F. Client `thePlayer.playSound` (now through `MixinWorldClient`) | bound; per frame mean 0.000 | pass |
| L. Client sound at the player's eyes (`posY + getEyeHeight()`, as the arrow-hit sound) | bound; per frame mean 0.000 | pass |
| I. `/playsound records.ward Developer` (vanilla command), mixer client | `Played sound 'records.ward' to Developer`; bound to the player over 65.7 blocks; per frame mean 0.000 | pass |
| K. Direct `S29PacketSoundEffect` at the player's block centre (what `CraftPlayer.playSound` sends on Crucible) | bound to the player over 66 blocks | pass |
| G. Pig dies while its sound plays | followed it, then finished where it died | pass |
| H. Client without hello: `playSoundAtEntity(pig)` | vanilla point | pass |
| J. `/playsound records.13 Developer`, client without the mixer | played at (685.25, 11.88, 14.50), the player's feet at (685.30, 11.92, 14.50); vanilla would have used the corner (685, 12, 14) | pass (`/playsound` fix) |

```
I /playsound records.ward Developer (mixer client) t=12 sound=EntityBoundSound source=(534.60, 11.92, 14.50) player=(534.60, 11.92, 14.50) sourceToPlayer=0.00 sourceFromStart=0.00
I /playsound records.ward Developer (mixer client) t=52 sound=EntityBoundSound source=(578.60, 11.92, 14.50) player=(578.60, 11.92, 14.50) sourceToPlayer=0.00 sourceFromStart=44.00
I /playsound records.ward Developer (mixer client): per-frame distance to the player's feet over 120 frames: mean 0.000 max 0.000
K direct S29 at block centre (CraftPlayer.playSound) t=72 sound=EntityBoundSound source=(682.00, 11.92, 14.50) player=(682.00, 11.92, 14.50) sourceToPlayer=0.00 sourceFromStart=66.00
L client world sound at player eyes: per-frame distance to the player's feet over 109 frames: mean 0.000 max 0.000
J server player at (685.3000000000034, 11.921599998474122, 14.5); vanilla corner would be (685.0, 12.0, 14.0)
J /playsound, client without the mixer: sound=PositionedSoundRecord source=(685.25, 11.88, 14.50) player feet=(685.30, 11.92, 14.50)
```

## Not run

- **A Crucible server.** Building or running Crucible (third-party code) was not permitted in this session, and its
  GitHub releases were not reachable; JitPack only has its API jar. Crucible is checked against its source only
  (section 3). To check on the real server: Crucible + UniMixins + this jar in `mods`, a client with the jar, then
  `/playsound <sound> <you>` while flying, a plugin's `player.playSound(player.getLocation(), ...)` and
  `world.playSound(...)`, and a vanished player's sounds (must stay hidden).
- Real Bukkit plugins, ProtocolLib (a packet it writes straight to the channel, without `sendPacket`, stays vanilla),
  a plugin sending from its own thread (the code path is thread safe by design, not observed), a dedicated server with
  real ping, Windows, the Forbidden pack.
- `/playsound` with relative offsets (`~ ~1 ~`) keeps vanilla's meaning (relative to the block corner), and a fixed
  point; only the no-offset form is "at the player".
