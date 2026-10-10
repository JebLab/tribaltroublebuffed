package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.model.weapon.IronAxeWeapon;
import com.oddlabs.tt.model.weapon.RockAxeWeapon;
import com.oddlabs.tt.model.weapon.RubberAxeWeapon;
import com.oddlabs.tt.pathfinder.Occupant;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.ruleset.RulesetStats.TorchStats;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;


public abstract class Building extends Selectable<BuildingTemplate> implements Occupant {
    public enum BuildState {
        START,
        HALFBUILT,
        BUILT
    }

    public Building(@NonNull Player owner, @NonNull BuildingTemplate template) {
        super(owner, template);
    }

    public abstract boolean hasRallyPoint();

    public abstract Target getRallyPoint();

    public abstract @Nullable UnitContainer getUnitContainer();

    public abstract @Nullable SupplyContainer getSupplyContainer(@NonNull Class<?> key);

    public abstract @Nullable BuildSupplyContainer getBuildSupplyContainer(@NonNull Class<?> key);

    public abstract DeployContainer getDeployContainer(DeployType type);

    public abstract @Nullable ChieftainContainer getChieftainContainer();

    public abstract int getUnitCount();

    public abstract boolean canExitTower();

    public abstract void exitTower();

    public abstract void deployUnits(@NonNull DeployType type, int num_units);

    public abstract void createHarvesters(int num_tree, int num_rock, int num_iron, int num_rubber);

    public abstract void buildWeapons(@NonNull Class<?> type, int num_weapons, boolean infinite);

    public abstract boolean canBuildChieftain();

    public abstract boolean canStopChieftain();

    public abstract void trainChieftain(boolean start);

    public abstract void deployChieftain();

    public abstract void createArmy(int num_peon, int num_rock, int num_iron, int num_rubber);

    /** Deploys warriors with Buffed's gear, which only an Armory stocks (gear warriors do not board ships). */
    public void createGearArmy(int num_shield, int num_torch) {
        throw new IllegalStateException(this + " has no gear");
    }

    /** Deploys one warrior with Buffed's Drum or Net ({@code Race.UNIT_WARRIOR_DRUM} or {@code UNIT_WARRIOR_NET}). */
    public void createGearWarrior(int template) {
        throw new IllegalStateException(this + " has no gear");
    }

    /**
     * A torch's blow (Buffed): sets the building on fire for the torch's {@code fire_seconds}, or starts them again.
     * Buildings that cannot burn (ships) ignore it.
     */
    public void ignite(@NonNull TorchStats torch, @NonNull Player burner) {
    }

    /**
     * Sets what a finished Market trades (Buffed): {@code give} for {@code get}, as {@link Market} resource indices.
     */
    public void setTrade(int give, int get) {
    }

    /**
     * Orders Champions at a finished Lodge (Buffed), as weapons are ordered at an Armory: {@code infinite} keeps
     * training. Other buildings ignore it.
     */
    public void trainChampions(int num_champions, boolean infinite) {
    }

    /** Lets out the unit that went into a Lodge first (Buffed); only a Lodge shelters units. */
    public void createSheltered() {
        throw new IllegalStateException(this + " shelters nobody");
    }

    /** Whether this is a Palisade segment or a Gate (Buffed). */
    public final boolean isWall() {
        return Race.isWall(getTemplate().getTemplateID());
    }

    /** Whether this is a Great Tower (Buffed), which has the Tower's job for several throwers. */
    public final boolean isGreatTower() {
        return getTemplate().getTemplateID() == Race.BUILDING_GREAT_TOWER;
    }

    public abstract void createTransporters(int num_tree, int num_rock, int num_iron, int num_rubber);

    public abstract boolean isDamaged();

    public abstract int getHitPoints();

    public abstract void repair(int amount);

    /**
     * What a peon sent to work on this building should fetch: wood, unless the building needs something else first
     * (the Totem's finishing rock, the Chicken Coop's stock of chickens).
     */
    public @NonNull Class<? extends Supply> getWorkMaterial() {
        return TreeSupply.class;
    }

    /** Whether one load of {@code material} would be used here now. Wood repairs (and builds) damaged buildings. */
    public boolean needsMaterial(@NonNull Class<? extends Supply> material) {
        return material == TreeSupply.class && isDamaged();
    }

    /** Whether peons have work here: for most buildings, whether it is damaged. */
    public final boolean hasWork() {
        return needsMaterial(getWorkMaterial());
    }

    /** Hands over one load of a material other than wood, which {@link #needsMaterial} asked for. */
    public void deliverMaterial(@NonNull Class<? extends Supply> material) {
        throw new IllegalStateException(this + " takes no " + material.getSimpleName());
    }

    public abstract boolean isPlacingLegal();

    public abstract boolean isPlaced();

    public abstract boolean isComplete();

    public abstract void place();

    public abstract boolean isValidRallyPoint(Target t);

    public abstract void setRallyPoint(@NonNull Target target);

    public abstract void fillSupplies(@NonNull Class<?> key, int max);

    public abstract void removeSupplies(@NonNull Class<?> key);

    public abstract @NonNull BuildState getRenderLevel();

    public Building getEntrance() {
        return this;
    }

    public final boolean hasExitCell() {
        Building entrance = getEntrance();
        return getUnitGrid().findGridTargets(UnitGrid.toGridCoordinate(entrance.getPositionX()),
                UnitGrid.toGridCoordinate(entrance.getPositionY()), 1, true, entrance.getIslandId())[0] != null;
    }

    public Building getBase() {
        return this;
    }

    public void printDebugInfo() {
        IO.println("-----------------------------------");
        if (getAbilities().hasAbilities(Abilities.REPRODUCE)) {
            IO.println("Units = " + getUnitContainer().getNumSupplies());
        } else if (getAbilities().hasAbilities(Abilities.BUILD_ARMIES)) {
            IO.println("Units = " + getUnitContainer().getNumSupplies());
            IO.println("Tree = " + getSupplyContainer(TreeSupply.class).getNumSupplies());
            IO.println("Rock = " + getSupplyContainer(RockSupply.class).getNumSupplies());
            IO.println("Iron = " + getSupplyContainer(IronSupply.class).getNumSupplies());
            IO.println("Rubber = " + getSupplyContainer(RubberSupply.class).getNumSupplies());
            IO.println("Rock Weapons = " + getSupplyContainer(RockAxeWeapon.class).getNumSupplies());
            IO.println("Iron Weapons = " + getSupplyContainer(IronAxeWeapon.class).getNumSupplies());
            IO.println("Rubber Weapons = " + getSupplyContainer(RubberAxeWeapon.class).getNumSupplies());
        }
    }
}
