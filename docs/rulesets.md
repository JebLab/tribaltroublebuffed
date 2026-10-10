# Rulesets

A *ruleset* is the set of gameplay numbers a game is played with. Three ship with the game, chosen from the **Ruleset** pulldown in the Single-player skirmish menu or with one click from the built-in presets in its **Mode & presets** tab:

| Ruleset | File | What it is |
|---|---|---|
| **Classic (2004)** | [`classic.json`](../tt/src/main/resources/rulesets/classic.json) | The numbers of the 2004 Oddlabs release. |
| **Resurrected** | [`resurrected.json`](../tt/src/main/resources/rulesets/resurrected.json) | Tribal Trouble: Resurrected's numbers when this fork was made (October 2026). The complete base file. |
| **Buffed** | [`buffed.json`](../tt/src/main/resources/rulesets/buffed.json) | This fork's new content on top of Resurrected's numbers, which it keeps: so far the Chicken Coop / Henhouse and the Totem / Runestone (M5), the Shield and Torch gear (M6), the Trading Post / Market, Palisade and Gate (M7), the Great Tower, the Spirit Lodge / Mead Hall and its Champion (M8), and the Drum / Horn and Net / Snare gear (M9). |

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

**Features** (`features`): `ships`, `enormous_islands`, `archipelago` (true/false), `max_players` (6–12), `adjustable_limits` (false = the 2004 limits). `TerrainMenu.enforceRulesetFeatures()` applies them. `chicken_coop` and `totem` say whether peons may build Buffed's buildings: `Player` sets its build permissions from them, and the peon panel shows their buttons only when they are on. `shield` and `torch` say whether the Armory makes Buffed's gear (`Player.canBuildShields` / `canBuildTorches`; the Armory's submenus show their buttons only when they are on). `market` and `palisade` say whether peons may build the Market, and Palisade segments and Gates (both behind the one flag). `great_tower` and `lodge` say whether peons may build the Great Tower and the Lodge; the Lodge's flag also lets it train the Champion. `drum` and `net` say whether the Armory makes the Drum / Horn and the Net (`Player.canBuildDrums` / `canBuildNets`; the Net's flag also shows the Lay Snare button). `fauna` says whether wild animals are placed when the world is made (`Fauna.populate`; nothing else checks it, since a world without animals has none to act). `new_spells` says whether chieftains have the third slot ([design/spells.md](design/spells.md)): `Player` enables spells 2 and 3 from it (as a campaign island enables the 2004 ones), and the chieftain's panel shows their buttons only when it is on.

Every number is per race (`natives`, `vikings`) except the spells, which belong to one race each.

**Units** (`peon`, `rock_warrior`, `iron_warrior`, `chicken_warrior`, `shield_warrior`, `torch_warrior`, `drum_warrior`, `net_warrior`, `champion`, `chieftain`). The shield and torch warriors ([design/gear.md](design/gear.md)), the drum and net warriors ([design/drum-and-net.md](design/drum-and-net.md)) and the Champion ([design/lodge-and-champion.md](design/lodge-and-champion.md)) are Buffed's; like its buildings, their numbers are also in `resurrected.json`, where the `features` keep them out. They fight hand to hand, so their `hit_chance` is that of a blow; the drum warrior never attacks (its `hit_chance` is 0 and unused):

| Key | Meaning |
|---|---|
| `hit_points` | 1 for everything but the chieftain: any hit kills. |
| `speed` | meters per second. |
| `defense_chance` | chance that a hit aimed at the unit misses (the dodge in the plan). Ignored while stunned. |
| `hit_chance` | base chance the unit's own attack hits. The roll is `(hit_chance + terrain bonus + campaign difficulty bonus + totem bonus) × (1 − target's defense_chance)`, tripled for a unit in a tower or on a ship. The totem bonus is Buffed's and 0 elsewhere. |

**Buildings** (`quarters`, `armory`, `tower`, `ship`): `hit_points`. Peons build every building with wood, 5 hit points per log, so its wood cost is its hit points over 5.

**Buffed's buildings.** A building that Classic and Resurrected do not have still has its numbers in `resurrected.json`, because the base must be complete, and its `features` flag is false there; `buffed.json` turns the flag on and repeats the numbers, so Buffed's balance reads in one file.

| Building | Key | Meaning |
|---|---|---|
| Chicken Coop / Henhouse (`chicken_coop`) | `hit_points` | 100: 20 logs. |
| | `stock` | chickens peons must bring to the finished coop before it breeds (2). Peons sent to it catch them themselves. |
| | `spawn_seconds` | a new chicken every so many seconds (90), counted while fewer than `max_chickens` of its own roam. |
| | `max_chickens` | its chickens alive at a time (6). They roam near it, anyone may catch them, and they do not count towards the island's three wild flocks. |
| Totem / Runestone (`totem`) | `hit_points` | 30. |
| | `rock` | rocks that finish it (1). Each rock is the last 5 hit points, so the recipe is 5 logs and then a rock, which peons fetch themselves. |
| | `hit_bonus` | added to the hit chance of a unit within `radius` of a finished totem of its own team (0.05), inside the hit chance, so a tower triples it too. It counts for thrown weapons and blows, not for spells. |
| | `radius` | meters (10). |
| | `max_stacking` | at most this many totems add up (2). |
| Trading Post / Market (`market`, [design/market.md](design/market.md)) | `hit_points` | 150: 30 logs. |
| | `give`, `get` | a trade takes `give` of one resource (3) from the owner's nearest Armory and puts back `get` of another (1). The player picks the two among wood, rock, iron and chickens. |
| | `seconds` | man-seconds a trade takes (20): the peons inside share the work, as in the Armory. |
| Palisade (`palisade`, [design/palisade.md](design/palisade.md)) | `hit_points` | 40: 8 logs. One grid cell. |
| | `max_segments` | Palisade segments and Gates a player may have at a time (100); they do not count against the building limit. |
| Gate (`gate`) | `hit_points` | 120: 24 logs (every building is built at 5 hit points a log). One grid cell; lets its owner's team through. |
| Great Tower (`great_tower`, [design/great-tower.md](design/great-tower.md)) | `hit_points` | 300. |
| | `rock` | rocks that finish it (10). Every load, log or rock, is 5 hit points, so the recipe is 50 logs and then 10 rocks (R-35). |
| | `throwers` | warriors with thrown weapons it holds at once (3), each with the Tower's +8 m and triple hit chance. |
| Spirit Lodge / Mead Hall (`lodge`, [design/lodge-and-champion.md](design/lodge-and-champion.md)) | `hit_points` | 200. |
| | `iron` | iron that finishes it (5): 35 logs and then 5 iron. |
| | `shelter` | units of its owner it holds (30): anyone but the chieftain, out of the world while inside. |
| | `spell_radius`, `spell_charge_factor` | a chieftain of its team within this many meters (30) charges its spells this many times faster (2: half the time). Several Lodges do not add up. |
| | `champion_seconds` | seconds a Champion trains (30), while a peon is inside to become it. |
| | `max_champions` | a player's Champions alive, sheltered or in training (5). |

**The torch** (`torch`, Buffed's): `building_damage`, what its blow takes from any building, always (4; 6 until M9, D-09); `fire_seconds` (15) and `fire_damage` (2 per second): the fire it lights, which takes its `fire_seconds × fire_damage` hit points one at a time, is started again by another blow, and goes out when a peon repairs the building. The gear's recipes (Shield: 2 wood + 1 rock in 40 man-seconds; Torch: 2 wood + 1 rock + 1 iron in 80) and deploy times (1 s, 1.5 s) are constants in `LandBuilding`, as the 2004 weapons' are; the Champion's (2 wood + 1 iron + 1 chicken, taken from the nearest Armory) is a constant in `Lodge`.

**The drum and the net** (`drum`, `net`, Buffed's, [design/drum-and-net.md](design/drum-and-net.md)): `drum.hit_bonus` (0.1) is added inside the hit chance, like the totem's, and `drum.speed_bonus` (0.15) to the walking speed, of every unit of the drummer's team within `drum.radius` meters (12), the drummer included; drummers do not add up. `net.snares` (3) is how many snares one chicken catcher keeps lying (a fourth takes up its oldest), and `net.stun_seconds` (4) how long a snare stuns the first enemy unit on it. A net catches a chicken in one stroke (a peon needs ten): a rule in `HarvestBehaviour`, not a number. Recipes (Drum: 3 wood + 1 iron; Net: 2 wood + 1 chicken; 60 man-seconds each) and deploy times (1.5 s, 1 s) are constants in `LandBuilding`.

**The fauna** (`fauna`, Buffed's, [design/fauna.md](design/fauna.md)): a world uses the numbers of the race whose terrain it has (natives tropical, vikings northern); both hold the same. `start_clearance` (40 m) keeps every animal away from the players' starts; `defense_chance` (0.3) is every animal's dodge (one hit point each). `crab`: `per_100m_shore` (2) and `max` (20) set how many, `speed` (3 m/s), `flee_radius` (3 m). `monkey` and `predator` (the boar or wolf) each have a `count` by island size (`small`, `medium`, `large`, `enormous`: 3 / 5 / 8 / 12 monkeys, on tropical islands only, and 2 / 3 / 5 / 8 boars or wolves), a running `speed` (6 m/s), a `sight` (4 m: a carrier walking by; 6 m: a lone peon), a `leash` (12 m: the chase ends that far from home) and `rest_seconds` after a theft or a strike (30, 20). `predator.company_radius` (8 m): a peon with another unit of its team that close is not alone; `predator.hit_chance` (0.5): its blow, before the peon's dodge. Where they live, how far apart, and their roaming are constants in `Fauna`, `Crab` and `Predator`.

**Spells** (by their in-game names):

| Spell | Race | Keys |
|---|---|---|
| `stinking_stew` | Natives | `radius` (m), `hit_chance`, `interval` (s between damage ticks), `seconds` (lifetime), `damage` per tick |
| `crackling_cloud` | Natives | `seconds` (lifetime), `seconds_per_hit`, `speed` (m/s), `hit_chance`, `damage` per strike |
| `terrifying_toot` | Vikings | `radius` (m), `stun_seconds_closest`, `stun_seconds_farthest` |
| `ravaging_roar` | Vikings | `radius` (m), `hit_chance_closest`, `hit_chance_farthest`, `damage_closest`, `damage_farthest`, `seconds` (time for the ring to reach the radius). Hit chance and damage fall off along a curve, so `hit_chance_closest` can exceed 1. |
| `third_slot_seconds` | both | seconds the third slot (Buffed's new spells) takes to charge (100); the 2004 spells' 40 s and 70 s stay constants in `Unit`. Any cast empties every charge. |
| `jolly_jungle` | Natives (Buffed) | `radius` (m), `seconds`: enemy units on the ground within the radius cannot walk for that long, but still fight. |
| `poultry_panic` | Natives (Buffed) | `radius` (m), `stun_seconds`, `chickens`: enemy units on the ground within the radius are stunned; that many chickens are left that anyone may catch. |
| `hammer_of_thor` | Vikings (Buffed) | `range` (m to the target's edge), `damage`: one bolt at a chosen enemy, with no roll. |
| `fjord_fog` | Vikings (Buffed) | `radius` (m), `seconds`, `hit_penalty`: enemies attacking from inside the mist lose that much hit chance (inside the hit chance, like the Totem's bonus); fogs do not add up. |

**Not in the data files (yet).** Numbers tied to the models stay in `RacesResources`: footprints (also used by the island generator), selection shapes, attachment offsets, and the animation timings that decide when a throw or spell is released. The throw range (6 m) stays a constant because the tower and ship target scans are sized from it.

## Adding a number

1. Add the component to the record in `RulesetStats` and the key to `resurrected.json` (the base must stay complete).
2. Read it where the game used the literal: from the stats passed to `RacesResources`, or from `world.getRuleset().getStats()` in simulation code.
3. If Classic or Buffed differ, add the override to their file, and extend `RulesetTest`.
