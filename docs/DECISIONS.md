# Decisions

Where sessions bank what only Josh can decide, record Josh's answers, and log the reversible choices they made on their own ([SESSION_RULES.md](SESSION_RULES.md) §2–3). Newest last in each section. Josh can answer here or in chat; the next session moves the item to "Answered".

## Waiting for Josh

| Id | Decision | Recommendation | Blocks |
|---|---|---|---|
| D-01 | Ask the original developers for the same blessing to use the "Tribal Trouble" name that Resurrected has (PLAN.md §1, principle 4), or plan a rename. | Josh sends one short email before the first public release; a draft can be prepared on request. | the first public release on itch.io (M22) |

## Josh's checks

| Id | Milestone | Try this | Expect |
|---|---|---|---|
| J-01 | M4 | `./gradlew.bat tools:blenderAddon`, then in Blender (`.toolchain/blender/blender.exe`): Edit > Preferences > Add-ons > Install from Disk..., pick `tools/build/blender/io_scene_tribaltrouble.zip`, enable it. File > Import > Tribal Trouble, pick `assets/geometry/geometry.xml` (Sprite: `natives/warrior`), press Space. Then File > Export > Tribal Trouble into an empty folder. | A textured warrior with an armature; it plays its idle animation (other actions in the Action editor); Properties > Object Data shows "Tribal Trouble animation"; the export writes 8 files and a text `tt_sprite.xml`. Tested headless in M4; the menus and dialogs were not. |
| J-02 | M5 | A Buffed skirmish against a Normal AI on a Medium island: select peons, press K (or the hut-with-chicken button) and place a Chicken Coop, then Y (the red pole) and place a Totem. Select the coop when it is finished. | Peons build the coop from 20 logs, then catch two chickens and bring them in (the coop panel counts them); about 90 s later a chicken hops out and roams by the coop, up to six. The totem takes 5 logs, then peons fetch a rock to finish it. The placeholders (a small orange hut, a red pole; a reddish house and a blue-grey stone for Vikings) read as new buildings. Do they feel worth building? |

## Answered

| Id | Decision | Answer | Date |
|---|---|---|---|
| D-00 | What Classic means | "Classic should be just classic, no resurrected features": the 2004 numbers and only the 2004 world options. | 2026-10-09 |
| D-02 | May sessions tag versions and publish GitHub releases on JebLab/tribaltroublebuffed without asking? | Yes, GitHub only. itch.io, Flathub, winget, accounts and money stay banked. | 2026-10-09 |
| D-03 | May sessions drive the game window with synthetic clicks and keys for in-game checks? | Yes, with `tools/scripts/drive_game.py`, game-window screenshots only, kept short. | 2026-10-09 |
| D-04 | Chain mode when the session chain was set up | Free: sessions chain through the queue without asking. Josh can switch to manual in `.claude/settings.local.json`. | 2026-10-09 |
| D-05 | Blender for the content pipeline (M4) | "Feel free to install blender" (in the M3 session's chat). Blender 5.2.2 LTS portable in `.toolchain/blender`, downloaded and SHA-256-checked by the M3 session. SimoGecko's XML-to-glTF converter was not part of the answer; M4 wrote its own importer instead. | 2026-10-09 |

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
| R-10 | The Blender add-on imports the game's XML itself instead of going through SimoGecko's XML-to-glTF converter. | No second download or format in between, and the importer can keep each source file for an exact re-export. | 2026-10-09 |
| R-11 | Export keeps imported numbers: values that still match the imported file within a small tolerance are written as imported, and an unchanged structure is patched in place ("Keep imported numbers", on by default). | Blender holds 32-bit floats and orthonormal bones, so recomputed numbers differ in the last digits; this makes an untouched model byte-identical (M4's done-when) and keeps Git diffs to real edits. Turning it off gives the fully computed export. | 2026-10-09 |
| R-12 | Add-on conventions: a Blender bone is the 2004 bone matrix times a fixed axis swap (X along the bone becomes Y); files are named after their object or action; models are listed most triangles first; animation name, position, wpc and type are Action properties; new files use the standard layout (IntelliJ style, 120 columns, no final newline). | Exact conversion both ways and the least to set by hand. | 2026-10-09 |
| R-13 | CI runs the add-on's plain-Python tests with Python 3.12 on Windows and Linux; the Blender round trip (`tools/blender/roundtrip.py`) stays a local check. | The runners have Python but not Blender; a 400 MB Blender download per CI run is not worth it. | 2026-10-09 |
| R-14 | A building that Classic and Resurrected lack keeps its numbers in `resurrected.json` behind a `features` flag that is false there (`chicken_coop`, `totem`); `buffed.json` turns the flag on and repeats the numbers. | The strict loader needs every field in the base; a flag per building lets a ruleset or a mod offer buildings one by one; the repeat makes Buffed's balance read in one file. | 2026-10-09 |
| R-15 | "20 wood plus 2 chickens to stock": the coop is built from wood like every building (100 hit points, 20 logs); finished, it needs 2 chickens, which peons sent to it catch and bring in by themselves; then it lets out a chicken every 90 s while fewer than 6 of its own are alive. They are ordinary chickens anyone may catch, in their own flock outside the island's limit of three wild flocks. | Keeps the plan's numbers and the rule that placing costs nothing; needs no new command or button, and the AI stocks it with a plain order to go to the coop. | 2026-10-09 |
| R-16 | "5 wood plus 1 rock" for the 30-hit-point Totem: logs build 25 hit points, then a rock, fetched by the peons, adds the last 5. | Fits both the plan's recipe and its hit points to the 5-points-per-load rule. | 2026-10-09 |
| R-17 | The Totem's aura counts for units of the totem owner's team within 10 m; the bonus is added inside the hit chance (so a tower triples it) for thrown weapons and blows, not spells; the numbers are the attacking unit's race's. | "Friendlies" in a skirmish are one's own team; spells have their own hit numbers; both races' totems have the same numbers anyway. | 2026-10-09 |
| R-18 | Footprints: the coop has the tower's placing size 3 (3 x 3 grid cells), the totem size 2 (one cell). | Size 1 occupies no grid cell in this engine; the plan's "footprint 1" means one cell. | 2026-10-09 |
| R-19 | K builds the coop and Y the totem (free in every context); their buttons form a second column left of Quarters and Armory; a selected coop shows its chicken stock. | Unused keys; one taller column would not fit a 768-pixel screen beside the chieftain's buttons. | 2026-10-09 |
| R-20 | AI: Normal and Hard build one coop (above 30 and 25 units) and send peons to stock it, then add up to 2 chicken gatherers; Totems go by the Armory, then the Quarters (Normal one above 40 units, Hard two above 30 and 60); Easy builds neither. | Easy stays the gentle opponent; Buffed AI personalities are M14's. | 2026-10-09 |
| R-21 | Placeholder textures are tinted copies at 1024² (their sources are 2048²). | About 11 MB in the repository instead of 46 MB, for art that will be replaced. | 2026-10-09 |
