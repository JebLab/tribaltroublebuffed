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
     */
    public record Features(boolean ships, boolean enormous_islands, boolean archipelago, int max_players,
                           boolean adjustable_limits, boolean chicken_coop, boolean totem, boolean shield,
                           boolean torch) {
    }

    /**
     * One race's numbers. A building or unit that a ruleset does not offer still has its numbers here (the base file
     * must be complete); its {@link Features} flag keeps it out of the game. The shield and torch warriors fight hand
     * to hand: their {@code hit_chance} is that of a blow.
     */
    public record RaceStats(@NonNull UnitStats peon, @NonNull UnitStats rock_warrior, @NonNull UnitStats iron_warrior,
                            @NonNull UnitStats chicken_warrior, @NonNull UnitStats shield_warrior,
                            @NonNull UnitStats torch_warrior, @NonNull UnitStats chieftain,
                            @NonNull BuildingStats quarters, @NonNull BuildingStats armory,
                            @NonNull BuildingStats tower, @NonNull BuildingStats ship,
                            @NonNull ChickenCoopStats chicken_coop, @NonNull TotemStats totem,
                            @NonNull TorchStats torch) {
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

    /** The chieftains' spells, by their in-game names. */
    public record SpellStats(@NonNull StinkingStewStats stinking_stew, @NonNull CracklingCloudStats crackling_cloud,
                             @NonNull TerrifyingTootStats terrifying_toot,
                             @NonNull RavagingRoarStats ravaging_roar) {
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
}
