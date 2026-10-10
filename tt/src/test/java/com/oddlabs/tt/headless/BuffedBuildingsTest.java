package com.oddlabs.tt.headless;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.headless.HeadlessMatchConfig.PlayerConfig;
import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.ChickenCoop;
import com.oddlabs.tt.model.LandBuilding;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.RubberGroup;
import com.oddlabs.tt.model.RubberSupply;
import com.oddlabs.tt.model.RubberSupplyManager;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.TotemAura;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.BuildingSiteScanFilter;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.ruleset.Ruleset;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Buffed's Chicken Coop and Totem (M5): only Buffed offers them, peons build them from wood and finish or stock them
 * with rock and chickens, the coop breeds, the totem raises friendly hit chance, and the AI builds and uses both.
 * Played on headless worlds without AIs unless a test says otherwise.
 */
final class BuffedBuildingsTest {
    private static final int TICKS_PER_SECOND = Math.round(1 / AnimationManager.ANIMATION_SECONDS_PER_TICK);

    private static @NonNull World newWorld(@NonNull Ruleset ruleset) {
        HeadlessMatchRunner.setUp();
        HeadlessMatchConfig config = new HeadlessMatchConfig(ruleset, Landscape.TerrainType.NATIVE, Game.SIZE_SMALL,
                .5f, .5f, .5f, 7, List.of(new PlayerConfig(0, RacesResources.RACE_NATIVES, PlayerSlot.AI_HARD),
                        new PlayerConfig(1, RacesResources.RACE_VIKINGS, PlayerSlot.AI_HARD)),
                HeadlessMatchConfig.DEFAULT_MAX_TICKS);
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

    private static int startX(@NonNull Player player) {
        return UnitGrid.toGridCoordinate(player.getStartX());
    }

    private static int startY(@NonNull Player player) {
        return UnitGrid.toGridCoordinate(player.getStartY());
    }

    /** A finished building near the player's start, as campaign scripts place them. */
    private static @NonNull LandBuilding build(@NonNull Player player, int building) {
        Building b = player.buildBuilding(building, startX(player), startY(player));
        assertNotNull(b, "no room for building " + building);
        assertTrue(b.isComplete());
        return (LandBuilding) b;
    }

    private static @NonNull Unit unit(@NonNull Player player, float x, float y, int template) {
        return new Unit(player, x, y, null, player.getRace().getUnitTemplate(template));
    }

    private static @Nullable LandBuilding find(@NonNull Player player, int building) {
        for (Selectable<?> s : player.getUnits().getSet()) {
            if (s instanceof LandBuilding b && !b.isDead() && b.getTemplate().getTemplateID() == building)
                return b;
        }
        return null;
    }

    @ParameterizedTest
    @ValueSource(strings = {"classic", "resurrected"})
    void onlyBuffedOffersThem(String id) {
        Player player = newWorld(Ruleset.fromId(id)).getPlayers()[0];
        assertFalse(player.canBuild(Race.BUILDING_CHICKEN_COOP));
        assertFalse(player.canBuild(Race.BUILDING_TOTEM));
        Player buffed = newWorld(Ruleset.BUFFED).getPlayers()[0];
        assertTrue(buffed.canBuild(Race.BUILDING_CHICKEN_COOP));
        assertTrue(buffed.canBuild(Race.BUILDING_TOTEM));
    }

    @Test
    void totemsRaiseFriendlyHitChanceUpToTwo() {
        World world = newWorld(Ruleset.BUFFED);
        Player natives = world.getPlayers()[0];
        Player vikings = world.getPlayers()[1];
        LandBuilding first = build(natives, Race.BUILDING_TOTEM);
        Unit friend = unit(natives, first.getPositionX(), first.getPositionY(), Race.UNIT_WARRIOR_ROCK);
        Unit enemy = unit(vikings, first.getPositionX(), first.getPositionY(), Race.UNIT_WARRIOR_ROCK);
        assertEquals(.05f, TotemAura.getHitBonus(friend));
        assertEquals(0f, TotemAura.getHitBonus(enemy), "an enemy totem helps nobody");

        LandBuilding second = build(natives, Race.BUILDING_TOTEM);
        LandBuilding third = build(natives, Race.BUILDING_TOTEM);
        for (LandBuilding totem : List.of(second, third)) {
            float dx = totem.getPositionX() - friend.getPositionX();
            float dy = totem.getPositionY() - friend.getPositionY();
            assertTrue(dx * dx + dy * dy <= 100f, "test setup: every totem within 10 m of the unit");
        }
        assertEquals(.1f, TotemAura.getHitBonus(friend), "at most two totems add up");

        Unit far = unit(natives, first.getPositionX() + 30f, first.getPositionY(), Race.UNIT_WARRIOR_ROCK);
        float dx = far.getPositionX() - first.getPositionX();
        float dy = far.getPositionY() - first.getPositionY();
        assertTrue(dx * dx + dy * dy > 10f * 10f, "test setup: a unit out of the totems' reach");
        assertEquals(0f, TotemAura.getHitBonus(far));

        natives.killSelection(new Selectable<?>[]{second, third});
        assertEquals(.05f, TotemAura.getHitBonus(friend), "a destroyed totem stops helping");
        assertEquals(1, world.getTotems().size());
    }

    @Test
    void chickenCoopBreedsOnceStocked() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        LandBuilding coop = build(player, Race.BUILDING_CHICKEN_COOP);
        ChickenCoop breeding = coop.getChickenCoop();
        assertNotNull(breeding);
        assertTrue(breeding.needsStock());
        assertTrue(coop.hasWork(), "an empty coop wants peons");
        assertEquals(RubberSupply.class, coop.getWorkMaterial());

        tick(world, 200);
        assertEquals(0, breeding.getNumChickens(), "an empty coop does not breed");

        coop.deliverMaterial(RubberSupply.class);
        assertTrue(breeding.needsStock());
        coop.deliverMaterial(RubberSupply.class);
        assertFalse(breeding.needsStock());
        assertFalse(coop.hasWork());

        tick(world, 89);
        assertEquals(0, breeding.getNumChickens(), "the first chicken after 90 s");
        tick(world, 2);
        assertEquals(1, breeding.getNumChickens());
        tick(world, 5 * 90);
        assertEquals(6, breeding.getNumChickens());
        tick(world, 200);
        assertEquals(6, breeding.getNumChickens(), "at most six of its own roam");
    }

