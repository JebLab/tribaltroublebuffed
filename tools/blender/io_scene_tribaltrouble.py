"""Tribal Trouble geometry for Blender: import and export meshes, skeletons and animations.

A single-file add-on in two halves. The first half is plain Python with no Blender in it: it reads and writes the
game's geometry XML (assets/geometry), keeps imported numbers on export, and compares files. The tests in
tools/blender/tests run it without Blender, and it works from a shell:

    python io_scene_tribaltrouble.py diff ORIGINAL EXPORTED [--tolerance]   (files or folders)
    python io_scene_tribaltrouble.py format FILE...                         (rewrite in the standard layout)

The second half is the Blender add-on (Blender 4.2 or newer): File > Import / Export > Tribal Trouble.
docs/content-pipeline.md explains the format, the conventions and the workflow.
"""

import ast
import base64
import math
import os
import re
import struct
import sys
import zlib
from dataclasses import dataclass, field

bl_info = {
    "name": "Tribal Trouble geometry (XML)",
    "author": "Tribal Trouble Buffed",
    "version": (0, 1, 0),
    "blender": (4, 2, 0),
    "location": "File > Import-Export > Tribal Trouble",
    "description": "Import and export the meshes, skeletons and animations of Tribal Trouble (geometry XML)",
    "doc_url": "https://github.com/JebLab/tribaltroublebuffed/blob/revamp/docs/content-pipeline.md",
    "category": "Import-Export",
}

# ---------------------------------------------------------------------------------------------------------------------
# The format (plain Python)
# ---------------------------------------------------------------------------------------------------------------------

# The files in assets/geometry were laid out by IntelliJ's XML formatter: four-space indents, attributes wrapped at
# 120 columns and continued under the first attribute.
MARGIN = 120
INDENT = "    "

MATRIX_ATTRS = tuple("m%d%d" % (i, j) for i in range(4) for j in range(4))
VERTEX_ATTRS = ("x", "y", "z", "r", "g", "b", "a", "nx", "ny", "nz", "u", "v")
UV2_ATTRS = ("u2", "v2")
DUMMY_BONE = "dummy_bone"

# How far a number computed in Blender may be from the imported one and still count as unchanged. Untouched data
# then exports with its original text, so a re-export changes only what was edited.
TOLERANCE = {"position": 1e-5, "color": 1e-4, "normal": 1e-3, "uv": 1e-5, "weight": 1e-4, "matrix": 1e-4}
ATTR_KIND = {
    "x": "position", "y": "position", "z": "position",
    "r": "color", "g": "color", "b": "color", "a": "color",
    "nx": "normal", "ny": "normal", "nz": "normal",
    "u": "uv", "v": "uv", "u2": "uv", "v2": "uv",
}

# The headers (XML declaration and DTD) of the standard layout. The converter validates against the DTD.
_MATRIX_DTD = "".join("        <!ATTLIST transform %s CDATA #REQUIRED>\n" % name for name in ("name",) + MATRIX_ATTRS)
_BONE_DTD = "        <!ATTLIST bone name CDATA #REQUIRED>\n        <!ATTLIST bone parent CDATA #REQUIRED>\n"
_VERTEX_DTD = "".join(
    "        <!ATTLIST vertex %s CDATA #%s>\n" % (name, "IMPLIED" if name in UV2_ATTRS else "REQUIRED")
    for name in VERTEX_ATTRS + UV2_ATTRS)
_DECLARATION = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n\n'
HEADERS = {
    "mesh": _DECLARATION + "<!DOCTYPE mesh [\n"
    "        <!ELEMENT mesh       (polygons, skeleton?)>\n"
    "        <!ELEMENT polygons   (polygon+)>\n"
    "        <!ELEMENT polygon    (vertex, vertex, vertex)>\n"
    "        <!ELEMENT vertex     (skin+)>\n"
    "        <!ELEMENT skin        EMPTY>\n"
    "        <!ELEMENT skeleton   (bones, init_pose)>\n"
    "        <!ELEMENT bones      (bone+)>\n"
    "        <!ELEMENT init_pose  (transform+)>\n"
    "        <!ELEMENT bone        EMPTY>\n"
    "        <!ELEMENT transform   EMPTY>\n"
    "        <!ATTLIST mesh texture CDATA #IMPLIED>\n"
    + _VERTEX_DTD
    + "        <!ATTLIST skin bone CDATA #REQUIRED>\n"
    "        <!ATTLIST skin weight CDATA #REQUIRED>\n"
    + _MATRIX_DTD + _BONE_DTD + "        ]>\n\n",
    "skeleton": _DECLARATION + "<!DOCTYPE skeleton [\n"
    "        <!ELEMENT skeleton   (bones, init_pose)>\n"
    "        <!ELEMENT bones      (bone+)>\n"
    "        <!ELEMENT init_pose  (transform+)>\n"
    "        <!ELEMENT bone        EMPTY>\n"
    "        <!ELEMENT transform   EMPTY>\n"
    + _MATRIX_DTD + _BONE_DTD + "        ]>\n\n",
    "animation": _DECLARATION + "<!DOCTYPE animation [\n"
    "        <!ELEMENT animation  (frame+)>\n"
    "        <!ELEMENT frame      (transform+)>\n"
    "        <!ELEMENT transform   EMPTY>\n"
    "        <!ATTLIST frame index CDATA #REQUIRED>\n"
    + _MATRIX_DTD + "        ]>\n\n",
}


class FormatError(ValueError):
    """A file that is not a geometry XML file the converter would accept."""


# Numbers --------------------------------------------------------------------------------------------------------------

_F32 = struct.Struct("<f")


def f32(x):
    """x rounded to the nearest 32-bit float, as Blender and the game store it."""
    return _F32.unpack(_F32.pack(x))[0]


def fmt(x):
    """The text written for a number. Below 1e-9 it is '0', as tools/scripts/format_assets.py writes it. A number
    that is a 32-bit float (everything that comes from Blender) gets the shortest decimal that reads back as that
    float; anything more precise (imported text with more digits than a float holds) gets nine significant digits,
    as format_assets.py does. Either way the text is unchanged by format_assets.py."""
    if abs(x) < 1e-9:
        return "0"
    try:
        y = f32(x)
    except (OverflowError, struct.error):
        return "{:.9g}".format(x)
    if y == x:
        for digits in range(1, 10):
            text = "%.*g" % (digits, y)
            if f32(float(text)) == y:
                return "{:.9g}".format(float(text))
    return "{:.9g}".format(x)


# Matrices -------------------------------------------------------------------------------------------------------------
#
# A transform holds a bone's absolute transform in model space (Z up, metres) as the 3ds Max exporter wrote it:
# attribute m<i><j> is row i+1, component j+1 of the Max matrix. In the column-vector convention of Blender and
# OpenGL that is column i, row j: m30 m31 m32 is the translation and m00 m01 m02 the bone's X axis, which points
# along the bone. The converter and the game only use frame x inverse(init pose), so any axis convention works as
# long as the skeleton, the animations and the mesh agree.

def xml_to_rows(m):
    """The 16 transform values (m00..m33) as a 4x4 row-major matrix in the column-vector convention."""
    return [[m[c * 4 + r] for c in range(4)] for r in range(4)]


def rows_to_xml(rows):
    return tuple(rows[r][c] for c in range(4) for r in range(4))


def matmul(a, b):
    return [[sum(a[r][k] * b[k][c] for k in range(4)) for c in range(4)] for r in range(4)]


def invert(m):
    """Inverse of a 4x4 matrix (Gauss-Jordan with partial pivoting); ValueError when it is singular."""
    n = [list(row) + [1.0 if r == c else 0.0 for c in range(4)] for r, row in enumerate(m)]
    for c in range(4):
        pivot = max(range(c, 4), key=lambda r: abs(n[r][c]))
        if abs(n[pivot][c]) < 1e-12:
            raise ValueError("singular matrix")
        n[c], n[pivot] = n[pivot], n[c]
        p = n[c][c]
        n[c] = [v / p for v in n[c]]
        for r in range(4):
            if r != c and n[r][c] != 0.0:
                f = n[r][c]
                n[r] = [a - f * b for a, b in zip(n[r], n[c])]
    return [row[4:] for row in n]


