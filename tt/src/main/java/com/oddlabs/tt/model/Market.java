package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.particle.LinearEmitter;
import com.oddlabs.tt.ruleset.RulesetStats.MarketStats;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * What a finished Market does (Buffed): the peons inside trade {@code give} of one resource for {@code get} of another
 * with the owner's nearest finished Armory, one trade per {@code seconds} man-seconds, which the peons inside share as
 * in the Armory's weapon production. While it cannot trade (no Armory, too little to give there, or the store of the
 * resource asked for is full) the work waits (docs/design/market.md).
 */
public final class Market {
    // The resources it trades, by the index the player command and the panel use.
    public static final int WOOD = 0;
    public static final int ROCK = 1;
    public static final int IRON = 2;
    public static final int CHICKEN = 3;
    private static final List<Class<? extends Supply>> RESOURCES = List.of(TreeSupply.class, RockSupply.class,
            IronSupply.class, RubberSupply.class);

    private final @NonNull LandBuilding building;
    private final @NonNull MarketStats stats;
    private final @NonNull LinearEmitter emitter;

    private int give = WOOD;
    private int get = IRON;
    private float work = 0;

    Market(@NonNull LandBuilding building, @NonNull MarketStats stats, @NonNull LinearEmitter emitter) {
        this.building = building;
        this.stats = stats;
        this.emitter = emitter;
    }

    public static int numResources() {
        return RESOURCES.size();
    }

    public static @NonNull Class<? extends Supply> getResource(int index) {
        return RESOURCES.get(index);
    }

    /** Whether {@code give} for {@code get} is a trade a Market can make: two different resources. */
    public static boolean isValidTrade(int give, int get) {
        return give >= 0 && give < RESOURCES.size() && get >= 0 && get < RESOURCES.size() && give != get;
    }

    public int getGive() {
        return give;
    }

    public int getGet() {
        return get;
    }

    public @NonNull MarketStats getStats() {
        return stats;
    }

    /** The work done towards the next trade, from 0 to 1. */
    public float getProgress() {
        return work / stats.seconds();
    }

    void setTrade(int give, int get) {
        assert isValidTrade(give, get);
        this.give = give;
        this.get = get;
    }

    void animate(float t) {
        int workers = building.getUnitContainer().getNumSupplies();
        LandBuilding armory = workers > 0 ? findArmory() : null;
        if (armory == null || !canTrade(armory)) {
            emitter.stop();
            return;
        }
        emitter.start();
        work += workers * t;
        while (work >= stats.seconds() && canTrade(armory)) {
            work -= stats.seconds();
            armory.getSupplyContainer(RESOURCES.get(give)).increaseSupply(-stats.give());
            armory.getSupplyContainer(RESOURCES.get(get)).increaseSupply(stats.get());
            building.getOwner().tradeMade();
        }
        work = Math.min(work, stats.seconds());
    }

    private boolean canTrade(@NonNull LandBuilding armory) {
        SupplyContainer given = armory.getSupplyContainer(RESOURCES.get(give));
        SupplyContainer gotten = armory.getSupplyContainer(RESOURCES.get(get));
        return given.getNumSupplies() >= stats.give()
                && gotten.getNumSupplies() + stats.get() <= gotten.getMaxSupplyCount();
    }

    /** The owner's finished Armory nearest to the Market (the first of equally near ones), or null. */
    public @Nullable LandBuilding findArmory() {
        LandBuilding best = null;
        int best_dist_squared = Integer.MAX_VALUE;
        for (Selectable<?> s : building.getOwner().getUnits().getSet()) {
            if (s instanceof LandBuilding armory && !armory.isDead()
                    && armory.getAbilities().hasAbilities(Abilities.BUILD_ARMIES)) {
                int dx = armory.getGridX() - building.getGridX();
                int dy = armory.getGridY() - building.getGridY();
                int dist_squared = dx * dx + dy * dy;
                if (dist_squared < best_dist_squared) {
                    best_dist_squared = dist_squared;
                    best = armory;
                }
            }
        }
        return best;
    }
}
