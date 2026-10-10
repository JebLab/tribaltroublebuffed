package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;

/**
 * Buffed: a chicken catcher walks up to a cell and lays a snare on it (docs/design/drum-and-net.md), from the cell next
 * to it, so that it does not stand on its own snare. Where it cannot get there, it lays the snare where it stands.
 */
public final class SnareController extends Controller {
    // Next to the cell, diagonals included.
    private static final float REACH = 1.5f;

    private final @NonNull Unit unit;
    private final @NonNull Target target;
    private boolean laid;

    public SnareController(@NonNull Unit unit, @NonNull Target target) {
        super(1);
        this.unit = unit;
        this.target = target;
    }

    @Override
    public void decide() {
        if (laid) {
            unit.popController();
        } else if (unit.isCloseEnough(REACH, target)) {
            laid = true;
            unit.setBehaviour(new LaySnareBehaviour(unit, target.getGridX(), target.getGridY()));
        } else if (shouldGiveUp(0)) {
            laid = true;
            unit.setBehaviour(new LaySnareBehaviour(unit, unit.getGridX(), unit.getGridY()));
        } else {
            unit.setBehaviour(new WalkBehaviour(unit, target, REACH, false));
        }
    }
}
