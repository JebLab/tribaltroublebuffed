# Milestones — one session each

How this works: each milestone is sized for one working session. A session starts by reading this file and [PLAN.md](PLAN.md), does the milestone, records the outcome in the log at the bottom, commits on `revamp` and pushes. Scope: **single-player only**; multiplayer items are parked at the end. Order after M4 is a suggestion and can be reshuffled.

Definition of done for every milestone: it builds (`gradlew build`), the game runs, the change is behind the *Buffed* ruleset or a flag where it alters gameplay, strings exist in every language file, and the log below is updated.

## Queue

| # | Milestone | Scope | Done when |
|---|---|---|---|
| M1 | **Project setup** ✅ | Fork Resurrected, re-base the local repo, plan, README, Steam switched off, build and run verified. | The game starts from `gradlew tt:run` on this PC. |
| M2 | **Rulesets and data-driven stats** | Move unit, building, weapon and spell numbers out of `RacesResources.java` into data files; add Classic / Resurrected / Buffed rulesets to the preset library and the Single-player menu. | A skirmish can be started under each ruleset; Classic reproduces the 2004 numbers exactly. |
| M3 | **Tests and CI for this fork** | Port the restoration fork's headless AI-vs-AI match runner and full-game test; add a replay-based determinism test; replace the inherited Steam/server/translation workflows with one build-and-test workflow; tag `v0.1.0` with a Windows package. | CI is green on `revamp`; a release exists on GitHub. |
| M4 | **Content pipeline** | Blender add-on that exports mesh, skeleton and animations to the game's geometry XML; import the originals via the XML→glTF converter and round-trip the Native warrior; `docs/content-pipeline.md` and a new-unit checklist. | An exported warrior is byte-equivalent (after formatting) to the original and animates in game. |
| M5 | **Chicken Coop and Totem** | First two buildings: build stages, HP, recipes, behaviour (chicken spawning; hit-chance aura), icons, strings, AI build rules; placeholder models (recoloured existing ones) until art exists. | Both are buildable under Buffed; AI builds them; Classic unaffected. |
| M6 | **Shield and Torch gear** | Two Armory recipes and the units they create; burning-building mechanic; AI deploy rules. | Units deploy from the Armory and behave per `docs/design/`. |
| M7 | **Market and Palisade/Gate** | Resource conversion building; wall segments and gates with pathfinding support. | Walls block, gates admit allies, the Market converts. |
| M8 | **Great Tower, Spirit Lodge / Mead Hall, Champion** | Three-thrower tower; the Lodge (shelter, spell charge bonus, trains the Champion); the Champion unit. | All buildable; Champion capped at five. |
| M9 | **Drum/Horn and Net/Snare gear; neutral fauna** | Support aura unit; chicken-catching and snares; crabs, monkeys, boars/wolves. | In game under Buffed. |
| M10 | **New chieftain spells** | Two per race in a third slot; campaign-style unlock flag. | Castable; AI uses them on Hard. |
| M11 | **Single-player game modes** | King of the Hill, Treasure Hunt, Hold Out (waves) and Chicken Rush via the mode registry, playable against AI. | Selectable in the Single-player menu with win conditions. |
| M12 | **Minimap** | Real minimap with pings, alerts and click-to-move; replaces nothing (map mode stays). | Usable at Enormous size. |
| M13 | **Save and load mid-game** | Event-log based save (map code + log + settings), fast-forward load, save slots in the pause menu. | A saved skirmish resumes identically. |
| M14 | **AI** | AI uses every Buffed unit and building; Rusher / Turtle / Economist personalities; a Relaxed level below Easy. | Headless AI-vs-AI matches under Buffed finish without stalls. |
| M15 | **Volcanic terrain** | Generator recipe, two tree types, plants, ambience, music hook. | Selectable in Single player. |
| M16 | **Challenge islands** | 20 standalone scenarios with timers and local best scores. | Playable from a new menu entry. |
| M17 | **Campaign Act III: The Chicken War** (2–3 sessions) | 12 islands using the new content; new-spell unlocks; narrative. | Playable start to finish. |
| M18 | **Accessibility: hearing, vision, motor** | Self-voicing menus and chat; event feed; reduced-flash option; click-to-drag selection; edge-scroll dead zone; auto-pause. | Options present and working. |
| M19 | **Accessibility: cognition and onboarding** | Tribalpedia, tutorial refresh with objectives panel, font-size tiers, dyslexia-friendly font. | Options present and working. |
| M20 | **Gamepad** | Full controller scheme (cursor, radial menu, camera) with 16:10 layout checks. | A skirmish is playable without mouse or keyboard. |
| M21 | **Display and QoL polish** | Desktop-resolution default, borderless, ultrawide menu art handling, select-all hotkeys, triple-click, zoom range. | Verified at 2560×1440 and 21:9. |
| M22 | **Packaging and distribution** | Windows/macOS/Linux packages from CI, itch.io page, checksums, Flathub/winget manifests. | Downloads for all three platforms on the release page. |
| M23 | **Engine convergence I** | Port the restoration fork's module split and null-safety tooling. | Builds green; tests pass. |
| M24 | **Engine convergence II** | Port its rendering performance work; profile 12 AI on an Enormous island. | Measurable frame-time improvement. |
| M25 | **Mod support** | Mod folders overriding data files, assets and strings. | A sample mod changes a unit's stats and texture. |
| M26 | **Replays and photo mode** | In-game replay browser; timelapse/GIF export. | Replays listed and playable in game. |
| M27 | **Mangrove terrain and seasonal variants** | Second new terrain; holiday model variants by date. | Selectable in Single player. |

### Parked (multiplayer, out of scope for now)

LAN/direct-IP play, a self-hosted server, spectators, tournaments, co-op campaign, Steam P2P, Steam release. The inherited multiplayer code stays in the tree but is not maintained.

### Needs an artist or a budget

Remastered 16:9 menu and campaign art (C7), new music tracks (C6), final models for the new units and buildings (placeholders are used until then).

## Log

| Date | Milestone | Outcome |
|---|---|---|
| 2026-10-09 | M1 | Forked Resurrected (`JebLab/tribaltroublebuffed`); local repo re-based (`main` mirrors upstream, `revamp` is ours, old Ant work on `legacy/ant-java21`); JDK 26 in `.toolchain/`; compiled in 2 min 21 s and ran; `docs/PLAN.md` written; README replaced; Steam switched off. |
