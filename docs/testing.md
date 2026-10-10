# Testing

`gradlew build` compiles every module, checks the formatting and runs the tests. CI runs the same build on Windows and Linux for every push to `revamp` and every pull request ([.github/workflows/build.yml](../.github/workflows/build.yml)). The rules in [SESSION_RULES.md](SESSION_RULES.md) ask for a green build, headless matches included, before every commit.

## What the tests cover

| Test (in `tt/src/test/java/com/oddlabs/tt/`) | What it pins |
|---|---|
| `ruleset/RulesetTest` | Each ruleset's numbers, bit for bit: Resurrected against the fork point, Classic against the 2004 source. |
| `player/ClassifyUnitsTest` | AI unit grouping comes in a fixed order, independent of hash codes. |
| `headless/FullGameSimulationTest` | AI-only matches play to a victory without an exception: a 1v1 and a six-tribe free-for-all under each ruleset, and a 6v6 on an Enormous island under Resurrected. |
| `headless/DeterminismTest` | The same seeded match gives the same checksums twice in one JVM, and in two fresh JVMs, the second with every identity hash code equal (`-XX:hashCode=2`). With the PR #1 fix reverted this test fails. |
| `headless/GoldenTraceTest` | Each headless match matches the checksum trace recorded in `tt/src/test/resources/com/oddlabs/tt/headless/golden-traces.txt`. |
| `headless/BuffedBuildingsTest` | Buffed's Chicken Coop and Totem (M5): only Buffed offers them; peons build the coop and stock it with chickens they catch, and finish the totem with a rock they fetch; the coop breeds one chicken per 90 s up to six, outside the wild-flock limit; totems add 0.05 hit chance to friendly units within 10 m, two at most; Normal and Hard AIs build and use both (from the matches' census), Easy ones and every AI under Classic and Resurrected never do. |
| `headless/BuffedMarketAndWallsTest` | Buffed's Market, Palisade and Gate (M7): only Buffed offers them; the Market trades 3 for 1 with the nearest Armory every 20 man-seconds (one peon every 20 s, two every 10), its work waits without goods, the player picks both sides, only peons go in; a drag lays a staircase line of segments at once, outside the building limit, and a few peons build it all; a ring of palisade keeps its owner's peon in; a gate lets its owner's and allies' units through, gives its cell back after them and stops enemies; nothing is built on a gate; torches burn walls; Normal and Hard AIs build a trading Market and a palisade line with a gate (from the census), Easy ones and every AI under Classic and Resurrected never do. |
| `headless/BuffedGearTest` | Buffed's Shield and Torch (M6): only Buffed offers them; the Armory makes them from their recipes in their labour time and deploys them; they strike at range 0, keep out of towers and the Quarters, and hand their gear back in the Armory; a torch's blow takes 6 from a building and lights a fire of 30 hit points over 15 s, started again by another blow, put out by a repair, able to bring a building down; a shield never burns anything; Normal and Hard AIs field both and set fires (from the census), Easy ones and every AI under Classic and Resurrected never do. |

The headless tests take about a minute: a match plays 15 to 60 minutes of game time in seconds.

## The headless match runner

`com.oddlabs.tt.headless.HeadlessMatchRunner` builds a world the way a Single-player skirmish does, gives every slot an AI and ticks the simulation until one team is left or a tick limit is reached. It needs no window, OpenGL context, sound device or network: with `Headless.enable()` the resource loaders still compute everything the simulation reads (sprite bounds and animation types, heights, supplies) and skip textures, vertex buffers and sounds. In this engine AIs act on the world inside the tick, so an AI-only match is a pure function of its settings and the simulation code.

Play one match from the command line (PowerShell; in Git Bash the quoted `--args` trips over the space in the project path):

```
.\gradlew.bat tt:headlessMatch --args="--ruleset buffed --size 1 --seed 11 --player 0:0:3 --player 1:1:3"
```

