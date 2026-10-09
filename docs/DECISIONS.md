# Decisions

Where sessions bank what only Josh can decide, record Josh's answers, and log the reversible choices they made on their own ([SESSION_RULES.md](SESSION_RULES.md) §2–3). Newest last in each section. Josh can answer here or in chat; the next session moves the item to "Answered".

## Waiting for Josh

| Id | Decision | Recommendation | Blocks |
|---|---|---|---|
| D-01 | Ask the original developers for the same blessing to use the "Tribal Trouble" name that Resurrected has (PLAN.md §1, principle 4), or plan a rename. | Josh sends one short email before the first public release; a draft can be prepared on request. | the first public release on itch.io (M22) |

## Josh's checks

| Id | Milestone | Try this | Expect |
|---|---|---|---|

## Answered

| Id | Decision | Answer | Date |
|---|---|---|---|
| D-00 | What Classic means | "Classic should be just classic, no resurrected features": the 2004 numbers and only the 2004 world options. | 2026-10-09 |
| D-02 | May sessions tag versions and publish GitHub releases on JebLab/tribaltroublebuffed without asking? | Yes, GitHub only. itch.io, Flathub, winget, accounts and money stay banked. | 2026-10-09 |
| D-03 | May sessions drive the game window with synthetic clicks and keys for in-game checks? | Yes, with `tools/scripts/drive_game.py`, game-window screenshots only, kept short. | 2026-10-09 |
| D-04 | Chain mode when the session chain was set up | Free: sessions chain through the queue without asking. Josh can switch to manual in `.claude/settings.local.json`. | 2026-10-09 |

## Decided by default (reversible)

| Id | Choice | Why | Date |
|---|---|---|---|
| R-01 | The skirmish menu preselects Buffed; campaign, tutorials and multiplayer play under Resurrected. | Buffed is this fork's game; the campaign was tuned for those numbers and the servers expect them. | 2026-10-09 |
| R-02 | Classic keeps interface improvements (rebindable keys, accessibility options, control groups). | They change no rule, and removing accessibility would hurt players for no gain. | 2026-10-09 |
| R-03 | Classic does not revert engine fixes made since 2004 (listed in rulesets.md). | They are fixes, not features; any one can be reverted later if it matters. | 2026-10-09 |
