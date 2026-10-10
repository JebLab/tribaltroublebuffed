# Content pipeline

Models for Tribal Trouble are made in Blender and exported with the add-on in [tools/blender/io_scene_tribaltrouble.py](../tools/blender/io_scene_tribaltrouble.py) to the geometry XML in `assets/geometry`, which the build converts to the binary sprites the game loads. The add-on also imports the original models, so an existing unit can be opened, studied, changed or used as the starting point of a new one. What else a new unit or building needs (icons, sounds, strings, AI, rules) is in [new-content-checklist.md](new-content-checklist.md).

Proven in M4 on the Native warrior: imported and exported again, every file came back byte for byte; exported with every number computed by Blender, it matched the original within float precision and animated in game (idle, run, die). M5 made the placeholder Chicken Coops and Totems with it: each race's Quarters and Tower stages imported, scaled down (0.43 to 0.45 and 0.35 to 0.4), exported under new names and given tinted copies of their textures (at 1024², half the originals' size; the team-colour masks are reused, since the UVs did not change). M6's Shield and Torch warriors (a background agent, scripted in Blender) are the race's warrior mesh with a prop on the forearm or hand bone, on the warrior's own skeleton and animations: a new sprite needs only new meshes, textures and team masks, and reuses the skeleton and animation files. M7's placeholders came the same way: the Market is the race's Armory scaled to the coop's footprint (0.51 and 0.55) with teal-tinted textures; the Palisade and Gate are new meshes built by a Blender script (a 3 x 3 cluster of sharpened logs that fills its cell, and four posts with crossbeams and team-coloured banners) on a new 512² texture per race. M8's too: the Great Tower is the race's Tower widened to the Quarters' footprint (x 2.3 to 3.1) and 1.2 times as tall, the Lodge needs no meshes at all (new sprite entries reuse the Quarters' files with `scale="0.9"` and tinted textures), and the Champion is the warrior with a lathe-built club or a double-bladed axe on the hand bone; re-exported, the Champions came back byte for byte. M9's wild animals (crab, monkey, boar, wolf) are the first new skeletons: a background agent built bones, a 325 to 360 triangle mesh, idle, run and attack animations with two-bone IK for planted feet, and numpy-painted 512² textures by Blender script, exported them with the add-on and re-imported them to check (R-56). Not every model round-trips byte for byte: the high-detail building meshes come back with normals off by up to 8.5e-3 (about half a degree), because Blender averages nearly identical custom normals at a vertex; low-detail meshes and the warrior come back identical.

## Set up

