package com.oddlabs.tt.headless;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.headless.HeadlessMatchConfig.PlayerConfig;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.model.Action;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.LandBuilding;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.RubberSupply;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.behaviour.CastController;
import com.oddlabs.tt.model.behaviour.HuntController;
import com.oddlabs.tt.model.behaviour.StunController;
import com.oddlabs.tt.model.weapon.FjordFog;
import com.oddlabs.tt.model.weapon.LightningCloudFactory;
import com.oddlabs.tt.model.weapon.PoisonFogFactory;
import com.oddlabs.tt.model.weapon.SonicBlastFactory;
import com.oddlabs.tt.model.weapon.StunFactory;
import com.oddlabs.tt.model.weapon.TargetedMagicFactory;
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

import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Buffed's third-slot chieftain spells (M10, docs/design/spells.md): only Buffed has them; they share a 100 s charge
 * that any cast empties and a Lodge halves; Jolly Jungle roots enemies (who still fight), Poultry Panic knocks them
 * flat and leaves five chickens, Hammer of Thor strikes one chosen enemy, Fjord Fog takes hit chance from enemies in
 * it; the Hard AI casts them. Played on headless worlds without AIs unless a test says otherwise, away from both
 * starts so that only the units a test makes are near.
 */
final class BuffedSpellsTest {
    private static final int TICKS_PER_SECOND = Math.round(1 / AnimationManager.ANIMATION_SECONDS_PER_TICK);
    private static final int JUNGLE = RacesResources.INDEX_MAGIC_JUNGLE;
    private static final int PANIC = RacesResources.INDEX_MAGIC_PANIC;
    private static final int HAMMER = RacesResources.INDEX_MAGIC_HAMMER;
    private static final int FOG = RacesResources.INDEX_MAGIC_FOG;

    private static @NonNull World newWorld(@NonNull Ruleset ruleset) {
        HeadlessMatchRunner.setUp();
        HeadlessMatchConfig config = new HeadlessMatchConfig(ruleset, Landscape.TerrainType.NATIVE, Game.SIZE_MEDIUM,
                .5f, .5f, .5f, 7, List.of(new PlayerConfig(0, RacesResources.RACE_NATIVES, PlayerSlot.AI_HARD),
                        new PlayerConfig(1, RacesResources.RACE_VIKINGS, PlayerSlot.AI_HARD)),
                HeadlessMatchConfig.DEFAULT_MAX_TICKS);
        return HeadlessMatchRunner.newWorld(config, false);
    }

    private static void tick(@NonNull World world, float seconds) {
        for (int i = 0; i < seconds * TICKS_PER_SECOND; i++)
            world.tick(AnimationManager.ANIMATION_SECONDS_PER_TICK);
    }

    private static boolean tickUntil(@NonNull World world, float seconds, @NonNull BooleanSupplier condition) {
        for (int i = 0; i < seconds * TICKS_PER_SECOND; i++) {
            if (condition.getAsBoolean())
                return true;
            world.tick(AnimationManager.ANIMATION_SECONDS_PER_TICK);
        }
        return condition.getAsBoolean();
    }

    /** The middle of the line between the two starts: far from both, so no starting unit takes part. */
    private static @NonNull Target middle(@NonNull World world) {
        Player a = world.getPlayers()[0];
        Player b = world.getPlayers()[1];
        int x = UnitGrid.toGridCoordinate((a.getStartX() + b.getStartX()) / 2);
        int y = UnitGrid.toGridCoordinate((a.getStartY() + b.getStartY()) / 2);
        return world.getUnitGrid().findGridTargets(x, y, 1, false)[0];
    }

    private static @NonNull Unit unit(@NonNull Player player, @NonNull Target at, float dx, float dy, int template) {
        return new Unit(player, at.getPositionX() + dx, at.getPositionY() + dy, null,
                player.getRace().getUnitTemplate(template));
    }

    private static @NonNull Unit chieftain(@NonNull Player player, @NonNull Target at) {
        Unit chieftain = unit(player, at, 0f, 0f, Race.UNIT_CHIEFTAIN);
        player.setActiveChieftain(chieftain);
        return chieftain;
    }

    private static void charge(@NonNull Unit chieftain) {
        for (int magic = 0; magic < RacesResources.NUM_MAGIC; magic++)
            chieftain.increaseMagicEnergy(magic, 1000);
    }

