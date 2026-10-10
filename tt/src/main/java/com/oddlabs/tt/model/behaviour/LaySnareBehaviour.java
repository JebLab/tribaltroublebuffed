package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.model.Snare;
import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

/**
 * Buffed: a chicken catcher bends down for a second (its attack animation) and leaves a snare on a cell next to it or
 * under it.
 */
public final class LaySnareBehaviour implements Behaviour {
    private static final float SECONDS = 1f;
    private final @NonNull Unit unit;
    private final int grid_x;
    private final int grid_y;
    private float anim_time;

    public LaySnareBehaviour(@NonNull Unit unit, int grid_x, int grid_y) {
        this.unit = unit;
        this.grid_x = grid_x;
        this.grid_y = grid_y;
        if (grid_x != unit.getGridX() || grid_y != unit.getGridY())
            unit.aimAtTarget(new LandscapeTarget(grid_x, grid_y));
        unit.switchAnimation(1f / SECONDS, Unit.Animation.THROWING);
    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public @NonNull State animate(float t) {
        anim_time += t;
        if (anim_time < SECONDS)
            return State.UNINTERRUPTIBLE;
        Snare.lay(unit, grid_x, grid_y);
        unit.switchToIdleAnimation();
        return State.DONE;
    }

    @Override
    public void forceInterrupted() {
    }
}
