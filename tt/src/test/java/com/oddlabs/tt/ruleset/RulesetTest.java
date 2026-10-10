package com.oddlabs.tt.ruleset;

import com.oddlabs.matchmaking.Preset;
import com.oddlabs.matchmaking.WorldConfig;
import com.oddlabs.tt.gamemode.PresetLibrary;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.model.IronSupply;
import com.oddlabs.tt.model.LandBuilding;
import com.oddlabs.tt.model.RockSupply;
import com.oddlabs.tt.model.behaviour.RepairBehaviour;
import com.oddlabs.tt.ruleset.RulesetStats.BuildingStats;
import com.oddlabs.tt.ruleset.RulesetStats.ChickenCoopStats;
import com.oddlabs.tt.ruleset.RulesetStats.RaceStats;
import com.oddlabs.tt.ruleset.RulesetStats.SpellStats;
import com.oddlabs.tt.ruleset.RulesetStats.TotemStats;
import com.oddlabs.tt.ruleset.RulesetStats.TorchStats;
import com.oddlabs.tt.ruleset.RulesetStats.UnitStats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

    /**
     * 2004 had no boats, Small/Medium/Large islands only, six players (MAX_PLAYERS = 6) and fixed limits. Neither
     * 2004 nor Resurrected has the Chicken Coop, the Totem, the Shield or the Torch.
     */
    @Test
    void classicOffersOnly2004WorldOptions() {
        assertEquals(new RulesetStats.Features(false, false, false, 6, false, false, false, false, false),
                Ruleset.CLASSIC.getStats().features());
        assertEquals(new RulesetStats.Features(true, true, true, 12, true, false, false, false, false),
                Ruleset.RESURRECTED.getStats().features());
    }

    /** Buffed offers Resurrected's world options plus its own buildings and gear. */
    @Test
    void buffedOffersItsBuildings() {
        assertEquals(new RulesetStats.Features(true, true, true, 12, true, true, true, true, true),
                Ruleset.BUFFED.getStats().features());
    }

    /** PLAN.md section 4.5: the Chicken Coop / Henhouse and the Totem / Runestone, the same for both races. */
    @Test
    void buffedBuildingsMatchThePlan() {
        for (boolean vikings : new boolean[]{false, true}) {
            RaceStats race = Ruleset.BUFFED.getStats().race(vikings);
            assertAll(
                    () -> assertEquals(new ChickenCoopStats(100, 2, 90f, 6), race.chicken_coop(), "chicken_coop"),
                    () -> assertEquals(new TotemStats(30, 1, .05f, 10f, 2), race.totem(), "totem"));
        }
    }

    /** PLAN.md section 4.4 and docs/design/gear.md: the Shield and the Torch, the same for both races. */
    @Test
    void buffedGearMatchesThePlan() {
        for (boolean vikings : new boolean[]{false, true}) {
            RaceStats race = Ruleset.BUFFED.getStats().race(vikings);
            assertAll(
                    () -> assertUnit(race.shield_warrior(), 1, 3.5f, .85f, .3f),
                    () -> assertUnit(race.torch_warrior(), 1, 4f, .3f, .5f),
                    () -> assertEquals(new TorchStats(6, 15f, 2f), race.torch(), "torch"));
        }
    }

    /** The gear's recipes and labour: 2 wood + 1 rock in 40 man-seconds, 2 wood + 1 rock + 1 iron in 80. */
    @Test
    void gearCostsWhatThePlanSays() {
        assertEquals(List.of(TreeSupply.class, RockSupply.class),
                List.of(LandBuilding.COST_SHIELD_WEAPON.getSupplyTypes()));
        assertArrayEquals(new int[]{2, 1}, LandBuilding.COST_SHIELD_WEAPON.getSupplyAmounts());
        assertEquals(List.of(TreeSupply.class, RockSupply.class, IronSupply.class),
                List.of(LandBuilding.COST_TORCH_WEAPON.getSupplyTypes()));
        assertArrayEquals(new int[]{2, 1, 1}, LandBuilding.COST_TORCH_WEAPON.getSupplyAmounts());
    }

    /** A building's hit points are built 5 per log, so the plan's wood cost is its hit points over 5. */
    @Test
    void buffedBuildingsCostWhatThePlanSays() {
        RaceStats race = Ruleset.BUFFED.getStats().natives();
        assertEquals(20, race.chicken_coop().hit_points() / RepairBehaviour.REPAIRS_PER_SUPPLY, "coop wood");
        assertEquals(5,
                (race.totem().hit_points() - race.totem().rock() * RepairBehaviour.REPAIRS_PER_SUPPLY) / RepairBehaviour.REPAIRS_PER_SUPPLY,
                "totem wood");
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

    /** Buffed adds buildings and changes none of Resurrected's numbers. */
    @Test
    void buffedKeepsResurrectedNumbers() {
        RulesetStats resurrected = Ruleset.RESURRECTED.getStats();
        RulesetStats buffed = Ruleset.BUFFED.getStats();
        assertEquals(resurrected.spells(), buffed.spells());
        for (boolean vikings : new boolean[]{false, true}) {
            RaceStats r = resurrected.race(vikings);
            RaceStats b = buffed.race(vikings);
            assertAll(
                    () -> assertEquals(r.peon(), b.peon()),
                    () -> assertEquals(r.rock_warrior(), b.rock_warrior()),
                    () -> assertEquals(r.iron_warrior(), b.iron_warrior()),
                    () -> assertEquals(r.chicken_warrior(), b.chicken_warrior()),
                    () -> assertEquals(r.chieftain(), b.chieftain()),
                    () -> assertBuildings(b, 200, 200, 100, 250));
        }
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
