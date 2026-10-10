# M9: Drum/Horn and Net/Snare gear; neutral fauna

Follow [docs/SESSION_RULES.md](../SESSION_RULES.md) (first actions, decisions, closing, launching the next session).

## The milestone

From [MILESTONES.md](../MILESTONES.md): first, Josh's balance answers D-09 and D-10; then a support aura unit (Drum / Horn), chicken-catching and snares (Net / Snare), and neutral fauna (beach crabs, monkeys, boars / wolves). **Done when** they are in game under Buffed.

In practice:

1. **D-09 and D-10 first, as their own commit:** `torch.building_damage` 6 → 4 and `shield_warrior.defense_chance` 0.85 → 0.7 for both races, in `buffed.json` and in the Buffed-only numbers `resurrected.json` holds (R-14), with `RulesetTest`, docs/design/gear.md, rulesets.md and the gear tests (`BuffedGearTest`: the torch's 36 hit points a blow and fire, the Armory falling in about 25 s) updated; `SIM_VERSION` bump, only the Buffed traces move.
2. **Design first:** `docs/design/drum-and-net.md` and `docs/design/fauna.md` from PLAN.md §4.4 (the table rows and "Neutral fauna"), as M6 to M8 did ([design/gear.md](../design/gear.md), [design/lodge-and-champion.md](../design/lodge-and-champion.md)). Numbers into `buffed.json` (and `resurrected.json` behind `features` flags that are false there).
3. **Drum / Horn** (Drummer / Hornblower), 3 wood + 1 iron, 60 man-seconds: no attack; +0.10 hit and +15 % speed to friendlies within 12 m, not stacking; dodge 0.5; "auto-targeted first (priority 4)": `AttackScanFilter.Priority` has WARRIOR at 4 and SHIP at 5, so decide what "first" means and log it. The Totem's aura (`TotemAura`, added inside the hit chance) and the Lodge's spell factor (`Lodge.getSpellChargeFactor`, an empty-list fast path) are the patterns; a speed bonus touches `Unit.getMetersPerSecond`, so keep Classic's float operations exactly as they are when no drummer is near.
4. **Net / Snare** (Chicken Catcher / Fowler), 2 wood + 1 chicken, 60: catches a chicken in one hit instead of ten; lays up to 3 snares that stun the first enemy for 4 s; melee hit 0.4. Snares are a new kind of world object: decide how they are placed (a player command renumbers the events, as `trainChampions` did), what sees them, and how the AI uses them.
5. **Neutral fauna:** crabs on beaches, monkeys that steal one resource from a passing carrier, boars (tropical) and wolves (northern) that attack a lone peon in deep forest; about 300 triangles and three animations each. Chickens (`RubberSupply`, `RubberGroup`, the coop's flock outside the island's three wild flocks, M5) are the nearest pattern. Fauna must not keep a player alive, must stay out of Classic and Resurrected (the island generator runs for every ruleset: spawn them only under the Buffed flag, and check that the Classic and Resurrected traces do not move), and must be deterministic.
6. **Wire them in** with [new-content-checklist.md](../new-content-checklist.md) (M8 added a section on towers for several, shelters and units trained outside the Armory).
7. **AI** (Normal and Hard at least): drums in attacks, nets for chickens and snares in defence; Easy may do none, as before.
8. **Tests and checks:** `RulesetTest`; behaviour tests in the style of `BuffedTowerLodgeTest` (headless worlds without AIs, `HeadlessMatchRunner.newWorld(config, false)`); the AI census (`CensusTaker`); `SIM_VERSION` bump with only the Buffed traces moving; `BuffedStringsTest`'s pattern for the new strings; an in-game look with `drive_game.py`.
9. This is the biggest milestone so far: if it is too much for one session, do D-09/D-10 and the two gear items first and continue in `m09-part2-...` with the fauna.

## Read first

- PLAN.md §4.2 and §4.4; [rulesets.md](../rulesets.md); [new-content-checklist.md](../new-content-checklist.md); the design notes above.
- `model/weapon/GearFactory.java`, `model/TotemAura.java`, `model/Lodge.java`, `model/RubberSupply.java`, `model/RubberGroup.java`, `model/ChickenCoop.java`, `model/AttackScanFilter.java`, `player/AdvancedAI.java` (`nodeBuildGear`, `attackWithGear`, `nodeRunLodges`).
- M8's commit (`42f9deae`) as the latest worked example: two buildings and a unit, a player command, a panel with its own key context, AI rules, census, tests and art.

## Context from M8

- **Classic invariance:** gate Buffed AI work behind the feature check; append enum values at the end; keep float operations in order (M8 kept `t` unmultiplied when no Lodge is near). The golden traces prove it: compare under the old `SIM_VERSION` first (set it back temporarily, run `GoldenTraceTest`, restore it), then bump and regenerate.
- **Match lengths:** Buffed AI matches now last about 28 minutes (1v1) and 30 (six tribes); the headless limit is 60. They swing with every change (M8's drafts took 35 and 43, and 33 to 54 with one building switched off). A throwaway probe that runs both Buffed matches (`new HeadlessMatchRunner(false).run(Matches.ALL.get(name))`, about 5 s each) and prints the final tick, with an AI feature switched off by editing its `MAX_*` table, finds what lengthens a match in minutes; delete it before committing.
- **Art by a background agent** worked again (M8: 14 sprites, 9 textures, 2 masks and 6 icons in about 28 minutes, with exact selection sizes, hit heights, mount offsets and icon quads in its report). Give it exact file names, footprints, `BLENDER_USER_RESOURCES` in the scratchpad, and say it must not change existing sprites or `icons.xml`. **The icon sheet is nearly full:** the only 64 × 192 cells left were used; tell the agent to extend `icons.png` downward (the quads are normalised by the texture's size, `IconQuad`) with existing pixels unchanged. Fauna are new skeletons and animations, not a rig reuse: expect the agent to take longer, and give it the animation positions the game picks (`Unit.Animation`) or design the fauna to need only idle, run and attack.
- **Formatting traps:** `spotlessApply` writes CRLF, so `perl`/`sed` patterns with `\n` stop matching edited Java files: use the Edit tool for multi-line edits. `sed` turns `\u` in a replacement into an escape: write `\\u` (a doubled backslash from `printf '\x5c'`), and check that `.properties` files stay ASCII. A Python helper run from PowerShell (`python -I script.py`) inserts strings in all six languages safely (M8's is in that session's scratchpad; rewrite it).
- **In-game:** a 7 × 7 building needs flatter ground than the green placement cells suggest ("Placement illegal" in the Gradle output); observers cannot select buildings, so check a panel in a played game; an Easy AI attacks within about six game minutes, so spawn guards first (F3 after `iamacheater`); synthetic mouse moves show no tooltips (`BuffedStringsTest` formats them instead); to watch AIs, start two Hard AIs on different teams, type `/iamacheater`, box-select and F6 your peons, then Observer mode; numpad + speeds up; Space shows the map and a click there moves the camera.
- Josh owes J-01 to J-06; none blocks this milestone. D-01: the fork is for private use; nothing beyond the GitHub repository is published.

## Banked decisions that touch this milestone

- D-09 and D-10 (Josh, answered): apply them first (step 1).
- Final art needs an artist or a budget (MILESTONES.md "Needs an artist"): placeholders only.
