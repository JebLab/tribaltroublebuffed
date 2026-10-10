# M6: Shield and Torch gear

Follow [docs/SESSION_RULES.md](../SESSION_RULES.md) (first actions, decisions, closing, launching the next session).

## The milestone

From [MILESTONES.md](../MILESTONES.md): two Armory recipes and the units they create; the burning-building mechanic; AI deploy rules. **Done when** units deploy from the Armory and behave per `docs/design/`.

`docs/design/` does not exist yet: PLAN.md §4.4 is the design. Write `docs/design/gear.md` from it first (numbers, rules, counters, and the choices you make below), so "behave per docs/design/" has something to point at. In practice:

1. **Numbers** (PLAN.md §4.4, Buffed only): *Shield* (Bark-Shield Bearer / Round-Shield Carl): 2 wood + 1 rock, 40 man-seconds, dodge 0.85, speed 3.5, melee hit 0.3. *Torch* (Firebrand / Torchbearer): 2 wood + 1 rock + 1 iron, 80 man-seconds, melee hit 0.5 against units; against buildings it always hits for 6 and sets them burning, −2 HP/s for 15 s unless peons repair; dodge 0.3. Into `buffed.json`, with the fields also in `resurrected.json` behind `features` flags that are false there (R-14 is the pattern M5 set). Every warrior today throws; these two fight in melee (`InstantHitFactory`, as peons and chieftains do): decide how, and log it.
2. **Wire them in** following [new-content-checklist.md](../new-content-checklist.md), "Gear and recipes" and "Code that checks the weapon class": a weapon class per race in `model/weapon/`, recipes and build/deploy times in `model/LandBuilding.java`, `DeployType` values, `buildXWeapons` in `PlayerInterface`/`Player`/`NoOpPlayerInterface` (a new player command renumbers the game events), `Race` unit ids and templates, `Unit.isWarrior`, `DensityMap`, `AdvancedAI.getUnitScore`, `ShipHR`/`Ship`, `BuildSpinner`, `ActionButtonPanel` (build and deploy spinners, status icons), `Cost.getIconQuad`, icons, strings in six languages.
3. **Burning:** a building hit by a torch burns for 15 s at −2 HP/s; a peon repairing it puts the fire out. Look at `LandBuilding.hit`, `setHitPoints` and its `damaged_emitter` for the smoke; keep it inside the simulation (game-time animation, world random only).
4. **Placeholder art:** a warrior is the peon rig plus a prop; weapon tiers are texture variants (`Race.UNIT_WARRIOR_ROCK/IRON/RUBBER` = texture indices 0, 1, 2 of the warrior sprite). New gear needs either new texture variants on the existing warrior sprites or new sprites; a shield and a torch prop can be made in Blender from existing meshes (the spear, the axe, a scaled building part). Do not change any existing sprite (the world reads sprite bounds).
5. **AI:** deploy rules in `AdvancedAI` (shields in front when attacking, torches against buildings), Normal and Hard at least; build the weapons through the Armory's production like the others.
6. **Tests and checks:** `RulesetTest`, behaviour tests in the style of `BuffedBuildingsTest` (headless worlds without AIs: `HeadlessMatchRunner.newWorld(config, false)`), the AI census (extend it with deployed unit types), `SIM_VERSION` bump with only the Buffed traces moving, and an in-game look with `drive_game.py`.

## Read first

- PLAN.md §4.2 and §4.4; [new-content-checklist.md](../new-content-checklist.md); [rulesets.md](../rulesets.md) ("Adding a number", "Buffed's buildings").
- `model/LandBuilding.java` (weapon containers, `COST_*_WEAPON`, deploy containers), `model/weapon/WeaponFactory.java` and `InstantHitFactory.java`, `model/RacesResources.java` (unit templates and weapon factories), `player/AdvancedAI.java` (`nodeDeployArmy`, `nodeDeployUnitsInArmory`).
- M5's commit (`544b865f`) as the worked example of Buffed-only content end to end.

## Context from M5

- **Classic invariance traps.** Under Classic and Resurrected nothing may change the simulation: the golden traces prove it. Gate Buffed content with `features` flags (the `Player` constructor turns them into `can_build`; the AI checks first). An extra `reclassify()` in the AI changes the checksum (it re-issues the Armory's production orders), so put Buffed AI work behind the feature check, as `AdvancedAI.nodeBuildBuffedBuildings` does. Floating-point: keep the original order of operations and add bonuses only when non-zero (see `WeaponFactory.attack`).
- **The AI groups finished buildings by `NullController.getKey`** (abilities bits); a finished building without a known bit counts as a construction site.
- **Art by a background agent worked well** (M5: 24 meshes, 12 sprites, textures, icons in about 12 minutes) while the main session wrote code; give it exact file names, the scales to aim for and `BLENDER_USER_RESOURCES` in the scratchpad. Keep placeholder textures at 1024² (R-21). Icon sheet: x 768..1023, y 512..703 is now used by M5's icons; free space remains below y 704 (check the pixels).
- **In-game checks:** a Hard AI on a Small island rushes within a few minutes; use a Normal AI on a Medium island. After a defeat, *Observer mode*, then map mode (Space) and a click on the map, shows the AI's base. `drive_game.py` gained `drag x1 y1 x2 y2` (box-select) and `key add` / `key subtract` (game speed).
- **Building numbers** from the add-on: high-detail building meshes do not round-trip byte for byte (normals within 8.5e-3); low-detail ones and the warrior do.
- The empty folder `.claude/worktrees/quirky-mayer-5f7d69` is still locked by some process, even after `gradlew --stop`; leave it.
- Josh owes J-01 (the add-on's menus) and J-02 (how the coop and totem play); neither blocks this milestone.

## Banked decisions that touch this milestone

- Final art needs an artist or a budget (MILESTONES.md "Needs an artist"): placeholders only.
- If the milestone is too big for one session, do the Shield (recipe, unit, melee, AI deploy) and the Buffed gating first and continue in `m06-part2-...` with the Torch and burning.
