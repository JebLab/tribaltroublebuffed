package com.oddlabs.tt.model;

import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.ScanFilter;
import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class AttackScanFilter implements ScanFilter {
    public enum Priority {
        NONE(0),
        // Only the order of the values matters: every player's target moved up by one to make room for ANIMAL.
        QUARTERS(2),
        ARMORY(2),
        TOWER(3),
        PEON(4),
        WARRIOR(5),
        // Ships stay first.
        SHIP(7),
        // Buffed: a drummer is taken before every other unit (docs/design/drum-and-net.md).
        DRUMMER(6),
        // Buffed: a wild animal is taken only when nothing of a player is in reach (docs/design/fauna.md).
        ANIMAL(1);

        public final int value;

        Priority(int value) {
            this.value = value;
        }
    }

    public static final int UNIT_RANGE = 8;
    public static final int TOWER_RANGE = (int) (RacesResources.THROW_RANGE + MountUnitContainer.ATTACK_RANGE_INCREASE);

    private final int max_range;

    private final @NonNull Player owner;
    private final boolean hunts_animals;

    private @Nullable Hittable target = null;
    private @NonNull Priority target_priority = Priority.NONE;

    public AttackScanFilter(@NonNull Player owner, int max_range) {
        this(owner, max_range, false);
    }

    /** @param hunts_animals whether wild animals are targets too: for warriors and towers (Buffed's fauna) */
    public AttackScanFilter(@NonNull Player owner, int max_range, boolean hunts_animals) {
        this.owner = owner;
        this.max_range = max_range;
        this.hunts_animals = hunts_animals;
    }

    public @Nullable Hittable removeTarget() {
        Hittable result = target;
        target = null;
        target_priority = Priority.NONE;
        return result;
    }

    @Override
    public int getMinRadius() {
        return 1;
    }

    @Override
    public int getMaxRadius() {
        return max_range;
    }

    @Override
    public boolean filter(int grid_x, int grid_y, @NonNull Occupant occ) {
        if (occ instanceof Selectable<?> s && !s.isDead() && owner.isEnemy(s.getOwner())) {
            Priority priority = s.getAttackPriority();
            if (target_priority.value < priority.value) {
                target_priority = priority;
                target = s;
            }
        } else if (hunts_animals && occ instanceof Animal animal && animal.isPrey() && !animal.isDead()) {
            if (target_priority.value < Priority.ANIMAL.value) {
                target_priority = Priority.ANIMAL;
                target = animal;
            }
        }
        return false;
    }
}
