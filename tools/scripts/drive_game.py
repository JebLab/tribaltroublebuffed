"""Drive the running game's window for in-game checks: clicks, keys, screenshots of the game window only.

Coordinates are in the game window's client area (the game runs windowed, 1024x768 by default), the same pixels
as the screenshots this saves. Clicks and keys move the real mouse and keyboard focus, so a session uses this only
when Josh allows it (docs/SESSION_RULES.md section 8). Standard library only; run it with the Windows Python.

  python tools/scripts/drive_game.py shot <name>         save %TEMP%\\tt-game\\<name>.png
  python tools/scripts/drive_game.py zoom <name> <x> <y> <w> <h>   save that part of the window, three times larger
  python tools/scripts/drive_game.py click <x> <y>       left click
  python tools/scripts/drive_game.py rclick <x> <y>      right click (orders selected units)
  python tools/scripts/drive_game.py move <x> <y>        move the cursor (e.g. off a button before a screenshot)
  python tools/scripts/drive_game.py key <key>           tap a key: esc, enter, space, tab, f1..f12, a..z, 0..9
  python tools/scripts/drive_game.py type <letters>      tap each of a..z, 0..9, / in turn (chat commands)
  python tools/scripts/drive_game.py wait <seconds>      sleep (between steps that load)

Several steps can be chained in one call: drive_game.py click 87 334 wait 1 shot menu
Each step prints one JSON line.
"""
import ctypes
import ctypes.wintypes as wt
import json
import os
import struct
import sys
import time
import zlib

TITLE = "Tribal Trouble"
OUT = os.path.join(os.environ.get("TEMP", "."), "tt-game")
KEYS = {"esc": 0x1B, "enter": 0x0D, "space": 0x20, "tab": 0x09, "backspace": 0x08,
        "left": 0x25, "up": 0x26, "right": 0x27, "down": 0x28}
KEYS.update({f"f{n}": 0x6F + n for n in range(1, 13)})
KEYS.update({chr(c): c - 32 for c in range(ord("a"), ord("z") + 1)})
KEYS.update({str(d): 0x30 + d for d in range(10)})
KEYS["/"] = 0xBF  # VK_OEM_2: the slash key on a US layout, for chat commands such as /iamacheater

user32 = ctypes.WinDLL("user32", use_last_error=True)
gdi32 = ctypes.WinDLL("gdi32", use_last_error=True)
user32.SetProcessDPIAware()


def game_window():
    hwnd = user32.FindWindowW(None, TITLE)
    if not hwnd:
        raise SystemExit(json.dumps({"ok": False, "error": f"no window titled {TITLE!r}; is the game running?"}))
    return hwnd


def client_box(hwnd):
    rect = wt.RECT()
    user32.GetClientRect(hwnd, ctypes.byref(rect))
    origin = wt.POINT(0, 0)
    user32.ClientToScreen(hwnd, ctypes.byref(origin))
    return origin.x, origin.y, rect.right - rect.left, rect.bottom - rect.top


def focus(hwnd):
    user32.mouse_event(0x0001, 0, 0, 0, 0)  # a zero-length move lets SetForegroundWindow succeed
    user32.SetForegroundWindow(hwnd)
    time.sleep(0.15)


class BITMAPINFOHEADER(ctypes.Structure):
    _fields_ = [("biSize", wt.DWORD), ("biWidth", wt.LONG), ("biHeight", wt.LONG),
                ("biPlanes", wt.WORD), ("biBitCount", wt.WORD), ("biCompression", wt.DWORD),
                ("biSizeImage", wt.DWORD), ("biXPelsPerMeter", wt.LONG),
                ("biYPelsPerMeter", wt.LONG), ("biClrUsed", wt.DWORD), ("biClrImportant", wt.DWORD)]


