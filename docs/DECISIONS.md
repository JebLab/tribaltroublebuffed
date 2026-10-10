# Decisions

Where sessions bank what only Josh can decide, record Josh's answers, and log the reversible choices they made on their own ([SESSION_RULES.md](SESSION_RULES.md) §2–3). Newest last in each section. Josh can answer here or in chat; the next session moves the item to "Answered".

## Waiting for Josh

| Id | Decision | Recommendation | Blocks |
|---|---|---|---|
| D-01 | Ask the original developers for the same blessing to use the "Tribal Trouble" name that Resurrected has (PLAN.md §1, principle 4), or plan a rename. | Josh sends one short email before the first public release; a draft can be prepared on request. | the first public release on itch.io (M22) |
| D-05 | Blender for the content pipeline (M4): it is not installed. Either Josh installs Blender (4.2 LTS or newer), or a session may download the portable Blender zip from blender.org into `.toolchain/blender` (about 350 MB, nothing installed) and SimoGecko's XML→glTF converter from GitHub. | Allow the session to download both: portable, inside the ignored `.toolchain` folder, removable by deleting it. | M4's round trip and in-game check (the rest of M4 can go ahead) |

## Josh's checks

| Id | Milestone | Try this | Expect |
|---|---|---|---|

## Answered

| Id | Decision | Answer | Date |
|---|---|---|---|
| D-00 | What Classic means | "Classic should be just classic, no resurrected features": the 2004 numbers and only the 2004 world options. | 2026-10-09 |
| D-02 | May sessions tag versions and publish GitHub releases on JebLab/tribaltroublebuffed without asking? | Yes, GitHub only. itch.io, Flathub, winget, accounts and money stay banked. | 2026-10-09 |
| D-03 | May sessions drive the game window with synthetic clicks and keys for in-game checks? | Yes, with `tools/scripts/drive_game.py`, game-window screenshots only, kept short. | 2026-10-09 |
| D-04 | Chain mode when the session chain was set up | Free: sessions chain through the queue without asking. Josh can switch to manual in `.claude/settings.local.json`. | 2026-10-09 |

## Decided by default (reversible)

| Id | Choice | Why | Date |
|---|---|---|---|
| R-01 | The skirmish menu preselects Buffed; campaign, tutorials and multiplayer play under Resurrected. | Buffed is this fork's game; the campaign was tuned for those numbers and the servers expect them. | 2026-10-09 |
| R-02 | Classic keeps interface improvements (rebindable keys, accessibility options, control groups). | They change no rule, and removing accessibility would hurt players for no gain. | 2026-10-09 |
| R-03 | Classic does not revert engine fixes made since 2004 (listed in rulesets.md). | They are fixes, not features; any one can be reverted later if it matters. | 2026-10-09 |
| R-04 | Headless matches use a JVM-wide switch (`Headless.enable()`) that makes the resource loaders skip textures, vertex buffers and sounds, instead of splitting model data from GPU registration. | Smallest change that keeps the headless simulation identical to the game's (bounds and animation types are computed by the same code); the module split belongs to M23. | 2026-10-09 |
| R-05 | The in-game version comes from `git describe` of `v*` tags (`0.1.0` on the tag, `0.1.0-3-gabc1234` after it, `<root version>-dev` without one); the root `version` is `0.1.0` and is also jpackage's `--app-version`. | A release build then shows its tag in game; the `-API.SIM` suffix stays. | 2026-10-09 |
| R-06 | GitHub releases before 1.0 are marked pre-release. | The fork is early and Josh has not play-tested a release yet. | 2026-10-09 |
| R-07 | The cross-system replay check is a set of golden checksum traces of the headless matches, recorded with `SIM_VERSION` and compared on Windows and Linux in CI; the game's own `event.log` replay stays a local check. | The event-log replay needs a window and a recorded human session; the traces catch the same divergence plus simulation changes made without a `SIM_VERSION` bump. | 2026-10-09 |
| R-08 | CI builds and tests on Windows and Linux; the Windows package is built only for `v*` tags and manual runs; no macOS job until M22. | A push takes about five minutes with a warm Gradle cache (the first Windows run, cold, took 24); packages for other systems are M22's scope. | 2026-10-09 |
| R-09 | The Enormous-island test plays 6v6 instead of a 12-tribe free-for-all. | The free-for-all (seed 5) is still undecided after 60 minutes; that stall is for the AI milestone (M14). | 2026-10-09 |
