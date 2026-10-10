"""Tests of the plain-Python half of the Blender add-on (no Blender needed).

    python -m unittest discover -s tools/blender/tests

They read the game's own files in assets/geometry. The Blender round trip itself is tools/blender/roundtrip.py.
"""

import math
import os
import random
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))

import io_scene_tribaltrouble as tt  # noqa: E402

GEOMETRY = os.path.normpath(os.path.join(HERE, "..", "..", "..", "assets", "geometry"))
WARRIOR = os.path.join(GEOMETRY, "natives", "warrior")
WARRIOR_FILES = sorted(f for f in os.listdir(WARRIOR) if f.endswith(".xml"))


def source(path):
    return tt.read_text(path).replace("\r\n", "\n")


def geometry_files():
    for folder, _, files in os.walk(GEOMETRY):
        for name in sorted(files):
            if name.endswith(".xml") and name != "geometry.xml":
                yield os.path.join(folder, name)


def vertex_groups(skins):
    """Skins as Blender's vertex groups hold them and the exporter reads them: a bone listed twice becomes one
    weight (added as 32-bit floats), and bones with no weight are left out."""
    merged = {}
    for bone, weight in skins:
        merged[bone] = tt.f32(merged.get(bone, 0.0) + tt.f32(weight))
    return [(bone, weight) for bone, weight in merged.items() if weight > 0]


def as_blender(data, kind, noise=0.0, rng=None):
    """data as the exporter would compute it from Blender: every number a 32-bit float, plus optional noise."""
    def n(x):
        return tt.f32(x + (rng.uniform(-noise, noise) if noise else 0.0))

    if kind == "mesh":
        corners = [tt.Corner({k: n(v) for k, v in c.values.items()}, vertex_groups(c.skins), i)
                   for i, c in enumerate(data.corners)]
        return tt.MeshData(corners, data.texture)
    if kind == "skeleton":
        return tt.SkeletonData(list(data.bones), {k: tuple(n(v) for v in m) for k, m in data.pose.items()})
    return tt.AnimationData([{k: tuple(n(v) for v in m) for k, m in f.items()} for f in data.frames])


EXPORT = {"mesh": tt.export_mesh, "skeleton": tt.export_skeleton, "animation": tt.export_animation}


def orthonormal(rows):
    """What Blender keeps of a rest matrix: the rotation (Gram-Schmidt on the bone axis first) and the head."""
    y = [rows[r][1] for r in range(3)]
    z = [rows[r][2] for r in range(3)]
    ly = math.sqrt(sum(v * v for v in y))
    y = [v / ly for v in y]
    d = sum(a * b for a, b in zip(z, y))
    z = [a - d * b for a, b in zip(z, y)]
    lz = math.sqrt(sum(v * v for v in z))
    z = [v / lz for v in z]
    x = [y[1] * z[2] - y[2] * z[1], y[2] * z[0] - y[0] * z[2], y[0] * z[1] - y[1] * z[0]]
    out = [[tt.f32(x[r]), tt.f32(y[r]), tt.f32(z[r]), tt.f32(rows[r][3])] for r in range(3)]
    return out + [[0.0, 0.0, 0.0, 1.0]]


class NumberTest(unittest.TestCase):
    def test_numbers_print_like_format_assets(self):
        for text in ("0.0326763", "1.34309e-06", "-0.992698", "1", "10", "0.5", "1.47838", "0.666667"):
            self.assertEqual(tt.fmt(float(text)), text)
            self.assertEqual(tt.fmt(tt.f32(float(text))), text)
        self.assertEqual(tt.fmt(-0.0), "0")
        self.assertEqual(tt.fmt(3e-10), "0")
        self.assertEqual(tt.fmt(0.032165455818176), "0.0321654558")  # more digits than a float: nine, as format_assets
        self.assertEqual(tt.fmt(tt.f32(0.1) + 0.0), "0.1")

    def test_axis_conversion_is_exact(self):
        rng = random.Random(1)
        for _ in range(100):
            m = tuple(tt.f32(rng.uniform(-2, 2)) for _ in range(16))
            self.assertEqual(tt.from_blender(tt.to_blender(m)), m)

    def test_decompose_and_compose(self):
        rng = random.Random(2)
        for _ in range(50):
            q = [rng.uniform(-1, 1) for _ in range(4)]
            length = math.sqrt(sum(v * v for v in q))
            q = [v / length for v in q]
            m = tt.compose((1, 2, 3), q, (1.5, 0.5, 2))
            location, quaternion, scale = tt.decompose(m)
            again = tt.compose(location, quaternion, scale)
            for r in range(4):
                for c in range(4):
                    self.assertAlmostEqual(m[r][c], again[r][c], places=9)
        location, quaternion, scale = tt.decompose(tt.compose((1, 2, 3), (1, 0, 0, 0), (0, 0, 0)))
        self.assertEqual((location, quaternion, scale), ((1, 2, 3), (1.0, 0.0, 0.0, 0.0), (0.0, 0.0, 0.0)))


