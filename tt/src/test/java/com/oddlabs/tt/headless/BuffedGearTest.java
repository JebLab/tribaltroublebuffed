package com.oddlabs.tt.headless;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.headless.HeadlessMatchConfig.PlayerConfig;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Action;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.DeployType;
import com.oddlabs.tt.model.IronSupply;
import com.oddlabs.tt.model.LandBuilding;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.RockSupply;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.weapon.GearFactory;
import com.oddlabs.tt.model.weapon.Shield;
import com.oddlabs.tt.model.weapon.Torch;
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

import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Buffed's Shield and Torch (M6, docs/design/gear.md): only Buffed offers them, the Armory makes and deploys them,
 * they fight hand to hand and stay out of towers, a torch sets buildings on fire, and the AI fields both. Played on
 * headless worlds without AIs unless a test says otherwise.
 */
final class BuffedGearTest {
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

    /**
     * A warrior at its own start, out of reach of the enemy's buildings, so that it does not join in by itself: the
     * tests strike with its weapon directly.
     */
    private static @NonNull Unit away(@NonNull Player player, int template) {
        return unit(player, player.getStartX(), player.getStartY(), template);
    }

    /** An Armory with four peons at work and enough wood, rock and iron for a few pieces of gear. */
    private static @NonNull LandBuilding armory(@NonNull Player player) {
        LandBuilding armory = build(player, Race.BUILDING_ARMORY);
        armory.getUnitContainer().increaseSupply(4);
        armory.fillSupplies(TreeSupply.class, 20);
        armory.fillSupplies(RockSupply.class, 10);
        armory.fillSupplies(IronSupply.class, 10);
        return armory;
    }

    private static int stock(@NonNull Building building, @NonNull Class<?> gear) {
        return building.getSupplyContainer(gear).getNumSupplies();
    }

    private static @Nullable Unit find(@NonNull Player player, int template) {
        for (Selectable<?> s : player.getUnits().getSet()) {
            if (s instanceof Unit u && !u.isDead() && u.getTemplate() == player.getRace().getUnitTemplate(template))
                return u;
        }
        return null;
    }

    @ParameterizedTest
    @ValueSource(strings = {"classic", "resurrected"})
    void onlyBuffedOffersThem(String id) {
        World world = newWorld(Ruleset.fromId(id));
        Player player = world.getPlayers()[0];
        assertFalse(player.canBuildShields());
        assertFalse(player.canBuildTorches());
        LandBuilding armory = armory(player);
        player.buildShieldWeapons(armory, 2, false);
        player.buildTorchWeapons(armory, 2, false);
        tick(world, 60);
        assertEquals(0, stock(armory, Shield.class), "the Armory made a shield");
        assertEquals(0, stock(armory, Torch.class), "the Armory made a torch");
        armory.fillSupplies(Shield.class, 1);
        player.deployUnits(armory, DeployType.SHIELD_WARRIOR, 1);
        tick(world, 5);
        assertEquals(1, stock(armory, Shield.class), "a shield was deployed");

        Player buffed = newWorld(Ruleset.BUFFED).getPlayers()[0];
        assertTrue(buffed.canBuildShields());
        assertTrue(buffed.canBuildTorches());
    }

    /** 2 wood + 1 rock in 40 man-seconds; 2 wood + 1 rock + 1 iron in 80. Four peons make a shield in 10 s. */
    @Test
    void theArmoryMakesAndDeploysGear() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        LandBuilding armory = armory(player);
        player.buildShieldWeapons(armory, 2, false);
        tick(world, 9);
        assertEquals(0, stock(armory, Shield.class), "a shield in less than 10 s");
        tick(world, 2);
        assertEquals(1, stock(armory, Shield.class));
        assertEquals(18, armory.getSupplyContainer(TreeSupply.class).getNumSupplies());
        assertEquals(9, armory.getSupplyContainer(RockSupply.class).getNumSupplies());
        assertEquals(10, armory.getSupplyContainer(IronSupply.class).getNumSupplies(), "a shield takes no iron");
        assertTrue(tickUntil(world, 15, () -> stock(armory, Shield.class) == 2));

        player.buildTorchWeapons(armory, 1, false);
        tick(world, 19);
        assertEquals(0, stock(armory, Torch.class), "a torch in less than 20 s");
        tick(world, 2);
        assertEquals(1, stock(armory, Torch.class));
        assertEquals(9, armory.getSupplyContainer(IronSupply.class).getNumSupplies());

