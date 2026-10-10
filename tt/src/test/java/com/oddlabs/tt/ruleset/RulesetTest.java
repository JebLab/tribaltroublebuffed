package com.oddlabs.tt.ruleset;

import com.oddlabs.matchmaking.Preset;
import com.oddlabs.matchmaking.WorldConfig;
import com.oddlabs.tt.gamemode.PresetLibrary;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.model.IronSupply;
import com.oddlabs.tt.model.LandBuilding;
import com.oddlabs.tt.model.Lodge;
import com.oddlabs.tt.model.RockSupply;
import com.oddlabs.tt.model.RubberSupply;
import com.oddlabs.tt.model.behaviour.RepairBehaviour;
import com.oddlabs.tt.ruleset.RulesetStats.BuildingStats;
import com.oddlabs.tt.ruleset.RulesetStats.ChickenCoopStats;
import com.oddlabs.tt.ruleset.RulesetStats.CountBySize;
import com.oddlabs.tt.ruleset.RulesetStats.CrabStats;
import com.oddlabs.tt.ruleset.RulesetStats.DrumStats;
import com.oddlabs.tt.ruleset.RulesetStats.FaunaStats;
import com.oddlabs.tt.ruleset.RulesetStats.FjordFogStats;
import com.oddlabs.tt.ruleset.RulesetStats.GreatTowerStats;
import com.oddlabs.tt.ruleset.RulesetStats.HammerOfThorStats;
import com.oddlabs.tt.ruleset.RulesetStats.JollyJungleStats;
import com.oddlabs.tt.ruleset.RulesetStats.LodgeStats;
import com.oddlabs.tt.ruleset.RulesetStats.MarketStats;
import com.oddlabs.tt.ruleset.RulesetStats.MonkeyStats;
import com.oddlabs.tt.ruleset.RulesetStats.NetStats;
import com.oddlabs.tt.ruleset.RulesetStats.PalisadeStats;
import com.oddlabs.tt.ruleset.RulesetStats.PoultryPanicStats;
import com.oddlabs.tt.ruleset.RulesetStats.PredatorStats;
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
     * 2004 nor Resurrected has the Chicken Coop, the Totem, the Shield, the Torch, the Market, the Palisade, the Great
     * Tower, the Lodge, the Drum or the Net, nor wild animals, nor the chieftains' third slot.
     */
    @Test
    void classicOffersOnly2004WorldOptions() {
        assertEquals(new RulesetStats.Features(false, false, false, 6, false, false, false, false, false, false, false,
                false,
                false, false, false, false, false),
                Ruleset.CLASSIC.getStats().features());
        assertEquals(new RulesetStats.Features(true, true, true, 12, true, false, false, false, false, false, false,
                false,
                false, false, false, false, false),
                Ruleset.RESURRECTED.getStats().features());
    }

    /** Buffed offers Resurrected's world options and every one of its own buildings, pieces of gear and spells. */
    @Test
    void buffedOffersItsBuildings() {
        assertEquals(new RulesetStats.Features(true, true, true, 12, true, true, true, true, true, true, true, true,
                true, true, true, true, true),
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
                    () -> assertUnit(race.shield_warrior(), 1, 3.5f, .7f, .3f),
                    () -> assertUnit(race.torch_warrior(), 1, 4f, .3f, .5f),
                    () -> assertEquals(new TorchStats(4, 15f, 2f), race.torch(), "torch"));
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

    /**
     * PLAN.md section 4.5, docs/design/market.md and palisade.md: the Market trades 3 for 1 in 20 man-seconds; a
     * Palisade segment has 40 hit points and a Gate 120; a player may have 100 of them. The same for both races.
     */
    @Test
    void buffedMarketAndWallsMatchThePlan() {
        for (boolean vikings : new boolean[]{false, true}) {
            RaceStats race = Ruleset.BUFFED.getStats().race(vikings);
            assertAll(
                    () -> assertEquals(new MarketStats(150, 3, 1, 20f), race.market(), "market"),
                    () -> assertEquals(new PalisadeStats(40, 100), race.palisade(), "palisade"),
                    () -> assertEquals(new BuildingStats(120), race.gate(), "gate"));
        }
    }

    /**
     * PLAN.md sections 4.4 and 4.5, docs/design/great-tower.md and lodge-and-champion.md: the Great Tower (300 hit
     * points, three throwers), the Lodge (200 hit points, 30 units, half the charge time within 30 m, a Champion in 30
     * s, five alive) and the Champion (speed 5, dodge 0.75, hit 0.9). The same for both races.
     */
    @Test
    void buffedGreatTowerLodgeAndChampionMatchThePlan() {
        for (boolean vikings : new boolean[]{false, true}) {
            RaceStats race = Ruleset.BUFFED.getStats().race(vikings);
            assertAll(
                    () -> assertEquals(new GreatTowerStats(300, 10, 3), race.great_tower(), "great_tower"),
                    () -> assertEquals(new LodgeStats(200, 5, 30, 30f, 2f, 30f, 5), race.lodge(), "lodge"),
                    () -> assertUnit(race.champion(), 1, 5f, .75f, .9f));
        }
    }

    /**
     * PLAN.md section 4.4 and docs/design/drum-and-net.md: the Drummer / Hornblower (no attack, dodge 0.5, +0.10 hit
     * and +15 % speed within 12 m) and the Chicken Catcher / Fowler (blow 0.4, dodge 0.3, 3 snares of 4 s). The same
     * for both races.
     */
    @Test
    void buffedDrumAndNetMatchThePlan() {
        for (boolean vikings : new boolean[]{false, true}) {
            RaceStats race = Ruleset.BUFFED.getStats().race(vikings);
            assertAll(
                    () -> assertUnit(race.drum_warrior(), 1, 4f, .5f, 0f),
                    () -> assertUnit(race.net_warrior(), 1, 4f, .3f, .4f),
                    () -> assertEquals(new DrumStats(.1f, .15f, 12f), race.drum(), "drum"),
                    () -> assertEquals(new NetStats(3, 4f), race.net(), "net"));
        }
    }

    /**
     * docs/design/fauna.md: one hit point and dodge 0.3; no animal within 40 m of a start; crabs 2 per 100 m of shore
     * up to 20; monkeys 3 / 5 / 8 / 12 that steal within 4 m at 6 m/s and rest 30 s; boars or wolves 2 / 3 / 5 / 8 that
     * charge a peon within 6 m with no company within 8 m at 6 m/s, strike at 0.5 and rest 20 s, giving up 12 m from
     * home. The same for both races, which hold them for their terrains.
     */
    @Test
    void buffedFaunaMatchesTheDesign() {
        FaunaStats expected = new FaunaStats(40f, .3f, new CrabStats(2f, 20, 3f, 3f),
                new MonkeyStats(new CountBySize(3, 5, 8, 12), 6f, 4f, 12f, 30f),
                new PredatorStats(new CountBySize(2, 3, 5, 8), 6f, 6f, 8f, 12f, .5f, 20f));
        for (boolean vikings : new boolean[]{false, true}) {
            assertEquals(expected, Ruleset.BUFFED.getStats().race(vikings).fauna());
            assertEquals(expected, Ruleset.RESURRECTED.getStats().race(vikings).fauna());
        }
        CountBySize monkeys = expected.monkey().count();
        assertEquals(List.of(3, 5, 8, 12, 12), List.of(monkeys.forMetersPerWorld(256), monkeys.forMetersPerWorld(512),
                monkeys.forMetersPerWorld(1024), monkeys.forMetersPerWorld(2048), monkeys.forMetersPerWorld(4096)));
    }

    /**
     * docs/design/spells.md (PLAN.md section 4.4): the third slot charges in 100 s; Jolly Jungle roots within 20 m for
     * 8 s, Poultry Panic knocks flat within 30 m for 3 s and leaves 5 chickens, Hammer of Thor strikes within 20 m for
     * 40, Fjord Fog lasts 20 s over 30 m and takes 0.2 from enemies' hit chance. Resurrected holds the same numbers
     * with
     * the feature off.
     */
    @Test
    void buffedSpellsMatchThePlan() {
        for (Ruleset ruleset : new Ruleset[]{Ruleset.BUFFED, Ruleset.RESURRECTED}) {
            SpellStats spells = ruleset.getStats().spells();
            assertAll(
                    () -> assertEquals(100f, spells.third_slot_seconds(), "third_slot_seconds"),
                    () -> assertEquals(new JollyJungleStats(20f, 8f), spells.jolly_jungle(), "jolly_jungle"),
                    () -> assertEquals(new PoultryPanicStats(30f, 3f, 5), spells.poultry_panic(), "poultry_panic"),
                    () -> assertEquals(new HammerOfThorStats(20f, 40), spells.hammer_of_thor(), "hammer_of_thor"),
                    () -> assertEquals(new FjordFogStats(30f, 20f, .2f), spells.fjord_fog(), "fjord_fog"));
        }
    }

    /** The Drum's and the Net's recipes: 3 wood + 1 iron, and 2 wood + 1 chicken. */
    @Test
    void drumAndNetCostWhatThePlanSays() {
        assertEquals(List.of(TreeSupply.class, IronSupply.class), List.of(
                LandBuilding.COST_DRUM_WEAPON.getSupplyTypes()));
        assertArrayEquals(new int[]{3, 1}, LandBuilding.COST_DRUM_WEAPON.getSupplyAmounts());
        assertEquals(List.of(TreeSupply.class, RubberSupply.class), List.of(
                LandBuilding.COST_NET_WEAPON.getSupplyTypes()));
        assertArrayEquals(new int[]{2, 1}, LandBuilding.COST_NET_WEAPON.getSupplyAmounts());
    }

    /** The Champion's recipe: 2 wood + 1 iron + 1 chicken, taken from the nearest Armory. */
    @Test
    void championCostsWhatThePlanSays() {
        assertEquals(List.of(TreeSupply.class, IronSupply.class, RubberSupply.class),
                List.of(Lodge.COST_CHAMPION.getSupplyTypes()));
        assertArrayEquals(new int[]{2, 1, 1}, Lodge.COST_CHAMPION.getSupplyAmounts());
    }

    /**
     * The plan's recipes do not add up at 5 hit points a load (R-35): the hit points stand, every load is 5 of them,
     * and the plan's rock or iron is the last of them, so the Great Tower takes 50 logs and 10 rocks and the Lodge 35
     * logs and 5 iron.
     */
    @Test
    void greatTowerAndLodgeLoadsAddUpToTheirHitPoints() {
        RaceStats race = Ruleset.BUFFED.getStats().natives();
        assertEquals(50,
                race.great_tower().hit_points() / RepairBehaviour.REPAIRS_PER_SUPPLY - race.great_tower().rock(),
                "great tower wood");
        assertEquals(35, race.lodge().hit_points() / RepairBehaviour.REPAIRS_PER_SUPPLY - race.lodge().iron(),
                "lodge wood");
    }

    /** A building's hit points are built 5 per log, so the plan's wood cost is its hit points over 5. */
    @Test
    void buffedBuildingsCostWhatThePlanSays() {
        RaceStats race = Ruleset.BUFFED.getStats().natives();
        assertEquals(20, race.chicken_coop().hit_points() / RepairBehaviour.REPAIRS_PER_SUPPLY, "coop wood");
        assertEquals(5,
                (race.totem().hit_points() - race.totem().rock() * RepairBehaviour.REPAIRS_PER_SUPPLY) / RepairBehaviour.REPAIRS_PER_SUPPLY,
                "totem wood");
        assertEquals(30, race.market().hit_points() / RepairBehaviour.REPAIRS_PER_SUPPLY, "market wood");
        assertEquals(8, race.palisade().hit_points() / RepairBehaviour.REPAIRS_PER_SUPPLY, "palisade wood");
        // The plan's "8 wood" is the segment's; the gate keeps 5 hit points a log (R-29).
        assertEquals(24, race.gate().hit_points() / RepairBehaviour.REPAIRS_PER_SUPPLY, "gate wood");
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
