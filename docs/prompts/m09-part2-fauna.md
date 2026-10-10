# M9, part 2: neutral fauna

Follow [docs/SESSION_RULES.md](../SESSION_RULES.md) (first actions, decisions, closing, launching the next session).

## The milestone

From [MILESTONES.md](../MILESTONES.md), M9: Drum/Horn and Net/Snare gear; neutral fauna. **Done when** they are in game under Buffed. The first session (log row "M9, part 1") did D-09 and D-10 and both pieces of gear; this one does the fauna and then ticks the M9 row.

In practice:

1. **Design is written:** [docs/design/fauna.md](../design/fauna.md) (crabs on beaches; monkeys that steal one resource from a passing carrier; boars on tropical and wolves on northern islands that attack a lone peon in deep forest). Settle its "Open for the second session" items, change numbers if building it shows they are wrong, and log every choice.
2. **Neutral, not a player's:** the design makes animals world objects of their own, like the chickens (`RubberSupply`, `RubberGroup`, `RubberSupplyManager`), so they never count as anyone's units, never keep a player alive and no AI commands them. A boar's or wolf's strike needs a way to kill a peon without an attacking `Player` (`Unit.hit` takes one and credits a kill): add one that credits nobody.
3. **Buffed only, deterministic:** a `features.fauna` flag (false in `resurrected.json`, true in `buffed.json`), numbers under a `fauna` object (R-14's layout). The island generator runs for every ruleset: place the animals after it, from the world's random numbers, only when the flag is on, and check that the Classic and Resurrected golden traces do not move (compare under the old `SIM_VERSION` first, then bump and regenerate).
4. **Targets:** warriors and towers attack animals at a new lowest priority appended to `AttackScanFilter.Priority` (only the order of values matters; M9 part 1 added `DRUMMER` and moved `SHIP` to 6, R-44).
5. **Art** by a background agent: new skeletons with idle, run and attack (about 300 triangles): `misc/crab`, `natives/monkey`, `natives/boar`, `vikings/wolf`, and a visitor in `RenderState` to draw them. Give it the exact files, `BLENDER_USER_RESOURCES` in the scratchpad, and say it must not change existing sprites, `icons.xml` or existing pixels of `icons.png`. Animals need no icons unless a panel shows them (they cannot be selected).
6. **Tests and checks:** `RulesetTest` (the flag and the numbers), a headless behaviour test in the style of `BuffedDrumNetTest` (placement only under Buffed and away from starts; a crab flees; a monkey takes a passing carrier's load; a boar attacks a lone peon and not a pair; warriors kill animals; nobody is credited), the census (peons lost to animals, if the AI needs a rule), `SIM_VERSION` bump with only the Buffed traces moving, an in-game look with `drive_game.py`, a Josh check.
7. Close M9: tick its row, a log row "M9, part 2", and write the M10 prompt.

## Read first

- [docs/design/fauna.md](../design/fauna.md), PLAN.md §4.4 ("Neutral fauna"), [new-content-checklist.md](../new-content-checklist.md) (M9 added a section on units that do not fight, auras and world objects).
- `model/RubberSupply.java`, `model/RubberGroup.java`, `model/RubberSupplyManager.java`, `model/SupplyModel.java`, `model/Snare.java` (M9's world object: a `SceneryModel` with an `Animated` trap), `render/RenderState.java` (`visitRubberSupply`), `model/AttackScanFilter.java`, `landscape/World.java` and the generator code that places supplies (`SupplyManagers`, `landscape/`).
- M9 part 1's commit as the latest worked example.

## Context from M9 part 1

- **Classic invariance held** with three kinds of change worth knowing: world lists read empty first (`World.getDrummers`, `getSnares`), controller keys that gained a field (`IdleController`, `GatherController`: the AI's groups partition the same in Classic, and `Player.groupByKey` keeps first-seen order), and enum values appended at the end (`Action.SNARE`, `DeployType.DRUM_WARRIOR` / `NET_WARRIOR`, `AttackScanFilter.Priority.DRUMMER`). The golden traces proved it.
- **Match lengths:** with the drums and nets, Buffed AI matches take about 46 minutes (1v1) and 43 (six tribes; seed 9 since R-43). Buffed six-tribe matches stall on about half of all seeds (Easy AIs at the unit cap; logged in testing.md "Known gaps" for M14): if a change leaves `buffed-6p` undecided, pick another seed that finishes (R-43) and say so.
- **Traps:** in Git Bash, `python` is a Windows Store alias that waits on stdin and hangs the call: run Python only from PowerShell (`python -I script.py`). PowerShell 5.1's `Set-Content -Encoding utf8` writes a BOM: strip it or use Git Bash for text edits. `spotlessApply` writes CRLF, so `perl` patterns must allow `\r?$`; use the Edit tool for multi-line Java edits. `Map.of` takes at most ten pairs (`BuffedStringsTest` uses `Map.ofEntries` now). The M9 strings helper (scratchpad, not committed) wrote raw characters through a function that escapes them as `\uXXXX`; rewrite it, and never type a `\u` escape into a tool call.
- **Placeholder sprites while the art agent works:** pointing new templates at existing sprites (the torch warrior, the rally point) let the headless tests run before the art landed; the real sprites change Buffed's traces again (their bounds), so regenerate them only after the art is in.
- **In game:** see M8's notes in its prompt (`docs/prompts/m09-drum-net-fauna.md`, "In-game"); type `/iamacheater` (with the slash; without it the line is only chat), close the chat window with its X (it keeps the keys while open), then F2 to F4, Shift+F2 to Shift+F6 spawn units under the mouse pointer (Shift+F5 a drummer, Shift+F6 a chicken catcher); in this build the action panel sits at the top right.
- Josh owes J-01 to J-07; none blocks this session.

## Banked decisions that touch this

- Final art needs an artist or a budget (MILESTONES.md "Needs an artist"): placeholders only.
- Sounds: reuse existing clips; new ones need a source (PLAN.md C6).