# Blender bones point along their local Y axis, Max bones along X. A Blender bone's matrix is the Max matrix times
# AXIS, which only swaps and negates axes, so converting back and forth loses nothing.
AXIS = [[0.0, 1.0, 0.0, 0.0], [-1.0, 0.0, 0.0, 0.0], [0.0, 0.0, 1.0, 0.0], [0.0, 0.0, 0.0, 1.0]]
AXIS_INVERSE = [[0.0, -1.0, 0.0, 0.0], [1.0, 0.0, 0.0, 0.0], [0.0, 0.0, 1.0, 0.0], [0.0, 0.0, 0.0, 1.0]]


def to_blender(m):
    """A transform (16 values) as the matching Blender bone matrix (4x4 rows)."""
    return matmul(xml_to_rows(m), AXIS)


def from_blender(rows):
    """A Blender bone matrix (armature space, 4x4 rows) as a transform (16 values)."""
    return rows_to_xml(matmul([list(r) for r in rows], AXIS_INVERSE))


def pose_basis(rest, pose, parent_rest=None, parent_pose=None):
    """The local pose matrix (Blender's matrix_basis) that puts a bone at `pose` (armature space), given its rest
    matrix and its parent's rest and pose matrices, for Blender's default bone settings (inherit rotation and full
    scale, local location): pose = parent_pose @ inverse(parent_rest) @ rest @ basis."""
    if parent_rest is None:
        return matmul(invert(rest), pose)
    return matmul(matmul(matmul(invert(rest), parent_rest), invert(parent_pose)), pose)


def decompose(m):
    """Location, rotation quaternion (w, x, y, z) and scale of a 4x4 matrix. A matrix with a zero axis (a bone
    scaled to nothing, as the props in the warrior's sitting animation) gets the identity rotation."""
    location = (m[0][3], m[1][3], m[2][3])
    columns = [[m[r][c] for r in range(3)] for c in range(3)]
    scale = [math.sqrt(sum(v * v for v in col)) for col in columns]
    if min(scale) < 1e-9:
        return location, (1.0, 0.0, 0.0, 0.0), tuple(scale)
    axes = [[v / s for v in col] for col, s in zip(columns, scale)]
    x, y, z = axes
    det = (x[0] * (y[1] * z[2] - y[2] * z[1]) - x[1] * (y[0] * z[2] - y[2] * z[0])
           + x[2] * (y[0] * z[1] - y[1] * z[0]))
    if det < 0:
        scale[0] = -scale[0]
        axes[0] = [-v for v in x]
    r = [[axes[c][row] for c in range(3)] for row in range(3)]
    return location, matrix_to_quaternion(r), tuple(scale)


def matrix_to_quaternion(r):
    trace = r[0][0] + r[1][1] + r[2][2]
    if trace > 0:
        s = math.sqrt(trace + 1.0) * 2
        q = (0.25 * s, (r[2][1] - r[1][2]) / s, (r[0][2] - r[2][0]) / s, (r[1][0] - r[0][1]) / s)
    elif r[0][0] > r[1][1] and r[0][0] > r[2][2]:
        s = math.sqrt(1.0 + r[0][0] - r[1][1] - r[2][2]) * 2
        q = ((r[2][1] - r[1][2]) / s, 0.25 * s, (r[0][1] + r[1][0]) / s, (r[0][2] + r[2][0]) / s)
    elif r[1][1] > r[2][2]:
        s = math.sqrt(1.0 + r[1][1] - r[0][0] - r[2][2]) * 2
        q = ((r[0][2] - r[2][0]) / s, (r[0][1] + r[1][0]) / s, 0.25 * s, (r[1][2] + r[2][1]) / s)
    else:
        s = math.sqrt(1.0 + r[2][2] - r[0][0] - r[1][1]) * 2
        q = ((r[1][0] - r[0][1]) / s, (r[0][2] + r[2][0]) / s, (r[1][2] + r[2][1]) / s, 0.25 * s)
    norm = math.sqrt(sum(v * v for v in q))
    return tuple(v / norm for v in q)


