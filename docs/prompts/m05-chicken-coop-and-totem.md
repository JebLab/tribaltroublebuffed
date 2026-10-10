# M5: Chicken Coop and Totem

Follow [docs/SESSION_RULES.md](../SESSION_RULES.md) (first actions, decisions, closing, launching the next session).

## The milestone

From [MILESTONES.md](../MILESTONES.md): the first two buildings: build stages, HP, recipes, behaviour (chicken spawning; hit-chance aura), icons, strings, AI build rules; placeholder models (recoloured existing ones) until art exists. **Done when** both are buildable under Buffed, the AI builds them, and Classic is unaffected.

In practice:

1. **Design numbers** from PLAN.md §4.5: *Chicken Coop / Henhouse*, footprint 3, 100 HP, 20 wood plus 2 chickens to stock, spawns one chicken every 90 s up to six roaming nearby; *Totem / Runestone*, footprint 1, 30 HP, 5 wood plus 1 rock, +0.05 hit chance for friendlies within 10 m, at most two stacking. Write them into `buffed.json`; the building fields must exist in `resurrected.json` too (the loader is strict), so decide how a building that Classic and Resurrected do not have is represented (a `Features` flag that is false there is the expected answer) and log the choice.
2. **Wire them in** following [new-content-checklist.md](../new-content-checklist.md): `Race` ids, `RacesResources` templates, `RulesetStats`, `Player.canBuild`, `ActionButtonPanel` buttons, key bindings, icons (`icons.xml` and its DTD), strings in all six languages, `AdvancedAI` build rules.
3. **Behaviour:** the coop's chickens: wild chickens are "rubber" in the code (`model/RubberSupply`, `RubberGroup`, `RubberSupplyManager`, which caps an island at three flocks); the coop should spawn the same kind of catchable chicken near itself. The totem's aura changes the hit roll (`hit_chance * (1 - target.getDefenseChance())` with the terrain and tower bonuses; find where thrown weapons roll, e.g. `model/weapon/`, and `LightningCloud` for the pattern).
4. **Placeholder models:** three build stages (start, half-built, built), each with a high- and a low-detail model, no skeleton. Make them with the M4 pipeline ([content-pipeline.md](../content-pipeline.md)): import an existing building sprite (`natives/tower`, `natives/quarters` ...) in Blender, rescale or rearrange it to the footprint, export it under new file names and new `<sprite>` entries. Recoloured textures: copy and tint the PNGs (Blender's Python can load, change and save images; or Java's ImageIO in `tools/`). Do not change any existing sprite: the world reads sprite bounds, so that would move the Classic and Resurrected traces.
5. **Tests:** ruleset numbers in `RulesetTest` (its `Features` pins included), behaviour where it can be tested headless, then bump `SIM_VERSION` and regenerate the golden traces: only the Buffed traces may move ([testing.md](../testing.md)). Check in game (`drive_game.py`): build both under Buffed, see a chicken appear, see the AI build them; Classic shows no new buttons.

## Read first

- PLAN.md §4.2 (the rules new content must obey) and §4.5 (the buildings).
- [new-content-checklist.md](../new-content-checklist.md) and [rulesets.md](../rulesets.md) ("Adding a number").
- `model/RacesResources.java` (`createBuildingTemplate`, the size constants at lines 58 to 63; `MAX_BUILDING_SIZE` is also used by `procedural/Landscape`), `model/LandBuilding.java`, `model/Race.java`, `player/AdvancedAI.java`.

## Context from M4

- Blender 5.2.2 LTS is at `.toolchain/blender/blender.exe` (portable, ignored by Git). The add-on runs headless from a script too: `tools/blender/roundtrip.py` shows how to load it (`importlib`, then `register()`), import a sprite (`import_sprite`) and export (`export_objects`). Set `BLENDER_USER_RESOURCES` to a scratch folder if you install the add-on, so Josh's Blender settings stay untouched.
- In-game cheats in single player: type `/iamacheater` in the chat, then F1 spawns a peon, F2 to F4 warriors, F6 kills the selection. `drive_game.py` now has `rclick`, `type` (letters, digits, `/`) and `zoom` (an enlarged crop).
- The session's safety classifier refused to overwrite tracked asset files for a temporary in-game test; M4 used a disposable worktree in the scratchpad instead (`git -c core.longpaths=true worktree add --detach <path> HEAD`; the long paths are needed for `ci/p2-data`). Remove it with `git worktree remove` when done.
- Animations are picked by position, not by name; buildings have no skeleton and get one still frame.
- The empty folder `.claude/worktrees/quirky-mayer-5f7d69` was still locked by an old process; try deleting it again after `./gradlew.bat --stop`.
- J-01 (the add-on's menus in Blender) is owed by Josh; it does not block this milestone.

## Banked decisions that touch this milestone

- Final art needs an artist or a budget (MILESTONES.md "Needs an artist"): placeholders only.
- If the milestone is too big for one session, do the coop and the Buffed gating first and continue in `m05-part2-...`.
