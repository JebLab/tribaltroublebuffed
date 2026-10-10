# Chieftain spells: the third slot

Buffed's four new chieftain spells (M10), from PLAN.md §4.4: two per race, *Jolly Jungle* and *Poultry Panic* for the Natives, *Hammer of Thor* and *Fjord Fog* for the Vikings. Classic and Resurrected do not have them (the `features.new_spells` flag is false there).

| Race | Spell | Effect |
|---|---|---|
| Natives | *Jolly Jungle* | vines root every enemy within 20 m for 8 s |
| Natives | *Poultry Panic* | a chicken stampede knocks every enemy within 30 m flat for 3 s and leaves five chickens |
| Vikings | *Hammer of Thor* | one bolt at a chosen enemy within 20 m: a unit dies, a building loses 40 hit points |
| Vikings | *Fjord Fog* | mist 30 m across the chieftain's spot for 20 s: enemies fighting inside hit 0.2 less often |

## The third slot

- **One charge, two spells.** The 2004 chieftain has two spells charged by time (40 s and 70 s), and a cast empties both charges. The third slot is a 100 s charge that both new spells share: the chieftain's panel shows two more buttons, left of the old ones, and either may be cast once the charge is full. In code they are spells 2 and 3 (`RacesResources.NUM_MAGIC` is 4), each with a 100 s charge; since every cast still empties every charge, and all charges fill together, two charges of the same length are always equal, so this is one slot holding two spells without a picker. The first two entries of every spell array are unchanged.
- **The cost of waiting.** Casting any spell empties all four charges (the 2004 rule). To cast a new spell a chieftain must go 100 s without casting; by a finished Lodge of its team (R-38) all four charge twice as fast, the new ones in 50 s.
- **Unlocking.** As in the 2004 campaign, a player's chieftain casts only the spells the player has (`Player.enableMagic`). The new spells start enabled where the ruleset has `features.new_spells` (Buffed) and disabled elsewhere, so a skirmish under Buffed has all four from the start, and a campaign island can lock or unlock them one by one as the 2004 islands do with the old ones. Act III (M17) will keep its own unlocks in `CampaignState` (new boolean fields default to false, a compatible change to the save format).
- **Keys.** V and B (the old spells are S and C): free in the unit context outside developer mode (where V freezes the frustum, as in R-48). The keys belong to the slots, not the spells: V casts Jolly Jungle or Hammer of Thor, B Poultry Panic or Fjord Fog.

## Numbers (Buffed)

In `spells` (`resurrected.json` holds them too, with the feature off; R-14):

| Field | Value | Meaning |
|---|---|---|
| `third_slot_seconds` | 100 | the new spells' charge |
| `jolly_jungle.radius`, `.seconds` | 20 m, 8 s | who is rooted, for how long |
| `poultry_panic.radius`, `.stun_seconds`, `.chickens` | 30 m, 3 s, 5 | who is knocked flat, for how long; chickens left |
| `hammer_of_thor.range`, `.damage` | 20 m, 40 | how far the chieftain strikes; hit points the bolt takes |
| `fjord_fog.radius`, `.seconds`, `.hit_penalty` | 30 m, 20 s, 0.2 | the mist's size and life; hit chance taken from enemies in it |

How they compare: the old spells are area attacks (the Stew 10 damage every 2 s for 20 s in 26 m, the Cloud 30 a strike for 22 s, the Toot a stun of 10 to 30 s in 36 m, the Roar up to 150 damage in 36 m). The new ones are control and support for a longer charge: the Toot stuns longer than either Native spell holds anyone, but stops attacks too; roots and the fog leave the enemy fighting, worse. The Hammer is the one sniper among them.

## The spells

