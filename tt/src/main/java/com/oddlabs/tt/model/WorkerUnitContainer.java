package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class WorkerUnitContainer extends UnitContainer {
    private final @NonNull Building building;
    private final boolean peons_only;

    public WorkerUnitContainer(@NonNull Building building, boolean peons_only) {
        super(building.getOwner().getWorld().getMaxUnitCount());
        this.building = building;
        this.peons_only = peons_only;
    }

    @Override
    public void enter(@NonNull Unit unit) {
        assert canEnter(unit);
        unit.removeNow();
        increaseSupply(1);
    }

    @Override
    public boolean canEnter(@NonNull Unit unit) {
        // A Champion has no gear to hand back (Buffed): only a Lodge shelters it.
        return getTotalSupplies() != getMaxSupplyCount()
                && (!peons_only || unit.getAbilities().hasAbilities(Abilities.BUILD)) && !unit.isChampion();
    }

    private int getTotalSupplies() {
//		return getNumSupplies() + building.getBuildSupplyContainer(Unit.class).getNumSupplies() == getMaxSupplyCount();
        return getNumSupplies() + getNumPreparing();
    }

    @Override
    public @Nullable Unit exit() {
        assert getNumSupplies() > 0;
        increaseSupply(-1);
        return null;
    }

    @Override
    public int increaseSupply(int amount) {
        int result = building.getOwner().getUnitCountContainer().increaseSupply(amount);
        assert result == amount : "result = " + result + " | amount = " + amount;
        return super.increaseSupply(amount);
    }
}
