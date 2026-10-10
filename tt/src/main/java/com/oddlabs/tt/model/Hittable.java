package com.oddlabs.tt.model;

import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;

/**
 * What a unit's weapon can aim at and hit: a player's unit or building ({@link Selectable}), or one of Buffed's wild
 * animals ({@link Animal}), which belongs to no player.
 */
public interface Hittable extends Target {
    /** Chance that a hit aimed at it misses. */
    float getDefenseChance();

    /** Height above the ground that thrown weapons aim at. */
    float getHitOffsetZ();

    float getPositionZ();

    void hit(int damage, float direction_x, float direction_y, @NonNull Player attacker);
}
