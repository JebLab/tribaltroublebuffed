package com.oddlabs.tt.headless;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.headless.HeadlessMatchConfig.PlayerConfig;
import com.oddlabs.tt.landscape.LandscapeTarget;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Action;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.DeployType;
import com.oddlabs.tt.model.IronSupply;
import com.oddlabs.tt.model.LandBuilding;
import com.oddlabs.tt.model.Lodge;
import com.oddlabs.tt.model.MountUnitContainer;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.RockSupply;
import com.oddlabs.tt.model.RubberSupply;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.ShelterUnitContainer;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.UnitTemplate;
import com.oddlabs.tt.model.behaviour.RepairBehaviour;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.player.BuildingSiteScanFilter;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.ruleset.Ruleset;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Buffed's Great Tower, Spirit Lodge / Mead Hall and Champion (M8, docs/design/great-tower.md and
 * lodge-and-champion.md): only Buffed offers them, three throwers fire from the Great Tower, both buildings are
 * finished with their second material, the Lodge shelters units and halves the charge time of chieftains nearby, it
 * trains Champions from its peons with the cost taken from the Armory, at most five alive, and the AI uses them.
 * Played on headless worlds without AIs unless a test says otherwise.
 */
final class BuffedTowerLodgeTest {
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

    /** A construction site near the player's start, placed but not built. */
    private static @NonNull LandBuilding site(@NonNull Player player, int building) {
        BuildingSiteScanFilter filter = new BuildingSiteScanFilter(player.getWorld().getUnitGrid(),
                player.getRace().getBuildingTemplate(building), 40, true);
        player.getWorld().getUnitGrid().scan(filter, UnitGrid.toGridCoordinate(player.getStartX()),
                UnitGrid.toGridCoordinate(player.getStartY()));
        assertFalse(filter.getResult().isEmpty(), "no site for building " + building);
        LandscapeTarget target = filter.getResult().getFirst();
        Building b = player.getRace().getBuildingTemplate(building).create(player, target.getGridX(),
                target.getGridY());
        b.place();
        return (LandBuilding) b;
    }

    private static @NonNull Unit unit(@NonNull Player player, float x, float y, int template) {
        return new Unit(player, x, y, null, player.getRace().getUnitTemplate(template));
    }

    private static @NonNull Unit unitAt(@NonNull Player player, @NonNull Building building, int template) {
        return unit(player, building.getPositionX(), building.getPositionY(), template);
    }

    /** A building's unit container, as {@code T}. */
    @SuppressWarnings("unchecked")
    private static <T> T container(@NonNull Building building) {
        return (T) building.getUnitContainer();
    }

    private static @NonNull Unit chieftain(@NonNull Player player, float x, float y) {
        Unit chieftain = new Unit(player, x, y, null, player.getRace().getUnitTemplate(Race.UNIT_CHIEFTAIN));
        player.setActiveChieftain(chieftain);
        return chieftain;
    }

    private static int stock(@NonNull Building building, @NonNull Class<?> resource) {
        return building.getSupplyContainer(resource).getNumSupplies();
    }

    @ParameterizedTest
    @ValueSource(strings = {"classic", "resurrected"})
    void onlyBuffedOffersThem(String id) {
        Player player = newWorld(Ruleset.fromId(id)).getPlayers()[0];
        assertFalse(player.canBuild(Race.BUILDING_GREAT_TOWER));
        assertFalse(player.canBuild(Race.BUILDING_LODGE));
        Player buffed = newWorld(Ruleset.BUFFED).getPlayers()[0];
        assertTrue(buffed.canBuild(Race.BUILDING_GREAT_TOWER));
        assertTrue(buffed.canBuild(Race.BUILDING_LODGE));
    }

