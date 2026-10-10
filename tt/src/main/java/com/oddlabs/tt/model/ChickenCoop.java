package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.ruleset.RulesetStats.ChickenCoopStats;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;

/**
 * What a finished Chicken Coop does (Buffed): once peons have brought it its stock of chickens, it lets out a new
 * catchable chicken every {@code spawn_seconds} while fewer than {@code max_chickens} of its own roam. Its chickens
 * are ordinary chickens anyone can catch, and do not count towards the island's limit of wild flocks.
 */
public final class ChickenCoop {
    private final @NonNull LandBuilding building;
    private final @NonNull ChickenCoopStats stats;
    private final @NonNull SupplyContainer stock;
    private final @NonNull RubberGroup flock;

    private float time_to_spawn;

    ChickenCoop(@NonNull LandBuilding building, @NonNull ChickenCoopStats stats) {
        this.building = building;
        this.stats = stats;
        this.stock = new SupplyContainer(stats.stock());
        this.flock = RubberGroup.newCoopFlock(building.getOwner().getWorld());
        this.time_to_spawn = stats.spawn_seconds();
    }

    /** The chickens brought so far; the coop breeds once it is full. */
    public @NonNull SupplyContainer getStock() {
        return stock;
    }

    public boolean needsStock() {
        return !stock.isSupplyFull();
    }

    void addStock() {
        stock.increaseSupply(1);
    }

    /** Chickens of this coop still roaming. */
    public int getNumChickens() {
        return flock.size();
    }

    void animate(float t) {
        if (needsStock())
            return;
        if (flock.size() >= stats.max_chickens()) {
            time_to_spawn = stats.spawn_seconds();
            return;
        }
        time_to_spawn -= t;
        if (time_to_spawn <= 0) {
            time_to_spawn += stats.spawn_seconds();
            spawnChicken();
        }
    }

    private void spawnChicken() {
        // Land on a random side of the coop: the free cell nearest to a random point just outside its walls.
        World world = building.getOwner().getWorld();
        int reach = building.getTemplate().getPlacingSize();
        int grid_x = building.getGridX() + world.getRandom().nextInt(2 * reach + 1) - reach;
        int grid_y = building.getGridY() + world.getRandom().nextInt(2 * reach + 1) - reach;
        Target target = world.getUnitGrid().findGridTargets(grid_x, grid_y, 1, true)[0];
        if (target != null)
            flock.spawn(target, building.getPositionX(), building.getPositionY());
    }
}
