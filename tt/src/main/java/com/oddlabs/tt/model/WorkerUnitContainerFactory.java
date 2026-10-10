package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

public final class WorkerUnitContainerFactory implements UnitContainerFactory {
    private final boolean peons_only;

    public WorkerUnitContainerFactory() {
        this(false);
    }

    /** @param peons_only whether only peons may enter (the Market): warriors keep their weapons outside */
    public WorkerUnitContainerFactory(boolean peons_only) {
        this.peons_only = peons_only;
    }

    @Override
    public @NonNull UnitContainer createContainer(@NonNull Building building) {
        return new WorkerUnitContainer(building, peons_only);
    }
}