    @Test
    void coopChickensAreNotAWildFlock() {
        World world = newWorld(Ruleset.BUFFED);
        RubberSupplyManager manager = (RubberSupplyManager) world.getSupplyManager(RubberSupply.class);
        int wild = manager.getNumGroups();
        Player player = world.getPlayers()[0];
        RubberGroup flock = RubberGroup.newCoopFlock(world);
        Target target = world.getUnitGrid().findGridTargets(startX(player), startY(player), 1, true)[0];
        RubberSupply chicken = flock.spawn(target, player.getStartX(), player.getStartY());
        assertEquals(wild, manager.getNumGroups());
        tick(world, 3);
        while (!chicken.isEmpty())
            chicken.hit();
        assertEquals(0, flock.size());
        assertEquals(wild, manager.getNumGroups(), "catching a coop's last chicken frees no wild flock");
    }

    /** The totem takes five logs, then waits for a rock: peons fetch one by themselves. */
    @Test
    void peonsFinishTheTotemWithARock() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        Unit[] peons = new Unit[3];
        for (int i = 0; i < peons.length; i++)
            peons[i] = unit(player, player.getStartX(), player.getStartY(), Race.UNIT_PEON);
        LandscapeTarget site = site(player, Race.BUILDING_TOTEM);
        player.placeBuilding(peons, Race.BUILDING_TOTEM, site.getGridX(), site.getGridY());

