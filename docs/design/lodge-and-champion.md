# Spirit Lodge / Mead Hall and the Champion

Buffed's mid-game "tech" building and its unit (M8), from PLAN.md §4.5 and the Champion under "Stretch" in §4.4: the Lodge shelters units, speeds the chieftain's spells and trains the Champion, the Headhunter (Natives) or Berserker (Vikings), a fast, tough fighter capped at five. There is still no research tree: the Lodge is the progression. Classic and Resurrected do not have them (the `features.lodge` flag is false there).

## Numbers (Buffed, both races)

| | Spirit Lodge / Mead Hall |
|---|---|
| Footprint | size 5: 7 × 7 grid cells (14 × 14 m), like the Quarters |
| Hit points | 200: 35 logs, then 5 iron |
| Shelter | 30 of its owner's units |
| Spells | a chieftain within 30 m charges twice as fast (40 s → 20 s, 70 s → 35 s) |
| Trains | the Champion, one at a time, 30 s each |

| | Champion (Headhunter / Berserker) | (an iron warrior, for comparison) |
|---|---|---|
| Cost | a peon in the Lodge, 2 wood + 1 iron + 1 chicken | a peon in the Armory, 2 wood + 1 iron |
| Hit points | 1 | 1 |
| Speed | 5 m/s | 4 |
| Dodge (`defense_chance`) | 0.75 | 0.7 |
| Attack | hand to hand, hit chance 0.9 | thrown, 0.75 |
| Limit | five alive per player | — |

The plan's "40 wood + 5 iron" for 200 hit points does not add up under 5 hit points a load (it would be 225): as for the Great Tower, the hit points stand and every load is 5 of them, so 35 logs and then 5 iron, which peons fetch themselves (R-35). The numbers are ruleset fields (`lodge` in `buffed.json`: `hit_points`, `iron`, `shelter`, `spell_radius`, `spell_charge_factor`, `champion_seconds`, `max_champions`; the Champion's `champion` unit numbers; also in `resurrected.json`, where the feature keeps them out; R-14). The Champion's recipe is a constant in `Lodge`, as the Armory's recipes are in `LandBuilding`.

## The Lodge

- **Shelter.** Any of its owner's units but the chieftain can go in (right-click it): peons, warriors with thrown weapons or gear, Champions; up to 30. Inside they are out of the world, safe from blows, throws and spells, as peons in the Quarters are: they count towards the unit limit and keep their owner alive, and they die if the Lodge falls. A peon's load is lost, as in the Quarters. The Lodge's panel lets them out (the Leave spinner, one every half second, in the order they went in) towards its rally point. Warriors keep what they carry: a sheltered iron warrior comes out an iron warrior.
- **Spells.** The owner's chieftain, or an ally's (the owner's team, as for the Totem, R-17), charges both spells twice as fast while within 30 m of a finished Lodge: 20 s and 35 s instead of 40 and 70. Two Lodges do not add up. Without a finished Lodge in the world the chieftain charges exactly as before.
- **Building it.** 35 logs build its first 175 hit points; then the peons fetch 5 iron, from iron nodes or carried out of an Armory (the Armory's iron transporters, after a Market has traded for it on an island without iron), each adding the last 5 hit points.
- **Counters.** A 200-hit-point building like the Quarters: torches set it on fire (a fire burns every land building), warriors knock it down, and what is inside dies with it.

## The Champion

- **Training.** The Lodge's Train spinner orders Champions as the Armory's spinners order weapons (a click adds one, the infinite order keeps training). One Champion trains at a time. Training starts when a peon is inside, the owner's nearest finished Armory holds 2 wood, 1 iron and 1 chicken (the Market's rule, R-28: the Lodge has no store of its own), and fewer than five Champions are alive; it takes the cost from that Armory then. It lasts 30 s, and waits while no peon is inside; at the end a peon inside steps out as the Champion, towards the rally point. A Lodge that falls while training loses the cost.
- **One new player command**, `trainChampions(lodge, number, infinite)`, which renumbers the game events (as M6's gear orders and M7's commands did); the AI calls `Player` directly.
- **Five alive.** A player's Champions are counted wherever they are: in the world, sheltered in a Lodge, or in training. A sixth does not start until one dies.
- **Fighting.** A Champion fights hand to hand like the gear (`GearFactory`, R-22, with a `Champion` key): it walks up to its target and strikes at the release point of the warrior's attack (46/100 for Natives, 29/58 for Vikings) every two seconds, with the usual roll, `(0.9 + terrain + campaign + totem bonus) × (1 − target's dodge)`. Against a building its blow is a peon's: 6 to a tower, otherwise 1 on a hit. Like other gear warriors it cannot man a tower or board a ship (R-23); it cannot enter the Armory either (it has no gear to hand back) nor the Quarters or the Market. Only a Lodge shelters it. It counts as a warrior everywhere else: it is selected with the army, rallies aggressively and the AI counts it in its army.
- **Counters.** 1 hit point like everyone: chicken warriors (0.95 × 0.25, 24 % a throw), towers (× 3) and spells kill it. It is fast enough to catch throwers but costs a chicken and a peon's time in the Lodge.

## The AI (Normal and Hard)

- **When and where.** One Lodge once the base is done, above 45 (Normal) or 35 (Hard) units, by the Quarters (where its chieftain is trained and comes home), and only when the home island has iron for its last 5 hit points. Eight builders. Easy builds none (as with M5's buildings).
- **Champions.** It keeps one peon inside and orders Champions while it has fewer than 3 (Normal) or 5 (Hard). Its Champions join its attacks with the shields at the front.
- **Shelter.** It does not hide other units in the Lodge.

## Art (placeholders)

New sprites `natives/lodge` and `vikings/lodge` with their halfbuilt and start stages: the race's Quarters meshes at 0.9 scale with purple (Spirit Lodge) and amber-gold (Mead Hall) tinted copies of the Quarters' textures; no new meshes. A Lodge smokes from its roof while a Champion trains. New unit sprites `natives/champion` and `vikings/champion`: the race's warrior mesh with a two-handed carved war club or a double-bladed great axe in the right hand and red war paint, on the warrior's skeleton and animations. Icons on the shared sheet. Final art needs an artist (MILESTONES.md).
