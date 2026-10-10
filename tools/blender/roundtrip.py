"""Round trip in Blender: import sprites from assets/geometry, export them again and compare with the originals.

    blender --background --factory-startup --python tools/blender/roundtrip.py -- [options]

Options (after the --):
    --sprite GROUP/NAME   a sprite from geometry.xml; repeatable (default natives/warrior and natives/spear)
    --out FOLDER          where the exports go (default build/roundtrip)
    --blend FILE          also save the imported scene as a .blend file

Each sprite is exported twice: with "Keep imported numbers" (out/keep/<group>), which must give every file back
byte for byte, and without it (out/fresh/<group>), where every number is computed from the Blender scene and must
match the original within the tolerances in io_scene_tribaltrouble.TOLERANCE. Exit code 0 when both hold.
"""

import importlib.util
import os
import sys
import time

import bpy

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
GEOMETRY_XML = os.path.join(ROOT, "assets", "geometry", "geometry.xml")


def load_addon():
    spec = importlib.util.spec_from_file_location("io_scene_tribaltrouble", os.path.join(HERE,
                                                                                         "io_scene_tribaltrouble.py"))
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    module.register()
    return module


def arguments():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    sprites, out, blend = [], os.path.join(ROOT, "build", "roundtrip"), None
    i = 0
    while i < len(argv):
        if argv[i] == "--sprite":
            sprites.append(argv[i + 1])
        elif argv[i] == "--out":
            out = os.path.abspath(argv[i + 1])
        elif argv[i] == "--blend":
            blend = os.path.abspath(argv[i + 1])
        else:
            raise SystemExit("unknown option %s" % argv[i])
        i += 2
    return sprites or ["natives/warrior", "natives/spear"], out, blend


def main():
    tt = load_addon()
    sprites, out, blend = arguments()
    context = bpy.context
    for obj in list(bpy.data.objects):
        bpy.data.objects.remove(obj)
    context.view_layer.update()
    entries = {(s.group, s.name): s for s in tt.read_sprites(tt.read_text(GEOMETRY_XML))}
    failures = 0
    for sprite in sprites:
        group, name = sprite.split("/")
        entry = entries[(group, name)]
        started = time.time()
        armature, meshes, actions = tt.import_sprite(context, GEOMETRY_XML, group, name)
        print("imported %s: %s, %d meshes, %d actions in %.1f s" % (
            sprite, armature.name if armature else "no skeleton", len(meshes), len(actions), time.time() - started))
        objects = [armature] if armature else meshes
        originals = {}
        if entry.skeleton:
            originals[os.path.basename(entry.skeleton)] = entry.skeleton
        for path, _, _ in entry.models:
            originals[os.path.basename(path)] = path
        for _, _, _, path in entry.animations:
            originals[os.path.basename(path)] = path
        for mode, keep in (("keep", True), ("fresh", False)):
            folder = os.path.join(out, mode, group)
            started = time.time()
            written, snippets = tt.export_objects(context, objects, folder, keep)
            print("exported %s (%s) in %.1f s" % (sprite, mode, time.time() - started))
            for file_name in sorted(written):
                original = os.path.join(os.path.dirname(GEOMETRY_XML), originals[file_name])
                result = tt.compare(tt.read_text(original), written[file_name])
                if result.identical:
                    state = "identical"
                elif result.formatted:
                    state = "identical after formatting"
                elif result.within_tolerance:
                    state = "within tolerance"
                else:
                    state = "DIFFERENT"
                worst = ", ".join("%s %.1e" % (k, v) for k, v in sorted(result.deviation.items()))
                print("  %-5s %-36s %-26s %s" % (mode, file_name, state, worst))
                for problem in result.problems[:2]:
                    print("        " + problem)
                passed = result.identical if keep else result.within_tolerance
                failures += 0 if passed else 1
            missing = sorted(set(originals) - set(written))
            if missing:
                print("  %-5s not exported: %s" % (mode, ", ".join(missing)))
                failures += len(missing)
            if mode == "keep":
                print(snippets[0])
    if blend:
        bpy.ops.wm.save_as_mainfile(filepath=blend)
    print("round trip %s" % ("passed" if failures == 0 else "FAILED: %d files" % failures))
    return failures


if __name__ == "__main__":
    code = 1
    try:
        code = 1 if main() else 0
    except Exception:
        import traceback
        traceback.print_exc()
    sys.stdout.flush()
    os._exit(code)