def compose(location, quaternion, scale):
    """Inverse of decompose: translation @ rotation @ scale."""
    w, x, y, z = quaternion
    r = [[1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
         [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
         [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)]]
    return [[r[row][0] * scale[0], r[row][1] * scale[1], r[row][2] * scale[2], location[row]] for row in range(3)] + [
        [0.0, 0.0, 0.0, 1.0]]


# Reading --------------------------------------------------------------------------------------------------------------

_TOKEN = re.compile(r"<!--.*?-->|<(/?)([A-Za-z_][\w.:-]*)((?:\s+[\w.:-]+\s*=\s*(?:\"[^\"]*\"|'[^']*'))*)\s*(/?)>",
                    re.S)
_ATTRIBUTE = re.compile(r"([\w.:-]+)\s*=\s*(?:\"([^\"]*)\"|'([^']*)')")
_ENTITIES = {"&lt;": "<", "&gt;": ">", "&quot;": '"', "&apos;": "'", "&amp;": "&"}


def _unescape(value):
    return re.sub(r"&(?:lt|gt|quot|apos|amp);", lambda m: _ENTITIES[m.group(0)], value) if "&" in value else value


def _escape(value):
    return value.replace("&", "&amp;").replace("<", "&lt;").replace('"', "&quot;")


class Element:
    """One element of a parsed file, with the character positions the exporter needs to patch it in place."""
    __slots__ = ("tag", "attrs", "spans", "children", "start", "body_start", "body_end", "end")

    def __init__(self, tag, start):
        self.tag = tag
        self.attrs = {}
        self.spans = {}  # attribute name -> (start, end) of its value, inside the quotes
        self.children = []
        self.start = start
        self.body_start = self.body_end = self.end = None

    def text(self, source):
        """The text before the first child (a path in geometry.xml)."""
        if self.body_start is None:
            return ""
        end = self.children[0].start if self.children else self.body_end
        return source[self.body_start:end].strip()

    def find(self, tag):
        return next((c for c in self.children if c.tag == tag), None)

    def findall(self, tag):
        return [c for c in self.children if c.tag == tag]


@dataclass
class Document:
    """A parsed file: the text (line endings normalised to LF), its root element and what comes before it."""
    text: str
    root: Element

    @property
    def header(self):
        return self.text[:self.root.start]

    @property
    def final_newline(self):
        return self.text.endswith("\n")


def parse(text):
    """Parse a geometry XML file (as text). The parser is small on purpose: it keeps the position of every attribute
    value so that the exporter can change single numbers and leave the rest of a file as it was."""
    text = text.replace("\r\n", "\n")
    root = None
    stack = []
    for match in _TOKEN.finditer(text):
        closing, tag, attributes, empty = match.groups()
        if tag is None:
            continue  # a comment
        if closing:
            if not stack or stack[-1].tag != tag:
                raise FormatError("unexpected </%s> at character %d" % (tag, match.start()))
            element = stack.pop()
            element.body_end = match.start()
            element.end = match.end()
            continue
        element = Element(tag, match.start())
        offset = match.start(3)
        for attribute in _ATTRIBUTE.finditer(attributes):
            group = 2 if attribute.group(2) is not None else 3
            element.attrs[attribute.group(1)] = _unescape(attribute.group(group))
            element.spans[attribute.group(1)] = (offset + attribute.start(group), offset + attribute.end(group))
        if stack:
            stack[-1].children.append(element)
        elif root is None:
            root = element
        else:
            raise FormatError("more than one root element")
        if empty:
            element.end = match.end()
        else:
            element.body_start = match.end()
            stack.append(element)
    if root is None:
        raise FormatError("no root element")
    if stack:
        raise FormatError("<%s> is not closed" % stack[-1].tag)
    return Document(text, root)


def read_text(path):
    with open(path, "rb") as f:
        raw = f.read()
    try:
        return raw.decode("utf-8")
    except UnicodeDecodeError:
        return raw.decode("latin-1")


def _float(element, name):
    try:
        return float(element.attrs[name])
    except KeyError:
        raise FormatError("<%s> without %s" % (element.tag, name)) from None
    except ValueError:
        raise FormatError("<%s %s=\"%s\"> is not a number" % (element.tag, name, element.attrs[name])) from None


def _matrix(element):
    return tuple(_float(element, name) for name in MATRIX_ATTRS)


@dataclass
class Corner:
    """One <vertex>: a corner of a triangle."""
    values: dict  # attribute name -> number, for VERTEX_ATTRS and u2 v2 when present
    skins: list  # [(bone, weight)] in file order; a bone may appear twice (the converter adds the weights)
    source: int = None  # exporter only: the index of the imported corner this one was computed from, if any


@dataclass
class MeshData:
    corners: list  # three per triangle, in file order
    texture: str = None  # the <mesh texture> attribute; the converter ignores it
    extra: str = None  # an embedded <skeleton> (two Viking axes have a dummy one), kept as text; also ignored


@dataclass
class SkeletonData:
    bones: list  # [(name, parent)], parent "" for a root
    pose: dict  # bone name -> transform (16 numbers, m00..m33)


@dataclass
class AnimationData:
    frames: list  # [{bone name -> transform}], frame i has index i


def mesh_from(document):
    root = document.root
    if root.tag != "mesh":
        raise FormatError("not a mesh: <%s>" % root.tag)
    polygons = root.find("polygons")
    if polygons is None:
        raise FormatError("<mesh> without <polygons>")
    corners = []
    for polygon in polygons.findall("polygon"):
        vertices = polygon.findall("vertex")
        if len(vertices) != 3:
            raise FormatError("a polygon with %d vertices; the converter needs triangles" % len(vertices))
        for vertex in vertices:
            values = {name: _float(vertex, name) for name in VERTEX_ATTRS}
            for name in UV2_ATTRS:
                if name in vertex.attrs:
                    values[name] = _float(vertex, name)
            skins = [(skin.attrs.get("bone", ""), _float(skin, "weight")) for skin in vertex.findall("skin")]
            if not skins:
                raise FormatError("a vertex without <skin>")
            corners.append(Corner(values, skins))
    extra = root.find("skeleton")
    if extra is not None:
        extra = document.text[document.text.rfind("\n", 0, extra.start) + 1:extra.end]
    return MeshData(corners, root.attrs.get("texture"), extra)


def skeleton_from(document):
    root = document.root
    if root.tag != "skeleton":
        raise FormatError("not a skeleton: <%s>" % root.tag)
    bones_element, pose_element = root.find("bones"), root.find("init_pose")
    if bones_element is None or pose_element is None:
        raise FormatError("<skeleton> needs <bones> and <init_pose>")
    bones = [(b.attrs.get("name", ""), b.attrs.get("parent", "")) for b in bones_element.findall("bone")]
    pose = {t.attrs.get("name", ""): _matrix(t) for t in pose_element.findall("transform")}
    missing = [name for name, _ in bones if name not in pose]
    if missing:
        raise FormatError("bones without an init pose: %s" % ", ".join(missing))
    return SkeletonData(bones, pose)


def animation_from(document):
    root = document.root
    if root.tag != "animation":
        raise FormatError("not an animation: <%s>" % root.tag)
    frames = []
    for i, frame in enumerate(root.findall("frame")):
        if frame.attrs.get("index") != str(i):
            raise FormatError("frame %d has index %s; the converter needs 0, 1, 2 ..." % (i, frame.attrs.get("index")))
        frames.append({t.attrs.get("name", ""): _matrix(t) for t in frame.findall("transform")})
    if not frames:
        raise FormatError("an animation without frames")
    return AnimationData(frames)


def load(text):
    """(kind, data, document) for the text of a mesh, skeleton or animation file."""
    document = parse(text)
    kind = document.root.tag
    reader = {"mesh": mesh_from, "skeleton": skeleton_from, "animation": animation_from}.get(kind)
    if reader is None:
        raise FormatError("not a mesh, skeleton or animation: <%s>" % kind)
    return kind, reader(document), document


# Writing --------------------------------------------------------------------------------------------------------------

def _open_tag(depth, tag, attributes, close):
    """The lines of a start tag in the standard layout. attributes: [(name, text)]; close: '>' or '/>'."""
    line = INDENT * depth + "<" + tag
    if not attributes:
        return [line + close]
    continuation = " " * (len(line) + 1)
    lines = []
    for i, (name, value) in enumerate(attributes):
        item = '%s="%s"' % (name, _escape(value))
        if i == len(attributes) - 1:
            item_width = len(item) + len(close)  # the closing bracket counts on the last attribute's line
        else:
            item_width = len(item)
        if i == 0:
            line += " " + item
        elif len(line) + 1 + item_width > MARGIN:
            lines.append(line)
            line = continuation + item
        else:
            line += " " + item
    lines.append(line + close)
    return lines


def _transform_lines(depth, name, matrix):
    return _open_tag(depth, "transform", [("name", name)] + [(a, fmt(v)) for a, v in zip(MATRIX_ATTRS, matrix)], "/>")


def _vertex_attributes(corner):
    names = VERTEX_ATTRS + tuple(n for n in UV2_ATTRS if n in corner.values)
    return [(name, fmt(corner.values[name])) for name in names]


def _skin_lines(depth, skins):
    lines = []
    for bone, weight in skins:
        lines += _open_tag(depth, "skin", [("bone", bone), ("weight", fmt(weight))], "/>")
    return lines


def _finish(lines, header, final_newline):
    return header + "\n".join(lines) + ("\n" if final_newline else "")


def write_mesh(mesh, header=None, final_newline=False):
    lines = ["<mesh>" if mesh.texture is None else '<mesh texture="%s">' % _escape(mesh.texture), INDENT + "<polygons>"]
    for i in range(0, len(mesh.corners), 3):
        lines.append(INDENT * 2 + "<polygon>")
        for corner in mesh.corners[i:i + 3]:
            lines += _open_tag(3, "vertex", _vertex_attributes(corner), ">")
            lines += _skin_lines(4, corner.skins)
            lines.append(INDENT * 3 + "</vertex>")
        lines.append(INDENT * 2 + "</polygon>")
    lines.append(INDENT + "</polygons>")
    if mesh.extra:
        lines.append(mesh.extra)
    lines.append("</mesh>")
    return _finish(lines, HEADERS["mesh"] if header is None else header, final_newline)


def write_skeleton(skeleton, header=None, final_newline=False, order=None):
    """order: the bone names in the order to write them; by default sorted, as validate_assets.py wants."""
    parents = dict(skeleton.bones)
    names = order or sorted(parents)
    lines = ["<skeleton>", INDENT + "<bones>"]
    for name in names:
        lines += _open_tag(2, "bone", [("name", name), ("parent", parents[name])], "/>")
    lines += [INDENT + "</bones>", INDENT + "<init_pose>"]
    for name in names:
        lines += _transform_lines(2, name, skeleton.pose[name])
    lines += [INDENT + "</init_pose>", "</skeleton>"]
    return _finish(lines, HEADERS["skeleton"] if header is None else header, final_newline)


def write_animation(animation, header=None, final_newline=False, order=None):
    lines = ["<animation>"]
    for index, frame in enumerate(animation.frames):
        lines.append(INDENT + '<frame index="%d">' % index)
        for name in (order if order and set(order) == set(frame) else sorted(frame)):
            lines += _transform_lines(2, name, frame[name])
        lines.append(INDENT + "</frame>")
    lines.append("</animation>")
    return _finish(lines, HEADERS["animation"] if header is None else header, final_newline)


def write(kind, data, **layout):
    return {"mesh": write_mesh, "skeleton": write_skeleton, "animation": write_animation}[kind](data, **layout)


def write_file(path, text):
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


# Keeping imported numbers ---------------------------------------------------------------------------------------------
#
# On export every number is computed from the Blender scene. When the data came from an imported file, each computed
# number within TOLERANCE of the imported one is replaced by the imported one, and when the structure is unchanged
# (same triangles, bones and frames) the imported file is patched in place instead of rewritten. Untouched data
# therefore exports byte for byte as it was imported, whatever its layout, and an edit changes only the numbers it
# touched. With nothing to keep (a new model, or "Keep imported numbers" off) files are written in the standard layout.

def _snap(value, source, tolerance):
    return source if abs(value - source) <= tolerance else value


def _snap_matrix(matrix, source):
    tolerance = TOLERANCE["matrix"]
    return tuple(_snap(v, s, tolerance) for v, s in zip(matrix, source))


def merged_skins(skins):
    """Weight per bone, adding the weights of a bone listed twice (as the converter does) and leaving out bones
    with no weight (the warrior has a few; they change nothing)."""
    merged = {}
    for bone, weight in skins:
        merged[bone] = merged.get(bone, 0.0) + weight
    return {bone: weight for bone, weight in merged.items() if abs(weight) >= 1e-9}


def _skins_match(a, b):
    a, b = merged_skins(a), merged_skins(b)
    return a.keys() == b.keys() and all(abs(a[k] - b[k]) <= TOLERANCE["weight"] for k in a)


def _normal(values):
    n = [values["nx"], values["ny"], values["nz"]]
    length = math.sqrt(sum(v * v for v in n))
    return [v / length for v in n] if length > 0 else n


def snap_corner(corner, source):
    """The corner with every value that matches `source` within tolerance replaced by the source value."""
    values = {}
    for name, value in corner.values.items():
        kind = ATTR_KIND[name]
        if name in source.values and kind != "normal":
            value = _snap(value, source.values[name], TOLERANCE[kind])
        values[name] = value
    if all(n in source.values for n in ("nx", "ny", "nz")):
        if all(abs(a - b) <= TOLERANCE["normal"] for a, b in zip(_normal(corner.values), _normal(source.values))):
            for name in ("nx", "ny", "nz"):
                values[name] = source.values[name]
    skins = list(source.skins) if _skins_match(corner.skins, source.skins) else list(corner.skins)
    return Corner(values, skins, corner.source)


def _patch(text, edits):
    out, position = [], 0
    for start, end, replacement in sorted(edits):
        out.append(text[position:start])
        out.append(replacement)
        position = end
    out.append(text[position:])
    return "".join(out)


def _value_edits(element, names, new, old):
    return [(*element.spans[name], fmt(new[name])) for name in names if new[name] != old[name]]


def export_mesh(mesh, source_text=None):
    """The text of a mesh file computed in Blender, keeping the numbers of `source_text` (the imported file) where
    they still match. mesh.corners[i].source tells which imported corner corner i was computed from."""
    if source_text is None:
        return write_mesh(mesh)
    document = parse(source_text)
    source = mesh_from(document)
    corners = [snap_corner(c, source.corners[c.source])
               if c.source is not None and 0 <= c.source < len(source.corners) else c for c in mesh.corners]
    same_shape = (len(corners) == len(source.corners) and mesh.texture == source.texture
                  and all(c.source == i and c.values.keys() == s.values.keys()
                          for i, (c, s) in enumerate(zip(corners, source.corners))))
    if not same_shape:
        return write_mesh(MeshData(corners, mesh.texture, source.extra), document.header, document.final_newline)
    edits = []
    vertices = [v for p in document.root.find("polygons").findall("polygon") for v in p.findall("vertex")]
    for vertex, corner, old in zip(vertices, corners, source.corners):
        edits += _value_edits(vertex, corner.values, corner.values, old.values)
        if corner.skins != old.skins:
            skins = vertex.findall("skin")
            depth = len(document.text[skins[0].start - 64:skins[0].start].rsplit("\n", 1)[-1]) // len(INDENT)
            lines = _skin_lines(depth, corner.skins)
            edits.append((skins[0].start, skins[-1].end, ("\n" + INDENT * depth).join(l.lstrip() for l in lines)))
    return _patch(document.text, edits)


def export_skeleton(skeleton, source_text=None):
    if source_text is None:
        return write_skeleton(skeleton)
    document = parse(source_text)
    source = skeleton_from(document)
    pose = {name: _snap_matrix(m, source.pose[name]) if name in source.pose else m
            for name, m in skeleton.pose.items()}
    if sorted(skeleton.bones) != sorted(source.bones) or pose.keys() != source.pose.keys():
        return write_skeleton(SkeletonData(skeleton.bones, pose), document.header, document.final_newline)
    edits = []
    for transform in document.root.find("init_pose").findall("transform"):
        name = transform.attrs["name"]
        edits += _value_edits(transform, MATRIX_ATTRS, dict(zip(MATRIX_ATTRS, pose[name])),
                              dict(zip(MATRIX_ATTRS, source.pose[name])))
    return _patch(document.text, edits)


def export_animation(animation, source_text=None):
    if source_text is None:
        return write_animation(animation)
    document = parse(source_text)
    source = animation_from(document)
    frames = [{name: _snap_matrix(m, old[name]) if name in old else m for name, m in frame.items()}
              for frame, old in zip(animation.frames, source.frames)] + animation.frames[len(source.frames):]
    first = document.root.find("frame")
    order = [t.attrs["name"] for t in first.findall("transform")] if first is not None else None
    if len(frames) != len(source.frames) or any(f.keys() != s.keys() for f, s in zip(frames, source.frames)):
        return write_animation(AnimationData(frames), document.header, document.final_newline, order)
    edits = []
    for element, frame, old in zip(document.root.findall("frame"), frames, source.frames):
        for transform in element.findall("transform"):
            name = transform.attrs["name"]
            edits += _value_edits(transform, MATRIX_ATTRS, dict(zip(MATRIX_ATTRS, frame[name])),
                                  dict(zip(MATRIX_ATTRS, old[name])))
    return _patch(document.text, edits)


def pack_source(text):
    """An imported file's text, compressed for keeping in a custom property."""
    return base64.b64encode(zlib.compress(text.replace("\r\n", "\n").encode("utf-8"), 9)).decode("ascii")


def unpack_source(packed):
    return zlib.decompress(base64.b64decode(packed)).decode("utf-8")


# Building Blender data from a file (plain Python parts) ---------------------------------------------------------------

def weld(corners):
    """Vertices for an editable mesh: corners at the same position with the same skin share a vertex, except where
    that would repeat a vertex within a triangle or repeat a triangle (both-sided surfaces), which Blender does not
    allow. Returns (positions, skins per vertex, triangles as vertex index triples)."""
    positions, skins, triangles = [], [], []
    by_key, seen = {}, set()

    def new_vertex(corner):
        positions.append((corner.values["x"], corner.values["y"], corner.values["z"]))
        skins.append(list(corner.skins))
        return len(positions) - 1

    for i in range(0, len(corners), 3):
        triangle = []
        for corner in corners[i:i + 3]:
            key = (corner.values["x"], corner.values["y"], corner.values["z"], tuple(corner.skins))
            vertex = by_key.get(key)
            if vertex is None or vertex in triangle:
                vertex = new_vertex(corner)
                by_key.setdefault(key, vertex)
            triangle.append(vertex)
        if frozenset(triangle) in seen:
            triangle = [new_vertex(corner) for corner in corners[i:i + 3]]
        seen.add(frozenset(triangle))
        triangles.append(tuple(triangle))
    return positions, skins, triangles


def bone_lengths(skeleton, default=0.1):
    """Display lengths for Blender bones: the distance along the bone to its farthest child, else half the parent's
    length. Lengths do not affect the export."""
    pose = skeleton.pose
    children = {}
    for name, parent in skeleton.bones:
        children.setdefault(parent, []).append(name)
    lengths = {}

    def head(name):
        return pose[name][12:15]

    for name, _ in skeleton.bones:
        axis, start = pose[name][0:3], head(name)
        reach = [sum((h - s) * a for h, s, a in zip(head(child), start, axis)) for child in children.get(name, [])]
        if reach and max(reach) > 1e-3:
            lengths[name] = max(reach)
    parents = dict(skeleton.bones)

    def fallback(name, depth=0):
        if name in lengths:
            return lengths[name]
        parent = parents.get(name, "")
        if parent in parents and depth < 64:
            return max(0.5 * fallback(parent, depth + 1), 0.02)
        return default

    return {name: fallback(name) for name, _ in skeleton.bones}


def bone_order(skeleton):
    """Bone names with every parent before its children."""
    parents = dict(skeleton.bones)
    order, placed = [], set()

    def place(name, depth=0):
        if name in placed or depth > len(parents):
            return
        parent = parents.get(name, "")
        if parent in parents:
            place(parent, depth + 1)
        placed.add(name)
        order.append(name)

    for name, _ in skeleton.bones:
        place(name)
    return order


def animation_keys(skeleton, rest, animation):
    """Location, rotation and scale keys per bone for every frame: {bone: [(location, quaternion, scale)]}.
    rest: bone name -> Blender rest matrix (armature space, 4x4 rows) as Blender built it from the skeleton."""
    parents = dict(skeleton.bones)
    keys = {name: [] for name in parents}
    for frame in animation.frames:
        pose = {name: to_blender(frame[name]) for name in parents if name in frame}
        for name in parents:
            if name not in pose:
                pose[name] = rest[name]  # a bone the animation leaves out stays at rest
        for name in parents:
            parent = parents[name]
            if parent in parents:
                try:
                    basis = pose_basis(rest[name], pose[name], rest[parent], pose[parent])
                except ValueError:
                    raise FormatError("bone %s: its parent %s is scaled to nothing" % (name, parent)) from None
            else:
                basis = pose_basis(rest[name], pose[name])
            location, quaternion, scale = decompose(basis)
            previous = keys[name][-1][1] if keys[name] else None
            if previous and sum(a * b for a, b in zip(previous, quaternion)) < 0:
                quaternion = tuple(-v for v in quaternion)  # keep neighbouring keys on the same side
            keys[name].append((location, quaternion, scale))
    return keys


# Sprites in geometry.xml ----------------------------------------------------------------------------------------------

@dataclass
class SpriteEntry:
    group: str
    name: str
    scale: float = 1.0
    skeleton: str = None  # path relative to geometry.xml
    models: list = field(default_factory=list)  # [(path, (r, g, b), [(texture, team or None)])]
    animations: list = field(default_factory=list)  # [(name, wpc, type, path)]


def read_sprites(geometry_xml_text):
    """The sprites listed in geometry.xml, in order."""
    document = parse(geometry_xml_text)
    text = document.text
    sprites = []
    for group in document.root.findall("group"):
        for sprite in group.findall("sprite"):
            entry = SpriteEntry(group.attrs.get("name", ""), sprite.attrs.get("name", ""),
                                float(sprite.attrs.get("scale", "1")))
            skeleton = sprite.find("skeleton")
            if skeleton is not None:
                entry.skeleton = skeleton.text(text)
            for model in sprite.findall("model"):
                colour = tuple(int(model.attrs.get(c, "0")) for c in "rgb")
                textures = [(t.attrs.get("name", ""), t.attrs.get("team")) for t in model.findall("texture")]
                entry.models.append((model.text(text), colour, textures))
            for animation in sprite.findall("animation"):
                entry.animations.append((animation.attrs.get("name", ""), float(animation.attrs.get("wpc", "1")),
                                         animation.attrs.get("type", "loop"), animation.text(text)))
            sprites.append(entry)
    return sprites


def sprite_snippet(name, skeleton_path, models, animations, scale=1.0):
    """A <sprite> entry for geometry.xml. models: [(path, (r, g, b), [(texture, team)])]; animations:
    [(name, wpc, type, path)] in the order the game expects (it picks animations by position)."""
    head = '<sprite name="%s"%s>' % (name, "" if scale == 1.0 else ' scale="%s"' % fmt(scale))
    lines = [head]
    if skeleton_path:
        lines.append(INDENT + "<skeleton>%s</skeleton>" % skeleton_path)
    for path, colour, textures in models:
        lines.append(INDENT + '<model r="%d" g="%d" b="%d">' % colour)
        lines.append(INDENT * 2 + path)
        for texture, team in textures:
            lines.append(INDENT * 2 + ('<texture name="%s"/>' % texture if not team else
                                       '<texture name="%s" team="%s"/>' % (texture, team)))
        lines.append(INDENT + "</model>")
    for anim_name, wpc, anim_type, path in animations:
        lines.append(INDENT + '<animation name="%s" wpc="%s" type="%s">%s</animation>'
                     % (anim_name, fmt(wpc), anim_type, path))
    lines.append("</sprite>")
    return "\n".join(lines) + "\n"


# Comparing files ------------------------------------------------------------------------------------------------------

_FORMAT_ATTRS = set(VERTEX_ATTRS + UV2_ATTRS + MATRIX_ATTRS + ("weight", "scale", "wpc"))


def _format_assets_value(match):
    name, value = match.group(1), match.group(2)
    if name not in _FORMAT_ATTRS:
        return match.group(0)
    try:
        number = float(value)
    except ValueError:
        return match.group(0)
    return '%s="%s"' % (name, "0" if abs(number) < 1e-9 else "{:.9g}".format(number))


def normalise(text):
    """A file as tools/scripts/format_assets.py would leave it, with LF line endings: what "byte-equivalent after
    formatting" compares."""
    return re.sub(r'(\w+)="([^"]+)"', _format_assets_value, text.replace("\r\n", "\n"))


@dataclass
class Comparison:
    identical: bool  # the same bytes once line endings are normalised
    formatted: bool  # the same after normalise()
    same_structure: bool
    deviation: dict  # value kind -> largest absolute difference
    problems: list  # structural differences, or the first differing line
    within_tolerance: bool = False


def _first_difference(a, b):
    for number, (x, y) in enumerate(zip(a.split("\n"), b.split("\n")), 1):
        if x != y:
            return "line %d: %r != %r" % (number, x.strip()[:100], y.strip()[:100])
    return "lengths differ: %d and %d lines" % (a.count("\n") + 1, b.count("\n") + 1)


def compare(original_text, exported_text):
    a, b = original_text.replace("\r\n", "\n"), exported_text.replace("\r\n", "\n")
    identical = a == b
    formatted = identical or normalise(a) == normalise(b)
    deviation, problems = {}, []

    def note(kind, x, y):
        deviation[kind] = max(deviation.get(kind, 0.0), abs(x - y))

    try:
        kind_a, data_a, _ = load(a)
        kind_b, data_b, _ = load(b)
    except FormatError as e:
        return Comparison(identical, formatted, False, {}, [str(e)])
    if kind_a != kind_b:
        problems.append("<%s> and <%s>" % (kind_a, kind_b))
    elif kind_a == "mesh":
        if len(data_a.corners) != len(data_b.corners):
            problems.append("%d and %d corners" % (len(data_a.corners), len(data_b.corners)))
        for i, (x, y) in enumerate(zip(data_a.corners, data_b.corners)):
            if x.values.keys() != y.values.keys():
                problems.append("corner %d: attributes %s and %s" % (i, sorted(x.values), sorted(y.values)))
                break
            for name in x.values:
                if ATTR_KIND[name] != "normal":
                    note(ATTR_KIND[name], x.values[name], y.values[name])
            for p, q in zip(_normal(x.values), _normal(y.values)):
                note("normal", p, q)
            mx, my = merged_skins(x.skins), merged_skins(y.skins)
            if mx.keys() != my.keys():
                problems.append("corner %d: bones %s and %s" % (i, sorted(mx), sorted(my)))
                break
            for bone in mx:
                note("weight", mx[bone], my[bone])
    elif kind_a == "skeleton":
        if sorted(data_a.bones) != sorted(data_b.bones):
            problems.append("different bones or parents")
        for name in data_a.pose.keys() & data_b.pose.keys():
            for p, q in zip(data_a.pose[name], data_b.pose[name]):
                note("matrix", p, q)
    else:
        if len(data_a.frames) != len(data_b.frames):
            problems.append("%d and %d frames" % (len(data_a.frames), len(data_b.frames)))
        for i, (x, y) in enumerate(zip(data_a.frames, data_b.frames)):
            if x.keys() != y.keys():
                problems.append("frame %d: different bones" % i)
                break
            for name in x:
                for p, q in zip(x[name], y[name]):
                    note("matrix", p, q)
    same = not problems
    within = same and all(v <= TOLERANCE[k] for k, v in deviation.items())
    if not formatted and not problems:
        problems.append(_first_difference(normalise(a), normalise(b)))
    return Comparison(identical, formatted, same, deviation, problems, within)


def _pairs(original, exported):
    if os.path.isdir(original):
        names = sorted(n for n in os.listdir(exported) if n.endswith(".xml"))
        return [(os.path.join(original, n), os.path.join(exported, n)) for n in names]
    return [(original, exported)]


def main(argv):
    """Command line: diff and format (see the module docstring)."""
    if len(argv) >= 3 and argv[0] == "diff":
        tolerant = "--tolerance" in argv
        paths = [a for a in argv[1:] if not a.startswith("--")]
        failed = 0
        for original, exported in _pairs(*paths):
            if not os.path.exists(original):
                print("%s: no original" % os.path.basename(exported))
                failed += 1
                continue
            result = compare(read_text(original), read_text(exported))
            if result.identical:
                state = "identical"
            elif result.formatted:
                state = "identical after formatting"
            elif result.within_tolerance:
                state = "within tolerance"
            else:
                state = "DIFFERENT"
            worst = ", ".join("%s %.1e" % (k, v) for k, v in sorted(result.deviation.items()) if v > 0)
            print("%-40s %s%s" % (os.path.basename(exported), state, " (largest difference: %s)" % worst if worst
                                  and not result.formatted else ""))
            for problem in result.problems[:3]:
                print("    " + problem)
            if not (result.formatted or (tolerant and result.within_tolerance)):
                failed += 1
        return 1 if failed else 0
    if len(argv) >= 2 and argv[0] == "format":
        for path in argv[1:]:
            kind, data, document = load(read_text(path))
            order = None
            if kind == "animation":
                first = document.root.find("frame")
                order = [t.attrs["name"] for t in first.findall("transform")]
            layout = {"header": HEADERS[kind], "final_newline": False}
            if order:
                layout["order"] = order
            write_file(path, write(kind, data, **layout))
            print("formatted %s" % path)
        return 0
    print(__doc__)
    return 2


# ---------------------------------------------------------------------------------------------------------------------
# The Blender add-on
# ---------------------------------------------------------------------------------------------------------------------

try:
    import bpy
    from bpy.props import BoolProperty, EnumProperty, FloatProperty, IntProperty, StringProperty
    from bpy_extras.io_utils import ExportHelper, ImportHelper
except ImportError:  # plain Python: the format half above still works
    bpy = None

SOURCE_KEY = "_tt_source"  # an imported file's text (pack_source), hidden from the Custom Properties panel
MODEL_KEY = "_tt_model"  # the <model> colour and textures from geometry.xml, for the <sprite> snippet
TEXTURE_KEY = "tt_texture"  # the <mesh texture> attribute (shown in Custom Properties so it can be edited)
CORNER_ATTRIBUTE = "tt_corner"  # per face corner: the index of the imported corner it came from


if bpy is not None:

    def _rows(matrix):
        return [list(row) for row in matrix]

    def _fcurve_collections(action):
        """The F-curve collections of an action: its channelbags (layered actions, Blender 4.4 and newer) or its
        own list (older Blender)."""
        layers = getattr(action, "layers", None)
        if layers is not None and len(layers) > 0:
            for layer in layers:
                for strip in layer.strips:
                    for bag in getattr(strip, "channelbags", ()):
                        yield bag.fcurves
        elif getattr(action, "fcurves", None) is not None:
            yield action.fcurves

    def _find_fcurve(action, data_path, index):
        for fcurves in _fcurve_collections(action):
            curve = fcurves.find(data_path, index=index)
            if curve is not None:
                return curve
        return None

    _BONE_PATH = re.compile(r'pose\.bones\["((?:[^"\\]|\\.)*)"\]')

    def actions_for(armature_object):
        """The actions that animate this armature's bones, in their Tribal Trouble order, then by name."""
        bones = set(armature_object.data.bones.keys())
        found = []
        for action in bpy.data.actions:
            names = set()
            for fcurves in _fcurve_collections(action):
                for curve in fcurves:
                    match = _BONE_PATH.match(curve.data_path)
                    if match:
                        names.add(match.group(1))
            if names and names <= bones:
                found.append(action)
        return sorted(found, key=lambda a: (a.tt_order, a.name))

    def _assign_action(armature_object, action):
        data = armature_object.animation_data or armature_object.animation_data_create()
        data.action = action
        if hasattr(data, "action_slot") and data.action_slot is None and len(getattr(action, "slots", ())) > 0:
            data.action_slot = action.slots[0]

    def _textures_folder(geometry_xml):
        return os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(geometry_xml))), "textures", "models")

    def _material(texture_name, textures_folder):
        material = bpy.data.materials.get(texture_name)
        if material is not None:
            return material
        material = bpy.data.materials.new(texture_name)
        path = os.path.join(textures_folder, texture_name + ".png") if textures_folder else None
        if path and os.path.exists(path):
            try:
                material.use_nodes = True
            except AttributeError:
                pass
            nodes = material.node_tree.nodes
            shader = next((n for n in nodes if n.type == "BSDF_PRINCIPLED"), None)
            image = nodes.new("ShaderNodeTexImage")
            image.image = bpy.data.images.load(path, check_existing=True)
            if shader is not None:
                material.node_tree.links.new(image.outputs["Color"], shader.inputs["Base Color"])
        return material

    # Import ------------------------------------------------------------------------------------------------------

    def import_skeleton(context, path, name=None):
        text = read_text(path)
        kind, skeleton, _ = load(text)
        if kind != "skeleton":
            raise FormatError("%s is not a skeleton" % path)
        name = name or os.path.splitext(os.path.basename(path))[0]
        armature = bpy.data.armatures.new(name)
        armature[SOURCE_KEY] = pack_source(text)
        obj = bpy.data.objects.new(name, armature)
        context.collection.objects.link(obj)
        for other in context.selected_objects:
            other.select_set(False)
        obj.select_set(True)
        context.view_layer.objects.active = obj
        lengths = bone_lengths(skeleton)
        bpy.ops.object.mode_set(mode="EDIT")
        try:
            for bone_name in bone_order(skeleton):
                bone = armature.edit_bones.new(bone_name)
                bone.head = (0.0, 0.0, 0.0)
                bone.tail = (0.0, lengths[bone_name], 0.0)
                bone.matrix = _mathutils_matrix(to_blender(skeleton.pose[bone_name]))
            for bone_name, parent in skeleton.bones:
                if parent in armature.edit_bones:
                    armature.edit_bones[bone_name].parent = armature.edit_bones[parent]
                    armature.edit_bones[bone_name].use_connect = False
        finally:
            bpy.ops.object.mode_set(mode="OBJECT")
        armature.display_type = "STICK"
        return obj, skeleton

    def _mathutils_matrix(rows):
        from mathutils import Matrix
        return Matrix(rows)

    def import_mesh(context, path, armature_object=None, name=None, model=None, textures_folder=None):
        text = read_text(path)
        kind, mesh, _ = load(text)
        if kind != "mesh":
            raise FormatError("%s is not a mesh" % path)
        name = name or os.path.splitext(os.path.basename(path))[0]
        positions, skins, triangles = weld(mesh.corners)
        me = bpy.data.meshes.new(name)
        me.from_pydata(positions, [], triangles)
        me[SOURCE_KEY] = pack_source(text)
        if mesh.texture is not None:
            me[TEXTURE_KEY] = mesh.texture
        if model is not None:
            me[MODEL_KEY] = repr(model)
        corners = mesh.corners
        me.polygons.foreach_set("use_smooth", [True] * len(triangles))
        uv = me.uv_layers.new(name="UVMap")
        uv.data.foreach_set("uv", [v for c in corners for v in (c.values["u"], c.values["v"])])
        if all("u2" in c.values and "v2" in c.values for c in corners):
            uv2 = me.uv_layers.new(name="UVMap2")
            uv2.data.foreach_set("uv", [v for c in corners for v in (c.values["u2"], c.values["v2"])])
        colours = me.color_attributes.new("Col", "FLOAT_COLOR", "CORNER")
        colours.data.foreach_set("color", [c.values[k] for c in corners for k in ("r", "g", "b", "a")])
        index = me.attributes.new(CORNER_ATTRIBUTE, "INT", "CORNER")
        index.data.foreach_set("value", list(range(len(corners))))
        me.normals_split_custom_set([_normal(c.values) for c in corners])
        me.update()
        obj = bpy.data.objects.new(name, me)
        context.collection.objects.link(obj)
        if armature_object is not None and any(b != DUMMY_BONE for s in skins for b, _ in s):
            groups = {}
            for vertex, vertex_skins in enumerate(skins):
                for bone, weight in vertex_skins:
                    if bone not in groups:
                        groups[bone] = obj.vertex_groups.new(name=bone)
                    groups[bone].add([vertex], weight, "ADD")
            obj.parent = armature_object
            modifier = obj.modifiers.new("Armature", "ARMATURE")
            modifier.object = armature_object
        if model is not None and model[2]:
            me.materials.append(_material(model[2][0][0], textures_folder))
        return obj

    def import_animation(context, path, armature_object, skeleton, name=None, info=None):
        """Import an animation onto an armature made by import_skeleton. info: (name, wpc, type, order)."""
        text = read_text(path)
        kind, animation, _ = load(text)
        if kind != "animation":
            raise FormatError("%s is not an animation" % path)
        name = name or os.path.splitext(os.path.basename(path))[0]
        rest = {bone.name: _rows(bone.matrix_local) for bone in armature_object.data.bones}
        keys = animation_keys(skeleton, rest, animation)
        action = bpy.data.actions.new(name)
        action.use_fake_user = True
        action[SOURCE_KEY] = pack_source(text)
        if info is not None:
            action.tt_name, action.tt_wpc, action.tt_type, action.tt_order = info
        _assign_action(armature_object, action)
        count = len(animation.frames)
        for bone_name, bone_keys in keys.items():
            pose_bone = armature_object.pose.bones[bone_name]
            pose_bone.rotation_mode = "QUATERNION"
            location, quaternion, scale = bone_keys[0]
            pose_bone.location, pose_bone.rotation_quaternion, pose_bone.scale = location, quaternion, scale
            channels = (("location", 3, 0), ("rotation_quaternion", 4, 1), ("scale", 3, 2))
            for prop, _, _ in channels:
                pose_bone.keyframe_insert(prop, frame=0, group=bone_name)
            for prop, size, part in channels:
                path = 'pose.bones["%s"].%s' % (bpy.utils.escape_identifier(bone_name), prop)
                for i in range(size):
                    curve = _find_fcurve(action, path, i)
                    points = curve.keyframe_points
                    points.add(count - len(points))
                    points.foreach_set("co", [v for f in range(count) for v in (f, bone_keys[f][part][i])])
                    for point in points:
                        point.interpolation = "LINEAR"
                    curve.update()
        action.use_frame_range = True
        action.frame_start, action.frame_end = 0, max(count - 1, 1)
        return action

    def import_sprite(context, geometry_xml, group, sprite_name):
        """Import a sprite listed in geometry.xml: its skeleton, models and animations."""
        entry = next((s for s in read_sprites(read_text(geometry_xml)) if s.group == group and s.name == sprite_name),
                     None)
        if entry is None:
            raise FormatError("no sprite %s/%s in %s" % (group, sprite_name, geometry_xml))
        base = os.path.dirname(os.path.abspath(geometry_xml))
        textures = _textures_folder(geometry_xml)
        armature_object, skeleton = None, None
        if entry.skeleton:
            armature_object, skeleton = import_skeleton(context, os.path.join(base, entry.skeleton))
            armature_object["tt_sprite"] = "%s/%s" % (group, sprite_name)
        meshes = []
        for path, colour, texture_list in entry.models:
            full = os.path.join(base, path)
            name = os.path.splitext(os.path.basename(path))[0]
            if name in bpy.data.objects and bpy.data.objects[name].get("tt_sprite_model") == full:
                continue  # the same file twice (a sprite without a low-poly model)
            obj = import_mesh(context, full, armature_object, model=(path, colour, texture_list),
                              textures_folder=textures)
            obj["tt_sprite_model"] = full
            meshes.append(obj)
        actions = []
        for order, (anim_name, wpc, anim_type, path) in enumerate(entry.animations):
            actions.append(import_animation(context, os.path.join(base, path), armature_object, skeleton,
                                            info=(anim_name, wpc, anim_type, order)))
        if armature_object is not None and actions:
            _assign_action(armature_object, actions[0])
        return armature_object, meshes, actions

    # Export ------------------------------------------------------------------------------------------------------

    def _armature_of(mesh_object):
        for modifier in mesh_object.modifiers:
            if modifier.type == "ARMATURE" and modifier.object is not None:
                return modifier.object
        return None

    def meshes_for(armature_object):
        """The meshes bound to an armature, most triangles first: the game takes the first model of a sprite as
        the high-detail one and the second as the low-detail one."""
        found = [o for o in bpy.data.objects if o.type == "MESH" and _armature_of(o) is armature_object]
        return sorted(found, key=lambda o: (-sum(len(p.vertices) - 2 for p in o.data.polygons), o.name))

    def _snippet_prefix(folder):
        """Paths in geometry.xml are relative to its folder; without one above `folder`, use the folder's name."""
        folder = os.path.abspath(folder)
        here = folder
        while True:
            if os.path.exists(os.path.join(here, "geometry.xml")):
                return os.path.relpath(folder, here).replace(os.sep, "/")
            parent = os.path.dirname(here)
            if parent == here:
                return os.path.basename(folder)
            here = parent

    def skeleton_data(armature_object):
        bones = [(b.name, b.parent.name if b.parent else "") for b in armature_object.data.bones]
        pose = {b.name: from_blender(_rows(b.matrix_local)) for b in armature_object.data.bones}
        return SkeletonData(bones, pose)

    def mesh_data(context, mesh_object, report):
        """The corners of a mesh object in its armature's space (or its own space without one), triangulated."""
        armature_object = _armature_of(mesh_object)
        others = [m for m in mesh_object.modifiers if m.type != "ARMATURE" and m.show_viewport]
        evaluated = None
        if others:
            hidden = [m for m in mesh_object.modifiers if m.type == "ARMATURE" and m.show_viewport]
            for m in hidden:
                m.show_viewport = False
            try:
                context.view_layer.update()
                evaluated = mesh_object.evaluated_get(context.evaluated_depsgraph_get())
                me = evaluated.to_mesh()
            finally:
                for m in hidden:
                    m.show_viewport = True
        else:
            me = mesh_object.data
        try:
            space = None
            if armature_object is not None:
                space = armature_object.matrix_world.inverted() @ mesh_object.matrix_world
            if space is not None and all(abs(space[r][c] - (r == c)) < 1e-12 for r in range(4) for c in range(4)):
                space = None
            normal_space = space.to_3x3().inverted().transposed() if space is not None else None
            me.calc_loop_triangles()
            uv_layers = list(me.uv_layers)
            colours = me.color_attributes.active_color or (me.color_attributes[0] if len(me.color_attributes) else
                                                           None)
            corner_index = me.attributes.get(CORNER_ATTRIBUTE)
            if corner_index is not None and (corner_index.domain != "CORNER" or corner_index.data_type != "INT"):
                corner_index = None
            groups = {g.index: g.name for g in mesh_object.vertex_groups}
            bones = set(armature_object.data.bones.keys()) if armature_object is not None else set()
            root = next((b.name for b in armature_object.data.bones if b.parent is None), None) \
                if armature_object is not None else None
            normals = me.corner_normals
            unweighted = 0
            corners = []
            for triangle in me.loop_triangles:
                for loop in triangle.loops:
                    vertex = me.vertices[me.loops[loop].vertex_index]
                    position = vertex.co if space is None else space @ vertex.co
                    normal = normals[loop].vector if normal_space is None else (normal_space @ normals[loop].vector)
                    normal = normal.normalized()
                    values = {"x": position[0], "y": position[1], "z": position[2]}
                    if colours is None:
                        colour = (1.0, 1.0, 1.0, 1.0)
                    elif colours.domain == "CORNER":
                        colour = tuple(colours.data[loop].color)
                    else:
                        colour = tuple(colours.data[me.loops[loop].vertex_index].color)
                    values.update(zip("rgba", colour))
                    values.update(nx=normal[0], ny=normal[1], nz=normal[2])
                    u, v = uv_layers[0].data[loop].uv if uv_layers else (0.0, 0.0)
                    values.update(u=u, v=v)
                    if len(uv_layers) > 1:
                        values["u2"], values["v2"] = uv_layers[1].data[loop].uv
                    if armature_object is None:
                        skins = [(DUMMY_BONE, 1.0)]
                    else:
                        skins = [(groups[g.group], g.weight) for g in vertex.groups
                                 if g.weight > 0 and groups.get(g.group) in bones]
                        total = sum(w for _, w in skins)
                        if not skins:
                            unweighted += 1
                            skins = [(root, 1.0)]
                        elif abs(total - 1.0) > 1e-6:
                            skins = [(b, w / total) for b, w in skins]
                    source = corner_index.data[loop].value if corner_index is not None else None
                    corners.append(Corner(values, skins, source))
            if unweighted:
                report({"WARNING"}, "%s: %d corners had no bone weights and were given to %s"
                       % (mesh_object.name, unweighted, root))
            return MeshData(corners, mesh_object.data.get(TEXTURE_KEY))
        finally:
            if evaluated is not None:
                evaluated.to_mesh_clear()

    def animation_data(context, armature_object, action):
        scene = context.scene
        data = armature_object.animation_data
        saved = (data.action if data else None, scene.frame_current, armature_object.data.pose_position)
        _assign_action(armature_object, action)
        armature_object.data.pose_position = "POSE"
        start, end = (int(round(v)) for v in action.frame_range)
        frames = []
        try:
            for frame in range(start, end + 1):
                scene.frame_set(frame)
                frames.append({b.name: from_blender(_rows(b.matrix)) for b in armature_object.pose.bones})
        finally:
            if saved[0] is not None:
                _assign_action(armature_object, saved[0])
            scene.frame_set(saved[1])
            armature_object.data.pose_position = saved[2]
        return AnimationData(frames)

    def _source(datablock, keep):
        packed = datablock.get(SOURCE_KEY) if keep else None
        return unpack_source(packed) if packed else None

    def export_objects(context, objects, folder, keep=True, report=print):
        """Export armatures (skeleton, bound meshes, animations) and meshes into `folder`, each file named after
        its object or action. Returns {file name: text written} and the <sprite> snippets."""
        written, snippets = {}, []

        def put(file_name, text):
            write_file(os.path.join(folder, file_name), text)
            written[file_name] = text

        done = set()
        prefix = _snippet_prefix(folder)
        for obj in objects:
            if obj.type != "ARMATURE":
                continue
            put(obj.name + ".xml", export_skeleton(skeleton_data(obj), _source(obj.data, keep)))
            models = []
            for mesh_object in meshes_for(obj):
                put(mesh_object.name + ".xml", export_mesh(mesh_data(context, mesh_object, report),
                                                           _source(mesh_object.data, keep)))
                done.add(mesh_object.name)
                models.append(_model_entry(mesh_object, prefix))
            animations = []
            for action in actions_for(obj):
                put(action.name + ".xml", export_animation(animation_data(context, obj, action),
                                                           _source(action, keep)))
                animations.append((action.tt_name or action.name, action.tt_wpc, action.tt_type,
                                   "%s/%s.xml" % (prefix, action.name)))
            sprite = obj.get("tt_sprite", obj.name).split("/")[-1]
            snippets.append(sprite_snippet(sprite, "%s/%s.xml" % (prefix, obj.name), models, animations))
        for obj in objects:
            if obj.type == "MESH" and obj.name not in done:
                put(obj.name + ".xml", export_mesh(mesh_data(context, obj, report), _source(obj.data, keep)))
                snippets.append(sprite_snippet(obj.name, None, [_model_entry(obj, prefix)], []))
        return written, snippets

    def _model_entry(mesh_object, prefix):
        path = "%s/%s.xml" % (prefix, mesh_object.name)
        stored = mesh_object.data.get(MODEL_KEY)
        if stored:
            try:
                _, colour, textures = ast.literal_eval(stored)
                return path, tuple(colour), [tuple(t) for t in textures]
            except (SyntaxError, ValueError, TypeError):
                pass
        textures = [(m.name, None) for m in mesh_object.data.materials if m is not None][:1]
        return path, (90, 60, 30), textures

    # Operators and panel -----------------------------------------------------------------------------------------

    class TT_OT_import(bpy.types.Operator, ImportHelper):
        """Import a Tribal Trouble skeleton, mesh or animation file, or a sprite from geometry.xml"""
        bl_idname = "import_scene.tribal_trouble"
        bl_label = "Import Tribal Trouble"
        bl_options = {"REGISTER", "UNDO"}
        filename_ext = ".xml"
        filter_glob: StringProperty(default="*.xml", options={"HIDDEN"})
        sprite: StringProperty(
            name="Sprite", default="natives/warrior",
            description="For geometry.xml: the group and sprite to import, as group/sprite")

        def execute(self, context):
            try:
                root = parse(read_text(self.filepath)).root.tag
                if root == "geometry":
                    group, _, name = self.sprite.partition("/")
                    import_sprite(context, self.filepath, group, name)
                elif root == "skeleton":
                    import_skeleton(context, self.filepath)
                elif root == "mesh":
                    active = context.active_object
                    import_mesh(context, self.filepath, active if active and active.type == "ARMATURE" else None)
                elif root == "animation":
                    active = context.active_object
                    if active is None or active.type != "ARMATURE" or SOURCE_KEY not in active.data:
                        raise FormatError("select the armature (imported from its skeleton file) first")
                    _, skeleton, _ = load(unpack_source(active.data[SOURCE_KEY]))
                    import_animation(context, self.filepath, active, skeleton)
                else:
                    raise FormatError("<%s> is not a Tribal Trouble geometry file" % root)
            except (FormatError, OSError) as e:
                self.report({"ERROR"}, str(e))
                return {"CANCELLED"}
            return {"FINISHED"}

    class TT_OT_export(bpy.types.Operator):
        """Export the selected armatures (skeleton, meshes and animations) and meshes as Tribal Trouble files"""
        bl_idname = "export_scene.tribal_trouble"
        bl_label = "Export Tribal Trouble"
        bl_options = {"REGISTER"}
        directory: StringProperty(subtype="DIR_PATH")
        filter_folder: BoolProperty(default=True, options={"HIDDEN"})
        keep_numbers: BoolProperty(
            name="Keep imported numbers", default=True,
            description="Write imported values unchanged where the scene still matches them, so a re-export "
                        "changes only what was edited")

        def invoke(self, context, event):
            context.window_manager.fileselect_add(self)
            return {"RUNNING_MODAL"}

        def execute(self, context):
            objects = [o for o in context.selected_objects if o.type in ("ARMATURE", "MESH")]
            if not objects:
                self.report({"ERROR"}, "select an armature or a mesh")
                return {"CANCELLED"}
            try:
                written, snippets = export_objects(context, objects, self.directory, self.keep_numbers, self.report)
            except (FormatError, OSError, ValueError) as e:
                self.report({"ERROR"}, str(e))
                return {"CANCELLED"}
            text = bpy.data.texts.get("tt_sprite.xml") or bpy.data.texts.new("tt_sprite.xml")
            text.clear()
            text.write("".join(snippets))
            self.report({"INFO"}, "Wrote %d files to %s; the <sprite> entry is in the text tt_sprite.xml"
                        % (len(written), self.directory))
            return {"FINISHED"}

    class TT_PT_action(bpy.types.Panel):
        """Tribal Trouble settings of the armature's current action"""
        bl_label = "Tribal Trouble animation"
        bl_space_type = "PROPERTIES"
        bl_region_type = "WINDOW"
        bl_context = "data"

        @classmethod
        def poll(cls, context):
            obj = context.object
            return obj is not None and obj.type == "ARMATURE"

        def draw(self, context):
            data = context.object.animation_data
            action = data.action if data else None
            if action is None:
                self.layout.label(text="No action")
                return
            self.layout.label(text=action.name)
            self.layout.prop(action, "tt_name")
            self.layout.prop(action, "tt_order")
            self.layout.prop(action, "tt_wpc")
            self.layout.prop(action, "tt_type")

    def _menu_import(self, context):
        self.layout.operator(TT_OT_import.bl_idname, text="Tribal Trouble (.xml)")

    def _menu_export(self, context):
        self.layout.operator(TT_OT_export.bl_idname, text="Tribal Trouble (.xml)")

    _CLASSES = (TT_OT_import, TT_OT_export, TT_PT_action)

    def register():
        bpy.types.Action.tt_name = StringProperty(
            name="Name", description="The animation's name in geometry.xml (idle, run, attack, die, sit ...)")
        bpy.types.Action.tt_order = IntProperty(
            name="Position", min=0, description="Its position in the sprite: the game picks animations by position "
                                                "(0 idle, 1 run, 2 attack, 3 die, then 4 sit or magic)")
        bpy.types.Action.tt_wpc = FloatProperty(
            name="Distance per cycle", default=1.0, min=0.001,
            description="wpc: metres walked per cycle for run animations, 1 for the others")
        bpy.types.Action.tt_type = EnumProperty(
            name="Type", default="loop",
            items=(("loop", "Loop", "Repeats"), ("plain", "Plain", "Plays once (attack, die)")))
        for cls in _CLASSES:
            bpy.utils.register_class(cls)
        bpy.types.TOPBAR_MT_file_import.append(_menu_import)
        bpy.types.TOPBAR_MT_file_export.append(_menu_export)

    def unregister():
        bpy.types.TOPBAR_MT_file_export.remove(_menu_export)
        bpy.types.TOPBAR_MT_file_import.remove(_menu_import)
        for cls in reversed(_CLASSES):
            bpy.utils.unregister_class(cls)
        for name in ("tt_name", "tt_order", "tt_wpc", "tt_type"):
            delattr(bpy.types.Action, name)


if __name__ == "__main__" and bpy is None:
    sys.exit(main(sys.argv[1:]))
