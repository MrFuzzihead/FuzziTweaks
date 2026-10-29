# FuzziTweaks

[![Build status](https://github.com/MrFuzzihead/FuzziTweaks/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/MrFuzzihead/FuzziTweaks/actions/workflows/build-and-test.yml)
[![Latest release](https://img.shields.io/github/v/release/MrFuzzihead/FuzziTweaks?include_prereleases&sort=semver)](https://github.com/MrFuzzihead/FuzziTweaks/releases/latest)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.7.10-62a34a)](https://minecraft.wiki/w/Java_Edition_1.7.10)
[![Forge](https://img.shields.io/badge/Forge-10.13.4.1614-1e2b4f)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.7.10.html)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

Some configurable tweaks to bring modern features and QoL changes to legacy Minecraft.

### What is FuzziTweaks?

FuzziTweaks adds some configurable QoL changes from Modern Minecraft to legacy Minecraft.

### AI Improvements

The [AI Improvements](https://github.com/BuiltBrokenModding/AI-Improvements) mod (MIT, by
BuiltBrokenModding/DarkCow) is folded into FuzziTweaks - its features no longer need the separate mod.
Everything lives in the `ai` section of `config/fuzzitweaks.cfg`:

| Option                        | Default | What it does                                                                                                                                                                                                                                                                                                                                        |
|-------------------------------|---------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `EnableAIImprovementsMain`    | `true`  | Main switch for all AI Improvements tweaks, including the folded Squidless spawn and pack-size mixins. When disabled, none of the AI mixins are applied and the look-goal removal listener is not registered. Read at startup; restart after changing it.                                                                               |
| `EnableFastTrig`              | `true`  | Replaces the hot `Math.atan2` calls in mob AI (head tracking in `EntityLookHelper`, `EntityLiving.faceEntity` for legacy-AI mobs such as spiders, endermen and zombie pigmen, and squid swimming) with a lookup table (`FastTrig`). Off by less than a degree, so visually identical, and cheaper with lots of entities.                            |
| `RemoveLookAtPlayerGoal`      | `false` | Removes the `EntityAIWatchClosest` goal (mobs turning their head towards the closest player) from every mob, modded subclasses such as `EntityAIWatchClosest2` included. Visual only, but it also disables head tracking.                                                                                                                           |
| `RemoveLookIdleGoal`          | `false` | Removes the `EntityAILookIdle` goal (mobs looking at random nearby spots) from every mob. Visual only, but it also disables idle head movement.                                                                                                                                                                                                     |
| `OnlyRunLookGoalsNearPlayers` | `true`  | Runs the two visual look goals only while a player is within `LookGoalPlayerRange` blocks. Nothing can be observed from further away, so it is invisible by construction, and unlike the removals above it keeps head tracking fully normal for every mob a player can actually see. Aimed at chunk loaded areas, spawn chunks and mob-heavy bases. |
| `LookGoalPlayerRange`         | `128`   | Distance in blocks for `OnlyRunLookGoalsNearPlayers`. The default is well past the range at which head movement is noticeable; lower it to save more CPU at the cost of distant mobs ignoring players.                                                                                                                                              |
| `EnableMeleeAttackRateFix`    | `true`  | Fixes the vanilla 1.7.10 melee bug where in-range mobs swing and attack **every tick** instead of once per second.                                                                                                                                                                                                                                  |
| `MeleeAttackCooldownTicks`    | `20`    | Cooldown between melee attacks in ticks (20 = 1 second, the 1.8+ vanilla value).                                                                                                                                                                                                                                                                    |

**Balance note:** the melee fix is on by default and matches Minecraft 1.8 and later, so hostile mobs
deal roughly half the melee damage they do in unpatched 1.7.10 (the bugged behavior lets them hit about
twice as often as intended). Set `EnableMeleeAttackRateFix=false` to keep the old, buggy rate.

### Mob spawn light levels

Hostile mobs use modern Java Edition's light rules instead of 1.7.10's block-light-7 check. The raw
stored sky-light gate and the thunder-time brightness adjustment are preserved from modern Minecraft.
Everything lives in the `minecraft` section of `config/fuzzitweaks.cfg`:

| Option                   | Default | What it does                                                                                                                                                                                                                   |
|--------------------------|---------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `MaxMobSpawnLightLevel`  | `0`     | Hard maximum block light level at which hostile mobs can spawn (0-15). `0` matches modern Minecraft in the Overworld.                                                                                                           |
| `MaxMobSkyLightLevel`    | `7`     | Maximum effective sky-adjusted light level accepted by the random darkness check (0-15). `7` matches modern Minecraft; lower values make spawn attempts rarer. The separate stored sky-light check remains modern's random gate. |

As in modern Minecraft, the final sky-adjusted check is probabilistic: a lower light value passes more
often than one close to the configured maximum, so `MaxMobSkyLightLevel` is a threshold, not a guarantee
that every block at or below it will spawn a mob.

### Leads

Leads tied from a horse or other mob to a fence post would regularly snap off (dropping a lead item on the
ground) as soon as the chunk holding them was loaded, because the mob happened to be ticked before the world
could hand it its anchor. Vanilla 1.7.10 has no way to tell that apart from the fence really being gone:
`EntityLiving` stashes the saved `Leash` compound in `field_110170_bx` on load, throws that stash away on the
mob's very first `updateLeashedState()`, and calls `clearLeashed(true, true)` - which drops a lead - the moment
`leashedToEntity` is null or dead. The lead knot itself is never saved to chunk NBT, so the only copy of it is
the one rebuilt from the mob, and the `EntityHanging#onUpdate` revalidation of that knot reads the block with a
plain `getBlock()`, which reports air while the fence's own chunk is not resident and kills the knot. Every
automatic break funnels through `clearLeashed`, so that is where the fix goes. Everything lives in the `leash`
section of `config/fuzzitweaks.cfg`:

| Option                  | Default | What it does                                                                                                                                                                                                                                                                                              |
|-------------------------|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `EnableLeashLoadFix`    | `true`  | Remembers a restored mob's saved fence-post anchor past the single tick vanilla gives it and holds `clearLeashed` off while the fence's chunk is not loaded. A resident, valid fence is re-attached immediately; a resident chunk whose block is no longer leashable is treated as a real removal and dropped as vanilla does. Only the `X/Y/Z` fence form is tracked: player/entity UUID anchors and malformed or missing `Leash` tags keep vanilla behaviour. Also stops a lead knot from killing itself while the chunk holding its fence post is not loaded, and writes the remembered anchor back to NBT while the decision is still pending. |
| `LeashLoadGraceTicks`   | `200`   | How long (in ticks) a restored mob may keep its lead while its fence chunk is not loaded or its knot cannot yet be attached. A mob whose fence is resident and valid is re-attached on the first tick and is unaffected; this only bounds the load-order race and drops an unresolvable lead as normal once the window closes. 0 restores vanilla's single-tick behaviour. |

Only a restored mob with a saved fence-post anchor and an unloaded/unresolved fence is ever deferred. A player
taking a lead off by hand, a player/entity UUID anchor, malformed NBT, and a fence known to have been removed
all keep vanilla behaviour, so this cannot be used to farm lead items or undo a player breaking a knot.

### Hats

Hats downloads its hat models from a hardcoded URL (`http://www.creeperrepo.net/ichun/static/hats.xml`). That
domain has expired - it now parks and answers with a JavaScript redirect page - so the manifest parse throws
inside the mod's download thread, the exception is swallowed, and the game starts with zero hats: no player
hats, no mobs wearing hats, empty selection GUI. Everything lives in the `hats` section of
`config/fuzzitweaks.cfg`:

| Option                     | Default                    | What it does                                                                                                                                                                                                                                                                                                                          |
|----------------------------|----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `EnableHatsEndpointFix`    | `true`                     | Redirects the Hats manifest and hat-file downloads to `HatDownloadBaseUrl`.                                                                                                                                                                                                                                                           |
| `HatDownloadBaseUrl`       | `http://dist.creeper.host` | `scheme://host` used instead of the defunct repository hosts (creeperrepo.net, redstone.tech, ...). Only the host is swapped and the path is kept, so the manifest and every hat file land on the same layout on the new host. Point it at your own mirror to stop depending on third parties; leave it blank to disable the rewrite. |
| `PreferLocalHatMobSupport` | `true`                     | Reads the modded-mob hat placement list from `hats/HatModMobSupport.json` when that file exists, instead of a raw GitHub URL that now returns 404. A copy ships inside the Hats jar at `assets/hats/mod/HatModMobSupport.json`.                                                                                                       |

The download walks all ~400 manifest entries one at a time, which takes several minutes on a normal
connection; files already on disk are skipped, so relaunching resumes where it stopped. A hat that fails to
transfer is now skipped with a one-line message and retried on the next launch, instead of aborting every
remaining entry in the manifest the way the unpatched mod does.

The hats themselves are third-party content served by whoever runs the mirror. A pack that has to build
offline should ship the downloaded `hats/` folder in the pack rather than rely on the download at runtime.

### Galacticraft

Galacticraft tweaks all share a late mixin gate, so they are only loaded when Galacticraft is present. Everything
lives in the `galacticraft` section of `config/fuzzitweaks.cfg`:

| Option                                        | Default | What it does                                                                                                                                                                                                                                                                                                                       |
|-----------------------------------------------|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `EnableGalacticraftMain`                      | `true`  | Master switch for all Galacticraft mixins. When disabled, none of the mixins below are applied; the individual switches still have their own settings.                                                                                                                                                                              |
| `EnableGalacticraftCoalGeneratorFuelTweak`    | `true`  | Lets the Coal Generator burn any furnace-burnable item (charcoal, wood, mod fuels, ...) instead of only coal and coal blocks, scaled to each item's vanilla furnace burn time at Galacticraft's 1/5 rate. The GUI slot, hoppers/pipes and the tile's `isItemValidForSlot` all accept the wider fuel set.                        |
| `EnableGalacticraftCompressorNEIFix`          | `true`  | Re-registers Galacticraft's Ingot/Electric-Ingot Compressor NEI handlers and the NASA Workbench rocket displays (Tier 1-3 and cargo variants) that the GTNH fork leaves to GregTech. Disabling it removes all of those recipes from NEI, not just the compressor handlers.                                                           |

### Squid spawns

The [Squidless](https://github.com/hilburn/Squidless) mod is folded into FuzziTweaks. It stops squids from
spawning in the band around a player that vanilla still allows (anything past 64 blocks), which is where most
squids used to appear, and can cap how many squids spawn together. Everything lives in the `squid` section of
`config/fuzzitweaks.cfg`:

| Option                    | Default | What it does                                                                                                                                                                                                                                                                                                                                                     |
|---------------------------|---------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `EnableSquidSpawnControl` | `true`  | Denies a squid spawn when no player is within `SquidSpawnPlayerRange` blocks. Only the exact `EntitySquid` class is affected, so modded water mobs and squid subclasses are untouched.                                                                                                                                                                              |
| `SquidSpawnPlayerRange`   | `64`    | Distance in blocks a player must be within for squids to spawn (Squidless used 64).                                                                                                                                                                                                                                                                               |
| `SquidMaxPackSize`        | `2`     | Maximum squid pack size. `0` keeps vanilla (4). Squidless set `LivingPackSizeEvent.maxPackSize = 2` but never called `setResult(ALLOW)`, so Forge discarded the value and vanilla's 4 was always used - this port returns the value directly, so 2 now takes effect for the first time. Set it to `0` if you prefer vanilla pack sizes.                       |

The squid mixins are loaded under the shared `EnableAIImprovementsMain` gate (see AI Improvements); the options
above still control their individual behaviour.

Squidless shipped no license file (its upstream repository has none either), so this is a clean-room
reimplementation from its observed/decompiled behaviour rather than a copy of its code.

### Dormant chunk cache

The [TileEntity Reset Fix](https://github.com/MysteriumTeam/TileEntityResetFix) mod is folded into FuzziTweaks.
When Forge's dormant chunk cache is enabled, `ChunkProviderServer` parks an unloaded chunk in it and may serve
that same chunk object again later. By then the object has already been through `Chunk.onChunkUnload`, which
detaches its tile entities, so reusing it can resurrect stale tile entity data instead of the fresh copy saved
to disk. The fix invalidates each entry as soon as it has been handed back out. It lives in the
`tileentityreset` section of `config/fuzzitweaks.cfg`:

| Option                     | Default | What it does                                                                                                                                                                              |
|----------------------------|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `EnableTileEntityResetFix` | `true`  | Drops a dormant chunk from Forge's cache immediately after it is served. Only has an effect when `config/forgeChunkLoading.cfg` sets `dormantChunkCacheSize` above `0`; with the default `0` the cache evicts instantly and this is a no-op. |

TileEntityResetFix ships no license file, so this is likewise a clean-room reimplementation of its observed
behaviour.

### Careful casts

The [Careful Cast Corrector](https://github.com/Chocohead/Careful-Cast-Corrector) mod is folded into FuzziTweaks.
Vanilla casts the result of `World.getTileEntity` straight to `TileEntityChest` in
`BlockChest.onNeighborBlockChange` and to `TileEntityChest` / `TileEntityMobSpawner` in
`WorldGenDungeons.generate`. A mod that puts its own tile entity at one of those positions makes the cast throw
`ClassCastException`. Instead of rewriting the casts, the lookups are redirected to return `null` for a foreign
tile entity, so the existing null checks simply skip the affected step - the same effective behaviour as CCC
with no extension handler registered. It lives in the `carefulcast` section of `config/fuzzitweaks.cfg`:

| Option                       | Default | What it does                                                                                                                              |
|------------------------------|---------|-------------------------------------------------------------------------------------------------------------------------------------------|
| `EnableCarefulCastCorrector` | `true`  | Skips the vanilla chest/dungeon tile entity step instead of throwing when another mod has replaced the tile entity. Chests and dungeons behave normally otherwise. |

CCC's extension API (`CarefullyCastChestContentFillers` / `CarefullyCastMobSpawnerSetters`) is intentionally not
ported; nothing in the pack registers a handler. Careful Cast Corrector ships no license file, so this is a
clean-room reimplementation of its observed behaviour.

### Railcraft minecart placement

Railcraft replaces vanilla minecart items with its own cart entities from a `PlayerInteractEvent` handler and
denies the vanilla item use. Another mod can run after Railcraft and reset that result, which lets
`ItemInWorldManager` place the vanilla cart as well - one click, two carts. FuzziTweaks injects at the return of
`ForgeEventFactory.onPlayerInteract`, after every event handler has finished, and re-asserts `useItem = DENY` for
every cart item in Railcraft's `CartUtils.vanillaCartItemMap`. It lives in the `railcraft` section of
`config/fuzzitweaks.cfg`:

| Option                                | Default | What it does                                                                                                                                                                                                                     |
|---------------------------------------|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `EnableRailcraftMinecartPlacementFix` | `true`  | Re-asserts the vanilla-use deny after the whole interaction event has been posted, so a handler that runs after Railcraft cannot undo it. Only the Railcraft cart remains.                                                       |
| `LogMinecartPlacementDebug`           | `false` | Prints the item, stack size, event result and a stack trace for every minecart interaction. Two traces for one click mean another mod calls `ForgeEventFactory.onPlayerInteract` twice; a single trace with `useItemBefore=DEFAULT` means a handler reset the result (in 1.7.10, `PlayerInteractEvent.setCanceled(false)` resets `useItem`/`useBlock` from `DENY` to `DEFAULT`) or the event was canceled before Railcraft's handler ran. A synthetic event posted directly to the bus does not appear as a trace. |

The mixin is loaded when either option is enabled, so `EnableRailcraftMinecartPlacementFix=false` together with
`LogMinecartPlacementDebug=true` observes the unmodified behaviour while still collecting the event traces.

### Issues

Please open issues [here](https://github.com/MrFuzzihead/FuzziTweaks/issues).

