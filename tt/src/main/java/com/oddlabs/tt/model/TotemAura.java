package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.ruleset.RulesetStats.TotemStats;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * The Totem's aura (Buffed): a unit within {@code radius} meters of finished totems of its own team gets
 * {@code hit_bonus} added to its hit chance per totem, for at most {@code max_stacking} totems. The numbers are the
 * attacking unit's race's.
 */
public final class TotemAura {
    private TotemAura() {
    }

    public static float getHitBonus(@NonNull Selectable<?> unit) {
        Player owner = unit.getOwner();
        World world = owner.getWorld();
        List<LandBuilding> totems = world.getTotems();
        if (totems.isEmpty())
            return 0f;
        TotemStats stats = world.getRuleset().getStats().race(
                owner.getPlayerInfo().getRace() == RacesResources.RACE_VIKINGS).totem();
        float radius_squared = stats.radius() * stats.radius();
        int team = owner.getPlayerInfo().getTeam();
        int count = 0;
        for (LandBuilding totem : totems) {
            if (totem.getOwner().getPlayerInfo().getTeam() != team)
                continue;
            float dx = totem.getPositionX() - unit.getPositionX();
            float dy = totem.getPositionY() - unit.getPositionY();
            if (dx * dx + dy * dy <= radius_squared)
                count++;
        }
        return Math.min(count, stats.max_stacking()) * stats.hit_bonus();
    }
}
