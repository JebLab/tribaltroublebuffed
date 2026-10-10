package com.oddlabs.tt.headless;

import com.oddlabs.tt.model.ChickenCoop;
import com.oddlabs.tt.model.LandBuilding;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.player.Player;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Notes what each player has built, for the tests to check that the AI uses its buildings. It only reads the world,
 * so taking a census never changes a match.
 */
final class CensusTaker {
    private final List<Set<Integer>> completed_buildings = new ArrayList<>();
    private final boolean[] chicken_coop_bred;

    CensusTaker(int num_players) {
        for (int i = 0; i < num_players; i++)
            completed_buildings.add(new TreeSet<>());
        chicken_coop_bred = new boolean[num_players];
    }

    void update(@NonNull Player @NonNull [] players) {
        for (int i = 0; i < players.length; i++) {
            for (Selectable<?> s : players[i].getUnits().getSet()) {
                if (s instanceof LandBuilding building && !building.isDead() && building.isComplete()) {
                    completed_buildings.get(i).add(building.getTemplate().getTemplateID());
                    ChickenCoop coop = building.getChickenCoop();
                    if (coop != null && coop.getNumChickens() > 0)
                        chicken_coop_bred[i] = true;
                }
            }
        }
    }

    @NonNull
    List<HeadlessMatchResult.@NonNull Census> result() {
        List<HeadlessMatchResult.Census> census = new ArrayList<>();
        for (int i = 0; i < chicken_coop_bred.length; i++)
            census.add(new HeadlessMatchResult.Census(completed_buildings.get(i), chicken_coop_bred[i]));
        return census;
    }
}
