# M10: New chieftain spells

Follow [docs/SESSION_RULES.md](../SESSION_RULES.md) (first actions, decisions, closing, launching the next session).

## The milestone

From [MILESTONES.md](../MILESTONES.md), M10: two new spells per race in a third slot; a campaign-style unlock flag. **Done when** they are castable and the AI uses them on Hard.

In practice:

1. **Design first:** `docs/design/spells.md` from PLAN.md §4.4 ("Chieftain spells": *Jolly Jungle* and *Poultry Panic* for the Natives, *Hammer of Thor* and *Fjord Fog* for the Vikings, a third slot with a 100 s charge). Settle what "two in a third slot" means (one slot that holds either of two spells the player picks, or a third and a fourth slot), how the slot unlocks (the 2004 game unlocks spells per campaign island through `Player.can_do_magic`; skirmish under Buffed starts with them unlocked), and how each spell's numbers fit the existing ones (`spells` in the rulesets: `stinking_stew`, `crackling_cloud`, `terrifying_toot`, `ravaging_roar`). Log every choice.
2. **Buffed only:** a `features` flag (false in `resurrected.json`, true in `buffed.json`), numbers under `spells` (R-14's layout: `resurrected.json` holds them, the flag keeps them out). Classic and Resurrected traces must not move: compare under the old `SIM_VERSION` first, then bump and regenerate.
3. **Code:** `RacesResources.NUM_MAGIC` (2) sizes `Player.can_do_magic`, `Unit`'s `magic_energy` and `MAX_MAGIC_ENERGY`, `Race`'s magic factories and the panel's `magic1_button` / `magic2_button` (`gui/ActionButtonPanel`, `RechargeButton`, `GameAction.MAGIC_1` / `MAGIC_2`); every array indexed by spell must keep its first two entries exactly as they are. Existing spells are `MagicFactory` subclasses in `model/weapon/` (`PoisonFogFactory`, `LightningCloudFactory`, `StunFactory`, `SonicBlastFactory`) run by `MagicController` / `MagicBehaviour`; *Hammer of Thor* reuses the chieftain's `thor` animation (position 5 in `Unit.Animation`). The Lodge's spell charge factor (`Lodge.getSpellChargeFactor`, R-38) must apply to the new slot too.
4. **Effects:** roots (no movement for 8 s; the snare's stun, `Unit.stun`, is the nearest pattern but also stops attacks), a knock-down that leaves five catchable chickens (`RubberGroup.newCoopFlock(world).spawn(...)`, as the coop does), a single bolt (40 to a building or a kill), a fog that lowers enemy hit chance (an aura like `TotemAura` / `DrumAura`, with an empty-list fast path so Classic's floats stay bit for bit). Animals (M9's fauna, `World.getAnimals()`) are not units: decide whether spells touch them (simplest: no).
5. **Interface:** the third slot's button, icons (the icon sheet grows downward; existing pixels unchanged, as in M9), keys and tooltips in six languages (`BuffedStringsTest`'s pattern).
6. **AI on Hard** (Normal optional): `player/ChieftainAI`, `NativeChieftainAI`, `VikingChieftainAI` decide the existing spells; add the new ones there, behind the feature check.
7. **Art:** the effects need sprites or particles; reuse the existing emitters and textures where possible (the poison fog, the lightning cloud, the sonic blast), placeholders otherwise, by a background agent if new models are needed. Sounds: reuse existing clips (PLAN.md C6).
8. **Tests and checks:** `RulesetTest` (the flag and the numbers); a headless test in the style of `BuffedFaunaTest` (each spell's effect, unlocking, the charge time, the Lodge factor on the new slot, nothing under Classic or Resurrected); the census if the AI needs one (spells cast per player); `SIM_VERSION` bump with only the Buffed traces moving; an in-game look with `drive_game.py`; a Josh check.

## Read first

- PLAN.md §4.4 ("Chieftain spells"), [rulesets.md](../rulesets.md) (Spells), [new-content-checklist.md](../new-content-checklist.md).
- `model/weapon/MagicFactory.java` and its four subclasses, `model/behaviour/MagicController.java`, `MagicBehaviour.java`, `model/Unit.java` (`doMagic`, `canDoMagic`, `magic_energy`), `model/Lodge.java`, `player/ChieftainAI.java` and both subclasses, `gui/ActionButtonPanel.java` (the chieftain group), `tutorial/MagicTrigger.java`.
- M9 part 2's commit as the latest worked example (a new interface across the weapons, a world list, AI rule, census, tests, cheats, art).

## Context from M9

- **Classic invariance held** again: M9 part 2 retyped the whole attack chain from `Selectable` to a new `Hittable` interface (weapons, behaviours, `AttackScanFilter`) and renumbered `AttackScanFilter.Priority` (only the order of the values is compared), and the Classic and Resurrected traces did not move under the old `SIM_VERSION`. Fauna is placed only behind `features.fauna` (`Fauna.populate`); the AI's hunt rule waits on a counter that never moves without animals.
- **Animals are not units:** `World.getAnimals()` lists them; they are `Model`s, not `Selectable`s, so the existing spells (which look for a player's units) pass them by. Decide whether the new spells touch them; the simplest is no, and say so in the design.
- **Match lengths:** Buffed AI matches now take about 20 minutes (1v1) and 54 (six tribes, seed 9), close to the 60-minute limit; if a change leaves `buffed-6p` undecided, pick another seed that finishes (R-43) and say so. Animals draw from the world's random numbers, so a test that leans on a lucky roll can flip (`BuffedDrumNetTest`'s snare test did; it now sends the catcher away).
- **In game** (SESSION_RULES §8): another Claude session on this PC (Josh's Roblox project) also drives the mouse; claim input first (`ListAgents`, "CLAIM input ~Ns", wait for "OK") and release it after, and don't take focus while a Roblox window covers the game and someone may be at the PC. Clicks that worked in M9: main menu Single player (87, 334); terrain pulldown (385, 236), Tropical (350, 266); OK (610, 552); after `/iamacheater` the chat window's X at (466, 516). With units selected the action panel fills the top right: right-clicks there hit buttons, not the ground. Shift+F7 to Shift+F9 put a boar or wolf, a monkey and a crab under the pointer; F5 makes a chieftain.
- **Traps** (from M9 part 1, still true): run Python only from PowerShell (`python -I`); never type a `\u` escape into a tool call; `spotlessApply` writes CRLF; Spotless reformats a multi-line `else if` condition oddly, so nest the second test instead.
- Josh owes J-01 to J-08; none blocks this session.

## Banked decisions that touch this

- Final art needs an artist or a budget (MILESTONES.md "Needs an artist"): placeholders only.
- Sounds: reuse existing clips; new ones need a source (PLAN.md C6).
