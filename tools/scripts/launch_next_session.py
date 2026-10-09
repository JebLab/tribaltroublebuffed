"""Start the next chained Claude Code session in the Claude desktop app (session-chain helper).

Ported from the launcher of Josh's "Build to Survive the Baddies" project; the UI Automation steps are unchanged.
The rules it serves are in docs/SESSION_RULES.md.

Chain mode is set by Josh in .claude/settings.local.json (agents never edit that file):
  "env": {"TT_CHAIN_MODE": "free"} selects free mode; anything else is manual mode.
  manual - every launch needs Josh's permission: the launcher's "launch" rule sits under "ask", so the command
           waits on the permission prompt (and the session sends a push notification first).
  free   - sessions start one after another until the milestone queue is done; the "launch" rule sits under
           "allow". The prompt must be for the next open milestone in docs/MILESTONES.md.
Either mode refuses while reports/STOP_CHAIN exists (the chain is stopped), and while reports/REVIEW_DUE exists
(work is waiting on Josh's verdict) unless Josh asked for this launch (--user-request).
Standard library only (ctypes + PowerShell UI Automation). Run it with the Windows Python, e.g. from PowerShell.

  python tools/scripts/launch_next_session.py status
      Print the chain mode, the gates and the next open milestone. Sends nothing.
  python tools/scripts/launch_next_session.py probe
      Bring the Claude window forward, save a screenshot of it and a UI Automation dump. Sends nothing.
  python tools/scripts/launch_next_session.py launch --prompt docs/prompts/<next>.md
      Open a new Code session in this project folder in the Claude app and send the standard kickoff.
  python tools/scripts/launch_next_session.py launch --prompt docs/prompts/<next>.md --user-request chat
      Same, for a launch Josh asked for in chat: skips the milestone-order check and REVIEW_DUE, never STOP_CHAIN.
  ... launch --dry-run
      Open the new-session composer and verify it, type nothing.

Outputs go to %TEMP%\\claude-launcher\\ and a JSON result is printed on stdout.
"""
import argparse
import ctypes
import ctypes.wintypes as wt
import json
import os
import re
import struct
import subprocess
import time
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(os.environ.get("TEMP", ROOT), "claude-launcher")
SETTINGS = os.path.join(ROOT, ".claude", "settings.local.json")
STOP_FILE = os.path.join(ROOT, "reports", "STOP_CHAIN")
REVIEW_FILE = os.path.join(ROOT, "reports", "REVIEW_DUE")
MILESTONES = os.path.join(ROOT, "docs", "MILESTONES.md")
KICKOFF = (
    'Read and follow, exactly and in order, the prompt in {prompt} (project root: {root}), together with '
    'docs/SESSION_RULES.md. First make sure Remote Control is on for this session (set_remote_control, '
    'session_id "self", enabled true). The previous session has finished, committed and pushed. '
    'Chain mode: {mode} ({mode_note}).'
)
MODE_NOTES = {
    "manual": "starting the next session needs Josh's permission: push-notify Josh, then run the launcher, "
              "which waits for Josh's approval",
    "free": "start the next session yourself when this milestone is closed; push-notify Josh only for failures, "
            "a stopped chain, a review Josh owes, or the queue finishing; bank decisions as usual",
}


# --- chain mode and gates ---------------------------------------------------------------------
def _settings():
    try:
        with open(SETTINGS, encoding="utf-8") as handle:
            return json.load(handle)
    except (OSError, ValueError):
        return {}


def chain_mode():
    """'free' only when Josh sets env.TT_CHAIN_MODE to "free"; anything else is 'manual'."""
    value = str(_settings().get("env", {}).get("TT_CHAIN_MODE", "")).strip().lower()
    return "free" if value == "free" else "manual"


def launch_rule():
    """Where the launcher's launch rule sits: 'allow' (no prompt), 'ask' (permission prompt) or None."""
    permissions = _settings().get("permissions", {})
    for kind in ("ask", "allow"):
        if any("launch_next_session.py launch" in rule or rule.endswith("launch_next_session.py *)")
               for rule in permissions.get(kind, [])):
            return kind
    return None


