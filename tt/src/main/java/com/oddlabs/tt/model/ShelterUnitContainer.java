package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Lodge's shelter (Buffed, docs/design/lodge-and-champion.md): any of its owner's units but the chieftain, out of
 * the world while inside, as peons are in the Quarters. They count towards the unit limit and leave in the order they
 * went in, as what they were: the shelter keeps the template of each.
 */
public final class ShelterUnitContainer extends UnitContainer {
    private final @NonNull Building building;
    // The units inside, in the order they went in. The first getNumPreparing() of them are being let out.
    private final @NonNull List<@NonNull UnitTemplate> sheltered = new ArrayList<>();

    public ShelterUnitContainer(@NonNull Building building, int capacity) {
        super(capacity);
        this.building = building;
    }

    @Override
    public void enter(@NonNull Unit unit) {
        assert canEnter(unit);
        sheltered.add(unit.getTemplate());
        unit.removeNow();
        increaseSupply(1);
    }

    @Override
    public boolean canEnter(@NonNull Unit unit) {
        return getNumSupplies() + getNumPreparing() != getMaxSupplyCount() && !unit.isChieftain();
    }

    /** Lets out the unit that went in first; {@link #exitSheltered} says which it was. */
    @Override
    public @Nullable Unit exit() {
        exitSheltered();
        return null;
    }

    /** Lets out the unit that went in first and returns its template, to create it outside. */
    public @NonNull UnitTemplate exitSheltered() {
        assert getNumSupplies() > 0;
        increaseSupply(-1);
        return sheltered.removeFirst();
    }

    /** How many of the units inside, not counting those being let out, are of this template. */
    public int count(@NonNull UnitTemplate template) {
        int count = 0;
        for (int i = getNumPreparing(); i < sheltered.size(); i++) {
            if (sheltered.get(i) == template)
                count++;
        }
        return count;
    }

    /**
     * Takes out the first unit of this template that is not being let out, without creating it: a peon who becomes a
     * Champion. Returns whether there was one.
     */
    public boolean take(@NonNull UnitTemplate template) {
        for (int i = getNumPreparing(); i < sheltered.size(); i++) {
            if (sheltered.get(i) == template) {
                sheltered.remove(i);
                increaseSupply(-1);
                return true;
            }
        }
        return false;
    }

    @Override
    public int increaseSupply(int amount) {
        int result = building.getOwner().getUnitCountContainer().increaseSupply(amount);
        assert result == amount : "result = " + result + " | amount = " + amount;
        return super.increaseSupply(amount);
    }
}
