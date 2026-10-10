package com.oddlabs.tt.ruleset;

import org.jspecify.annotations.NonNull;

/**
 * Every gameplay number a {@link Ruleset} sets, read from its data file. Component names are the JSON keys. Numbers
 * that are tied to the models (animation timings, attachment offsets, footprints, selection shapes) stay in
 * {@code RacesResources}.
 */
public record RulesetStats(@NonNull Features features, @NonNull RaceStats natives, @NonNull RaceStats vikings,
                           @NonNull SpellStats spells) {

    public @NonNull RaceStats race(boolean vikings) {
        return vikings ? this.vikings : this.natives;
    }

    /**
     * Which world options the skirmish menu offers under this ruleset. Classic turns off everything the 2004 game did
     * not have.
     *
     * @param ships             boats (the Advanced... checkbox)
     * @param enormous_islands  the 2048 m island size
     * @param archipelago       the Archipelago island size (always plays with boats)
     * @param max_players       player slots, 6 to 12
     * @param adjustable_limits the starting units, unit and building limits under Advanced...; when false the game
     *                          uses the 2004 values (20, 250, 20)
     * @param chicken_coop      peons can build the Chicken Coop / Henhouse
     * @param totem             peons can build the Totem / Runestone
     * @param shield            the Armory makes Shields (the Bark-Shield Bearer / Round-Shield Carl)
     * @param torch             the Armory makes Torches (the Firebrand / Torchbearer)
     * @param market            peons can build the Trading Post / Market
     * @param palisade          peons can build Palisade segments and Gates
     * @param great_tower       peons can build the Great Tower
     * @param lodge             peons can build the Spirit Lodge / Mead Hall, which trains the Champion
     * @param drum              the Armory makes Drums and Horns (the Drummer / Hornblower)
     * @param net               the Armory makes Nets (the Chicken Catcher / Fowler), which lay snares
     * @param fauna             wild animals live on the island (crabs, monkeys, boars or wolves; docs/design/fauna.md)
     * @param new_spells        chieftains have the third slot's spells: Jolly Jungle and Poultry Panic, Hammer of
     *                          Thor and Fjord Fog (docs/design/spells.md)
     */
    public record Features(boolean ships, boolean enormous_islands, boolean archipelago, int max_players,
                           boolean adjustable_limits, boolean chicken_coop, boolean totem, boolean shield,
                           boolean torch, boolean market, boolean palisade, boolean great_tower, boolean lodge,
                           boolean drum, boolean net, boolean fauna, boolean new_spells) {
    }

    /**
     * One race's numbers. A building or unit that a ruleset does not offer still has its numbers here (the base file
     * must be complete); its {@link Features} flag keeps it out of the game. The shield, torch and net warriors fight
     * hand to hand, and so does the Champion: their {@code hit_chance} is that of a blow. The drum warrior never
     * attacks.
     */
    public record RaceStats(@NonNull UnitStats peon, @NonNull UnitStats rock_warrior, @NonNull UnitStats iron_warrior,
                            @NonNull UnitStats chicken_warrior, @NonNull UnitStats shield_warrior,
                            @NonNull UnitStats torch_warrior, @NonNull UnitStats chieftain,
                            @NonNull BuildingStats quarters, @NonNull BuildingStats armory,
                            @NonNull BuildingStats tower, @NonNull BuildingStats ship,
                            @NonNull ChickenCoopStats chicken_coop, @NonNull TotemStats totem,
                            @NonNull TorchStats torch, @NonNull MarketStats market,
                            @NonNull PalisadeStats palisade, @NonNull BuildingStats gate,
                            @NonNull UnitStats champion, @NonNull GreatTowerStats great_tower,
                            @NonNull LodgeStats lodge, @NonNull UnitStats drum_warrior,
                            @NonNull UnitStats net_warrior, @NonNull DrumStats drum, @NonNull NetStats net,
                            @NonNull FaunaStats fauna) {
    }

    /**
     * @param hit_points     1 for everything but the chieftain: any hit kills
     * @param speed          meters per second
     * @param defense_chance chance that a hit aimed at the unit misses (dodge)
     * @param hit_chance     base chance that the unit's own attack hits, before the target's defense chance
     */
    public record UnitStats(int hit_points, float speed, float defense_chance, float hit_chance) {
    }

    public record BuildingStats(int hit_points) {
    }

    /**
     * Breeds catchable chickens once peons have brought it its first chickens. Built from wood like every building
     * (5 hit points per log).
     *
     * @param stock         chickens peons must bring to the finished coop before it breeds
     * @param spawn_seconds seconds between two new chickens, counted while fewer than {@code max_chickens} roam
     * @param max_chickens  chickens of one coop alive at a time; they roam near it and anyone may catch them
     */
    public record ChickenCoopStats(int hit_points, int stock, float spawn_seconds, int max_chickens) {
    }

    /**
     * Raises the hit chance of its owner's and allies' units nearby. Built from wood, then finished with rock: each
     * rock is the last 5 of its hit points.
     *
     * @param rock         rocks that finish it, after the wood
     * @param hit_bonus    added to the hit chance of a friendly unit within {@code radius}, per totem
     * @param radius       meters
     * @param max_stacking at most this many totems add up
     */
    public record TotemStats(int hit_points, int rock, float hit_bonus, float radius, int max_stacking) {
    }

    /**
     * What a torch does to a building: its blow always hits and sets the building on fire. Another blow restarts the
     * fire; a peon repairing the building puts it out.
     *
     * @param building_damage hit points a blow takes from a building
     * @param fire_seconds    how long the fire burns
     * @param fire_damage     hit points the fire takes per second
     */
    public record TorchStats(int building_damage, float fire_seconds, float fire_damage) {
    }

    /**
     * Peons inside trade one resource for another with the owner's nearest Armory (docs/design/market.md). Built from
     * wood like every building.
     *
     * @param give    resources a trade takes from the Armory
     * @param get     resources a trade puts back, of the kind asked for
     * @param seconds man-seconds of work a trade takes: the peons inside share it
     */
    public record MarketStats(int hit_points, int give, int get, float seconds) {
    }

    /**
     * A wall segment of one grid cell (docs/design/palisade.md). Segments and Gates count against their own limit,
     * not the building limit.
     *
     * @param max_segments Palisade segments and Gates one player may have at a time
     */
    public record PalisadeStats(int hit_points, int max_segments) {
    }

    /**
     * A tower that several throwers man at once (docs/design/great-tower.md), each with the Tower's range and hit
     * bonus. Built from wood, then finished with rock: every load, log or rock, is 5 of its hit points.
     *
     * @param rock     rocks that finish it, after the wood
     * @param throwers warriors with thrown weapons it holds at once
     */
    public record GreatTowerStats(int hit_points, int rock, int throwers) {
    }

    /**
     * Shelters units, speeds the spells of chieftains nearby and trains the Champion (lodge-and-champion.md in
     * docs/design). Built from wood, then finished with iron: every load is 5 of its hit points.
     *
     * @param iron                iron that finishes it, after the wood
     * @param shelter             units of its owner it holds
     * @param spell_radius        meters within which a chieftain of its team charges faster
     * @param spell_charge_factor how many times faster (2: half the charge time); several Lodges do not add up
     * @param champion_seconds    seconds a Champion trains, while a peon is inside
     * @param max_champions       Champions one player may have alive, sheltered or in training
     */
    public record LodgeStats(int hit_points, int iron, int shelter, float spell_radius, float spell_charge_factor,
                             float champion_seconds, int max_champions) {
    }

    /**
     * The Drum / Horn's aura (docs/design/drum-and-net.md): units of the drummer's team within {@code radius} of it,
     * itself included, hit and walk better. Several drummers do not add up.
     *
     * @param hit_bonus   added to the hit chance, inside it like the Totem's
     * @param speed_bonus the share by which walking speed grows (0.15: 15 % faster)
     * @param radius      meters
     */
    public record DrumStats(float hit_bonus, float speed_bonus, float radius) {
    }

    /**
     * The Net's snares (docs/design/drum-and-net.md): a chicken catcher keeps at most {@code snares} lying at a time;
     * each stuns the first enemy unit that steps on it.
     *
     * @param snares       snares of one catcher lying at a time; laying another takes up its oldest
     * @param stun_seconds how long a snare stuns
     */
    public record NetStats(int snares, float stun_seconds) {
    }

    /**
     * Buffed's wild animals (docs/design/fauna.md). They belong to the terrain, not to a race: a world uses the numbers
     * of the race whose terrain it has (natives: tropical, vikings: northern), and both races hold the same.
     *
     * @param start_clearance meters around every player's start that no animal is placed in
     * @param defense_chance  chance that a hit aimed at an animal misses (dodge); every animal has one hit point
     * @param crab            beach crabs, ambience: no one attacks them
     * @param monkey          monkeys at the forest edges (tropical islands only), which steal a carrier's load
     * @param predator        boars (tropical) and wolves (northern) in deep forest, which attack a lone peon
     */
    public record FaunaStats(float start_clearance, float defense_chance, @NonNull CrabStats crab,
                             @NonNull MonkeyStats monkey, @NonNull PredatorStats predator) {
    }

    /**
     * @param per_100m_shore crabs per 100 meters of shore
     * @param max            crabs on the island at most
     * @param speed          meters per second
     * @param flee_radius    a crab scuttles away from any unit this close (meters)
     */
    public record CrabStats(float per_100m_shore, int max, float speed, float flee_radius) {
    }

    /**
     * @param count        monkeys by island size
     * @param speed        meters per second when it runs for a load and back
     * @param sight        a carrier passing this close (meters) is robbed
     * @param leash        it gives up when the carrier is this far from its tree (meters)
     * @param rest_seconds it waits this long after a theft
     */
    public record MonkeyStats(@NonNull CountBySize count, float speed, float sight, float leash, float rest_seconds) {
    }

    /**
     * @param count          boars or wolves by island size
     * @param speed          meters per second when it charges and goes back
     * @param sight          a lone peon this close (meters) is attacked
     * @param company_radius a peon with another unit of its team this close (meters) is not alone
     * @param leash          it gives up when the peon is this far from its patch (meters)
     * @param hit_chance     chance that its blow hits, before the peon's defense chance
     * @param rest_seconds   it rests this long after a strike, whatever the outcome
     */
    public record PredatorStats(@NonNull CountBySize count, float speed, float sight, float company_radius,
                                float leash, float hit_chance, float rest_seconds) {
    }

    /** A number for each island size: Small (256 m), Medium (512 m), Large (1024 m), Enormous (2048 m). */
    public record CountBySize(int small, int medium, int large, int enormous) {
        /** The count for a world this many meters across (an Archipelago counts as Enormous). */
        public int forMetersPerWorld(int meters_per_world) {
            if (meters_per_world <= 256)
                return small;
            if (meters_per_world <= 512)
                return medium;
            if (meters_per_world <= 1024)
                return large;
            return enormous;
        }
    }

    /**
     * The chieftains' spells, by their in-game names. The third slot's four (Buffed, docs/design/spells.md) share one
     * charge of {@code third_slot_seconds}; the old ones charge in 40 s and 70 s.
     */
    public record SpellStats(@NonNull StinkingStewStats stinking_stew, @NonNull CracklingCloudStats crackling_cloud,
                             @NonNull TerrifyingTootStats terrifying_toot,
                             @NonNull RavagingRoarStats ravaging_roar, float third_slot_seconds,
                             @NonNull JollyJungleStats jolly_jungle, @NonNull PoultryPanicStats poultry_panic,
                             @NonNull HammerOfThorStats hammer_of_thor, @NonNull FjordFogStats fjord_fog) {
    }

    /** Native poison fog: every {@code interval} seconds for {@code seconds}, hits enemies within the radius. */
    public record StinkingStewStats(float radius, float hit_chance, float interval, float seconds, int damage) {
    }

    /** Native lightning cloud: drifts at {@code speed}, striking once per {@code seconds_per_hit}. */
    public record CracklingCloudStats(float seconds, float seconds_per_hit, float speed, float hit_chance, int damage) {
    }

    /** Viking stun: stuns enemies within the radius, longest for the closest. */
    public record TerrifyingTootStats(float radius, float stun_seconds_closest, float stun_seconds_farthest) {
    }

    /** Viking sonic blast: damage and hit chance fall off from the closest to the farthest target. */
    public record RavagingRoarStats(float radius, float hit_chance_closest, float hit_chance_farthest,
                                    int damage_closest, int damage_farthest, float seconds) {
    }

    /** Native vines (Buffed): enemies on the ground within the radius cannot walk for {@code seconds}. */
    public record JollyJungleStats(float radius, float seconds) {
    }

    /**
     * Native chicken stampede (Buffed): enemies on the ground within the radius are stunned for {@code stun_seconds},
     * and {@code chickens} chickens are left that anyone may catch.
     */
    public record PoultryPanicStats(float radius, float stun_seconds, int chickens) {
    }

    /**
     * Viking bolt (Buffed): the chieftain strikes one enemy unit or building within {@code range}, which loses
     * {@code damage} hit points without a roll.
     */
    public record HammerOfThorStats(float range, int damage) {
    }

    /**
     * Viking mist (Buffed): for {@code seconds}, enemies whose throw or blow starts within the radius of where it was
     * cast hit with {@code hit_penalty} less chance.
     */
    public record FjordFogStats(float radius, float seconds, float hit_penalty) {
    }
}
