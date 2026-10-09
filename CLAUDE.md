# Tribal Trouble Buffed

A single-player fork of Tribal Trouble Resurrected (Java 26, Gradle, LWJGL). The work runs one milestone per session from [docs/MILESTONES.md](docs/MILESTONES.md); sessions chain themselves.

**Start every session with [docs/SESSION_RULES.md](docs/SESSION_RULES.md)**: first actions, decision policy, closing steps, and how to start the next session. Background: [docs/PLAN.md](docs/PLAN.md); open decisions: [docs/DECISIONS.md](docs/DECISIONS.md); rulesets: [docs/rulesets.md](docs/rulesets.md).

## Working here

- JDK 26 lives in `.toolchain/jdk26`. In Git Bash: `export JAVA_HOME="$(cygpath -w "$PWD/.toolchain/jdk26")"` then `./gradlew.bat build` (compiles, runs the tests and the Spotless check; `spotlessApply` formats) or `./gradlew.bat tt:run` (the game, windowed, stdout in the Gradle output).
- Python 3.13 is on PATH in PowerShell, not in Git Bash. Run `tools/scripts/*.py` from the PowerShell tool.
- Tool inputs decode `\u` escapes, so a `.properties` escape typed directly becomes the raw character. Write a literal backslash (e.g. `B=$(printf '\x5c')` in Bash), and check that `.properties` files stay ASCII.
- `core.autocrlf` is on: files may be LF in the working copy after `sed -i`; Git normalises them, so diffs stay clean.
- Never run `cat > file` without input in Bash; it waits on stdin and hangs the call.
