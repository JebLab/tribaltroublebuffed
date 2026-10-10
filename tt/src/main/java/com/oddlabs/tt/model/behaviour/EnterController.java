package com.oddlabs.tt.model.behaviour;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.UnitSupplyContainer;
import com.oddlabs.tt.model.Unit;
import org.jspecify.annotations.NonNull;

public final class EnterController extends Controller {
    private final @NonNull Building building;
    private final @NonNull Unit unit;

    public EnterController(@NonNull Unit unit, @NonNull Building building) {
        super(1);
        this.unit = unit;
        this.building = building.getEntrance();
    }

    public @NonNull Building getBuilding() {
        return building;
    }

    @Override
    public void decide() {
        if (building.isDead()) {
            unit.popController();
        } else if (unit.isCloseEnough(0f, building)) {
            if (building.getUnitContainer() != null && building.getUnitContainer().canEnter(unit)) {
                UnitSupplyContainer unitSupply = unit.getSupplyContainer();
                int numSupply = (unitSupply != null) ? unitSupply.getNumSupplies() : 0;
                if (building.getAbilities().hasAbilities(Abilities.SUPPLY_CONTAINER)) {
                    // A peon's load, or (Buffed) the chicken a catcher carries.
                    if ((unit.getAbilities().hasAbilities(Abilities.HARVEST) || unit.isNetter()) && numSupply > 0) {
                        Class type = unitSupply.getSupplyType();
                        building.getSupplyContainer(type).increaseSupply(numSupply);
                        unitSupply.increaseSupply(-numSupply, type);
                    }
                    // A warrior hands back its weapon or gear.
                    Class<?> type = unit.getWeaponFactory().getType();
                    if (type != null)
                        building.getSupplyContainer(type).increaseSupply(1);
                }
                building.getUnitContainer().enter(unit);
            } else {
                unit.popController();
            }
        } else {
            if (shouldGiveUp(0)) {
                unit.popController();
            } else
                unit.setBehaviour(new WalkBehaviour(unit, building, 0, false));
        }
    }
}