        assertTrue(tickUntil(world, 600, () -> {
            LandBuilding totem = find(player, Race.BUILDING_TOTEM);
            return totem != null && totem.isComplete();
        }), "the totem was not finished in ten minutes");
        assertTrue(player.getRockHarvested() > 0, "no rock was fetched");
        assertEquals(1, world.getTotems().size());
    }

    /** Peons build the coop from wood, then catch two chickens and bring them in. */
    @Test
    void peonsBuildAndStockTheCoop() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        Unit[] peons = new Unit[6];
        for (int i = 0; i < peons.length; i++)
            peons[i] = unit(player, player.getStartX(), player.getStartY(), Race.UNIT_PEON);
        LandscapeTarget site = site(player, Race.BUILDING_CHICKEN_COOP);
        player.placeBuilding(peons, Race.BUILDING_CHICKEN_COOP, site.getGridX(), site.getGridY());

        assertTrue(tickUntil(world, 900, () -> {
            LandBuilding coop = find(player, Race.BUILDING_CHICKEN_COOP);
            return coop != null && coop.getChickenCoop() != null && !coop.getChickenCoop().needsStock();
        }), "the coop was not built and stocked in fifteen minutes");
        assertTrue(player.getRubberHarvested() >= 2);
        assertNull(find(player, Race.BUILDING_TOTEM));
        assertEquals(0, player.getRockHarvested(), "a coop takes no rock");
    }

    private static @NonNull LandscapeTarget site(@NonNull Player player, int building) {
        BuildingSiteScanFilter filter = new BuildingSiteScanFilter(player.getWorld().getUnitGrid(),
                player.getRace().getBuildingTemplate(building), 40, true);
        player.getWorld().getUnitGrid().scan(filter, startX(player), startY(player));
        assertFalse(filter.getResult().isEmpty(), "no site for building " + building);
        return filter.getResult().getFirst();
    }

    /** Hard AIs build both buildings under Buffed, stock and breed the coop; Easy ones build neither. */
    @Test
    void aiBuildsThemUnderBuffed() {
        for (String name : List.of("buffed-1v1", "buffed-6p")) {
            HeadlessMatchConfig config = Matches.ALL.get(name);
            HeadlessMatchResult result = Matches.play(name);
            for (int i = 0; i < config.players().size(); i++) {
                HeadlessMatchResult.Census census = result.census().get(i);
                String player = name + " player " + i + ": " + census;
                if (config.players().get(i).difficulty() == PlayerSlot.AI_EASY) {
                    assertFalse(census.completedBuildings().contains(Race.BUILDING_CHICKEN_COOP), player);
                    assertFalse(census.completedBuildings().contains(Race.BUILDING_TOTEM), player);
                }
            }
            long coops = result.census().stream().filter(
                    c -> c.completedBuildings().contains(Race.BUILDING_CHICKEN_COOP)).count();
            long totems = result.census().stream().filter(
                    c -> c.completedBuildings().contains(Race.BUILDING_TOTEM)).count();
            long bred = result.census().stream().filter(HeadlessMatchResult.Census::chickenCoopBred).count();
            assertTrue(coops > 0 && totems > 0 && bred > 0, name + ": " + result.census());
        }
    }

    /** Under Classic and Resurrected nobody ever has one: the AI does not even try. */
    @ParameterizedTest
    @ValueSource(strings = {"classic-1v1", "classic-6p", "resurrected-1v1", "resurrected-6p"})
    void nobodyHasThemOutsideBuffed(String name) {
        for (HeadlessMatchResult.Census census : Matches.play(name).census()) {
            assertFalse(census.completedBuildings().contains(Race.BUILDING_CHICKEN_COOP), name + ": " + census);
            assertFalse(census.completedBuildings().contains(Race.BUILDING_TOTEM), name + ": " + census);
        }
    }
}
