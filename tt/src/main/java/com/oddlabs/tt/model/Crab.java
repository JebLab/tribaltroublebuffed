package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.ruleset.RulesetStats.CrabStats;
import org.jspecify.annotations.NonNull;

import java.util.Random;

/**
 * A beach crab (Buffed, docs/design/fauna.md): ambience. It wanders along the shore near where it was placed and
 * scuttles away from any unit that comes close. It harms no one, and no one attacks it.
 */
public final class Crab extends Animal {
    private static final int ROAM_CELLS = 3;
    private static final float WANDER_CHANCE = .15f;
    private static final float WANDER_SPEED_FACTOR = .5f;
    private static final int FLEE_TRIES = 4;

    private boolean fleeing;

    public Crab(@NonNull World world, int grid_x, int grid_y) {
        super(world, Species.CRAB, grid_x, grid_y);
    }

    @Override
    public boolean isPrey() {
        return false;
    }

    @Override
    protected void think() {
        if (!isWalking())
            fleeing = false;
        CrabStats stats = getStats().crab();
        Unit threat = findUnit(stats.flee_radius(), unit -> true);
        Random random = getWorld().getRandom();
        if (threat != null && !fleeing) {
            flee(threat, stats.speed());
        } else if (!isWalking() && random.nextFloat() < WANDER_CHANCE) {
            walkToCell(getHomeX() + random.nextInt(2 * ROAM_CELLS + 1) - ROAM_CELLS,
                    getHomeY() + random.nextInt(2 * ROAM_CELLS + 1) - ROAM_CELLS,
                    stats.speed() * WANDER_SPEED_FACTOR);
        }
    }

    /** Runs to the cell near home, of a few picked at random, that is farthest from the threat. */
    private void flee(@NonNull Unit threat, float speed) {
        Random random = getWorld().getRandom();
        int best_x = getGridX();
        int best_y = getGridY();
        int best_distance = -1;
        for (int i = 0; i < FLEE_TRIES; i++) {
            int x = getHomeX() + random.nextInt(2 * ROAM_CELLS + 3) - ROAM_CELLS - 1;
            int y = getHomeY() + random.nextInt(2 * ROAM_CELLS + 3) - ROAM_CELLS - 1;
            int dx = x - threat.getGridX();
            int dy = y - threat.getGridY();
            int distance = dx * dx + dy * dy;
            if (distance > best_distance) {
                best_distance = distance;
                best_x = x;
                best_y = y;
            }
        }
        fleeing = true;
        walkToCell(best_x, best_y, speed);
    }
}