def next_milestone():
    """The first queue row in docs/MILESTONES.md whose milestone cell has no check mark, e.g. ("M3", "Tests and
    CI for this fork"); None when every row is done."""
    try:
        with open(MILESTONES, encoding="utf-8") as handle:
            lines = handle.read().splitlines()
    except OSError:
        return None
    for line in lines:
        match = re.match(r"\|\s*(M\d+)\s*\|\s*([^|]*)\|", line)
        if match and "✅" not in match.group(2):
            return match.group(1), match.group(2).replace("*", "").strip()
    return None


def milestone_of(prompt):
    match = re.match(r"m(\d+)", os.path.basename(prompt or ""), re.IGNORECASE)
    return f"M{int(match.group(1))}" if match else None


def first_line(path):
    try:
        with open(path, encoding="utf-8-sig") as handle:
            lines = handle.read().strip().splitlines()
        return lines[0] if lines else ""
    except OSError:
        return ""


def status(args):
    mode = chain_mode()
    upcoming = next_milestone()
    print(json.dumps({"ok": True, "mode": mode, "launchRule": launch_rule(),
                      "stopChain": first_line(STOP_FILE) if os.path.exists(STOP_FILE) else None,
                      "reviewDue": first_line(REVIEW_FILE) if os.path.exists(REVIEW_FILE) else None,
                      "nextMilestone": upcoming[0] if upcoming else None,
                      "nextMilestoneTitle": upcoming[1] if upcoming else None,
                      "note": MODE_NOTES[mode]}))


user32 = ctypes.WinDLL("user32", use_last_error=True)
gdi32 = ctypes.WinDLL("gdi32", use_last_error=True)
kernel32 = ctypes.WinDLL("kernel32", use_last_error=True)
user32.SetProcessDPIAware()


# --- windows -------------------------------------------------------------------------------
def _process_name(pid):
    handle = kernel32.OpenProcess(0x1000, False, pid)  # PROCESS_QUERY_LIMITED_INFORMATION
    if not handle:
        return ""
    try:
        size = wt.DWORD(1024)
        buf = ctypes.create_unicode_buffer(1024)
        if kernel32.QueryFullProcessImageNameW(handle, 0, buf, ctypes.byref(size)):
            return os.path.basename(buf.value).lower()
        return ""
    finally:
        kernel32.CloseHandle(handle)


def claude_window():
    found = []
    proc = ctypes.WINFUNCTYPE(wt.BOOL, wt.HWND, wt.LPARAM)

    def visit(hwnd, _):
        if not user32.IsWindowVisible(hwnd):
            return True
        length = user32.GetWindowTextLengthW(hwnd)
        title = ctypes.create_unicode_buffer(length + 1)
        user32.GetWindowTextW(hwnd, title, length + 1)
        pid = wt.DWORD()
        user32.GetWindowThreadProcessId(hwnd, ctypes.byref(pid))
        if _process_name(pid.value) == "claude.exe" and title.value:
            rect = wt.RECT()
            user32.GetWindowRect(hwnd, ctypes.byref(rect))
            box = (rect.left, rect.top, rect.right, rect.bottom)
            found.append((hwnd, title.value, (box[2] - box[0]) * (box[3] - box[1]), box))
        return True

    user32.EnumWindows(proc(visit), 0)
    if not found:
        raise SystemExit(json.dumps({"ok": False, "error": "Claude desktop window not found"}))
    found.sort(key=lambda item: item[2], reverse=True)  # the main window is the largest
    return found[0][0], found[0][1]


def project_labels():
    """Names the Claude app may give this project's sidebar group, for its "New session in <name>" button: the
    folder name, and the repository name of the origin remote (the app shows that one for this project)."""
    labels = [os.path.basename(ROOT)]
    try:
        url = subprocess.run(["git", "-C", ROOT, "remote", "get-url", "origin"], capture_output=True,
                             encoding="utf-8", errors="replace", timeout=20).stdout.strip()
    except (OSError, subprocess.SubprocessError):
        url = ""
    repo = re.sub(r"\.git$", "", url.rstrip("/").split("/")[-1]) if url else ""
    if repo and repo not in labels:
        labels.append(repo)
    return labels


