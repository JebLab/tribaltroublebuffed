package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.HeightMap;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.ruleset.RulesetStats.FaunaStats;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Places Buffed's wild animals when a world is made (docs/design/fauna.md): crabs on the shore, monkeys at forest
 * edges (tropical islands only), and boars (tropical) or wolves (northern) in deep forest, none near a player's start.
 * It runs after the island generator and the players' starts, only when the ruleset's {@code features.fauna} is on, and
 * draws from the world's random numbers, so worlds without fauna draw exactly the numbers they always did.
 */
public final class Fauna {
    // Cells kept between two animals of a kind, so that they spread over the island ("one in each patch" of forest).
    private static final int CRAB_SPACING = 4;
    private static final int MONKEY_SPACING = 8;
    private static final int PREDATOR_SPACING = 12;
    private static final int TRIES_PER_ANIMAL = 30;
    // Trees never stand side by side, so depth is the trees within this many cells (a square 18 m across).
    private static final int FOREST_RADIUS = 4;
    // At least this many trees around for deep forest, the island's densest woods; at most this many for an edge.
    private static final int DEEP_FOREST_TREES = 6;
    private static final int MIN_DEEP_FOREST_TREES = 3;
    private static final int FOREST_EDGE_TREES = 4;
    // Thinner woods count as deep while there are fewer candidate cells than this per boar or wolf.
    private static final int CELLS_PER_PREDATOR = 10;

    private Fauna() {
    }

    /** The fauna's numbers in this world: those of the race whose terrain it has. */
    public static @NonNull FaunaStats getStats(@NonNull World world) {
        return world.getRuleset().getStats().race(world.getTerrain() == Landscape.TerrainType.VIKING).fauna();
    }

    /** Places the animals, if the world's ruleset has them. */
    public static void populate(@NonNull World world, float @NonNull [] @NonNull [] starting_locations) {
        if (!world.getRuleset().getStats().features().fauna())
            return;
        FaunaStats stats = getStats(world);
        UnitGrid grid = world.getUnitGrid();
        int meters_per_world = world.getHeightMap().getMetersPerWorld();
        List<int[]> placed = new ArrayList<>();

        List<int[]> shore = shoreCells(grid);
        int shore_meters = shore.size() * HeightMap.METERS_PER_UNIT_GRID;
        int crabs = Math.min(stats.crab().max(), Math.round(shore_meters / 100f * stats.crab().per_100m_shore()));
        for (int[] cell : pick(world, shore, crabs, CRAB_SPACING, starting_locations, stats, placed))
            new Crab(world, cell[0], cell[1]);

        List<int[]> forest = forestCells(grid);
        if (world.getTerrain() == Landscape.TerrainType.NATIVE) {
            int monkeys = stats.monkey().count().forMetersPerWorld(meters_per_world);
            List<int[]> edge = forest.stream().filter(cell -> cell[3] > 0 && cell[2] <= FOREST_EDGE_TREES).toList();
            for (int[] cell : pick(world, edge, monkeys, MONKEY_SPACING, starting_locations, stats, placed))
                new Monkey(world, cell[0], cell[1]);
        }

        Animal.Species species = world.getTerrain() == Landscape.TerrainType.VIKING ? Animal.Species.WOLF : Animal.Species.BOAR;
        int predators = stats.predator().count().forMetersPerWorld(meters_per_world);
        List<int[]> deep = List.of();
        for (int trees = DEEP_FOREST_TREES; trees >= MIN_DEEP_FOREST_TREES
                && deep.size() < predators * CELLS_PER_PREDATOR; trees--) {
            int min_trees = trees;
            deep = forest.stream().filter(cell -> cell[2] >= min_trees).toList();
        }
        for (int[] cell : pick(world, deep, predators, PREDATOR_SPACING, starting_locations, stats, placed))
            new Predator(world, species, cell[0], cell[1]);
    }

    /** Free land cells next to the sea or a lake. */
    private static @NonNull List<int[]> shoreCells(@NonNull UnitGrid grid) {
        int size = grid.getGridSize();
        List<int[]> cells = new ArrayList<>();
        for (int y = 1; y < size - 1; y++) {
            for (int x = 1; x < size - 1; x++) {
                if (isFree(grid, x, y) && countAround(grid, x, y, 1, true) > 0)
                    cells.add(new int[]{x, y});
            }
        }
        return cells;
    }

