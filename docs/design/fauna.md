# Neutral fauna

Buffed's wild animals (M9, PLAN.md §4.4 "Neutral fauna"): ambient, cheap and on-brand, with two of them a small hazard of the map. Written in M9's first session as the design for its second (`docs/prompts/m09-part2-fauna.md`), which built them; "As built" at the end lists what building them changed. Classic and Resurrected do not have them (a `features.fauna` flag, false there).

| Animal | Where | What it does | Counter |
|---|---|---|---|
| **Beach crab** | beaches (the cells next to the sea, both terrains) | scuttles along the shore, away from any unit within 3 m; harmless | none needed: ambience |
| **Monkey** | tropical (Native terrain) forest edges | steals the load of a passing carrier peon and runs back into the trees with it | warriors kill it; escort carriers or gather elsewhere |
| **Boar** (tropical) / **Wolf** (northern) | deep forest: cells with trees on most neighbours | attacks a lone peon that comes near; leaves groups and warriors alone | warriors kill it; gather in twos, keep warriors by the woodcutters |

## Rules

- **Neutral.** No player owns them: they are not units of any player, so they never count towards a player's units or keep a player alive, never block victory, and no AI commands them. They are their own world objects, like the chickens (`RubberSupply` is a moving `SupplyModel` that is `Animated` and `Movable`): an animal class with a `PathTracker`, its own animations and its own decisions once a tick, drawn by a visitor of its own.
- **One hit point, like everyone.** Warriors and towers may attack monkeys, boars and wolves (crabs are not targets). An idle or walking warrior picks one as a target only when nothing of a player is in range: a new lowest priority, appended to `AttackScanFilter.Priority` with a value below every other (only the order matters, R-44). Their dodge is 0.3.
- **Monkeys.** A monkey waits at the forest edge. When a peon of any player carrying a resource (wood, rock, iron or a chicken) passes within 4 m, it runs to it (6 m/s, faster than a loaded peon) and, on reaching it, takes the load (the peon's supply container empties; the peon goes back to work) and runs back into the trees, where the load is gone. Then it rests for 30 s. It never attacks.
- **Boars and wolves.** One lives in each patch of deep forest, roaming a few cells. When a peon comes within 6 m and no other unit of that peon's team is within 8 m of it, the animal charges (6 m/s) and strikes like a peon (hit chance 0.5, the peon's dodge 0), then goes back to its patch and rests for 20 s whatever the outcome. It never attacks warriors, chieftains, buildings or groups, and gives up the chase when the peon reaches company or moves 12 m from the patch. A kill counts for nobody (no player's kill statistics).
- **How many.** Per island, scaled with its size (Small 256 m, Medium, Large, Enormous): crabs about 2 per 100 m of shore up to 20; monkeys 3 / 5 / 8 / 12 (Native terrain only); boars or wolves 2 / 3 / 5 / 8. They do not come back once killed, so a player who clears the woods is rid of them for the game. Ruleset fields under a new `fauna` object, per race file layout as the rest (both races the same, since they belong to the terrain, not a race).
- **Placed when the world is made, deterministically.** The island generator runs for every ruleset, so the animals are placed after it, from the world's random numbers, only when `features.fauna` is on: Classic and Resurrected draw no extra numbers and their golden traces must not move. Spawn cells avoid every player's start area (no animal within 40 m of a start), so nobody begins next to a wolf.
- **Determinism.** Everything an animal does is decided in the simulation tick from the world's random numbers, as the chickens' wandering is; their sounds and effects are drawn only from the world's random numbers too.

## The AI

The AI's warriors attack animals in reach like any target (the lowest priority), and its gatherers keep working. Escorting woodcutters was the first idea; as built, Normal and Hard hunt down a boar or wolf that has killed one of their peons (see "As built").

## Art (placeholders)

Four new skeletons with three animations each (idle, run, attack; a hit animal plays no death animation: it falls over and fades like a killed chicken), about 300 triangles: `misc/crab`, `natives/monkey`, `natives/boar`, `vikings/wolf`, with textures in the style of the chicken's. The animals' animation positions are their own (an animal class picks them itself, unlike `Unit.Animation`). Final art needs an artist (MILESTONES.md).

## As built (M9, part 2)

What building it settled or changed (R-51 to R-56 in [DECISIONS.md](../DECISIONS.md)):

- **Code.** `model/Animal` (grid, path tracker, one hit point, death) with `Crab`, `Monkey` and `Predator` (boar and wolf); `model/Fauna` places them; `World.getAnimals()` lists the living ones. Weapons aim at a `Hittable` (a player's `Selectable` or an `Animal`) instead of a `Selectable`; `AttackScanFilter` takes animals only when built to hunt them (warriors and tower throwers; not peons, chieftains or ships) and gives them `Priority.ANIMAL`, below everything of a player. `Unit.hitByAnimal` kills without crediting anyone; `Unit.loseLoad` empties a carrier and sends it back to work.
- **Where.** The island's "contour" is only its docking spots, so the shore is every free land cell next to water. Trees never stand side by side in this engine (a free cell has at most two trees around it), so depth is the number of trees within 4 cells (a square 18 m across): deep forest has at least 6 (fewer, down to 3, while there are under 10 such cells per boar or wolf), a forest edge is a cell next to a tree with at most 4. Northern coasts are mostly cliff: the Medium test island has 71 walkable cells by the water and gets 3 crabs, the tropical one 20.
- **How many** is per map (the island size setting; an Archipelago counts as Enormous), not per island. Animals of a kind keep apart: crabs 4 cells, monkeys 8, boars and wolves 12, any two animals 2.
- **Moving.** Crabs wander within 3 cells of home at half their 3 m/s and run from units within 3 m; boars and wolves roam within 2 cells at 40 % of their 6 m/s. An animal decides twice a second, on a cell. Its attack takes 0.6 s and lands half way, on a peon up to two cells away (a lunge).
- **Monkeys** rob a unit walking by with a load (peons and chicken catchers), and give up when it gets 12 m from their home, like boars and wolves. **The load is simply gone** (the plan says "steal"; dropping it would need a new kind of supply on the ground).
- **Crabs cannot be caught**: catching would need supply code for them; they stay ambience.
- **Kills.** An animal's kill counts for nobody; a dead animal counts in no player's statistics either (the census keeps both, and loads stolen). Players can right-click an animal with warriors; hovering shows its name.
- **Death.** It falls onto its side in 0.4 s, lies there 1.5 s and sinks 1 m over 3 s (no death animation). **Sounds:** the chicken's death clip for every animal, nothing else; new clips need a source (C6).
- **The AI.** Normal and Hard send 2 / 3 idle throwers after the boar or wolf nearest to where one of their peons was killed (within 20 cells). Without it a Hard AI lost 13 peons to wolves in the six-tribe test match; with it, 4.
