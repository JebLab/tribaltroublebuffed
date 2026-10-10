# Palisade and Gate

Buffed's walls (M7), from PLAN.md §4.5: territory play on big maps. A palisade segment blocks movement; a gate lets its owner's team through and stops everyone else. Classic and Resurrected do not have them (the `features.palisade` flag is false there).

## Numbers (Buffed, both races)

| | Palisade segment | Gate |
|---|---|---|
| Footprint | one grid cell (2 × 2 m; size 2, like the Totem) | one grid cell |
| Hit points | 40: 8 logs | 120: 24 logs |
| Limit | 100 segments and gates per player, outside the 20-building limit | (shared) |

The plan gives "8 wood" for both; the gate keeps the rule that every building is built at 5 hit points a log (PLAN.md §4.2), so its 120 hit points take 24 logs (R-29). The numbers are ruleset fields (`palisade.hit_points`, `palisade.max_segments`, `gate.hit_points` in `buffed.json`; also in `resurrected.json`, where the feature keeps them out; R-14).

## Placing

- **A palisade is dragged.** Press on the ground, drag, release: the segments fill the line between the two cells, at most 20 at a time. The line is a staircase of cells that touch side by side, never only at a corner, because units step diagonally between cells and would slip through a diagonal line. A click without a drag lays one segment. **A gate is a click**, one cell.
- **They are laid out at once.** Unlike other buildings, whose site appears when the first peon arrives, every segment and gate of the order becomes a construction site immediately (it blocks at once). Cells that are not free (a building, a tree, a rock, a moving unit, water, steep ground) are skipped; standing units are moved aside. Walls need no free border: segments touch each other and may touch other buildings.
- **Builders move along.** The selected peons are spread along the line; a peon whose segment is finished goes on to the nearest unfinished palisade or gate of its owner within 16 m, so a few peons build a whole line.
- One new player command, `placePalisade(peons, building, x1, y1, x2, y2)`, lays a line of palisade or one gate; the AI calls `Player` directly.
- Walls keep the landscape as it is (other buildings flatten the ground under them, but neighbouring segments would undo each other's flattening).

## Blocking and passing (the pathfinder)

- **A palisade is a building in its cell** (`Occupant.STATIC`), so nothing walks through it. The pathfinder's region graph is built once from the terrain and does not see buildings: units find their way around a wall with the grid search (up to 600 cells per search), as they do around any building. A closed ring stops them at the wall; aggressive units then attack what they can reach, the wall included (it has the lowest attack priority, like a Quarters), and torches burn it.
- **A gate is a building too, and the occupant of its cell,** but units of its owner's team walk through it: the grid search treats the cell as free for them, and a unit stepping in takes the cell while it stands there and hands it back to the gate when it leaves. Everyone else (enemies, neutral players, chickens) meets a building. A unit inside a gate can be seen and hit like any unit; no unit or building is ever placed in a gate's cell.
- **Classic is untouched.** `UnitGrid` keeps gates in a layer of their own that stays empty until a gate is placed, and every new check asks that layer first, so Classic and Resurrected run exactly the 2004 path search (their golden traces did not move).
- Walls and gates do not keep a player alive (the standard rules count units, the chieftain and the Quarters) and do not count against the 20-building limit.

## Counters

Torches: a torch's blow burns any land building, construction sites included, so palisades and gates burn (40 hit points: two blows and their fire bring a segment down); a peon's repair puts the fire out. Warriors and towers knock them down like any building.

## The AI (Normal and Hard)

- **Where.** One line of about 9 cells with a gate in the middle, across the direction from its Quarters to the island's centre: a screen in front of the base that funnels attackers without closing the base in. It tries 13 cells out first (its towers stand at 10), then 11, 15, 9 and 17, and takes the first place where the gate and at least 5 segments fit; cells that are not free are skipped. The line is laid as two halves that end in the gate's cell, so the gate always stands in it.
- **When.** Once its base is done, above 55 (Normal) or 40 (Hard) units; 9 builders, a third of them on the gate (its 24 logs would otherwise come last). In the headless 1v1 both Hard AIs finish the gate about 8.5 minutes in. Easy builds no walls (as with M5's buildings).
- **Attacking.** Its attacks aim at real buildings, not at palisades and gates; units blocked by a wall attack what is in reach, the wall included.

## Art (placeholders)

New sprites `natives/palisade`, `natives/gate`, `vikings/palisade`, `vikings/gate` with their halfbuilt and start stages: a square cluster of sharpened logs that fills its cell (segments are never rotated, so they must join in any direction) and an open frame of taller posts with a team-coloured mark. Icons on the shared sheet. Final art needs an artist (MILESTONES.md).
