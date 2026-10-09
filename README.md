# Tribal Trouble Buffed

![Tribal Trouble](./.github/images/tt_logo.png)

An independent, single-player fork of **Tribal Trouble**, the 2004 real-time strategy game by Oddlabs in which Viking raiders and island natives fight over tropical and northern islands with rocks, iron, chickens and a bit of magic.

Oddlabs released the source under the GPL v2 in 2014. The community project [Tribal Trouble: Resurrected](https://github.com/Tribal-Trouble/tribaltrouble) modernised it (Java 26, LWJGL 3, OpenGL 4.1, bigger islands, boats, accessibility options). **Buffed starts from Resurrected's code and goes its own way:** new units, buildings, spells, game modes, maps and terrains for the single-player game, all in the original's cartoon style, with the classic rules always available as a preset.

**Status (October 2026):** project set up; builds and runs on Windows; content work starts next. The full plan is in [docs/PLAN.md](docs/PLAN.md) and the milestone queue in [docs/MILESTONES.md](docs/MILESTONES.md).

## Building and running

Requirements: **JDK 26** and the bundled Gradle wrapper. Everything else (Gradle itself, libraries, asset conversion) is fetched or built on the first run, which takes a few minutes.

Windows:

```bat
set JAVA_HOME=C:\path\to\jdk-26
gradlew.bat tt:run
```

macOS / Linux:

```bash
export JAVA_HOME=/path/to/jdk-26
./gradlew tt:run
```

Getting a JDK: download Eclipse Temurin 26 from [adoptium.net](https://adoptium.net) and either install it or unzip it into `.toolchain/jdk26` inside this folder (that folder is ignored by git), then point `JAVA_HOME` at it.

Other useful tasks:

| Task | What it does |
|---|---|
| `gradlew tt:run` | build everything and start the game |
| `gradlew build` | compile all modules and run the formatter check |
| `gradlew spotlessApply` | auto-format Java code (CI rejects unformatted code) |
| `gradlew assets:geometry` / `assets:textures` | rebuild the binary model and texture assets from the sources in `assets/` |
| `gradlew tt:packageWindows` / `packageLinux` / `packageMacX86` / `packageMacArm64` | self-contained packages under `tt/build/dist/` (each only on its own OS) |
| `gradlew clean` | delete build outputs |

Settings, campaign saves and per-run logs are written next to the game in portable mode or under the OS data directory; `docs/event-logs.md` explains where, and how to replay a session with `--eventload`.

## How this differs from Resurrected

- **Single-player focus.** The multiplayer menus, matchmaking client and server modules are inherited but not maintained here, and there are no Buffed servers. Steam integration is switched off (`Globals.STEAM_ENABLED`): no app id, achievements or rich presence.
- **Rulesets.** *Classic* (the 2004 numbers), *Resurrected* (their defaults at the time of the fork) and *Buffed* (new content) will be selectable when starting a game, so the original balance is never lost.
- **New content**, in the order listed in [docs/MILESTONES.md](docs/MILESTONES.md): buildings such as the Chicken Coop and Totem, new gear-based unit types, chieftain spells, single-player game modes, a minimap, mid-game saving, new terrains and a new campaign act.

## Repository layout

| Folder | Contents |
|---|---|
| `tt` | the game client (Java sources, GUI data, translations) |
| `assets` | model, texture, font and sound sources and their conversion build |
| `common` | code shared by the client and the inherited servers |
| `tools` | asset conversion tools and scripts |
| `server`, `servlet`, `database` | Resurrected's multiplayer servers, inherited and unused |
| `docs` | plan, milestones, design notes, inherited documentation |

## Credits and license

- **Oddlabs** made Tribal Trouble (2001–2007): 3D art by Chaz Willets; audio by Michael Huang, Nicklas Schmidt and Herman Witkam.
- **Tribal Trouble: Resurrected** (Ryan Linehan, Omar Mokhtar and contributors) modernised the game and is the base of this fork; their README is preserved in [docs/RESURRECTED_README.md](docs/RESURRECTED_README.md).
- **The restoration fork** by Mike Duigou (bondolo) did much of the engine modernisation and accessibility work that Resurrected builds on.

This project is licensed under the **GNU General Public License v2**, like the code it is derived from; see [LICENSE](LICENSE). "Tribal Trouble" is a trademark of Oddlabs ApS. This project is not affiliated with Oddlabs or with the Resurrected team.
