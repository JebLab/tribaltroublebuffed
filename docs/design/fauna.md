# Neutral fauna

Buffed's wild animals (M9, PLAN.md §4.4 "Neutral fauna"): ambient, cheap and on-brand, with two of them a small hazard of the map. Written in M9's first session as the design for its second (`docs/prompts/m09-part2-fauna.md`), which builds them; numbers there may still move, and the session logs what it changes. Classic and Resurrected do not have them (a `features.fauna` flag, false there).

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

The AI needs no new orders: its warriors attack animals in reach like any target (the lowest priority), and its gatherers keep working. Normal and Hard AIs send one warrior with each group of woodcutters when the island has boars or wolves (optional, if it stops their peons being eaten; measure with the census: peons lost to animals per AI).

## Art (placeholders)

Three new skeletons with three animations each (idle, run, attack; a hit animal plays no death animation: it falls over and fades like a killed chicken), about 300 triangles: `misc/crab`, `natives/monkey`, `natives/boar`, `vikings/wolf`, with textures in the style of the chicken's. The animals' animation positions are their own (an animal class picks them itself, unlike `Unit.Animation`). Final art needs an artist (MILESTONES.md).

## Open for the second session

- Whether monkeys drop the stolen load in the forest (where anyone could pick it up again) or it is simply gone (simpler; the plan says "steal").
- Whether crabs can be caught for food (they are not in the plan; leave them as ambience unless it is free).
- Sounds: the chickens' are reused for now; new clips need a source (C6).
