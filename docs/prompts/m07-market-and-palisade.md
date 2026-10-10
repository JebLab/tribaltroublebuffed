# M7: Market and Palisade/Gate

Follow [docs/SESSION_RULES.md](../SESSION_RULES.md) (first actions, decisions, closing, launching the next session).

## The milestone

From [MILESTONES.md](../MILESTONES.md): a resource conversion building; wall segments and gates with pathfinding support. **Done when** walls block, gates admit allies, the Market converts.

Design first: write `docs/design/market.md` and `docs/design/palisade.md` from PLAN.md §4.5 (as M6 wrote [design/gear.md](../design/gear.md)), with the choices below. In practice:

1. **Numbers** (PLAN.md §4.5, Buffed only): *Trading Post / Market*: footprint 3, 150 hit points, 30 wood; peons inside convert 3 of one resource into 1 of another every 20 s. *Palisade and Gate*: one cell per segment, 40 hit points (gate 120), 8 wood; built fast; blocks movement; the gate lets its owner and allies through. Into `buffed.json`, with the fields also in `resurrected.json` behind `features` flags that are false there (R-14).
2. **Decide and log:** how a player picks what the Market converts (it needs a choice of "from" and "to": a new player command, which renumbers the game events, or a fixed rule); how walls are placed (one segment per click, or a drag that lays a line of segments; the AI needs the same); how the gate's passability works in the pathfinder (`pathfinder/UnitGrid`, `RegionBuilder`, `Occupant`: buildings occupy cells as `Occupant.STATIC` for everyone today). Gates for "owner and allies" mean per-team passability: find the smallest change that keeps Classic's paths bit for bit (the golden traces prove it).
3. **Wire them in** with [new-content-checklist.md](../new-content-checklist.md) ("Building recipes beyond wood", an `Abilities` bit per new job and `NullController.getKey`, `EmptyUnitContainerFactory` for buildings nobody enters, `AI.classifyIndex`).
4. **Torches** (M6) burn every land building, so palisades and gates burn too; check that this stays true and say so in the design notes ("countered by Torches" in the plan).
5. **AI** (Normal and Hard at least): when to build a Market and what to convert (the lopsided-resource case the plan names), and whether and where to wall; Easy may build neither, as with M5's buildings.
6. **Tests and checks:** `RulesetTest`, behaviour tests in the style of `BuffedGearTest` (headless worlds without AIs: `HeadlessMatchRunner.newWorld(config, false)`): a wall blocks a path, a gate admits its own and allied units and stops enemies, the Market converts at its rate; the AI census (extend `CensusTaker` if needed); `SIM_VERSION` bump with only the Buffed traces moving; an in-game look with `drive_game.py`.
7. If it is too much for one session, do the Market and the Buffed gating first and continue in `m07-part2-...` with the Palisade and Gate (the pathfinder work).

## Read first

- PLAN.md §4.2 and §4.5; [rulesets.md](../rulesets.md) ("Buffed's buildings", "Adding a number"); [new-content-checklist.md](../new-content-checklist.md); [design/gear.md](../design/gear.md).
- `model/LandBuilding.java` (containers, `getWorkMaterial`, the fire), `model/behaviour/RepairController.java`, `pathfinder/` (`UnitGrid`, `RegionBuilder`, `PathTracker`), `delegate/PlacingDelegate` and `render/BuildingSiteRenderer` (placement), `player/AdvancedAI.java` (`nodeBuildBuffedBuildings`).
- M5's commit (`544b865f`, buildings) and M6's (gear) as worked examples.

## Context from M6

- **Classic invariance traps** still apply: gate Buffed AI work behind the feature check (an extra `reclassify()` changes the checksum); append enum values at the end (`DeployType`'s ordinal goes into the checksum); keep the original order of float operations. M6 kept the Armory's gear containers in every ruleset (empty, R-25); that pattern worked and needed no null checks.
- **Match lengths:** Buffed AI matches now last about 20 minutes (1v1) and 30 (six tribes) against Classic's 15 and 25, and swing a lot with the dice (a draft of M6 took 35); the headless limit is 60. Walls could lengthen them further: if a Buffed match stops ending in a victory, look at the AI before raising the limit.
- **Torches are strong** (two bring down an Armory in about 25 s); J-03 asks Josh. Don't retune before his answer.
- **Art by a background agent** worked again (M6: 8 meshes, 4 textures, 2 team masks, 28 icons in about 30 minutes, Blender scripted in the scratchpad). Give it exact file names, scales, `BLENDER_USER_RESOURCES` in the scratchpad, and say it must not change existing sprites. `icons.png` is 1131 × 1024; free cells remain at y 704..959 from x 512 to 1087 (check the pixels).
- **In-game:** type `/iamacheater` (with the slash) in the chat, then press Enter on the empty line to close the chat; F1..F6 cheats then work, and Shift+F2 / Shift+F3 spawn a shield or torch warrior under Buffed. Esc closes an Armory submenu; a second Esc opens the game menu (Resume at 60,265). A new random island each game: placements can be illegal (red), so look before clicking. `drive_game.py key shift+f2` holds modifiers now.
- Josh owes J-01, J-02 and J-03; none blocks this milestone.
- The empty folder `.claude/worktrees/quirky-mayer-5f7d69` may still be locked; leave it.

## Banked decisions that touch this milestone

- Final art needs an artist or a budget (MILESTONES.md "Needs an artist"): placeholders only.
