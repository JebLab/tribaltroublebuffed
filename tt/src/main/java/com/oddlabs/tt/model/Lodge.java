package com.oddlabs.tt.model;

import com.oddlabs.tt.gui.BuildSpinner;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.particle.LinearEmitter;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.ruleset.RulesetStats.LodgeStats;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * What a finished Spirit Lodge / Mead Hall does (Buffed, docs/design/lodge-and-champion.md) besides sheltering units:
 * it trains Champions, one at a time, from peons sheltered inside, with the cost taken from its owner's nearest
 * finished Armory, and it speeds the spells of chieftains of its team nearby ({@link #getSpellChargeFactor}).
 */
public final class Lodge {
    @SuppressWarnings({"unchecked"})
    public static final Cost COST_CHAMPION = new Cost(
            new Class[]{TreeSupply.class, IronSupply.class, RubberSupply.class}, new int[]{2, 1, 1});

    private final @NonNull LandBuilding building;
    private final @NonNull LodgeStats stats;
    private final @NonNull LinearEmitter emitter;
    private final @NonNull ChampionOrders orders = new ChampionOrders();

    private boolean training = false;
    private float progress = 0;

    /** @param emitter the chimney's smoke, while a Champion trains */
    Lodge(@NonNull LandBuilding building, @NonNull LodgeStats stats, @NonNull LinearEmitter emitter) {
        this.building = building;
        this.stats = stats;
        this.emitter = emitter;
    }

    public @NonNull LodgeStats getStats() {
        return stats;
    }

    /** The Champions ordered here, which the Lodge's spinner shows (keyed by {@code Champion} in the building). */
    @NonNull
    BuildSupplyContainer getOrders() {
        return orders;
    }

    void order(int amount, boolean infinite) {
        orders.orderSupply(amount, infinite);
    }

    public boolean isTraining() {
        return training;
    }

    /** The training of the current Champion, from 0 to 1. */
    public float getProgress() {
        return progress / stats.champion_seconds();
    }

    /** The owner's Champions this Lodge holds: sheltered, and the one in training. */
    public int getChampionsInside() {
        return getShelter().count(getChampionTemplate()) + (training ? 1 : 0);
    }

    private @NonNull ShelterUnitContainer getShelter() {
        return (ShelterUnitContainer) building.getUnitContainer();
    }

    private @NonNull UnitTemplate getChampionTemplate() {
        return building.getOwner().getRace().getUnitTemplate(Race.UNIT_CHAMPION);
    }

    private @NonNull UnitTemplate getPeonTemplate() {
        return building.getOwner().getRace().getUnitTemplate(Race.UNIT_PEON);
    }

    void animate(float t) {
        if (!training) {
            if (orders.getNumSupplies() == 0 || !canStart()) {
                emitter.stop();
                return;
            }
            LandBuilding armory = Market.findArmory(building);
            for (int i = 0; i < COST_CHAMPION.getSupplyTypes().length; i++)
                armory.getSupplyContainer(COST_CHAMPION.getSupplyTypes()[i]).increaseSupply(
                        -COST_CHAMPION.getSupplyAmounts()[i]);
            training = true;
            progress = 0;
        }
        // The training waits while no peon is inside to become the Champion.
        if (getShelter().count(getPeonTemplate()) == 0) {
            emitter.stop();
            return;
        }
        emitter.start();
        progress += t;
        if (progress >= stats.champion_seconds()) {
            getShelter().take(getPeonTemplate());
            training = false;
            progress = 0;
            orders.trained();
            building.createChampion();
        }
    }

    /** A peon inside, the cost in the nearest Armory, and fewer Champions than the limit. */
    private boolean canStart() {
        if (getShelter().count(getPeonTemplate()) == 0
                || building.getOwner().getChampionCount() >= stats.max_champions())
            return false;
        LandBuilding armory = Market.findArmory(building);
        if (armory == null)
            return false;
        for (int i = 0; i < COST_CHAMPION.getSupplyTypes().length; i++) {
            if (armory.getSupplyContainer(
                    COST_CHAMPION.getSupplyTypes()[i]).getNumSupplies() < COST_CHAMPION.getSupplyAmounts()[i])
                return false;
        }
        return true;
    }

    /**
     * How many times faster a chieftain charges its spells: the Lodge's factor within its radius of a finished Lodge
     * of the chieftain's team, otherwise 1. Several Lodges do not add up; the numbers are the chieftain's race's.
     */
    public static float getSpellChargeFactor(@NonNull Selectable<?> chieftain) {
        Player owner = chieftain.getOwner();
        World world = owner.getWorld();
        List<LandBuilding> lodges = world.getLodges();
        if (lodges.isEmpty())
            return 1f;
        LodgeStats stats = world.getRuleset().getStats().race(
                owner.getPlayerInfo().getRace() == RacesResources.RACE_VIKINGS).lodge();
        float radius_squared = stats.spell_radius() * stats.spell_radius();
        int team = owner.getPlayerInfo().getTeam();
        for (LandBuilding lodge : lodges) {
            if (lodge.getOwner().getPlayerInfo().getTeam() != team)
                continue;
            float dx = lodge.getPositionX() - chieftain.getPositionX();
            float dy = lodge.getPositionY() - chieftain.getPositionY();
            if (dx * dx + dy * dy <= radius_squared)
                return stats.spell_charge_factor();
        }
        return 1f;
    }

    /** The Champions ordered at a Lodge, as the Armory's weapon orders are kept, for the Lodge's spinner. */
    private final class ChampionOrders extends BuildSupplyContainer {
        private boolean infinite = false;

        ChampionOrders() {
            super(BuildSpinner.INFINITE_LIMIT);
        }

        void orderSupply(int amount, boolean infinite) {
            this.infinite = infinite;
            if (infinite)
                orderSupply(BuildSpinner.INFINITE_LIMIT - getNumSupplies(), amount);
            else
                orderSupply(amount);
        }

        void trained() {
            if (!infinite)
                increaseSupply(-1);
        }

        @Override
        public float getBuildProgress() {
            return getProgress();
        }
    }

    /** The Lodge's training, for code that only has the building. */
    public static @Nullable Lodge of(@NonNull Building building) {
        return building instanceof LandBuilding land && !land.isDead() ? land.getLodge() : null;
    }
}
