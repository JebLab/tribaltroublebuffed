package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The throwers manning a tower: one in the Tower, several in Buffed's Great Tower (docs/design/great-tower.md). Each
 * stands at its own place on the top and fights on its own with the tower's range bonus.
 */
public final class MountUnitContainer extends UnitContainer {
    public static final float ATTACK_RANGE_INCREASE = 8f;
    // Where the throwers of a tower holding several stand, in meters from its centre.
    private static final float PLACE_OFFSET = 2f;
    private static final float @NonNull [] @NonNull [] PLACES = {{PLACE_OFFSET, 0f}, {-PLACE_OFFSET, PLACE_OFFSET}, {-PLACE_OFFSET, -PLACE_OFFSET}};

    private final @NonNull Building building;
    private final @NonNull List<@NonNull Unit> units = new ArrayList<>();

    public MountUnitContainer(@NonNull Building building) {
        this(building, 1);
    }

    public MountUnitContainer(@NonNull Building building, int capacity) {
        super(capacity);
        assert capacity == 1 || capacity <= PLACES.length;
        this.building = building;
    }

    @Override
    public void enter(@NonNull Unit unit) {
        if (getMaxSupplyCount() == 1) {
            unit.mount(building);
        } else {
            float[] place = PLACES[units.size()];
            unit.mount(building, place[0], place[1]);
        }
        units.add(unit);
        unit.increaseRange(ATTACK_RANGE_INCREASE);
        increaseSupply(1);
        building.getAbilities().addAbilities(Abilities.TARGET);
    }

    /** Lets out the thrower who went in last. */
    @Override
    public @NonNull Unit exit() {
        assert !units.isEmpty();
        Unit result = units.removeLast();
        result.unmount();
        result.increaseRange(-ATTACK_RANGE_INCREASE);
        increaseSupply(-1);
        if (units.isEmpty())
            building.getAbilities().removeAbilities(Abilities.TARGET);
        return result;
    }

    @Override
    public boolean canEnter(@NonNull Unit unit) {
        return !isSupplyFull() && unit.getAbilities().hasAbilities(Abilities.THROW) && !unit.isGearWarrior();
    }

    /** The thrower who would leave first ({@link #exit}), or null if nobody is inside. */
    public @Nullable Unit getUnit() {
        return units.isEmpty() ? null : units.getLast();
    }

    /** Everyone inside, in the order they went in. */
    public @NonNull List<@NonNull Unit> getUnits() {
        return units;
    }
}
