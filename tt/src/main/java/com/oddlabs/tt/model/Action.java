package com.oddlabs.tt.model;

public enum Action {
    DEFAULT,
    MOVE,
    ATTACK,
    GATHER_REPAIR,
    DEFEND,
    // Buffed: a chicken catcher lays a snare there (docs/design/drum-and-net.md). Last, so the others keep their
    // ordinals in recorded games.
    SNARE,
    // Buffed: a chieftain casts Hammer of Thor at the target; the rest of a selection attacks it
    // (docs/design/spells.md). Last, as above.
    THOR
}
