package com.oddlabs.tt.model;

import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.ruleset.RulesetStats.DrumStats;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The Drum / Horn's aura (Buffed, docs/design/drum-and-net.md): a unit within {@code radius} meters of a drummer of its
 * own team, the drummer itself included, gets {@code hit_bonus} added to its hit chance and walks {@code speed_bonus}
 * faster. Several drummers do not add up. The numbers are the drummer's race's.
 */
public final class DrumAura {
    private DrumAura() {
    }

    /** The first drummer of the unit's team within reach, or null. */
    private static @Nullable Unit findDrummer(@NonNull Selectable<?> unit) {
        List<Unit> drummers = unit.getOwner().getWorld().getDrummers();
        if (drummers.isEmpty())
            return null;
        int team = unit.getOwner().getPlayerInfo().getTeam();
        for (Unit drummer : drummers) {
            if (drummer.getOwner().getPlayerInfo().getTeam() != team)
                continue;
            float radius = stats(drummer.getOwner()).radius();
            float dx = drummer.getPositionX() - unit.getPositionX();
            float dy = drummer.getPositionY() - unit.getPositionY();
            if (dx * dx + dy * dy <= radius * radius)
                return drummer;
        }
        return null;
    }

    private static @NonNull DrumStats stats(@NonNull Player owner) {
        return owner.getWorld().getRuleset().getStats().race(
                owner.getPlayerInfo().getRace() == RacesResources.RACE_VIKINGS).drum();
    }

    /** The hit chance a drummer nearby adds, or 0. */
    public static float getHitBonus(@NonNull Selectable<?> unit) {
        Unit drummer = findDrummer(unit);
        return drummer != null ? stats(drummer.getOwner()).hit_bonus() : 0f;
    }

    /** The factor a drummer nearby multiplies walking speed by, or 1 when none is near. */
    public static float getSpeedFactor(@NonNull Selectable<?> unit) {
        Unit drummer = findDrummer(unit);
        return drummer != null ? 1f + stats(drummer.getOwner()).speed_bonus() : 1f;
    }
}