- **Jolly Jungle** (Natives, the magic animation). When the spell is released, every enemy unit on the ground within 20 m of the chieftain is rooted for 8 s: it cannot walk, but it still throws, strikes, gathers and builds within reach, and keeps its dodge (unlike a stun). A walking unit stops where it is and goes on when the vines let go; a unit sent to attack something that comes within its reach attacks it. A rooted unit blocks the cells it stands on like a building, so others path around it. Rooting again takes the longer of the two times. Units in towers and on ships are not rooted (they do not walk).
- **Poultry Panic** (Natives, the magic animation). When the spell is released, every enemy unit on the ground within 30 m of the chieftain is knocked flat for 3 s, stunned as by *Terrifying Toot* (no attacks, no dodge), and five chickens fly out from the chieftain to free cells 5 to 8 m around it, one in each of five directions. They form a flock of their own, like the Chicken Coop's: outside the island's three wild flocks, and anyone may catch them.
- **Hammer of Thor** (Vikings, the `thor` animation, position 5 in `Unit.Animation`). The button (or V) and then a click on an enemy unit or building: the chieftain walks to within 20 m of it and raises the hammer; one bolt strikes the target for 40 hit points with no roll (a unit of one hit point dies; a building or ship loses 40; a chieftain 40, which fells a Native chieftain at full health, as two strikes of the *Crackling Cloud* fell a Viking one). The bolt strikes a tower, not the thrower inside. If the target dies, or another spell spends the charge first, the order ends without a cast. Other units in the selection take the click as an attack order. The click is a value appended to `Action` (`THOR`), sent with the existing `setTarget`, so there is no new player command.
- **Fjord Fog** (Vikings, the magic animation). When the spell is released, a mist 30 m across settles where the chieftain stands and stays 20 s. An enemy unit whose throw or blow starts inside it hits with 0.2 less chance, taken inside the hit chance like the Totem's bonus (R-17), so a tower triples the lower chance; a chance below zero never hits. Fogs do not add up. Spells, and the chieftain's own team, are not affected. With no fog in the world the hit roll reads an empty list and is computed exactly as before, so Classic's floats do not change.
- **Animals** (M9's fauna) are not units, and no spell touches them: they are not rooted, knocked flat or struck, and fight as before in a fog.

## The AI (Normal and Hard)

The chieftain AI (`NativeChieftainAI`, `VikingChieftainAI`) considers the new spells only where the player has them and the AI is Normal or Hard; Easy keeps the 2004 spells (R-20). In a skirmish only the Hard AI fields a chieftain (it joins attacks from the third one on), so in practice the Hard AI casts them.

Every cast empties every charge, and the old spells, charged first, would always fire before the third slot is full. So a full third slot **opens the next fight**: while it is full the AI holds the 2004 spells and casts a new one when:

- **Natives**, at least 2 enemy units within 20 m: *Poultry Panic* if at least 6 are within 30 m, otherwise *Jolly Jungle*.
- **Vikings**: *Fjord Fog* with at least 5 enemy units and at least 3 of its own units or buildings within 30 m; otherwise, with at least 2 enemy units within 30 m, *Hammer of Thor* at the best enemy unit within 20 m (a chieftain, then a Champion or a drummer, then any warrior, then a peon, the nearest of the best kind); with no enemy unit within 30 m, the Hammer at an enemy building within 20 m (a tower first): a siege bolt.

After the cast the 2004 rules apply again as their charges fill, until the third slot is full once more.

In the headless matches the two Hard AIs cast each of the four (BuffedSpellsTest); across ten 1v1 seeds the Natives cast 0 to 3 Jungles and 0 to 2 Panics a match, the Vikings 0 to 3 Hammers and 0 to 2 Fogs, and nine of the ten matches ended within 30 minutes (seed 7, the pinned one, did not: the Natives sat at the unit cap; R-59).

## Interface and art (placeholders)

- Two more buttons in the chieftain's panel, a column left of the old two; the third slot's watch shows the charge. Tooltips name each spell with its key and effect, in six languages. A cheat (Shift+F1 after `/iamacheater`, Buffed only) fills the chieftain's charges.
- Icons in the free space at the bottom right of `icons.png` (x 768 to 1024, y 1024 to 1216; existing pixels unchanged).
- Effects reuse the existing emitters: leaves (a new 64² texture, like the star's) circle a rooted unit's feet for its root and burst from the Native chieftain; feathers (a new texture) burst from Poultry Panic over the stun stars; the Hammer is a lightning bolt from the sky (the Cloud's `Lightning` and sound); the fog is the Stew's bursts in pale grey smoke. Sounds reuse the existing clips (PLAN.md C6): the Stew's gas for the vines, the chickens' clucks for the stampede, the lightning for the Hammer, the Cloud's rumble for the fog.
