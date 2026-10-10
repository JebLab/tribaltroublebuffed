package com.oddlabs.tt.headless;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.headless.HeadlessMatchConfig.PlayerConfig;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Action;
import com.oddlabs.tt.model.Animal;
import com.oddlabs.tt.model.AttackScanFilter;
import com.oddlabs.tt.model.Crab;
import com.oddlabs.tt.model.Monkey;
import com.oddlabs.tt.model.Predator;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.ruleset.Ruleset;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Buffed's wild animals (M9, docs/design/fauna.md): only Buffed places them, away from every start; they are no
 * player's units; a crab flees, a monkey takes a passing carrier's load, a boar strikes a lone peon and not one with
 * company, warriors kill animals only when no player's unit is in reach, and no kill is credited to anyone. Played on
 * headless worlds without AIs unless a test says otherwise.
 */
final class BuffedFaunaTest {
    private static final int TICKS_PER_SECOND = Math.round(1 / AnimationManager.ANIMATION_SECONDS_PER_TICK);

    private static @NonNull World newWorld(@NonNull Ruleset ruleset, Landscape.@NonNull TerrainType terrain,
            int seed) {
        HeadlessMatchRunner.setUp();
        HeadlessMatchConfig config = new HeadlessMatchConfig(ruleset, terrain, Game.SIZE_MEDIUM, .5f, .5f, .5f, seed,
                List.of(new PlayerConfig(0, RacesResources.RACE_NATIVES, PlayerSlot.AI_HARD),
                        new PlayerConfig(1, RacesResources.RACE_VIKINGS, PlayerSlot.AI_HARD)),
                HeadlessMatchConfig.DEFAULT_MAX_TICKS);
        return HeadlessMatchRunner.newWorld(config, false);
    }

    private static @NonNull World newWorld() {
        return newWorld(Ruleset.BUFFED, Landscape.TerrainType.NATIVE, 7);
    }

    private static boolean tickUntil(@NonNull World world, float seconds, @NonNull BooleanSupplier condition) {
        for (int i = 0; i < seconds * TICKS_PER_SECOND; i++) {
            if (condition.getAsBoolean())
                return true;
            world.tick(AnimationManager.ANIMATION_SECONDS_PER_TICK);
        }
        return condition.getAsBoolean();
    }

    /** The free cell nearest to this many cells east and north of the player's start. */
    private static @NonNull Target near(@NonNull Player player, int dx, int dy) {
        int x = UnitGrid.toGridCoordinate(player.getStartX()) + dx;
        int y = UnitGrid.toGridCoordinate(player.getStartY()) + dy;
        return player.getWorld().getUnitGrid().findGridTargets(x, y, 1, false)[0];
    }

    private static @NonNull Unit unit(@NonNull Player player, @NonNull Target cell, int template) {
        return new Unit(player, UnitGrid.coordinateFromGrid(cell.getGridX()), UnitGrid.coordinateFromGrid(
                cell.getGridY()), null, player.getRace().getUnitTemplate(template));
    }

    private static int distanceSquared(@NonNull Target a, @NonNull Target b) {
        int dx = a.getGridX() - b.getGridX();
        int dy = a.getGridY() - b.getGridY();
        return dx * dx + dy * dy;
    }

    private static long count(@NonNull World world, Animal.@NonNull Species species) {
        return world.getAnimals().stream().filter(a -> a.getSpecies() == species).count();
    }

    @ParameterizedTest
    @ValueSource(strings = {"classic", "resurrected"})
    void onlyBuffedHasFauna(String id) {
        assertFalse(Ruleset.fromId(id).getStats().features().fauna());
        for (Landscape.TerrainType terrain : Landscape.TerrainType.values())
            assertTrue(newWorld(Ruleset.fromId(id), terrain, 7).getAnimals().isEmpty(), id + " " + terrain);
    }