1. **Blender 4.2 or newer.** Tested with 5.2.2 LTS (on Josh's PC a portable copy lives in `.toolchain/blender`, which Git ignores).
2. **Build the add-on zip:** `./gradlew.bat tools:blenderAddon` writes `tools/build/blender/io_scene_tribaltrouble.zip`.
3. **Install it:** in Blender, Edit > Preferences > Add-ons, the drop-down menu at the top right > Install from Disk..., pick the zip (or the `.py` itself), and tick "Tribal Trouble geometry (XML)". It adds File > Import > Tribal Trouble (.xml) and File > Export > Tribal Trouble (.xml).

## Import

File > Import > Tribal Trouble (.xml) takes any of the game's files:

| File | What you get |
|---|---|
| `assets/geometry/geometry.xml` with *Sprite* set to `group/name` (for example `natives/warrior`) | The whole sprite: an armature named after the skeleton file, a mesh object per model (named after its file, textured from `assets/textures/models`), and an action per animation, with its name, position, distance per cycle and type. |
| a skeleton file | An armature. |
| a mesh file | A mesh object, bound to the active armature if one is selected. |
| an animation file | An action on the active armature (which must come from a skeleton import). |

The importer keeps a compressed copy of each file in the scene (a hidden custom property) and numbers every face corner (the `tt_corner` attribute), so the exporter can tell later what was changed. The `<mesh texture>` attribute, which the converter ignores, is kept in the mesh's custom property `tt_texture`.

## Conventions for a model

- **Space:** metres, Z up, the unit standing on the origin and facing **+X** (the game turns +X towards the direction of travel). The Native warrior is about 2.9 units tall in its files and the Viking warrior about 2.3 (measured in M9; the 1.5 m written here before was wrong), so scale new props to the figures.
- **Armature:** any bone names and any hierarchy, at most 128 bones (the converter stores bone indices in a byte). Blender bones point along their Y axis and the 2004 rigs along X; the exporter converts between the two exactly, so nothing needs to be rotated by hand. Constraints, IK and drivers are fine: the exporter reads the evaluated pose of every frame.
- **Mesh:** quads and n-gons are triangulated on export. At most 65,536 distinct vertices per model (16-bit indices; the converter merges identical corners); units are about 500 triangles, buildings about 2,000 (PLAN.md §4.1). Mesh positions are written in the armature's space. Modifiers other than Armature (Mirror, for instance) are applied.
- **Weights:** every vertex needs a weight on at least one bone. Weights are normalised to add up to 1; a vertex without any goes to the root bone, with a warning. A mesh with no Armature modifier (a prop such as the spear) is written with the `dummy_bone` skin that skeleton-less sprites use.
- **Corner data:** the first UV map becomes `u v`, a second one `u2 v2` (only buildings use it); the active colour attribute becomes `r g b a` (white without one); normals are Blender's, custom normals included.
- **Detail levels:** a sprite lists its models from most to fewest triangles: the game draws the first close up and the second far away.
- **Textures:** hand-painted PNG in `assets/textures/models/<name>.png` (256² for units, 512² for chieftains and buildings), with the team-colour mask `<name>_team.png` in `assets/textures/teamdecals/`. UVs are as Blender shows them; the build flips the images for the graphics card. Weapon tiers are texture variants of one model (rock, iron, chicken in that order).

## Animations

- **One action per animation.** The exporter writes one file frame per Blender frame over the action's frame range (the importer sets a manual range from 0); the game steps through them without blending.
- **Settings per action** in Properties > Object Data (of the armature) > Tribal Trouble animation, for the active action: *Name* (as in `geometry.xml`), *Position*, *Distance per cycle* (`wpc`: metres per cycle for a run animation, 1 for the others) and *Type* (loop, or plain for attack and die).
- **The game picks animations by position, not by name:** 0 idle, 1 run, 2 attack, 3 die, then 4 sit for warriors and peons (peons add 5 steer, 6 paddle right, 7 paddle left) or 4 magic and 5 thor for chieftains.
- **Timing lives in code too:** a thrown weapon leaves the hand at a fixed fraction of the attack animation (46/100 for Natives, 29/58 for Vikings, in `RacesResources`). An attack with a different length needs that fraction changed.
- **The simulation reads models.** The world uses each animation's bounding box (from skinning every frame, flattened to a square in X and Y) and whether it loops. A changed model or animation can therefore change the simulation and the golden traces ([testing.md](testing.md)). Changing an existing sprite would change Classic too, so existing models stay as they are and new content goes into new sprites behind the Buffed ruleset.

## Export

Select the armature (or a prop mesh), File > Export > Tribal Trouble (.xml), and choose a folder such as `assets/geometry/natives/warrior`. The export writes the skeleton (named after the armature object), one file per mesh bound to it and one per action that animates it, each named after its object or action.

**Keep imported numbers** (on by default): every number is computed from the scene, and where it still matches the imported file within the tolerance below, the imported text is written instead. When the triangles, bones and frames are the same as on import, the imported file is patched in place rather than rewritten. A re-export of an untouched model is therefore byte for byte the original, and an edit shows up in Git as only the numbers it changed. Without imported data (a new model, or the option off) files are written in the standard layout.

| Value | Counts as unchanged within |
|---|---|
| positions, UVs | 1e-5 |
| colours, weights, matrix entries | 1e-4 |
| normals (per component, after normalising) | 1e-3 |

Blender stores numbers as 32-bit floats and keeps custom normals in a compressed form; these tolerances sit just above that noise and well below anything visible.

The export also puts a `<sprite>` entry into the Blender text `tt_sprite.xml` (Scripting workspace, or any Text Editor): paste it into `assets/geometry/geometry.xml` under the race's `<group>` and set the sprite's name. Its paths are relative to the folder that holds `geometry.xml`.

## Check, convert, test

- **Compare** an export with the original, file by file or folder by folder (Python 3, no Blender):
  `python tools/blender/io_scene_tribaltrouble.py diff assets/geometry/natives/warrior <export folder> [--tolerance]`
  Each file is "identical", "identical after formatting" (after `tools/scripts/format_assets.py`'s number style and LF line endings), "within tolerance" or "DIFFERENT", with the largest difference per kind of value. `... format FILE...` rewrites files in the standard layout.
- **Validate:** the converter validates every file against its DTD and fails the build on an error. `tools/scripts/validate_assets.py` checks more (attribute order, weights, sorted bones) but needs `xmllint` (libxml2), which is not installed on Josh's PC.
- **Convert:** any build runs `assets:geometry`, which turns `geometry.xml` and its files into `.binsprite` files (`assets/build/geometry_bin`).
- **In game:** `gradlew tt:run`, start a skirmish with the right race, type `/iamacheater` in the chat (Enter opens it): F2, F3 and F4 then spawn a rock, iron and chicken warrior at the centre of the view, and F6 kills the selection (the die animation). Sessions drive this with `tools/scripts/drive_game.py` (D-03).
- **Round trip** of whole sprites, as M4 ran it (the paths are this repository's):
  `.toolchain/blender/blender.exe --background --factory-startup --python tools/blender/roundtrip.py -- --sprite natives/warrior --sprite natives/spear`
  It imports each sprite, exports it with and without kept numbers into `build/roundtrip/`, and fails unless the first is byte for byte the original and the second within tolerance.
- **Tests** of the plain-Python half (reading, writing, keeping numbers, the bone arithmetic), run in CI: `python -m unittest discover -s tools/blender/tests`.

## The format

`assets/geometry/geometry.xml` lists the sprites; each points to XML files written by the 2004 3ds Max exporters, which survive in `tools/3dsmax/` as the original specification.

| Element | Attributes and meaning |
|---|---|
| `<group name>` | A folder of sprites (`natives`, `vikings`, `misc`); a sprite is loaded as `/geometry/<group>/<sprite>.binsprite`. |
| `<sprite name scale>` | One model with its animations. `scale` multiplies the vertices at conversion. |
| `<skeleton>` | Path of the skeleton file. Without one, the sprite gets a single still frame (buildings, props). |
| `<model r g b>` | Path of a mesh file and its textures; the first model is the high-detail one. The colour was the background for tree billboards and has been unused since Resurrected moved to shaders. |
| `<texture name team>` | `assets/textures/models/<name>.png` and the team-colour mask. The texture index the game asks for picks one, so their order matters. |
| `<animation name wpc type>` | Path of an animation file; `type` is `loop` or `plain`. |
| mesh: `<polygon>` | Exactly three `<vertex>` elements. |
| mesh: `<vertex x y z r g b a nx ny nz u v [u2 v2]>` | A triangle corner in model space; the normal is normalised when loaded. |
| mesh: `<skin bone weight>` | One bone influence; the weights of a corner add up to 1, and a bone listed twice adds up. |
| skeleton: `<bone name parent>` | Bones sorted by name; the root has parent `""`. |
| skeleton: `<init_pose>` | Each bone's transform in the bind pose (the pose the mesh was modelled in). |
| animation: `<frame index>` | Frames 0, 1, 2 ..., each with a transform per bone. |
| `<transform name m00 .. m33>` | A bone's absolute transform in model space. `m<i><j>` is column *i*, row *j* in the column-vector convention: `m30 m31 m32` is the position and `m00 m01 m02` the bone's X axis, which points along the bone. The converter computes frame × inverse(bind pose) per bone. |

**Layout.** Most files are in the standard layout of Resurrected's IntelliJ formatter: the XML declaration and DTD shown in `HEADERS` in the add-on, four-space indents, attributes wrapped at 120 columns under the first one, no newline at the end, numbers in the style of `format_assets.py` (`0` below 1e-9, at most nine significant digits). A few files keep older layouts: the 2004 exporter's line breaks (the chicken, palm and icon files) and the single-quoted style of Resurrected's ship-era files (the sitting, paddling and steering animations and the ships). The exporter keeps whatever layout a file had when it patches it in place.
