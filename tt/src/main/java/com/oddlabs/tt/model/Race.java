package com.oddlabs.tt.model;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.gui.RaceIcons;
import com.oddlabs.tt.model.weapon.MagicFactory;
import com.oddlabs.tt.player.ChieftainAI;
import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

public final class Race {
    public static final int BUILDING_QUARTERS = 0;
    public static final int BUILDING_ARMORY = 1;
    public static final int BUILDING_TOWER = 2;
    public static final int BUILDING_SHIP = 3;
    // Buffed only: the ruleset's features decide whether a player may build these (Player.canBuild).
    public static final int BUILDING_CHICKEN_COOP = 4;
    public static final int BUILDING_TOTEM = 5;
    public static final int BUILDING_MARKET = 6;
    public static final int BUILDING_PALISADE = 7;
    public static final int BUILDING_GATE = 8;
    public static final int BUILDING_GREAT_TOWER = 9;
    public static final int BUILDING_LODGE = 10;
    public static final int NUM_BUILDINGS = 11;

    /**
     * Palisade segments and Gates: one cell each, laid out at once, outside the building limit
     * (docs/design/palisade.md).
     */
    public static boolean isWall(int building) {
        return building == BUILDING_PALISADE || building == BUILDING_GATE;
    }

    public static final int UNIT_WARRIOR_ROCK = 0;
    public static final int UNIT_WARRIOR_IRON = 1;
    public static final int UNIT_WARRIOR_RUBBER = 2;
    public static final int UNIT_PEON = 3;
    public static final int UNIT_CHIEFTAIN = 4;
    // Buffed only: the ruleset's features decide whether the Armory makes their gear (Player.canBuildShields, canBuildTorches).
    public static final int UNIT_WARRIOR_SHIELD = 5;
    public static final int UNIT_WARRIOR_TORCH = 6;
    // Buffed only: trained at the Lodge (features.lodge).
    public static final int UNIT_CHAMPION = 7;
    public static final int NUM_UNITS = 8;

    private final @NonNull BuildingTemplate[] buildings = new BuildingTemplate[NUM_BUILDINGS];
    private final @NonNull UnitTemplate[] units = new UnitTemplate[NUM_UNITS];
    private final @NonNull SpriteKey rally_point;
    private final @NonNull RaceIcons icons;
    private final @NonNull Audio attack_notification;
    private final @NonNull Audio building_notification;
    private final @NonNull MagicFactory @NonNull [] magic_factory;
    private final @NonNull ChieftainAI chieftain_ai;
    private final @NonNull String music_path;

    public Race(
            @NonNull BuildingTemplate quarters,
            @NonNull BuildingTemplate armory,
            @NonNull BuildingTemplate tower,
            @NonNull BuildingTemplate ship,
            @NonNull BuildingTemplate chicken_coop,
            @NonNull BuildingTemplate totem,
            @NonNull BuildingTemplate market,
            @NonNull BuildingTemplate palisade,
            @NonNull BuildingTemplate gate,
            @NonNull BuildingTemplate great_tower,
            @NonNull BuildingTemplate lodge,
            @NonNull UnitTemplate warrior_rock,
            @NonNull UnitTemplate warrior_iron,
            @NonNull UnitTemplate warrior_rubber,
            @NonNull UnitTemplate peon,
            @NonNull UnitTemplate chieftain,
            @NonNull UnitTemplate warrior_shield,
            @NonNull UnitTemplate warrior_torch,
            @NonNull UnitTemplate champion,
            @NonNull SpriteKey rally_point,
            @NonNull RaceIcons icons,
            @NonNull Audio attack_notification,
            @NonNull Audio building_notification,
            @NonNull MagicFactory @NonNull [] magic_factory,
            @NonNull ChieftainAI chieftain_ai,
            @NonNull String music_path) {
        buildings[BUILDING_QUARTERS] = quarters;
        buildings[BUILDING_ARMORY] = armory;
        buildings[BUILDING_TOWER] = tower;
        buildings[BUILDING_SHIP] = ship;
        buildings[BUILDING_CHICKEN_COOP] = chicken_coop;
        buildings[BUILDING_TOTEM] = totem;
        buildings[BUILDING_MARKET] = market;
        buildings[BUILDING_PALISADE] = palisade;
        buildings[BUILDING_GATE] = gate;
        buildings[BUILDING_GREAT_TOWER] = great_tower;
        buildings[BUILDING_LODGE] = lodge;
        for (int i = 0; i < buildings.length; i++) {
            assert buildings[i].getTemplateID() == i;
        }
        units[UNIT_WARRIOR_ROCK] = warrior_rock;
        units[UNIT_WARRIOR_IRON] = warrior_iron;
        units[UNIT_WARRIOR_RUBBER] = warrior_rubber;
        units[UNIT_PEON] = peon;
        units[UNIT_CHIEFTAIN] = chieftain;
        units[UNIT_WARRIOR_SHIELD] = warrior_shield;
        units[UNIT_WARRIOR_TORCH] = warrior_torch;
        units[UNIT_CHAMPION] = champion;
        this.rally_point = rally_point;
        this.icons = icons;
        this.attack_notification = attack_notification;
        this.building_notification = building_notification;
        this.magic_factory = magic_factory;
        this.chieftain_ai = chieftain_ai;
        this.music_path = music_path;
    }

    public @NonNull BuildingTemplate getBuildingTemplate(int index) {
        return buildings[index];
    }

    public @NonNull UnitTemplate getUnitTemplate(int index) {
        return units[index];
    }

    public @NonNull SpriteKey getRallyPoint() {
        return rally_point;
    }

    public @NonNull RaceIcons getIcons() {
        return icons;
    }

    public @NonNull Audio getAttackNotificationAudio() {
        return attack_notification;
    }

    public @NonNull Audio getBuildingNotificationAudio() {
        return building_notification;
    }

    public @NonNull MagicFactory getMagicFactory(int i) {
        return magic_factory[i];
    }

    public @NonNull ChieftainAI getChieftainAI() {
        return chieftain_ai;
    }

    public @NonNull String getMusicPath() {
        return music_path;
    }
}
