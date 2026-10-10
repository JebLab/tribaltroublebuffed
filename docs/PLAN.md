# Tribal Trouble Revamp — Project Plan

*Written 9 October 2026. Status: adopted the same day — the base decision in section 1 is made; the fork is [JebLab/tribaltroublebuffed](https://github.com/JebLab/tribaltroublebuffed). Scope: **single-player only**; multiplayer items are parked. Work is done one milestone per session, in the order kept in [MILESTONES.md](MILESTONES.md).*

This plan has three parts, as requested:

1. **Part 1** — bring the game into the modern era with the same content.
2. **Part 2** — make it fully free and accessible.
3. **Part 3** — new units and buildings in the original style, more content, larger maps, and other ideas.

It is grounded in three things: a line-by-line survey of the original Oddlabs source (units, buildings, rules, engine limits, assets), a review of every active fork on GitHub, and the code of the two forks that matter. Where an item is already done by someone else, the plan says so, because the single most important finding is this:

> **The game is no longer abandoned.** A community revival, *Tribal Trouble: Resurrected*, launches free on Steam on **15 October 2026** with working multiplayer, 12-player games, 2048 m islands, boats, and most of the modernization and accessibility work this project set out to do. A second fork, the *restoration fork*, has rebuilt the engine on OpenGL 4.1 with a test suite. Both are active daily.

Everything below is organised around one principle: **take their code as the base of an independent fork, and put our effort where it adds the most — new content, missing engineering, and the gaps in free/accessible play.** Josh's decision (9 October 2026): this project does not coordinate with, or contribute to, the Resurrected team. It uses their GPL-2 code, merges their public changes when useful, and ships its own builds as **Tribal Trouble Buffed**.

Effort sizes used throughout (one experienced developer, rough): **S** = days, **M** = 1–3 weeks, **L** = 1–2 months, **XL** = a quarter or more.

---

## 0. The landscape in October 2026

| | Oddlabs original ([sunenielsen/tribaltrouble](https://github.com/sunenielsen/tribaltrouble)) | Restoration fork ([bondolo/tribaltrouble](https://github.com/bondolo/tribaltrouble)) | Tribal Trouble: Resurrected ([Tribal-Trouble/tribaltrouble](https://github.com/Tribal-Trouble/tribaltrouble)) |
|---|---|---|---|
| Last activity | January 2015 | 9 Oct 2026 (daily commits) | 29 Sep 2026 on `main`; Steam launch 15 Oct 2026 |
| Commits | 7 | 914 | 988 (9 contributors; the restoration fork's author is the 2nd biggest) |
| Build | Ant, Java 1.4-era source | Gradle 9.7, **Java 27**, 22 modules, ErrorProne + NullAway | Gradle, **Java 26**, 5 modules, Spotless formatting, CI per platform |
| Graphics | LWJGL 2 (2006), OpenGL 1.3 fixed-function, 32-bit natives | LWJGL 3.4.3, **OpenGL 4.1 core profile** (shaders, UBOs, FBOs), Basis Universal textures | LWJGL 3.4.1, OpenGL 4.1 core (inherited from the restoration fork, then diverged in April 2026) |
| Tests | none | **51 test files**, including a headless AI-vs-AI full-match runner | **none** |
| Multiplayer | servers dead since the 2010s | server + services modules (Micronaut, SQLite, Flyway), not run publicly | live matchmaker + router (MySQL), Discord bot, OpenSkill leaderboard, web spectating, in-game spectators |
| Content | 2 races · 5 unit types · 3 buildings · 23 campaign islands · 6 tutorials · 3 island sizes · 6 players | identical to original (restoration only) | **+ boats**, Enormous (2048 m) and Archipelago islands, **12 players**, advanced game settings and presets, campaign difficulty, achievements, 7 languages |
| Free / accessible | demo limits and registration | registration, demo and updater removed; accessibility suite; open fonts (Inter) | same, plus free on Steam, itch.io, GitHub and tribaltrouble.org; "with the original developers' blessing" (Steam page) |
| Packaging | 2004 installers | source only | Windows, macOS x86 + Apple Silicon (jpackage), Linux AppImage, Steam, itch |

Relationship between the two live forks: Resurrected branched from the restoration fork and is now 540 commits behind it and 614 ahead; syncing is tracked as their issue #166. The restoration fork deliberately stays "pure" (no multiplayer, no new features); Resurrected is the product.

What our own Milestone 0 achieved (Java 21 + LWJGL 2.9.3 on the 2015 code, `tt.cmd`) is superseded by both. It was a day's work and taught us the codebase; keep it on a `legacy/ant-java21` branch for reference and move on.

---

## 1. The decision: which base to build on

**Decided 9 October 2026: an independent fork of Resurrected — *Tribal Trouble Buffed*, at `JebLab/tribaltroublebuffed`.**

- Resurrected's code is the base (GPL-2 allows it; attribution stays). `main` mirrors their `main` so their public fixes can be merged into `revamp` while the gap is small; everything of ours lives on `revamp` and in our own releases.
- No upstream pull requests, no Discord, nothing asked of the current maintainers. Design discussion happens in this repository's issues and `docs/design/`.
- The restoration fork remains the engine reference: when we touch rendering or structure, we port from its public repository rather than invent.

Consequences that shape the rest of the plan:

- Their matchmaking servers, Discord bot and Steam app are theirs, and a modified client would be refused by their server anyway (it checks `API_VERSION` and `SIM_VERSION`). **Buffed is a single-player project**: the inherited multiplayer code stays in the tree unmaintained, and LAN/direct-IP play and a self-hosted server are parked (M5, F2).
- Our builds ship only through GitHub Releases: the fork is for private use (Josh, 9 October 2026; DECISIONS.md D-01, D-06). Steam integration is switched off in the code (`Globals.STEAM_ENABLED = false`).
- Their Google-Sheet translation workflow is theirs; we maintain the `.properties` files in-repo.

Alternatives considered: contributing upstream (rejected — Josh prefers an independent project), and continuing from the 2015 code (two years and ~1,900 commits behind; not sensible).

Principles that apply to everything below:

1. **Classic is sacred.** Every new unit, building, spell, mode or rule sits behind a flag and a *ruleset* preset chosen when starting a game: **Classic** (2004 numbers), **Resurrected** (their defaults as of our base), **Buffed** (our content).
2. **Determinism is law.** The simulation is lockstep: every client runs the same world and compares checksums (Adler32 every 500 ticks). Any gameplay change must pass the headless simulation tests (Part 1, M4) before it merges.
3. **Match the style.** Section 4.1 is a style guide derived from the original assets. New art and names should be indistinguishable in spirit from Oddlabs' work.
4. **License clean.** Code GPL-2; new art and audio under CC-BY-SA 4.0 (or GPL-2) with attribution; no proprietary fonts or stock sounds of unknown origin. "Tribal Trouble" is an Oddlabs trademark that Resurrected uses with the original developers' blessing; *Tribal Trouble Buffed* is for private use, so it does not ask (D-01); a public release would need that email or a rename first. Credits to Oddlabs and to the Resurrected and restoration forks stay in the game.
5. **AI first-class.** No unit or building ships until the three AI difficulties know how to build and use it (the AI is table-driven per difficulty in `AdvancedAI`; extend the tables).
6. **Design before code.** Each content item gets a one-page design note in `docs/design/` before implementation.

---

## 2. Part 1 — Into the modern era, same content

Status key: ✅ done upstream · 🟡 partly done or in progress upstream · ⬜ open (our work).

| # | Milestone | Status | Our work | Effort |
|---|---|---|---|---|
| M1 | **Re-base and developer setup** | ✅ 9 Oct 2026 | Done: fork, local re-base (`main` mirrors upstream, `revamp` is ours, the Ant work is on `legacy/ant-java21`), portable JDK 26 in `.toolchain/`, build and run verified (2 min 21 s to compile). Next: Gradle toolchain auto-provisioning (foojay resolver) so nobody installs a JDK by hand; our own README and project identity; disable the Steam app-id hook. | S |
| M2 | **Modern runtime and display** | ✅ | Java 26, LWJGL 3.4.1 (GLFW window, OpenAL, STB, TinyFD), OpenGL 4.1 core context, jpackage/jlink/AppImage packaging, CI builds on every push, Windows fullscreen-behind-taskbar fix (#174), macOS display fix (#173), HiDPI cursor fix, hardware cursor restored (v1.0.0). Remaining for us: a **verification pass** on common hardware (2560×1440 at 120 Hz, ultrawide 21:9, 4K at 150 % scaling, 1280×800 Steam Deck, Intel iGPU) and bug reports. Note the menu and campaign art is still 800×600 paintings stretched to the window; aspect-correct (pillarboxed or repainted 16:9) menu backgrounds belong to Part 3 C7. | S (verify) |
| M3 | **Engine convergence with the restoration fork** | 🟡 (#166) | Resurrected is 540 commits behind the fork it was built on: the 22-module split (simulation / engine / gui / net / …), ErrorProne + NullAway null-safety, the October 2026 rendering performance work (early-Z sorted foliage, particle shaders, global UBO, scissor clipping, GPU timing), Basis Universal textures, and 51 tests. Port in slices by subsystem from the restoration fork's public repository. This is the highest-leverage engineering item: it unblocks M4 and M6. | L, ongoing |
| M4 | **Simulation test harness** | 🟡 ours since M3, 9 Oct 2026 ([testing.md](testing.md)); pathfinder and generator unit tests open | Port `HeadlessMatchRunner` / `FullGameSimulationTest` (runs AI-vs-AI matches to victory, with simulated network jitter) into Resurrected. CI then runs seeded 1v1, 6-player and 12-player-Enormous matches per PR with no exception or desync. Add replay-based determinism checks: record an `event.log` on one OS, replay on another in CI, compare world checksums. Add unit tests for the pathfinder and the island generator (seed → identical heightmap hash on every platform). This is the safety net for every `SIM_VERSION` bump in Part 3. | M |
| M5 | **Play without a server** | ⏸ parked (multiplayer is out of scope) | For the record: a joiner needs a matchmaker tunnel today, and the lobby server refuses non-loopback sockets, even though the relay ("router") is already embedded in every client for single-player. If multiplayer ever returns, **Host LAN / Direct-IP game** is the design: lobby on a real interface, host runs the embedded router, joiners enter `ip:port`, UDP broadcast for LAN discovery. | M |
| M6 | **Scale and performance** | 🟡 | The original was designed for 6 × 250 units on 1024 m islands; Resurrected allows 12 × 250 on 2048 m (an "Enormous island crashing at start" bug was fixed in #250). Known ceilings in the inherited code: far-tree billboards share one 16-bit index buffer (65,535 vertices); grid A* is capped to a 128×128 window and 600 nodes, with 1024 cost buckets that lose ordering on long paths; the animation manager registers entities in linear lists; region building allocates one node per 2 m cell (one million at 2048 m); terrain colour maps are re-baked on every load (256 chunks of 512² at 2048 m). Work: profiling suite (headless runs + the restoration fork's GPU timers), hierarchical pathfinding with larger windows and flow fields for groups, spatial hashing for target scans, batched draws, cached colour maps per map seed. | M–L, after M3/M4 |
| M7 | **Controls and quality of life** (no rule changes) | 🟡 | Rebindable keys, keybinds in tooltips and Ctrl+1–9 control groups exist. Open upstream requests we can take: select-all peons / army hotkeys (#194), triple-click to select a type (#223), extra mouse buttons as keys (#71), click-and-drag selection preference (#66), zoom out further (#27), camera on hilly maps (#192), idle-peon button, edge-scroll speed. | S each |
| M8 | **Documentation and onboarding** | ⬜ | Server hosting guide (#13), keybinds tutorial (#8), an `ARCHITECTURE.md` (neither fork has one; our survey is the draft), and the content-pipeline guide from Part 3. | S–M |

---

## 3. Part 2 — Free and accessible

"Accessible" is used in both senses: *easy for anyone to get and run*, and *playable by people with disabilities*.

### 3.1 Free and easy to get

Already done upstream: registration, demo mode and the SVN updater removed; the Microsoft Tahoma/Impact bitmap fonts replaced by Inter (open font); free on Steam (15 Oct), itch.io, GitHub Releases and tribaltrouble.org; a media-license review (their #198) closed; campaign-save migration to Steam documented.

| # | Item | Status | Our work | Effort |
|---|---|---|---|---|
| F1 | **Non-Steam distribution parity** | ⬜ | Flathub (from the existing AppImage), winget, Homebrew cask, AUR; signed checksums on releases (minisign/Sigstore) because builds are unsigned today (the README walks users through OS warnings); an auto-update check for GitHub/itch builds. Code-signing certificates (Windows OV/EV, Apple notarisation) cost money — a maintainer decision, documented with prices. | S each |
| F2 | **Self-host-friendly server** | ⏸ parked (multiplayer is out of scope) | For the record: the live server needs MySQL, two processes and manual setup; the restoration fork has a Micronaut + SQLite + Flyway services module that would be the base for a one-jar server. Pairs with M5. | M |
| F3 | **Preservation and license hygiene** | ⬜ | Archive the 2004 installers and manual; record asset provenance (3D art Chaz Willets; audio Michael Huang, Nicklas Schmidt, Herman Witkam; music untagged); a written policy for new assets (CC-BY-SA 4.0); confirm the 2015 leftovers (hard-coded database passwords, a private-key passphrase in a servlet descriptor, a Vim swap file) are gone from the live forks — the credentials are already externalised to `server.properties`. | S |
| F4 | **Credits and about screen** (#141) | ⬜ | Oddlabs, the original artists, and every community contributor, in-game. | S |

### 3.2 Accessible to play

Already done upstream (from the restoration fork's `ACCESSIBILITY.md`, inherited by Resurrected): UI scaling (`ui_scale`), high-contrast mode with contrast/brightness/clarity sliders, colour inversion, real-time colour-vision-deficiency filters (protan/deutan/tritan, adjustable), editable team colours, team *stencils* (silhouette patterns so allegiance never depends on colour alone), fully rebindable keys, keybinds in tooltips, independent master/SFX/music volumes, audio cues for chat and alerts.

Their own list of known barriers: no screen-reader support (the GUI is a custom OpenGL canvas), no battlefield narration, and mouse capture during edge-scrolling fights third-party magnifiers. The Steam page carries no controller tag.

| # | Item | Status | Our work | Effort |
|---|---|---|---|---|
| A1 | **Self-voicing menus and chat** | ⬜ | Speak the focused widget's label and value, chat lines and alerts through the OS speech engine (Windows SAPI, macOS `say`, Linux speech-dispatcher); enforce a logical focus order and full keyboard operability (their definition-of-done already requires it for new UI). Practical screen-reader support without an accessibility tree. | M |
| A2 | **Gamepad and Steam Deck** | ⬜ (restoration fork lists it as "in development"; no gamepad code in either fork) | GLFW gamepad API: stick-driven cursor, radial command menu, camera on the right stick, hold-to-modify for control groups; Steam Input config; Deck checks (1280×800 layout, default UI scale, Steam overlay keyboard for chat); aim for "Deck Verified". Also enables one-handed and keyboard-only schemes. | L |
| A3 | **Battlefield cues for deaf, blind and low-vision players** | 🟡 | Directional attack alerts with screen-edge and minimap indicators; a text event feed ("Armory under attack, north-east"); optional spoken event summaries; reduced-flash option for the Crackling Cloud lightning and any screen shake (photosensitivity). | M |
| A4 | **Motor accessibility** | ⬜ | Click-to-start / click-to-end drag selection (no holding), adjustable double- and triple-click timing, edge-scroll dead zone and off switch (fixes the magnifier conflict), auto-pause when a dialog opens in single-player, a **Relaxed** AI below Easy. | S–M |
| A5 | **Cognitive load and onboarding** | ⬜ | Tutorial refresh with a persistent objectives panel, optional hints, an in-game encyclopedia ("Tribalpedia") with unit/building/spell cards, text labels beside every icon, font-size tiers, a dyslexia-friendly font option (Atkinson Hyperlegible, open). | M |
| A6 | **Localisation** | 🟡 (7 languages inherited; Polish hidden) | Their translation sheet is theirs, so we keep the `.properties` files in-repo with a small CSV export/import script for translators. Add a Cyrillic/CJK fallback font (Noto Sans) so Russian, Chinese, Japanese and Korean can be added; every Part 3 string ships in all languages. Right-to-left scripts only if the GUI toolkit gains mirroring (bigger job). | M |
| A7 | **Verification, continuously** | ⬜ | Adopt the restoration fork's definition of done as a change checklist; recruit playtesters with disabilities through accessibility-gaming communities (e.g. the AbleGamers and Can I Play That? communities); publish an accessibility statement with each release. | S, recurring |

---

## 4. Part 3 — New content

### 4.1 Style guide (what "closely matching" means, in numbers)

From the original assets and code:

- **Units** are ~500 triangles with an ~80-triangle low-detail version; chieftains ~800/100; **buildings** ~2,000/200 in three stages (site, half-built, built). Textures are hand-painted: 256² for peons and warriors, 512² for chieftains and buildings, each with a half-resolution **team decal mask** (team colour is blended in, never tinted over the whole model).
- Every unit has exactly the animations **idle, run, attack, die** (chieftains add **magic**; the Viking chieftain also has a **thor** pose). Buildings animate while producing. Playback is frame-stepped, no blending, and the simulation's random generator even drives particle effects — effects are part of the deterministic world.
- Warriors are the **peon model holding gear**: the three weapon tiers are texture variants plus a prop mesh on one rig. Resource-carrying peons reuse the same rig with a carried fragment. This is the cheapest possible way to add unit types, and it is how we will add ours.
- Sound: 2–6 short mono Ogg clips per action (hits, throws, deaths, chickens), two race-specific alert jingles, three music tracks. No voice acting.
- Tone: cartoon proportions, no gore, chickens as the running joke, alliterative spell names (*Terrifying Toot*, *Ravaging Roar*, *Stinking Stew*, *Crackling Cloud*), pseudo-Bantu island names for the Viking campaign and pseudo-Icelandic ones for the Native campaign.
- Icons live on a single 1024² sheet indexed by `icons.xml`; every unit, building, action and spell has one.

### 4.2 The rules new content must obey (how the game actually works)

- **A unit is a peon plus gear.** Peons are born free in the Quarters (rate depends on how many peons are inside). A warrior is one peon plus one weapon deployed from the Armory; weapons are crafted there from wood + rock / iron / chicken by peons working inside; a warrior who walks back in returns the weapon to stock. There is no research or upgrade tree — the weapon tier *is* the progression.
- **Every unit has 1 HP.** Any hit kills. Combat is a roll: `hit chance × (1 − target's dodge)` with a terrain-height bonus and ×3 from a tower. Rock / iron / chicken weapons have 0.5 / 0.75 / 0.95 hit chance and give the wearer 0.5 / 0.7 / 0.7 dodge; chicken weapons bounce to a second target. Only chieftains (40–60 HP) and buildings (100–200 HP) take damage over time.
- **Buildings cost nothing to place and are built by peons carrying wood** (5 HP per log). Quarters and Armory 200 HP, Tower 100 HP (holds one thrower, +8 range, triple hit chance). Limits: 20 buildings, 250 units per player.
- **Chieftain:** one per player, two spells per race charged by time (40 s and 70 s), a cast resets both. No mana.
- **The races mirror each other** except chieftain HP, spells, weapon bounce speed and animation timing.

Everything in 4.4–4.6 fits these rules on purpose.

### 4.3 Content pipeline (prerequisite for everything else)

| # | Item | Why | Effort |
|---|---|---|---|
| P1 | **Blender → geometry XML exporter** (a Blender add-on) ✅ M4 ([content-pipeline.md](content-pipeline.md)) | The only exporters are 2004 MAXScripts for 3ds Max with Character Studio. Nobody has a Max rig today. A community member (SimoGecko) wrote an XML → glTF converter, so originals can be *imported* into Blender; we write the reverse. Round-trip tests: import a warrior, export, diff against the original. Resurrected's `validate_assets.py` / `format_assets.py` check the result; their `assets` Gradle module converts XML to the binary runtime format. | M |
| P2 | **"New unit / new building" kits** ✅ M4 (checklist: [new-content-checklist.md](new-content-checklist.md)) | A documented checklist with templates: model + LOD, decal mask, icon sheet entry, animation set, 2–6 sounds, strings in every language, AI table rows, tooltip, achievement hooks, feature flag, preset entry, test. Calibration point: Resurrected's ship commit touched 109 files (35 in `model/`, plus player, render, gui, pathfinder, landscape, resource, icons and five language bundles). Expect ~40 code files per new unit *type*; gear variants on an existing rig are far cheaper. | S (docs) |
| P3 | **Data-driven stats** (first step of mod support, O3) ✅ M2 | Today every number lives in `RacesResources.java`. Moving unit, building and spell numbers into data files makes Classic/Resurrected/Revamped presets a file swap and lets the community tune balance without recompiling. | M |
| P4 | **Flags and rulesets** ✅ M2 (rulesets; per-feature flags come with each feature) | `SHIPS_ENABLED`-style flags per feature; a lobby *ruleset* dropdown wired into the existing preset library (`presets.json`); `SIM_VERSION` bump discipline; the M4 tests gate merges. | S |

### 4.4 New units — four pieces of gear, mirrored across races

Each is a texture variant + prop on the existing peon/warrior rig, crafted in the Armory and deployed like a weapon, so it inherits the whole deploy/return/AI machinery.

| Gear (Armory recipe, labour) | Natives / Vikings | Role | Proposed numbers (Revamped ruleset) | Counter |
|---|---|---|---|---|
| **Shield** — 2 wood + 1 rock, 40 man-s ✅ M6 | Bark-Shield Bearer / Round-Shield Carl | Front line that soaks throws | dodge 0.7 (0.85 until M9, D-10), speed 3.5, melee hit 0.3 | chicken warriors (0.95 hit), tower fire (×3), spells |
| **Torch** — 2 wood + 1 rock + 1 iron, 80 ✅ M6 | Firebrand / Torchbearer | Siege: burns buildings | melee hit 0.5 vs units; vs buildings always hits for 4 (6 until M9, D-09) and sets fire: −2 HP/s for 15 s unless peons repair; dodge 0.3 | any warrior, towers |
| **Drum / Horn** — 3 wood + 1 iron, 60 ✅ M9 | Drummer / Hornblower | Support aura | no attack; +0.10 hit and +15 % speed to friendlies within 12 m (doesn't stack); dodge 0.5; auto-targeted first (priority 4: a priority of its own ahead of the warriors', R-44) | focus fire |
| **Net / Snare** — 2 wood + 1 chicken, 60 ✅ M9 | Chicken Catcher / Fowler | Utility | catches a chicken in one hit instead of ten; lays up to 3 snares that stun the first enemy for 4 s; melee hit 0.4 | cheap and fragile |

Stretch, ✅ M8: a per-race **Champion** (Headhunter / Berserker — the "fast, tanky berserker" idea from Tribal Trouble 2 that players asked for in Resurrected's #136), trained at the Lodge (4.5), capped at five alive: speed 5, dodge 0.75, melee hit 0.9, costs 2 wood + 1 iron + 1 chicken.

**Chieftain spells** — two new per race in a third slot (100 s charge), unlockable in the campaign like the originals:

| Race | Spell | Effect |
|---|---|---|
| Natives | *Jolly Jungle* | vines root every enemy in 20 m for 8 s |
| Natives | *Poultry Panic* | a 30 m chicken stampede knocks units flat for 3 s and leaves five catchable chickens |
| Vikings | *Hammer of Thor* | one bolt: 40 damage to a building or kills a unit (reuses the existing *thor* animation) |
| Vikings | *Fjord Fog* | mist, 30 m for 20 s: enemy hit chance −0.2 |

**Neutral fauna** (ambient, cheap, very on-brand): beach crabs; monkeys that steal one resource from a passing carrier; boars (tropical) and wolves (northern) that attack a lone peon in deep forest. One ~300-triangle model and three animations each.

### 4.5 New buildings — six, mirrored, three build stages each

| Building (Natives / Vikings) | Footprint · HP · recipe | Function | Why it fits |
|---|---|---|---|
| **Chicken Coop / Henhouse** ✅ M5 | 3 · 100 · 20 wood + 2 chickens to stock | spawns one chicken every 90 s, up to six roaming nearby | wild chickens are capped at three flocks per island, which starves chicken weapons on big maps; this is the premium-economy building the game lacks |
| **Palisade and Gate** ✅ M7 | 1 per segment · 40 (gate 120) · 8 wood | blocks movement; gate passable by owner and allies; built fast | territory play on 2048 m maps; countered by Torches |
| **Great Tower** ✅ M8 | 5 · 300 · 40 wood + 10 rock (built as 50 logs + 10 rocks, R-35) | three throwers, +8 range, triple hit | the first building with a non-wood cost; a late-game anchor |
| **Spirit Lodge / Mead Hall** ✅ M8 | 5 · 200 · 40 wood + 5 iron (built as 35 logs + 5 iron, R-35) | trains the Champion; shelters 30 units; halves spell charge time within 30 m | a mid-game "tech" building without inventing a research tree |
| **Totem / Runestone** ✅ M5 | 1 · 30 · 5 wood + 1 rock | +0.05 hit for friendlies within 10 m (max two stacking) | cheap, visible, destroyable: creates skirmishes over ground |
| **Trading Post / Market** ✅ M7 | 3 · 150 · 30 wood | peons inside convert 3 of one resource into 1 of another every 20 s | solves lopsided resource spawns on Enormous and Archipelago maps |

(Ships are already a "movable building" upstream; docks and ship variants are their domain.)

### 4.6 More content

| # | Item | Status | Notes | Effort |
|---|---|---|---|---|
| C1 | **Archipelago missions** | ⬜ (Resurrected is building its own Archipelago campaign for December; ours is separate) | A first set of boat-based missions using the existing trigger toolkit (victory, defeat, near-point, near-army, spell-cast, supply-gathered, time, reinforcements) and the boats already in the code. | M |
| C2 | **Campaign Act III — "The Chicken War"** | ⬜ | 12 islands, both races playable, built around the Part 3 units and buildings; unlocks the new spells the way the originals unlock *Terrifying Toot* and *Crackling Cloud*. | L |
| C3 | **Challenge islands** | ⬜ | 20 standalone scenarios with online leaderboards (survive 15 minutes exists as a campaign objective type; add speed-kill, defend-the-statue, no-armory runs). | M |
| C4 | **Game modes (single-player, against AI)** | 🟡 (a mode registry exists with only *Standard*; *Protect the Chief* is designed upstream, #238) | King of the Hill (hold the golden statue 5 minutes — the statues already exist as campaign scenery), Treasure Hunt (collect N statues), Hold Out (waves of AI attackers), Chicken Rush (first to stock 50 chickens), Protect the Chief. | M each |
| C5 | **New terrains** | ⬜ (tropical and northern exist; the generator is a per-terrain recipe plus tree/plant sets) | Volcanic (iron-rich, sparse wood), Mangrove swamp (chicken-rich, slow shallows), Winter (snow-dressed oak and pine), Savanna (open sightlines, few trees). Each needs two tree models, plants, an ambient set and a music track. | M each |
| C6 | **Music and sound** | ⬜ | Three tracks (12 minutes) today. Commission four more in the same style under CC-BY; terrain ambience sets; barks for new units. | M (money) |
| C7 | **Remastered 2D art** | 🟡 (upstream goal 3 "remaster graphics") | Menu, loading and campaign-map paintings repainted at 16:9/4K — the current art is 800×600 stretched. | M (artist) |
| C8 | **Seasonal variants** | 🟡 (#129) | Holiday model variants chosen by date; our pipeline (P1) makes them cheap. | S each |

### 4.7 Larger maps

Status: Small 256 m, Medium 512 m, Large 1024 m, **Enormous 2048 m**, **Archipelago** (several islands, boats), 12 players, configurable unit and building caps, map codes for sharing seeds. Sizes must be powers of two (asserted in the generator).

| # | Item | Status | Notes | Effort |
|---|---|---|---|---|
| L1 | Performance at scale | see M6 | Everything else here is bottlenecked on this. | — |
| L2 | **Map editor and map browser** | 🟡 (their draft PR #281: 15 k lines; terrain brushes, resource painting, 12 spawns, shared editing, server-hosted sharing) | Once it lands in their public `main` we can merge it; otherwise build our own, smaller editor. Then curate hand-made maps and a ranked map pool. | M |
| L3 | **Minimap** | ⬜ (only a full-screen "map mode" exists) | A real minimap with pings, alerts and click-to-move; essential at 2048 m and for A3. | M |
| L4 | **Fairness and start options** | ⬜ | Mirror/symmetric generation for ranked; same-island teams and min/max island count for Archipelago (maintainer suggestions on #80). | M |
| L5 | **Continent maps** | ⬜ | 4096 m with rivers and lakes (ships on rivers, per #80), several biomes per map. Needs streaming or cached colour maps (1,024 chunks at this size) and L1. | XL |
| L6 | **Fog of war (optional ruleset)** | ⬜ | No visibility system exists; everyone sees everything. Per-team visibility in the simulation, renderer masking, AI awareness. A big change in feel, so strictly opt-in. | L |
| L7 | **Economy for distance** | — | The Market, Coop and Palisade (4.5) exist largely to make huge maps strategically interesting rather than just long. | — |

### 4.8 Other ideas

| # | Idea | Status | Notes | Effort |
|---|---|---|---|---|
| O1 | **Save and load mid-game** | ⬜ (campaign progress only) | The simulation already records a complete, deterministic `event.log` per run. A save is the map code + the log + a settings snapshot; a load is a fast-forward replay (developer time-warp keys already exist). Single-player first; multiplayer resume later (#35). | M |
| O2 | **Replay browser and share codes** | 🟡 (replays only via `--eventload` and website links) | In-game list, scrub/time-warp, spectator camera (upstream's cinematic/recording camera applies). | M |
| O3 | **Mod support** | ⬜ | Builds on P3: mod folders overriding data files, assets and strings; later Steam Workshop for maps and mods. | L |
| O4 | **Custom AI framework** | ⬜ (#98 asks for it) | AI behind an interface; ship personalities (Rusher, Turtle, Economist) plus Relaxed; AI that uses boats and all new content. | L |
| O5 | **Balance telemetry** | ⬜ | Per-unit kills/deaths and resource curves from match reports → a public dashboard beside the OpenSkill leaderboard; data-driven balance votes. | M |
| O6 | **Tournaments and seasons** | ⏸ parked (multiplayer) | Brackets and seasonal resets on a small website, if multiplayer ever returns. | S–M |
| O7 | **Tribalpedia** | ⬜ | In-game encyclopedia and lore; doubles as A5. | S–M |
| O8 | **Co-op campaign** | ⏸ parked (multiplayer) | Two humans share a team on campaign islands. | M |
| O9 | **Cosmetics** | ⬜ | Chieftain customisation (the Tribal Trouble 2 idea) and banner choices as achievement unlocks; never gameplay. | M |
| O10 | **Timelapse / GIF export** | 🟡 (cinematic and recording camera exist) | | S |
| O11 | **Speedrun timers and achievements for new content** | ⬜ | | S |

---

## 5. Sequencing

Work is done **one milestone per session**. The ordered queue, each milestone's scope and its definition of done, and the log of finished sessions live in [MILESTONES.md](MILESTONES.md). The order is: foundations first (rulesets and data-driven stats, tests and CI, the content pipeline), then content in small drops (two buildings, then gear units, then more buildings, spells, modes), then the systems that make a long single-player game pleasant (minimap, save/load, AI), then terrains, challenges and the new campaign, with accessibility, gamepad, display polish, packaging and engine convergence interleaved. Every milestone ends with a commit on `revamp`, and every few milestones with a tagged release on GitHub and itch.io.

## 6. Risks

| Risk | Mitigation |
|---|---|
| Upstream keeps moving fast (~1,000 commits in 15 months) and merging gets hard | Merge `upstream/main` into `revamp` monthly while the gap is small; once our content lands, switch to cherry-picking fixes; M3 keeps the engine aligned with the restoration fork instead. |
| Multiplayer | Out of scope by decision. The inherited multiplayer code stays unmaintained; the Multiplayer menu entry is hidden in the display-polish milestone so nobody connects to servers that would refuse them. |
| Balance upheaval alienates veterans | Classic preset untouched; Buffed is opt-in; O5 telemetry informs tuning. |
| Art bandwidth | Gear-variant design reuses rigs; P1 exporter; commission or recruit artists directly; budget for C6, C7. |
| Desyncs from gameplay changes | M4 harness runs on every change; `SIM_VERSION` discipline; replay-based cross-platform checks. |
| Name and trademark | Ask the original developers for the same blessing Resurrected has; keep Oddlabs and fork credits; be ready to rename. |
| One-person burnout | Quarterly scope cuts are allowed; every milestone ships something on its own. |

## 7. Where things stand

Milestone 1 (project setup) was completed on 9 October 2026: independent fork at github.com/JebLab/tribaltroublebuffed, local repo re-based (`main` mirrors upstream, `revamp` is ours, the Ant work lives on `legacy/ant-java21`), JDK 26 build and run verified, README replaced, Steam switched off. Milestone 2 (rulesets and data-driven stats, P3 and P4 below) was completed the same day; see [rulesets.md](rulesets.md). It found that Resurrected changed no gameplay number since 2004, only engine logic. Milestone 3 (tests and CI) followed: headless AI-vs-AI matches under every ruleset, a two-JVM determinism test, golden checksum traces replayed on Windows and Linux in one CI workflow, world state in event-log replay checksums, and the first release, v0.1.0 (Windows, pre-release); see [testing.md](testing.md). It covers the Part 1 M4 test harness except the pathfinder and island-generator unit tests. Milestone 4 (content pipeline, P1 and P2) followed: a single-file Blender add-on imports the original models and exports mesh, skeleton and animations; the Native warrior round-trips byte for byte, and with every number recomputed in Blender it still animates in game; see [content-pipeline.md](content-pipeline.md) and the [new-content checklist](new-content-checklist.md). Milestone 5 added the first two Buffed buildings from §4.5, the Chicken Coop / Henhouse and the Totem / Runestone, with placeholder models made in the pipeline, AI build rules and tests that Classic and Resurrected are untouched; see [rulesets.md](rulesets.md). Milestone 6 added the first two Buffed units from §4.4, the Shield and Torch gear, which fight hand to hand; a torch sets buildings on fire, and the AI leads its attacks with shields and sends torches at buildings; see [design/gear.md](design/gear.md). Milestone 7 added the Trading Post / Market, where peons trade 3 of one resource for 1 of another, and Palisade segments and Gates: walls laid in lines that block, and gates that let their own side through, with the smallest pathfinder change that keeps Classic's paths bit for bit; the AI builds both; see [design/market.md](design/market.md) and [design/palisade.md](design/palisade.md). Milestone 8 added the Great Tower, which three throwers man at once, and the Spirit Lodge / Mead Hall, which shelters units, halves the spell charge time of chieftains nearby and trains the Champion (Headhunter / Berserker), a fast, hard-to-hit fighter capped at five; the AI builds both and trains Champions; see [design/great-tower.md](design/great-tower.md) and [design/lodge-and-champion.md](design/lodge-and-champion.md). Milestone 9 began with Josh's weaker torches and shields (D-09, D-10) and added the last two pieces of gear from §4.4: the Drum / Horn, whose bearer never fights but makes its side nearby hit and walk better and draws every enemy's fire, and the Net, whose bearer catches chickens in one stroke and lays snares that stun the first enemy on them; the AI fields both; see [design/drum-and-net.md](design/drum-and-net.md). The next session finishes **M9** with the neutral fauna of [design/fauna.md](design/fauna.md) (crabs, monkeys, boars and wolves), from [MILESTONES.md](MILESTONES.md).
