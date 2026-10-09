# Rulesets

A *ruleset* is the set of gameplay numbers a game is played with. Three ship with the game, chosen from the **Ruleset** pulldown in the Single-player skirmish menu or with one click from the built-in presets in its **Mode & presets** tab:

| Ruleset | File | What it is |
|---|---|---|
| **Classic (2004)** | [`classic.json`](../tt/src/main/resources/rulesets/classic.json) | The numbers of the 2004 Oddlabs release. |
| **Resurrected** | [`resurrected.json`](../tt/src/main/resources/rulesets/resurrected.json) | Tribal Trouble: Resurrected's numbers when this fork was made (October 2026). The complete base file. |
| **Buffed** | [`buffed.json`](../tt/src/main/resources/rulesets/buffed.json) | This fork's new content. Identical to Resurrected until the first new buildings land (M5). |

The skirmish menu preselects Buffed. Everything else plays under **Resurrected**: the campaign and tutorials (their islands were tuned for those numbers) and multiplayer (every client and the inherited servers expect them, so the pulldown is hidden there). Presets saved before rulesets existed load as Resurrected.

A ruleset also decides which of Resurrected's world options the menu offers (the `features` block). **Classic is the 2004 game only**: no boats, no Enormous or Archipelago islands, six players, and the fixed limits of 20 starting units, 250 units and 20 buildings (the *Advanced...* button is greyed out). A choice Classic does not offer snaps back to the nearest one it does: Large island, six players. Resurrected and Buffed offer everything. Interface improvements (rebindable keys, accessibility options, control groups) are not rule changes and stay available under every ruleset. The built-in presets pick a medium island, the default limits, no boats, and you against one Easy AI.

## Classic and 2004

Every unit, building, weapon and spell number in the 2004 source (`oddlabs/master`) was compared with this fork's base on 9 October 2026, along with every other gameplay constant in `model/`, `model/behaviour/`, `model/weapon/`, `player/` and `landscape/`: Armory recipes, build and deploy times, hit points per log, Quarters reproduction, spell charge times (40 s / 70 s), the tower's +8 range and triple hit, chicken bounce, harvest rates, resource node sizes and chicken flocks. **Resurrected changed none of them**, and its new limits default to the 2004 values (20 starting units, 250 units, 20 buildings). So `classic.json` overrides no number, only the `features` (above); `RulesetTest` pins it to the 2004 literals so it stays correct if the base file moves.

What Classic does *not* restore is engine behaviour changed since 2004. These are code changes, not numbers, and most are fixes:

| Change since 2004 | Effect on play |
|---|---|
| The terrain hit bonus reads heights by bilinear interpolation instead of the mesh triangles (`HeightMap`) | the ±0.25 height bonus can differ slightly on uneven ground |
| Each thrown weapon draws two extra random numbers (`ThrowingWeapon`) | same odds, different dice: a 2004 replay would not replay |
| Pathfinder cost buckets 1024 → 2048 (`RegionBuilder`, `PocketList`) | long paths can differ |
| Standing units no longer block building placement and are moved out from under it (`LandBuilding`) | quality of life |
| Leaving a tower needs a free exit cell (`LandBuilding`, `Building`) | fix |
| A gathering peon keeps the drop-off building chosen at order time (`GatherController`) | differs only with several Armories |
| The AI's unit groups come out of a hash map (`Player.classifyUnits`) | AI choices can differ (and vary between runs; tracked separately) |
| Quarters reproduction uses `Math.pow` instead of `StrictMath.pow` (`ReproduceUnitContainer`) | may differ in the last bit |

Reverting any of these behind Classic is possible later, per item, if it turns out to matter.

## How it works

- `tt/src/main/resources/rulesets/<id>.json` holds the numbers. A file may say `"extends": "<other id>"`; its objects are then merged key by key over the parent's, so it lists only what it changes. `resurrected.json` is complete; `classic.json` and `buffed.json` extend it.
- `RulesetLoader` reads the file into the `RulesetStats` records (component names are the JSON keys) and rejects a file that misses a field or has one it does not know, so a typo fails at load instead of playing as zero.
- The chosen `Ruleset` travels in `WorldParameters` to `World.getRuleset()`, and `WorldViewer` hands its stats to `RacesResources`, which builds the unit and building templates, weapon factories and spell factories from them for that game.
- Determinism: every client of a game must use the same ruleset. Single player has one client; multiplayer is fixed to Resurrected. Event-log replays record the menu clicks, so they replay the ruleset choice too.
- `RulesetTest` pins Resurrected to the literals `RacesResources` had at the fork point and Classic to the 2004 source, bit for bit.

## Fields

**Features** (`features`): `ships`, `enormous_islands`, `archipelago` (true/false), `max_players` (6–12), `adjustable_limits` (false = the 2004 limits). `TerrainMenu.enforceRulesetFeatures()` applies them.

Every number is per race (`natives`, `vikings`) except the spells, which belong to one race each.

**Units** (`peon`, `rock_warrior`, `iron_warrior`, `chicken_warrior`, `chieftain`):

| Key | Meaning |
|---|---|
| `hit_points` | 1 for everything but the chieftain: any hit kills. |
| `speed` | meters per second. |
| `defense_chance` | chance that a hit aimed at the unit misses (the dodge in the plan). Ignored while stunned. |
| `hit_chance` | base chance the unit's own attack hits. The roll is `(hit_chance + terrain bonus + campaign difficulty bonus) × (1 − target's defense_chance)`, tripled for a unit in a tower or on a ship. |

**Buildings** (`quarters`, `armory`, `tower`, `ship`): `hit_points`.

**Spells** (by their in-game names):

| Spell | Race | Keys |
|---|---|---|
| `stinking_stew` | Natives | `radius` (m), `hit_chance`, `interval` (s between damage ticks), `seconds` (lifetime), `damage` per tick |
| `crackling_cloud` | Natives | `seconds` (lifetime), `seconds_per_hit`, `speed` (m/s), `hit_chance`, `damage` per strike |
| `terrifying_toot` | Vikings | `radius` (m), `stun_seconds_closest`, `stun_seconds_farthest` |
| `ravaging_roar` | Vikings | `radius` (m), `hit_chance_closest`, `hit_chance_farthest`, `damage_closest`, `damage_farthest`, `seconds` (time for the ring to reach the radius). Hit chance and damage fall off along a curve, so `hit_chance_closest` can exceed 1. |

**Not in the data files (yet).** Numbers tied to the models stay in `RacesResources`: footprints (also used by the island generator), selection shapes, attachment offsets, and the animation timings that decide when a throw or spell is released. The throw range (6 m) stays a constant because the tower and ship target scans are sized from it.

## Adding a number

1. Add the component to the record in `RulesetStats` and the key to `resurrected.json` (the base must stay complete).
2. Read it where the game used the literal: from the stats passed to `RacesResources`, or from `world.getRuleset().getStats()` in simulation code.
3. If Classic or Buffed differ, add the override to their file, and extend `RulesetTest`.