    /**
     * Free land cells within two cells of a tree, each as {x, y, trees within {@link #FOREST_RADIUS} cells, trees on
     * the eight cells around it}, in row order.
     */
    private static @NonNull List<int[]> forestCells(@NonNull UnitGrid grid) {
        int size = grid.getGridSize();
        boolean[][] near_tree = new boolean[size][size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (grid.getOccupant(x, y) instanceof TreeSupply) {
                    for (int ny = Math.max(0, y - 2); ny <= Math.min(size - 1, y + 2); ny++) {
                        for (int nx = Math.max(0, x - 2); nx <= Math.min(size - 1, x + 2); nx++)
                            near_tree[ny][nx] = true;
                    }
                }
            }
        }
        List<int[]> cells = new ArrayList<>();
        for (int y = FOREST_RADIUS; y < size - FOREST_RADIUS; y++) {
            for (int x = FOREST_RADIUS; x < size - FOREST_RADIUS; x++) {
                if (near_tree[y][x] && isFree(grid, x, y))
                    cells.add(new int[]{x, y, countAround(grid, x, y, FOREST_RADIUS, false), countAround(grid, x, y,
                            1, false)});
            }
        }
        return cells;
    }

    /** Water cells, or trees, within this many cells of the cell (the cell itself not counted). */
    private static int countAround(@NonNull UnitGrid grid, int x, int y, int radius, boolean water) {
        int count = 0;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if ((dx != 0 || dy != 0) && (water ? grid.isWater(x + dx, y + dy) : grid.getOccupant(x + dx,
                        y + dy) instanceof TreeSupply))
                    count++;
            }
        }
        return count;
    }

    /** A cell an animal may stand on and walk from: empty land that the path finder knows. */
    private static boolean isFree(@NonNull UnitGrid grid, int x, int y) {
        int size = grid.getGridSize();
        return x >= 0 && y >= 0 && x < size && y < size && grid.getOccupant(x, y) == null && !grid.isWater(x, y)
                && grid.getRegion(x, y) != null;
    }

    /**
     * Picks up to {@code count} free cells from the candidates at random, away from the players' starts, at least
     * {@code spacing} cells from each other and two from any other animal.
     */
    private static @NonNull List<int[]> pick(@NonNull World world, @NonNull List<int[]> candidates, int count,
            int spacing, float @NonNull [] @NonNull [] starting_locations, @NonNull FaunaStats stats,
            @NonNull List<int[]> placed) {
        List<int[]> picked = new ArrayList<>();
        if (candidates.isEmpty())
            return picked;
        Random random = world.getRandom();
        UnitGrid grid = world.getUnitGrid();
        float clearance_squared = stats.start_clearance() * stats.start_clearance();
        for (int i = 0; i < count; i++) {
            for (int tries = 0; tries < TRIES_PER_ANIMAL; tries++) {
                int[] cell = candidates.get(random.nextInt(candidates.size()));
                if (!isFree(grid, cell[0], cell[1]) || isNearStart(cell, starting_locations, clearance_squared)
                        || isNear(cell, picked, spacing) || isNear(cell, placed, 2))
                    continue;
                picked.add(cell);
                placed.add(cell);
                break;
            }
        }
        return picked;
    }

    private static boolean isNearStart(int @NonNull [] cell, float @NonNull [] @NonNull [] starting_locations,
            float clearance_squared) {
        float x = UnitGrid.coordinateFromGrid(cell[0]);
        float y = UnitGrid.coordinateFromGrid(cell[1]);
        for (float[] start : starting_locations) {
            float dx = x - start[0];
            float dy = y - start[1];
            if (dx * dx + dy * dy < clearance_squared)
                return true;
        }
        return false;
    }

    private static boolean isNear(int @NonNull [] cell, @NonNull List<int[]> others, int cells) {
        for (int[] other : others) {
            int dx = cell[0] - other[0];
            int dy = cell[1] - other[1];
            if (dx * dx + dy * dy < cells * cells)
                return true;
        }
        return false;
    }
}
