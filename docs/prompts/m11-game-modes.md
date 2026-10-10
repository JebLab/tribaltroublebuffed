# M11: Single-player game modes

Follow [docs/SESSION_RULES.md](../SESSION_RULES.md) (first actions, decisions, closing, launching the next session).

## The milestone

From [MILESTONES.md](../MILESTONES.md), M11: King of the Hill, Treasure Hunt, Hold Out (waves) and Chicken Rush via the mode registry, playable against AI. **Done when** they are selectable in the Single-player menu with win conditions.

In practice:

1. **Design first:** `docs/design/game-modes.md` from PLAN.md §4.6 C4 (King of the Hill: hold the golden statue for 5 minutes, the statues already exist as campaign scenery; Treasure Hunt: collect N statues; Hold Out: survive waves of AI attackers; Chicken Rush: first to stock 50 chickens). Settle for each: the win and loss conditions, where the objects go on a generated island (fair to every start), what "hold" and "collect" mean in this engine (units near a point? peons carrying a statue like a supply?), timers and their HUD, and what the AI does to play it. Log every choice. Four modes are a lot for one session: if the context gets unwieldy, finish two modes end to end (design, code, AI, tests, menu) and write `m11-part2-...` for the other two (SESSION_RULES §1).
2. **Rulesets:** game modes are a world option. Classic offers only the 2004 options (memory and D-00), so the new modes belong to Buffed; decide whether Resurrected offers them (recommended: no, as with the other Buffed content) and gate them like `TerrainMenu.enforceRulesetFeatures()` does the islands, with a `features` flag if needed. Standard mode must stay exactly as it is: the golden traces for every ruleset must not move under the old `SIM_VERSION`.
3. **Code:** `gamemode/GameModeRules` (`isPlayerAlive`, `onGameStart`, options), `GameModeRegistry`, `standard/StandardModeRules`; the `GameMode` enum is in `common/.../matchmaking/GameMode.java` (append values; it travels in `WorldParameters`); the menu's `ModeAndPresetsPanel` in `form/TerrainMenu.java` (`modeChosen`); victory and defeat through `trigger/GameOverTrigger`, `Player.isAlive`; the campaign's trigger toolkit in `trigger/campaign/` (victory, defeat, near-point, near-army, time, reinforcements, supply-gathered) and the campaign islands' statues (`SceneryModel` with `i18n("statue")`, `player/campaign/NativeIsland0.java`) as worked examples. Upstream designed *Protect the Chief* (#238): look at its notes for the shape of a mode, but it is not in M11's list.
4. **AI:** `player/AdvancedAI` must play each mode, at least simply (send an army to hold the hill, peons to carry statues, keep chickens in stock, defend in waves); Hold Out's waves are AI attackers the mode spawns or directs.
5. **Headless:** `HeadlessMatchConfig` has no mode yet: add one (and `--mode` to `tt:headlessMatch`), a match per mode in `headless/Matches.java` only if it finishes reliably, and a test per mode in the style of `BuffedSpellsTest` (the win condition fires; Standard unchanged; the census or a result field shows the mode's progress).
6. **Interface:** mode choice in the Single-player menu, a HUD line for the objective and timer, strings in six languages (`BuffedStringsTest`'s pattern), a mode-specific end-of-game message.
7. **Checks:** `gradlew build` green, `SIM_VERSION` bump if the simulation changes (only Buffed's traces, or none, may move), an in-game look with `drive_game.py`, a Josh check.

## Read first

- PLAN.md §4.6 (C4), [rulesets.md](../rulesets.md) (Features), [testing.md](../testing.md), [new-content-checklist.md](../new-content-checklist.md).
- The game-mode code above, `form/TerrainMenu.java`, `net/WorldStarter.java` (where `onGameStart` is called), `trigger/GameOverTrigger.java`, `player/Player.isAlive`.
- M10's commit (`fa5914ad`) as the latest worked example (a ruleset flag, a new `Action`, AI rules, census, tests, cheats, art by a background agent).

## Context from M10

- **Seeds:** with M10's spells the Buffed matches moved to `buffed-1v1` seed 9 and `buffed-6p` seed 10 (R-59); Buffed six-tribe seeds stall at the unit cap two times in three. If a change leaves one undecided, survey seeds and pick one that finishes (R-43, R-59). Run `tt:headlessMatch` from PowerShell: Git Bash breaks the `--args` quoting (`'C:\Users\Josh\Documents\Tribal' is not recognized`). A survey loop over seeds takes about 15 s a 1v1 and 40 s a six-tribe match.
- **Proving a change leaves the rest alone:** switching a Buffed feature off in `buffed.json` for one `tt:headlessMatch` run should reproduce the old golden result exactly; M10 used that to separate its spells from everything else.
- **In game** (SESSION_RULES §8): claim input first (`ListAgents`; the Roblox session "Luau execution loop issue" is on another machine and was offline, so it cannot be using this mouse). Check `GetLastInputInfo` before taking focus. Clicks that worked: main menu Single player (87, 334); OK (610, 552); race pulldown of Player 1 (457, 314), Vikings (434, 367); after `/iamacheater` the chat window's X at (466, 516). Space toggles map mode, where F5 spawns a chieftain under the pointer anywhere on the island; Space back returns the camera home and Tab jumps to the newest notification (the new chieftain). Shift+F1 charges the chieftain's spells. `drive_game.py` has no Page Up / Page Down (camera zoom). Enemy units cannot be spawned by cheats: find the AI's base in map mode (its buildings show its colour).
- **Traps** (still true): run Python only from PowerShell (`python -I`); never type a `\u` escape into a tool call (write non-ASCII through a script with `chr(92)`, as `spell_strings.py` did in M10's scratchpad); `spotlessApply` writes CRLF; Spotless reformats a multi-line `else if` condition oddly; the `ActionButtonPanel` key handler is one deeply nested `else if` chain, so add new keys as a separate block after it (M10 did).
- Josh owes J-01 to J-09; none blocks this session.

## Banked decisions that touch this

- Final art needs an artist or a budget (MILESTONES.md "Needs an artist"): placeholders only (a hill marker, statue reuse).
- Sounds: reuse existing clips; new ones need a source (PLAN.md C6).
- Multiplayer stays unmaintained (SESSION_RULES §4): the modes are for single player against AI.