Options: `--ruleset classic|resurrected|buffed`, `--terrain NATIVE|VIKING`, `--size 0..3` (small to enormous), `--seed`, `--hills`/`--vegetation`/`--supplies` (0 to 1), `--max-ticks`, and one `--player team:race:difficulty` per tribe (race 0 natives, 1 vikings; difficulty 1 easy, 2 normal, 3 hard). `--quiet` drops the five-minute progress lines, and `-PheadlessJvmArgs="..."` passes JVM options. It prints the checksum trace, a `result` line with the winning team, the final tick and checksum, and a `census` line per player: the buildings it finished during the match (`Race.BUILDING_*` ids, sampled every ten seconds), whether its Chicken Coop bred, the unit types it had in the field (`Race.UNIT_*` ids) and how many fires its torches lit. The census only reads the world, so it never changes a match.

The checksum is the one multiplayer peers compare: the tick, the world's running checksum and every animation's contribution (`HeadlessMatchRunner.checksum`). It is sampled every 500 ticks (ten seconds of game time), exactly where a game computes it, because computing it flushes the animation managers' removal lists and can reorder animations.

## Golden traces and SIM_VERSION

`golden-traces.txt` records, for every match in `headless/Matches.java`, the result and the world checksum once a minute of game time, together with the `SIM_VERSION` they were recorded under. CI compares against it on Windows and Linux, so it is the "record on one system, replay on another" check, and it fails when:

- the simulation changed without a `SIM_VERSION` bump (`common/.../util/Compatibility.java`);
- a change meant for one ruleset moved another (a Buffed-only change must leave the Classic and Resurrected traces alone);
- the two systems compute the simulation differently.

After an intended simulation change, bump `SIM_VERSION` and regenerate the file:

```
gradlew tt:test --tests com.oddlabs.tt.headless.GoldenTraceTest -PupdateGoldenTraces
```

Then read the diff: only the matches of the ruleset you changed should move. Classic and Resurrected still give the same traces, because Classic differs from Resurrected only in world options the matches do not use; Buffed's have differed since M5, whose AIs build the Chicken Coop and Totem, and moved again in M6, whose AIs field shields and torches (Buffed matches now last longer: the 1v1 20 minutes instead of 13, the six-tribe match 30 instead of 24; the length swings with the dice, and a draft of M6 with other random draws took 35), and again in M7, whose AIs build Markets and palisade lines (the 1v1 22 minutes, the six-tribe match 31).

## Replaying a game

Every run of the game records an `event.log` that `--eventload` replays ([event-logs.md](event-logs.md)). Every two seconds the game logs a checksum of its event queue, and a replay compares its own with the logged one and logs `SEVERE: ... Checksum mismatch at tick N` at the first difference. Since M3 that checksum includes the world's tick and running checksum (`WorldViewer.updateChecksum`); before, it covered only the frame loop, so a replay whose simulation diverged went unnoticed.

The replay needs the full game with a window, so it is a local check, not part of CI. To run it (Windows, PowerShell):

1. `.\gradlew.bat tt:run`, start a skirmish, play a minute, quit through the menu. The output names the log: `Logging to ...\logs\<run>\event.log`.
2. `.\gradlew.bat tt:run --args="--eventload normal <that path>"` and keep hands off the mouse and keyboard: the replay drives the menus itself and quits where the recording did.
3. Search the output for `Checksum mismatch`; there should be none. The `Map code:` line and the tick in `Generating landscape at tick N` should match the recording's.

A log recorded by a build with a different simulation, or a different checksum, reports a mismatch at the first comparison after the game starts; that is expected, and a quick way to see that the comparison is live. Checked in M3 on 9 October 2026 with a one-minute Buffed skirmish against an Easy AI.

## Known gaps

- A 12-tribe free-for-all on an Enormous island (seed 5, Resurrected, all Hard) does not finish within 60 minutes: three tribes are left, two of them at the 250-unit cap, and the third still counts as alive with 0 units and 0 buildings in the progress line. That is for the AI milestone (M14) to look into, so the Enormous test plays 6v6.
- Pathfinder and island-generator unit tests (PLAN.md M4) are not written yet; the golden traces cover both indirectly, since any change to either moves the checksums.
- macOS is not in CI.
