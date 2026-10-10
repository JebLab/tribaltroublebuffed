# New unit and new building checklist

What adding a unit type or a building touches, in the order to do it. Nothing in the engine is registered by name: units and buildings are fixed integer ids in `Race`, built explicitly in `RacesResources`, and weapon types are told apart by their class in a dozen places. Paths below are under `tt/src/main/java/com/oddlabs/tt/` unless they say otherwise. Models come from the Blender pipeline in [content-pipeline.md](content-pipeline.md); the numbers live in the rulesets ([rulesets.md](rulesets.md)).

**Calibration.** Resurrected's first ship commit (`006dcbeb`, a building type that moves) touched 109 files: 64 Java files (24 in `model/`, 11 in `model/behaviour/`), 15 `.properties`, `icons.xml`, 23 geometry files and 4 textures. A gear variant on the existing warrior rig (PLAN.md §4.4: shield, torch, drum, net) is far cheaper: about 15 to 25 files. Build it one step at a time with `gradlew build` green in between.

## Before you start

- [ ] It has a design: numbers and counters in `docs/design/` or PLAN.md §4.4 to 4.5, and the [rules new content must obey](PLAN.md) (§4.2: 1 HP units, gear from the Armory, buildings built from wood).
- [ ] It is behind the *Buffed* ruleset (below); Classic and Resurrected must not change. Their golden traces prove it.

## Art

- [ ] **Model and low-detail model** in Blender, exported with the add-on ([content-pipeline.md](content-pipeline.md)): about 500 triangles and an 80-triangle LOD for a unit, 2,000 and 200 for a building (PLAN.md §4.1). A gear variant reuses the warrior rig and adds a prop mesh, as the spear and the axe do.
- [ ] **Building stages**: three sprites, start, half-built and built (`LandBuilding.getRenderLevel()` switches at 50 % and 100 %), each with a high- and a low-detail model.
- [ ] **Animation set**, in the order the game picks them (by position, not by name): `idle, run, attack, die, sit` for warriors; peons add `steer, paddle_right, paddle_left`; chieftains have `magic, thor` at 4 and 5 (`Unit.Animation`). `run` gets `wpc` = metres per cycle. Buildings need no skeleton (they get an identity animation).
- [ ] **Textures**: hand-painted PNG in `assets/textures/models/` (256² for units, 512² for buildings) and a team-colour mask `<name>_team.png` in `assets/textures/teamdecals/` (`team=` in `geometry.xml`). Weapon tiers are texture variants: the `<texture>` order must match `Race.UNIT_WARRIOR_ROCK/IRON/RUBBER` (0, 1, 2).
- [ ] **`geometry.xml` entry**: the export writes a `<sprite>` snippet into the Blender text `tt_sprite.xml`; paste it under the race's `<group>`. The runtime path is `/geometry/<group>/<sprite>.binsprite`.
- [ ] **Icons**: draw them on `assets/textures/gui/icons.png` (normal, active, disabled) and add an element to `tt/src/main/resources/gui/icons.xml`, including its line in the DTD at the top (the parser validates). Java side: `gui/GUIIcons.java`, `gui/RaceIcons.java`; status icons via `model/Cost.iconList()`.
- [ ] **Sounds**: 2 to 6 short mono Ogg clips per action in `tt/src/main/resources/sfx/` (death, throw, hits; buildings share `building_hit` and `building_crash`), passed to the `UnitTemplate` and `ThrowingFactory` in `RacesResources`.

## Wiring it in

