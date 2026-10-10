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

The headless tests take about a minute: a match plays 15 to 60 minutes of game time in seconds.

## The headless match runner

`com.oddlabs.tt.headless.HeadlessMatchRunner` builds a world the way a Single-player skirmish does, gives every slot an AI and ticks the simulation until one team is left or a tick limit is reached. It needs no window, OpenGL context, sound device or network: with `Headless.enable()` the resource loaders still compute everything the simulation reads (sprite bounds and animation types, heights, supplies) and skip textures, vertex buffers and sounds. In this engine AIs act on the world inside the tick, so an AI-only match is a pure function of its settings and the simulation code.

Play one match from the command line (PowerShell; in Git Bash the quoted `--args` trips over the space in the project path):

```
.\gradlew.bat tt:headlessMatch --args="--ruleset buffed --size 1 --seed 11 --player 0:0:3 --player 1:1:3"
```

Options: `--ruleset classic|resurrected|buffed`, `--terrain NATIVE|VIKING`, `--size 0..3` (small to enormous), `--seed`, `--hills`/`--vegetation`/`--supplies` (0 to 1), `--max-ticks`, and one `--player team:race:difficulty` per tribe (race 0 natives, 1 vikings; difficulty 1 easy, 2 normal, 3 hard). `--sample-interval <ticks>` sets how often a checksum is printed, `--quiet` drops the five-minute progress lines, and `-PheadlessJvmArgs="..."` passes JVM options. It prints the checksum trace and a `result` line with the winning team, the final tick and checksum.

The checksum is the one multiplayer peers compare: the tick, the world's running checksum and every animation's contribution (`HeadlessMatchRunner.checksum`).

## Golden traces and SIM_VERSION

`golden-traces.txt` records, for every match in `headless/Matches.java`, the result and the world checksum once a minute of game time, together with the `SIM_VERSION` they were recorded under. CI compares against it on Windows and Linux, so it is the "record on one system, replay on another" check, and it fails when:

- the simulation changed without a `SIM_VERSION` bump (`common/.../util/Compatibility.java`);
- a change meant for one ruleset moved another (a Buffed-only change must leave the Classic and Resurrected traces alone);
- the two systems compute the simulation differently.

After an intended simulation change, bump `SIM_VERSION` and regenerate the file:

```
gradlew tt:test --tests com.oddlabs.tt.headless.GoldenTraceTest -PupdateGoldenTraces
```

Then read the diff: only the matches of the ruleset you changed should move. Today all three rulesets give the same traces, because Classic and Buffed do not yet differ from Resurrected in any number the AI matches touch.

## Replaying a game

Every run of the game records an `event.log` that `--eventload` replays ([event-logs.md](event-logs.md)). During a replay the game compares its state checksum with the logged one every few seconds and logs `Checksum mismatch` at the first difference. That replay needs the full game with a window, so it is a local check, not part of CI.

## Known gaps

- A 12-tribe free-for-all on an Enormous island (seed 5, Resurrected, all Hard) does not finish within 60 minutes: three tribes are left, two of them at the 250-unit cap, and the third still counts as alive with 0 units and 0 buildings in the progress line. That is for the AI milestone (M14) to look into, so the Enormous test plays 6v6.
- Pathfinder and island-generator unit tests (PLAN.md M4) are not written yet; the golden traces cover both indirectly, since any change to either moves the checksums.
- macOS is not in CI.