class LayoutTest(unittest.TestCase):
    def test_every_file_reads_and_writes_back_the_same_data(self):
        count = 0
        for path in geometry_files():
            text = source(path)
            kind, data, _ = tt.load(text)
            written = tt.write(kind, data)
            result = tt.compare(text, written)
            self.assertTrue(result.same_structure, (path, result.problems))
            # Numbers keep nine significant digits (some Resurrected files have more than a float holds).
            self.assertTrue(all(v < 1e-6 for v in result.deviation.values()), (path, result.deviation))
            self.assertEqual(tt.write(kind, tt.load(written)[1]), written, path)
            count += 1
        self.assertGreater(count, 150)

    def test_warrior_files_in_the_standard_layout_write_back_byte_for_byte(self):
        checked = 0
        for folder in (WARRIOR, os.path.join(GEOMETRY, "vikings", "warrior")):
            for name in sorted(os.listdir(folder)):
                text = source(os.path.join(folder, name))
                kind, data, document = tt.load(text)
                if document.header != tt.HEADERS[kind]:
                    continue  # the sitting animations and an old export keep another layout
                layout = {"final_newline": document.final_newline}
                if kind == "animation":
                    layout["order"] = list(data.frames[0])
                self.assertEqual(tt.write(kind, data, **layout), text, name)
                checked += 1
        self.assertGreaterEqual(checked, 15)


class KeepNumbersTest(unittest.TestCase):
    def test_untouched_warrior_exports_byte_for_byte(self):
        rng = random.Random(3)
        for name in WARRIOR_FILES:
            text = source(os.path.join(WARRIOR, name))
            kind, data, _ = tt.load(text)
            noise = {"mesh": 1e-6, "skeleton": 1e-5, "animation": 1e-5}[kind]
            self.assertEqual(EXPORT[kind](as_blender(data, kind, noise, rng), text), text, name)

    def test_an_edit_changes_only_its_number(self):
        text = source(os.path.join(WARRIOR, "native_warrior_run.xml"))
        _, data, _ = tt.load(text)
        computed = as_blender(data, "animation")
        frame = computed.frames[7]
        frame["Head"] = frame["Head"][:12] + (frame["Head"][12] + 0.01,) + frame["Head"][13:]
        out = tt.export_animation(computed, text)
        changed = [(a, b) for a, b in zip(text.split("\n"), out.split("\n")) if a != b]
        self.assertEqual(len(out.split("\n")), len(text.split("\n")))
        self.assertEqual(len(changed), 1)
        self.assertIn('m30="%s"' % tt.fmt(computed.frames[7]["Head"][12]), changed[0][1])

    def test_a_skin_edit_rewrites_only_that_vertex(self):
        text = source(os.path.join(WARRIOR, "native_warrior_mesh.xml"))
        _, data, _ = tt.load(text)
        computed = as_blender(data, "mesh")
        computed.corners[0].skins = [("Pelvis", 0.25), ("Spine", 0.75)]
        out = tt.export_mesh(computed, text)
        again = tt.load(out)[1]
        self.assertEqual(again.corners[0].skins, [("Pelvis", 0.25), ("Spine", 0.75)])
        self.assertEqual(again.corners[1:], data.corners[1:])
        self.assertEqual(len(out.split("\n")), len(text.split("\n")) + 1)  # Tail 1 became two skins

    def test_a_new_frame_rewrites_the_file_in_the_standard_layout(self):
        text = source(os.path.join(WARRIOR, "native_warrior_idle.xml"))
        _, data, _ = tt.load(text)
        computed = as_blender(data, "animation")
        computed.frames.append(dict(computed.frames[-1]))
        out = tt.export_animation(computed, text)
        again = tt.load(out)[1]
        self.assertEqual(len(again.frames), len(data.frames) + 1)
        self.assertEqual(again.frames[:-1], data.frames)  # the old frames keep their imported numbers
        self.assertTrue(out.startswith(tt.HEADERS["animation"]))

    def test_without_a_source_the_standard_layout_is_written(self):
        text = source(os.path.join(WARRIOR, "native_warrior_skeleton.xml"))
        _, data, _ = tt.load(text)
        self.assertEqual(tt.export_skeleton(data), text)

    def test_source_packing(self):
        text = source(os.path.join(WARRIOR, "native_warrior_skeleton.xml"))
        self.assertEqual(tt.unpack_source(tt.pack_source(text)), text)


