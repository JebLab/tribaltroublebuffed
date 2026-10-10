package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.ruleset.RulesetStats.MonkeyStats;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A monkey (Buffed, docs/design/fauna.md), on tropical islands only. It waits at a forest edge; when a unit of any
 * player walks by within its sight carrying a load (wood, rock, iron or a chicken), it runs to it, takes the load and
 * runs back into the trees, where the load is lost; then it rests. It never attacks. Warriors and towers may kill it.
 */
public final class Monkey extends Animal {
    private enum State {
        WAIT,
        CHASE,
        RETURN,
        REST
    }

    private @NonNull State state = State.WAIT;
    private @Nullable Unit carrier;

    public Monkey(@NonNull World world, int grid_x, int grid_y) {
        super(world, Species.MONKEY, grid_x, grid_y);
    }

    @Override
    public boolean isPrey() {
        return true;
    }

    /** A unit walking by with a load. */
    private static boolean isPassingCarrier(@NonNull Unit unit) {
        return unit.isCarrying() && unit.isMoving();
    }

    @Override
    protected void think() {
        MonkeyStats stats = getStats().monkey();
        switch (state) {
            case WAIT -> {
                carrier = findUnit(stats.sight(), Monkey::isPassingCarrier);
                if (carrier != null) {
                    state = State.CHASE;
                    walkTo(carrier, stats.speed());
                }
            }
            case CHASE -> {
                Unit target = carrier;
                if (target == null || !target.isCarrying()
                        || !isWithin(getHomeX(), getHomeY(), target.getGridX(), target.getGridY(), stats.leash())) {
                    goHome(stats);
                } else if (isInReach(target)) {
                    attack(target);
                } else {
                    walkTo(target, stats.speed());
                }
            }
            case RETURN -> {
                if (!isWalking()) {
                    state = State.REST;
                    rest(stats.rest_seconds());
                }
            }
            case REST -> {
                if (!isResting())
                    state = State.WAIT;
            }
        }
    }

    @Override
    protected void strike() {
        Unit target = carrier;
        if (target != null && target.isCarrying() && isInReach(target)) {
            target.loseLoad();
            target.getOwner().loadStolen();
        }
    }

    @Override
    protected void attackDone() {
        goHome(getStats().monkey());
    }

    private void goHome(@NonNull MonkeyStats stats) {
        carrier = null;
        state = State.RETURN;
        walkHome(stats.speed());
    }
}