def window_box(hwnd):
    rect = wt.RECT()
    user32.GetWindowRect(hwnd, ctypes.byref(rect))
    return [rect.left, rect.top, rect.right, rect.bottom]


def bring_forward(hwnd):
    if user32.IsIconic(hwnd):
        user32.ShowWindow(hwnd, 9)  # SW_RESTORE
    # A zero-length synthetic mouse move lets SetForegroundWindow succeed. Never tap Alt
    # (it opens the app menu) and never send Escape to the app (it can stop a running session).
    user32.mouse_event(0x0001, 0, 0, 0, 0)
    user32.SetForegroundWindow(hwnd)
    time.sleep(0.6)
    return user32.GetForegroundWindow() == hwnd


# --- screenshot of the Claude window (GDI -> PNG, half size) ---------------------------------
class BITMAPINFOHEADER(ctypes.Structure):
    _fields_ = [("biSize", wt.DWORD), ("biWidth", wt.LONG), ("biHeight", wt.LONG),
                ("biPlanes", wt.WORD), ("biBitCount", wt.WORD), ("biCompression", wt.DWORD),
                ("biSizeImage", wt.DWORD), ("biXPelsPerMeter", wt.LONG),
                ("biYPelsPerMeter", wt.LONG), ("biClrUsed", wt.DWORD), ("biClrImportant", wt.DWORD)]


def screenshot(path, box):
    """Capture box = [left, top, right, bottom] in screen pixels; only the Claude window is ever captured."""
    left, top = box[0], box[1]
    width, height = box[2] - box[0], box[3] - box[1]
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
    for y in range(0, height, 2):
        line = raw[y * stride:(y + 1) * stride]
        rgb = bytearray(len(line[0::8]) * 3)
        rgb[0::3], rgb[1::3], rgb[2::3] = line[2::8], line[1::8], line[0::8]
        rows.append(b"\x00" + bytes(rgb))
    out_w, out_h = len(raw[0:stride][0::8]), len(rows)

    def chunk(kind, body):
        return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", out_w, out_h, 8, 2, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(b"".join(rows), 6)) + chunk(b"IEND", b"")
    with open(path, "wb") as handle:
        handle.write(png)
    return {"path": path, "box": list(box), "image": [out_w, out_h], "scale": 0.5}


# --- UI Automation (Windows PowerShell 5.1, inline -Command; no script files) ----------------
UIA_DUMP = r"""
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Add-Type -AssemblyName UIAutomationClient, UIAutomationTypes
$root = [System.Windows.Automation.AutomationElement]::FromHandle([IntPtr]__HWND__)
$walker = [System.Windows.Automation.TreeWalker]::ControlViewWalker
$items = New-Object System.Collections.ArrayList
function Walk($el, $depth) {
  if ($items.Count -ge 2500 -or $depth -gt 40) { return }
  try {
    $c = $el.Current; $r = $c.BoundingRectangle
    $kind = $c.ControlType.ProgrammaticName.Replace('ControlType.', '')
    [void]$items.Add([ordered]@{ d = $depth; type = $kind; name = $c.Name; id = $c.AutomationId;
      cls = $c.ClassName; focus = $c.HasKeyboardFocus;
      x = [int]$r.X; y = [int]$r.Y; w = [int]$r.Width; h = [int]$r.Height })
  } catch { return }
  $child = $walker.GetFirstChild($el)
  while ($child -ne $null) { Walk $child ($depth + 1); $child = $walker.GetNextSibling($child) }
}
for ($i = 0; $i -lt 4; $i++) { $items.Clear(); Walk $root 0; if ($items.Count -gt 40) { break }; Start-Sleep -Milliseconds 700 }
$items | ConvertTo-Json -Depth 3 -Compress
"""