    /**
     * Tropical islands have crabs, monkeys and boars, northern ones crabs and wolves, none within 40 m of a start and
     * none counted as anyone's unit.
     */
    @ParameterizedTest
    @EnumSource(Landscape.TerrainType.class)
    void buffedPlacesAnimalsAwayFromStarts(Landscape.TerrainType terrain) {
        World world = newWorld(Ruleset.BUFFED, terrain, 7);
        boolean tropical = terrain == Landscape.TerrainType.NATIVE;
        assertTrue(count(world, Animal.Species.CRAB) > 0, "no crabs");
        assertEquals(tropical, count(world, Animal.Species.MONKEY) > 0, "monkeys");
        assertEquals(tropical, count(world, Animal.Species.BOAR) > 0, "boars");
        assertEquals(!tropical, count(world, Animal.Species.WOLF) > 0, "wolves");
        assertTrue(count(world, Animal.Species.MONKEY) <= 5 && count(world, Animal.Species.BOAR) <= 3
                && count(world, Animal.Species.WOLF) <= 3 && count(world, Animal.Species.CRAB) <= 20,
                "a Medium island has at most 5 monkeys, 3 boars or wolves and 20 crabs");
        for (Animal animal : world.getAnimals()) {
            for (Player player : world.getPlayers()) {
                float dx = animal.getPositionX() - player.getStartX();
                float dy = animal.getPositionY() - player.getStartY();
                assertTrue(dx * dx + dy * dy >= 40 * 40, animal.getSpecies() + " near a start");
                assertTrue(player.getUnits().getSet().isEmpty(), "an animal counts as a unit");
            }
        }
    }

