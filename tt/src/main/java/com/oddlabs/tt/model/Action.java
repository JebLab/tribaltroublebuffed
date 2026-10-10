package com.oddlabs.tt.model;

public enum Action {
    DEFAULT,
    MOVE,
    ATTACK,
    GATHER_REPAIR,
    DEFEND,
    // Buffed: a chicken catcher lays a snare there (docs/design/drum-and-net.md). Last, so the others keep their
    // ordinals in recorded games.
    SNARE
}