def uia_dump(hwnd):
    script = UIA_DUMP.replace("__HWND__", str(int(hwnd)))
    done = subprocess.run(["powershell", "-NoProfile", "-NonInteractive", "-Command", script],
                          capture_output=True, encoding="utf-8", errors="replace", timeout=120)
    text = done.stdout.strip()
    try:
        items = json.loads(text) if text else []
    except json.JSONDecodeError:
        items = []
    if isinstance(items, dict):
        items = [items]
    return items, done.stderr.strip()[:500]


def probe(args):
    os.makedirs(OUT, exist_ok=True)
    hwnd, title = claude_window()
    forward = bring_forward(hwnd)
    box = window_box(hwnd)
    stamp = time.strftime("%Y%m%d-%H%M%S")
    shot = screenshot(os.path.join(OUT, f"probe-{stamp}-window.png"), box)
    items, error = uia_dump(hwnd)
    dump_path = os.path.join(OUT, f"probe-{stamp}-uia.json")
    with open(dump_path, "w", encoding="utf-8") as handle:
        json.dump(items, handle, indent=1, ensure_ascii=False)
    names = {i.get("name") for i in items}
    found = [label for label in project_labels() if "New session in " + label in names]
    print(json.dumps({"ok": True, "window": title, "foreground": forward, "box": box, "windowShot": shot,
                      "uia": dump_path, "elements": len(items), "newSessionButton": found[0] if found else None,
                      "uiaError": error}))