    /** The same seed places the same animals on the same cells. */
    @Test
    void placementIsDeterministic() {
        List<Animal> first = newWorld().getAnimals();
        List<Animal> second = newWorld().getAnimals();
        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            assertSame(first.get(i).getSpecies(), second.get(i).getSpecies());
            assertEquals(first.get(i).getGridX(), second.get(i).getGridX());
            assertEquals(first.get(i).getGridY(), second.get(i).getGridY());
        }
    }

    @Test
    void aCrabFlees() {
        World world = newWorld();
        Player player = world.getPlayers()[0];
        Target cell = near(player, 6, 0);
        Crab crab = new Crab(world, cell.getGridX(), cell.getGridY());
        Unit peon = unit(player, near(player, 5, 0), Race.UNIT_PEON);
        assertTrue(distanceSquared(crab, peon) <= 2, "the peon is not next to the crab");
        assertTrue(tickUntil(world, 5, () -> distanceSquared(crab, peon) >= 9), "the crab stayed by the peon");
        assertFalse(crab.isPrey(), "crabs are not targets");
    }

    @Test
    void aMonkeyTakesAPassingCarriersLoad() {
        World world = newWorld();
        Player player = world.getPlayers()[0];
        Target cell = near(player, 6, 2);
        Monkey monkey = new Monkey(world, cell.getGridX(), cell.getGridY());
        Unit peon = unit(player, near(player, 0, 0), Race.UNIT_PEON);
        peon.getSupplyContainer().increaseSupply(1, TreeSupply.class);
        assertTrue(peon.isCarrying());
        player.setLandscapeTarget(new Selectable<?>[]{peon}, cell.getGridX() + 10, cell.getGridY(), Action.MOVE,
                false);
        assertTrue(tickUntil(world, 20, () -> player.getLoadsStolen() == 1), "the monkey took nothing");
        assertEquals(0, peon.getSupplyContainer().getNumSupplies());
        assertFalse(peon.isDead(), "a monkey never attacks");
        assertTrue(tickUntil(world, 10, () -> distanceSquared(monkey, cell) <= 2), "the monkey did not go home");
    }

    /** A peon with company is left alone; alone, it is struck, and its death is credited to no one. */
    @Test
    void aBoarStrikesALonePeonAndNotAPair() {
        World world = newWorld();
        Player player = world.getPlayers()[0];
        Target cell = near(player, 6, 0);
        Predator boar = new Predator(world, Animal.Species.BOAR, cell.getGridX(), cell.getGridY());
        Unit peon = unit(player, near(player, 3, 0), Race.UNIT_PEON);
        Unit friend = unit(player, near(player, 1, 0), Race.UNIT_PEON);
        assertFalse(tickUntil(world, 10, () -> boar.getStrikes() > 0), "the boar struck a peon with company");
        friend.removeNow();
        assertTrue(tickUntil(world, 10, () -> boar.getStrikes() > 0), "the boar left a lone peon alone");
        assertTrue(tickUntil(world, 200, peon::isDead), "five strikes in a row missed");
        assertEquals(1, player.getUnitsLostToAnimals());
        for (Player p : world.getPlayers())
            assertEquals(0, p.getUnitsKilled(), "a kill was credited");
    }

    /** A boar never attacks a warrior. */
    @Test
    void aBoarLeavesWarriorsAlone() {
        World world = newWorld();
        Player player = world.getPlayers()[0];
        Target cell = near(player, 6, 0);
        Predator boar = new Predator(world, Animal.Species.BOAR, cell.getGridX(), cell.getGridY());
        unit(player, near(player, 3, 0), Race.UNIT_WARRIOR_SHIELD);
        // A shield warrior hunts the boar; keep the boar alive long enough to see it never strikes.
        assertFalse(tickUntil(world, 3, () -> boar.getStrikes() > 0));
    }

    /** An idle warrior kills an animal in reach; the kill is no unit kill. */
    @Test
    void warriorsKillAnimals() {
        World world = newWorld();
        Player player = world.getPlayers()[0];
        Target cell = near(player, 8, 0);
        Predator boar = new Predator(world, Animal.Species.BOAR, cell.getGridX(), cell.getGridY());
        int animals = world.getAnimals().size();
        unit(player, near(player, 0, 0), Race.UNIT_WARRIOR_ROCK);
        assertTrue(tickUntil(world, 60, boar::isDead), "the warrior left the boar alive");
        assertEquals(1, player.getAnimalsKilled());
        assertEquals(0, player.getUnitsKilled());
        assertEquals(animals - 1, world.getAnimals().size());
        assertTrue(tickUntil(world, 5, () -> world.getUnitGrid().getOccupant(boar.getGridX(), boar.getGridY()) != boar),
                "the dead boar still takes its cell");
    }

    /** A player's unit in reach comes before any animal: animals are the lowest priority. */
    @Test
    void enemiesComeBeforeAnimals() {
        World world = newWorld();
        Player player = world.getPlayers()[0];
        Player enemy = world.getPlayers()[1];
        Target cell = near(player, 2, 0);
        Predator boar = new Predator(world, Animal.Species.BOAR, cell.getGridX(), cell.getGridY());
        Unit warrior = unit(player, near(player, 0, 0), Race.UNIT_WARRIOR_ROCK);
        AttackScanFilter filter = new AttackScanFilter(player, AttackScanFilter.UNIT_RANGE, true);
        warrior.scanVicinity(filter);
        assertSame(boar, filter.removeTarget());
        Unit peon = unit(enemy, near(player, 5, 0), Race.UNIT_PEON);
        warrior.scanVicinity(filter);
        assertSame(peon, filter.removeTarget());
        AttackScanFilter peon_filter = new AttackScanFilter(player, AttackScanFilter.UNIT_RANGE);
        peon.removeNow();
        warrior.scanVicinity(peon_filter);
        assertEquals(null, peon_filter.removeTarget(), "a filter that does not hunt took the boar");
        assertTrue(warrior.canAttack(boar, false));
    }

    /** In the AI matches the animals do something under Buffed and nothing anywhere else. */
    @Test
    void aiMatchesMeetTheAnimals() {
        int stolen = 0;
        int killed = 0;
        for (String name : List.of("buffed-1v1", "buffed-6p")) {
            for (HeadlessMatchResult.Census census : Matches.play(name).census()) {
                stolen += census.loadsStolen();
                killed += census.animalsKilled();
            }
        }
        assertTrue(stolen > 0, "monkeys took nothing");
        assertTrue(killed > 0, "no warrior killed an animal");
        for (String name : List.of("classic-1v1", "resurrected-1v1")) {
            for (HeadlessMatchResult.Census census : Matches.play(name).census()) {
                assertEquals(0, census.loadsStolen() + census.animalsKilled() + census.unitsLostToAnimals(),
                        name + ": " + census);
            }
        }
    }
}
