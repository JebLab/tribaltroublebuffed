package com.oddlabs.tt.ruleset;

import com.oddlabs.matchmaking.Preset;
import com.oddlabs.matchmaking.WorldConfig;
import com.oddlabs.tt.gamemode.PresetLibrary;
import com.oddlabs.tt.ruleset.RulesetStats.BuildingStats;
import com.oddlabs.tt.ruleset.RulesetStats.RaceStats;
import com.oddlabs.tt.ruleset.RulesetStats.SpellStats;
import com.oddlabs.tt.ruleset.RulesetStats.UnitStats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the ruleset data files to the numbers they stand for. Float comparisons are exact: a ruleset has to reproduce
 * the original constants bit for bit, or a replay under it would diverge.
 */
final class RulesetTest {

    @ParameterizedTest
    @EnumSource(Ruleset.class)
    void everyRulesetLoads(Ruleset ruleset) {
        assertNotNull(ruleset.getStats());
        assertEquals(ruleset, Ruleset.fromId(ruleset.getId()));
    }

    /** The literals RacesResources passed to its templates and factories at the fork point (commit 4da43e68). */
    @Test
    void resurrectedMatchesTheForkPoint() {
        RulesetStats stats = Ruleset.RESURRECTED.getStats();
        for (boolean vikings : new boolean[]{false, true}) {
            RaceStats race = stats.race(vikings);
            assertAll(
                    () -> assertUnit(race.peon(), 1, 5f, 0f, 1 / 5f),
                    () -> assertUnit(race.rock_warrior(), 1, 4f, .5f, 0.5f),
                    () -> assertUnit(race.iron_warrior(), 1, 4f, .7f, 0.75f),
                    () -> assertUnit(race.chicken_warrior(), 1, 4f, .7f, 0.95f),
                    () -> assertUnit(race.chieftain(), vikings ? 60 : 40, 4f, 0.5f, 3 / 4f),
                    () -> assertBuildings(race, 200, 200, 100, 250));
        }
        assertResurrectedSpells(stats.spells());
    }

    /**
     * The literals of the 2004 RacesResources ({@code git show oddlabs/master:tt/classes/com/oddlabs/tt/model/
     * RacesResources.java}). 2004 had no ships; their hit points are not pinned here.
     */
    @Test
    void classicMatches2004() {
        RulesetStats stats = Ruleset.CLASSIC.getStats();
        for (boolean vikings : new boolean[]{false, true}) {
            RaceStats race = stats.race(vikings);
            assertAll(
                    () -> assertUnit(race.peon(), 1, 5f, 0f, 1 / 5f),
                    () -> assertUnit(race.rock_warrior(), 1, 4f, .5f, 0.5f),
                    () -> assertUnit(race.iron_warrior(), 1, 4f, .7f, 0.75f),
                    () -> assertUnit(race.chicken_warrior(), 1, 4f, .7f, 0.95f),
                    () -> assertUnit(race.chieftain(), vikings ? 60 : 40, 4f, 0.5f, 3 / 4f),
                    () -> assertEquals(new BuildingStats(200), race.quarters(), "quarters"),
                    () -> assertEquals(new BuildingStats(200), race.armory(), "armory"),
                    () -> assertEquals(new BuildingStats(100), race.tower(), "tower"));
        }
        SpellStats spells = stats.spells();
        assertEquals(new RulesetStats.StinkingStewStats(26f, .5f, 2f, 20f, 10), spells.stinking_stew());
        assertEquals(new RulesetStats.CracklingCloudStats(22f, 1f, 8f, 1f, 30), spells.crackling_cloud());
        assertEquals(new RulesetStats.TerrifyingTootStats(36f, 30f, 10f), spells.terrifying_toot());
        assertEquals(new RulesetStats.RavagingRoarStats(36f, 17f, 2f, 150, 30, .8f), spells.ravaging_roar());
    }

    /** 2004's Player hard-coded 20 starting units, 250 units and 20 buildings; the Classic preset must keep them. */
    @Test
    void classicPresetUses2004Limits() {
        PresetLibrary library = new PresetLibrary();
        library.addRulesetPresets();
        Preset classic = library.findById("builtin-classic");
        assertNotNull(classic);
        WorldConfig world = classic.getWorld();
        assertAll(
                () -> assertTrue(classic.isBuiltIn()),
                () -> assertEquals(Ruleset.CLASSIC, Ruleset.fromId(world.getRuleset())),
                () -> assertEquals(20, world.getStartingUnits()),
                () -> assertEquals(250, world.getMaxUnits()),
                () -> assertEquals(20, world.getMaxBuildings()),
                () -> assertFalse(world.isShips()));
        assertTrue(library.all().isEmpty(), "built-in presets must not be saved with the user's");
    }

    @Test
    void buffedIsResurrectedUntilNewContentLands() {
        assertEquals(Ruleset.RESURRECTED.getStats(), Ruleset.BUFFED.getStats());
    }

    @Test
    void unknownFieldIsRejected() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> RulesetLoader.load("test_unknown_field"));
        assertTrue(e.getMessage().contains("hit_ponts"), e.getMessage());
    }

    @Test
    void missingFieldIsRejected() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> RulesetLoader.load("test_missing_field"));
        assertTrue(e.getMessage().contains("Missing creator property 'rock_warrior'"), e.getMessage());
    }

    @Test
    void inheritanceLoopIsRejected() {
        assertThrows(IllegalStateException.class, () -> RulesetLoader.load("test_loop_a"));
    }

    static void assertUnit(UnitStats unit, int hit_points, float speed, float defense_chance, float hit_chance) {
        assertAll(
                () -> assertEquals(hit_points, unit.hit_points(), "hit_points"),
                () -> assertEquals(speed, unit.speed(), "speed"),
                () -> assertEquals(defense_chance, unit.defense_chance(), "defense_chance"),
                () -> assertEquals(hit_chance, unit.hit_chance(), "hit_chance"));
    }

    static void assertBuildings(RaceStats race, int quarters, int armory, int tower, int ship) {
        assertAll(
                () -> assertEquals(new BuildingStats(quarters), race.quarters(), "quarters"),
                () -> assertEquals(new BuildingStats(armory), race.armory(), "armory"),
                () -> assertEquals(new BuildingStats(tower), race.tower(), "tower"),
                () -> assertEquals(new BuildingStats(ship), race.ship(), "ship"));
    }

    static void assertResurrectedSpells(SpellStats spells) {
        assertEquals(new RulesetStats.StinkingStewStats(26f, .5f, 2f, 20f, 10), spells.stinking_stew());
        assertEquals(new RulesetStats.CracklingCloudStats(22f, 1f, 8f, 1f, 30), spells.crackling_cloud());
        assertEquals(new RulesetStats.TerrifyingTootStats(36f, 30f, 10f), spells.terrifying_toot());
        assertEquals(new RulesetStats.RavagingRoarStats(36f, 17f, 2f, 150, 30, .8f), spells.ravaging_roar());
    }
}
