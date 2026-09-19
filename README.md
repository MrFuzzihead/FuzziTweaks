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

| Option                     | Default | What it does                                                                                                                                                                                                              |
|----------------------------|---------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `EnableLookHelperMathFix`  | `true`  | Replaces the two `Math.atan2` calls every living entity makes every server tick with a lookup table (`FastTrig`). Visually identical, cheaper with lots of entities.                                                      |
| `RemoveLookAtPlayerGoal`   | `false` | Removes the `EntityAIWatchClosest` goal (mobs turning their head towards the closest player) from every mob, modded subclasses such as `EntityAIWatchClosest2` included. Visual only, but it also disables head tracking. |
| `RemoveLookIdleGoal`       | `false` | Removes the `EntityAILookIdle` goal (mobs looking at random nearby spots) from every mob. Visual only, but it also disables idle head movement.                                                                           |
| `EnableMeleeAttackRateFix` | `true`  | Fixes the vanilla 1.7.10 melee bug where in-range mobs swing and attack **every tick** instead of once per second.                                                                                                        |
| `MeleeAttackCooldownTicks` | `20`    | Cooldown between melee attacks in ticks (20 = 1 second, the 1.8+ vanilla value).                                                                                                                                          |

**Balance note:** the melee fix is on by default and matches Minecraft 1.8 and later, so hostile mobs
deal roughly half the melee damage they do in unpatched 1.7.10 (the bugged behavior lets them hit about
twice as often as intended). Set `EnableMeleeAttackRateFix=false` to keep the old, buggy rate.

### Issues

Please open issues [here](https://github.com/MrFuzzihead/FuzziTweaks/issues).