    private static float distance(@NonNull Selectable<?> a, @NonNull Selectable<?> b) {
        float dx = a.getPositionX() - b.getPositionX();
        float dy = a.getPositionY() - b.getPositionY();
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /** Sends a unit walking far away, without stopping to fight on the way. */
    private static void walkAway(@NonNull Unit unit, float dx, float dy) {
        int x = UnitGrid.toGridCoordinate(unit.getPositionX() + dx);
        int y = UnitGrid.toGridCoordinate(unit.getPositionY() + dy);
        unit.getOwner().setLandscapeTarget(new Selectable<?>[]{unit}, x, y, Action.MOVE, false);
    }

    @ParameterizedTest
    @ValueSource(strings = {"classic", "resurrected"})
    void onlyBuffedHasTheThirdSlot(String id) {
        World world = newWorld(Ruleset.fromId(id));
        Player natives = world.getPlayers()[0];
        assertTrue(natives.canDoMagic(0) && natives.canDoMagic(1), "the 2004 spells");
        assertFalse(natives.canDoMagic(JUNGLE) || natives.canDoMagic(PANIC), id + " offers the third slot");
        Unit chieftain = chieftain(natives, middle(world));
        charge(chieftain);
        assertFalse(chieftain.canDoMagic(JUNGLE));
        chieftain.doMagic(JUNGLE, true);
        assertEquals(0, natives.getMagics(), "a spell the ruleset does not have was cast");
        assertEquals(1f, chieftain.getMagicProgress(0), "a refused cast emptied the charges");

        Player buffed = newWorld(Ruleset.BUFFED).getPlayers()[1];
        assertTrue(buffed.canDoMagic(HAMMER) && buffed.canDoMagic(FOG), "Buffed lacks the third slot");
    }

    /** The 2004 spells keep their places, factories and charge times; the new ones come after them. */
    @Test
    void theOldSpellsKeepTheirSlots() {
        World world = newWorld(Ruleset.BUFFED);
        Player natives = world.getPlayers()[0];
        Player vikings = world.getPlayers()[1];
        assertEquals(4, RacesResources.NUM_MAGIC);
        assertInstanceOf(PoisonFogFactory.class, natives.getRace().getMagicFactory(RacesResources.INDEX_MAGIC_POISON));
        assertInstanceOf(LightningCloudFactory.class, natives.getRace().getMagicFactory(
                RacesResources.INDEX_MAGIC_LIGHTNING));
        assertInstanceOf(StunFactory.class, vikings.getRace().getMagicFactory(RacesResources.INDEX_MAGIC_STUN));
        assertInstanceOf(SonicBlastFactory.class, vikings.getRace().getMagicFactory(RacesResources.INDEX_MAGIC_BLAST));
        Unit chieftain = chieftain(natives, middle(world));
        tick(world, 41);
        assertTrue(chieftain.canDoMagic(0), "40 s");
        assertFalse(chieftain.canDoMagic(1));
        tick(world, 30);
        assertTrue(chieftain.canDoMagic(1), "70 s");
    }

    /**
     * Both new spells charge in 100 s, together; any cast, old or new, empties all four charges; a Lodge of the
     * chieftain's team nearby halves the time.
     */
    @Test
    void theThirdSlotChargesIn100Seconds() {
        World world = newWorld(Ruleset.BUFFED);
        Player natives = world.getPlayers()[0];
        Unit chieftain = chieftain(natives, middle(world));
        tick(world, 99);
        assertFalse(chieftain.canDoMagic(JUNGLE) || chieftain.canDoMagic(PANIC), "charged before 100 s");
        assertEquals(.99f, chieftain.getMagicProgress(PANIC), .01f);
        tick(world, 2);
        assertTrue(chieftain.canDoMagic(JUNGLE) && chieftain.canDoMagic(PANIC), "not charged after 100 s");
        chieftain.doMagic(RacesResources.INDEX_MAGIC_LIGHTNING, true);
        for (int magic = 0; magic < RacesResources.NUM_MAGIC; magic++)
            assertEquals(0f, chieftain.getMagicProgress(magic), "a cast left charge " + magic);
        assertEquals(1, natives.getMagics(RacesResources.INDEX_MAGIC_LIGHTNING));

        LandBuilding lodge = (LandBuilding) natives.buildBuilding(Race.BUILDING_LODGE,
                UnitGrid.toGridCoordinate(natives.getStartX()), UnitGrid.toGridCoordinate(natives.getStartY()));
        assertNotNull(lodge, "no room for a Lodge");
        Target by_lodge = world.getUnitGrid().findGridTargets(lodge.getGridX(), lodge.getGridY(), 1, false)[0];
        Unit near = chieftain(natives, by_lodge);
        tick(world, 51);
        assertTrue(near.canDoMagic(JUNGLE), "not charged after 50 s by the Lodge");
    }

    /**
     * Jolly Jungle: enemy units within 20 m stand still for 8 s and then walk on; the caster's own units and enemies
     * farther away walk; a rooted unit still attacks what is in its reach.
     */
    @Test
    void jollyJungleRootsEnemiesNearby() {
        World world = newWorld(Ruleset.BUFFED);
        Player natives = world.getPlayers()[0];
        Player vikings = world.getPlayers()[1];
        Target at = middle(world);
        Unit chieftain = chieftain(natives, at);
        charge(chieftain);
        Unit near = unit(vikings, at, 12f, 0f, Race.UNIT_PEON);
        Unit far = unit(vikings, at, -26f, 0f, Race.UNIT_PEON);
        Unit friend = unit(natives, at, 0f, 12f, Race.UNIT_PEON);
        assertTrue(distance(chieftain, near) < 20f && distance(chieftain, far) > 20f, "test setup");
        chieftain.doMagic(JUNGLE, true);
        assertTrue(tickUntil(world, 6f, near::isRooted), "the enemy within 20 m was not rooted");
        assertFalse(far.isRooted(), "an enemy beyond 20 m was rooted");
        assertFalse(friend.isRooted(), "the caster's own unit was rooted");
        // The chieftain has done its part; keep it from hunting the rooted peon.
        natives.killSelection(new Selectable<?>[]{chieftain});

        int x = near.getGridX();
        int y = near.getGridY();
        int friend_y = friend.getGridY();
        walkAway(near, 0f, 40f);
        walkAway(far, 0f, -40f);
        walkAway(friend, 0f, 40f);
        tick(world, 6f);
        assertEquals(x, near.getGridX(), "a rooted unit walked");
        assertEquals(y, near.getGridY(), "a rooted unit walked");
        assertTrue(near.isRooted());
        assertTrue(friend.getGridY() != friend_y, "the caster's own unit stood");
        assertTrue(tickUntil(world, 4f, () -> near.getGridY() != y), "the unit did not walk on after 8 s");
        assertFalse(near.isRooted());

        // Still fighting: a rooted warrior sent at an enemy in its reach attacks it from where it stands.
        Unit warrior = unit(vikings, at, 0f, -20f, Race.UNIT_WARRIOR_ROCK);
        Unit target = unit(natives, at, 0f, -24f, Race.UNIT_PEON);
        warrior.root(8f);
        vikings.setTarget(new Selectable<?>[]{warrior}, target, Action.ATTACK, true);
        assertTrue(tickUntil(world, 5f, () -> target.isDead() || warrior.isDead()
                || warrior.getCurrentController() instanceof HuntController
                        && warrior.getAnimation() == Unit.Animation.THROWING), "a rooted warrior did not throw");
    }

    /**
     * Poultry Panic: enemy units within 30 m are stunned for 3 s (not the caster's own, not those beyond 30 m), and
     * five chickens land around the chieftain that anyone may catch.
     */
    @Test
    void poultryPanicKnocksEnemiesFlatAndLeavesChickens() {
        World world = newWorld(Ruleset.BUFFED);
        Player natives = world.getPlayers()[0];
        Player vikings = world.getPlayers()[1];
        Target at = middle(world);
        int chickens_before = chickensAround(world, at, 12);
        Unit chieftain = chieftain(natives, at);
        charge(chieftain);
        Unit near = unit(vikings, at, 24f, 0f, Race.UNIT_WARRIOR_ROCK);
        Unit far = unit(vikings, at, -36f, 0f, Race.UNIT_PEON);
        Unit friend = unit(natives, at, 0f, 10f, Race.UNIT_PEON);
        assertTrue(distance(chieftain, near) < 30f && distance(chieftain, far) > 30f, "test setup");
        chieftain.doMagic(PANIC, true);
        assertTrue(tickUntil(world, 6f, () -> near.getCurrentController() instanceof StunController),
                "the enemy within 30 m was not knocked flat");
        assertFalse(far.getCurrentController() instanceof StunController, "an enemy beyond 30 m was knocked flat");
        assertFalse(friend.getCurrentController() instanceof StunController, "the caster's own unit was knocked flat");
        assertEquals(0f, near.getDefenseChance(), "a stunned unit dodged");
        assertTrue(tickUntil(world, 3.5f, () -> !(near.getCurrentController() instanceof StunController)),
                "the stun lasted beyond 3 s");
        tick(world, 2f);
        assertEquals(5, chickensAround(world, at, 12) - chickens_before, "chickens left around the chieftain");
    }

    private static int chickensAround(@NonNull World world, @NonNull Target at, int cells) {
        int count = 0;
        UnitGrid grid = world.getUnitGrid();
        for (int x = at.getGridX() - cells; x <= at.getGridX() + cells; x++) {
            for (int y = at.getGridY() - cells; y <= at.getGridY() + cells; y++) {
                if (grid.getOccupant(x, y) instanceof RubberSupply)
                    count++;
            }
        }
        return count;
    }

    /**
     * Hammer of Thor: sent at an enemy out of range, the chieftain walks within 20 m and one bolt kills it; at a
     * building it takes 40 hit points; other units in the order attack the target; nothing is cast without a charge
     * or at a friend.
     */
    @Test
    void hammerOfThorStrikesItsTarget() {
        World world = newWorld(Ruleset.BUFFED);
        Player natives = world.getPlayers()[0];
        Player vikings = world.getPlayers()[1];
        Target at = middle(world);
        Unit chieftain = chieftain(vikings, at);
        Unit friend = unit(vikings, at, 0f, 8f, Race.UNIT_PEON);
        vikings.setTarget(new Selectable<?>[]{chieftain}, friend, Action.THOR, false);
        assertFalse(chieftain.getCurrentController() instanceof CastController, "an order without a charge");
        charge(chieftain);
        vikings.setTarget(new Selectable<?>[]{chieftain}, friend, Action.THOR, false);
        assertFalse(chieftain.getCurrentController() instanceof CastController, "an order at a friend");

        // 32 m towards the Natives' start: over land, beyond the Hammer's reach.
        float dx = natives.getStartX() - at.getPositionX();
        float dy = natives.getStartY() - at.getPositionY();
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        Unit enemy = unit(natives, at, 32f * dx / length, 32f * dy / length, Race.UNIT_WARRIOR_IRON);
        assertTrue(distance(chieftain, enemy) > 26f, "test setup: the enemy in reach");
        Unit warrior = unit(vikings, at, 0f, -6f, Race.UNIT_WARRIOR_ROCK);
        vikings.setTarget(new Selectable<?>[]{chieftain, warrior}, enemy, Action.THOR, false);
        assertInstanceOf(CastController.class, chieftain.getCurrentController());
        assertInstanceOf(HuntController.class, warrior.getCurrentController(), "the rest of the order attacks");
        assertTrue(tickUntil(world, 30f, () -> vikings.getMagics(HAMMER) == 1), "the Hammer was never cast");
        assertTrue(distance(chieftain, enemy) < 24f, "cast from beyond its range: " + distance(chieftain, enemy));
        assertTrue(tickUntil(world, 3f, enemy::isDead), "the bolt did not kill");
        for (int magic = 0; magic < RacesResources.NUM_MAGIC; magic++)
            assertEquals(0f, chieftain.getMagicProgress(magic), .1f, "the cast left charge " + magic);

        Building quarters = natives.buildBuilding(Race.BUILDING_QUARTERS,
                UnitGrid.toGridCoordinate(natives.getStartX()), UnitGrid.toGridCoordinate(natives.getStartY()));
        assertNotNull(quarters);
        Unit striker = chieftain(vikings, world.getUnitGrid().findGridTargets(quarters.getGridX() + 8,
                quarters.getGridY(), 1, false)[0]);
        charge(striker);
        int hit_points = quarters.getHitPoints();
        striker.doMagicAt(HAMMER, quarters);
        assertTrue(tickUntil(world, 5f, () -> quarters.getHitPoints() < hit_points), "the bolt missed the building");
        assertEquals(hit_points - 40, quarters.getHitPoints());
    }

    /**
     * Fjord Fog: for 20 s, enemies attacking from within 30 m of where it was cast lose 0.2 of their hit chance; the
     * caster's own units and enemies outside do not; two fogs do not add up; then it is gone.
     */
    @Test
    void fjordFogLowersEnemyHitChance() {
        World world = newWorld(Ruleset.BUFFED);
        Player natives = world.getPlayers()[0];
        Player vikings = world.getPlayers()[1];
        Target at = middle(world);
        Unit chieftain = chieftain(vikings, at);
        charge(chieftain);
        Unit enemy = unit(natives, at, 20f, 0f, Race.UNIT_PEON);
        Unit outside = unit(natives, at, -36f, 0f, Race.UNIT_PEON);
        Unit friend = unit(vikings, at, 0f, 10f, Race.UNIT_PEON);
        assertEquals(0f, FjordFog.getHitPenalty(enemy), "a world without fog");
        chieftain.doMagic(FOG, true);
        assertTrue(tickUntil(world, 6f, () -> !world.getMists().isEmpty()), "no fog");
        assertEquals(.2f, FjordFog.getHitPenalty(enemy));
        assertEquals(0f, FjordFog.getHitPenalty(outside), "an enemy outside the fog");
        assertEquals(0f, FjordFog.getHitPenalty(friend), "the caster's own unit");

        Unit second = chieftain(vikings, world.getUnitGrid().findGridTargets(at.getGridX(), at.getGridY() - 4, 1,
                false)[0]);
        charge(second);
        second.doMagic(FOG, true);
        assertTrue(tickUntil(world, 6f, () -> world.getMists().size() == 2), "no second fog");
        assertEquals(.2f, FjordFog.getHitPenalty(enemy), "two fogs added up");
        assertTrue(tickUntil(world, 20f, () -> world.getMists().isEmpty()), "the fog outlasted 20 s");
        assertEquals(0f, FjordFog.getHitPenalty(enemy));
    }

    /**
     * The Hard AIs cast each of the four new spells in the Buffed matches (the Natives' and the Vikings' third slot,
     * spells 2 and 3 of each race); no AI has them under Classic or Resurrected.
     */
    @Test
    void theHardAiCastsTheNewSpells() {
        int[][] cast = new int[2][RacesResources.NUM_MAGIC];
        for (String name : List.of("buffed-1v1", "buffed-6p")) {
            HeadlessMatchConfig config = Matches.ALL.get(name);
            List<HeadlessMatchResult.Census> census = Matches.play(name).census();
            for (int player = 0; player < census.size(); player++) {
                int race = config.players().get(player).race();
                for (int magic = 0; magic < RacesResources.NUM_MAGIC; magic++)
                    cast[race][magic] += census.get(player).spellsCast().get(magic);
            }
        }
        String report = "natives " + Arrays.toString(
                cast[RacesResources.RACE_NATIVES]) + ", vikings " + Arrays.toString(cast[RacesResources.RACE_VIKINGS]);
        assertTrue(cast[RacesResources.RACE_NATIVES][JUNGLE] > 0, "no Jolly Jungle: " + report);
        assertTrue(cast[RacesResources.RACE_NATIVES][PANIC] > 0, "no Poultry Panic: " + report);
        assertTrue(cast[RacesResources.RACE_VIKINGS][HAMMER] > 0, "no Hammer of Thor: " + report);
        assertTrue(cast[RacesResources.RACE_VIKINGS][FOG] > 0, "no Fjord Fog: " + report);
        for (String name : List.of("classic-1v1", "resurrected-1v1")) {
            for (HeadlessMatchResult.Census census : Matches.play(name).census()) {
                List<Integer> spells = census.spellsCast();
                assertEquals(0, spells.get(2) + spells.get(3), name + ": " + spells);
            }
        }
    }

    /** A chieftain of a race whose spells are all cast around it ignores a Hammer order (the Natives). */
    @Test
    void onlyTheHammerIsAimed() {
        World world = newWorld(Ruleset.BUFFED);
        Player natives = world.getPlayers()[0];
        Player vikings = world.getPlayers()[1];
        Target at = middle(world);
        Unit chieftain = chieftain(natives, at);
        charge(chieftain);
        assertEquals(-1, chieftain.getTargetedMagicIndex());
        Unit enemy = unit(vikings, at, 10f, 0f, Race.UNIT_PEON);
        natives.setTarget(new Selectable<?>[]{chieftain}, enemy, Action.THOR, false);
        assertFalse(chieftain.getCurrentController() instanceof CastController);
        assertInstanceOf(TargetedMagicFactory.class, vikings.getRace().getMagicFactory(HAMMER));
        Unit viking_chieftain = chieftain(vikings, world.getUnitGrid().findGridTargets(at.getGridX(),
                at.getGridY() + 6, 1, false)[0]);
        charge(viking_chieftain);
        viking_chieftain.doMagic(HAMMER, true);
        assertEquals(0, vikings.getMagics(), "the Hammer was cast without a target");
    }
}
