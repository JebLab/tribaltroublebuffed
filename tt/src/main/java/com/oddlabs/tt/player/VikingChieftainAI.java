package com.oddlabs.tt.player;

import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.weapon.TargetedMagicFactory;
import com.oddlabs.tt.pathfinder.FindOccupantFilter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.stream.StreamSupport;

public final class VikingChieftainAI extends ChieftainAI {
    private static final int NUM_UNITS_FOR_STUN = 5;
    private static final int NUM_UNITS_FOR_BLAST = 7;
    // Buffed's third slot (docs/design/spells.md): Fjord Fog for a fight of this many enemy units and own units or
    // buildings within its radius, otherwise Hammer of Thor; a siege bolt at a building when no enemy unit is near.
    private static final int NUM_UNITS_FOR_THIRD_SLOT = 2;
    private static final int NUM_UNITS_FOR_FOG = 5;
    private static final int NUM_OWN_UNITS_FOR_FOG = 3;
    private static final float NO_UNITS_RADIUS_FOR_SIEGE = 30f;

    @Override
    public void decide(@NonNull Unit chieftain, boolean new_spells) {
        // Buffed: a full third slot (the longest charge, which any cast empties) opens the next fight, and the 2004
        // spells follow as they charge again.
        if (new_spells && chieftain.getMagicProgress(RacesResources.INDEX_MAGIC_FOG) >= 1) {
            nodeThirdSlot(chieftain);
            return;
        }
        nodeBlast(chieftain);
        nodeStun(chieftain);
    }

    private void nodeThirdSlot(@NonNull Unit chieftain) {
        float fog_radius = chieftain.getOwner().getRace().getMagicFactory(
                RacesResources.INDEX_MAGIC_FOG).getHitRadius();
        int enemies_close = countClose(chieftain, fog_radius, Unit.class, true);
        if (enemies_close >= NUM_UNITS_FOR_FOG
                && countClose(chieftain, fog_radius, Selectable.genericClass(), false) >= NUM_OWN_UNITS_FOR_FOG) {
            chieftain.doMagic(RacesResources.INDEX_MAGIC_FOG, false);
            return;
        }
        float range = ((TargetedMagicFactory) chieftain.getOwner().getRace().getMagicFactory(
                RacesResources.INDEX_MAGIC_HAMMER)).getRange();
        Selectable<?> target = null;
        if (enemies_close >= NUM_UNITS_FOR_THIRD_SLOT) {
            target = findHammerTarget(chieftain, range, Unit.class);
        } else if (countClose(chieftain, NO_UNITS_RADIUS_FOR_SIEGE, Unit.class, true) == 0) {
            target = findHammerTarget(chieftain, range, Building.class);
        }
        if (target != null)
            chieftain.doMagicAt(RacesResources.INDEX_MAGIC_HAMMER, target);
    }

    /**
     * The enemy the Hammer is worth most against within its range: a chieftain, then a Champion or a drummer, then
     * any warrior, then a peon; among buildings a tower (Great Towers too), then any. The nearest of the best kind.
     */
    private static @Nullable Selectable<?> findHammerTarget(@NonNull Unit chieftain, float range,
            @NonNull Class<? extends Selectable<?>> type) {
        var filter = new FindOccupantFilter<>(chieftain.getPositionX(), chieftain.getPositionY(), range, chieftain,
                type);
        chieftain.getUnitGrid().scan(filter, chieftain.getGridX(), chieftain.getGridY());
        Selectable<?> best = null;
        int best_rank = -1;
        float best_dist = Float.MAX_VALUE;
        for (Selectable<?> s : filter.getResult()) {
            if (s.isDead() || !chieftain.getOwner().isEnemy(s.getOwner()))
                continue;
            int rank = hammerRank(s);
            float dx = s.getPositionX() - chieftain.getPositionX();
            float dy = s.getPositionY() - chieftain.getPositionY();
            float dist = dx * dx + dy * dy;
            if (rank > best_rank || (rank == best_rank && dist < best_dist)) {
                best = s;
                best_rank = rank;
                best_dist = dist;
            }
        }
        return best;
    }

