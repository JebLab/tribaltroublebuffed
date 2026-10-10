# M4: Content pipeline

Follow [docs/SESSION_RULES.md](../SESSION_RULES.md) (first actions, decisions, closing, launching the next session).

## The milestone

From [MILESTONES.md](../MILESTONES.md): a Blender add-on that exports mesh, skeleton and animations to the game's geometry XML; import the originals via the XML→glTF converter and round-trip the Native warrior; `docs/content-pipeline.md` and a new-unit checklist. **Done when** an exported warrior is byte-equivalent (after formatting) to the original and animates in game.

In practice:

1. **Learn the format.** `assets/geometry/` holds the XML sources (`geometry.xml` lists them; `natives/warrior/`, `vikings/...`). `tools/scripts/validate_assets.py` pins the element and attribute order, `format_assets.py` normalises a file, and the `assets:geometry` Gradle task converts the XML to the binary sprites the game loads. The 2004 exporters are the MAXScripts in `tools/3dsmax/` (mesh, skeleton, animation): they are the specification of what each element means (bone matrices, skin weights, `wpc`, animation `type`).
2. **The add-on** (Python, `tools/blender/`): export a selected armature and its meshes to the mesh, skeleton and animation XML files the converter reads. Keep it a single-file add-on that installs from a zip.
3. **Round trip**: import the Native warrior into Blender (SimoGecko's XML→glTF converter, PLAN.md §4.3 P1, or an importer in the same add-on if that is simpler), export it again, format both with `format_assets.py`, diff. Then build, load it in game (`drive_game.py`, D-03) and check that it animates.
4. **Docs**: `docs/content-pipeline.md` (install the add-on, rig conventions, export, validate, convert, test in game) and the new-unit / new-building checklist (PLAN.md §4.3 P2: model and LOD, team-colour mask, icon sheet entry, animation set, sounds, strings in all six languages, AI table rows, tooltip, ruleset numbers, golden-trace regeneration, test).

## Read first

- PLAN.md §4.1 (style guide), §4.3 (pipeline items P1 and P2), §4.4–4.5 for what the pipeline must later carry.
- [testing.md](../testing.md): a new unit changes the simulation, so the checklist must say to bump `SIM_VERSION` and regenerate the golden traces.

## Context from M3

- **Blender is not installed on this PC**, and downloading it (or the converter) needs Josh's permission: that is **D-05** in DECISIONS.md. If D-05 is still open, do everything that needs no Blender: the format study, the add-on code written against the `bpy` API, a pure-Python XML read/write/diff harness that the round trip will use, the docs and the checklist. Leave the round trip and the in-game check for when D-05 is answered, and say so in the log row. Do not mark M4 done without the round trip.
- `./gradlew.bat build` now runs headless AI matches and golden checksum traces (about a minute). A model change that alters sprite bounds or animation lengths changes the simulation: bounds and animation types are read by the headless world too.
- In-game version comes from `git describe` (v0.1.0 is the first tag); releases are cut by pushing a tag ([releasing.md](../releasing.md)).
- The empty folder `.claude/worktrees/quirky-mayer-5f7d69` could not be removed while an old Gradle daemon held it; delete it if it is still there.

## Banked decisions that touch this milestone

- D-05 (Blender and the converter download), above.
- Art for new units needs an artist or a budget (MILESTONES.md "Needs an artist"); M4 builds the pipeline, not new models.
