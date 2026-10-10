package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

public final class ShelterUnitContainerFactory implements UnitContainerFactory {
    private final int capacity;

    /** @param capacity the units the Lodge shelters */
    public ShelterUnitContainerFactory(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public @NonNull UnitContainer createContainer(@NonNull Building building) {
        return new ShelterUnitContainer(building, capacity);
    }
}
