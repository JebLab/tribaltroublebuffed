package com.oddlabs.tt.headless;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.headless.HeadlessMatchConfig.PlayerConfig;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Action;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.BuildingTemplate;
import com.oddlabs.tt.model.IronSupply;
import com.oddlabs.tt.model.LandBuilding;
import com.oddlabs.tt.model.Market;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.RockSupply;
import com.oddlabs.tt.model.RubberSupply;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.WallLine;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.ruleset.Ruleset;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Buffed's Market, Palisade and Gate (M7, docs/design/market.md and palisade.md): only Buffed offers them, the Market
 * trades 3 for 1 at its rate with the nearest Armory, walls are laid as lines and block, a gate lets its own side
 * through and stops everyone else, torches burn them, and the AI builds them. Played on headless worlds without AIs
 * unless a test says otherwise.
 */
final class BuffedMarketAndWallsTest {
    private static final int TICKS_PER_SECOND = Math.round(1 / AnimationManager.ANIMATION_SECONDS_PER_TICK);
    // The walls the movement tests build: a square ring of cells this far from its centre.
    private static final int RING = 3;

    private static @NonNull World newWorld(@NonNull Ruleset ruleset) {
        return newWorld(ruleset, List.of(new PlayerConfig(0, RacesResources.RACE_NATIVES, PlayerSlot.AI_HARD),
                new PlayerConfig(1, RacesResources.RACE_VIKINGS, PlayerSlot.AI_HARD)));
    }

    private static @NonNull World newWorld(@NonNull Ruleset ruleset, @NonNull List<PlayerConfig> players) {
        HeadlessMatchRunner.setUp();
        HeadlessMatchConfig config = new HeadlessMatchConfig(ruleset, Landscape.TerrainType.NATIVE, Game.SIZE_SMALL,
                .5f, .5f, .5f, 7, players, HeadlessMatchConfig.DEFAULT_MAX_TICKS);
        return HeadlessMatchRunner.newWorld(config, false);
    }

    private static void tick(@NonNull World world, float seconds) {
        for (int i = 0; i < seconds * TICKS_PER_SECOND; i++)
            world.tick(AnimationManager.ANIMATION_SECONDS_PER_TICK);
    }

    /** Ticks until the condition holds, at most {@code seconds}; returns whether it held. */
    private static boolean tickUntil(@NonNull World world, float seconds, @NonNull BooleanSupplier condition) {
        for (int i = 0; i < seconds * TICKS_PER_SECOND; i++) {
            if (condition.getAsBoolean())
                return true;
            world.tick(AnimationManager.ANIMATION_SECONDS_PER_TICK);
        }
        return condition.getAsBoolean();
    }

    /** A finished building near the player's start, as campaign scripts place them. */
    private static @NonNull LandBuilding build(@NonNull Player player, int building) {
        Building b = player.buildBuilding(building, UnitGrid.toGridCoordinate(player.getStartX()),
                UnitGrid.toGridCoordinate(player.getStartY()));
        assertNotNull(b, "no room for building " + building);
        assertTrue(b.isComplete());
        return (LandBuilding) b;
    }

    /** A finished Palisade segment or Gate in a given cell. */
    private static @NonNull LandBuilding wall(@NonNull Player player, int building, int grid_x, int grid_y) {
        Building b = player.getRace().getBuildingTemplate(building).create(player, grid_x, grid_y);
        b.place();
        b.repair(1000);
        assertTrue(b.isComplete());
        return (LandBuilding) b;
    }

    private static @NonNull Unit unit(@NonNull Player player, int grid_x, int grid_y, int template) {
        return new Unit(player, UnitGrid.coordinateFromGrid(grid_x), UnitGrid.coordinateFromGrid(grid_y), null,
                player.getRace().getUnitTemplate(template));
    }

    private static int stock(@NonNull Building building, @NonNull Class<?> resource) {
        return building.getSupplyContainer(resource).getNumSupplies();
    }

    /** The cells of the square ring around a centre. */
    private static @NonNull List<WallLine.Cell> ring(WallLine.@NonNull Cell center) {
        List<WallLine.Cell> cells = new ArrayList<>();
        for (int dy = -RING; dy <= RING; dy++) {
            for (int dx = -RING; dx <= RING; dx++) {
                if (Math.max(Math.abs(dx), Math.abs(dy)) == RING)
                    cells.add(new WallLine.Cell(center.x() + dx, center.y() + dy));
            }
        }
        return cells;
    }

