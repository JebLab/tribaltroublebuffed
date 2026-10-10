package com.oddlabs.tt.model;

public enum DeployType {
    ROCK_WARRIOR,
    IRON_WARRIOR,
    RUBBER_WARRIOR,
    PEON,
    PEON_HARVEST_TREE,
    PEON_TRANSPORT_TREE,
    PEON_HARVEST_ROCK,
    PEON_TRANSPORT_ROCK,
    PEON_HARVEST_IRON,
    PEON_TRANSPORT_IRON,
    PEON_HARVEST_RUBBER,
    PEON_TRANSPORT_RUBBER,
    // Buffed's gear. Last, because the ordinal goes into the world checksum.
    SHIELD_WARRIOR,
    TORCH_WARRIOR,
    // Buffed: a unit sheltered in the Lodge, let out as it went in.
    SHELTERED
}
