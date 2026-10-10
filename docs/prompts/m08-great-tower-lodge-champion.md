# M8: Great Tower, Spirit Lodge / Mead Hall, Champion

Follow [docs/SESSION_RULES.md](../SESSION_RULES.md) (first actions, decisions, closing, launching the next session).

## The milestone

From [MILESTONES.md](../MILESTONES.md): a three-thrower tower; the Lodge (shelter, spell charge bonus, trains the Champion); the Champion unit. **Done when** all three are buildable under Buffed and the Champion is capped at five.

Design first: write `docs/design/great-tower.md` and `docs/design/lodge-and-champion.md` from PLAN.md §4.4 (the Champion, under "Stretch") and §4.5, as M6 and M7 did ([design/gear.md](../design/gear.md), [design/market.md](../design/market.md), [design/palisade.md](../design/palisade.md)). In practice:

1. **Numbers** (Buffed only; into `buffed.json`, the fields also in `resurrected.json` behind `features` flags that are false there, R-14): *Great Tower*: size 5, 300 hit points, 40 wood + 10 rock; three throwers, +8 range, triple hit. *Spirit Lodge / Mead Hall*: size 5, 200 hit points, 40 wood + 5 iron; shelters 30 units; halves spell charge time within 30 m; trains the Champion. *Champion* (Headhunter / Berserker): speed 5, dodge 0.75, melee hit 0.9, 2 wood + 1 iron + 1 chicken, at most five alive.
2. **Decide and log:** the recipes do not add up under the 5-hit-points-a-load rule (40 logs + 10 rocks is 250, not 300; 40 logs + 5 iron is 225, not 200): pick a rule (the Totem's "logs, then the other material for the last hit points" is R-16; R-29 kept 5 a log for the Gate) and say why. How the tower holds three throwers (`MountUnitContainer` holds one; gear warriors stay out, R-23). How the Lodge shelters units (a container like the Quarters' without reproduction?) and how its spell bonus reaches the chieftain (`Unit.increaseMagicEnergy`). Where the Champion's resources come from (the Lodge has no store: the Market's "nearest Armory" rule, R-28, is one answer) and how training works (a new player command renumbers the events, as M6 and M7 did). The Champion fights hand to hand like the gear (`GearFactory`, R-22) on the warrior rig.
3. **Wire them in** with [new-content-checklist.md](../new-content-checklist.md) (building recipes beyond wood, an `Abilities` bit per new job and `NullController.getKey`, `AI.classifyIndex`, a peons-only container, the 20-building limit). The Great Tower probably shares `Abilities.ATTACK` with the tower: check what the AI and `LandBuilding` do with that bit (`getAttackPriority`, `canExitTower`, `AI.manTowers`).
4. **Torches** burn both buildings (every land building burns); say so in the design notes.
5. **AI** (Normal and Hard at least): when to build each, whether to man the Great Tower with throwers, and whether to train Champions; Easy may do none, as before.
6. **Tests and checks:** `RulesetTest`; behaviour tests in the style of `BuffedMarketAndWallsTest` (headless worlds without AIs, `HeadlessMatchRunner.newWorld(config, false)`): three throwers fire from the Great Tower, the Lodge halves the charge time in range, the Champion's cost and the cap of five; the AI census (`CensusTaker`); `SIM_VERSION` bump with only the Buffed traces moving; an in-game look with `drive_game.py`.
7. If it is too much for one session, do the Great Tower and the Lodge first and continue in `m08-part2-...` with the Champion.

## Read first

- PLAN.md §4.2, §4.4 (Champion) and §4.5; [rulesets.md](../rulesets.md) ("Buffed's buildings", "Adding a number"); [new-content-checklist.md](../new-content-checklist.md); the three design notes above.
- `model/LandBuilding.java` (containers, `getWorkMaterial`, the Market's `findArmory` in `model/Market.java`), `model/MountUnitContainer.java`, `model/ReproduceUnitContainer.java`, `model/Unit.java` (magic energy, `isGearWarrior`), `model/weapon/GearFactory.java`, `player/AdvancedAI.java` (`nodeBuildBuffedBuildings`, `nodeRunMarkets`, `nodeGuardTowers`).
- M7's commit (`f97c0111`) as the latest worked example: a new building with a job, a player command, a panel with its own key context, AI rules, census and tests.

## Context from M7

- **Classic invariance traps** still apply: gate Buffed AI work behind the feature check (an extra `reclassify()` changes the checksum); append enum values at the end; keep float operations in order. M7 kept Classic's paths bit for bit by making every new pathfinder check read an empty (null) gate layer first; do the same for any new per-unit rule.
- **Match lengths:** Buffed AI matches last about 22 minutes (1v1) and 31 (six tribes); the headless limit is 60. If a Buffed match stops ending in a victory, look at the AI before raising the limit.
- **AI building sites:** a fixed spot in front of the Quarters can fall on forest or water; M7's walls try several distances and take the first that fits (`AdvancedAI.nodeBuildWall`). The AI's `buildBuilding` scans up to 40 cells for a legal site.
- **Probing a match:** a throwaway test that builds a world with AIs (`HeadlessMatchRunner.newWorld(config, true)`), ticks it and prints what each player has every 30 s found M7's slow gate in minutes; delete it before committing.
- **Art by a background agent** worked again (M7: 18 sprites, textures, masks, 6 icons in about 22 minutes). Give it exact file names, footprints in cells, `BLENDER_USER_RESOURCES` in the scratchpad, and say it must not change existing sprites or `icons.xml`. Free icon cells on `icons.png` (1131 × 1024): y 704..959 from x 896 to 1087, and y 896..959 from x 512 to 895 (check the pixels).
- **In-game:** to watch AIs, start a skirmish with two Hard AIs on different teams, type `/iamacheater` in the chat (Enter, the text, Enter, Enter), box-select your peons, F6 kills them, then "Observer mode" at the game-over screen; numpad + (`drive_game.py key add`) speeds the game up; Space shows the map and a click there moves the camera. `drive_game.py` has no `[`/`]` keys.
- Strings: `sed` treats `\u` in a replacement as an escape; write `\\u` (a doubled backslash from `printf '\x5c'`), and check the `.properties` files stay ASCII.
- Josh owes J-01 to J-04; none blocks this milestone.
- The empty folder `.claude/worktrees/quirky-mayer-5f7d69` may still be locked; leave it.

## Banked decisions that touch this milestone

- Final art needs an artist or a budget (MILESTONES.md "Needs an artist"): placeholders only.