- [ ] **Ids**: `model/Race.java` (`BUILDING_*`, `NUM_BUILDINGS`, `UNIT_*` and the fixed template array with its index assert). A building with a new job gets an `Abilities` bit too (the Chicken Coop has `BREED`, the Totem `AURA`) and that bit goes into `NullController.getKey`: the AI groups finished buildings by those bits and counts any finished building without one as a construction site.
- [ ] **Templates**: `model/RacesResources.java` creates the `SpriteFile`s, `UnitTemplate`s (lines ~651 to 830), weapon factories (~607 to 638) and building templates (`createBuildingTemplate`). For a thrown weapon, the release point is a fraction of the attack animation (`46f/100f` for Natives, `29f/58f` for Vikings): if the attack has a different number of frames, change it there. A building nobody enters takes `EmptyUnitContainerFactory` (code such as `DensityMap` reads every finished building's unit container). Size n covers (2n − 3) × (2n − 3) grid cells of 2 m, so 2 is the smallest that occupies the grid.
- [ ] **Gear and recipes**: a weapon class in `model/weapon/`: a `ThrowingWeapon` subclass for a thrown weapon (one per race, sharing one container), or for gear that fights hand to hand a key class and a `GearFactory` (M6's `Shield` and `Torch`, shared by both races). `WeaponFactory.getType()` returns that class: it keys the Armory's stock, production and deploy containers and the weapon a warrior hands back. The recipe (`COST_*_WEAPON`) and the build and deploy times in `model/LandBuilding.java` (every Armory has the containers; the player's permission keeps them empty under other rulesets); a `model/DeployType` value, added at the end, because its ordinal goes into the world checksum; `buildXWeapons` in `player/PlayerInterface`, `player/Player` and `net/NoOpPlayerInterface` (a new player command renumbers the game events, so it changes the simulation; the headless AIs call `Player` directly, so the traces do not see it).
- [ ] **Building recipes beyond wood**: hit points are built 5 per log. A building that also takes another material says so through `Building.getWorkMaterial`, `needsMaterial` and `deliverMaterial` (`LandBuilding`: the Totem's finishing rock, the Chicken Coop's stock of chickens); `RepairController` then sends peons to fetch it, so neither the player nor the AI needs a new command.
- [ ] **Code that checks the weapon class**: `Unit.isWarrior` (any Armory weapon) and `Unit.isGearWarrior` (hand to hand: no towers, no ships; `MountUnitContainer`, `ShipUnitContainer`, `AI.getIdleThrowers`), `landscape/DensityMap.calcWeight`, `player/AdvancedAI.getUnitScore`, `model/ShipHR`, `model/Ship` (its own deploy and supply code), `gui/BuildSpinner.order`, `gui/ActionButtonPanel`, `model/Cost.getIconQuad`.
- [ ] **Interface**: build, deploy and status buttons in `gui/ActionButtonPanel.java`; a `input/GameAction` with a default key in `input/InputManager` and the conflict list in `input/KeyBindingConflicts`; building placement in `delegate/PlacingDelegate` and `render/BuildingSiteRenderer`. Hover tooltips use the template name (`render/ToolTipAdapter`).
- [ ] **AI**: build and deploy rules in `player/AdvancedAI.java` (per-difficulty tables at the top, `nodeBuild*` and `nodeDeploy*`) and `player/AI.java` (`classifyIndex` groups buildings by ability: a new building with `ATTACK` counts as a tower). Campaign scripts in `player/campaign/` use the `Race` ids too.

## Numbers and the ruleset

- [ ] **Ruleset fields**: a component in `ruleset/RulesetStats.java` (`RaceStats` has one per unit and building), its values under both `natives` and `vikings` in `tt/src/main/resources/rulesets/resurrected.json`, and Buffed's numbers in `buffed.json`. The loader is strict: a missing or unknown field fails. See "Adding a number" in [rulesets.md](rulesets.md).
- [ ] **Gate it**: a `Features` flag (false in `resurrected.json`, true in `buffed.json`) that the `Player` constructor turns into its build permission (`canBuild`), that hides the buttons in `ActionButtonPanel`, and that keeps the AI out; update `RulesetTest`, which pins the `Features` values. AI code for Buffed content runs only behind such a check (see `AdvancedAI.nodeBuildBuffedBuildings`): even an extra `reclassify()` changes the simulation, because it re-issues the Armory's production orders.

## Words

- [ ] **Strings in all six languages** (en, da, de, es, it, pt; non-ASCII as `\uXXXX`): the name in `tt/src/main/resources/com/oddlabs/tt/model/RacesResources*.properties`, button tooltips in `gui/ActionButtonPanel*.properties`, the key label in `form/OptionsMenu*.properties`.

## Proving it

- [ ] **Unit test** for its rules (the ruleset numbers in `RulesetTest`, behaviour where it can be tested headless).
- [ ] **Headless AI matches**: a new unit or building changes the simulation. Bump `SIM_VERSION` in `common/src/main/java/com/oddlabs/util/Compatibility.java` and regenerate the golden traces ([testing.md](testing.md)): `./gradlew.bat tt:test --tests com.oddlabs.tt.headless.GoldenTraceTest -PupdateGoldenTraces`. Only the Buffed traces may move. The match census (`HeadlessMatchResult.census`) shows which buildings each AI finished; `BuffedBuildingsTest` uses it to check that the AI builds the new ones only under Buffed.
- [ ] **In game**: `gradlew tt:run`, a Buffed skirmish; in single player, typing `iamacheater` in the chat (Enter) enables the F-key cheats (F2 to F4 spawn warriors at the centre of the view, F6 kills the selection). New loading code must also work under `Headless.enable()` (no textures, sounds or icons).
- [ ] **Log it**: the milestone's log row; "Josh's checks" in [DECISIONS.md](DECISIONS.md) for how it plays.