    /** Three throwers man it, each at its own place with the Tower's +8 m; gear and a fourth stay out. */
    @Test
    void theGreatTowerHoldsThreeThrowers() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        LandBuilding tower = build(player, Race.BUILDING_GREAT_TOWER);
        MountUnitContainer container = container(tower);
        assertFalse(container.canEnter(unitAt(player, tower, Race.UNIT_WARRIOR_SHIELD)), "a shield manned it");
        assertFalse(container.canEnter(unitAt(player, tower, Race.UNIT_CHAMPION)), "a Champion manned it");
        assertFalse(container.canEnter(unitAt(player, tower, Race.UNIT_PEON)), "a peon manned it");
        Unit outside = unitAt(player, tower, Race.UNIT_WARRIOR_ROCK);
        List<Unit> throwers = new ArrayList<>();
        Set<List<Float>> places = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            Unit thrower = unitAt(player, tower, Race.UNIT_WARRIOR_ROCK);
            assertTrue(container.canEnter(thrower), "thrower " + i + " was refused");
            container.enter(thrower);
            throwers.add(thrower);
            places.add(List.of(thrower.getPositionX(), thrower.getPositionY()));
            assertTrue(thrower.isMounted());
            assertEquals(outside.getRange(tower) + MountUnitContainer.ATTACK_RANGE_INCREASE, thrower.getRange(tower),
                    1e-4f);
        }
        assertEquals(3, places.size(), "two throwers share a place");
        assertFalse(container.canEnter(outside), "a fourth thrower went in");

        tower.exitTower();
        assertEquals(2, container.getNumSupplies());
        assertFalse(throwers.get(2).isMounted(), "the last thrower in was not the first out");
        assertEquals(outside.getRange(tower), throwers.get(2).getRange(tower));
    }

    /** All three throwers fire at enemies in reach. */
    @Test
    void threeThrowersFireFromTheGreatTower() {
        World world = newWorld(Ruleset.BUFFED);
        Player natives = world.getPlayers()[0];
        Player vikings = world.getPlayers()[1];
        LandBuilding tower = build(natives, Race.BUILDING_GREAT_TOWER);
        MountUnitContainer container = container(tower);
        List<Unit> throwers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Unit thrower = unitAt(natives, tower, Race.UNIT_WARRIOR_IRON);
            container.enter(thrower);
            throwers.add(thrower);
        }
        for (int i = 0; i < 12; i++)
            unit(vikings, tower.getPositionX() + 10f, tower.getPositionY() + 2f * (i - 6), Race.UNIT_PEON);
        boolean[] threw = new boolean[throwers.size()];
        assertTrue(tickUntil(world, 10, () -> {
            for (int i = 0; i < threw.length; i++)
                threw[i] |= !throwers.get(i).isDead() && throwers.get(i).getAnimation() == Unit.Animation.THROWING;
            return threw[0] && threw[1] && threw[2];
        }), "not every thrower fired: " + List.of(threw[0], threw[1], threw[2]));
        tick(world, 2);
        assertTrue(natives.getWeaponsThrown() >= 3, "fewer than three throws: " + natives.getWeaponsThrown());
    }

    /** Logs build 250 of its 300 hit points; then it waits for 10 rocks, each the last 5 (R-35). */
    @Test
    void theGreatTowerIsFinishedWithRock() {
        World world = newWorld(Ruleset.BUFFED);
        LandBuilding tower = site(world.getPlayers()[0], Race.BUILDING_GREAT_TOWER);
        assertFinishedWith(tower, RockSupply.class, 10);
    }

    /** Logs build 175 of its 200 hit points; then it waits for 5 iron (R-35). */
    @Test
    void theLodgeIsFinishedWithIron() {
        World world = newWorld(Ruleset.BUFFED);
        LandBuilding lodge = site(world.getPlayers()[0], Race.BUILDING_LODGE);
        assertFinishedWith(lodge, IronSupply.class, 5);
        assertEquals(1, world.getLodges().size());
    }

    private static void assertFinishedWith(@NonNull LandBuilding building, @NonNull Class<?> material, int loads) {
        int wood = building.getTemplate().getMaxHitPoints() - loads * RepairBehaviour.REPAIRS_PER_SUPPLY;
        building.repair(wood - 2);
        assertTrue(building.needsMaterial(TreeSupply.class), "it stopped taking logs early");
        building.repair(1);
        assertEquals(wood, building.getHitPoints());
        assertFalse(building.needsMaterial(TreeSupply.class), "it took more logs than its share");
        assertEquals(material, building.getWorkMaterial());
        for (int i = 0; i < loads; i++) {
            assertFalse(building.isComplete(), "complete after " + i + " loads");
            assertTrue(building.needsMaterial(material.asSubclass(com.oddlabs.tt.model.Supply.class)));
            building.deliverMaterial(material.asSubclass(com.oddlabs.tt.model.Supply.class));
        }
        assertTrue(building.isComplete());
        assertFalse(building.hasWork());
    }

    /**
     * Any of its owner's units but the chieftain goes in, up to 30; they count as units, are safe inside and come out
     * as what they were, in the order they went in.
     */
    @Test
    void theLodgeSheltersUnits() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        LandBuilding lodge = build(player, Race.BUILDING_LODGE);
        ShelterUnitContainer shelter = container(lodge);
        Unit chieftain = chieftain(player, lodge.getPositionX(), lodge.getPositionY());
        assertFalse(chieftain.canEnter(lodge), "the chieftain went in");
        int[] templates = {Race.UNIT_WARRIOR_SHIELD, Race.UNIT_PEON, Race.UNIT_WARRIOR_IRON};
        List<Unit> units = new ArrayList<>();
        for (int template : templates)
            units.add(unitAt(player, lodge, template));
        int count = player.getUnitCountContainer().getNumSupplies();
        player.setTarget(units.toArray(Selectable[]::new), lodge, Action.DEFAULT, false);
        assertTrue(tickUntil(world, 30, () -> units.stream().allMatch(Unit::isDead)), "not everyone went in");
        assertEquals(3, lodge.getUnitCount());
        assertEquals(count, player.getUnitCountContainer().getNumSupplies(), "sheltered units stopped counting");

        player.deployUnits(lodge, DeployType.SHELTERED, 3);
        tick(world, 3);
        assertEquals(0, lodge.getUnitCount());
        List<UnitTemplate> out = new ArrayList<>();
        for (Selectable<?> s : player.getUnits().getSet()) {
            if (s instanceof Unit unit && !unit.isDead() && unit != chieftain)
                out.add(unit.getTemplate());
        }
        List<UnitTemplate> expected = new ArrayList<>();
        for (int template : templates)
            expected.add(player.getRace().getUnitTemplate(template));
        assertEquals(expected, out, "they did not come out as they went in");
        assertEquals(count, player.getUnitCountContainer().getNumSupplies());

        for (int i = 0; i < 30; i++)
            shelter.enter(unitAt(player, lodge, Race.UNIT_PEON));
        assertFalse(shelter.canEnter(unitAt(player, lodge, Race.UNIT_PEON)), "a 31st unit went in");
    }

    /** What is inside dies with the Lodge. */
    @Test
    void shelteredUnitsDieWithTheLodge() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        LandBuilding lodge = build(player, Race.BUILDING_LODGE);
        ShelterUnitContainer shelter = container(lodge);
        List<Unit> warriors = new ArrayList<>();
        for (int i = 0; i < 4; i++)
            warriors.add(unitAt(player, lodge, Race.UNIT_WARRIOR_ROCK));
        int count = player.getUnitCountContainer().getNumSupplies();
        for (Unit warrior : warriors)
            shelter.enter(warrior);
        assertEquals(count, player.getUnitCountContainer().getNumSupplies());
        lodge.hit(1000, 0f, 1f, world.getPlayers()[1]);
        assertEquals(count - 4, player.getUnitCountContainer().getNumSupplies());
        assertTrue(world.getLodges().isEmpty());
    }

    /**
     * A chieftain within 30 m of a finished Lodge of its team charges both spells twice as fast; farther away, or by
     * an enemy's Lodge, as before.
     */
    @Test
    void theLodgeHalvesTheChargeTimeNearby() {
        World world = newWorld(Ruleset.BUFFED);
        Player natives = world.getPlayers()[0];
        Player vikings = world.getPlayers()[1];
        Unit before = chieftain(natives, natives.getStartX(), natives.getStartY());
        assertEquals(1f, Lodge.getSpellChargeFactor(before), "a world without Lodges");
        LandBuilding lodge = build(natives, Race.BUILDING_LODGE);
        Unit near = chieftain(natives, lodge.getPositionX(), lodge.getPositionY());
        Unit enemy = chieftain(vikings, lodge.getPositionX(), lodge.getPositionY());
        Unit far = chieftain(natives, lodge.getPositionX() + 40f, lodge.getPositionY());
        float dx = far.getPositionX() - lodge.getPositionX();
        float dy = far.getPositionY() - lodge.getPositionY();
        assertTrue(dx * dx + dy * dy > 30f * 30f, "test setup: a chieftain out of the Lodge's reach");
        assertEquals(2f, Lodge.getSpellChargeFactor(near));
        assertEquals(1f, Lodge.getSpellChargeFactor(enemy), "an enemy's Lodge helped");
        assertEquals(1f, Lodge.getSpellChargeFactor(far));
        vikings.killSelection(new Selectable<?>[]{enemy});

        tick(world, 21);
        assertTrue(near.canDoMagic(0), "the first spell was not ready after 21 s by the Lodge");
        assertFalse(near.canDoMagic(1));
        assertFalse(far.canDoMagic(0), "the first spell was ready after 21 s away from the Lodge");
        tick(world, 15);
        assertTrue(near.canDoMagic(1), "the second spell was not ready after 36 s by the Lodge");
        assertEquals(36f / 70f, far.getMagicProgress(1), .02f);
    }

    /**
     * A sheltered peon and 2 wood, 1 iron and 1 chicken from the Armory make a Champion in 30 s; the cost goes when
     * the training starts.
     */
    @Test
    void theLodgeTrainsAChampion() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        LandBuilding armory = build(player, Race.BUILDING_ARMORY);
        armory.fillSupplies(TreeSupply.class, 4);
        armory.fillSupplies(IronSupply.class, 2);
        armory.fillSupplies(RubberSupply.class, 2);
        LandBuilding lodge = build(player, Race.BUILDING_LODGE);
        ShelterUnitContainer shelter = container(lodge);
        shelter.enter(unitAt(player, lodge, Race.UNIT_PEON));
        int count = player.getUnitCountContainer().getNumSupplies();

        player.trainChampions(lodge, 1, false);
        tick(world, 1);
        assertEquals(2, stock(armory, TreeSupply.class));
        assertEquals(1, stock(armory, IronSupply.class));
        assertEquals(1, stock(armory, RubberSupply.class));
        assertEquals(1, player.getChampionCount(), "the Champion in training does not count");
        tick(world, 28);
        assertEquals(1, lodge.getUnitCount(), "the Champion came out early");
        tick(world, 2);
        assertEquals(0, lodge.getUnitCount(), "the peon is still inside");
        assertEquals(1, champions(player).size());
        assertEquals(1, player.getChampionCount());
        assertEquals(count, player.getUnitCountContainer().getNumSupplies());
        Unit champion = champions(player).getFirst();
        assertTrue(champion.isGearWarrior(), "a Champion throws");
        assertEquals(0f, champion.getWeaponFactory().getRange());

        tick(world, 40);
        assertEquals(1, champions(player).size(), "it trained more than it was asked for");
        assertEquals(2, stock(armory, TreeSupply.class));
    }

    /** Without a peon inside the training waits; without the cost in the Armory it does not start. */
    @Test
    void championTrainingWaits() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        LandBuilding armory = build(player, Race.BUILDING_ARMORY);
        LandBuilding lodge = build(player, Race.BUILDING_LODGE);
        ShelterUnitContainer shelter = container(lodge);
        shelter.enter(unitAt(player, lodge, Race.UNIT_PEON));
        player.trainChampions(lodge, 1, false);
        tick(world, 60);
        assertEquals(0, player.getChampionCount(), "a Champion started without its cost");

        armory.fillSupplies(TreeSupply.class, 2);
        armory.fillSupplies(IronSupply.class, 1);
        armory.fillSupplies(RubberSupply.class, 1);
        tick(world, 10);
        assertEquals(1, player.getChampionCount());
        player.deployUnits(lodge, DeployType.SHELTERED, 1);
        tick(world, 60);
        assertEquals(0, champions(player).size(), "a Champion trained without a peon inside");
        shelter.enter(unitAt(player, lodge, Race.UNIT_PEON));
        tick(world, 21);
        assertEquals(1, champions(player).size(), "the training did not go on where it stopped");
    }

    /** At most five Champions alive: sheltered ones count, and a sixth starts only when one dies. */
    @Test
    void atMostFiveChampions() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        LandBuilding armory = build(player, Race.BUILDING_ARMORY);
        armory.fillSupplies(TreeSupply.class, 40);
        armory.fillSupplies(IronSupply.class, 20);
        armory.fillSupplies(RubberSupply.class, 20);
        LandBuilding lodge = build(player, Race.BUILDING_LODGE);
        ShelterUnitContainer shelter = container(lodge);
        List<Unit> alive = new ArrayList<>();
        for (int i = 0; i < 4; i++)
            alive.add(unitAt(player, lodge, Race.UNIT_CHAMPION));
        shelter.enter(unitAt(player, lodge, Race.UNIT_CHAMPION));
        assertEquals(5, player.getChampionCount());
        for (int i = 0; i < 3; i++)
            shelter.enter(unitAt(player, lodge, Race.UNIT_PEON));
        player.trainChampions(lodge, 3, false);
        tick(world, 60);
        assertEquals(5, player.getChampionCount(), "a sixth Champion");
        assertEquals(40, stock(armory, TreeSupply.class), "the cost of a sixth was taken");

        player.killSelection(new Selectable<?>[]{alive.getFirst()});
        tick(world, 32);
        assertEquals(5, player.getChampionCount());
        assertEquals(38, stock(armory, TreeSupply.class));
    }

    /** Only a Lodge takes a Champion in: it has no gear for an Armory, and towers and ships take throwers. */
    @Test
    void championsShelterOnlyInLodges() {
        World world = newWorld(Ruleset.BUFFED);
        Player player = world.getPlayers()[0];
        LandBuilding armory = build(player, Race.BUILDING_ARMORY);
        LandBuilding quarters = build(player, Race.BUILDING_QUARTERS);
        LandBuilding tower = build(player, Race.BUILDING_TOWER);
        LandBuilding lodge = build(player, Race.BUILDING_LODGE);
        Unit champion = unitAt(player, lodge, Race.UNIT_CHAMPION);
        assertTrue(champion.isChampion());
        for (Building building : List.of(armory, quarters, tower))
            assertFalse(champion.canEnter(building), "a Champion went into " + building.getTemplate().getName());
        assertTrue(champion.canEnter(lodge));
    }

    private static @NonNull List<Unit> champions(@NonNull Player player) {
        List<Unit> champions = new ArrayList<>();
        for (Selectable<?> s : player.getUnits().getSet()) {
            if (s instanceof Unit unit && !unit.isDead() && unit.isChampion())
                champions.add(unit);
        }
        return champions;
    }

    /**
     * Normal and Hard AIs build a Great Tower they man with three throwers and a Lodge that trains Champions under
     * Buffed; Easy ones do none of it.
     */
    @Test
    void aiBuildsThemUnderBuffed() {
        for (String name : List.of("buffed-1v1", "buffed-6p")) {
            HeadlessMatchConfig config = Matches.ALL.get(name);
            HeadlessMatchResult result = Matches.play(name);
            for (int i = 0; i < config.players().size(); i++) {
                HeadlessMatchResult.Census census = result.census().get(i);
                if (config.players().get(i).difficulty() == PlayerSlot.AI_EASY) {
                    String player = name + " player " + i + ": " + census;
                    assertFalse(census.completedBuildings().contains(Race.BUILDING_GREAT_TOWER), player);
                    assertFalse(census.completedBuildings().contains(Race.BUILDING_LODGE), player);
                    assertFalse(census.unitTypes().contains(Race.UNIT_CHAMPION), player);
                }
            }
            long towers = result.census().stream().filter(c -> c.greatTowerThrowers() == 3).count();
            long lodges = result.census().stream().filter(c -> c.completedBuildings().contains(Race.BUILDING_LODGE)
                    && c.unitTypes().contains(Race.UNIT_CHAMPION)).count();
            assertTrue(towers > 0 && lodges > 0, name + ": " + result.census());
        }
    }

    /** Under Classic and Resurrected nobody builds them. */
    @ParameterizedTest
    @ValueSource(strings = {"classic-1v1", "classic-6p", "resurrected-1v1", "resurrected-6p"})
    void nobodyBuildsThemOutsideBuffed(String name) {
        for (HeadlessMatchResult.Census census : Matches.play(name).census()) {
            assertFalse(census.completedBuildings().contains(Race.BUILDING_GREAT_TOWER), name + ": " + census);
            assertFalse(census.completedBuildings().contains(Race.BUILDING_LODGE), name + ": " + census);
            assertFalse(census.unitTypes().contains(Race.UNIT_CHAMPION), name + ": " + census);
        }
    }
}
