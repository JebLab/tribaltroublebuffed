package com.oddlabs.tt.headless;

import com.oddlabs.tt.model.ChickenCoop;
import com.oddlabs.tt.model.LandBuilding;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Notes what each player has built and fielded, for the tests to check that the AI uses its buildings and gear. It
 * only reads the world, so taking a census never changes a match.
 */
final class CensusTaker {
    private final List<Set<Integer>> completed_buildings = new ArrayList<>();
    private final List<Set<Integer>> unit_types = new ArrayList<>();
    private final boolean[] chicken_coop_bred;
    private final int[] fires_lit;
    private final int[] trades_made;
    private final int[] great_tower_throwers;
    private final int[] snares_laid;
    private final int[] snares_sprung;
    private final int[] units_lost_to_animals;
    private final int[] loads_stolen;
    private final int[] animals_killed;
    private final int[][] spells_cast;

    CensusTaker(int num_players) {
        for (int i = 0; i < num_players; i++) {
            completed_buildings.add(new TreeSet<>());
            unit_types.add(new TreeSet<>());
        }
        chicken_coop_bred = new boolean[num_players];
        fires_lit = new int[num_players];
        trades_made = new int[num_players];
        great_tower_throwers = new int[num_players];
        snares_laid = new int[num_players];
        snares_sprung = new int[num_players];
        units_lost_to_animals = new int[num_players];
        loads_stolen = new int[num_players];
        animals_killed = new int[num_players];
        spells_cast = new int[num_players][RacesResources.NUM_MAGIC];
    }

    void update(@NonNull Player @NonNull [] players) {
        for (int i = 0; i < players.length; i++) {
            for (Selectable<?> s : players[i].getUnits().getSet()) {
                if (s instanceof LandBuilding building && !building.isDead() && building.isComplete()) {
                    completed_buildings.get(i).add(building.getTemplate().getTemplateID());
                    ChickenCoop coop = building.getChickenCoop();
                    if (building.isGreatTower())
                        great_tower_throwers[i] = Math.max(great_tower_throwers[i],
                                building.getUnitContainer().getNumSupplies());
                    if (coop != null && coop.getNumChickens() > 0)
                        chicken_coop_bred[i] = true;
                } else if (s instanceof Unit unit && !unit.isDead()) {
                    for (int type = 0; type < Race.NUM_UNITS; type++) {
                        if (players[i].getRace().getUnitTemplate(type) == unit.getTemplate())
                            unit_types.get(i).add(type);
                    }
                }
            }
            fires_lit[i] = players[i].getFiresLit();
            trades_made[i] = players[i].getTradesMade();
            snares_laid[i] = players[i].getSnaresLaid();
            snares_sprung[i] = players[i].getSnaresSprung();
            units_lost_to_animals[i] = players[i].getUnitsLostToAnimals();
            loads_stolen[i] = players[i].getLoadsStolen();
            animals_killed[i] = players[i].getAnimalsKilled();
            for (int magic = 0; magic < RacesResources.NUM_MAGIC; magic++)
                spells_cast[i][magic] = players[i].getMagics(magic);
        }
    }

    @NonNull
    List<HeadlessMatchResult.@NonNull Census> result() {
        List<HeadlessMatchResult.Census> census = new ArrayList<>();
        for (int i = 0; i < chicken_coop_bred.length; i++)
            census.add(new HeadlessMatchResult.Census(completed_buildings.get(i), chicken_coop_bred[i],
                    unit_types.get(i), fires_lit[i], trades_made[i], great_tower_throwers[i], snares_laid[i],
                    snares_sprung[i], units_lost_to_animals[i], loads_stolen[i], animals_killed[i],
                    Arrays.stream(spells_cast[i]).boxed().toList()));
        return census;
    }
}
