package com.oddlabs.tt.pathfinder;

import org.jspecify.annotations.NonNull;

/**
 * An occupant some movers may walk through (Buffed's Gate). {@link UnitGrid} keeps gates in a layer of their own: a
 * gate is its cell's occupant until a mover it admits steps in, and gets the cell back when the mover leaves.
 */
public interface Gate extends Occupant {
    boolean admits(@NonNull Movable movable);
}