    private static int hammerRank(@NonNull Selectable<?> s) {
        if (s instanceof Unit unit) {
            if (unit.isChieftain())
                return 4;
            if (unit.isChampion() || unit.isDrummer())
                return 3;
            return unit.isWarrior() ? 2 : 1;
        }
        return s.getAbilities().hasAbilities(Abilities.ATTACK) ? 1 : 0;
    }

    private void nodeStun(@NonNull Unit chieftain) {
        if (chieftain.getMagicProgress(RacesResources.INDEX_MAGIC_STUN) < 1)
            return;

        float hit_radius = 30f;
        int num_enemy_units = numEnemyUnits(chieftain.getOwner());
        int num_enemy_units_close = getNumEnemyUnitsClose(chieftain, hit_radius, Unit.class);
        if (num_enemy_units_close >= NUM_UNITS_FOR_STUN
                || (num_enemy_units < NUM_UNITS_FOR_STUN && num_enemy_units_close > 1)
                || (chieftain.getHitPoints() <= 2 && num_enemy_units_close > 1)) {
            chieftain.doMagic(RacesResources.INDEX_MAGIC_STUN, false);
        }
    }

    private void nodeBlast(@NonNull Unit chieftain) {
        if (chieftain.getMagicProgress(RacesResources.INDEX_MAGIC_BLAST) < 1)
            return;

        float hit_radius = chieftain.getOwner().getRace().getMagicFactory(1).getHitRadius();
        int num_enemy_units = numEnemyUnits(chieftain.getOwner());

        int num_enemy_units_close = getNumEnemyUnitsClose(chieftain, hit_radius, Selectable.genericClass());
        int num_friendly_units_close = getNumFriendlyUnitsClose(chieftain, hit_radius);
        if (2 * num_friendly_units_close < num_enemy_units_close
                && (num_enemy_units_close >= NUM_UNITS_FOR_BLAST
                        || (num_enemy_units < NUM_UNITS_FOR_BLAST && num_enemy_units_close > 1)
                        || (chieftain.getHitPoints() <= 2 && num_enemy_units_close > 1))) {
            chieftain.doMagic(RacesResources.INDEX_MAGIC_BLAST, false);
        }
    }

    private <S extends Selectable<?>> int getNumEnemyUnitsClose(@NonNull Unit chieftain, float hit_radius,
            Class<S> type) {
        FindOccupantFilter<S> filter = new FindOccupantFilter<>(chieftain.getPositionX(), chieftain.getPositionY(),
                hit_radius, chieftain, type);
        chieftain.getUnitGrid().scan(filter, chieftain.getGridX(), chieftain.getGridY());
        long num_enemy_units_close = StreamSupport.stream(filter.getResult().spliterator(), false).filter(
                Selectable::isAlive).filter(s -> {
                    float dx = s.getPositionX() - chieftain.getPositionX();
                    float dy = s.getPositionY() - chieftain.getPositionY();
                    float squared_dist = dx * dx + dy * dy;
                    return chieftain.getOwner().isEnemy(s.getOwner()) && squared_dist < hit_radius * hit_radius;
                }).count();
        return (int) num_enemy_units_close;
    }

    private int getNumFriendlyUnitsClose(@NonNull Unit chieftain, float hit_radius) {
        var filter = new FindOccupantFilter<>(chieftain.getPositionX(), chieftain.getPositionY(), hit_radius, chieftain,
                Selectable.genericClass());
        chieftain.getUnitGrid().scan(filter, chieftain.getGridX(), chieftain.getGridY());
        long num_friendly_units_close = StreamSupport.stream(filter.getResult().spliterator(), false).filter(
                Selectable::isAlive).filter(s -> {
                    float dx = s.getPositionX() - chieftain.getPositionX();
                    float dy = s.getPositionY() - chieftain.getPositionY();
                    float squared_dist = dx * dx + dy * dy;
                    return !chieftain.getOwner().isEnemy(s.getOwner()) && squared_dist < hit_radius * hit_radius;
                }).count();
        return (int) num_friendly_units_close;
    }
}