class BlenderMathTest(unittest.TestCase):
    """The import and export arithmetic of the Blender half, imitated: rest matrices orthonormalised as edit bones
    keep them, keys stored as 32-bit floats, poses composed the way Blender composes them."""

    def round_trip(self, animation_name):
        _, skeleton, _ = tt.load(source(os.path.join(WARRIOR, "native_warrior_skeleton.xml")))
        text = source(os.path.join(WARRIOR, animation_name))
        _, animation, _ = tt.load(text)
        rest = {name: orthonormal(tt.to_blender(m)) for name, m in skeleton.pose.items()}
        keys = tt.animation_keys(skeleton, rest, animation)
        parents = dict(skeleton.bones)
        frames = []
        for i in range(len(animation.frames)):
            pose = {}
            for name in tt.bone_order(skeleton):
                location, quaternion, scale = (tuple(tt.f32(v) for v in part) for part in keys[name][i])
                basis = tt.compose(location, quaternion, scale)
                parent = parents[name]
                if parent in parents:
                    pose[name] = tt.matmul(tt.matmul(tt.matmul(pose[parent], tt.invert(rest[parent])), rest[name]),
                                           basis)
                else:
                    pose[name] = tt.matmul(rest[name], basis)
            frames.append({name: tuple(tt.f32(v) for v in tt.from_blender(m)) for name, m in pose.items()})
        computed = tt.AnimationData(frames)
        comparison = tt.compare(text, tt.write_animation(computed))
        self.assertTrue(comparison.within_tolerance, comparison.deviation)
        self.assertEqual(tt.export_animation(computed, text), text)
        return comparison.deviation

    def test_run(self):
        self.assertLess(self.round_trip("native_warrior_run.xml")["matrix"], 1e-5)

    def test_sitting_with_props_scaled_to_nothing(self):
        self.assertLess(self.round_trip("native_warrior_sitting.xml")["matrix"], 5e-5)

    def test_skeleton_export_keeps_numbers_after_orthonormalising(self):
        text = source(os.path.join(WARRIOR, "native_warrior_skeleton.xml"))
        _, skeleton, _ = tt.load(text)
        pose = {name: tuple(tt.f32(v) for v in tt.from_blender(orthonormal(tt.to_blender(m))))
                for name, m in skeleton.pose.items()}
        self.assertEqual(tt.export_skeleton(tt.SkeletonData(skeleton.bones, pose), text), text)


class MeshBuildingTest(unittest.TestCase):
    def test_weld(self):
        for name in ("native_warrior_mesh.xml", "native_warrior_low_poly_mesh.xml", "native_warrior_spear.xml"):
            _, mesh, _ = tt.load(source(os.path.join(WARRIOR, name)))
            positions, skins, triangles = tt.weld(mesh.corners)
            self.assertEqual(len(triangles) * 3, len(mesh.corners))
            self.assertLess(len(positions), len(mesh.corners))
            self.assertTrue(all(len(set(t)) == 3 for t in triangles))
            self.assertEqual(len({frozenset(t) for t in triangles}), len(triangles))
            for i, corner in enumerate(mesh.corners):
                vertex = triangles[i // 3][i % 3]
                self.assertEqual(positions[vertex], (corner.values["x"], corner.values["y"], corner.values["z"]))
                self.assertEqual(skins[vertex], corner.skins)

    def test_bone_lengths_and_order(self):
        _, skeleton, _ = tt.load(source(os.path.join(WARRIOR, "native_warrior_skeleton.xml")))
        lengths = tt.bone_lengths(skeleton)
        self.assertTrue(all(0.01 < v < 1 for v in lengths.values()), lengths)
        order = tt.bone_order(skeleton)
        parents = dict(skeleton.bones)
        self.assertEqual(sorted(order), sorted(parents))
        for name in order:
            if parents[name] in parents:
                self.assertLess(order.index(parents[name]), order.index(name))

    def test_sprites(self):
        sprites = tt.read_sprites(source(os.path.join(GEOMETRY, "geometry.xml")))
        warrior = next(s for s in sprites if (s.group, s.name) == ("natives", "warrior"))
        self.assertEqual(warrior.skeleton, "natives/warrior/native_warrior_skeleton.xml")
        self.assertEqual([a[0] for a in warrior.animations], ["idle", "run", "attack", "die", "sit"])
        self.assertEqual(warrior.animations[1][1:3], (3.2, "loop"))
        self.assertEqual(warrior.models[0][1:], ((90, 60, 30), [
            ("native_warrior_rock", "native_warrior_rock_team"), ("native_warrior_iron", "native_warrior_iron_team"),
            ("native_warrior_rubber", "native_warrior_rubber_team")]))
        snippet = tt.sprite_snippet("warrior", warrior.skeleton, warrior.models, warrior.animations)
        self.assertIn('<animation name="run" wpc="3.2" type="loop">natives/warrior/native_warrior_run.xml', snippet)


class CompareTest(unittest.TestCase):
    def test_compare(self):
        text = source(os.path.join(WARRIOR, "native_warrior_idle.xml"))
        _, data, _ = tt.load(text)
        self.assertTrue(tt.compare(text, text.replace("\n", "\r\n")).identical)
        rng = random.Random(4)
        noisy = tt.write_animation(as_blender(data, "animation", 1e-6, rng))
        result = tt.compare(text, noisy)
        self.assertFalse(result.formatted)
        self.assertTrue(result.within_tolerance)
        self.assertLess(result.deviation["matrix"], 2e-6)
        data.frames.pop()
        result = tt.compare(text, tt.write_animation(data))
        self.assertFalse(result.same_structure)


if __name__ == "__main__":
    unittest.main()