        player.deployUnits(armory, DeployType.SHIELD_WARRIOR, 1);
        player.deployUnits(armory, DeployType.TORCH_WARRIOR, 1);
        assertTrue(tickUntil(world, 10, () -> find(player, Race.UNIT_WARRIOR_SHIELD) != null
                && find(player, Race.UNIT_WARRIOR_TORCH) != null), "no gear warriors deployed");
        assertEquals(1, stock(armory, Shield.class));
        assertEquals(0, stock(armory, Torch.class));
        assertEquals(2, armory.getUnitCount(), "each warrior is one of the peons inside");
    }

    /** They strike at range 0 like a peon, keep the warrior's bits, and stay out of towers and the Quarters. */
    @Test
    void gearWarriorsFightHandToHand() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        Unit shield = unit(player, player.getStartX(), player.getStartY(), Race.UNIT_WARRIOR_SHIELD);
        Unit torch = unit(player, player.getStartX(), player.getStartY(), Race.UNIT_WARRIOR_TORCH);
        Unit rock = unit(player, player.getStartX(), player.getStartY(), Race.UNIT_WARRIOR_ROCK);
        for (Unit gear : List.of(shield, torch)) {
            assertInstanceOf(GearFactory.class, gear.getWeaponFactory());
            assertEquals(0f, gear.getWeaponFactory().getRange());
            assertTrue(gear.isWarrior());
            assertTrue(gear.isGearWarrior());
        }
        assertEquals(Shield.class, shield.getWeaponFactory().getType());
        assertEquals(Torch.class, torch.getWeaponFactory().getType());
        assertEquals(.85f, shield.getDefenseChance());
        assertEquals(.3f, torch.getDefenseChance());
        assertFalse(rock.isGearWarrior());

        LandBuilding tower = build(player, Race.BUILDING_TOWER);
        assertFalse(tower.getUnitContainer().canEnter(shield), "a shield bearer manned a tower");
        assertFalse(tower.getUnitContainer().canEnter(torch), "a torchbearer manned a tower");
        assertTrue(tower.getUnitContainer().canEnter(rock));
        LandBuilding quarters = build(player, Race.BUILDING_QUARTERS);
        assertFalse(quarters.getUnitContainer().canEnter(shield), "the Quarters took a warrior");
    }

    /** A warrior who walks back into the Armory hands in the gear, as throwers hand in their weapons. */
    @Test
    void gearGoesBackToStock() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        LandBuilding armory = build(player, Race.BUILDING_ARMORY);
        Unit torch = unit(player, armory.getPositionX(), armory.getPositionY(), Race.UNIT_WARRIOR_TORCH);
        player.setTarget(new Selectable<?>[]{torch}, armory, Action.DEFAULT, false);
        assertTrue(tickUntil(world, 30, torch::isDead), "the torchbearer did not go in");
        assertEquals(1, stock(armory, Torch.class));
        assertEquals(1, armory.getUnitCount());
    }

    /** A torch's blow always takes 6 and lights a fire: 2 a second for 15 s, started again by another blow. */
    @Test
    void torchesSetBuildingsOnFire() {
        World world = newWorld(Ruleset.BUFFED);
        Player natives = world.getPlayers()[0];
        Player vikings = world.getPlayers()[1];
        LandBuilding quarters = build(vikings, Race.BUILDING_QUARTERS);
        Unit torch = away(natives, Race.UNIT_WARRIOR_TORCH);
        int hit_points = quarters.getHitPoints();

        torch.getWeaponFactory().attack(torch, quarters);
        assertEquals(hit_points - 6, quarters.getHitPoints(), "a torch always hits a building for 6");
        assertTrue(quarters.isBurning());
        assertEquals(natives, quarters.getFireOwner());
        assertEquals(1, natives.getFiresLit());

        tick(world, 10);
        int after_ten_seconds = quarters.getHitPoints();
        assertTrue(Math.abs(hit_points - 6 - 20 - after_ten_seconds) <= 1,
                "2 hit points a second: " + after_ten_seconds);
        torch.getWeaponFactory().attack(torch, quarters);
        tick(world, 14);
        assertTrue(quarters.isBurning(), "another blow starts the 15 s again");
        tick(world, 2);
        assertFalse(quarters.isBurning());
        assertEquals(after_ten_seconds - 6 - 30, quarters.getHitPoints(), "a fire takes 30 hit points in all");
        tick(world, 10);
        assertEquals(after_ten_seconds - 6 - 30, quarters.getHitPoints(), "a fire out takes nothing");
    }

    /** A peon's repair puts the fire out at once. */
    @Test
    void repairPutsTheFireOut() {
        World world = newWorld(Ruleset.BUFFED);
        Player natives = world.getPlayers()[0];
        Player vikings = world.getPlayers()[1];
        LandBuilding armory = build(vikings, Race.BUILDING_ARMORY);
        Unit torch = away(natives, Race.UNIT_WARRIOR_TORCH);
        torch.getWeaponFactory().attack(torch, armory);
        tick(world, 3);
        assertTrue(armory.isBurning());
        int hit_points = armory.getHitPoints();
        armory.repair(5);
        assertFalse(armory.isBurning());
        assertEquals(hit_points + 5, armory.getHitPoints());
        tick(world, 10);
        assertEquals(hit_points + 5, armory.getHitPoints());
    }

    /** A fire can bring a building down; the player whose torch lit it is credited. */
    @Test
    void fireBurnsATotemDown() {
        World world = newWorld(Ruleset.BUFFED);
        Player natives = world.getPlayers()[0];
        Player vikings = world.getPlayers()[1];
        LandBuilding totem = build(vikings, Race.BUILDING_TOTEM);
        Unit torch = away(natives, Race.UNIT_WARRIOR_TORCH);
        torch.getWeaponFactory().attack(torch, totem);
        assertEquals(24, totem.getHitPoints());
        assertTrue(tickUntil(world, 13, totem::isDead), "30 hit points did not burn down in 12 s");
        assertEquals(1, natives.getBuildingsDestroyed());
        assertEquals(1, vikings.getBuildingsLost());
    }

    /** A shield's blow on a building is a peon's: never a fire, and only towers always take damage. */
    @Test
    void shieldsDoNotBurn() {
        World world = newWorld(Ruleset.BUFFED);
        Player natives = world.getPlayers()[0];
        Player vikings = world.getPlayers()[1];
        LandBuilding tower = build(vikings, Race.BUILDING_TOWER);
        Unit shield = away(natives, Race.UNIT_WARRIOR_SHIELD);
        int hit_points = tower.getHitPoints();
        shield.getWeaponFactory().attack(shield, tower);
        assertEquals(hit_points - 6, tower.getHitPoints(), "a blow on a tower takes 6");
        assertFalse(tower.isBurning());
    }

    /** Normal and Hard AIs field shields and torches under Buffed and set buildings on fire; Easy ones never do. */
    @Test
    void aiFieldsGearUnderBuffed() {
        for (String name : List.of("buffed-1v1", "buffed-6p")) {
            HeadlessMatchConfig config = Matches.ALL.get(name);
            HeadlessMatchResult result = Matches.play(name);
            for (int i = 0; i < config.players().size(); i++) {
                HeadlessMatchResult.Census census = result.census().get(i);
                if (config.players().get(i).difficulty() == PlayerSlot.AI_EASY) {
                    String player = name + " player " + i + ": " + census;
                    assertFalse(census.unitTypes().contains(Race.UNIT_WARRIOR_SHIELD), player);
                    assertFalse(census.unitTypes().contains(Race.UNIT_WARRIOR_TORCH), player);
                    assertEquals(0, census.firesLit(), player);
                }
            }
            long shields = result.census().stream().filter(
                    c -> c.unitTypes().contains(Race.UNIT_WARRIOR_SHIELD)).count();
            long torches = result.census().stream().filter(
                    c -> c.unitTypes().contains(Race.UNIT_WARRIOR_TORCH)).count();
            long fires = result.census().stream().mapToInt(HeadlessMatchResult.Census::firesLit).sum();
            assertTrue(shields > 0 && torches > 0 && fires > 0, name + ": " + result.census());
        }
    }

    /** Under Classic and Resurrected nobody ever fields gear: the AI does not even order it. */
    @ParameterizedTest
    @ValueSource(strings = {"classic-1v1", "classic-6p", "resurrected-1v1", "resurrected-6p"})
    void nobodyFieldsGearOutsideBuffed(String name) {
        for (HeadlessMatchResult.Census census : Matches.play(name).census()) {
            assertFalse(census.unitTypes().contains(Race.UNIT_WARRIOR_SHIELD), name + ": " + census);
            assertFalse(census.unitTypes().contains(Race.UNIT_WARRIOR_TORCH), name + ": " + census);
            assertEquals(0, census.firesLit(), name + ": " + census);
        }
    }
}