def shot(hwnd, name, box=None, scale=1):
    """Save the game window, or the part `box` (x, y, width, height) of it enlarged `scale` times."""
    left, top, width, height = client_box(hwnd)
    screen = user32.GetDC(0)
    memory = gdi32.CreateCompatibleDC(screen)
    bitmap = gdi32.CreateCompatibleBitmap(screen, width, height)
    gdi32.SelectObject(memory, bitmap)
    gdi32.BitBlt(memory, 0, 0, width, height, screen, left, top, 0x00CC0020)  # SRCCOPY
    header = BITMAPINFOHEADER(ctypes.sizeof(BITMAPINFOHEADER), width, -height, 1, 32, 0, 0, 0, 0, 0, 0)
    data = ctypes.create_string_buffer(width * height * 4)
    gdi32.GetDIBits(memory, bitmap, 0, height, data, ctypes.byref(header), 0)
    gdi32.DeleteObject(bitmap)
    gdi32.DeleteDC(memory)
    user32.ReleaseDC(0, screen)
    raw, stride, rows = data.raw, width * 4, []
    for y in range(height):
        line = raw[y * stride:(y + 1) * stride]
        rgb = bytearray(width * 3)
        rgb[0::3], rgb[1::3], rgb[2::3] = line[2::4], line[1::4], line[0::4]
        rows.append(bytes(rgb))
    if box is not None:
        x, y, w, h = box
        rows = [b"".join(row[(x + i) * 3:(x + i + 1) * 3] * scale for i in range(w))
                for row in rows[y:y + h] for _ in range(scale)]
        width, height = w * scale, h * scale
    rows = [b"\x00" + row for row in rows]

    def chunk(kind, body):
        return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body) & 0xFFFFFFFF)

    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, name + ".png")
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(b"".join(rows), 6)) + chunk(b"IEND", b"")
    with open(path, "wb") as handle:
        handle.write(png)
    return {"ok": True, "shot": path, "size": [width, height]}


def click(hwnd, x, y, right=False):
    left, top, _, _ = client_box(hwnd)
    focus(hwnd)
    user32.SetCursorPos(left + x, top + y)
    time.sleep(0.12)
    user32.mouse_event(0x0008 if right else 0x0002, 0, 0, 0, 0)  # button down
    time.sleep(0.06)
    user32.mouse_event(0x0010 if right else 0x0004, 0, 0, 0, 0)  # button up
    time.sleep(0.4)
    return {"ok": True, "rclick" if right else "click": [x, y]}


def move(hwnd, x, y):
    left, top, _, _ = client_box(hwnd)
    user32.SetCursorPos(left + x, top + y)
    time.sleep(0.3)
    return {"ok": True, "move": [x, y]}


def key(hwnd, name):
    code = KEYS.get(name.lower())
    if code is None:
        return {"ok": False, "error": f"unknown key {name!r}"}
    focus(hwnd)
    user32.keybd_event(code, 0, 0, 0)
    time.sleep(0.06)
    user32.keybd_event(code, 0, 2, 0)  # KEYEVENTF_KEYUP
    time.sleep(0.4)
    return {"ok": True, "key": name}


def main(argv):
    if not argv:
        print(__doc__)
        return
    hwnd = game_window()
    steps, i = [], 0
    while i < len(argv):
        command = argv[i]
        if command in ("click", "rclick", "move"):
            steps.append((command, int(argv[i + 1]), int(argv[i + 2])))
            i += 3
        elif command == "zoom":
            steps.append((command, argv[i + 1], tuple(int(v) for v in argv[i + 2:i + 6])))
            i += 6
        elif command in ("key", "shot", "wait", "type"):
            steps.append((command, argv[i + 1]))
            i += 2
        else:
            raise SystemExit(json.dumps({"ok": False, "error": f"unknown command {command!r}"}))
    for step in steps:
        if step[0] in ("click", "rclick"):
            result = click(hwnd, step[1], step[2], right=step[0] == "rclick")
        elif step[0] == "type":
            results = [key(hwnd, letter) for letter in step[1]]
            result = {"ok": all(r["ok"] for r in results), "type": step[1]}
        elif step[0] == "move":
            result = move(hwnd, step[1], step[2])
        elif step[0] == "key":
            result = key(hwnd, step[1])
        elif step[0] == "shot":
            result = shot(hwnd, step[1])
        elif step[0] == "zoom":
            result = shot(hwnd, step[1], step[2], 3)
        else:
            time.sleep(float(step[1]))
            result = {"ok": True, "wait": float(step[1])}
        print(json.dumps(result), flush=True)


if __name__ == "__main__":
    main(sys.argv[1:])
