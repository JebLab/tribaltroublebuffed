package com.oddlabs.tt.headless;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.headless.HeadlessMatchConfig.PlayerConfig;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Action;
import com.oddlabs.tt.model.AttackScanFilter;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.DeployType;
import com.oddlabs.tt.model.DrumAura;
import com.oddlabs.tt.model.IronSupply;
import com.oddlabs.tt.model.LandBuilding;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.RubberGroup;
import com.oddlabs.tt.model.RubberSupply;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Snare;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.behaviour.GatherController;
import com.oddlabs.tt.model.behaviour.StunController;
import com.oddlabs.tt.model.weapon.Drum;
import com.oddlabs.tt.model.weapon.GearFactory;
import com.oddlabs.tt.model.weapon.Net;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.ruleset.Ruleset;
import com.oddlabs.tt.util.Target;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Buffed's Drum / Horn and Net / Snare (M9, docs/design/drum-and-net.md): only Buffed offers them, the Armory makes
 * and deploys them, the drummer never attacks, lifts its team nearby and is everyone's first target, the chicken
 * catcher takes a chicken in one stroke and lays snares that stun the first enemy, and the AI fields both. Played on
 * headless worlds without AIs unless a test says otherwise.
 */
final class BuffedDrumNetTest {
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

    /** A finished building near the player's start, as campaign scripts place them. */
    private static @NonNull LandBuilding build(@NonNull Player player, int building) {
        Building b = player.buildBuilding(building, UnitGrid.toGridCoordinate(player.getStartX()),
                UnitGrid.toGridCoordinate(player.getStartY()));
        assertNotNull(b, "no room for building " + building);
        assertTrue(b.isComplete());
        return (LandBuilding) b;
    }

    private static @NonNull Unit unit(@NonNull Player player, float x, float y, int template) {
        return new Unit(player, x, y, null, player.getRace().getUnitTemplate(template));
    }

    /** A unit at the player's start, far from the other player's. */
    private static @NonNull Unit home(@NonNull Player player, int template) {
        return unit(player, player.getStartX(), player.getStartY(), template);
    }

    /** An Armory with four peons at work and enough wood, iron and chickens for a few pieces of gear. */
    private static @NonNull LandBuilding armory(@NonNull Player player) {
        LandBuilding armory = build(player, Race.BUILDING_ARMORY);
        armory.getUnitContainer().increaseSupply(4);
        armory.fillSupplies(TreeSupply.class, 20);
        armory.fillSupplies(IronSupply.class, 10);
        armory.fillSupplies(RubberSupply.class, 10);
        return armory;
    }

    private static int stock(@NonNull Building building, @NonNull Class<?> gear) {
        return building.getSupplyContainer(gear).getNumSupplies();
    }

    /** A chicken of a flock of its own, landing on the free cell nearest to the grid point. */
    private static @NonNull RubberSupply chicken(@NonNull World world, int grid_x, int grid_y) {
        Target target = world.getUnitGrid().findGridTargets(grid_x, grid_y, 1, false)[0];
        return RubberGroup.newCoopFlock(world).spawn(target, UnitGrid.coordinateFromGrid(target.getGridX()),
                UnitGrid.coordinateFromGrid(target.getGridY()));
    }

    private static void order(@NonNull Unit unit, int grid_x, int grid_y, @NonNull Action action) {
        unit.getOwner().setLandscapeTarget(new Selectable<?>[]{unit}, grid_x, grid_y, action, false);
    }

    @ParameterizedTest
    @ValueSource(strings = {"classic", "resurrected"})
    void onlyBuffedOffersThem(String id) {
        World world = newWorld(Ruleset.fromId(id));
        Player player = world.getPlayers()[0];
        assertFalse(player.canBuildDrums());
        assertFalse(player.canBuildNets());
        LandBuilding armory = armory(player);
        player.buildDrumWeapons(armory, 2, false);
        player.buildNetWeapons(armory, 2, false);
        tick(world, 40);
        assertEquals(0, stock(armory, Drum.class), "the Armory made a drum");
        assertEquals(0, stock(armory, Net.class), "the Armory made a net");
        armory.fillSupplies(Drum.class, 1);
        player.deployUnits(armory, DeployType.DRUM_WARRIOR, 1);
        tick(world, 3);
        assertEquals(1, stock(armory, Drum.class), "a drum was deployed");
        assertTrue(world.getDrummers().isEmpty());
        Player buffed = newWorld(Ruleset.BUFFED).getPlayers()[0];
        assertTrue(buffed.canBuildDrums());
        assertTrue(buffed.canBuildNets());
    }