    /**
     * A centre near the player's start where a ring of walls fits, with nothing inside it and a free cell well
     * outside its east side (where the units are sent).
     */
    private static WallLine.@NonNull Cell ringSite(@NonNull World world, @NonNull Player player) {
        UnitGrid grid = world.getUnitGrid();
        BuildingTemplate palisade = player.getRace().getBuildingTemplate(Race.BUILDING_PALISADE);
        int start_x = UnitGrid.toGridCoordinate(player.getStartX());
        int start_y = UnitGrid.toGridCoordinate(player.getStartY());
        for (int r = 0; r < 30; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r)
                        continue;
                    WallLine.Cell center = new WallLine.Cell(start_x + dx, start_y + dy);
                    if (ring(center).stream().allMatch(c -> palisade.isPlacingLegal(grid, c.x(), c.y()))
                            && insideIsFree(grid, center)
                            && grid.getOccupant(center.x() + 2 * RING, center.y()) == null)
                        return center;
                }
            }
        }
        return fail("no room for a ring of walls near " + start_x + "," + start_y);
    }

    private static boolean insideIsFree(@NonNull UnitGrid grid, WallLine.@NonNull Cell center) {
        for (int dy = 1 - RING; dy < RING; dy++) {
            for (int dx = 1 - RING; dx < RING; dx++) {
                if (grid.getOccupant(center.x() + dx, center.y() + dy) != null)
                    return false;
            }
        }
        return true;
    }

    /**
     * Walls the ring with Palisade segments of {@code owner}, and puts a Gate of {@code gate_owner} on its east side.
     */
    private static void buildRing(WallLine.@NonNull Cell center, @NonNull Player owner, @Nullable Player gate_owner) {
        for (WallLine.Cell cell : ring(center)) {
            if (gate_owner != null && cell.x() == center.x() + RING && cell.y() == center.y())
                wall(gate_owner, Race.BUILDING_GATE, cell.x(), cell.y());
            else
                wall(owner, Race.BUILDING_PALISADE, cell.x(), cell.y());
        }
    }

    private static boolean inside(@NonNull Unit unit, WallLine.@NonNull Cell center) {
        return Math.abs(unit.getGridX() - center.x()) < RING && Math.abs(unit.getGridY() - center.y()) < RING;
    }

    /** Sends a unit from the ring's centre to a cell well outside its east side. */
    private static void sendEast(@NonNull Player player, @NonNull Unit unit, WallLine.@NonNull Cell center) {
        player.setLandscapeTarget(new Selectable<?>[]{unit}, center.x() + 2 * RING, center.y(), Action.MOVE, false);
    }

    @ParameterizedTest
    @ValueSource(strings = {"classic", "resurrected"})
    void onlyBuffedOffersThem(String id) {
        World world = newWorld(Ruleset.fromId(id));
        Player player = world.getPlayers()[0];
        assertFalse(player.canBuild(Race.BUILDING_MARKET));
        assertFalse(player.canBuild(Race.BUILDING_PALISADE));
        assertFalse(player.canBuild(Race.BUILDING_GATE));
        int x = UnitGrid.toGridCoordinate(player.getStartX());
        int y = UnitGrid.toGridCoordinate(player.getStartY());
        player.placePalisade(new Selectable<?>[0], Race.BUILDING_PALISADE, x - 3, y, x + 3, y);
        assertTrue(player.getUnits().getSet().isEmpty(), "a wall was laid: " + player.getUnits().getSet());

        Player buffed = newWorld(Ruleset.BUFFED).getPlayers()[0];
        assertTrue(buffed.canBuild(Race.BUILDING_MARKET));
        assertTrue(buffed.canBuild(Race.BUILDING_PALISADE));
        assertTrue(buffed.canBuild(Race.BUILDING_GATE));
    }

    /** 3 of one resource for 1 of another every 20 man-seconds, with the Armory: one peon every 20 s, two every 10. */
    @Test
    void theMarketTradesThreeForOne() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        LandBuilding armory = build(player, Race.BUILDING_ARMORY);
        armory.fillSupplies(TreeSupply.class, 20);
        LandBuilding building = build(player, Race.BUILDING_MARKET);
        Market market = building.getMarket();
        assertNotNull(market);
        assertEquals(Market.WOOD, market.getGive(), "a new Market gives wood");
        assertEquals(Market.IRON, market.getGet(), "a new Market gets iron");
        tick(world, 30);
        assertEquals(0, player.getTradesMade(), "a Market without peons traded");

        building.getUnitContainer().increaseSupply(1);
        tick(world, 19);
        assertEquals(0, stock(armory, IronSupply.class), "a trade in less than 20 s");
        tick(world, 2);
        assertEquals(1, stock(armory, IronSupply.class));
        assertEquals(17, stock(armory, TreeSupply.class));
        assertEquals(1, player.getTradesMade());

        building.getUnitContainer().increaseSupply(1);
        tick(world, 20);
        assertEquals(3, stock(armory, IronSupply.class), "two peons trade every 10 s");
        assertEquals(11, stock(armory, TreeSupply.class));
    }

    /** Without goods to give, its work waits instead of piling up. */
    @Test
    void theMarketWaitsForGoods() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        LandBuilding building = build(player, Race.BUILDING_MARKET);
        building.getUnitContainer().increaseSupply(1);
        tick(world, 30);
        assertEquals(0, player.getTradesMade(), "a Market traded without an Armory");

        LandBuilding armory = build(player, Race.BUILDING_ARMORY);
        armory.fillSupplies(TreeSupply.class, 2);
        tick(world, 30);
        assertEquals(0, player.getTradesMade(), "a Market traded 2 for 1");
        armory.fillSupplies(TreeSupply.class, 1);
        tick(world, 19);
        assertEquals(0, player.getTradesMade(), "the work piled up while it waited");
        tick(world, 2);
        assertEquals(1, player.getTradesMade());
        assertEquals(0, stock(armory, TreeSupply.class));
        assertEquals(1, stock(armory, IronSupply.class));
    }

    /** The player picks both sides of the trade; the two must differ. */
    @Test
    void thePlayerPicksTheTrade() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        LandBuilding armory = build(player, Race.BUILDING_ARMORY);
        armory.fillSupplies(RockSupply.class, 10);
        LandBuilding building = build(player, Race.BUILDING_MARKET);
        Market market = building.getMarket();
        assertNotNull(market);
        player.setTrade(building, Market.ROCK, Market.ROCK);
        assertEquals(Market.WOOD, market.getGive(), "a Market took rock for rock");
        player.setTrade(building, Market.ROCK, Market.CHICKEN);
        assertEquals(Market.ROCK, market.getGive());
        assertEquals(Market.CHICKEN, market.getGet());
        building.getUnitContainer().increaseSupply(1);
        tick(world, 21);
        assertEquals(7, stock(armory, RockSupply.class));
        assertEquals(1, stock(armory, RubberSupply.class));
    }

    /** Peons walk in on a click; warriors keep their weapons outside. */
    @Test
    void onlyPeonsEnterTheMarket() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        LandBuilding building = build(player, Race.BUILDING_MARKET);
        Unit peon = unit(player, building.getGridX(), building.getGridY(), Race.UNIT_PEON);
        Unit warrior = unit(player, building.getGridX(), building.getGridY(), Race.UNIT_WARRIOR_ROCK);
        assertFalse(building.getUnitContainer().canEnter(warrior), "a warrior entered a Market");
        player.setTarget(new Selectable<?>[]{peon}, building, Action.DEFAULT, false);
        assertTrue(tickUntil(world, 30, peon::isDead), "the peon did not go in");
        assertEquals(1, building.getUnitCount());
    }

    /** A staircase whose cells touch side by side, from one end to the other, at most 20 cells. */
    @Test
    void aWallLineIsAStaircase() {
        List<WallLine.Cell> cells = WallLine.cells(0, 0, 3, 2, WallLine.MAX_SEGMENTS);
        assertEquals(new WallLine.Cell(0, 0), cells.getFirst());
        assertEquals(new WallLine.Cell(3, 2), cells.getLast());
        assertEquals(6, cells.size());
        for (int i = 1; i < cells.size(); i++) {
            int dx = Math.abs(cells.get(i).x() - cells.get(i - 1).x());
            int dy = Math.abs(cells.get(i).y() - cells.get(i - 1).y());
            assertEquals(1, dx + dy, "cells " + (i - 1) + " and " + i + " do not touch side by side: " + cells);
        }
        assertEquals(WallLine.MAX_SEGMENTS, WallLine.cells(0, 0, 40, 0, WallLine.MAX_SEGMENTS).size());
        assertEquals(List.of(new WallLine.Cell(5, 5)), WallLine.cells(5, 5, 5, 5, WallLine.MAX_SEGMENTS));
    }

    /**
     * A drag lays every segment at once; they count against the wall limit, not the building limit; a few peons
     * build the whole line, moving on to the next segment when theirs is done.
     */
    @Test
    void peonsBuildALineOfPalisade() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        WallLine.Cell center = ringSite(world, player);
        int y = center.y() - RING;
        List<Unit> peons = new ArrayList<>();
        for (int i = 0; i < 4; i++)
            peons.add(unit(player, center.x(), center.y(), Race.UNIT_PEON));
        player.placePalisade(peons.toArray(Selectable[]::new), Race.BUILDING_PALISADE, center.x() - 2, y,
                center.x() + 2, y);
        List<Building> walls = new ArrayList<>();
        for (Selectable<?> s : player.getUnits().getSet()) {
            if (s instanceof Building b && b.isWall())
                walls.add(b);
        }
        assertEquals(5, walls.size(), "the line was not laid at once");
        assertTrue(walls.stream().allMatch(b -> b.isPlaced() && !b.isComplete()));
        assertEquals(0, player.getBuildingCountContainer().getNumSupplies(), "walls count as buildings");
        assertEquals(5, player.getBuildingCountContainer(Race.BUILDING_PALISADE).getNumSupplies());
        assertTrue(tickUntil(world, 600, () -> walls.stream().allMatch(Building::isComplete)),
                "four peons did not finish five segments in 10 minutes");
    }

    /** A closed ring of Palisade keeps its builder's own peon in. */
    @Test
    void aPalisadeBlocks() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        WallLine.Cell center = ringSite(world, player);
        buildRing(center, player, null);
        Unit peon = unit(player, center.x(), center.y(), Race.UNIT_PEON);
        sendEast(player, peon, center);
        tick(world, 30);
        assertTrue(inside(peon, center), "the peon got through the palisade");
    }

    /** A gate lets its owner's units through and gives its cell back after them. */
    @Test
    void aGateAdmitsItsOwner() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        WallLine.Cell center = ringSite(world, player);
        buildRing(center, player, player);
        Unit peon = unit(player, center.x(), center.y(), Race.UNIT_PEON);
        Unit warrior = unit(player, center.x(), center.y(), Race.UNIT_WARRIOR_ROCK);
        sendEast(player, peon, center);
        sendEast(player, warrior, center);
        assertTrue(tickUntil(world, 30, () -> !inside(peon, center) && !inside(warrior, center)
                && peon.getGridX() > center.x() + RING && warrior.getGridX() > center.x() + RING),
                "the owner's units did not pass the gate");
        tick(world, 5);
        UnitGrid grid = world.getUnitGrid();
        assertSame(grid.getGate(center.x() + RING, center.y()), grid.getOccupant(center.x() + RING, center.y()),
                "the gate did not get its cell back");
    }

    /** A gate lets its owner's allies through. */
    @Test
    void aGateAdmitsAllies() {
        World world = newWorld(Ruleset.BUFFED, List.of(
                new PlayerConfig(0, RacesResources.RACE_NATIVES, PlayerSlot.AI_HARD),
                new PlayerConfig(0, RacesResources.RACE_VIKINGS, PlayerSlot.AI_HARD),
                new PlayerConfig(1, RacesResources.RACE_VIKINGS, PlayerSlot.AI_HARD)));
        Player owner = world.getPlayers()[0];
        Player ally = world.getPlayers()[1];
        WallLine.Cell center = ringSite(world, owner);
        buildRing(center, owner, owner);
        Unit peon = unit(ally, center.x(), center.y(), Race.UNIT_PEON);
        sendEast(ally, peon, center);
        assertTrue(tickUntil(world, 30, () -> peon.getGridX() > center.x() + RING),
                "the ally's peon did not pass the gate");
    }

    /** A gate stops enemies, as a palisade does. */
    @Test
    void aGateStopsEnemies() {
        World world = newWorld(Ruleset.BUFFED);
        Player owner = world.getPlayers()[0];
        Player enemy = world.getPlayers()[1];
        WallLine.Cell center = ringSite(world, owner);
        buildRing(center, owner, owner);
        Unit peon = unit(enemy, center.x(), center.y(), Race.UNIT_PEON);
        sendEast(enemy, peon, center);
        tick(world, 30);
        assertTrue(inside(peon, center), "an enemy peon got through the gate");
    }

    /** Torches burn walls like any building: a blow and its fire take 34 of a segment's 40, the next the rest. */
    @Test
    void torchesBurnPalisades() {
        World world = newWorld(Ruleset.BUFFED);
        Player natives = world.getPlayers()[0];
        Player vikings = world.getPlayers()[1];
        WallLine.Cell center = ringSite(world, vikings);
        LandBuilding palisade = wall(vikings, Race.BUILDING_PALISADE, center.x(), center.y() - RING);
        Unit torch = unit(natives, UnitGrid.toGridCoordinate(natives.getStartX()),
                UnitGrid.toGridCoordinate(natives.getStartY()), Race.UNIT_WARRIOR_TORCH);
        torch.getWeaponFactory().attack(torch, palisade);
        assertTrue(palisade.isBurning());
        tick(world, 16);
        assertEquals(6, palisade.getHitPoints());
        torch.getWeaponFactory().attack(torch, palisade);
        assertTrue(tickUntil(world, 2, palisade::isDead), "the second blow did not bring the segment down");
        assertEquals(1, natives.getBuildingsDestroyed());
    }

    /** A gate's cell is never free for another building, even with a unit of its side inside. */
    @Test
    void nothingIsBuiltOnAGate() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        WallLine.Cell center = ringSite(world, player);
        LandBuilding gate = wall(player, Race.BUILDING_GATE, center.x(), center.y());
        assertSame(gate, world.getUnitGrid().getGate(center.x(), center.y()));
        BuildingTemplate palisade = player.getRace().getBuildingTemplate(Race.BUILDING_PALISADE);
        assertFalse(palisade.isPlacingLegal(world.getUnitGrid(), center.x(), center.y()));
        gate.hit(1000, 0f, 1f, world.getPlayers()[1]);
        tick(world, 1);
        assertNull(world.getUnitGrid().getGate(center.x(), center.y()), "a fallen gate stayed a gate");
        assertNull(world.getUnitGrid().getOccupant(center.x(), center.y()));
    }

    /**
     * Normal and Hard AIs build a Market that trades and a palisade line with a gate under Buffed; Easy ones never do.
     */
    @Test
    void aiBuildsMarketsAndWallsUnderBuffed() {
        for (String name : List.of("buffed-1v1", "buffed-6p")) {
            HeadlessMatchConfig config = Matches.ALL.get(name);
            HeadlessMatchResult result = Matches.play(name);
            for (int i = 0; i < config.players().size(); i++) {
                HeadlessMatchResult.Census census = result.census().get(i);
                if (config.players().get(i).difficulty() == PlayerSlot.AI_EASY) {
                    String player = name + " player " + i + ": " + census;
                    assertFalse(census.completedBuildings().contains(Race.BUILDING_MARKET), player);
                    assertFalse(census.completedBuildings().contains(Race.BUILDING_PALISADE), player);
                    assertEquals(0, census.tradesMade(), player);
                }
            }
            long markets = result.census().stream().filter(
                    c -> c.completedBuildings().contains(Race.BUILDING_MARKET) && c.tradesMade() > 0).count();
            long walls = result.census().stream().filter(c -> c.completedBuildings().contains(Race.BUILDING_PALISADE)
                    && c.completedBuildings().contains(Race.BUILDING_GATE)).count();
            assertTrue(markets > 0 && walls > 0, name + ": " + result.census());
        }
    }

    /** Under Classic and Resurrected nobody builds them. */
    @ParameterizedTest
    @ValueSource(strings = {"classic-1v1", "classic-6p", "resurrected-1v1", "resurrected-6p"})
    void nobodyBuildsThemOutsideBuffed(String name) {
        for (HeadlessMatchResult.Census census : Matches.play(name).census()) {
            for (int building : new int[]{Race.BUILDING_MARKET, Race.BUILDING_PALISADE, Race.BUILDING_GATE})
                assertFalse(census.completedBuildings().contains(building), name + ": " + census);
            assertEquals(0, census.tradesMade(), name + ": " + census);
        }
    }
}
