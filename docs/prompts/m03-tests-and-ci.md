# M3: Tests and CI for this fork

Follow [docs/SESSION_RULES.md](../SESSION_RULES.md) (first actions, decisions, closing, launching the next session).

## The milestone

From [MILESTONES.md](../MILESTONES.md): port the restoration fork's headless AI-vs-AI match runner and full-game test; add a replay-based determinism test; replace the inherited Steam/server/translation workflows with one build-and-test workflow; tag `v0.1.0` with a Windows package. **Done when** CI is green on `revamp` and a release exists on GitHub.

In practice:

1. **Merge the determinism fix first.** A background session fixed run-to-run nondeterminism in `Player.classifyUnits` (controller keys built from identity hash codes) and `StrictMath` in Quarters reproduction, on branch `fix/deterministic-classify-units` (commit 5cd67120, branched from 4da43e68, before M2; its worktree is `.claude/worktrees/quirky-mayer-5f7d69`). Check the branch is finished (no session still working in that worktree), merge it into `revamp`, resolve conflicts with M2, and keep its `SIM_VERSION` bump.
2. **Headless match runner.** `git fetch bondolo`, then port `headless/src/main/java/com/oddlabs/tt/headless/HeadlessMatchRunner.java` and `headless/src/test/java/com/oddlabs/tt/headless/FullGameSimulationTest.java` from `bondolo/master`. Their tree has 22 modules; ours has `tt`, `common`, `assets`, `tools`, `server`. Adapt to this codebase's APIs and keep it headless (no OpenGL context). The rulesets need `RacesResources` without rendering (`RenderQueues`), so expect to split model data from GPU registration, or stub the queues.
3. **Matches to run:** seeded 1v1 and 6-player AI matches to a victory under each ruleset (Classic, Resurrected, Buffed); a 12-player Enormous match under Resurrected if CI time allows. Assert no exception, a winner, and a tick limit.
4. **Determinism:** run the same seeded match in **two separate JVM processes** and compare `World.getChecksum()` at fixed ticks. Two processes are needed because the bug fixed in step 1 only showed between runs, never inside one. Then the replay test: record an `event.log` (see [event-logs.md](../event-logs.md)), replay it, and compare checksums.
5. **CI:** `.github/workflows/` holds Resurrected's `build-deploy-server.yml`, `gradle.yml`, `promote-release.yml`, `steam-publish.yml` and `sync-translations.yml`. Replace them with one workflow: build and test on Windows and Linux for pushes to `revamp` and for pull requests, plus a Windows package (`tt:packageWindows`) attached to tag builds.
6. **Release:** tag `v0.1.0` and publish a GitHub release with the Windows package, but only as DECISIONS.md allows (D-02). Otherwise prepare everything, bank it, and continue.

## Read first

- PLAN.md §2: M4 "Simulation test harness" (the design for this milestone) and M3 "Engine convergence".
- [rulesets.md](../rulesets.md) for how a ruleset reaches the world; `RulesetTest` is the only test module so far (JUnit 6 is set up in `tt/build.gradle.kts`).
- [releasing.md](../releasing.md) and [development-workflow.md](../development-workflow.md): Resurrected's process, kept for reference. Ours is simpler: `revamp` and tagged releases.

## Traps known so far

- Pushing workflow files needs a credential with the `workflow` scope. If `git push` is refused for `.github/workflows/*`, bank it (Josh refreshes the token) and keep the rest of the work.
- The version comes from `version = "2.0"` in the root `build.gradle.kts` (Resurrected's numbering); `common` derives `BuildInfo.VERSION` from it and from git history. Pick a scheme that makes `v0.1.0` and the in-game version agree (a reversible default; log it).
- The game window is 1024x768 windowed; `tools/scripts/drive_game.py` drives it if DECISIONS.md allows (D-03).
- Python runs from the PowerShell tool only.
