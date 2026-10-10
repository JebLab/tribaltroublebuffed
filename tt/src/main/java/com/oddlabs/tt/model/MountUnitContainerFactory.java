package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

public final class MountUnitContainerFactory implements UnitContainerFactory {
    private final int capacity;

    public MountUnitContainerFactory() {
        this(1);
    }

    /** @param capacity the throwers the tower holds: one, or several in Buffed's Great Tower */
    public MountUnitContainerFactory(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public @NonNull UnitContainer createContainer(@NonNull Building building) {
        return new MountUnitContainer(building, capacity);
    }
}
