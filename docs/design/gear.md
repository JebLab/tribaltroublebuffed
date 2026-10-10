# Gear: Shield and Torch

Buffed's first new units (M6), from PLAN.md §4.4. Like every warrior they are a peon plus a piece of gear crafted in the Armory, so they inherit the deploy, return and AI machinery; unlike the warriors of 2004 they do not throw, they fight hand to hand. Classic and Resurrected do not have them (the `features.shield` and `features.torch` flags are false there).

| Gear | Natives / Vikings | Role | Counter |
|---|---|---|---|
| **Shield** | Bark-Shield Bearer / Round-Shield Carl | front line that soaks throws | chicken warriors (0.95 hit), towers (×3), spells |
| **Torch** | Firebrand / Torchbearer | siege: burns buildings | any warrior, towers |

## Numbers (Buffed, both races)

| | Shield | Torch |
|---|---|---|
| Armory recipe | 2 wood + 1 rock | 2 wood + 1 rock + 1 iron |
| Labour | 40 man-seconds | 80 man-seconds |
| Deploy time | 1 s | 1.5 s |
| Hit points | 1 | 1 |
| Speed | 3.5 m/s (warriors 4) | 4 m/s |
| Dodge (`defense_chance`) | 0.85 | 0.3 |
| Melee hit chance vs units | 0.3 | 0.5 |
| Against buildings | a blow like a peon's: 6 to a tower, otherwise 1 on a hit | always hits for 6 and sets the building on fire |
| Fire | — | −2 hit points a second for 15 s, unless a peon repairs it |

Recipes, labour and deploy times are constants in `LandBuilding`, as the 2004 weapons' are. The unit numbers and the fire are ruleset fields (`shield_warrior`, `torch_warrior`, `torch` in `buffed.json`; also in `resurrected.json`, where the features keep them out; R-14). A deployed warrior who walks back into an Armory returns the gear to stock, like a thrown weapon.

## Rules

- **Melee.** A gear warrior fights like a peon or a chieftain: it walks up to its target (range 0 plus the target's size) and strikes at the release point of the warrior's attack animation (the throw's: 46/100 for Natives, 29/58 for Vikings), every two seconds. The roll is the usual one, `(hit chance + terrain + campaign + totem bonus) × (1 − target's dodge)`, so a totem helps gear warriors too.
- **Towers and ships take throwers only.** A melee warrior cannot man a tower (the tower's +8 range and ×3 hit would turn a blow into a throw) and does not board a ship (ship seats fight with thrown weapons and rowers need the peon's paddling animations). It still counts as a warrior everywhere else: double-click selects it with the army, the Quarters refuse it, it rallies aggressively, the AI counts it in its army.
- **Fire.** A torch blow on a building always hits for 6 and sets it burning for 15 s; another blow restarts the 15 s (fires do not stack). A burning building loses 2 hit points a second (1 every half second) and smokes in orange; at 0 it collapses and the player whose torch lit it is credited. A peon repairing it (a delivered log, or the Totem's rock) puts the fire out at once. Fire is part of the simulation: it advances with game time and its smoke draws only the world's random numbers, so it replays identically. Construction sites burn too; ships do not (a torch blow on a ship is a plain 6).
- **Counters.** Shields soak throws: a rock warrior hits a shield 0.5 × 0.15 = 7.5 % of the time instead of 25 % for a rock warrior. Chicken warriors (14 %), towers (×3) and spells (no dodge roll for most) are the answer. Torches die to anything (dodge 0.3) and to towers; they must reach a building to matter.

## The AI (Normal and Hard)

- **Production.** Under Buffed the Armory keeps a small stock of each, like the infinite orders it gives for the 2004 weapons but capped: shields up to 6 (Normal) or 10 (Hard), torches up to 3 or 5. Easy builds neither (as with M5's buildings).
- **Deploy.** When the AI musters an attack, about a quarter of the group are shields and a fifth torches, as far as the stock allows; the rest are chosen as before (chicken, iron, rock), and more gear makes up the group when the throwers run short. When defending its base it deploys throwers first and shields after; torches stay in stock.
- **Attack.** Shields march first: the throwers' order follows on the AI's next decision, a few seconds later, so the slower shields lead and take the first throws. Torches go for the nearest enemy building instead of the army's target.
- **Towers and ships** are manned with throwers only.

## Art (placeholders)

New sprites `natives/shield_warrior`, `natives/torch_warrior`, `vikings/shield_warrior`, `vikings/torch_warrior`: the race's warrior mesh with a shield on the left forearm, or with a torch in place of the spear or axe, on the warrior's own skeleton and animations, with tinted copies of the rock warrior's texture. Existing sprites are unchanged (the world reads their bounds). Icons on the shared sheet. Final art needs an artist (MILESTONES.md).
