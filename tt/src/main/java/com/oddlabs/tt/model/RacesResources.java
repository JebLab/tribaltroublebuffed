package com.oddlabs.tt.model;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.audio.AudioFile;
import com.oddlabs.tt.form.ProgressForm;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.Headless;
import com.oddlabs.tt.gui.GUIIcons;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.model.weapon.Champion;
import com.oddlabs.tt.model.weapon.Drum;
import com.oddlabs.tt.model.weapon.GearFactory;
import com.oddlabs.tt.model.weapon.InstantHitFactory;
import com.oddlabs.tt.model.weapon.IronAxeWeapon;
import com.oddlabs.tt.model.weapon.IronSpearWeapon;
import com.oddlabs.tt.model.weapon.LightningCloudFactory;
import com.oddlabs.tt.model.weapon.MagicFactory;
import com.oddlabs.tt.model.weapon.Net;
import com.oddlabs.tt.model.weapon.PoisonFogFactory;
import com.oddlabs.tt.model.weapon.RockAxeWeapon;
import com.oddlabs.tt.model.weapon.RockSpearWeapon;
import com.oddlabs.tt.model.weapon.RubberAxeWeapon;
import com.oddlabs.tt.model.weapon.RubberSpearWeapon;
import com.oddlabs.tt.model.weapon.Shield;
import com.oddlabs.tt.model.weapon.SonicBlastFactory;
import com.oddlabs.tt.model.weapon.StunFactory;
import com.oddlabs.tt.model.weapon.ThrowingFactory;
import com.oddlabs.tt.model.weapon.Torch;
import com.oddlabs.tt.model.weapon.WeaponFactory;
import com.oddlabs.tt.player.NativeChieftainAI;
import com.oddlabs.tt.player.VikingChieftainAI;
import com.oddlabs.tt.procedural.GeneratorDamageSmoke;
import com.oddlabs.tt.procedural.GeneratorHalos;
import com.oddlabs.tt.procedural.GeneratorLightning;
import com.oddlabs.tt.procedural.GeneratorPoison;
import com.oddlabs.tt.procedural.GeneratorSmoke;
import com.oddlabs.tt.render.RenderQueues;
import com.oddlabs.tt.render.ShadowListKey;
import com.oddlabs.tt.render.SpriteKey;
import com.oddlabs.tt.render.Texture;
import com.oddlabs.tt.render.TextureKey;
import com.oddlabs.tt.resource.Resources;
import com.oddlabs.tt.resource.SpriteFile;
import com.oddlabs.tt.resource.TextureFile;
import com.oddlabs.tt.ruleset.RulesetStats;
import com.oddlabs.tt.ruleset.RulesetStats.CracklingCloudStats;
import com.oddlabs.tt.ruleset.RulesetStats.GreatTowerStats;
import com.oddlabs.tt.ruleset.RulesetStats.LodgeStats;
import com.oddlabs.tt.ruleset.RulesetStats.RaceStats;
import com.oddlabs.tt.ruleset.RulesetStats.RavagingRoarStats;
import com.oddlabs.tt.ruleset.RulesetStats.SpellStats;
import com.oddlabs.tt.ruleset.RulesetStats.StinkingStewStats;
import com.oddlabs.tt.ruleset.RulesetStats.TerrifyingTootStats;
import com.oddlabs.tt.ruleset.RulesetStats.UnitStats;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public final class RacesResources {
    public static final int QUARTERS_SIZE = 5;
    public static final int ARMORY_SIZE = 5;
    public static final int TOWER_SIZE = 3;
    public static final int SHIP_SIZE = 12;
    // A building of size n covers (2n - 3) x (2n - 3) grid cells of 2 m: the coop 3 x 3 like the tower, the totem one.
    public static final int CHICKEN_COOP_SIZE = 3;
    public static final int TOTEM_SIZE = 2;
    // The Market covers 3 x 3 cells like the coop; a Palisade segment or a Gate one cell like the totem.
    public static final int MARKET_SIZE = 3;
    public static final int WALL_SIZE = 2;
    // The Great Tower and the Lodge cover 7 x 7 cells like the Quarters and the Armory.
    public static final int GREAT_TOWER_SIZE = 5;
    public static final int LODGE_SIZE = 5;
    public static final int MAX_BUILDING_SIZE = IntStream.of(QUARTERS_SIZE, ARMORY_SIZE,
            TOWER_SIZE, CHICKEN_COOP_SIZE, TOTEM_SIZE, MARKET_SIZE, WALL_SIZE, GREAT_TOWER_SIZE,
            LODGE_SIZE).max().orElseThrow();

    public static final int RACE_NATIVES = 0;
    public static final int RACE_VIKINGS = 1;

    public static final int NUM_MAGIC = 2;
    public static final int INDEX_MAGIC_POISON = 0;
    public static final int INDEX_MAGIC_LIGHTNING = 1;
    public static final int INDEX_MAGIC_STUN = 0;
    public static final int INDEX_MAGIC_BLAST = 1;
    public static final float THROW_RANGE = 6f;

    public static final GeneratorHalos DEFAULT_SHADOW_DESC = new GeneratorHalos(128,
            new float[][]{{0f, 0.75f}, {0.5f, 0f}}, new float[][]{{0.40f, 0f}, {0.41f, 1f}, {0.48f, 1f}, {0.49f, 0f}});

    private static final ResourceBundle bundle = ResourceBundle.getBundle(RacesResources.class.getName());

    private @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private static final String[] race_names = {Utils.getBundleString(bundle, "natives"), Utils.getBundleString(bundle,
            "vikings")
    };
    private static final int MAX_UNIT_RESOURCES = 1;

    private final @NonNull TextureKey[] smoke_textures = new TextureKey[1];
    private final @NonNull TextureKey[] damage_smoke_textures = new TextureKey[1];
    private final @NonNull TextureKey[] poison_textures = new TextureKey[1];
    private final @NonNull TextureKey lightning_texture;
    private final @NonNull TextureKey[] note_textures = new TextureKey[8];
    private final @NonNull TextureKey[] star_textures = new TextureKey[1];
    private final @NonNull Audio @NonNull [] tree_fall_sound;
    private final @NonNull Audio @NonNull [] building_hit_sound;
    private final @NonNull Audio gas_sound;
    private final @NonNull Audio bubbling_sound;
    private final @NonNull Audio lightning_sound;
    private final @NonNull Audio cloud_sound;
    private final @NonNull Audio @NonNull [] stun_sound;
    private final @NonNull Audio @NonNull [] blast_lur_sound;
    private final @NonNull Audio blast_rumble_sound;
    private final @NonNull Audio blast_blast_sound;
    private final @NonNull Audio armory_sound;
    private final @NonNull Audio building_collapse_sound;
    private final Map<@NonNull Class<? extends Supply>, @NonNull Audio[]> harvest_sounds = new HashMap<>();
    private final @NonNull SpriteKey[] wood_fragment_sprites = new SpriteKey[4];
    private final @NonNull SpriteKey[] treasure_sprites = new SpriteKey[6];
    // Buffed's snares (docs/design/drum-and-net.md), per race.
    private final @NonNull SpriteKey[] snare_sprites = new SpriteKey[2];
    private final @NonNull String snare_name;
    private final @NonNull Race @NonNull [] races;

    public static boolean isValidRace(int race) {
        return race == RACE_NATIVES || race == RACE_VIKINGS;
    }

    private static @NonNull BuildingTemplate createBuildingTemplate(
            @NonNull RenderQueues queues,
            int template_id,
            int type,
            @NonNull String built_name,
            float built_selection_radius,
            float built_selection_height,
            @NonNull String halfbuilt_name,
            float halfbuilt_selection_radius,
            float halfbuilt_selection_height,
            @NonNull String start_name,
            float start_selection_radius,
            float start_selection_height,
            float shadow_diameter,
            float ring_thickness,
            int placing_size,
            float smoke_radius,
            float smoke_height,
            int num_fragments,
            int max_hit_points,
            UnitContainerFactory unit_container_factory,
            @NonNull Abilities abilities,
            float @NonNull [] hit_offset_z,
            float mount_offset,
            float no_detail_size,
            float rally_x,
            float rally_y,
            float rally_z,
            float chimney_x,
            float chimney_y,
            float chimney_z,
            boolean is_vikings,
            @NonNull String name) {
        assert hit_offset_z.length == 3;

        final float ring_mid = 0.38f;
        final float fadeout = 0.005f;
        Supplier<Texture[]> building_shadow_desc = new GeneratorHalos(256, new float[][]{{0.15f, 0.5f}, {0.5f, 0f}},
                new float[][]{{ring_mid - ring_thickness / 2 - fadeout, 0f}, {ring_mid - ring_thickness / 2, 1f}, {ring_mid + ring_thickness / 2, 1f}, {ring_mid + ring_thickness / 2 + fadeout, 0f}});
        ShadowListKey shadow_renderer = queues.registerSelectableShadowList(building_shadow_desc);
        SpriteFile building = new SpriteFile(built_name,
                Globals.NO_MIPMAP_CUTOFF,
                true, false, true, false);
        SpriteFile building_halfbuilt = new SpriteFile(halfbuilt_name,
                Globals.NO_MIPMAP_CUTOFF,
                true, false, true, false);
        SpriteFile building_start = new SpriteFile(start_name,
                Globals.NO_MIPMAP_CUTOFF,
                true, false, true, false);
        return new BuildingTemplate(
                template_id,
                type,
                placing_size,
                smoke_radius,
                smoke_height,
                num_fragments,
                shadow_diameter,
                shadow_renderer,
                queues.register(building),
                built_selection_radius,
                built_selection_height,
                queues.register(building_halfbuilt),
                halfbuilt_selection_radius,
                halfbuilt_selection_height,
                queues.register(building_start),
                start_selection_radius,
                start_selection_height,
                max_hit_points,
                unit_container_factory,
                abilities,
                hit_offset_z,
                mount_offset,
                no_detail_size,
                0f,
                rally_x,
                rally_y,
                rally_z,
                chimney_x,
                chimney_y,
                chimney_z,
                is_vikings,
                name);
    }

    private static @NonNull BuildingTemplate createChickenCoopTemplate(@NonNull RenderQueues queues,
            @NonNull String race, int max_hit_points, boolean is_vikings, @NonNull String name) {
        String path = "/geometry/" + race + "/chicken_coop";
        return createBuildingTemplate(
                queues,
                Race.BUILDING_CHICKEN_COOP,
                BuildingTemplate.TYPE_BUILDING,
                path + ".binsprite",
                2.5f, 4.5f,
                path + "_halfbuilt.binsprite",
                2.5f, 3.5f,
                path + "_start.binsprite",
                2.5f, 1f,
                8f, .009f, CHICKEN_COOP_SIZE, 3f, 5f, 20, max_hit_points,
                new EmptyUnitContainerFactory(),
                new Abilities(Abilities.BREED),
                new float[]{0f, 1f, 2f}, 0f, 4f,
                0f, 0f, 0f,
                0f, 0f, 0f,
                is_vikings,
                name);
    }

    /**
     * A Buffed gear warrior: the race's warrior with a shield or a torch. Placeholder models: the warrior's mesh with
     * the prop, on its skeleton and animations, so the blow lands where the warrior's throw would leave the hand.
     */
    private static @NonNull UnitTemplate createGearWarriorTemplate(@NonNull RenderQueues queues, @NonNull String race,
            @NonNull String sprite, @NonNull UnitStats stats, @NonNull WeaponFactory weapon,
            @NonNull ShadowListKey shadow_list, @NonNull Audio death_sound, @NonNull String name, int status_value) {
        return createGearWarriorTemplate(queues, race, sprite, stats, weapon, shadow_list, death_sound, name,
                status_value, Abilities.ATTACK | Abilities.TARGET | Abilities.THROW, null);
    }

    /**
     * @param abilities      the Drum's bearer has no {@link Abilities#ATTACK}
     * @param supply_factory what the unit can carry (the Net's bearer carries a caught chicken), or null
     */
    private static @NonNull UnitTemplate createGearWarriorTemplate(@NonNull RenderQueues queues, @NonNull String race,
            @NonNull String sprite, @NonNull UnitStats stats, @NonNull WeaponFactory weapon,
            @NonNull ShadowListKey shadow_list, @NonNull Audio death_sound, @NonNull String name, int status_value,
            int abilities, @Nullable UnitSupplyContainerFactory supply_factory) {
        SpriteFile sprite_file = new SpriteFile("/geometry/" + race + "/" + sprite + ".binsprite",
                Globals.NO_MIPMAP_CUTOFF, true, true, true, false);
        return new UnitTemplate(.4f,
                1.2f,
                new Abilities(abilities),
                stats.speed(),
                weapon,
                queues.register(sprite_file),
                1.9f,
                shadow_list,
                supply_factory,
                death_sound,
                .25f,
                new float[]{1.2f},
                1f,
                stats.defense_chance(),
                name,
                stats.hit_points(),
                0f, 0f, 2f,
                status_value);
    }

    private static @NonNull BuildingTemplate createMarketTemplate(@NonNull RenderQueues queues, @NonNull String race,
            int max_hit_points, boolean is_vikings, @NonNull String name) {
        String path = "/geometry/" + race + "/market";
        return createBuildingTemplate(
                queues,
                Race.BUILDING_MARKET,
                BuildingTemplate.TYPE_BUILDING,
                path + ".binsprite",
                2.5f, 4.5f,
                path + "_halfbuilt.binsprite",
                2.5f, 3.5f,
                path + "_start.binsprite",
                2.5f, 1f,
                8f, .009f, MARKET_SIZE, 3f, 5f, 20, max_hit_points,
                new WorkerUnitContainerFactory(true),
                new Abilities(Abilities.TRADE | Abilities.RALLY_TO),
                new float[]{0f, 1f, 2f}, 0f, 4f,
                0f, 0f, 0f,
                0f, 0f, 3f,
                is_vikings,
                name);
    }

    /**
     * Numbers of a placeholder building model: selection radius and height of the built, halfbuilt and start stages,
     * shadow, where hits land per stage, where a thrower stands, the rally flag and the chimney.
     */
    private record BuildingModel(float @NonNull [] selection, float shadow_diameter, float ring_thickness,
                                 float @NonNull [] hit_offset_z, float mount_offset, float @NonNull [] rally,
                                 float @NonNull [] chimney) {
    }

    // M8's placeholders, measured on the models: the Great Tower is the race's Tower widened (x 3.1 / 2.5, y 3 / 2.3)
    // and 1.2 times as tall, its throwers on the top's floor; the Lodge is the race's Quarters at 0.9.
    private static final BuildingModel NATIVE_GREAT_TOWER = new BuildingModel(
            new float[]{3f, 17f, 3f, 17f, 4.5f, 2.5f}, 16f, .004f, new float[]{0f, 13.8f, 13.8f}, 15.6f,
            new float[]{2.9f, 0f, 15.6f}, new float[]{0f, 0f, 0f});
    private static final BuildingModel VIKING_GREAT_TOWER = new BuildingModel(
            new float[]{3f, 13f, 4.5f, 8.5f, 6f, 1.2f}, 22f, .001f, new float[]{0f, 2.4f, 9f}, 11.46f,
            new float[]{2.1f, 2f, 11.4f}, new float[]{0f, 0f, 0f});
    private static final BuildingModel NATIVE_LODGE = new BuildingModel(
            new float[]{3.6f, 7.2f, 3.6f, 5.4f, 4.5f, .9f}, 16f, .004f, new float[]{0f, .9f, 2.7f}, 0f,
            new float[]{-1.04f, -.69f, 9.9f}, new float[]{1.53f, 2.15f, 8.5f});
    private static final BuildingModel VIKING_LODGE = new BuildingModel(
            new float[]{3.15f, 6.3f, 3.15f, 5.4f, 4.5f, .9f}, 22f, .001f, new float[]{0f, .9f, 2.7f}, 0f,
            new float[]{3.29f, .23f, 7.2f}, new float[]{-.2f, .25f, 7.8f});

    private static @NonNull BuildingTemplate createModelBuildingTemplate(@NonNull RenderQueues queues,
            int template_id, @NonNull String path, @NonNull BuildingModel model, float smoke_height,
            int max_hit_points, @NonNull UnitContainerFactory unit_container_factory, @NonNull Abilities abilities,
            boolean is_vikings, @NonNull String name) {
        float[] s = model.selection();
        return createBuildingTemplate(
                queues,
                template_id,
                BuildingTemplate.TYPE_BUILDING,
                path + ".binsprite",
                s[0], s[1],
                path + "_halfbuilt.binsprite",
                s[2], s[3],
                path + "_start.binsprite",
                s[4], s[5],
                model.shadow_diameter(), model.ring_thickness(), 5, 6f, smoke_height, 30, max_hit_points,
                unit_container_factory,
                abilities,
                model.hit_offset_z(), model.mount_offset(), 6f,
                model.rally()[0], model.rally()[1], model.rally()[2],
                model.chimney()[0], model.chimney()[1], model.chimney()[2],
                is_vikings,
                name);
    }

    /**
     * The Great Tower (Buffed): a tower for several throwers on the Quarters' footprint. Placeholder models: the race's
     * Tower stages widened and recoloured.
     */
    private static @NonNull BuildingTemplate createGreatTowerTemplate(@NonNull RenderQueues queues,
            @NonNull String race, @NonNull GreatTowerStats stats, boolean is_vikings, @NonNull String name) {
        return createModelBuildingTemplate(queues, Race.BUILDING_GREAT_TOWER, "/geometry/" + race + "/great_tower",
                is_vikings ? VIKING_GREAT_TOWER : NATIVE_GREAT_TOWER, 14f, stats.hit_points(),
                new MountUnitContainerFactory(stats.throwers()),
                new Abilities(Abilities.ATTACK | Abilities.RALLY_TO | Abilities.TARGET), is_vikings, name);
    }

    /**
     * The Spirit Lodge / Mead Hall (Buffed): shelters units, speeds spells and trains Champions. Placeholder models:
     * the
     * race's Quarters stages at 0.9, recoloured.
     */
    private static @NonNull BuildingTemplate createLodgeTemplate(@NonNull RenderQueues queues, @NonNull String race,
            @NonNull LodgeStats stats, boolean is_vikings, @NonNull String name) {
        return createModelBuildingTemplate(queues, Race.BUILDING_LODGE, "/geometry/" + race + "/lodge",
                is_vikings ? VIKING_LODGE : NATIVE_LODGE, 9f, stats.hit_points(),
                new ShelterUnitContainerFactory(stats.shelter()),
                new Abilities(Abilities.SHELTER | Abilities.RALLY_TO | Abilities.TARGET), is_vikings, name);
    }

    /** A Palisade segment or a Gate: one cell, no job, nobody inside. */
    private static @NonNull BuildingTemplate createWallTemplate(@NonNull RenderQueues queues, @NonNull String race,
            int template_id, @NonNull String sprite, float height, int max_hit_points, boolean is_vikings,
            @NonNull String name) {
        String path = "/geometry/" + race + "/" + sprite;
        return createBuildingTemplate(
                queues,
                template_id,
                BuildingTemplate.TYPE_BUILDING,
                path + ".binsprite",
                1.4f, height,
                path + "_halfbuilt.binsprite",
                1.4f, height / 2,
                path + "_start.binsprite",
                1.4f, .6f,
                3f, .04f, WALL_SIZE, 1f, 3f, 6, max_hit_points,
                new EmptyUnitContainerFactory(),
                new Abilities(Abilities.NONE),
                new float[]{0f, .7f, 1.5f}, 0f, 2f,
                0f, 0f, 0f,
                0f, 0f, 0f,
                is_vikings,
                name);
    }

    private static @NonNull BuildingTemplate createTotemTemplate(@NonNull RenderQueues queues, @NonNull String race,
            int max_hit_points, boolean is_vikings, @NonNull String name) {
        String path = "/geometry/" + race + "/totem";
        return createBuildingTemplate(
                queues,
                Race.BUILDING_TOTEM,
                BuildingTemplate.TYPE_BUILDING,
                path + ".binsprite",
                .85f, 5f,
                path + "_halfbuilt.binsprite",
                .85f, 4.5f,
                path + "_start.binsprite",
                1f, 1f,
                3f, .04f, TOTEM_SIZE, 1f, 5f, 10, max_hit_points,
                new EmptyUnitContainerFactory(),
                new Abilities(Abilities.AURA),
                new float[]{0f, 1f, 2.5f}, 0f, 2f,
                0f, 0f, 0f,
                0f, 0f, 0f,
                is_vikings,
                name);
    }

    /**
     * Gameplay numbers (hit points, speeds, hit and defense chances, spell strengths) come from {@code stats}; the
     * numbers left inline here belong to the models: footprints, selection shapes, offsets and animation timings.
     */
    public RacesResources(@NonNull RenderQueues queues, @NonNull RulesetStats stats) {
        RaceStats natives = stats.natives();
        RaceStats vikings = stats.vikings();
        SpellStats spells = stats.spells();
        int num_progress = 25;
        SpriteFile native_rock_sprite = new SpriteFile("/geometry/natives/rock_resource.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        ProgressForm.progress(1f / num_progress);
        SpriteFile native_wood_sprite = new SpriteFile("/geometry/natives/wood_resource.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        SpriteFile native_rubber_sprite = new SpriteFile("/geometry/natives/rubber_resource.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        SpriteFile native_right_paddle_sprite = new SpriteFile(
                "/geometry/natives/right_paddle.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true,
                true,
                true,
                false);
        SpriteFile native_left_paddle_sprite = new SpriteFile(
                "/geometry/natives/left_paddle.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true,
                true,
                true,
                false);
        ProgressForm.progress(1f / num_progress);
        Map<Class<? extends Supply>, SpriteKey> native_supply_sprite_lists = Map.of(
                TreeSupply.class, queues.register(native_wood_sprite),
                RockSupply.class, queues.register(native_rock_sprite),
                IronSupply.class, queues.register(native_rock_sprite, 1),
                RubberSupply.class, queues.register(native_rubber_sprite),
                LeftPaddle.class, queues.register(native_left_paddle_sprite),
                RightPaddle.class, queues.register(native_right_paddle_sprite)
        );

        SpriteFile viking_wood_sprite = new SpriteFile("/geometry/vikings/wood_resource.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        SpriteFile viking_rubber_sprite = new SpriteFile("/geometry/vikings/rubber_resource.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        ProgressForm.progress(1f / num_progress);
        SpriteFile viking_rock_sprite = new SpriteFile("/geometry/vikings/rock_resource.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        SpriteFile viking_right_paddle_sprite = new SpriteFile(
                "/geometry/vikings/right_paddle.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true,
                true,
                true,
                false);
        SpriteFile viking_left_paddle_sprite = new SpriteFile(
                "/geometry/vikings/left_paddle.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true,
                true,
                true,
                false);

        ProgressForm.progress(1f / num_progress);
        Map<Class<? extends Supply>, SpriteKey> viking_supply_sprite_lists = Map.of(
                TreeSupply.class, queues.register(viking_wood_sprite),
                RockSupply.class, queues.register(viking_rock_sprite),
                IronSupply.class, queues.register(viking_rock_sprite, 1),
                RubberSupply.class, queues.register(viking_rubber_sprite),
                LeftPaddle.class, queues.register(viking_left_paddle_sprite),
                RightPaddle.class, queues.register(viking_right_paddle_sprite)
        );

        smoke_textures[0] = queues.registerTexture(new GeneratorSmoke(), 0);
        damage_smoke_textures[0] = queues.registerTexture(new GeneratorDamageSmoke(), 0);
        poison_textures[0] = queues.registerTexture(new GeneratorPoison(), 0);
        lightning_texture = queues.registerTexture(new GeneratorLightning(), 0);


        for (int i = 0; i < note_textures.length; i++) {
            note_textures[i] = queues.registerTexture(new TextureFile("/textures/effects/note" + (i + 1),
                    Globals.COMPRESSED_RGBA_FORMAT,
                    GL11.GL_LINEAR_MIPMAP_LINEAR,
                    GL11.GL_LINEAR,
                    org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE,
                    org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE));
        }

        star_textures[0] = queues.registerTexture(new TextureFile("/textures/effects/star",
                Globals.COMPRESSED_RGBA_FORMAT,
                GL11.GL_LINEAR_MIPMAP_LINEAR,
                GL11.GL_LINEAR,
                org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE,
                org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE));

        Audio death_peon_sound = Resources.findResource(new AudioFile("/sfx/death_peon.ogg"));
        Audio death_viking1_sound = Resources.findResource(new AudioFile("/sfx/death_viking_warrior1.ogg"));
        Audio death_viking2_sound = Resources.findResource(new AudioFile("/sfx/death_viking_warrior2.ogg"));
        Audio death_native1_sound = Resources.findResource(new AudioFile("/sfx/death_native_warrior1.ogg"));
        Audio death_native2_sound = Resources.findResource(new AudioFile("/sfx/death_native_warrior2.ogg"));

        Audio axe_throw_sound = Resources.findResource(new AudioFile("/sfx/weapon_axe.ogg"));
        Audio spear_throw_sound = Resources.findResource(new AudioFile("/sfx/weapon_spear.ogg"));

        tree_fall_sound = new Audio[]{Resources.findResource(new AudioFile(
                "/sfx/felling_tree.ogg")), Resources.findResource(new AudioFile("/sfx/felling_palmtree.ogg"))
        };

        ProgressForm.progress(1f / num_progress);

        building_hit_sound = new Audio[]{Resources.findResource(new AudioFile(
                "/sfx/impact_wood1.ogg")), Resources.findResource(new AudioFile(
                        "/sfx/impact_wood2.ogg")), Resources.findResource(new AudioFile(
                                "/sfx/impact_wood3.ogg")), Resources.findResource(new AudioFile(
                                        "/sfx/impact_wood4.ogg"))
        };

        gas_sound = Resources.findResource(new AudioFile("/sfx/gas.ogg"));
        bubbling_sound = Resources.findResource(new AudioFile("/sfx/bubbling.ogg"));
        lightning_sound = Resources.findResource(new AudioFile("/sfx/flash.ogg"));
        cloud_sound = Resources.findResource(new AudioFile("/sfx/crackling_cloud.ogg"));

        armory_sound = Resources.findResource(new AudioFile("/sfx/armory.ogg"));

        building_collapse_sound = Resources.findResource(new AudioFile("/sfx/building_crash.ogg"));

        stun_sound = new Audio[]{Resources.findResource(new AudioFile("/sfx/lur_stun1.ogg")), Resources.findResource(
                new AudioFile("/sfx/lur_stun2.ogg")), Resources.findResource(new AudioFile("/sfx/lur_stun3.ogg"))
        };

        blast_lur_sound = new Audio[]{Resources.findResource(new AudioFile(
                "/sfx/lur_blast1.ogg")), Resources.findResource(new AudioFile(
                        "/sfx/lur_blast2.ogg")), Resources.findResource(new AudioFile("/sfx/lur_blast3.ogg"))
        };
        blast_rumble_sound = Resources.findResource(new AudioFile("/sfx/rumble.ogg"));
        blast_blast_sound = Resources.findResource(new AudioFile("/sfx/lurblast.ogg"));

        Audio[] tree_cut_sound = new Audio[]{Resources.findResource(new AudioFile(
                "/sfx/axe_cutting_wood1.ogg")), Resources.findResource(new AudioFile(
                        "/sfx/axe_cutting_wood2.ogg")), Resources.findResource(new AudioFile(
                                "/sfx/axe_cutting_wood3.ogg")), Resources.findResource(new AudioFile(
                                        "/sfx/axe_cutting_wood4.ogg")), Resources.findResource(new AudioFile(
                                                "/sfx/axe_cutting_wood5.ogg")), Resources.findResource(new AudioFile(
                                                        "/sfx/axe_cutting_wood6.ogg"))
        };

        Audio[] rock_cut_sound = new Audio[]{Resources.findResource(new AudioFile(
                "/sfx/axe_cutting_stone1.ogg")), Resources.findResource(new AudioFile(
                        "/sfx/axe_cutting_stone2.ogg")), Resources.findResource(new AudioFile(
                                "/sfx/axe_cutting_stone3.ogg")), Resources.findResource(new AudioFile(
                                        "/sfx/axe_cutting_stone4.ogg")), Resources.findResource(new AudioFile(
                                                "/sfx/axe_cutting_stone5.ogg"))
        };

        Audio[] meat_cut_sound = new Audio[]{Resources.findResource(new AudioFile(
                "/sfx/impact_meat1.ogg")), Resources.findResource(new AudioFile(
                        "/sfx/impact_meat2.ogg")), Resources.findResource(new AudioFile(
                                "/sfx/impact_meat3.ogg")), Resources.findResource(new AudioFile(
                                        "/sfx/impact_meat4.ogg")), Resources.findResource(new AudioFile(
                                                "/sfx/impact_meat5.ogg"))
        };

        ProgressForm.progress(1f / num_progress);
        harvest_sounds.put(TreeSupply.class, tree_cut_sound);
        harvest_sounds.put(RockSupply.class, rock_cut_sound);
        harvest_sounds.put(IronSupply.class, rock_cut_sound);
        harvest_sounds.put(RubberSupply.class, meat_cut_sound);

        BuildingTemplate viking_quarters_template = createBuildingTemplate(
                queues,
                Race.BUILDING_QUARTERS,
                BuildingTemplate.TYPE_BUILDING,
                "/geometry/vikings/quarters.binsprite",
                3.5f, 7f,
                "/geometry/vikings/quarters_halfbuilt.binsprite",
                3.5f, 6f,
                "/geometry/vikings/quarters_start.binsprite",
                5f, 1f,
                22f, .001f, QUARTERS_SIZE, 6f, 9f, 30, vikings.quarters().hit_points(),
                new ReproduceUnitContainerFactory(),
                new Abilities(Abilities.REPRODUCE | Abilities.RALLY_TO | Abilities.TARGET),
                new float[]{0f, 1f, 3f}, 0f, 6f,
                3.65f, .25f, 8f,
                0f, 0f, 0f,
                true,
                i18n("quarters"));
        ProgressForm.progress(1f / num_progress);
        BuildingTemplate viking_armory_template = createBuildingTemplate(
                queues,
                Race.BUILDING_ARMORY,
                BuildingTemplate.TYPE_BUILDING,
                "/geometry/vikings/armory.binsprite",
                3.5f, 7f,
                "/geometry/vikings/armory_halfbuilt.binsprite",
                3.5f, 6f,
                "/geometry/vikings/armory_start.binsprite",
                5f, 1f,
                22f, .001f, ARMORY_SIZE, 6f, 9f, 30, vikings.armory().hit_points(),
                new WorkerUnitContainerFactory(),
                new Abilities(
                        Abilities.SUPPLY_CONTAINER | Abilities.BUILD_ARMIES | Abilities.RALLY_TO | Abilities.TARGET),
                new float[]{0f, 1f, 3f}, 0f, 6f,
                0f, 2.25f, 10f,
                .25f, -2.8f, 13.1f,
                true,
                i18n("armory"));
        ProgressForm.progress(1f / num_progress);
        BuildingTemplate viking_tower_template = createBuildingTemplate(
                queues,
                Race.BUILDING_TOWER,
                BuildingTemplate.TYPE_BUILDING,
                "/geometry/vikings/tower.binsprite",
                1.25f, 11f,
                "/geometry/vikings/tower_halfbuilt.binsprite",
                2f, 7f,
                "/geometry/vikings/tower_start.binsprite",
                2.5f, 1f,
                10f, .009f, TOWER_SIZE, 3f, 12f, 20, vikings.tower().hit_points(),
                new MountUnitContainerFactory(),
                new Abilities(Abilities.ATTACK | Abilities.RALLY_TO | Abilities.TARGET),
                new float[]{0f, 2f, 7.5f}, 9.55f, 2.5f,
                .85f, .85f, 9.5f,
                0f, 0f, 0f,
                true,
                i18n("tower"));
        ProgressForm.progress(1f / num_progress);
        BuildingTemplate native_quarters_template = createBuildingTemplate(
                queues,
                Race.BUILDING_QUARTERS,
                BuildingTemplate.TYPE_BUILDING,
                "/geometry/natives/quarters.binsprite",
                4f, 8f,
                "/geometry/natives/quarters_halfbuilt.binsprite",
                4f, 6f,
                "/geometry/natives/quarters_start.binsprite",
                5f, 1f,
                16f, .004f, QUARTERS_SIZE, 6f, 9f, 30, natives.quarters().hit_points(),
                new ReproduceUnitContainerFactory(),
                new Abilities(Abilities.REPRODUCE | Abilities.RALLY_TO | Abilities.TARGET),
                new float[]{0f, 1f, 3f}, 0f, 6f,
                -1.15f, -.77f, 11f,
                0f, 0f, 0f,
                false,
                i18n("quarters"));
        ProgressForm.progress(1f / num_progress);
        BuildingTemplate native_armory_template = createBuildingTemplate(
                queues,
                Race.BUILDING_ARMORY,
                BuildingTemplate.TYPE_BUILDING,
                "/geometry/natives/armory.binsprite",
                4f, 8f,
                "/geometry/natives/armory_halfbuilt.binsprite",
                4f, 6f,
                "/geometry/natives/armory_start.binsprite",
                5f, 1f,
                16f, .004f, ARMORY_SIZE, 6f, 9f, 30, natives.armory().hit_points(),
                new WorkerUnitContainerFactory(),
                new Abilities(
                        Abilities.SUPPLY_CONTAINER | Abilities.BUILD_ARMIES | Abilities.RALLY_TO | Abilities.TARGET),
                new float[]{0f, 1f, 3f}, 0f, 6f,
                0f, -.4f, 12f,
                0f, -1f, 11.5f,
                false,
                i18n("armory"));
        ProgressForm.progress(1f / num_progress);
        BuildingTemplate native_tower_template = createBuildingTemplate(
                queues,
                Race.BUILDING_TOWER,
                BuildingTemplate.TYPE_BUILDING,
                "/geometry/natives/tower.binsprite",
                1f, 14f,
                "/geometry/natives/tower_halfbuilt.binsprite",
                1f, 14f,
                "/geometry/natives/tower_start.binsprite",
                1.5f, 2f,
                5f, .025f, TOWER_SIZE, 3f, 12f, 20, natives.tower().hit_points(),
                new MountUnitContainerFactory(),
                new Abilities(Abilities.ATTACK | Abilities.RALLY_TO | Abilities.TARGET),
                new float[]{0f, 11.5f, 11.5f}, 13f, 2.5f,
                .95f, 0f, 13f,
                0f, 0f, 0f,
                false,
                i18n("tower"));
        ProgressForm.progress(1f / num_progress);

        BuildingTemplate native_ship_template = createBuildingTemplate(
                queues,
                Race.BUILDING_SHIP,
                BuildingTemplate.TYPE_SHIP,
                "/geometry/natives/ship.binsprite",
                3.5f,
                7f,
                "/geometry/natives/ship_halfbuilt.binsprite",
                3.5f,
                6f,
                "/geometry/natives/ship_start.binsprite",
                5f,
                1f,
                22f,
                .001f,
                SHIP_SIZE,
                6f,
                9f,
                100,
                natives.ship().hit_points(),
                null,
                new Abilities(
                        Abilities.SUPPLY_CONTAINER | Abilities.SAIL | Abilities.RALLY_TO | Abilities.TARGET),
                new float[]{0f, 1f, 3f},
                1.9f,
                6f,
                -0.5f,
                0.0f,
                3.1f,
                1.0f,
                0.0f,
                5.0f,
                false,
                Utils.getBundleString(bundle, "ship"));
        ProgressForm.progress(1f / num_progress);

        BuildingTemplate viking_ship_template = createBuildingTemplate(
                queues,
                Race.BUILDING_SHIP,
                BuildingTemplate.TYPE_SHIP,
                "/geometry/vikings/ship.binsprite",
                3.5f,
                7f,
                "/geometry/vikings/ship_halfbuilt.binsprite",
                3.5f,
                6f,
                "/geometry/vikings/ship_start.binsprite",
                5f,
                1f,
                22f,
                .001f,
                SHIP_SIZE,
                6f,
                9f,
                100,
                vikings.ship().hit_points(),
                null,
                new Abilities(
                        Abilities.SUPPLY_CONTAINER | Abilities.SAIL | Abilities.RALLY_TO | Abilities.TARGET),
                new float[]{0f, 1f, 3f},
                1.9f,
                6f,
                -0.5f,
                0.0f,
                3.1f,
                1.0f,
                0.0f,
                5.0f,
                true,
                Utils.getBundleString(bundle, "ship"));
        ProgressForm.progress(1f / num_progress);

        // Buffed's buildings. Placeholder models: the race's Quarters and Tower, scaled down and recoloured.
        BuildingTemplate native_chicken_coop_template = createChickenCoopTemplate(queues, "natives",
                natives.chicken_coop().hit_points(), false, i18n("chicken_coop_natives"));
        BuildingTemplate viking_chicken_coop_template = createChickenCoopTemplate(queues, "vikings",
                vikings.chicken_coop().hit_points(), true, i18n("chicken_coop_vikings"));
        BuildingTemplate native_totem_template = createTotemTemplate(queues, "natives", natives.totem().hit_points(),
                false, i18n("totem_natives"));
        BuildingTemplate viking_totem_template = createTotemTemplate(queues, "vikings", vikings.totem().hit_points(),
                true, i18n("totem_vikings"));
        // M7. Placeholder models: the race's Armory scaled down and recoloured; stakes and posts for the walls.
        BuildingTemplate native_market_template = createMarketTemplate(queues, "natives",
                natives.market().hit_points(), false, i18n("market_natives"));
        BuildingTemplate viking_market_template = createMarketTemplate(queues, "vikings",
                vikings.market().hit_points(), true, i18n("market_vikings"));
        BuildingTemplate native_palisade_template = createWallTemplate(queues, "natives", Race.BUILDING_PALISADE,
                "palisade", 3f, natives.palisade().hit_points(), false, i18n("palisade_natives"));
        BuildingTemplate viking_palisade_template = createWallTemplate(queues, "vikings", Race.BUILDING_PALISADE,
                "palisade", 3f, vikings.palisade().hit_points(), true, i18n("palisade_vikings"));
        BuildingTemplate native_gate_template = createWallTemplate(queues, "natives", Race.BUILDING_GATE, "gate",
                3.5f, natives.gate().hit_points(), false, i18n("gate_natives"));
        BuildingTemplate viking_gate_template = createWallTemplate(queues, "vikings", Race.BUILDING_GATE, "gate",
                3.5f, vikings.gate().hit_points(), true, i18n("gate_vikings"));
        // M8. Placeholder models: the race's Tower widened, and its Quarters recoloured (the model numbers above).
        BuildingTemplate native_great_tower_template = createGreatTowerTemplate(queues, "natives",
                natives.great_tower(), false, i18n("great_tower"));
        BuildingTemplate viking_great_tower_template = createGreatTowerTemplate(queues, "vikings",
                vikings.great_tower(), true, i18n("great_tower"));
        BuildingTemplate native_lodge_template = createLodgeTemplate(queues, "natives", natives.lodge(), false,
                i18n("lodge_natives"));
        BuildingTemplate viking_lodge_template = createLodgeTemplate(queues, "vikings", vikings.lodge(), true,
                i18n("lodge_vikings"));

        final float shadow_diameter_warrior = 1.9f;
        final float shadow_diameter_peon = 1.6f;
        final float shadow_diameter_chieftain = 2.2f;
        ProgressForm.progress(1f / num_progress);

        SpriteFile sprite_list_warrior = new SpriteFile("/geometry/vikings/warrior.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        ProgressForm.progress(1f / num_progress);

        SpriteFile sprite_list_chieftain = new SpriteFile("/geometry/vikings/chieftain.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        ProgressForm.progress(1f / num_progress);
        SpriteFile sprite_list_native_chieftain = new SpriteFile("/geometry/natives/chieftain.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        SpriteFile sprite_list_peon = new SpriteFile("/geometry/vikings/peon.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        ProgressForm.progress(1f / num_progress);
        SpriteFile sprite_list_native_peon = new SpriteFile("/geometry/natives/peon.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        ProgressForm.progress(1f / num_progress);
        SpriteFile sprite_list_native_warrior = new SpriteFile("/geometry/natives/warrior.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        ProgressForm.progress(1f / num_progress);
        SpriteFile viking_warrior_axe = new SpriteFile("/geometry/vikings/axe.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        ProgressForm.progress(1f / num_progress);
        SpriteFile native_warrior_spear = new SpriteFile("/geometry/natives/spear.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        ProgressForm.progress(1f / num_progress);

        Audio[] unit_hit_sounds = new Audio[]{Resources.findResource(new AudioFile(
                "/sfx/impact_meat1.ogg")), Resources.findResource(new AudioFile(
                        "/sfx/impact_meat2.ogg")), Resources.findResource(new AudioFile(
                                "/sfx/impact_meat3.ogg")), Resources.findResource(new AudioFile(
                                        "/sfx/impact_meat4.ogg")), Resources.findResource(new AudioFile(
                                                "/sfx/impact_meat5.ogg"))
        };
        WeaponFactory viking_warrior_rock_weapon = new ThrowingFactory<>(RockAxeWeapon.class, RockAxeWeapon::new,
                vikings.rock_warrior().hit_chance(),
                THROW_RANGE, 29f / 58f,
                queues.register(viking_warrior_axe, Race.UNIT_WARRIOR_ROCK),
                axe_throw_sound,
                unit_hit_sounds);
        WeaponFactory viking_warrior_iron_weapon = new ThrowingFactory<>(IronAxeWeapon.class, IronAxeWeapon::new,
                vikings.iron_warrior().hit_chance(),
                THROW_RANGE, 29f / 58f,
                queues.register(viking_warrior_axe, Race.UNIT_WARRIOR_IRON),
                axe_throw_sound,
                unit_hit_sounds);
        WeaponFactory viking_warrior_rubber_weapon = new ThrowingFactory<>(RubberAxeWeapon.class, RubberAxeWeapon::new,
                vikings.chicken_warrior().hit_chance(), THROW_RANGE, 29f / 58f,
                queues.register(viking_warrior_axe, Race.UNIT_WARRIOR_RUBBER),
                axe_throw_sound,
                unit_hit_sounds);
        WeaponFactory native_warrior_rock_weapon = new ThrowingFactory<>(RockSpearWeapon.class, RockSpearWeapon::new,
                natives.rock_warrior().hit_chance(), THROW_RANGE, 46f / 100f,
                queues.register(native_warrior_spear, Race.UNIT_WARRIOR_ROCK),
                spear_throw_sound,
                unit_hit_sounds);
        WeaponFactory native_warrior_iron_weapon = new ThrowingFactory<>(IronSpearWeapon.class, IronSpearWeapon::new,
                natives.iron_warrior().hit_chance(), THROW_RANGE, 46f / 100f,
                queues.register(native_warrior_spear, Race.UNIT_WARRIOR_IRON),
                spear_throw_sound,
                unit_hit_sounds);
        WeaponFactory native_warrior_rubber_weapon = new ThrowingFactory<>(RubberSpearWeapon.class,
                RubberSpearWeapon::new, natives.chicken_warrior().hit_chance(), THROW_RANGE, 46f / 100f,
                queues.register(native_warrior_spear, Race.UNIT_WARRIOR_RUBBER),
                spear_throw_sound,
                unit_hit_sounds);

        Audio[] native_chieftain_hit_sounds = new Audio[]{Resources.findResource(new AudioFile(
                "/sfx/hit3.ogg")), Resources.findResource(new AudioFile("/sfx/hit4.ogg")), Resources.findResource(
                        new AudioFile("/sfx/hit5.ogg")), Resources.findResource(new AudioFile("/sfx/hit6.ogg"))
        };
        Audio[] viking_chieftain_hit_sounds = new Audio[]{Resources.findResource(new AudioFile(
                "/sfx/hit1.ogg")), Resources.findResource(new AudioFile("/sfx/hit2.ogg")), Resources.findResource(
                        new AudioFile("/sfx/hit6.ogg")), Resources.findResource(new AudioFile("/sfx/hit7.ogg"))
        };

        ProgressForm.progress(1f / num_progress);
        ShadowListKey default_shadow_list = queues.registerSelectableShadowList(DEFAULT_SHADOW_DESC);
        UnitTemplate viking_warrior_rock_template = new UnitTemplate(.4f,
                1.2f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.THROW),
                vikings.rock_warrior().speed(),
                viking_warrior_rock_weapon,
                queues.register(sprite_list_warrior, Race.UNIT_WARRIOR_ROCK),
                shadow_diameter_warrior,
                default_shadow_list,
                null,
                death_viking1_sound,
                .25f,
                new float[]{1.2f},
                1f,
                vikings.rock_warrior().defense_chance(),
                i18n("rock_warrior"),
                vikings.rock_warrior().hit_points(),
                0f, 0f, 2f,
                3);
        UnitTemplate viking_warrior_iron_template = new UnitTemplate(.4f,
                1.2f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.THROW),
                vikings.iron_warrior().speed(),
                viking_warrior_iron_weapon,
                queues.register(sprite_list_warrior, Race.UNIT_WARRIOR_IRON),
                shadow_diameter_warrior,
                default_shadow_list,
                null,
                death_viking2_sound,
                .25f,
                new float[]{1.2f},
                1f,
                vikings.iron_warrior().defense_chance(),
                i18n("iron_warrior"),
                vikings.iron_warrior().hit_points(),
                0f, 0f, 2f,
                5);
        UnitTemplate viking_warrior_rubber_template = new UnitTemplate(.4f,
                1.2f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.THROW),
                vikings.chicken_warrior().speed(),
                viking_warrior_rubber_weapon,
                queues.register(sprite_list_warrior, Race.UNIT_WARRIOR_RUBBER),
                shadow_diameter_warrior,
                default_shadow_list,
                null,
                death_viking2_sound,
                .25f,
                new float[]{1.2f},
                1f,
                vikings.chicken_warrior().defense_chance(),
                i18n("chicken_warrior"),
                vikings.chicken_warrior().hit_points(),
                0f, 0f, 2f,
                10);
        UnitTemplate native_warrior_rock_template = new UnitTemplate(.4f,
                1.2f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.THROW),
                natives.rock_warrior().speed(),
                native_warrior_rock_weapon,
                queues.register(sprite_list_native_warrior, Race.UNIT_WARRIOR_ROCK),
                shadow_diameter_warrior,
                default_shadow_list,
                null,
                death_native1_sound,
                .25f,
                new float[]{1.2f},
                1f,
                natives.rock_warrior().defense_chance(),
                i18n("rock_warrior"),
                natives.rock_warrior().hit_points(),
                0f, 0f, 2f,
                3);
        UnitTemplate native_warrior_iron_template = new UnitTemplate(.4f,
                1.2f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.THROW),
                natives.iron_warrior().speed(),
                native_warrior_iron_weapon,
                queues.register(sprite_list_native_warrior, Race.UNIT_WARRIOR_IRON),
                shadow_diameter_warrior,
                default_shadow_list,
                null,
                death_native2_sound,
                .25f,
                new float[]{1.2f},
                1f,
                natives.iron_warrior().defense_chance(),
                i18n("iron_warrior"),
                natives.iron_warrior().hit_points(),
                0f, 0f, 2f,
                5);
        UnitTemplate native_warrior_rubber_template = new UnitTemplate(.4f,
                1.2f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.THROW),
                natives.chicken_warrior().speed(),
                native_warrior_rubber_weapon,
                queues.register(sprite_list_native_warrior, Race.UNIT_WARRIOR_RUBBER),
                shadow_diameter_warrior,
                default_shadow_list,
                null,
                death_native2_sound,
                .25f,
                new float[]{1.2f},
                1f,
                natives.chicken_warrior().defense_chance(),
                i18n("chicken_warrior"),
                natives.chicken_warrior().hit_points(),
                0f, 0f, 2f,
                10);
        UnitTemplate viking_peon_template = new UnitTemplate(.4f,
                1.1f,
                new Abilities(Abilities.BUILD | Abilities.HARVEST | Abilities.ATTACK | Abilities.TARGET),
                vikings.peon().speed(),
                new InstantHitFactory(vikings.peon().hit_chance(), 0f, 11f / 38f, unit_hit_sounds),
                queues.register(sprite_list_peon),
                shadow_diameter_peon,
                default_shadow_list,
                new UnitSupplyContainerFactory(MAX_UNIT_RESOURCES, viking_supply_sprite_lists),
                death_peon_sound,
                .25f,
                new float[]{.7f},
                1f,
                vikings.peon().defense_chance(),
                i18n("peon"),
                vikings.peon().hit_points(),
                .1f, 0f, 1.75f,
                1);
        UnitTemplate native_peon_template = new UnitTemplate(.4f,
                1.1f,
                new Abilities(Abilities.BUILD | Abilities.HARVEST | Abilities.ATTACK | Abilities.TARGET),
                natives.peon().speed(),
                new InstantHitFactory(natives.peon().hit_chance(), 0f, 51f / 83f, unit_hit_sounds),
                queues.register(sprite_list_native_peon),
                shadow_diameter_peon,
                default_shadow_list,
                new UnitSupplyContainerFactory(MAX_UNIT_RESOURCES, native_supply_sprite_lists),
                death_peon_sound,
                .25f,
                new float[]{.7f},
                1f,
                natives.peon().defense_chance(),
                i18n("peon"),
                natives.peon().hit_points(),
                0f, 0f, 1.75f,
                1);
        UnitTemplate viking_chieftain_template = new UnitTemplate(.4f,
                1.4f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.MAGIC),
                vikings.chieftain().speed(),
                new InstantHitFactory(vikings.chieftain().hit_chance(), 0f, 75f / 119f, viking_chieftain_hit_sounds),
                queues.register(sprite_list_chieftain),
                shadow_diameter_chieftain,
                default_shadow_list,
                null,
                death_viking2_sound,
                .15f,
                new float[]{1.7f},
                1f,
                vikings.chieftain().defense_chance(),
                i18n("chieftain"),
                vikings.chieftain().hit_points(),
                -.07f, .312f, 2.7f,
                40);
        UnitTemplate native_chieftain_template = new UnitTemplate(.4f,
                1.4f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.MAGIC),
                natives.chieftain().speed(),
                new InstantHitFactory(natives.chieftain().hit_chance(), 0f, 75f / 129f, native_chieftain_hit_sounds),
                queues.register(sprite_list_native_chieftain),
                shadow_diameter_chieftain,
                default_shadow_list,
                null,
                death_native2_sound,
                .15f,
                new float[]{1.7f},
                1f,
                natives.chieftain().defense_chance(),
                i18n("chieftain"),
                natives.chieftain().hit_points(),
                .878f, .151f, 2.8f,
                40);

        // Buffed's gear warriors: hand to hand, striking at the warrior's release point (46/100 and 29/58).
        UnitTemplate native_warrior_shield_template = createGearWarriorTemplate(queues, "natives", "shield_warrior",
                natives.shield_warrior(), new GearFactory(Shield.class, natives.shield_warrior().hit_chance(),
                        46f / 100f, null, unit_hit_sounds),
                default_shadow_list, death_native1_sound, i18n("shield_warrior_natives"), 4);
        UnitTemplate native_warrior_torch_template = createGearWarriorTemplate(queues, "natives", "torch_warrior",
                natives.torch_warrior(), new GearFactory(Torch.class, natives.torch_warrior().hit_chance(),
                        46f / 100f, natives.torch(), unit_hit_sounds),
                default_shadow_list, death_native2_sound, i18n("torch_warrior_natives"), 6);
        UnitTemplate viking_warrior_shield_template = createGearWarriorTemplate(queues, "vikings", "shield_warrior",
                vikings.shield_warrior(), new GearFactory(Shield.class, vikings.shield_warrior().hit_chance(),
                        29f / 58f, null, unit_hit_sounds),
                default_shadow_list, death_viking1_sound, i18n("shield_warrior_vikings"), 4);
        UnitTemplate viking_warrior_torch_template = createGearWarriorTemplate(queues, "vikings", "torch_warrior",
                vikings.torch_warrior(), new GearFactory(Torch.class, vikings.torch_warrior().hit_chance(),
                        29f / 58f, vikings.torch(), unit_hit_sounds),
                default_shadow_list, death_viking2_sound, i18n("torch_warrior_vikings"), 6);
        // Buffed's Champion, trained at the Lodge: hand to hand like the gear.
        UnitTemplate native_champion_template = createGearWarriorTemplate(queues, "natives", "champion",
                natives.champion(), new GearFactory(Champion.class, natives.champion().hit_chance(), 46f / 100f, null,
                        unit_hit_sounds),
                default_shadow_list, death_native1_sound, i18n("champion_natives"), 8);
        UnitTemplate viking_champion_template = createGearWarriorTemplate(queues, "vikings", "champion",
                vikings.champion(), new GearFactory(Champion.class, vikings.champion().hit_chance(), 29f / 58f, null,
                        unit_hit_sounds),
                default_shadow_list, death_viking1_sound, i18n("champion_vikings"), 8);
        // Buffed's Drum / Horn and Net (docs/design/drum-and-net.md). The Drummer never strikes (no ATTACK); the
        // Chicken Catcher strikes like the gear and carries a caught chicken like a peon.
        int drum_abilities = Abilities.TARGET | Abilities.THROW;
        int net_abilities = Abilities.ATTACK | Abilities.TARGET | Abilities.THROW;
        UnitTemplate native_warrior_drum_template = createGearWarriorTemplate(queues, "natives", "drum_warrior",
                natives.drum_warrior(), new GearFactory(Drum.class, natives.drum_warrior().hit_chance(), 46f / 100f,
                        null, unit_hit_sounds),
                default_shadow_list, death_native1_sound, i18n("drum_warrior_natives"), 5, drum_abilities, null);
        UnitTemplate native_warrior_net_template = createGearWarriorTemplate(queues, "natives", "net_warrior",
                natives.net_warrior(), new GearFactory(Net.class, natives.net_warrior().hit_chance(), 46f / 100f,
                        null, unit_hit_sounds),
                default_shadow_list, death_native2_sound, i18n("net_warrior_natives"), 4, net_abilities,
                new UnitSupplyContainerFactory(MAX_UNIT_RESOURCES, native_supply_sprite_lists));
        UnitTemplate viking_warrior_drum_template = createGearWarriorTemplate(queues, "vikings", "drum_warrior",
                vikings.drum_warrior(), new GearFactory(Drum.class, vikings.drum_warrior().hit_chance(), 29f / 58f,
                        null, unit_hit_sounds),
                default_shadow_list, death_viking1_sound, i18n("drum_warrior_vikings"), 5, drum_abilities, null);
        UnitTemplate viking_warrior_net_template = createGearWarriorTemplate(queues, "vikings", "net_warrior",
                vikings.net_warrior(), new GearFactory(Net.class, vikings.net_warrior().hit_chance(), 29f / 58f,
                        null, unit_hit_sounds),
                default_shadow_list, death_viking2_sound, i18n("net_warrior_vikings"), 4, net_abilities,
                new UnitSupplyContainerFactory(MAX_UNIT_RESOURCES, viking_supply_sprite_lists));

        StinkingStewStats stew = spells.stinking_stew();
        CracklingCloudStats cloud = spells.crackling_cloud();
        TerrifyingTootStats toot = spells.terrifying_toot();
        RavagingRoarStats roar = spells.ravaging_roar();
        MagicFactory[] native_magic = new MagicFactory[NUM_MAGIC];
        native_magic[INDEX_MAGIC_POISON] = new PoisonFogFactory(0.9f, 0f, 0.55f, stew.radius(), stew.hit_chance(),
                stew.interval(), stew.seconds(), stew.damage(), 5f, 80f / 224f, 163f / 224f);
        native_magic[INDEX_MAGIC_LIGHTNING] = new LightningCloudFactory(0.9f, 0f, 0.55f, cloud.seconds(),
                cloud.seconds_per_hit(), cloud.speed(), cloud.hit_chance(), cloud.damage(), 18f, 5f, 80f / 224f,
                163f / 224f);

        MagicFactory[] viking_magic = new MagicFactory[NUM_MAGIC];
        viking_magic[INDEX_MAGIC_STUN] = new StunFactory(2.57f, 0f, 3.8f, toot.radius(), toot.stun_seconds_closest(),
                toot.stun_seconds_farthest(), 6f, 57f / 159f, 100f / 159f);
        viking_magic[INDEX_MAGIC_BLAST] = new SonicBlastFactory(2.57f, 0f, 3.8f, roar.radius(),
                roar.hit_chance_closest(), roar.hit_chance_farthest(), roar.damage_closest(), roar.damage_farthest(),
                roar.seconds(), 6f, 57f / 159f, 100f / 159f);

        ProgressForm.progress(1f / num_progress);
        // The race icons are only drawn; a headless world (no context to load their atlas into) has none.
        GUIIcons icons = Headless.isEnabled() ? null : GUIIcons.getIcons();
        Race natives_race = new Race(native_quarters_template,
                native_armory_template,
                native_tower_template,
                native_ship_template,
                native_chicken_coop_template,
                native_totem_template,
                native_market_template,
                native_palisade_template,
                native_gate_template,
                native_great_tower_template,
                native_lodge_template,
                native_warrior_rock_template,
                native_warrior_iron_template,
                native_warrior_rubber_template,
                native_peon_template,
                native_chieftain_template,
                native_warrior_shield_template,
                native_warrior_torch_template,
                native_champion_template,
                native_warrior_drum_template,
                native_warrior_net_template,
                queues.register(new SpriteFile("/geometry/natives/rally_point.binsprite",
                        Globals.NO_MIPMAP_CUTOFF,
                        true, true, true, false)),
                icons != null ? icons.getNativeIcons() : null,
                Resources.findResource(new AudioFile("/sfx/attacknotify_native.ogg")),
                Resources.findResource(new AudioFile("/sfx/buildingnotify_native.ogg")),
                native_magic,
                new NativeChieftainAI(),
                "/music/native.ogg");
        Race vikings_race = new Race(viking_quarters_template,
                viking_armory_template,
                viking_tower_template,
                viking_ship_template,
                viking_chicken_coop_template,
                viking_totem_template,
                viking_market_template,
                viking_palisade_template,
                viking_gate_template,
                viking_great_tower_template,
                viking_lodge_template,
                viking_warrior_rock_template,
                viking_warrior_iron_template,
                viking_warrior_rubber_template,
                viking_peon_template,
                viking_chieftain_template,
                viking_warrior_shield_template,
                viking_warrior_torch_template,
                viking_champion_template,
                viking_warrior_drum_template,
                viking_warrior_net_template,
                queues.register(new SpriteFile("/geometry/vikings/rally_point.binsprite",
                        Globals.NO_MIPMAP_CUTOFF,
                        true, true, true, false)),
                icons != null ? icons.getVikingIcons() : null,
                Resources.findResource(new AudioFile("/sfx/attacknotify_viking.ogg")),
                Resources.findResource(new AudioFile("/sfx/buildingnotify_viking.ogg")),
                viking_magic,
                new VikingChieftainAI(),
                "/music/viking.ogg");
        races = new Race[]{natives_race, vikings_race};
        snare_name = i18n("snare");
        snare_sprites[RACE_NATIVES] = queues.register(new SpriteFile("/geometry/natives/snare.binsprite",
                Globals.NO_MIPMAP_CUTOFF, true, true, true, false));
        snare_sprites[RACE_VIKINGS] = queues.register(new SpriteFile("/geometry/vikings/snare.binsprite",
                Globals.NO_MIPMAP_CUTOFF, true, true, true, false));

        wood_fragment_sprites[0] = queues.register(new SpriteFile("/geometry/misc/wood_2.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false), 0);
        wood_fragment_sprites[1] = queues.register(new SpriteFile("/geometry/misc/wood_3.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false));
        wood_fragment_sprites[2] = queues.register(new SpriteFile("/geometry/misc/wood_4.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false));
        wood_fragment_sprites[3] = queues.register(new SpriteFile("/geometry/misc/wood_5.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false));

        treasure_sprites[0] = queues.register(new SpriteFile("/geometry/misc/icon.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false));
        treasure_sprites[1] = queues.register(new SpriteFile("/geometry/misc/treasure_1.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false));
        treasure_sprites[2] = queues.register(new SpriteFile("/geometry/misc/treasure_2.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false));
        treasure_sprites[3] = queues.register(new SpriteFile("/geometry/misc/treasure_3.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false));
        treasure_sprites[4] = queues.register(new SpriteFile("/geometry/misc/treasure_4.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false));
        treasure_sprites[5] = queues.register(new SpriteFile("/geometry/misc/treasure_5.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false));

        ProgressForm.progress(1f / num_progress);
        ProgressForm.progress(1f / num_progress);
    }

    public @NonNull TextureKey @NonNull [] getSmokeTextures() {
        return smoke_textures;
    }

    public @NonNull TextureKey @NonNull [] getDamageSmokeTextures() {
        return damage_smoke_textures;
    }

    public @NonNull TextureKey @NonNull [] getPoisonTextures() {
        return poison_textures;
    }

    public @NonNull TextureKey getLightningTexture() {
        return lightning_texture;
    }

    public @NonNull TextureKey @NonNull [] getNoteTextures() {
        return note_textures;
    }

    public @NonNull TextureKey @NonNull [] getStarTextures() {
        return star_textures;
    }

    public @NonNull Audio getHarvestSound(Class<? extends Supply> key, @NonNull Random random) {
        Audio[] sounds = harvest_sounds.get(key);
        return sounds[random.nextInt(sounds.length)];
    }

    public @NonNull Audio @NonNull [] getTreeFallSound() {
        return tree_fall_sound;
    }

    public @NonNull Audio getBuildingHitSound(@NonNull Random random) {
        return building_hit_sound[random.nextInt(building_hit_sound.length)];
    }

    public @NonNull Audio getGasSound() {
        return gas_sound;
    }

    public @NonNull Audio getBubblingSound() {
        return bubbling_sound;
    }

    public @NonNull Audio getLightningSound() {
        return lightning_sound;
    }

    public @NonNull Audio getCloudSound() {
        return cloud_sound;
    }

    public @NonNull Audio getStunSound(@NonNull Random random) {
        return stun_sound[random.nextInt(stun_sound.length)];
    }

    public @NonNull Audio getBlastLurSound(@NonNull Random random) {
        return blast_lur_sound[random.nextInt(blast_lur_sound.length)];
    }

    public @NonNull Audio getBlastRumbleSound() {
        return blast_rumble_sound;
    }

    public @NonNull Audio getBlastBlastSound() {
        return blast_blast_sound;
    }

    public @NonNull Audio getArmorySound() {
        return armory_sound;
    }

    public @NonNull Audio getBuildingCollapseSound() {
        return building_collapse_sound;
    }

    /** The snare a Chicken Catcher or Fowler lays (Buffed), for {@code race}. */
    public @NonNull String getSnareName() {
        return snare_name;
    }

    public @NonNull SpriteKey getSnareSprite(int race) {
        return snare_sprites[race];
    }

    public @NonNull Race getRace(int i) {
        return races[i];
    }

    public static @NonNull String getRaceName(int i) {
        return race_names[i];
    }

    public static int getNumRaces() {
        return race_names.length;
    }

    public @NonNull SpriteKey @NonNull [] getWoodFragments() {
        return wood_fragment_sprites;
    }

    public @NonNull SpriteKey @NonNull [] getTreasures() {
        return treasure_sprites;
    }
}