    /** 3 wood + 1 iron and 2 wood + 1 chicken, 60 man-seconds each: four peons make one in 15 s. */
    @Test
    void theArmoryMakesAndDeploysThem() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        LandBuilding armory = armory(player);
        player.buildDrumWeapons(armory, 1, false);
        tick(world, 14);
        assertEquals(0, stock(armory, Drum.class), "a drum in less than 15 s");
        assertTrue(tickUntil(world, 2, () -> stock(armory, Drum.class) == 1));
        assertEquals(17, stock(armory, TreeSupply.class));
        assertEquals(9, stock(armory, IronSupply.class));
        player.buildNetWeapons(armory, 1, false);
        tick(world, 14);
        assertEquals(0, stock(armory, Net.class), "a net in less than 15 s");
        assertTrue(tickUntil(world, 2, () -> stock(armory, Net.class) == 1));
        assertEquals(15, stock(armory, TreeSupply.class));
        assertEquals(9, stock(armory, RubberSupply.class));

        player.deployUnits(armory, DeployType.DRUM_WARRIOR, 1);
        player.deployUnits(armory, DeployType.NET_WARRIOR, 1);
        tick(world, 3);
        assertEquals(0, stock(armory, Drum.class));
        assertEquals(0, stock(armory, Net.class));
        assertEquals(2, armory.getUnitCount(), "each is one of the peons inside");
        assertEquals(1, world.getDrummers().size());
    }

    /** No attack bit, the warrior bit kept; a drummer sent at an enemy walks there and never strikes. */
    @Test
    void theDrummerNeverAttacks() {
        World world = newWorld(Ruleset.BUFFED);
        Player natives = world.getPlayers()[0];
        Player vikings = world.getPlayers()[1];
        Unit drummer = home(natives, Race.UNIT_WARRIOR_DRUM);
        assertFalse(drummer.getAbilities().hasAbilities(Abilities.ATTACK));
        assertTrue(drummer.getAbilities().hasAbilities(Abilities.THROW));
        assertTrue(drummer.isWarrior());
        assertTrue(drummer.isGearWarrior());
        assertTrue(drummer.isDrummer());
        assertInstanceOf(GearFactory.class, drummer.getWeaponFactory());
        assertEquals(.5f, drummer.getDefenseChance());
        LandBuilding quarters = build(vikings, Race.BUILDING_QUARTERS);
        int hit_points = quarters.getHitPoints();
        natives.setTarget(new Selectable<?>[]{drummer}, quarters, Action.ATTACK, true);
        tick(world, 60);
        assertEquals(hit_points, quarters.getHitPoints(), "a drummer struck a building");
        assertFalse(drummer.canAttack(quarters, true));
    }

    /** +0.10 hit and 15 % speed for its team within 12 m, itself included; not for enemies; two do not add up. */
    @Test
    void theDrumLiftsItsTeam() {
        World world = newWorld(Ruleset.BUFFED);
        Player natives = world.getPlayers()[0];
        Player vikings = world.getPlayers()[1];
        Unit warrior = home(natives, Race.UNIT_WARRIOR_ROCK);
        Unit enemy = unit(vikings, natives.getStartX(), natives.getStartY(), Race.UNIT_WARRIOR_ROCK);
        assertEquals(0f, DrumAura.getHitBonus(warrior), "a bonus without a drummer");
        assertEquals(4f, warrior.getMetersPerSecond());
        Unit drummer = home(natives, Race.UNIT_WARRIOR_DRUM);
        assertTrue(Math.abs(drummer.getPositionX() - warrior.getPositionX()) < 12f);
        assertEquals(.1f, DrumAura.getHitBonus(warrior));
        assertEquals(4f * 1.15f, warrior.getMetersPerSecond());
        assertEquals(4f * 1.15f, drummer.getMetersPerSecond(), "the drummer walks with its own beat");
        assertEquals(0f, DrumAura.getHitBonus(enemy), "the drum lifted an enemy");
        assertEquals(4f, enemy.getMetersPerSecond());
        home(natives, Race.UNIT_WARRIOR_DRUM);
        assertEquals(.1f, DrumAura.getHitBonus(warrior), "two drummers added up");
        assertEquals(4f * 1.15f, warrior.getMetersPerSecond());
        Unit far = unit(natives, vikings.getStartX(), vikings.getStartY(), Race.UNIT_WARRIOR_ROCK);
        assertEquals(0f, DrumAura.getHitBonus(far), "the drum reached beyond 12 m");
        drummer.removeNow();
        assertEquals(1, world.getDrummers().size(), "a drummer that left the world still counts");
    }

    /** Ahead of every other unit, behind ships. */
    @Test
    void theDrummerIsTargetedFirst() {
        World world = newWorld(Ruleset.BUFFED);
        Player natives = world.getPlayers()[0];
        Player vikings = world.getPlayers()[1];
        Unit warrior = home(natives, Race.UNIT_WARRIOR_IRON);
        Unit peon = home(natives, Race.UNIT_PEON);
        Unit drummer = home(natives, Race.UNIT_WARRIOR_DRUM);
        assertEquals(AttackScanFilter.Priority.DRUMMER, drummer.getAttackPriority());
        assertTrue(AttackScanFilter.Priority.DRUMMER.value > AttackScanFilter.Priority.WARRIOR.value);
        assertTrue(AttackScanFilter.Priority.SHIP.value > AttackScanFilter.Priority.DRUMMER.value);
        AttackScanFilter filter = new AttackScanFilter(vikings, AttackScanFilter.UNIT_RANGE);
        for (Unit unit : List.of(warrior, drummer, peon))
            filter.filter(unit.getGridX(), unit.getGridY(), unit);
        assertSame(drummer, filter.removeTarget());
    }

    /** One stroke instead of a peon's ten; the chicken goes to the Armory like a peon's. */
    @Test
    void theNetCatchesAChickenInOneStroke() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        LandBuilding armory = build(player, Race.BUILDING_ARMORY);
        Unit catcher = home(player, Race.UNIT_WARRIOR_NET);
        assertTrue(catcher.isNetter());
        assertEquals(.3f, catcher.getDefenseChance());
        RubberSupply chicken = chicken(world, catcher.getGridX() + 3, catcher.getGridY());
        tick(world, 3);
        player.setTarget(new Selectable<?>[]{catcher}, chicken, Action.DEFAULT, false);
        assertTrue(tickUntil(world, 6, () -> catcher.getSupplyContainer().getNumSupplies() == 1),
                "no chicken in 6 s (a peon needs 10 strokes)");
        assertTrue(chicken.isDead());
        assertTrue(tickUntil(world, 60, () -> stock(armory, RubberSupply.class) == 1), "the chicken never got home");
        assertFalse(catcher.isDead(), "the catcher went into the Armory");
    }

    /** Laid where it is sent, from the cell next to it; the first enemy on it is stunned for 4 s; friends pass. */
    @Test
    void aSnareStunsTheFirstEnemy() {
        World world = newWorld(Ruleset.BUFFED);
        Player natives = world.getPlayers()[0];
        Player vikings = world.getPlayers()[1];
        Unit catcher = home(natives, Race.UNIT_WARRIOR_NET);
        int x = catcher.getGridX() + 4;
        int y = catcher.getGridY();
        order(catcher, x, y, Action.SNARE);
        assertTrue(tickUntil(world, 30, () -> world.getSnares().size() == 1), "no snare laid");
        Snare snare = world.getSnares().getFirst();
        assertEquals(List.of(snare), catcher.getSnares());
        assertEquals(1, natives.getSnaresLaid());
        int sx = snare.getGridX();
        int sy = snare.getGridY();
        assertTrue(Math.abs(sx - x) <= 2 && Math.abs(sy - y) <= 2, "laid at " + sx + "," + sy);
        // The catcher walks off, out of reach of the enemy it is about to stun, which it would otherwise strike.
        order(catcher, x - 20, y, Action.MOVE);

        Unit friend = unit(natives, UnitGrid.coordinateFromGrid(sx - 4), UnitGrid.coordinateFromGrid(sy),
                Race.UNIT_PEON);
        order(friend, sx + 4, sy, Action.MOVE);
        tick(world, 5);
        assertEquals(1, world.getSnares().size(), "a friend sprang it");

        Unit enemy = unit(vikings, UnitGrid.coordinateFromGrid(sx + 4), UnitGrid.coordinateFromGrid(sy),
                Race.UNIT_PEON);
        order(enemy, sx, sy, Action.MOVE);
        assertTrue(tickUntil(world, 20, world.getSnares()::isEmpty), "the enemy never sprang it");
        assertInstanceOf(StunController.class, enemy.getCurrentController());
        assertEquals(0f, enemy.getDefenseChance(), "a stunned unit dodged");
        assertEquals(1, natives.getSnaresSprung());
        assertTrue(catcher.getSnares().isEmpty());
        tick(world, 3.5f);
        assertInstanceOf(StunController.class, enemy.getCurrentController(), "stunned for less than 4 s");
        tick(world, 1);
        assertFalse(enemy.getCurrentController() instanceof StunController, "stunned for more than 4 s");
    }

    /** At most 3 lying per catcher: a fourth takes up the oldest; they go with the catcher. */
    @Test
    void aCatcherKeepsThreeSnares() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        Unit catcher = home(player, Race.UNIT_WARRIOR_NET);
        int x = catcher.getGridX();
        int y = catcher.getGridY();
        for (int i = 1; i <= 4; i++) {
            int count = Math.min(i, 3);
            int laid = player.getSnaresLaid();
            order(catcher, x + 3 * i, y, Action.SNARE);
            assertTrue(tickUntil(world, 30, () -> player.getSnaresLaid() == laid + 1), "snare " + i);
            assertEquals(count, world.getSnares().size());
            assertEquals(count, catcher.getSnares().size());
        }
        Snare oldest = catcher.getSnares().getFirst();
        assertTrue(oldest.getGridX() > x + 3, "the first snare was not taken up");
        catcher.removeNow();
        assertTrue(world.getSnares().isEmpty(), "snares outlived their catcher");
    }

    /**
     * The AI groups units by their controller's key: gatherers by what they gather and whether they are peons, idle
     * units apart from drummers and catchers, so neither joins the army or the peons. Keys compare by value.
     */
    @Test
    void supportUnitsGroupApart() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        Unit peon = home(player, Race.UNIT_PEON);
        Unit other_peon = home(player, Race.UNIT_PEON);
        Unit catcher = home(player, Race.UNIT_WARRIOR_NET);
        Unit drummer = home(player, Race.UNIT_WARRIOR_DRUM);
        Unit warrior = home(player, Race.UNIT_WARRIOR_ROCK);
        assertEquals(new GatherController<>(peon, null, TreeSupply.class).getKey(),
                new GatherController<>(other_peon, null, TreeSupply.class).getKey());
        assertNotEquals(new GatherController<>(peon, null, TreeSupply.class).getKey(),
                new GatherController<>(peon, null, IronSupply.class).getKey());
        assertNotEquals(new GatherController<>(peon, null, RubberSupply.class).getKey(),
                new GatherController<>(catcher, null, RubberSupply.class).getKey());
        assertNotEquals(warrior.getPrimaryController().getKey(), drummer.getPrimaryController().getKey());
        assertEquals(drummer.getPrimaryController().getKey(), catcher.getPrimaryController().getKey());
        assertEquals(warrior.getPrimaryController().getKey(),
                home(player, Race.UNIT_WARRIOR_SHIELD).getPrimaryController().getKey());
    }

    /** Normal and Hard AIs field drummers and catchers under Buffed and lay snares; Easy ones never do. */
    @Test
    void aiFieldsDrumsAndNetsUnderBuffed() {
        for (String name : List.of("buffed-1v1", "buffed-6p")) {
            HeadlessMatchConfig config = Matches.ALL.get(name);
            HeadlessMatchResult result = Matches.play(name);
            for (int i = 0; i < config.players().size(); i++) {
                HeadlessMatchResult.Census census = result.census().get(i);
                if (config.players().get(i).difficulty() == PlayerSlot.AI_EASY) {
                    String player = name + " player " + i + ": " + census;
                    assertFalse(census.unitTypes().contains(Race.UNIT_WARRIOR_DRUM), player);
                    assertFalse(census.unitTypes().contains(Race.UNIT_WARRIOR_NET), player);
                    assertEquals(0, census.snaresLaid(), player);
                }
            }
            long drums = result.census().stream().filter(
                    c -> c.unitTypes().contains(Race.UNIT_WARRIOR_DRUM)).count();
            long nets = result.census().stream().filter(
                    c -> c.unitTypes().contains(Race.UNIT_WARRIOR_NET)).count();
            long snares = result.census().stream().mapToInt(HeadlessMatchResult.Census::snaresLaid).sum();
            assertTrue(drums > 0 && nets > 0 && snares > 0, name + ": " + result.census());
        }
    }

    /** Under Classic and Resurrected nobody fields them: the AI does not even order them. */
    @ParameterizedTest
    @ValueSource(strings = {"classic-1v1", "classic-6p", "resurrected-1v1", "resurrected-6p"})
    void nobodyFieldsThemOutsideBuffed(String name) {
        for (HeadlessMatchResult.Census census : Matches.play(name).census()) {
            assertFalse(census.unitTypes().contains(Race.UNIT_WARRIOR_DRUM), name + ": " + census);
            assertFalse(census.unitTypes().contains(Race.UNIT_WARRIOR_NET), name + ": " + census);
            assertEquals(0, census.snaresLaid(), name + ": " + census);
        }
    }
}