# The launch runs in one PowerShell process so UI Automation element references stay valid.
# Data arrives through LAUNCH_* environment variables; the script uses single quotes only.
UIA_LAUNCH = r"""
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Add-Type -AssemblyName UIAutomationClient, UIAutomationTypes, System.Windows.Forms
$A = [System.Windows.Automation.AutomationElement]
$T = [System.Windows.Automation.TreeScope]
$C = [System.Windows.Automation.ControlType]
$result = [ordered]@{ ok = $false; dryRun = ($env:LAUNCH_DRYRUN -eq '1'); steps = @() }
function Say($s) { $script:result.steps += $s }
function Finish() { [Console]::Out.WriteLine(($script:result | ConvertTo-Json -Depth 4 -Compress)); exit }
function Fail($s) { Say ('ABORT: ' + $s); Finish }
trap { Say ('ERROR: ' + $_.Exception.Message); Finish }
$root = $A::FromHandle([IntPtr][int64]$env:LAUNCH_HWND)
$claudePid = $root.Current.ProcessId
function ByName($name, $type) {
  $c1 = New-Object System.Windows.Automation.PropertyCondition($A::NameProperty, $name)
  $c2 = New-Object System.Windows.Automation.PropertyCondition($A::ControlTypeProperty, $type)
  return $root.FindFirst($T::Descendants, (New-Object System.Windows.Automation.AndCondition($c1, $c2)))
}
function Headers() {
  $names = @()
  $c = New-Object System.Windows.Automation.PropertyCondition($A::ControlTypeProperty, $C::Button)
  foreach ($b in $root.FindAll($T::Descendants, $c)) { $n = $b.Current.Name; if ($n -like '*, rename session') { $names += $n } }
  return ,$names
}
function MessageCount() {
  $chat = ByName 'Chat messages' $C::Group
  if ($chat -eq $null) { return 0 }
  $count = 0
  $c = New-Object System.Windows.Automation.PropertyCondition($A::ControlTypeProperty, $C::Group)
  foreach ($g in $chat.FindAll($T::Children, $c)) { if ($g.Current.Name -like 'Message *' -or $g.Current.Name -like '*streaming message') { $count++ } }
  return $count
}
function FocusedInClaude() {
  $f = $A::FocusedElement
  return ($f -ne $null -and $f.Current.ProcessId -eq $claudePid)
}
function FocusedInside($target) {
  $walker = [System.Windows.Automation.TreeWalker]::RawViewWalker
  $e = $A::FocusedElement
  for ($k = 0; $k -lt 6 -and $e -ne $null; $k++) {
    if ([System.Windows.Automation.Automation]::Compare($e, $target)) { return $true }
    $e = $walker.GetParent($e)
  }
  return $false
}

# 1. A native app menu left open would swallow input; Escape is sent only while that menu has focus.
$f = $A::FocusedElement
if ($f -ne $null -and $f.Current.ProcessId -eq $claudePid -and $f.Current.ControlType -eq $C::MenuItem) {
  [System.Windows.Forms.SendKeys]::SendWait('{ESC}'); Start-Sleep -Milliseconds 400; Say 'closed the app menu'
}
if (-not (FocusedInClaude)) { Fail 'keyboard focus is not in the Claude window' }

# 2. Remember the open session, then open a new session in this project folder.
$before = Headers
Say ('open session: ' + ($before -join ' | '))
$new = $null
foreach ($label in $env:LAUNCH_FOLDERS.Split('|')) {
  $new = ByName ('New session in ' + $label) $C::Button
  if ($new -ne $null) { Say ('project group: ' + $label); break }
}
if ($new -eq $null) { Fail ('no New session button for ' + $env:LAUNCH_FOLDERS) }
$new.GetCurrentPattern([System.Windows.Automation.InvokePattern]::Pattern).Invoke()
Say 'invoked New session'

# 3. Positive check that an empty new-session composer is showing.
$prompt = $null
for ($i = 0; $i -lt 20; $i++) {
  Start-Sleep -Milliseconds 500
  $now = Headers
  $stillOld = @($now | Where-Object { $before -contains $_ }).Count -gt 0
  $messages = MessageCount
  $prompt = ByName 'Prompt' $C::Edit
  if (-not $stillOld -and $messages -eq 0 -and $prompt -ne $null) { break }
  $prompt = $null
}
if ($prompt -eq $null) { Fail ('new-session view not confirmed (headers: ' + ((Headers) -join ' | ') + '; messages: ' + (MessageCount) + ')') }
Say 'new-session composer confirmed (old session header gone, no messages, prompt box present)'
if ($result.dryRun) { $result.ok = $true; Say 'dry run: nothing typed'; Finish }

# 4. Paste the kickoff into the new composer only, then send it.
$old = $null
try { $old = Get-Clipboard -Raw } catch {}
Set-Clipboard -Value $env:LAUNCH_KICKOFF
$prompt.SetFocus(); Start-Sleep -Milliseconds 400
if (-not (FocusedInClaude) -or -not (FocusedInside $prompt)) {
  if ($old -ne $null) { Set-Clipboard -Value $old }
  Fail 'the new prompt box did not take keyboard focus'
}
[System.Windows.Forms.SendKeys]::SendWait('^v'); Start-Sleep -Milliseconds 700
$text = ''
$vp = $null
if ($prompt.TryGetCurrentPattern([System.Windows.Automation.ValuePattern]::Pattern, [ref]$vp)) { $text = $vp.Current.Value }
if ([string]::IsNullOrEmpty($text)) {
  $tp = $null
  if ($prompt.TryGetCurrentPattern([System.Windows.Automation.TextPattern]::Pattern, [ref]$tp)) { $text = $tp.DocumentRange.GetText(-1) }
}
if ($old -ne $null) { Set-Clipboard -Value $old }
if (-not [string]::IsNullOrEmpty($text) -and $text -notlike ('*' + $env:LAUNCH_PROMPT + '*')) { Fail 'the prompt box does not contain the kickoff after pasting' }
Say ('pasted; prompt text verified: ' + (-not [string]::IsNullOrEmpty($text)))
if (-not (FocusedInClaude) -or -not (FocusedInside $prompt)) { Fail 'focus left the new prompt box before sending' }
[System.Windows.Forms.SendKeys]::SendWait('{ENTER}')
Say 'sent'

# 5. Confirm the new session started.
for ($i = 0; $i -lt 40; $i++) {
  Start-Sleep -Milliseconds 500
  $now = Headers
  $fresh = @($now | Where-Object { $before -notcontains $_ })
  if ($fresh.Count -gt 0 -or (MessageCount) -gt 0) { $result.ok = $true; $result.newSession = ($fresh -join ' | '); break }
}
if (-not $result.ok) { Say 'sent, but the new session was not confirmed within 20 s' }
Finish
"""


