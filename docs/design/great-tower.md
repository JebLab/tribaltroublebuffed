# Great Tower

Buffed's late-game anchor (M8), from PLAN.md §4.5: a tower three throwers man at once, and the first building with a material other than wood in its recipe. Classic and Resurrected do not have it (the `features.great_tower` flag is false there).

## Numbers (Buffed, both races)

| | Great Tower | (the Tower, for comparison) |
|---|---|---|
| Footprint | size 5: 7 × 7 grid cells (14 × 14 m), like the Quarters and the Armory | size 3: 3 × 3 cells |
| Hit points | 300: 50 logs, then 10 rocks | 100: 20 logs |
| Throwers | 3 | 1 |
| Range | +8 m for each thrower | +8 m |
| Hit chance | × 3 for each thrower | × 3 |

The plan's recipe, "40 wood + 10 rock", does not add up to its 300 hit points under the rule that peons build every building at 5 hit points a load (PLAN.md §4.2): 40 logs and 10 rocks are 250. The hit points stand and every load, log or rock, is 5 of them: 50 logs and then 10 rocks, which peons fetch themselves, as they fetch the Totem's rock (R-16) (R-35). The numbers are ruleset fields (`great_tower` in `buffed.json`: `hit_points`, `rock`, `throwers`; also in `resurrected.json`, where the feature keeps it out; R-14). The +8 range and the triple hit are the Tower's constants (`MountUnitContainer.ATTACK_RANGE_INCREASE`, `AttackBehaviour`).

## Rules

- **It is a tower.** It has the Tower's job (`Abilities.ATTACK`) and everything that goes with it: enemies attack it with the Tower's priority, a right-click on an enemy sends every thrower inside at it, the Exit button lets a thrower out, Terrifying Toot stuns the throwers inside, and a hit aimed at a thrower hits the building.
- **Three places.** `MountUnitContainer` holds up to three units (one for the Tower). Each thrower stands at its own place on the top, about 2 m from the centre, and fights on its own: it scans for targets in the Tower's range and throws with +8 m and × 3, as in the Tower. The Exit button lets out the last thrower who went in.
- **Throwers only.** Like the Tower it takes warriors with thrown weapons; gear warriors and Champions stay out (R-23), because +8 m and × 3 would turn a blow into a long-range strike.
- **Building it.** 50 logs build its first 250 hit points; then the peons fetch 10 rocks, from the rock nodes or carried out of an Armory (the Armory's rock transporters), each adding the last 5 hit points. Repairs take logs, as for every building.
- **Limits.** It counts against the 20 buildings like any other.
- **Counters.** Torches burn it (every land building burns, M6): 300 hit points take a while, and the throwers inside shoot at the torches. Terrifying Toot stuns all three throwers. Enough warriors bring it down like any building.

## The AI (Normal and Hard)

- **When and where.** One Great Tower once the base is done, above 50 (Normal) or 40 (Hard) units, 12 cells from the Armory towards the island's centre (its towers stand 10 cells from the Quarters); `buildBuilding` finds the nearest legal site. Ten builders. Easy builds none (as with M5's buildings).
- **Manning.** It keeps three throwers inside: each decision it sends idle throwers for the free places not already walking there, and deploys a warrior from the Armory when none is idle, as it does for its towers.
- **What it does to a match.** In the headless matches every Normal and Hard AI builds and mans one. A beaten AI's last three throwers in it, with a peon repairing it, can hold out for minutes: in one 1v1 seed they did for ten. The Buffed matches still end in a victory well within the 60-minute limit (R-41 and the M8 log).

## Art (placeholders)

New sprites `natives/great_tower` and `vikings/great_tower` with their halfbuilt and start stages: the race's Tower stages widened to the 14 m footprint (Natives x 3.1, y 3; Vikings x 2.5, y 2.3) and 1.2 times as tall, with stone-grey tinted copies of the Tower's texture and the Tower's team masks. The throwers stand on the top's floor, at 15.6 m (Natives) and 11.5 m (Vikings). Icons on the shared sheet. Final art needs an artist (MILESTONES.md).
