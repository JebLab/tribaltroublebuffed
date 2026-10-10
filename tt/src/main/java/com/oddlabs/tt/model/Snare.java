package com.oddlabs.tt.model;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.ruleset.RulesetStats.NetStats;
import com.oddlabs.tt.util.StateChecksum;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * A snare laid by a Chicken Catcher or Fowler (Buffed, docs/design/drum-and-net.md): it lies on one grid cell, which
 * units walk over, until the first enemy unit steps onto it; that unit is stunned and the snare is gone. Everyone sees
 * it, nothing avoids it, and it cannot be attacked. A catcher keeps at most {@link NetStats#snares()} lying; they go
 * with it when it leaves the world.
 */
public final class Snare extends SceneryModel {
    private final @NonNull Unit catcher;
    private final @NonNull Player owner;
    private final int grid_x;
    private final int grid_y;
    private final float stun_seconds;
    // Checked once a tick, in the world's animation order, so that a snare springs identically on every client.
    private final @NonNull Animated trap = new Animated() {
        @Override
        public void animate(float t) {
            spring();
        }

        @Override
        public void updateChecksum(@NonNull StateChecksum checksum) {
            checksum.update(grid_x);
            checksum.update(grid_y);
        }
    };

    private Snare(@NonNull Unit catcher, int grid_x, int grid_y, float stun_seconds) {
        super(catcher.getOwner().getWorld(), UnitGrid.coordinateFromGrid(grid_x), UnitGrid.coordinateFromGrid(
                grid_y), catcher.getDirectionX(), catcher.getDirectionY(), catcher.getOwner().getWorld().getRacesResources().getSnareSprite(
                        catcher.getOwner().getPlayerInfo().getRace()), 0f, false, catcher.getOwner().getWorld().getRacesResources().getSnareName());
        this.catcher = catcher;
        this.owner = catcher.getOwner();
        this.grid_x = grid_x;
        this.grid_y = grid_y;
        this.stun_seconds = stun_seconds;
        World world = owner.getWorld();
        world.getSnares().add(this);
        catcher.getSnares().add(this);
        world.getAnimationManagerGameTime().registerAnimation(trap);
        owner.snareLaid();
    }

    /**
     * Lays a snare on a cell next to the catcher, or under it when something stands on that cell, unless a snare lies
     * there already. A catcher with its snares out takes up its oldest first.
     *
     * @return whether a snare was laid
     */
    public static boolean lay(@NonNull Unit catcher, int grid_x, int grid_y) {
        World world = catcher.getOwner().getWorld();
        Occupant occupant = world.getUnitGrid().getOccupant(grid_x, grid_y);
        if (occupant != null && occupant != catcher) {
            grid_x = catcher.getGridX();
            grid_y = catcher.getGridY();
        }
        for (Snare snare : world.getSnares()) {
            if (snare.grid_x == grid_x && snare.grid_y == grid_y)
                return false;
        }
        NetStats stats = world.getRuleset().getStats().race(
                catcher.getOwner().getPlayerInfo().getRace() == RacesResources.RACE_VIKINGS).net();
        List<Snare> snares = catcher.getSnares();
        while (!snares.isEmpty() && snares.size() >= stats.snares())
            snares.getFirst().pullUp();
        new Snare(catcher, grid_x, grid_y, stats.stun_seconds());
        return true;
    }

    private void spring() {
        Occupant occupant = owner.getWorld().getUnitGrid().getOccupant(grid_x, grid_y);
        if (occupant instanceof Unit unit && !unit.isDead() && owner.isEnemy(unit.getOwner())) {
            pullUp();
            owner.snareSprung();
            unit.stun(stun_seconds);
        }
    }

    /** Takes the snare out of the world: sprung, replaced by a newer one, or gone with its catcher. */
    public void pullUp() {
        World world = owner.getWorld();
        world.getSnares().remove(this);
        catcher.getSnares().remove(this);
        world.getAnimationManagerGameTime().removeAnimation(trap);
        remove();
    }

    public @NonNull Player getSnareOwner() {
        return owner;
    }

    public @NonNull Unit getCatcher() {
        return catcher;
    }
}