def refuse(stopped, error, **extra):
    raise SystemExit(json.dumps(dict({"ok": False, "mode": chain_mode(), "stopped": stopped, "error": error}, **extra)))


def launch(args):
    prompt = args.prompt.replace("\\", "/")
    if not (prompt.startswith("docs/prompts/") and prompt.endswith(".md")):
        raise SystemExit(json.dumps({"ok": False, "error": "prompt must be docs/prompts/<name>.md"}))
    if not os.path.isfile(os.path.join(ROOT, prompt)):
        raise SystemExit(json.dumps({"ok": False, "error": f"missing {prompt}"}))
    mode = chain_mode()
    if os.path.exists(STOP_FILE):
        refuse("stop-file", "reports/STOP_CHAIN exists; the chain is stopped until Josh deletes it",
               why=first_line(STOP_FILE))
    if os.path.exists(REVIEW_FILE) and not args.user_request:
        refuse("review-due", "reports/REVIEW_DUE exists; Josh's verdict is owed before the next session starts",
               review=first_line(REVIEW_FILE))
    if not args.user_request:
        upcoming = next_milestone()
        if upcoming is None:
            refuse("queue-finished", "every milestone in docs/MILESTONES.md is checked off")
        if milestone_of(prompt) != upcoming[0]:
            refuse("milestone-order", f"{prompt!r} is not a prompt for the next open milestone, {upcoming[0]} "
                                      f"({upcoming[1]}); name it m{int(upcoming[0][1:]):02d}-<slug>.md")
    os.makedirs(OUT, exist_ok=True)
    hwnd, title = claude_window()
    if not bring_forward(hwnd):
        raise SystemExit(json.dumps({"ok": False, "error": "could not bring the Claude window forward"}))
    stamp = time.strftime("%Y%m%d-%H%M%S")
    before = screenshot(os.path.join(OUT, f"launch-{stamp}-before.png"), window_box(hwnd))
    env = dict(os.environ, LAUNCH_HWND=str(int(hwnd)), LAUNCH_FOLDERS="|".join(project_labels()),
               LAUNCH_PROMPT=prompt, LAUNCH_DRYRUN="1" if args.dry_run else "0",
               LAUNCH_KICKOFF=KICKOFF.format(prompt=prompt, root=ROOT, mode=mode, mode_note=MODE_NOTES[mode]))
    done = subprocess.run(["powershell", "-NoProfile", "-NonInteractive", "-Command", UIA_LAUNCH],
                          capture_output=True, encoding="utf-8", errors="replace", timeout=150,
                          env=env)
    try:
        result = json.loads(done.stdout.strip().splitlines()[-1])
    except (json.JSONDecodeError, IndexError):
        result = {"ok": False, "stdout": done.stdout[-800:]}
    time.sleep(1.0)
    result["before"] = before["path"]
    result["after"] = screenshot(os.path.join(OUT, f"launch-{stamp}-after.png"), window_box(hwnd))["path"]
    result["stderr"] = done.stderr.strip()[:800]
    result["prompt"] = prompt
    result["mode"] = mode
    if args.user_request:
        result["userRequest"] = args.user_request
    print(json.dumps(result))


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("status", help="print the chain mode, gates and next milestone; send nothing")
    sub.add_parser("probe", help="focus Claude, save a window screenshot and UI Automation dump; send nothing")
    launch_parser = sub.add_parser("launch", help="open a new Code session and send the kickoff")
    launch_parser.add_argument("--prompt", required=True, help="docs/prompts/<next>.md")
    launch_parser.add_argument("--dry-run", action="store_true", help="open the new-session composer, verify it, type nothing")
    launch_parser.add_argument("--user-request", dest="user_request", metavar="REF",
                               help="Josh asked for this launch (e.g. 'chat'); skips the milestone-order check and "
                                    "REVIEW_DUE, never STOP_CHAIN")
    args = parser.parse_args()
    {"status": status, "probe": probe, "launch": launch}[args.command](args)


if __name__ == "__main__":
    main()
