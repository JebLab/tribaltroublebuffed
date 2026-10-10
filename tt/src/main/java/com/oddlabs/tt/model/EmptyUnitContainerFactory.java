package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** For buildings no unit enters (the Chicken Coop, the Totem): a container that always holds nobody. */
public final class EmptyUnitContainerFactory implements UnitContainerFactory {
    @Override
    public @NonNull UnitContainer createContainer(@NonNull Building building) {
        return new UnitContainer(0) {
            @Override
            public void enter(@NonNull Unit unit) {
                throw new IllegalStateException("No unit can enter " + building);
            }

            @Override
            public boolean canEnter(@NonNull Unit unit) {
                return false;
            }

            @Override
            public @Nullable Unit exit() {
                return null;
            }
        };
    }
}
