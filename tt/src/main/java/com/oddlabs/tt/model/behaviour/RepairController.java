package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Supply;
import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class RepairController extends Controller {
    private enum State {
        HARVEST,
        REPAIR
    }

    // How far, in grid cells of 2 m, a Palisade builder looks for the next segment.
    private static final int NEXT_WALL_CELLS = 8;

    private final Building building;
    private final Unit unit;

    public RepairController(Unit unit, Building building) {
        super(State.values().length);
        this.unit = unit;
        this.building = building;
    }

    public Building getBuilding() {
        return building;
    }

    @Override
    public @NonNull Object getKey() {
        // Building does not override equals, so repairers group per building by identity. Only the equality
        // matters: the identity hash code merely picks a hash bucket and never decides group order or membership.
        return List.of(super.getKey(), building);
    }

    /**
     * Fetches what the building needs and works it in: wood for every building, a rock to finish the Totem and
     * chickens to stock the Chicken Coop. A building that takes nothing but wood sees exactly the 2004 behaviour.
     */
    @Override
    public void decide() {
        if (building.isDead()) {
            unit.popController();
            return;
        }
        if (building.isWall() && !building.hasWork()) {
            // A Palisade's builders move along the line (Buffed).
            Building next = findNextWallSite();
            if (next != null) {
                unit.swapController(new RepairController(unit, next));
                return;
            }
        }
        Class<? extends Supply> wanted = building.getWorkMaterial();
        Class<? extends Supply> carried = unit.getSupplyContainer().getNumSupplies() > 0 ? unit.getSupplyContainer().getSupplyType() : null;
        // Wood the building does not want now (the Totem waits for its rock) counts as carrying nothing: the peon
        // goes for what is wanted.
        if (carried != null && (building.needsMaterial(carried)
                || (carried == TreeSupply.class && wanted == TreeSupply.class))) {
            resetGiveUpCounter(State.HARVEST.ordinal());
            if (unit.isCloseEnough(0f, building)) {
                if (building.needsMaterial(carried)) {
                    unit.setBehaviour(new RepairBehaviour(unit, building, carried));
                } else if (carried == TreeSupply.class
                        && building.getAbilities().hasAbilities(Abilities.SUPPLY_CONTAINER)
                        && unit.getOwner() == building.getOwner()) {
                            unit.swapController(new EnterController(unit, building));
                        } else {
                            unit.popController();
                        }
            } else {
                if (shouldGiveUp(State.REPAIR.ordinal())) {
                    unit.popController();
                } else {
                    unit.setBehaviour(new WalkBehaviour(unit, building, 0, false));
                }
            }
        } else {
            resetGiveUpCounter(State.REPAIR.ordinal());
            if (!shouldGiveUp(State.HARVEST.ordinal())) {
                unit.pushController(newHarvestController(wanted));
            } else {
                unit.popController();
            }
        }
    }

    /** The owner's unfinished or damaged wall nearest to the peon within {@link #NEXT_WALL_CELLS}, or null. */
    private @Nullable Building findNextWallSite() {
        Building best = null;
        int best_dist_squared = NEXT_WALL_CELLS * NEXT_WALL_CELLS + 1;
        for (Selectable<?> s : unit.getOwner().getUnits().getSet()) {
            if (s instanceof Building wall && wall != building && !wall.isDead() && wall.isWall() && wall.isPlaced()
                    && wall.hasWork()) {
                int dx = wall.getGridX() - unit.getGridX();
                int dy = wall.getGridY() - unit.getGridY();
                int dist_squared = dx * dx + dy * dy;
                if (dist_squared < best_dist_squared) {
                    best_dist_squared = dist_squared;
                    best = wall;
                }
            }
        }
        return best;
    }

    private <S extends Supply> @NonNull HarvestController<S> newHarvestController(@NonNull Class<S> material) {
        return new HarvestController<>(unit, null, material);
    }
}
