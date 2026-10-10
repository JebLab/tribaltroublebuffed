# Session rules

How a session runs, decides, closes and starts the next one, so the milestone queue in [MILESTONES.md](MILESTONES.md) can run end to end. Every session prompt points here instead of repeating these rules. When a rule changes, change it here with a date. Set up 9 October 2026 (Josh: "I want this to be able to run autonomously"), modelled on the chain in Josh's *Build to Survive the Baddies* project.

## 1. Session shape

- **One milestone per session**, in queue order. A prompt in `docs/prompts/m<NN>-<slug>.md` names it. Split a milestone into a second session only when it is marked as several sessions (M17) or the context gets unwieldy; the follow-up prompt keeps the same number (`m17-part2-...`).
- **First actions**, in this order:
  1. Make sure Remote Control is on (`set_remote_control`, session `self`, enabled). If it reports anything but "on", say so in one line and continue.
  2. In PowerShell, run `python tools/scripts/launch_next_session.py status`. It reports the chain mode, the gates (§5) and the next open milestone.
  3. Read [DECISIONS.md](DECISIONS.md): new answers from Josh apply from now on. If this session's work depends on a banked item that is still open, do the parts that don't.
  4. Read the prompt, the milestone's row in MILESTONES.md and the PLAN.md sections it points to.
- Background tasks Josh starts from a suggestion chip run in `.claude/worktrees/` on their own branch. When one has finished (its branch has commits), merge it into `revamp` in the next session that touches the same code.

## 2. Decision policy (bank the critical ones, decide the rest)

- **Reversible, internal choices** (design details, file layout, test design, which upstream code to port): take the recommended option, carry on, and log it as one line under "Decided by default" in DECISIONS.md (id, choice, why, date).
- **Banked for Josh**: anything outward-facing, costly or hard to undo. That means publishing (GitHub releases and tags unless pre-approved in DECISIONS.md, itch.io, Flathub, winget, any public post); creating accounts; spending money (code signing, commissions, assets); contacting anyone (Oddlabs, Resurrected, the restoration fork); renaming the project or changing the license; deleting or rewriting saves, settings or Git history; skipping a milestone or changing its done-when; reviving multiplayer. Write it in DECISIONS.md under "Waiting for Josh" with a recommendation, skip only the work that depends on it, and continue.
- Don't re-ask banked items. When Josh is present (manual mode, or a chat reply), give the open ones as one short numbered list with a recommendation each.

## 3. Checks only Josh can do

Things no agent can verify: how a change feels to play, real hardware (gamepad, a 21:9 or 4K screen, a Steam Deck), accounts and payments. Do everything up to the check, write the check under "Josh's checks" in DECISIONS.md (what to try, what should happen, 5 minutes or less each), and mark the milestone done with "check owed" in its log row. Then continue with the queue.

Create `reports/REVIEW_DUE` (one line: what Josh needs to look at) only when the next milestone cannot sensibly start without Josh's answer. It stops the chain in both modes. Delete it only after Josh's answer is recorded in DECISIONS.md.

## 4. Rules carried forward

- Single-player scope; multiplayer code is inherited and unmaintained. Every gameplay change sits behind the *Buffed* ruleset or a flag; Classic stays the 2004 game ([rulesets.md](rulesets.md)).
- Determinism: a change to simulation behaviour bumps `SIM_VERSION` in `common/.../util/Compatibility.java`, following its comment. Once M3 lands, the headless match tests must pass before a commit.
- `gradlew build` green (it runs the formatter check; `gradlew spotlessApply` formats) before every commit. New strings go in all six language files (en, da, de, es, it, pt), non-ASCII as `\uXXXX` escapes.
- Report states precisely: built, unit-tested, ran in game, checked by Josh. Don't invent results; a skipped step is reported as skipped.
- Never edit `.claude/settings.json` or `.claude/settings.local.json` (they hold the chain mode and the permission rules; Josh owns them). Don't change the launcher without telling Josh. Don't push to `main`, never force-push.
- Desktop automation: use only the launcher and the game-window helper (§8). Screenshots cover the game window or the Claude window only, never the whole screen.

## 5. Session chaining

When the milestone is closed (§6), start the next session yourself with the PowerShell tool:

```
python tools/scripts/launch_next_session.py launch --prompt docs/prompts/m<NN>-<slug>.md
```

It opens a new Code session for this project in the Claude app (the sidebar group "tribaltroublebuffed") and sends the standard kickoff. It checks that the new session is empty before typing anything. In both modes it refuses while `reports/STOP_CHAIN` or `reports/REVIEW_DUE` exists, and when the prompt is not for the next open milestone in MILESTONES.md. A launch Josh asks for in chat may pass `--user-request chat`, which skips those last two checks (never STOP_CHAIN).

**The mode is Josh's**, set in `.claude/settings.local.json`:

| Mode | Setting | What the session does |
|---|---|---|
| **Manual** | `"TT_CHAIN_MODE": "manual"`; the launch rule under `permissions.ask` | Push-notify Josh ("M4 ready to start: approve the launch"), then run the launcher. The permission prompt waits for Josh's approval; on approval the new session starts by itself. If `status` reports the launch rule under "allow", ask in chat and wait for a yes first. |
| **Free** | `"TT_CHAIN_MODE": "free"`; the launch rule under `permissions.allow` | Launch without asking. Push-notify only for a failed or refused launch, a stopped chain, a review Josh owes, a failing build that can't be fixed, or the queue finishing. |

Any session may create `reports/STOP_CHAIN` (one line: why) to halt the chain, for example after two failed attempts at the same problem; only Josh deletes it. If a launch is refused, declined or not confirmed, don't retry: log it, notify Josh, and give the kickoff text to paste.

## 6. Closing a session

1. `gradlew build` green. Check the change in the game when it touches anything a player sees (§8).
2. MILESTONES.md: tick the row (✅) and add a log row: what changed, the states it reached, anything owed, and what was found on the way. Keep it to a short paragraph.
3. DECISIONS.md: new defaults, banked items, Josh's checks. README and PLAN.md "Where things stand" when the project status changed.
4. Commit with a message naming the milestone, then `git push origin revamp`. Stage only your own paths if another session is working in this folder.
5. Write the next prompt (§7) and commit it.
6. Launch the next session (§5), or stop if a gate is set or the queue is finished, and say which.

## 7. Prompt shape

`docs/prompts/m<NN>-<slug>.md`, short:

- the milestone row (scope and done-when) and what "done" means in practice;
- what to read first (PLAN.md sections, docs, code paths, upstream code to port);
- context the last session knows and the next one would not: leftovers, branches to merge, traps;
- banked decisions that touch this milestone;
- "Follow docs/SESSION_RULES.md" — no copied rule sections.

## 8. Checking in the game

`gradlew tt:run` starts the game windowed (stdout in the Gradle output; each skirmish logs `Ruleset: <id>`). Whether a session may drive the game window with synthetic clicks and keys is Josh's call, recorded in DECISIONS.md: it moves the real mouse cursor. When allowed, use `tools/scripts/drive_game.py` (click, key, screenshot of the game window) and keep it short. Other Claude sessions on this PC (Josh's Roblox project) drive the real mouse and keyboard too (added 10 October 2026): before the first click, find them with `ListAgents` and send "CLAIM input ~Ns: <what>", wait for "OK" (or 60 s of silence), and send "RELEASE input" when done; answer their claims with "OK" or "BUSY, ~Ns". Do not take focus while another window covers the game and someone may be using the PC (seconds since the last input: `GetLastInputInfo`); try again later or leave the look to Josh's check. When not, build and unit-test, and add the in-game check to "Josh's checks".
