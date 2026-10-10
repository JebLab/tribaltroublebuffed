# Market

Buffed's resource converter (M7), from PLAN.md §4.5: the Trading Post (Natives) and the Market (Vikings). It solves lopsided resource spawns, an island with no iron or no rock, at a premium: three of one resource for one of another. Classic and Resurrected do not have it (the `features.market` flag is false there).

## Numbers (Buffed, both races)

| | Market |
|---|---|
| Footprint | size 3: 3 × 3 grid cells (6 × 6 m), like the Chicken Coop |
| Hit points | 150: 30 logs |
| Trade | 3 of the given resource for 1 of the resource asked for |
| Labour | 20 man-seconds a trade: one peon inside trades every 20 s, two every 10 s |

The numbers are ruleset fields (`market` in `buffed.json`: `hit_points`, `give`, `get`, `seconds`; also in `resurrected.json`, where the feature keeps it out; R-14).

## Rules

- **Peons inside do the work.** Peons enter a finished Market like an Armory (right-click it); nobody else can (warriors keep their weapons, the chieftain stays out). The Market's panel lets them out again (the peon spinner) and has a rally point. Each peon inside adds one man-second a second, as in the Armory's weapon production.
- **What it trades.** The player picks the resource to give and the resource to get among wood, rock, iron and chickens (two buttons on the Market's panel: each click moves to the next resource; the two are never the same). A new Market gives wood for iron.
- **Where the goods are.** Resources live in the Armories, so the Market trades with its owner's nearest finished Armory: it takes the 3 from that Armory's store and puts the 1 back into it. While it cannot trade (no Armory, fewer than 3 of the given resource there, or the asked-for store full at 200), its work waits: the labour neither runs nor accumulates.
- **One new player command**, `setTrade(market, give, get)`, which renumbers the game events (like M6's gear orders); the AI calls `Player` directly.
- **Counters.** It is a 150-hit-point building like any other: torches set it on fire (a fire burns every land building), warriors knock it down, and it holds no warriors. Its peons are units: they count towards the unit limit and keep their owner alive, and they die if it falls, as an Armory's workers do.

## The AI (Normal and Hard)

- **When.** A Market once its base is done, when its island lacks rock or iron (the plan's lopsided case) above 20 units, or anyway above 60 (Normal) or 45 (Hard) units. One Market at most. Easy builds none (as with M5's buildings).
- **What to convert.** Each decision it looks at the nearest Armory's store: it asks for the scarcer of rock and iron and gives the most plentiful other resource (wood, rock or iron), but only while the giving store holds at least twice as much plus 6; otherwise it leaves the trade as it is. It never trades chickens.
- **Workers.** It keeps 2 (Normal) or 3 (Hard) peons inside, sent from its idle peons and gatherers.

## Art (placeholders)

New sprites `natives/market` and `vikings/market` with their halfbuilt and start stages: the race's Armory stages scaled down to the coop's footprint, with tinted copies of the Armory textures. Icons on the shared sheet. Final art needs an artist (MILESTONES.md).
