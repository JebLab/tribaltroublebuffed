# Gear: Drum / Horn and Net / Snare

Buffed's third and fourth pieces of gear (M9), from PLAN.md §4.4. Like the Shield and the Torch ([gear.md](gear.md)) each is a peon plus gear crafted in the Armory, deployed like a weapon and handed back in an Armory; unlike them neither is a fighter first. The Drum plays its army forward; the Net feeds the chicken economy and lays traps. Classic and Resurrected do not have them (the `features.drum` and `features.net` flags are false there).

| Gear | Natives / Vikings | Role | Counter |
|---|---|---|---|
| **Drum / Horn** | Drummer / Hornblower | support aura | focus fire: it is everyone's first target |
| **Net / Snare** | Chicken Catcher / Fowler | catches chickens, lays snares | cheap and fragile |

## Numbers (Buffed, both races)

| | Drum / Horn | Net / Snare |
|---|---|---|
| Armory recipe | 3 wood + 1 iron | 2 wood + 1 chicken |
| Labour | 60 man-seconds | 60 man-seconds |
| Deploy time | 1.5 s | 1 s |
| Hit points | 1 | 1 |
| Speed | 4 m/s (the warriors') | 4 m/s |
| Dodge (`defense_chance`) | 0.5 | 0.3 (the torch's) |
| Attack | none | hand to hand, hit chance 0.4; against buildings a peon's blow |
| Aura | +0.10 hit chance and +15 % speed for its team within 12 m, not stacking | — |
| Chickens | — | caught in one stroke instead of a peon's ten |
| Snares | — | at most 3 lying at a time; each stuns the first enemy on it for 4 s |

Recipes, labour and deploy times are constants in `LandBuilding`, as the other weapons' are. The unit numbers, the aura and the snares are ruleset fields (`drum_warrior`, `net_warrior`, `drum`, `net` in `buffed.json`; also in `resurrected.json`, where the features keep them out; R-14). The plan gives neither unit a speed, the Net no dodge and neither a deploy time: they take the warriors' speed, the torch's dodge ("fragile") and the iron and rock weapons' deploy times.

## The Drum / Horn

- **No attack.** A Drummer has the warrior bit (it is selected with the army, rallies with it, counts as a warrior for the Quarters and the AI's census) but not the attack bit: it never strikes, never hunts on its own and walks where it is sent, an attack order included.
- **Aura.** While a Drummer stands in the world, every unit of its team within 12 m of it (itself included) gets **+0.10 hit chance** and walks **15 % faster**. The hit bonus is added inside the hit chance like the Totem's (R-17): for thrown weapons and blows, not spells, and a unit in a tower has it tripled with the rest. The speed counts for every walking unit, peons and chieftains too. Several Drummers do not add up: a unit gets one Drummer's bonus at most; a Totem's bonus comes on top. The numbers are the Drummer's race's. With no Drummer in the world, hit chances and speeds are computed exactly as before (the code reads an empty list first), so Classic's float operations do not change.
- **Auto-targeted first.** The plan's "priority 4" is the warriors' number in `AttackScanFilter.Priority`, but "first" means ahead of them: a Drummer gets a new priority of its own, above every other unit and below ships (whose number moves up one; the order between the old priorities is unchanged). A tower or a warrior picking a target in range takes the Drummer before warriors and peons.
- **Counters.** 1 hit point, dodge 0.5 like a rock warrior, and every enemy shoots at it first: kill the Drummer and the army slows down.

## The Net / Snare

- **Catching chickens.** A Chicken Catcher right-clicked on a chicken catches it like a peon, carries it to the nearest Armory and goes back for the next, but takes it in one stroke instead of ten. It carries one at a time and walks at its own speed with it. When it finds no chicken it stands idle (a peon would go into a building to work).
- **Snares.** The Lay Snare button (or D), then a click on the ground: each selected Catcher walks to a free cell there and lays a snare in one second (its attack animation). A Catcher has at most 3 snares lying at a time; a fourth takes up its oldest. A snare stays where it is until the first enemy unit (any unit of a player not on its owner's team: peons, warriors, chieftains) steps onto its cell; that unit is stunned for 4 s, as by *Terrifying Toot*, and the snare is gone. A Catcher's snares go with it when it dies or goes into a building, and they never expire otherwise.
- **What sees them.** Everyone: there is no fog of war in this engine (PLAN.md L6), so a snare is drawn for every player like a rally flag, and hovering shows its name. Nothing avoids them: pathfinding and the AIs ignore snares, and they cannot be attacked or selected. Friendly units walk over them.
- **A snare is part of the simulation.** It is a scenery model with a trap checked once a tick (the first enemy on its cell, in the world's animation order), so it replays identically. A stunned unit cannot dodge (`Unit.getDefenseChance`), which makes a snare in a tower's range deadly.
- **One new action, not a new command.** Laying is `Action.SNARE` (added at the end of the enum) sent with the existing `setLandscapeTarget`; only Catchers act on it, other selected units ignore it. The Armory's two new orders, `buildDrumWeapons` and `buildNetWeapons`, are new player commands like the Shield's and Torch's (they renumber the game events).
- **Counters.** 1 hit point, dodge 0.3, a 0.4 blow: anything kills it. Snares are a nuisance, not a wall: each stops one unit for 4 s.

## The AI (Normal and Hard)

- **Production.** The Armory keeps up to 1 (Normal) or 2 (Hard) of each in stock, like the gear's capped orders (R-26). Easy builds neither (R-20).
- **Drums.** An attack of at least 8 warriors takes one Drummer per 8, as far as the stock allows; it marches with the throwers, which follow the shields on the AI's next decision, so the Drummer is behind the front line it speeds up.
- **Nets.** It keeps 1 (Normal) or 2 (Hard) Catchers out. An idle Catcher with fewer than 3 snares lying lays one on the approach to the base: on a ring 10 cells from the Quarters, the next of six spots towards the island's centre first. With its snares out it catches chickens (the nearest flock's). Catchers stay out of attacks and of the base defence.

## Art (placeholders)

New sprites `natives/drum_warrior` (the Native warrior with a hand drum and a stick), `vikings/drum_warrior` (the Viking warrior with a curved horn), `natives/net_warrior` and `vikings/net_warrior` (a pole with a hoop net in place of the spear or axe), on the warrior's skeleton and animations, with tinted copies of the rock warrior's texture; `natives/snare` and `vikings/snare`, still models of a loop of vine or rope with a stake. Icons on the shared sheet. Final art needs an artist (MILESTONES.md).
