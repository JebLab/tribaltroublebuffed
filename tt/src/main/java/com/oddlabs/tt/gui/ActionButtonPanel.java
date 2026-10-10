package com.oddlabs.tt.gui;

import com.oddlabs.tt.animation.Animated;
import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.delegate.CameraDelegate;
import com.oddlabs.tt.delegate.PlacingDelegate;
import com.oddlabs.tt.delegate.RallyPointDelegate;
import com.oddlabs.tt.delegate.TargetDelegate;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.Action;
import com.oddlabs.tt.model.Building;
import com.oddlabs.tt.model.LandBuilding;
import com.oddlabs.tt.model.Lodge;
import com.oddlabs.tt.model.Market;
import com.oddlabs.tt.model.Ship;
import com.oddlabs.tt.model.DeployType;
import com.oddlabs.tt.model.IronSupply;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.RockSupply;
import com.oddlabs.tt.model.RubberSupply;
import com.oddlabs.tt.model.SupplyCounter;
import com.oddlabs.tt.model.Unit;
import com.oddlabs.tt.model.WallLine;
import com.oddlabs.tt.model.behaviour.RepairBehaviour;
import com.oddlabs.tt.model.weapon.Champion;
import com.oddlabs.tt.model.weapon.IronAxeWeapon;
import com.oddlabs.tt.model.weapon.RockAxeWeapon;
import com.oddlabs.tt.model.weapon.RubberAxeWeapon;
import com.oddlabs.tt.model.weapon.Shield;
import com.oddlabs.tt.model.weapon.Drum;
import com.oddlabs.tt.model.weapon.Net;
import com.oddlabs.tt.model.weapon.Torch;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInterface;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.ruleset.RulesetStats;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.Selection;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

public final class ActionButtonPanel extends GUIObject implements Animated {
    private static final int GROUP_LEFT_OFFSET = 10;
    private static final int GROUP_BOTTOM_OFFSET = 10;
    private static final int GROUP_RIGHT_OFFSET = 10;
    private static final int GROUP_TOP_OFFSET = 20;

    // Every action handled inside the armory submenus: the spinner rows and the back button.
    // A key bound to any of these keeps that meaning while a submenu is open, even in a
    // submenu without that row, so it never doubles as a submenu switch (see canSwitchSubmenu).
    private static final Set<GameAction> ARMORY_SUBMENU_ACTIONS = EnumSet.of(GameAction.RES_TREE,
            GameAction.RES_TREE_DEC, GameAction.RES_TREE_BATCH, GameAction.RES_TREE_BATCH_DEC, GameAction.RES_ROCK,
            GameAction.RES_ROCK_DEC, GameAction.RES_ROCK_BATCH, GameAction.RES_ROCK_BATCH_DEC, GameAction.RES_IRON,
            GameAction.RES_IRON_DEC, GameAction.RES_IRON_BATCH, GameAction.RES_IRON_BATCH_DEC, GameAction.RES_CHICKEN,
            GameAction.RES_CHICKEN_DEC, GameAction.RES_CHICKEN_BATCH, GameAction.RES_CHICKEN_BATCH_DEC,
            GameAction.RES_SHIELD, GameAction.RES_SHIELD_DEC, GameAction.RES_SHIELD_BATCH,
            GameAction.RES_SHIELD_BATCH_DEC,
            GameAction.RES_TORCH, GameAction.RES_TORCH_DEC, GameAction.RES_TORCH_BATCH, GameAction.RES_TORCH_BATCH_DEC,
            GameAction.RES_DRUM, GameAction.RES_DRUM_DEC, GameAction.RES_DRUM_BATCH, GameAction.RES_DRUM_BATCH_DEC,
            GameAction.RES_NET, GameAction.RES_NET_DEC, GameAction.RES_NET_BATCH, GameAction.RES_NET_BATCH_DEC,
            GameAction.TRAIN_PEON, GameAction.TRAIN_PEON_DEC, GameAction.TRAIN_PEON_BATCH,
            GameAction.TRAIN_PEON_BATCH_DEC, GameAction.GAMEPLAY_BACK);

    private final Group unit_group = new NonFocusGroup();
    private final Group peon_group = new NonFocusGroup();
    private final Group chieftain_group = new NonFocusGroup();
    private final Group tower_group = new NonFocusGroup();
    private final Group quarters_status_group = new NonFocusGroup();
    private final Group quarters_group = new NonFocusGroup();
    private final Group status_group = new NonFocusGroup();
    private final Group armory_group = new NonFocusGroup();
    private final Group ship_group = new NonFocusGroup();
    private final Group harvest_group = new NonFocusGroup();
    private final Group build_group = new NonFocusGroup();
    private final Group army_group = new NonFocusGroup();
    private final Group ship_army_group = new NonFocusGroup();
    private final Group transport_group = new NonFocusGroup();
    private final Group chicken_coop_status_group = new NonFocusGroup();
    private final Group gear_status_group = new NonFocusGroup();
    private final Group market_status_group = new NonFocusGroup();
    private final Group market_group = new NonFocusGroup();
    private final Group lodge_status_group = new NonFocusGroup();
    private final Group lodge_group = new NonFocusGroup();

    private final @NonNull NonFocusIconButton tower_attack_button;
    private final @NonNull NonFocusIconButton tower_exit_button;
    //	private boolean tower_exit_button_disabled;
    private final @NonNull NonFocusIconButton move_button;
    private final @NonNull NonFocusIconButton attack_button;
    private final @NonNull NonFocusIconButton gather_repair_button;
    private final @NonNull NonFocusIconButton quarters_button;
    //	private boolean quarters_button_disabled;
    private final @NonNull RechargeButton magic1_button;
    private final @NonNull RechargeButton magic2_button;
    private final @NonNull NonFocusIconButton armory_button;
    //	private boolean armory_button_disabled;
    private final @NonNull NonFocusIconButton tower_button;
    //	private boolean tower_button_disabled;
    private final @NonNull NonFocusIconButton ship_button;
    // Buffed's buildings: the buttons exist under every ruleset but are shown only where it offers them.
    private final @NonNull NonFocusIconButton chicken_coop_button;
    private final @NonNull NonFocusIconButton totem_button;
    private final boolean chicken_coop_enabled;
    private final boolean totem_enabled;
    private final @NonNull StatusIcon chicken_coop_stock_status;
    // Buffed's Market and walls (M7). A Market's panel: its peons, what it sells and buys (a click moves to the next
    // resource; only the button of the current one shows), and a rally point.
    private final @NonNull NonFocusIconButton market_button;
    private final @NonNull NonFocusIconButton palisade_button;
    private final @NonNull NonFocusIconButton gate_button;
    private final boolean market_enabled;
    private final boolean palisade_enabled;
    private final @NonNull StatusIcon market_unit_status;
    private final @NonNull DeploySpinner market_peon_button;
    private final @NonNull NonFocusIconButton @NonNull [] market_sell_buttons = new NonFocusIconButton[Market.numResources()];
    private final @NonNull NonFocusIconButton @NonNull [] market_buy_buttons = new NonFocusIconButton[Market.numResources()];
    private final @NonNull NonFocusIconButton market_rally_point_button;
    private int shown_sell = -1;
    private int shown_buy = -1;
    // Buffed's Great Tower and Lodge (M8). A Lodge's panel: the units sheltered and the Champions alive, a spinner
    // that lets the units out, the Champion spinner, and a rally point.
    private final @NonNull NonFocusIconButton great_tower_button;
    private final @NonNull NonFocusIconButton lodge_button;
    private final boolean great_tower_enabled;
    private final boolean lodge_enabled;
    private final @NonNull StatusIcon lodge_unit_status;
    private final @NonNull StatusIcon lodge_champion_status;
    private final @NonNull DeploySpinner lodge_leave_button;
    private final @NonNull BuildSpinner lodge_champion_button;
    private final @NonNull NonFocusIconButton lodge_rally_point_button;
    // Buffed's gear: a second column in the Armory's weapon and army submenus, and its stock beside the status.
    private final boolean shield_enabled;
    private final boolean torch_enabled;
    private final @NonNull StatusIcon weapon_shield_status;
    private final @NonNull StatusIcon weapon_torch_status;
    private final @NonNull BuildSpinner build_weapon_shield_button;
    private final @NonNull BuildSpinner build_weapon_torch_button;
    private final @NonNull DeploySpinner army_warrior_shield_button;
    private final @NonNull DeploySpinner army_warrior_torch_button;
    // Buffed's Drum and Net (M9): the rest of the gear column; a chicken catcher's Lay Snare button, left of Move.
    private final boolean drum_enabled;
    private final boolean net_enabled;
    private final @NonNull StatusIcon weapon_drum_status;
    private final @NonNull StatusIcon weapon_net_status;
    private final @NonNull BuildSpinner build_weapon_drum_button;
    private final @NonNull BuildSpinner build_weapon_net_button;
    private final @NonNull DeploySpinner army_warrior_drum_button;
    private final @NonNull DeploySpinner army_warrior_net_button;
    private final Group catcher_group = new NonFocusGroup();
    private final @NonNull NonFocusIconButton lay_snare_button;
    private boolean current_catcher;
    private final @NonNull NonFocusIconButton harvest_button;
    private final @NonNull NonFocusIconButton build_button;
    private final @NonNull NonFocusIconButton army_button;
    private final @NonNull NonFocusIconButton transport_button;
    private final @NonNull NonFocusIconButton rally_point_button;
    private final @NonNull NonFocusIconButton ship_harvest_button;
    private final @NonNull NonFocusIconButton ship_army_button;
    private final @NonNull NonFocusIconButton ship_rally_point_button;
    private final @NonNull NonFocusIconButton ship_transport_button;
    private final @NonNull NonFocusIconButton ship_sail_button;


    private final @NonNull StatusIcon unit_status;
    private final @NonNull StatusIcon weapon_rock_status;
    private final @NonNull StatusIcon weapon_iron_status;
    private final @NonNull StatusIcon weapon_rubber_status;
    private final @NonNull StatusIcon tree_status;
    private final @NonNull StatusIcon rock_status;
    private final @NonNull StatusIcon iron_status;
    private final @NonNull StatusIcon rubber_status;

    private final @NonNull WatchStatusIcon quarters_unit_status;
    private final @NonNull DeploySpinner quarters_peon_button;
    private final @NonNull ChieftainButton quarters_chieftain_button;
    private final @NonNull NonFocusIconButton quarters_rally_point_button;

    private final @NonNull DeploySpinner harvest_tree_button;
    private final @NonNull DeploySpinner harvest_rock_button;
    private final @NonNull DeploySpinner harvest_iron_button;
    private final @NonNull DeploySpinner harvest_rubber_button;
    private final @NonNull NonFocusIconButton harvest_back_button;

    private final @NonNull BuildSpinner build_weapon_rock_button;
    private final @NonNull BuildSpinner build_weapon_iron_button;
    private final @NonNull BuildSpinner build_weapon_rubber_button;
    private final @NonNull NonFocusIconButton build_back_button;

    private final @NonNull DeploySpinner army_peon_button;
    private final @NonNull DeploySpinner army_warrior_rock_button;
    private final @NonNull DeploySpinner army_warrior_iron_button;
    private final @NonNull DeploySpinner army_warrior_rubber_button;
    private final @NonNull NonFocusIconButton army_back_button;

    private final @NonNull DeploySpinner ship_army_peon_button;
    private final @NonNull DeploySpinner ship_army_warrior_rock_button;
    private final @NonNull DeploySpinner ship_army_warrior_iron_button;
    private final @NonNull DeploySpinner ship_army_warrior_rubber_button;
    private final @NonNull NonFocusIconButton ship_army_chieftain_button;
    private final @NonNull NonFocusIconButton ship_army_back_button;

    private final @NonNull DeploySpinner transport_tree_button;
    private final @NonNull DeploySpinner transport_rock_button;
    private final @NonNull DeploySpinner transport_iron_button;
    private final @NonNull DeploySpinner transport_rubber_button;
    private final @NonNull NonFocusIconButton transport_back_button;
    private static final ResourceBundle bundle = ResourceBundle.getBundle(ActionButtonPanel.class.getName());

    public static @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private static @NonNull String getBinding(@NonNull GameAction action) {
        return Renderer.getLocalInput().getInputManager().getBindingString(action);
    }

    private final @NonNull GameCamera camera;
    private final @NonNull WorldViewer viewer;
    private final @NonNull Player player;
    private final @NonNull Selection selection;
    private final boolean read_only;

    private @Nullable Group current_submenu = null;
    private boolean update = false;
    private boolean current_quarters = false;
    private boolean current_armory = false;
    private boolean current_ship = false;
    private @Nullable Building current_building;
    private boolean current_unit = false;
    private boolean current_peon = false;
    private @Nullable Unit current_chieftain;
    private boolean current_tower = false;
    private boolean current_chicken_coop = false;
    private boolean current_market = false;
    private boolean current_lodge = false;
//	private boolean[] magic_disabled = new boolean[2];

    public ActionButtonPanel(@NonNull WorldViewer viewer, @NonNull GameCamera camera) {
        this(viewer, camera, viewer.getGUIRoot().getWidth(), viewer.getGUIRoot().getHeight());
    }

    public ActionButtonPanel(final @NonNull WorldViewer viewer, @NonNull GameCamera camera, int width, int height) {
        this(viewer, camera, width, height, viewer.getLocalPlayer(), viewer.getSelection(), false);
    }

    /** The panel for one player's selection; read only means it shows the selection but takes no input. */
    public ActionButtonPanel(final @NonNull WorldViewer viewer, @NonNull GameCamera camera, int width, int height,
            final @NonNull Player player, @NonNull Selection selection, boolean read_only) {
        this.viewer = viewer;
        this.camera = camera;
        this.player = player;
        this.selection = selection;
        this.read_only = read_only;
        RaceIcons race_icons = player.getRace().getIcons();
        Skin skin = Skin.getSkin();
        GUIIcons icons = GUIIcons.getIcons();
        String widest_char = new String(Character.toChars(skin.getEditFont().getWidestCodepoint("0123456789")));
        int label_width = skin.getEditFont().getWidth(widest_char + widest_char + widest_char);

        move_button = new NonFocusIconButton(race_icons.moveIcon(), GameAction.UNIT_MOVE,
                () -> i18n("move_tip", getBinding(GameAction.UNIT_MOVE)));
        move_button.setIconDisabler(() -> !player.canMove());
        unit_group.addChild(move_button);
        move_button.addMouseClickListener((_, _, _, _) -> pushDelegate(new TargetDelegate(viewer, camera,
                Action.MOVE)));
        attack_button = new NonFocusIconButton(race_icons.attackIcon(), GameAction.UNIT_ATTACK,
                () -> i18n("attack_tip", getBinding(GameAction.UNIT_ATTACK)));
        attack_button.setIconDisabler(() -> !player.canAttack());
        unit_group.addChild(attack_button);
        attack_button.addMouseClickListener((_, _, _, _) -> pushDelegate(new TargetDelegate(viewer, camera,
                Action.ATTACK)));
        move_button.place();
        attack_button.place(move_button, Placement.BOTTOM_MID);
        unit_group.compileCanvas(GROUP_LEFT_OFFSET, 0, GROUP_RIGHT_OFFSET, GROUP_BOTTOM_OFFSET);

        gather_repair_button = new NonFocusIconButton(race_icons.gatherRepairIcon(), GameAction.UNIT_GATHER,
                () -> i18n("gather_repair_tip", getBinding(GameAction.UNIT_GATHER)));
        peon_group.addChild(gather_repair_button);
        gather_repair_button.addMouseClickListener((_, _, _, _) -> pushDelegate(new TargetDelegate(viewer, camera,
                Action.GATHER_REPAIR)));
        gather_repair_button.setIconDisabler(() -> !player.canRepair());
        quarters_button = new NonFocusIconButton(race_icons.quartersIcon(), GameAction.UNIT_BUILD_QUARTERS,
                () -> i18n("quarters_tip", getBinding(GameAction.UNIT_BUILD_QUARTERS)));
        peon_group.addChild(quarters_button);
        quarters_button.addMouseClickListener((_, _, _, _) -> pushDelegate(new PlacingDelegate(viewer,
                camera.getState(), Race.BUILDING_QUARTERS)));
        quarters_button.setIconDisabler(() -> !player.canBuild(Race.BUILDING_QUARTERS));
        armory_button = new NonFocusIconButton(race_icons.armoryIcon(), GameAction.UNIT_BUILD_ARMORY,
                () -> i18n("armory_tip", getBinding(GameAction.UNIT_BUILD_ARMORY)));
        peon_group.addChild(armory_button);
        armory_button.addMouseClickListener((_, _, _, _) -> pushDelegate(new PlacingDelegate(viewer, camera.getState(),
                Race.BUILDING_ARMORY)));
        armory_button.setIconDisabler(() -> !player.canBuild(Race.BUILDING_ARMORY));
        tower_button = new NonFocusIconButton(race_icons.towerIcon(), GameAction.UNIT_BUILD_TOWER,
                () -> i18n("tower_tip", getBinding(GameAction.UNIT_BUILD_TOWER)));
        peon_group.addChild(tower_button);
        tower_button.addMouseClickListener((_, _, _, _) -> pushDelegate(new PlacingDelegate(viewer, camera.getState(),
                Race.BUILDING_TOWER)));
        tower_button.setIconDisabler(() -> !player.canBuild(Race.BUILDING_TOWER));

        ship_button = new NonFocusIconButton(race_icons.shipIcon(), GameAction.UNIT_BUILD_SHIP, () -> i18n("ship_tip",
                getBinding(GameAction.UNIT_BUILD_SHIP)));
        if (viewer.getWorld().isShipsEnabled()) {
            peon_group.addChild(ship_button);
            ship_button.addMouseClickListener((_, _, _, _) -> pushDelegate(new PlacingDelegate(viewer,
                    camera.getState(),
                    Race.BUILDING_SHIP)));
            ship_button.setIconDisabler(() -> !player.canBuild(Race.BUILDING_SHIP));
        }

        RulesetStats stats = viewer.getWorld().getRuleset().getStats();
        RulesetStats.RaceStats race_stats = stats.race(
                player.getPlayerInfo().getRace() == RacesResources.RACE_VIKINGS);
        chicken_coop_enabled = stats.features().chicken_coop();
        totem_enabled = stats.features().totem();
        shield_enabled = stats.features().shield();
        torch_enabled = stats.features().torch();
        drum_enabled = stats.features().drum();
        net_enabled = stats.features().net();
        lay_snare_button = new NonFocusIconButton(race_icons.laySnareIcon(), GameAction.UNIT_LAY_SNARE,
                () -> i18n("lay_snare_tip", getBinding(GameAction.UNIT_LAY_SNARE), race_stats.net().snares(),
                        race_stats.net().stun_seconds()));
        catcher_group.addChild(lay_snare_button);
        lay_snare_button.addMouseClickListener((_, _, _, _) -> pushDelegate(new TargetDelegate(viewer, camera,
                Action.SNARE)));
        lay_snare_button.place();
        catcher_group.compileCanvas(GROUP_LEFT_OFFSET, 0, 0, GROUP_BOTTOM_OFFSET);
        String chicken_coop_name = player.getRace().getBuildingTemplate(Race.BUILDING_CHICKEN_COOP).getName();
        chicken_coop_button = new NonFocusIconButton(race_icons.chickenCoopIcon(), GameAction.UNIT_BUILD_CHICKEN_COOP,
                () -> i18n("chicken_coop_tip", chicken_coop_name, getBinding(GameAction.UNIT_BUILD_CHICKEN_COOP),
                        race_stats.chicken_coop().stock()));
        if (chicken_coop_enabled) {
            peon_group.addChild(chicken_coop_button);
            chicken_coop_button.addMouseClickListener((_, _, _, _) -> pushDelegate(new PlacingDelegate(viewer,
                    camera.getState(), Race.BUILDING_CHICKEN_COOP)));
            chicken_coop_button.setIconDisabler(() -> !player.canBuild(Race.BUILDING_CHICKEN_COOP));
        }
        String totem_name = player.getRace().getBuildingTemplate(Race.BUILDING_TOTEM).getName();
        totem_button = new NonFocusIconButton(race_icons.totemIcon(), GameAction.UNIT_BUILD_TOTEM,
                () -> i18n("totem_tip", totem_name, getBinding(GameAction.UNIT_BUILD_TOTEM)));
        if (totem_enabled) {
            peon_group.addChild(totem_button);
            totem_button.addMouseClickListener((_, _, _, _) -> pushDelegate(new PlacingDelegate(viewer,
                    camera.getState(), Race.BUILDING_TOTEM)));
            totem_button.setIconDisabler(() -> !player.canBuild(Race.BUILDING_TOTEM));
        }
        market_enabled = stats.features().market();
        palisade_enabled = stats.features().palisade();
        String market_name = player.getRace().getBuildingTemplate(Race.BUILDING_MARKET).getName();
        market_button = new NonFocusIconButton(race_icons.marketIcon(), GameAction.UNIT_BUILD_MARKET,
                () -> i18n("market_tip", market_name, getBinding(GameAction.UNIT_BUILD_MARKET),
                        race_stats.market().give(), race_stats.market().get()));
        if (market_enabled) {
            peon_group.addChild(market_button);
            market_button.addMouseClickListener((_, _, _, _) -> pushDelegate(new PlacingDelegate(viewer,
                    camera.getState(), Race.BUILDING_MARKET)));
            market_button.setIconDisabler(() -> !player.canBuild(Race.BUILDING_MARKET));
        }
        String palisade_name = player.getRace().getBuildingTemplate(Race.BUILDING_PALISADE).getName();
        palisade_button = new NonFocusIconButton(race_icons.palisadeIcon(), GameAction.UNIT_BUILD_PALISADE,
                () -> i18n("palisade_tip", palisade_name, getBinding(GameAction.UNIT_BUILD_PALISADE),
                        WallLine.MAX_SEGMENTS));
        String gate_name = player.getRace().getBuildingTemplate(Race.BUILDING_GATE).getName();
        gate_button = new NonFocusIconButton(race_icons.gateIcon(), GameAction.UNIT_BUILD_GATE,
                () -> i18n("gate_tip", gate_name, getBinding(GameAction.UNIT_BUILD_GATE)));
        if (palisade_enabled) {
            peon_group.addChild(palisade_button);
            palisade_button.addMouseClickListener((_, _, _, _) -> pushDelegate(new PlacingDelegate(viewer,
                    camera.getState(), Race.BUILDING_PALISADE)));
            palisade_button.setIconDisabler(() -> !player.canBuild(Race.BUILDING_PALISADE));
            peon_group.addChild(gate_button);
            gate_button.addMouseClickListener((_, _, _, _) -> pushDelegate(new PlacingDelegate(viewer,
                    camera.getState(), Race.BUILDING_GATE)));
            gate_button.setIconDisabler(() -> !player.canBuild(Race.BUILDING_GATE));
        }
        great_tower_enabled = stats.features().great_tower();
        lodge_enabled = stats.features().lodge();
        String great_tower_name = player.getRace().getBuildingTemplate(Race.BUILDING_GREAT_TOWER).getName();
        great_tower_button = new NonFocusIconButton(race_icons.greatTowerIcon(), GameAction.UNIT_BUILD_GREAT_TOWER,
                () -> i18n("great_tower_tip", great_tower_name, getBinding(GameAction.UNIT_BUILD_GREAT_TOWER),
                        race_stats.great_tower().throwers(),
                        race_stats.great_tower().hit_points() / RepairBehaviour.REPAIRS_PER_SUPPLY - race_stats.great_tower().rock(),
                        race_stats.great_tower().rock()));
        if (great_tower_enabled) {
            peon_group.addChild(great_tower_button);
            great_tower_button.addMouseClickListener((_, _, _, _) -> pushDelegate(new PlacingDelegate(viewer,
                    camera.getState(), Race.BUILDING_GREAT_TOWER)));
            great_tower_button.setIconDisabler(() -> !player.canBuild(Race.BUILDING_GREAT_TOWER));
        }
        String lodge_name = player.getRace().getBuildingTemplate(Race.BUILDING_LODGE).getName();
        String champion_name = player.getRace().getUnitTemplate(Race.UNIT_CHAMPION).getName();
        lodge_button = new NonFocusIconButton(race_icons.lodgeIcon(), GameAction.UNIT_BUILD_LODGE,
                () -> i18n("lodge_tip", lodge_name, getBinding(GameAction.UNIT_BUILD_LODGE),
                        race_stats.lodge().shelter(), champion_name,
                        race_stats.lodge().hit_points() / RepairBehaviour.REPAIRS_PER_SUPPLY - race_stats.lodge().iron(),
                        race_stats.lodge().iron()));
        if (lodge_enabled) {
            peon_group.addChild(lodge_button);
            lodge_button.addMouseClickListener((_, _, _, _) -> pushDelegate(new PlacingDelegate(viewer,
                    camera.getState(), Race.BUILDING_LODGE)));
            lodge_button.setIconDisabler(() -> !player.canBuild(Race.BUILDING_LODGE));
        }

        gather_repair_button.place();
        quarters_button.place(gather_repair_button, Placement.BOTTOM_MID);
        armory_button.place(quarters_button, Placement.BOTTOM_MID);
        tower_button.place(armory_button, Placement.BOTTOM_MID);
        if (viewer.getWorld().isShipsEnabled()) {
            ship_button.place(tower_button, Placement.BOTTOM_MID);
        }
        // A second column, so the peon buttons do not grow taller than the screen.
        if (chicken_coop_enabled) {
            chicken_coop_button.place(quarters_button, Placement.LEFT_MID);
        }
        if (totem_enabled) {
            totem_button.place(armory_button, Placement.LEFT_MID);
        }
        if (market_enabled) {
            market_button.place(gather_repair_button, Placement.LEFT_MID);
        }
        if (palisade_enabled) {
            palisade_button.place(tower_button, Placement.LEFT_MID);
            gate_button.place(palisade_button, Placement.BOTTOM_MID);
        }
        // A third column for the late buildings, left of the Market and the coop's column.
        if (great_tower_enabled) {
            great_tower_button.place(market_enabled ? market_button : gather_repair_button, Placement.LEFT_MID);
        }
        if (lodge_enabled) {
            lodge_button.place(chicken_coop_enabled ? chicken_coop_button : quarters_button, Placement.LEFT_MID);
        }
        peon_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, 0);

        PlayerInterface player_interface = viewer.getPeerHub().getPlayerInterface();
        magic1_button = new RechargeButton(player_interface, race_icons.magic1Icon(), GameAction.MAGIC_1,
                race_icons.magic1Desc(), 0);
        chieftain_group.addChild(magic1_button);
//		magic1_button.addMouseClickListener(new MagicListener(0));
        magic2_button = new RechargeButton(player_interface, race_icons.magic2Icon(), GameAction.MAGIC_2,
                race_icons.magic2Desc(), 1);
        chieftain_group.addChild(magic2_button);
//		magic2_button.addMouseClickListener(new MagicListener(1));
        magic1_button.place();
        magic2_button.place(magic1_button, Placement.BOTTOM_MID);
        chieftain_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, 0);

        tower_attack_button = new NonFocusIconButton(race_icons.attackIcon(), GameAction.UNIT_ATTACK,
                () -> i18n("attack_tip", getBinding(GameAction.UNIT_ATTACK)));
        tower_group.addChild(tower_attack_button);
        tower_attack_button.addMouseClickListener((_, _, _, _) -> pushDelegate(new TargetDelegate(viewer, camera,
                Action.ATTACK)));
        tower_exit_button = new NonFocusIconButton(race_icons.towerExitIcon(), GameAction.UNIT_EXIT_TOWER,
                () -> i18n("exit_tip", getBinding(GameAction.UNIT_EXIT_TOWER)));
        tower_group.addChild(tower_exit_button);
        tower_exit_button.addMouseClickListener((_, _, _, _) -> {
            if (current_building != null && !current_building.isDead())
                viewer.getPeerHub().getPlayerInterface().exitTower(current_building);
            removeGroups();
            update = true;
        });
        tower_attack_button.place();
        tower_exit_button.place(tower_attack_button, Placement.BOTTOM_MID);
        tower_group.compileCanvas();

        unit_status = new StatusIcon(label_width, race_icons.unitStatusIcon(), i18n("units_tip"));
        status_group.addChild(unit_status);
        weapon_rock_status = new StatusIcon(label_width, race_icons.weaponRockStatusIcon(), i18n("rock_weapons_tip"));
        status_group.addChild(weapon_rock_status);
        weapon_iron_status = new StatusIcon(label_width, race_icons.weaponIronStatusIcon(), i18n("iron_weapons_tip"));
        status_group.addChild(weapon_iron_status);
        weapon_rubber_status = new StatusIcon(label_width, race_icons.weaponRubberStatusIcon(), i18n(
                "chicken_weapons_tip"));
        status_group.addChild(weapon_rubber_status);
        tree_status = new StatusIcon(label_width, icons.getTreeStatusIcon(), i18n("tree_resources_tip"));
        status_group.addChild(tree_status);
        rock_status = new StatusIcon(label_width, icons.getRockStatusIcon(), i18n("rock_resources_tip"));
        status_group.addChild(rock_status);
        iron_status = new StatusIcon(label_width, icons.getIronStatusIcon(), i18n("iron_resources_tip"));
        status_group.addChild(iron_status);
        rubber_status = new StatusIcon(label_width, icons.getRubberStatusIcon(), i18n("chicken_resources_tip"));
        status_group.addChild(rubber_status);
        unit_status.place();
        weapon_rock_status.place(unit_status, Placement.BOTTOM_MID);
        weapon_iron_status.place(weapon_rock_status, Placement.BOTTOM_MID);
        weapon_rubber_status.place(weapon_iron_status, Placement.BOTTOM_MID);
        tree_status.place(unit_status, Placement.LEFT_MID, 5);
        rock_status.place(tree_status, Placement.BOTTOM_MID);
        iron_status.place(rock_status, Placement.BOTTOM_MID);
        rubber_status.place(iron_status, Placement.BOTTOM_MID);
        status_group.compileCanvas(5, 5, 5, 5);

        chicken_coop_stock_status = new StatusIcon(label_width, icons.getRubberStatusIcon(), i18n("chicken_stock_tip",
                race_stats.chicken_coop().stock()));
        chicken_coop_status_group.addChild(chicken_coop_stock_status);
        chicken_coop_stock_status.place();
        chicken_coop_status_group.compileCanvas(5, 5, 5, 5);

        String shield_name = player.getRace().getUnitTemplate(Race.UNIT_WARRIOR_SHIELD).getName();
        String torch_name = player.getRace().getUnitTemplate(Race.UNIT_WARRIOR_TORCH).getName();
        weapon_shield_status = new StatusIcon(label_width, race_icons.weaponShieldStatusIcon(), i18n(
                "shield_weapons_tip"));
        weapon_torch_status = new StatusIcon(label_width, race_icons.weaponTorchStatusIcon(), i18n(
                "torch_weapons_tip"));
        if (shield_enabled) {
            gear_status_group.addChild(weapon_shield_status);
            weapon_shield_status.place();
        }
        if (torch_enabled) {
            gear_status_group.addChild(weapon_torch_status);
            if (shield_enabled)
                weapon_torch_status.place(weapon_shield_status, Placement.BOTTOM_MID);
            else
                weapon_torch_status.place();
        }
        weapon_drum_status = new StatusIcon(label_width, race_icons.weaponDrumStatusIcon(), i18n(
                player.getPlayerInfo().getRace() == RacesResources.RACE_VIKINGS ? "drum_weapons_tip_vikings" : "drum_weapons_tip_natives"));
        weapon_net_status = new StatusIcon(label_width, race_icons.weaponNetStatusIcon(), i18n("net_weapons_tip"));
        StatusIcon last_gear_status = torch_enabled ? weapon_torch_status : shield_enabled ? weapon_shield_status : null;
        if (drum_enabled) {
            gear_status_group.addChild(weapon_drum_status);
            if (last_gear_status != null)
                weapon_drum_status.place(last_gear_status, Placement.BOTTOM_MID);
            else
                weapon_drum_status.place();
            last_gear_status = weapon_drum_status;
        }
        if (net_enabled) {
            gear_status_group.addChild(weapon_net_status);
            if (last_gear_status != null)
                weapon_net_status.place(last_gear_status, Placement.BOTTOM_MID);
            else
                weapon_net_status.place();
        }
        if (anyGear())
            gear_status_group.compileCanvas(5, 5, 5, 5);

        quarters_unit_status = new WatchStatusIcon(label_width, race_icons.unitStatusIcon(), i18n("units_tip"));
        quarters_status_group.addChild(quarters_unit_status);
        quarters_unit_status.place();
        quarters_status_group.compileCanvas(5, 5, 5, 5);

        quarters_peon_button = new DeploySpinner(viewer, player_interface, race_icons.peonIcon(), i18n(
                "deploy_peon_tip"),
                List.of(race_icons.unitStatusIcon()), GameAction.TRAIN_PEON, GameAction.TRAIN_PEON_DEC, null, null);
        quarters_group.addChild(quarters_peon_button);
        quarters_chieftain_button = new ChieftainButton(viewer, player_interface, race_icons.chieftainIcon());
//		if (Settings.getSettings().developer_mode) {
        quarters_group.addChild(quarters_chieftain_button);
//		}
        quarters_rally_point_button = new NonFocusIconButton(race_icons.rallyPointIcon(), GameAction.UNIT_SET_RALLY,
                () -> i18n("rally_point_tip", getBinding(GameAction.UNIT_SET_RALLY)));
        quarters_group.addChild(quarters_rally_point_button);
        quarters_rally_point_button.addMouseClickListener(this::setRallyPoint);
        quarters_peon_button.place();
//		if (Settings.getSettings().developer_mode) {
        quarters_chieftain_button.place(quarters_peon_button, Placement.BOTTOM_MID);
        quarters_rally_point_button.place(quarters_chieftain_button, Placement.BOTTOM_MID);
//		} else {
//			quarters_rally_point_button.place(quarters_peon_button, Placement.Placement.BOTTOM_MID);
//		}
        quarters_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, GROUP_TOP_OFFSET);

        market_unit_status = new StatusIcon(label_width, race_icons.unitStatusIcon(), i18n("market_units_tip"));
        market_status_group.addChild(market_unit_status);
        market_unit_status.place();
        market_status_group.compileCanvas(5, 5, 5, 5);

        market_peon_button = new DeploySpinner(viewer, player_interface, race_icons.peonIcon(), i18n(
                "deploy_peon_tip"), List.of(race_icons.unitStatusIcon()), GameAction.TRAIN_PEON,
                GameAction.TRAIN_PEON_DEC, null, null);
        market_group.addChild(market_peon_button);
        market_peon_button.place();
        ModeIconQuads[] resource_icons = {icons.getTreeIcon(), icons.getRockIcon(), icons.getIronIcon(), icons.getRubberIcon()};
        String[] resource_names = {i18n("harvest_tree_tip"), i18n("harvest_rock_tip"), i18n("harvest_iron_tip"), i18n(
                "harvest_chicken_tip")};
        for (int i = 0; i < Market.numResources(); i++) {
            String resource_name = resource_names[i];
            market_sell_buttons[i] = new NonFocusIconButton(resource_icons[i], GameAction.MARKET_SELL,
                    () -> i18n("market_sell_tip", resource_name, race_stats.market().give(),
                            getBinding(GameAction.MARKET_SELL)));
            market_sell_buttons[i].addMouseClickListener((_, _, _, _) -> cycleTrade(true));
            market_group.addChild(market_sell_buttons[i]);
            market_sell_buttons[i].place(market_peon_button, Placement.BOTTOM_MID);
            market_buy_buttons[i] = new NonFocusIconButton(resource_icons[i], GameAction.MARKET_BUY,
                    () -> i18n("market_buy_tip", resource_name, race_stats.market().get(),
                            getBinding(GameAction.MARKET_BUY)));
            market_buy_buttons[i].addMouseClickListener((_, _, _, _) -> cycleTrade(false));
            market_group.addChild(market_buy_buttons[i]);
            market_buy_buttons[i].place(market_sell_buttons[0], Placement.BOTTOM_MID);
        }
        market_rally_point_button = new NonFocusIconButton(race_icons.rallyPointIcon(), GameAction.UNIT_SET_RALLY,
                () -> i18n("rally_point_tip", getBinding(GameAction.UNIT_SET_RALLY)));
        market_group.addChild(market_rally_point_button);
        market_rally_point_button.addMouseClickListener(this::setRallyPoint);
        market_rally_point_button.place(market_buy_buttons[0], Placement.BOTTOM_MID);
        market_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, GROUP_TOP_OFFSET);
        removeTradeButtons();

        lodge_unit_status = new StatusIcon(label_width, race_icons.unitStatusIcon(), i18n("lodge_units_tip"));
        lodge_status_group.addChild(lodge_unit_status);
        lodge_unit_status.place();
        lodge_champion_status = new StatusIcon(label_width, race_icons.championIcon().quad(ModeIconQuads.Mode.NORMAL),
                i18n(
                        "lodge_champions_tip", champion_name));
        lodge_status_group.addChild(lodge_champion_status);
        lodge_champion_status.place(lodge_unit_status, Placement.BOTTOM_MID);
        lodge_status_group.compileCanvas(5, 5, 5, 5);

        lodge_leave_button = new DeploySpinner(viewer, player_interface, race_icons.towerExitIcon(), i18n(
                "lodge_leave_tip"), List.of(race_icons.unitStatusIcon()), GameAction.TRAIN_PEON,
                GameAction.TRAIN_PEON_DEC, null, null);
        lodge_group.addChild(lodge_leave_button);
        lodge_leave_button.place();
        List<IconQuad> champion_cost = new ArrayList<>(List.of(race_icons.unitStatusIcon()));
        champion_cost.addAll(Lodge.COST_CHAMPION.iconList());
        lodge_champion_button = new BuildSpinner(viewer, player_interface, race_icons.championIcon(), i18n(
                "train_champion_tip", champion_name, race_stats.lodge().max_champions()), champion_cost,
                GameAction.TRAIN_CHAMPION, GameAction.TRAIN_CHAMPION_DEC);
        lodge_group.addChild(lodge_champion_button);
        lodge_champion_button.place(lodge_leave_button, Placement.BOTTOM_MID);
        lodge_rally_point_button = new NonFocusIconButton(race_icons.rallyPointIcon(), GameAction.UNIT_SET_RALLY,
                () -> i18n("rally_point_tip", getBinding(GameAction.UNIT_SET_RALLY)));
        lodge_group.addChild(lodge_rally_point_button);
        lodge_rally_point_button.addMouseClickListener(this::setRallyPoint);
        lodge_rally_point_button.place(lodge_champion_button, Placement.BOTTOM_MID);
        lodge_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, GROUP_TOP_OFFSET);

        harvest_button = new NonFocusIconButton(icons.getHarvestIcon(), GameAction.PROD_HARVEST,
                () -> i18n("gather_resources_tip", getBinding(GameAction.PROD_HARVEST)));
        harvest_button.setIconDisabler(() -> !player.canHarvest());
        armory_group.addChild(harvest_button);
        harvest_button.addMouseClickListener((_, _, _, _) -> openSubmenu(harvest_group));
        build_button = new NonFocusIconButton(race_icons.buildWeaponsIcon(), GameAction.PROD_WEAPONS,
                () -> i18n("produce_weapons_tip", getBinding(GameAction.PROD_WEAPONS)));
        build_button.setIconDisabler(() -> !player.canBuildWeapons());
        armory_group.addChild(build_button);
        build_button.addMouseClickListener((_, _, _, _) -> {
            openSubmenu(build_group);
            updateCounters();
        });
        army_button = new NonFocusIconButton(race_icons.armyIcon(), GameAction.PROD_ARMY,
                () -> i18n("deploy_army_tip", getBinding(GameAction.PROD_ARMY)));
        army_button.setIconDisabler(() -> !player.canBuildArmies());
        armory_group.addChild(army_button);
        army_button.addMouseClickListener((_, _, _, _) -> openSubmenu(army_group));
        transport_button = new NonFocusIconButton(race_icons.transportIcon(), GameAction.PROD_TRANSPORT,
                () -> i18n("transport_resources_tip", getBinding(GameAction.PROD_TRANSPORT)));
        armory_group.addChild(transport_button);
        transport_button.addMouseClickListener((_, _, _, _) -> openSubmenu(transport_group));
        rally_point_button = new NonFocusIconButton(race_icons.rallyPointIcon(), GameAction.UNIT_SET_RALLY,
                () -> i18n("rally_point_tip", getBinding(GameAction.UNIT_SET_RALLY)));
        rally_point_button.setIconDisabler(() -> !player.canSetRallyPoints());
        armory_group.addChild(rally_point_button);
        rally_point_button.addMouseClickListener(this::setRallyPoint);
        harvest_button.place();
        build_button.place(harvest_button, Placement.BOTTOM_MID);
        army_button.place(build_button, Placement.BOTTOM_MID);
        transport_button.place(army_button, Placement.BOTTOM_MID);
        rally_point_button.place(transport_button, Placement.BOTTOM_MID);
        armory_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, GROUP_TOP_OFFSET);

        ship_harvest_button = new NonFocusIconButton(icons.getHarvestIcon(), GameAction.PROD_HARVEST, () -> i18n(
                "gather_resources_tip", getBinding(GameAction.PROD_HARVEST)));
        ship_group.addChild(ship_harvest_button);
        ship_harvest_button.addMouseClickListener((_, _, _, _) -> openSubmenu(harvest_group));
        ship_army_button = new NonFocusIconButton(race_icons.armyIcon(), GameAction.PROD_ARMY, () -> i18n(
                "deploy_army_tip", getBinding(GameAction.PROD_ARMY)));
        ship_group.addChild(ship_army_button);
        ship_army_button.addMouseClickListener((_, _, _, _) -> openSubmenu(ship_army_group));
        ship_transport_button = new NonFocusIconButton(race_icons.transportIcon(), GameAction.PROD_TRANSPORT,
                () -> i18n("transport_resources_tip", getBinding(GameAction.PROD_TRANSPORT)));
        ship_group.addChild(ship_transport_button);
        ship_transport_button.addMouseClickListener((_, _, _, _) -> openSubmenu(transport_group));
        ship_rally_point_button = new NonFocusIconButton(race_icons.rallyPointIcon(), GameAction.UNIT_SET_RALLY,
                () -> i18n("rally_point_tip", getBinding(GameAction.UNIT_SET_RALLY)));
        ship_group.addChild(ship_rally_point_button);
        ship_rally_point_button.addMouseClickListener(this::setRallyPoint);
        ship_harvest_button.place();
        ship_army_button.place(ship_harvest_button, Placement.BOTTOM_MID);
        ship_transport_button.place(ship_army_button, Placement.BOTTOM_MID);
        ship_rally_point_button.place(ship_transport_button, Placement.BOTTOM_MID);
        ship_sail_button = new NonFocusIconButton(race_icons.shipIcon(), GameAction.UNIT_MOVE, () -> i18n("sail_tip",
                getBinding(GameAction.UNIT_MOVE)));
        ship_group.addChild(ship_sail_button);
        ship_sail_button.addMouseClickListener((_, _, _, _) -> pushDelegate(new TargetDelegate(viewer, camera,
                Action.MOVE, true)));
        ship_sail_button.place(ship_rally_point_button, Placement.BOTTOM_MID);
        ship_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, GROUP_TOP_OFFSET);

        Player local_player = player;
        harvest_tree_button = new DeploySpinner(viewer, player_interface, icons.getTreeIcon(), i18n("harvest_tree_tip"),
                List.of(race_icons.unitStatusIcon()), GameAction.RES_TREE, GameAction.RES_TREE_DEC, local_player,
                TreeSupply.class);
        harvest_group.addChild(harvest_tree_button);
        harvest_rock_button = new DeploySpinner(viewer, player_interface, icons.getRockIcon(), i18n("harvest_rock_tip"),
                List.of(race_icons.unitStatusIcon()), GameAction.RES_ROCK, GameAction.RES_ROCK_DEC, local_player,
                RockSupply.class);
        harvest_group.addChild(harvest_rock_button);
        harvest_iron_button = new DeploySpinner(viewer, player_interface, icons.getIronIcon(), i18n("harvest_iron_tip"),
                List.of(race_icons.unitStatusIcon()), GameAction.RES_IRON, GameAction.RES_IRON_DEC, local_player,
                IronSupply.class);
        harvest_group.addChild(harvest_iron_button);
        harvest_rubber_button = new DeploySpinner(viewer, player_interface, icons.getRubberIcon(), i18n(
                "harvest_chicken_tip"), List.of(race_icons.unitStatusIcon()), GameAction.RES_CHICKEN,
                GameAction.RES_CHICKEN_DEC, local_player, RubberSupply.class);
        harvest_group.addChild(harvest_rubber_button);
        harvest_back_button = new NonFocusIconButton(skin.getBackButton(), GameAction.GAMEPLAY_BACK,
                () -> i18n("back_tip", getBinding(GameAction.GAMEPLAY_BACK)));
        harvest_back_button.addMouseClickListener(this::cancelSubMenu);
        harvest_group.addChild(harvest_back_button);
        harvest_tree_button.place();
        harvest_rock_button.place(harvest_tree_button, Placement.BOTTOM_MID);
        harvest_iron_button.place(harvest_rock_button, Placement.BOTTOM_MID);
        harvest_rubber_button.place(harvest_iron_button, Placement.BOTTOM_MID);
        harvest_back_button.place(harvest_rubber_button, Placement.BOTTOM_MID);
        harvest_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, GROUP_TOP_OFFSET);

        build_weapon_rock_button = new BuildSpinner(viewer, player_interface, race_icons.buildWeaponRockIcon(), i18n(
                "build_rock_tip"), LandBuilding.COST_ROCK_WEAPON.iconList(), GameAction.RES_ROCK,
                GameAction.RES_ROCK_DEC);
        build_group.addChild(build_weapon_rock_button);
        build_weapon_iron_button = new BuildSpinner(viewer, player_interface, race_icons.buildWeaponIronIcon(), i18n(
                "build_iron_tip"), LandBuilding.COST_IRON_WEAPON.iconList(), GameAction.RES_IRON,
                GameAction.RES_IRON_DEC);
        build_group.addChild(build_weapon_iron_button);
        build_weapon_rubber_button = new BuildSpinner(viewer, player_interface, race_icons.buildWeaponRubberIcon(),
                i18n("build_chicken_tip"), LandBuilding.COST_RUBBER_WEAPON.iconList(), GameAction.RES_CHICKEN,
                GameAction.RES_CHICKEN_DEC);
        build_group.addChild(build_weapon_rubber_button);
        build_weapon_shield_button = new BuildSpinner(viewer, player_interface, race_icons.buildWeaponShieldIcon(),
                i18n("build_shield_tip"), LandBuilding.COST_SHIELD_WEAPON.iconList(), GameAction.RES_SHIELD,
                GameAction.RES_SHIELD_DEC);
        build_weapon_torch_button = new BuildSpinner(viewer, player_interface, race_icons.buildWeaponTorchIcon(),
                i18n("build_torch_tip"), LandBuilding.COST_TORCH_WEAPON.iconList(), GameAction.RES_TORCH,
                GameAction.RES_TORCH_DEC);
        if (shield_enabled)
            build_group.addChild(build_weapon_shield_button);
        if (torch_enabled)
            build_group.addChild(build_weapon_torch_button);
        build_weapon_drum_button = new BuildSpinner(viewer, player_interface, race_icons.buildWeaponDrumIcon(),
                i18n("build_drum_tip", player.getRace().getUnitTemplate(Race.UNIT_WARRIOR_DRUM).getName(),
                        race_stats.drum().radius()), LandBuilding.COST_DRUM_WEAPON.iconList(), GameAction.RES_DRUM,
                GameAction.RES_DRUM_DEC);
        build_weapon_net_button = new BuildSpinner(viewer, player_interface, race_icons.buildWeaponNetIcon(),
                i18n("build_net_tip", player.getRace().getUnitTemplate(Race.UNIT_WARRIOR_NET).getName()),
                LandBuilding.COST_NET_WEAPON.iconList(), GameAction.RES_NET,
                GameAction.RES_NET_DEC);
        if (drum_enabled)
            build_group.addChild(build_weapon_drum_button);
        if (net_enabled)
            build_group.addChild(build_weapon_net_button);
        build_back_button = new NonFocusIconButton(skin.getBackButton(), GameAction.GAMEPLAY_BACK,
                () -> i18n("back_tip", getBinding(GameAction.GAMEPLAY_BACK)));
        build_back_button.addMouseClickListener(this::cancelSubMenu);
        build_group.addChild(build_back_button);
        build_weapon_rock_button.place();
        build_weapon_iron_button.place(build_weapon_rock_button, Placement.BOTTOM_MID);
        build_weapon_rubber_button.place(build_weapon_iron_button, Placement.BOTTOM_MID);
        build_back_button.place(build_weapon_rubber_button, Placement.BOTTOM_MID);
        // A second column, so the submenu does not grow taller than the screen.
        if (shield_enabled)
            build_weapon_shield_button.place(build_weapon_rock_button, Placement.LEFT_MID);
        if (torch_enabled)
            build_weapon_torch_button.place(build_weapon_iron_button, Placement.LEFT_MID);
        if (drum_enabled)
            build_weapon_drum_button.place(build_weapon_rubber_button, Placement.LEFT_MID);
        if (net_enabled)
            build_weapon_net_button.place(build_back_button, Placement.LEFT_MID);
        build_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, GROUP_TOP_OFFSET);

        army_peon_button = new DeploySpinner(viewer, player_interface, race_icons.peonIcon(), i18n("deploy_peon_tip"),
                List.of(race_icons.unitStatusIcon()), GameAction.TRAIN_PEON, GameAction.TRAIN_PEON_DEC, null, null);
        army_group.addChild(army_peon_button);
        army_warrior_rock_button = new DeploySpinner(viewer, player_interface, race_icons.warriorRockIcon(), i18n(
                "deploy_rock_tip"),
                List.of(race_icons.unitStatusIcon(), race_icons.weaponRockStatusIcon()), GameAction.RES_ROCK,
                GameAction.RES_ROCK_DEC, null, null);
        army_group.addChild(army_warrior_rock_button);

        army_warrior_iron_button = new DeploySpinner(viewer, player_interface, race_icons.warriorIronIcon(), i18n(
                "deploy_iron_tip"),
                List.of(race_icons.unitStatusIcon(), race_icons.weaponIronStatusIcon()), GameAction.RES_IRON,
                GameAction.RES_IRON_DEC, null, null);
        army_group.addChild(army_warrior_iron_button);

        army_warrior_rubber_button = new DeploySpinner(viewer, player_interface, race_icons.warriorRubberIcon(), i18n(
                "deploy_chicken_tip"),
                List.of(race_icons.unitStatusIcon(), race_icons.weaponRubberStatusIcon()), GameAction.RES_CHICKEN,
                GameAction.RES_CHICKEN_DEC, null, null);
        army_group.addChild(army_warrior_rubber_button);

        army_warrior_shield_button = new DeploySpinner(viewer, player_interface, race_icons.warriorShieldIcon(),
                shield_name, List.of(race_icons.unitStatusIcon(), race_icons.weaponShieldStatusIcon()),
                GameAction.RES_SHIELD, GameAction.RES_SHIELD_DEC, null, null);
        army_warrior_torch_button = new DeploySpinner(viewer, player_interface, race_icons.warriorTorchIcon(),
                torch_name, List.of(race_icons.unitStatusIcon(), race_icons.weaponTorchStatusIcon()),
                GameAction.RES_TORCH, GameAction.RES_TORCH_DEC, null, null);
        if (shield_enabled)
            army_group.addChild(army_warrior_shield_button);
        if (torch_enabled)
            army_group.addChild(army_warrior_torch_button);
        army_warrior_drum_button = new DeploySpinner(viewer, player_interface, race_icons.warriorDrumIcon(),
                player.getRace().getUnitTemplate(Race.UNIT_WARRIOR_DRUM).getName(),
                List.of(race_icons.unitStatusIcon(), race_icons.weaponDrumStatusIcon()), GameAction.RES_DRUM,
                GameAction.RES_DRUM_DEC, null, null);
        army_warrior_net_button = new DeploySpinner(viewer, player_interface, race_icons.warriorNetIcon(),
                player.getRace().getUnitTemplate(Race.UNIT_WARRIOR_NET).getName(),
                List.of(race_icons.unitStatusIcon(), race_icons.weaponNetStatusIcon()), GameAction.RES_NET,
                GameAction.RES_NET_DEC, null, null);
        if (drum_enabled)
            army_group.addChild(army_warrior_drum_button);
        if (net_enabled)
            army_group.addChild(army_warrior_net_button);

        army_back_button = new NonFocusIconButton(skin.getBackButton(), GameAction.GAMEPLAY_BACK,
                () -> i18n("back_tip", getBinding(GameAction.GAMEPLAY_BACK)));
        army_back_button.addMouseClickListener(this::cancelSubMenu);
        army_group.addChild(army_back_button);
        army_peon_button.place();
        army_warrior_rock_button.place(army_peon_button, Placement.BOTTOM_MID);
        army_warrior_iron_button.place(army_warrior_rock_button, Placement.BOTTOM_MID);
        army_warrior_rubber_button.place(army_warrior_iron_button, Placement.BOTTOM_MID);
        army_back_button.place(army_warrior_rubber_button, Placement.BOTTOM_MID);
        if (shield_enabled)
            army_warrior_shield_button.place(army_warrior_rock_button, Placement.LEFT_MID);
        if (torch_enabled)
            army_warrior_torch_button.place(army_warrior_iron_button, Placement.LEFT_MID);
        if (drum_enabled)
            army_warrior_drum_button.place(army_warrior_rubber_button, Placement.LEFT_MID);
        if (net_enabled)
            army_warrior_net_button.place(army_back_button, Placement.LEFT_MID);
        army_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, GROUP_TOP_OFFSET);

        ship_army_peon_button = new DeploySpinner(viewer, player_interface, race_icons.peonIcon(), i18n(
                "deploy_peon_tip"),
                List.of(race_icons.unitStatusIcon()), GameAction.TRAIN_PEON, GameAction.TRAIN_PEON_DEC, null, null);
        ship_army_group.addChild(ship_army_peon_button);
        ship_army_warrior_rock_button = new DeploySpinner(viewer, player_interface, race_icons.warriorRockIcon(), i18n(
                "deploy_rock_tip"),
                List.of(race_icons.unitStatusIcon(), race_icons.weaponRockStatusIcon()), GameAction.RES_ROCK,
                GameAction.RES_ROCK_DEC, null, null);
        ship_army_group.addChild(ship_army_warrior_rock_button);

        ship_army_warrior_iron_button = new DeploySpinner(viewer, player_interface, race_icons.warriorIronIcon(), i18n(
                "deploy_iron_tip"),
                List.of(race_icons.unitStatusIcon(), race_icons.weaponIronStatusIcon()), GameAction.RES_IRON,
                GameAction.RES_IRON_DEC, null, null);
        ship_army_group.addChild(ship_army_warrior_iron_button);

        ship_army_warrior_rubber_button = new DeploySpinner(viewer, player_interface, race_icons.warriorRubberIcon(),
                i18n("deploy_chicken_tip"),
                List.of(race_icons.unitStatusIcon(), race_icons.weaponRubberStatusIcon()), GameAction.RES_CHICKEN,
                GameAction.RES_CHICKEN_DEC, null, null);
        ship_army_group.addChild(ship_army_warrior_rubber_button);

        ship_army_chieftain_button = new NonFocusIconButton(race_icons.chieftainIcon(), GameAction.DEPLOY_CHIEFTAIN,
                () -> i18n("deploy_chieftain_tip"));
        ship_army_chieftain_button.setIconDisabler(() -> !current_building.canBuildChieftain());
        ship_army_chieftain_button.addMouseClickListener(this::deployChieftainFromShip);
        ship_army_group.addChild(ship_army_chieftain_button);

        ship_army_back_button = new NonFocusIconButton(skin.getBackButton(), GameAction.GAMEPLAY_BACK,
                () -> i18n("back_tip", getBinding(GameAction.GAMEPLAY_BACK)));
        ship_army_back_button.addMouseClickListener(this::cancelSubMenu);
        ship_army_group.addChild(ship_army_back_button);
        ship_army_peon_button.place();
        ship_army_warrior_rock_button.place(ship_army_peon_button, Placement.BOTTOM_MID);
        ship_army_warrior_iron_button.place(ship_army_warrior_rock_button, Placement.BOTTOM_MID);
        ship_army_warrior_rubber_button.place(ship_army_warrior_iron_button, Placement.BOTTOM_MID);
        ship_army_chieftain_button.place(ship_army_warrior_rubber_button, Placement.BOTTOM_MID);
        ship_army_back_button.place(ship_army_chieftain_button, Placement.BOTTOM_MID);
        ship_army_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, GROUP_TOP_OFFSET);

        transport_tree_button = new DeploySpinner(viewer, player_interface, icons.getTreeIcon(), i18n(
                "transport_tree_tip"),
                List.of(race_icons.unitStatusIcon(), icons.getTreeStatusIcon()), GameAction.RES_TREE,
                GameAction.RES_TREE_DEC, null, null);
        transport_group.addChild(transport_tree_button);
        transport_rock_button = new DeploySpinner(viewer, player_interface, icons.getRockIcon(), i18n(
                "transport_rock_tip"),
                List.of(race_icons.unitStatusIcon(), icons.getRockStatusIcon()), GameAction.RES_ROCK,
                GameAction.RES_ROCK_DEC, null, null);
        transport_group.addChild(transport_rock_button);
        transport_iron_button = new DeploySpinner(viewer, player_interface, icons.getIronIcon(), i18n(
                "transport_iron_tip"),
                List.of(race_icons.unitStatusIcon(), icons.getIronStatusIcon()), GameAction.RES_IRON,
                GameAction.RES_IRON_DEC, null, null);
        transport_group.addChild(transport_iron_button);
        transport_rubber_button = new DeploySpinner(viewer, player_interface, icons.getRubberIcon(), i18n(
                "transport_chicken_tip"),
                List.of(race_icons.unitStatusIcon(), icons.getRubberStatusIcon()), GameAction.RES_CHICKEN,
                GameAction.RES_CHICKEN_DEC, null, null);
        transport_group.addChild(transport_rubber_button);
        transport_back_button = new NonFocusIconButton(skin.getBackButton(), GameAction.GAMEPLAY_BACK,
                () -> i18n("back_tip", getBinding(GameAction.GAMEPLAY_BACK)));
        transport_back_button.addMouseClickListener(this::cancelSubMenu);
        transport_group.addChild(transport_back_button);
        transport_tree_button.place();
        transport_rock_button.place(transport_tree_button, Placement.BOTTOM_MID);
        transport_iron_button.place(transport_rock_button, Placement.BOTTOM_MID);
        transport_rubber_button.place(transport_iron_button, Placement.BOTTOM_MID);
        transport_back_button.place(transport_rubber_button, Placement.BOTTOM_MID);
        transport_group.compileCanvas(GROUP_LEFT_OFFSET, GROUP_BOTTOM_OFFSET, GROUP_RIGHT_OFFSET, GROUP_TOP_OFFSET);

        setCanFocus(!read_only);
        displayChangedNotify(width, height);
    }

    @Override
    public void doAdd() {
        super.doAdd();
        viewer.getAnimationManagerLocal().registerAnimation(this);
        GUIRoot root = getParentGUIRoot();
        if (root != null) {
            displayChangedNotify(root.getWidth(), root.getHeight());
        }
    }

    @Override
    protected void doRemove() {
        super.doRemove();
        viewer.getAnimationManagerLocal().removeAnimation(this);
    }

    @Override
    public void animate(float t) {
        Building new_building = selection.getCurrentSelection().getBuilding();
        boolean different_building = new_building != current_building;
        current_building = new_building;
        viewer.getRenderer().setSelectedBuilding(new_building);

        Unit new_chieftain = selection.getCurrentSelection().getChieftain();
        boolean different_chieftain = new_chieftain != current_chieftain;
        current_chieftain = new_chieftain;

        int current_num_units = selection.getCurrentSelection().getNumUnits();
        int current_num_peons = selection.getCurrentSelection().getNumBuilders();

        boolean new_quarters = current_building != null && current_building.getAbilities().hasAbilities(
                Abilities.REPRODUCE);
        boolean new_armory = current_building != null && current_building.getAbilities().hasAbilities(
                Abilities.BUILD_ARMIES);
        boolean new_ship = current_building != null && current_building.getAbilities().hasAbilities(Abilities.SAIL);
        boolean new_unit = current_num_units > 0;
        boolean new_peon = current_num_peons > 0;
        boolean new_catcher = net_enabled && selection.getCurrentSelection().getNumCatchers() > 0;
        boolean new_tower = current_building != null && current_building.getAbilities().hasAbilities(Abilities.ATTACK);
        boolean new_chicken_coop = current_building != null
                && current_building.getAbilities().hasAbilities(Abilities.BREED);
        boolean new_market = current_building != null
                && current_building.getAbilities().hasAbilities(Abilities.TRADE);
        boolean new_lodge = current_building != null
                && current_building.getAbilities().hasAbilities(Abilities.SHELTER);
        update = update || different_building || different_chieftain || new_quarters != current_quarters
                || new_armory != current_armory || new_ship != current_ship || new_unit != current_unit
                || new_peon != current_peon || new_tower != current_tower
                || new_chicken_coop != current_chicken_coop || new_market != current_market
                || new_lodge != current_lodge || new_catcher != current_catcher;
        if (update) {
            current_quarters = new_quarters;
            current_armory = new_armory;
            current_ship = new_ship;
            current_tower = new_tower;
            current_chicken_coop = new_chicken_coop;
            current_market = new_market;
            current_lodge = new_lodge;
            current_unit = new_unit;
            current_peon = new_peon;
            current_catcher = new_catcher;
            update = false;

            removeGroups();

            if (current_unit) {
                addChild(unit_group);
            }
            if (current_peon) {
                addChild(peon_group);
            }
            if (current_catcher) {
                addChild(catcher_group);
            }
            if (current_chieftain != null) {
                addChild(chieftain_group);
                updateGroups();
                if (player.canDoMagic(0)) {
                    magic1_button.setUnit(current_chieftain);
                    magic1_button.setIconDisabler(() -> !current_chieftain.canDoMagic(0));
                    chieftain_group.addChild(magic1_button);
                } else
                    magic1_button.remove();
                if (player.canDoMagic(1)) {
                    magic2_button.setUnit(current_chieftain);
                    magic2_button.setIconDisabler(() -> !current_chieftain.canDoMagic(1));
                    chieftain_group.addChild(magic2_button);
                } else
                    magic2_button.remove();
            }
            if (current_tower) {
                addChild(tower_group);
                tower_attack_button.setIconDisabler(() -> current_building == null
                        || !current_building.getAbilities().hasAbilities(Abilities.ATTACK));
                tower_exit_button.setIconDisabler(() -> current_building == null || !current_building.canExitTower());
            }
            if (current_quarters) {
                addChild(quarters_status_group);
                addChild(quarters_group);
                SupplyCounter unit_counter = new SupplyCounter(current_building, Unit.class);
                quarters_unit_status.setCounter(unit_counter);
                quarters_unit_status.setUnitContainerBuilding(current_building);
                quarters_peon_button.setContainers(current_building, DeployType.PEON, null);
                quarters_peon_button.setIconDisabler(() -> unit_counter.getNumSupplies() == 0);
                quarters_chieftain_button.setIconDisabler(() -> current_building != null
                        && !current_building.canBuildChieftain() && !current_building.canStopChieftain());
                quarters_chieftain_button.setBuilding(current_building);
            }
            if (current_chicken_coop) {
                addChild(chicken_coop_status_group);
                chicken_coop_stock_status.setCounter(new SupplyCounter(current_building, RubberSupply.class));
            }
            if (current_market) {
                addChild(market_status_group);
                addChild(market_group);
                SupplyCounter unit_counter = new SupplyCounter(current_building, Unit.class);
                market_unit_status.setCounter(unit_counter);
                market_peon_button.setContainers(current_building, DeployType.PEON, null);
                market_peon_button.setIconDisabler(() -> unit_counter.getNumSupplies() == 0);
                removeTradeButtons();
            }
            if (current_lodge) {
                addChild(lodge_status_group);
                addChild(lodge_group);
                SupplyCounter unit_counter = new SupplyCounter(current_building, Unit.class);
                lodge_unit_status.setCounter(unit_counter);
                lodge_champion_status.setCounter(new ChampionCounter(current_building, player));
                lodge_leave_button.setContainers(current_building, DeployType.SHELTERED, null);
                lodge_leave_button.setIconDisabler(() -> unit_counter.getNumSupplies() == 0);
                lodge_champion_button.setBuildSupplyContainer(current_building, Champion.class);
            }
            if (current_armory) {
                addChild(status_group);
                if (anyGear())
                    addChild(gear_status_group);
                addChild(armory_group);
                if (player.canUseRubber()) {
                    build_group.addChild(build_weapon_rubber_button);
                    army_group.addChild(army_warrior_rubber_button);
                } else {
                    build_weapon_rubber_button.remove();
                    army_warrior_rubber_button.remove();
                }
                updateCounters();
            }
            if (current_ship) {
                addChild(status_group);
                addChild(ship_group);
                if (player.canUseRubber()) {
                    ship_army_group.addChild(ship_army_warrior_rubber_button);
                } else {
                    ship_army_warrior_rubber_button.remove();
                }
                updateCounters();
            }
        }
        updateButtons();
    }

    private void updateButtons() {
        if (current_building != null && current_building.getAbilities().hasAbilities(Abilities.ATTACK)) {
            tower_attack_button.doUpdate();
            tower_exit_button.doUpdate();
        } else if (current_building != null && current_building.getAbilities().hasAbilities(Abilities.BUILD_ARMIES)) {
            unit_status.doUpdate();
            weapon_rock_status.doUpdate();
            weapon_iron_status.doUpdate();
            weapon_rubber_status.doUpdate();
            tree_status.doUpdate();
            rock_status.doUpdate();
            iron_status.doUpdate();
            rubber_status.doUpdate();

            harvest_button.doUpdate();
            build_button.doUpdate();
            army_button.doUpdate();

            harvest_tree_button.doUpdate();
            harvest_rock_button.doUpdate();
            harvest_iron_button.doUpdate();
            harvest_rubber_button.doUpdate();
            build_weapon_rock_button.doUpdate();
            build_weapon_iron_button.doUpdate();
            build_weapon_rubber_button.doUpdate();
            army_warrior_rubber_button.doUpdate();
            army_warrior_iron_button.doUpdate();
            army_warrior_rock_button.doUpdate();
            army_peon_button.doUpdate();
            if (shield_enabled) {
                weapon_shield_status.doUpdate();
                build_weapon_shield_button.doUpdate();
                army_warrior_shield_button.doUpdate();
            }
            if (torch_enabled) {
                weapon_torch_status.doUpdate();
                build_weapon_torch_button.doUpdate();
                army_warrior_torch_button.doUpdate();
            }
            if (drum_enabled) {
                weapon_drum_status.doUpdate();
                build_weapon_drum_button.doUpdate();
                army_warrior_drum_button.doUpdate();
            }
            if (net_enabled) {
                weapon_net_status.doUpdate();
                build_weapon_net_button.doUpdate();
                army_warrior_net_button.doUpdate();
            }
            transport_tree_button.doUpdate();
            transport_rock_button.doUpdate();
            transport_iron_button.doUpdate();
            transport_rubber_button.doUpdate();
        } else if (current_building != null && current_building.getAbilities().hasAbilities(Abilities.REPRODUCE)) {
            quarters_unit_status.doUpdate();

            quarters_peon_button.doUpdate();
            quarters_chieftain_button.doUpdate();
        } else if (current_building != null
                && current_building.getAbilities().hasAbilities(Abilities.SAIL)) {
                    unit_status.doUpdate();
                    weapon_rock_status.doUpdate();
                    weapon_iron_status.doUpdate();
                    weapon_rubber_status.doUpdate();
                    tree_status.doUpdate();
                    rock_status.doUpdate();
                    iron_status.doUpdate();
                    rubber_status.doUpdate();

                    ship_harvest_button.doUpdate();
                    ship_army_button.doUpdate();
                    ship_sail_button.doUpdate();

                    harvest_tree_button.doUpdate();
                    harvest_rock_button.doUpdate();
                    harvest_iron_button.doUpdate();
                    harvest_rubber_button.doUpdate();
                    ship_army_warrior_rubber_button.doUpdate();
                    ship_army_warrior_iron_button.doUpdate();
                    ship_army_warrior_rock_button.doUpdate();
                    ship_army_peon_button.doUpdate();
                    ship_army_chieftain_button.doUpdate();
                    transport_tree_button.doUpdate();
                    transport_rock_button.doUpdate();
                    transport_iron_button.doUpdate();
                    transport_rubber_button.doUpdate();
                } else if (current_chicken_coop) {
                    chicken_coop_stock_status.doUpdate();
                } else if (current_market) {
                    market_unit_status.doUpdate();
                    market_peon_button.doUpdate();
                    showTradeButtons();
                    if (shown_sell >= 0)
                        market_sell_buttons[shown_sell].doUpdate();
                    if (shown_buy >= 0)
                        market_buy_buttons[shown_buy].doUpdate();
                    market_rally_point_button.doUpdate();
                } else if (current_lodge) {
                    lodge_unit_status.doUpdate();
                    lodge_champion_status.doUpdate();
                    lodge_leave_button.doUpdate();
                    lodge_champion_button.doUpdate();
                    lodge_rally_point_button.doUpdate();
                } else if (current_peon) {
                    quarters_button.doUpdate();
                    armory_button.doUpdate();
                    tower_button.doUpdate();
                    if (viewer.getWorld().isShipsEnabled()) {
                        ship_button.doUpdate();
                    }
                    if (chicken_coop_enabled) {
                        chicken_coop_button.doUpdate();
                    }
                    if (totem_enabled) {
                        totem_button.doUpdate();
                    }
                    if (market_enabled) {
                        market_button.doUpdate();
                    }
                    if (palisade_enabled) {
                        palisade_button.doUpdate();
                        gate_button.doUpdate();
                    }
                    if (great_tower_enabled) {
                        great_tower_button.doUpdate();
                    }
                    if (lodge_enabled) {
                        lodge_button.doUpdate();
                    }
                }
        if (current_unit) {
            move_button.doUpdate();
            attack_button.doUpdate();
            gather_repair_button.doUpdate();
        }
        if (current_catcher) {
            lay_snare_button.doUpdate();
        }
        if (current_chieftain != null) {
            magic1_button.doUpdate();
            magic2_button.doUpdate();
        }
    }

    /** Whether the ruleset offers any of Buffed's gear, whose stock shows beside the Armory's status. */
    private boolean anyGear() {
        return shield_enabled || torch_enabled || drum_enabled || net_enabled;
    }

    private void removeGroups() {
        unit_group.remove();
        peon_group.remove();
        chieftain_group.remove();
        tower_group.remove();
        quarters_status_group.remove();
        quarters_group.remove();
        status_group.remove();
        armory_group.remove();
        ship_group.remove();
        harvest_group.remove();
        build_group.remove();
        army_group.remove();
        ship_army_group.remove();
        transport_group.remove();
        chicken_coop_status_group.remove();
        gear_status_group.remove();
        market_status_group.remove();
        market_group.remove();
        lodge_status_group.remove();
        lodge_group.remove();
        catcher_group.remove();
        current_submenu = null;
    }

    /** Shows the sell and buy buttons of what the selected Market trades now. */
    private void showTradeButtons() {
        Market market = current_building instanceof LandBuilding building
                && !building.isDead() ? building.getMarket() : null;
        if (market == null)
            return;
        if (market.getGive() != shown_sell) {
            if (shown_sell >= 0)
                market_sell_buttons[shown_sell].remove();
            shown_sell = market.getGive();
            market_group.addChild(market_sell_buttons[shown_sell]);
        }
        if (market.getGet() != shown_buy) {
            if (shown_buy >= 0)
                market_buy_buttons[shown_buy].remove();
            shown_buy = market.getGet();
            market_group.addChild(market_buy_buttons[shown_buy]);
        }
    }

    private void removeTradeButtons() {
        for (int i = 0; i < Market.numResources(); i++) {
            market_sell_buttons[i].remove();
            market_buy_buttons[i].remove();
        }
        shown_sell = -1;
        shown_buy = -1;
    }

    /** Asks the selected Market to sell, or buy, the next resource; the two are never the same. */
    private void cycleTrade(boolean sell) {
        Market market = current_building instanceof LandBuilding building
                && !building.isDead() ? building.getMarket() : null;
        if (market == null)
            return;
        int give = market.getGive();
        int get = market.getGet();
        do {
            if (sell)
                give = (give + 1) % Market.numResources();
            else
                get = (get + 1) % Market.numResources();
        } while (give == get);
        viewer.getPeerHub().getPlayerInterface().setTrade(current_building, give, get);
    }

    /** Rebinds the spinners to the current building; a read-only copy has no clicks of its own to keep them in step. */
    public void refreshCounters() {
        if (read_only && getSubmenu() != SUBMENU_NONE && (current_armory || current_ship)
                && current_building != null && !current_building.isDead())
            updateCounters();
    }

    private void updateCounters() {
        assert current_building != null : "Building is null";
        SupplyCounter unit_counter = new SupplyCounter(current_building, Unit.class);
        unit_status.setCounter(unit_counter);
        SupplyCounter weapon_rock_counter = new SupplyCounter(current_building, RockAxeWeapon.class);
        weapon_rock_status.setCounter(weapon_rock_counter);
        SupplyCounter weapon_iron_counter = new SupplyCounter(current_building, IronAxeWeapon.class);
        weapon_iron_status.setCounter(weapon_iron_counter);
        SupplyCounter weapon_rubber_counter = new SupplyCounter(current_building, RubberAxeWeapon.class);
        weapon_rubber_status.setCounter(weapon_rubber_counter);
        SupplyCounter tree_counter = new SupplyCounter(current_building, TreeSupply.class);
        tree_status.setCounter(tree_counter);
        SupplyCounter rock_counter = new SupplyCounter(current_building, RockSupply.class);
        rock_status.setCounter(rock_counter);
        SupplyCounter iron_counter = new SupplyCounter(current_building, IronSupply.class);
        iron_status.setCounter(iron_counter);
        SupplyCounter rubber_counter = new SupplyCounter(current_building, RubberSupply.class);
        rubber_status.setCounter(rubber_counter);

        harvest_tree_button.setContainers(current_building, DeployType.PEON_HARVEST_TREE, null);
        harvest_tree_button.setIconDisabler(() -> unit_counter.getNumSupplies() == 0);
        harvest_rock_button.setContainers(current_building, DeployType.PEON_HARVEST_ROCK, null);
        harvest_rock_button.setIconDisabler(() -> unit_counter.getNumSupplies() == 0);
        harvest_iron_button.setContainers(current_building, DeployType.PEON_HARVEST_IRON, null);
        harvest_iron_button.setIconDisabler(() -> unit_counter.getNumSupplies() == 0);
        harvest_rubber_button.setContainers(current_building, DeployType.PEON_HARVEST_RUBBER, null);
        harvest_rubber_button.setIconDisabler(() -> unit_counter.getNumSupplies() == 0);

        build_weapon_rock_button.setBuildSupplyContainer(current_building, RockAxeWeapon.class);
        build_weapon_iron_button.setBuildSupplyContainer(current_building, IronAxeWeapon.class);
        build_weapon_rubber_button.setBuildSupplyContainer(current_building, RubberAxeWeapon.class);

        army_peon_button.setContainers(current_building, DeployType.PEON, null);
        army_peon_button.setIconDisabler(() -> unit_counter.getNumSupplies() == 0);
        army_warrior_rock_button.setContainers(current_building, DeployType.ROCK_WARRIOR, RockAxeWeapon.class);
        army_warrior_rock_button.setIconDisabler(() -> suppliesEmpty(unit_counter, weapon_rock_counter));
        army_warrior_iron_button.setContainers(current_building, DeployType.IRON_WARRIOR, IronAxeWeapon.class);
        army_warrior_iron_button.setIconDisabler(() -> suppliesEmpty(unit_counter, weapon_iron_counter));
        army_warrior_rubber_button.setContainers(current_building, DeployType.RUBBER_WARRIOR, RubberAxeWeapon.class);
        army_warrior_rubber_button.setIconDisabler(() -> suppliesEmpty(unit_counter, weapon_rubber_counter));

        // Ships have no gear (gear warriors do not board them); only an Armory's panel shows these.
        if (current_armory && anyGear()) {
            SupplyCounter weapon_shield_counter = new SupplyCounter(current_building, Shield.class);
            weapon_shield_status.setCounter(weapon_shield_counter);
            SupplyCounter weapon_torch_counter = new SupplyCounter(current_building, Torch.class);
            weapon_torch_status.setCounter(weapon_torch_counter);
            build_weapon_shield_button.setBuildSupplyContainer(current_building, Shield.class);
            build_weapon_torch_button.setBuildSupplyContainer(current_building, Torch.class);
            army_warrior_shield_button.setContainers(current_building, DeployType.SHIELD_WARRIOR, Shield.class);
            army_warrior_shield_button.setIconDisabler(() -> suppliesEmpty(unit_counter, weapon_shield_counter));
            army_warrior_torch_button.setContainers(current_building, DeployType.TORCH_WARRIOR, Torch.class);
            army_warrior_torch_button.setIconDisabler(() -> suppliesEmpty(unit_counter, weapon_torch_counter));
            SupplyCounter weapon_drum_counter = new SupplyCounter(current_building, Drum.class);
            weapon_drum_status.setCounter(weapon_drum_counter);
            SupplyCounter weapon_net_counter = new SupplyCounter(current_building, Net.class);
            weapon_net_status.setCounter(weapon_net_counter);
            build_weapon_drum_button.setBuildSupplyContainer(current_building, Drum.class);
            build_weapon_net_button.setBuildSupplyContainer(current_building, Net.class);
            army_warrior_drum_button.setContainers(current_building, DeployType.DRUM_WARRIOR, Drum.class);
            army_warrior_drum_button.setIconDisabler(() -> suppliesEmpty(unit_counter, weapon_drum_counter));
            army_warrior_net_button.setContainers(current_building, DeployType.NET_WARRIOR, Net.class);
            army_warrior_net_button.setIconDisabler(() -> suppliesEmpty(unit_counter, weapon_net_counter));
        }

        ship_army_peon_button.setContainers(current_building, DeployType.PEON, null);
        ship_army_peon_button.setIconDisabler(() -> unit_counter.getNumSupplies() == 0);
        ship_army_warrior_rock_button.setContainers(current_building, DeployType.ROCK_WARRIOR, RockAxeWeapon.class);
        ship_army_warrior_rock_button.setIconDisabler(() -> suppliesEmpty(unit_counter, weapon_rock_counter));
        ship_army_warrior_iron_button.setContainers(current_building, DeployType.IRON_WARRIOR, IronAxeWeapon.class);
        ship_army_warrior_iron_button.setIconDisabler(() -> suppliesEmpty(unit_counter, weapon_iron_counter));
        ship_army_warrior_rubber_button.setContainers(current_building, DeployType.RUBBER_WARRIOR,
                RubberAxeWeapon.class);
        ship_army_warrior_rubber_button.setIconDisabler(() -> suppliesEmpty(unit_counter, weapon_rubber_counter));

        transport_tree_button.setContainers(current_building, DeployType.PEON_TRANSPORT_TREE, TreeSupply.class);
        transport_tree_button.setIconDisabler(() -> suppliesEmpty(unit_counter, tree_counter));
        transport_rock_button.setContainers(current_building, DeployType.PEON_TRANSPORT_ROCK, RockSupply.class);
        transport_rock_button.setIconDisabler(() -> suppliesEmpty(unit_counter, rock_counter));
        transport_iron_button.setContainers(current_building, DeployType.PEON_TRANSPORT_IRON, IronSupply.class);
        transport_iron_button.setIconDisabler(() -> suppliesEmpty(unit_counter, iron_counter));
        transport_rubber_button.setContainers(current_building, DeployType.PEON_TRANSPORT_RUBBER, RubberSupply.class);
        transport_rubber_button.setIconDisabler(() -> suppliesEmpty(unit_counter, rubber_counter));
    }

    @Override
    public void displayChangedNotify(int width, int height) {
        setDim(width, height);
        updateGroups();
    }

    private void updateGroups() {
        int width = getWidth();
        int height = getHeight();
        unit_group.setPos(width - unit_group.getWidth(), height - unit_group.getHeight());
        peon_group.setPos(width - peon_group.getWidth(), unit_group.getY() - peon_group.getHeight());
        catcher_group.setPos(unit_group.getX() - catcher_group.getWidth(), height - catcher_group.getHeight());
        if (current_peon)
            chieftain_group.setPos(width - chieftain_group.getWidth(), peon_group.getY() - chieftain_group.getHeight());
        else
            chieftain_group.setPos(width - chieftain_group.getWidth(), unit_group.getY() - chieftain_group.getHeight());
        tower_group.setPos(width - tower_group.getWidth(), height - tower_group.getHeight());
        quarters_status_group.setPos(width - quarters_status_group.getWidth(),
                height - quarters_status_group.getHeight());
        quarters_group.setPos(width - quarters_group.getWidth(),
                quarters_status_group.getY() - quarters_group.getHeight());
        status_group.setPos(width - status_group.getWidth(), height - status_group.getHeight());
        gear_status_group.setPos(status_group.getX() - gear_status_group.getWidth(),
                height - gear_status_group.getHeight());
        armory_group.setPos(width - armory_group.getWidth(), status_group.getY() - armory_group.getHeight());
        ship_group.setPos(width - ship_group.getWidth(), status_group.getY() - ship_group.getHeight());
        harvest_group.setPos(width - harvest_group.getWidth(), status_group.getY() - harvest_group.getHeight());
        build_group.setPos(width - build_group.getWidth(), status_group.getY() - build_group.getHeight());
        army_group.setPos(width - army_group.getWidth(), status_group.getY() - army_group.getHeight());
        ship_army_group.setPos(width - ship_army_group.getWidth(),
                status_group.getY() - ship_army_group.getHeight());
        transport_group.setPos(width - transport_group.getWidth(), status_group.getY() - transport_group.getHeight());
        chicken_coop_status_group.setPos(width - chicken_coop_status_group.getWidth(),
                height - chicken_coop_status_group.getHeight());
        market_status_group.setPos(width - market_status_group.getWidth(), height - market_status_group.getHeight());
        market_group.setPos(width - market_group.getWidth(), market_status_group.getY() - market_group.getHeight());
        lodge_status_group.setPos(width - lodge_status_group.getWidth(), height - lodge_status_group.getHeight());
        lodge_group.setPos(width - lodge_group.getWidth(), lodge_status_group.getY() - lodge_group.getHeight());
    }

    @Override
    public void handleInput(@NonNull InputEvent event) {
        if (read_only)
            return;
        InputPhase phase = event.getPhase();
        boolean pressed = phase == InputPhase.PRESSED || phase == InputPhase.REPEAT;
        boolean released = phase == InputPhase.RELEASED;
        boolean repeat = phase == InputPhase.REPEAT;

        if (pressed) {
            if (!repeat) {
                // Re-ordered the submenu openers to come first in the chain, fixed Q not working inside submenus
                if (current_armory && canSwitchSubmenu(event) && event.consumeAction(GameAction.PROD_WEAPONS)) {
                    activate(event, build_button);
                } else if (current_armory && canSwitchSubmenu(event) && event.consumeAction(GameAction.PROD_ARMY)) {
                    activate(event, army_button);
                } else if (current_armory && canSwitchSubmenu(event) && event.consumeAction(
                        GameAction.PROD_TRANSPORT)) {
                            activate(event, transport_button);
                        } else if (current_armory && canSwitchSubmenu(event) && event.consumeAction(
                                GameAction.PROD_HARVEST)) {
                                    activate(event, harvest_button);
                                } else if (current_ship && canSwitchSubmenu(event) && event.consumeAction(
                                        GameAction.PROD_ARMY)) {
                                            activate(event, ship_army_button);
                                        } else if (current_ship && canSwitchSubmenu(event) && event.consumeAction(
                                                GameAction.PROD_TRANSPORT)) {
                                                    activate(event, ship_transport_button);
                                                } else if (current_ship && canSwitchSubmenu(event)
                                                        && event.consumeAction(
                                                                GameAction.PROD_HARVEST)) {
                                                                    activate(event, ship_harvest_button);
                                                                }

                // === Normal Unit / Peon Actions ===
                else if ((current_unit || current_ship) && event.consumeAction(GameAction.UNIT_MOVE)) {
                    // The same binding sails a selected ship, mirroring the sail button in ship_group.
                    if (current_unit) {
                        activate(event, move_button);
                    } else if (current_submenu == null) {
                        activate(event, ship_sail_button);
                    }
                } else if (current_unit && current_peon && event.consumeAction(GameAction.UNIT_BUILD_QUARTERS)) {
                    // Q - Build Quarters with Peon
                    activate(event, quarters_button);
                } else if (current_unit && current_peon && event.consumeAction(GameAction.UNIT_BUILD_SHIP)) {
                    if (viewer.getWorld().isShipsEnabled()) {
                        activate(event, ship_button);
                    }
                } else if (current_unit && current_peon && event.consumeAction(
                        GameAction.UNIT_BUILD_CHICKEN_COOP)) {
                            if (chicken_coop_enabled) {
                                activate(event, chicken_coop_button);
                            }
                        } else if (current_unit && current_peon && event.consumeAction(GameAction.UNIT_BUILD_TOTEM)) {
                            if (totem_enabled) {
                                activate(event, totem_button);
                            }
                        } else if (current_unit && current_peon && event.consumeAction(GameAction.UNIT_BUILD_MARKET)) {
                            if (market_enabled) {
                                activate(event, market_button);
                            }
                        } else if (current_unit && current_peon && event.consumeAction(
                                GameAction.UNIT_BUILD_PALISADE)) {
                                    if (palisade_enabled) {
                                        activate(event, palisade_button);
                                    }
                                } else if (current_unit && current_peon && event.consumeAction(
                                        GameAction.UNIT_BUILD_GATE)) {
                                            if (palisade_enabled) {
                                                activate(event, gate_button);
                                            }
                                        } else if (current_unit && current_peon && event.consumeAction(
                                                GameAction.UNIT_BUILD_GREAT_TOWER)) {
                                                    if (great_tower_enabled) {
                                                        activate(event, great_tower_button);
                                                    }
                                                } else if (current_unit && current_peon && event.consumeAction(
                                                        GameAction.UNIT_BUILD_LODGE)) {
                                                            if (lodge_enabled) {
                                                                activate(event, lodge_button);
                                                            }
                                                        } else if (current_catcher && event.consumeAction(
                                                                GameAction.UNIT_LAY_SNARE)) {
                                                                    activate(event, lay_snare_button);
                                                                } else if (current_market && event.consumeAction(
                                                                        GameAction.MARKET_SELL)) {
                                                                            if (shown_sell >= 0) {
                                                                                activate(event,
                                                                                        market_sell_buttons[shown_sell]);
                                                                            }
                                                                        } else if (current_market
                                                                                && event.consumeAction(
                                                                                        GameAction.MARKET_BUY)) {
                                                                                            if (shown_buy >= 0) {
                                                                                                activate(event,
                                                                                                        market_buy_buttons[shown_buy]);
                                                                                            }
                                                                                        } else if ((current_unit
                                                                                                || current_tower)
                                                                                                && event.consumeAction(
                                                                                                        GameAction.UNIT_ATTACK)) {
                                                                                                            if (current_unit) {
                                                                                                                activate(
                                                                                                                        event,
                                                                                                                        attack_button);
                                                                                                            } else
                                                                                                                if (current_tower) {
                                                                                                                    activate(
                                                                                                                            event,
                                                                                                                            tower_attack_button);
                                                                                                                }
                                                                                                        } else
                                                                                            if ((current_unit
                                                                                                    || current_armory
                                                                                                    || current_ship)
                                                                                                    && (event.consumeAction(
                                                                                                            GameAction.UNIT_GATHER)
                                                                                                            || event.consumeAction(
                                                                                                                    GameAction.PROD_HARVEST))) {
                                                                                                                        // G - Gather or Harvest
                                                                                                                        if (current_unit) {
                                                                                                                            activate(
                                                                                                                                    event,
                                                                                                                                    gather_repair_button);
                                                                                                                        } else
                                                                                                                            if (current_armory
                                                                                                                                    && current_submenu == null) {
                                                                                                                                        // Legacy gather alias only works from the top level; direct submenu
                                                                                                                                        // switching is reserved for the PROD_HARVEST binding handled above.
                                                                                                                                        activate(
                                                                                                                                                event,
                                                                                                                                                harvest_button);
                                                                                                                                    } else
                                                                                                                                if (current_ship
                                                                                                                                        && current_submenu == null) {
                                                                                                                                            activate(
                                                                                                                                                    event,
                                                                                                                                                    ship_harvest_button);
                                                                                                                                        }
                                                                                                                    } else
                                                                                                if ((current_peon
                                                                                                        || current_armory
                                                                                                        || current_ship)
                                                                                                        && event.consumeAction(
                                                                                                                GameAction.UNIT_BUILD_TOWER)) {
                                                                                                                    if (current_peon) {
                                                                                                                        activate(
                                                                                                                                event,
                                                                                                                                tower_button);
                                                                                                                    }
                                                                                                                } else
                                                                                                    if (current_quarters
                                                                                                            && event.consumeAction(
                                                                                                                    GameAction.TRAIN_CHIEFTAIN)) {
                                                                                                                        activate(
                                                                                                                                event,
                                                                                                                                quarters_chieftain_button);
                                                                                                                    } else
                                                                                                        if (current_ship
                                                                                                                && current_submenu == ship_army_group
                                                                                                                && event.consumeAction(
                                                                                                                        GameAction.DEPLOY_CHIEFTAIN)) {
                                                                                                                            activate(
                                                                                                                                    event,
                                                                                                                                    ship_army_chieftain_button);
                                                                                                                        } else
                                                                                                            if (current_chieftain != null
                                                                                                                    && event.consumeAction(
                                                                                                                            GameAction.MAGIC_2)) {
                                                                                                                                if (player.canDoMagic(
                                                                                                                                        1)) {
                                                                                                                                    activate(
                                                                                                                                            event,
                                                                                                                                            magic2_button);
                                                                                                                                }
                                                                                                                            } else
                                                                                                                if ((current_armory
                                                                                                                        || current_ship)
                                                                                                                        && current_submenu != null
                                                                                                                        && event.consumeAction(
                                                                                                                                GameAction.GAMEPLAY_BACK)) {
                                                                                                                                    if (current_submenu == harvest_group)
                                                                                                                                        activate(
                                                                                                                                                event,
                                                                                                                                                harvest_back_button);
                                                                                                                                    else if (current_submenu == build_group)
                                                                                                                                        activate(
                                                                                                                                                event,
                                                                                                                                                build_back_button);
                                                                                                                                    else if (current_submenu == army_group)
                                                                                                                                        activate(
                                                                                                                                                event,
                                                                                                                                                army_back_button);
                                                                                                                                    else if (current_submenu == ship_army_group)
                                                                                                                                        activate(
                                                                                                                                                event,
                                                                                                                                                ship_army_back_button);
                                                                                                                                    else if (current_submenu == transport_group)
                                                                                                                                        activate(
                                                                                                                                                event,
                                                                                                                                                transport_back_button);
                                                                                                                                } else
                                                                                                                    if (current_building == null
                                                                                                                            && current_peon
                                                                                                                            && event.consumeAction(
                                                                                                                                    GameAction.UNIT_BUILD_ARMORY)) {
                                                                                                                                        activate(
                                                                                                                                                event,
                                                                                                                                                armory_button);
                                                                                                                                    } else
                                                                                                                        if (current_building != null
                                                                                                                                && event.consumeAction(
                                                                                                                                        GameAction.UNIT_SET_RALLY)) {
                                                                                                                                            if (current_armory
                                                                                                                                                    && current_submenu == null) {
                                                                                                                                                activate(
                                                                                                                                                        event,
                                                                                                                                                        rally_point_button);
                                                                                                                                            } else
                                                                                                                                                if (current_ship
                                                                                                                                                        && current_submenu == null) {
                                                                                                                                                            activate(
                                                                                                                                                                    event,
                                                                                                                                                                    ship_rally_point_button);
                                                                                                                                                        } else
                                                                                                                                                    if (current_quarters) {
                                                                                                                                                        activate(
                                                                                                                                                                event,
                                                                                                                                                                quarters_rally_point_button);
                                                                                                                                                    } else
                                                                                                                                                        if (current_market) {
                                                                                                                                                            activate(
                                                                                                                                                                    event,
                                                                                                                                                                    market_rally_point_button);
                                                                                                                                                        } else
                                                                                                                                                            if (current_lodge) {
                                                                                                                                                                activate(
                                                                                                                                                                        event,
                                                                                                                                                                        lodge_rally_point_button);
                                                                                                                                                            }
                                                                                                                                        } else
                                                                                                                            if (current_tower
                                                                                                                                    && event.consumeAction(
                                                                                                                                            GameAction.UNIT_EXIT_TOWER)) {
                                                                                                                                                activate(
                                                                                                                                                        event,
                                                                                                                                                        tower_exit_button);
                                                                                                                                            } else
                                                                                                                                if (current_chieftain != null
                                                                                                                                        && event.consumeAction(
                                                                                                                                                GameAction.MAGIC_1)) {
                                                                                                                                                    if (player.canDoMagic(
                                                                                                                                                            0)) {
                                                                                                                                                        activate(
                                                                                                                                                                event,
                                                                                                                                                                magic1_button);
                                                                                                                                                    }
                                                                                                                                                }

                if (event.isConsumed()) return;
            }
            // Repeating Actions (Spinners)
            // Clear remaining actions after a match to prevent global actions (e.g. screenshot)
            // from also firing on the same key combo. Mirrors upstreams return-true behavior.
            if (current_building != null) {
                // First, since its key doubles as the chicken row's in the Armory's submenus.
                if (championShortcut(event, true))
                    event.getActions().clear();
                var peon = checkResourceAction(event, GameAction.TRAIN_PEON, GameAction.TRAIN_PEON_DEC,
                        GameAction.TRAIN_PEON_BATCH, GameAction.TRAIN_PEON_BATCH_DEC);
                if (peon.active()) {
                    if (current_quarters) {
                        quarters_peon_button.shortcutPressed(peon.decrement(), peon.batch());
                        event.getActions().clear(); // Prevent fallthrough to resource/global handlers when peon shortcut is handled.
                    } else if (current_market) {
                        market_peon_button.shortcutPressed(peon.decrement(), peon.batch());
                        event.getActions().clear();
                    } else if (current_lodge) {
                        lodge_leave_button.shortcutPressed(peon.decrement(), peon.batch());
                        event.getActions().clear();
                    } else if (current_armory && current_submenu == army_group) {
                        army_peon_button.shortcutPressed(peon.decrement(), peon.batch());
                        event.getActions().clear();
                    } else if (current_ship && current_submenu == ship_army_group) {
                        ship_army_peon_button.shortcutPressed(peon.decrement(), peon.batch());
                        event.getActions().clear();
                    }
                }
                // Chicken/Rubber
                var chicken = checkResourceAction(event, GameAction.RES_CHICKEN, GameAction.RES_CHICKEN_DEC,
                        GameAction.RES_CHICKEN_BATCH, GameAction.RES_CHICKEN_BATCH_DEC);
                if (chicken.active()) {
                    handleArmoryShortcut(true, chicken, harvest_rubber_button, build_weapon_rubber_button,
                            army_warrior_rubber_button, ship_army_warrior_rubber_button,
                            transport_rubber_button);
                    event.getActions().clear();
                }

                // Iron
                var iron = checkResourceAction(event, GameAction.RES_IRON, GameAction.RES_IRON_DEC,
                        GameAction.RES_IRON_BATCH, GameAction.RES_IRON_BATCH_DEC);
                if (iron.active()) {
                    handleArmoryShortcut(true, iron, harvest_iron_button, build_weapon_iron_button,
                            army_warrior_iron_button, ship_army_warrior_iron_button,
                            transport_iron_button);
                    event.getActions().clear();
                }

                var tree = checkResourceAction(event, GameAction.RES_TREE, GameAction.RES_TREE_DEC,
                        GameAction.RES_TREE_BATCH, GameAction.RES_TREE_BATCH_DEC);
                if (tree.active()) {
                    handleArmoryShortcut(true, tree, harvest_tree_button, null, null, null, transport_tree_button);
                    event.getActions().clear();
                }

                var rock = checkResourceAction(event, GameAction.RES_ROCK, GameAction.RES_ROCK_DEC,
                        GameAction.RES_ROCK_BATCH, GameAction.RES_ROCK_BATCH_DEC);
                if (rock.active()) {
                    handleArmoryShortcut(true, rock, harvest_rock_button, build_weapon_rock_button,
                            army_warrior_rock_button, ship_army_warrior_rock_button,
                            transport_rock_button);
                    event.getActions().clear();
                }

                if (gearShortcut(event, true))
                    event.getActions().clear();
            }
        } else if (released) {
            if (gearShortcut(event, false) || championShortcut(event, false))
                return;
            var chicken = checkResourceAction(event, GameAction.RES_CHICKEN, GameAction.RES_CHICKEN_DEC,
                    GameAction.RES_CHICKEN_BATCH, GameAction.RES_CHICKEN_BATCH_DEC);
            if (chicken.active()) {
                handleArmoryShortcut(false, chicken, harvest_rubber_button, build_weapon_rubber_button,
                        army_warrior_rubber_button, ship_army_warrior_rubber_button,
                        transport_rubber_button);
            } else {
                var peon = checkResourceAction(event, GameAction.TRAIN_PEON, GameAction.TRAIN_PEON_DEC,
                        GameAction.TRAIN_PEON_BATCH, GameAction.TRAIN_PEON_BATCH_DEC);
                if (peon.active()) {
                    if (current_quarters) {
                        quarters_peon_button.shortcutReleased(peon.decrement(), peon.batch());
                        event.getActions().clear(); // Prevent fallthrough to resource/global handlers when peon shortcut is handled.
                    } else if (current_market) {
                        market_peon_button.shortcutReleased(peon.decrement(), peon.batch());
                        event.getActions().clear();
                    } else if (current_lodge) {
                        lodge_leave_button.shortcutReleased(peon.decrement(), peon.batch());
                        event.getActions().clear();
                    } else if (current_armory && current_submenu == army_group) {
                        army_peon_button.shortcutReleased(peon.decrement(), peon.batch());
                        event.getActions().clear();
                    } else if (current_ship && current_submenu == ship_army_group) {
                        ship_army_peon_button.shortcutReleased(peon.decrement(), peon.batch());
                        event.getActions().clear();
                    }
                } else {
                    var iron = checkResourceAction(event, GameAction.RES_IRON, GameAction.RES_IRON_DEC,
                            GameAction.RES_IRON_BATCH, GameAction.RES_IRON_BATCH_DEC);
                    if (iron.active()) {
                        handleArmoryShortcut(false, iron, harvest_iron_button, build_weapon_iron_button,
                                army_warrior_iron_button, ship_army_warrior_iron_button,
                                transport_iron_button);
                    } else {
                        var rock = checkResourceAction(event, GameAction.RES_ROCK, GameAction.RES_ROCK_DEC,
                                GameAction.RES_ROCK_BATCH, GameAction.RES_ROCK_BATCH_DEC);
                        if (rock.active()) {
                            handleArmoryShortcut(false, rock, harvest_rock_button, build_weapon_rock_button,
                                    army_warrior_rock_button, ship_army_warrior_rock_button,
                                    transport_rock_button);
                        } else {
                            var tree = checkResourceAction(event, GameAction.RES_TREE, GameAction.RES_TREE_DEC,
                                    GameAction.RES_TREE_BATCH, GameAction.RES_TREE_BATCH_DEC);
                            if (tree.active()) {
                                handleArmoryShortcut(false, tree, harvest_tree_button, null, null, null,
                                        transport_tree_button);
                            } else if (event.consumeAction(GameAction.UNIT_BUILD_TOWER) || event.consumeAction(
                                    GameAction.PROD_TRANSPORT)) {
                                        // Legacy transport alias (tower key) only works from the top
                                        // level, so it cannot hijack an open submenu. Direct submenu
                                        // switching is reserved for the PROD_TRANSPORT press handling.
                                        if (current_armory && current_submenu == null) {
                                            transport_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
                                        } else if (current_ship && current_submenu == null) {
                                            ship_transport_button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
                                        }
                                    }
                        }
                    }
                }
            }
        }
    }

    // Fire a shortcut's button and consume the event so sibling actions bound to the same key
    // (and other input handlers) don't also react to the same press.
    private void activate(@NonNull InputEvent event, @NonNull GUIObject button) {
        button.mouseClickedAll(MouseButton.LEFT, 0, 0, 1);
        event.consume();
    }

    private record ResourceAction(boolean active, boolean decrement, boolean batch) {
    }

    private @NonNull ResourceAction checkResourceAction(@NonNull InputEvent event, @NonNull GameAction base,
            @NonNull GameAction dec, @NonNull GameAction batch, @NonNull GameAction batchDec) {
        if (event.consumeAction(base)) return new ResourceAction(true, false, false);
        if (event.consumeAction(dec)) return new ResourceAction(true, true, false);
        if (event.consumeAction(batch)) return new ResourceAction(true, false, true);
        if (event.consumeAction(batchDec)) return new ResourceAction(true, true, true);
        return new ResourceAction(false, false, false);
    }

    private void handleArmoryShortcut(boolean pressed, @NonNull ResourceAction action,
            @Nullable IconSpinner harvestBtn,
            @Nullable IconSpinner buildBtn,
            @Nullable IconSpinner armyBtn,
            @Nullable IconSpinner shipArmyBtn,
            @Nullable IconSpinner transportBtn) {
        if (!current_armory && !current_ship) return;

        IconSpinner target = null;
        if (current_submenu == harvest_group) target = harvestBtn;
        else if (current_submenu == build_group) target = buildBtn;
        else if (current_submenu == army_group) target = armyBtn;
        else if (current_submenu == ship_army_group) target = shipArmyBtn;
        else if (current_submenu == transport_group) target = transportBtn;

        if (target != null) {
            if (pressed) target.shortcutPressed(action.decrement(), action.batch());
            else target.shortcutReleased(action.decrement(), action.batch());
        }
    }

    /** Buffed's gear rows in the Armory's weapon and army submenus; returns whether the event was theirs. */
    private boolean gearShortcut(@NonNull InputEvent event, boolean pressed) {
        if (shield_enabled) {
            var shield = checkResourceAction(event, GameAction.RES_SHIELD, GameAction.RES_SHIELD_DEC,
                    GameAction.RES_SHIELD_BATCH, GameAction.RES_SHIELD_BATCH_DEC);
            if (shield.active()) {
                handleArmoryShortcut(pressed, shield, null, build_weapon_shield_button, army_warrior_shield_button,
                        null, null);
                return true;
            }
        }
        if (torch_enabled) {
            var torch = checkResourceAction(event, GameAction.RES_TORCH, GameAction.RES_TORCH_DEC,
                    GameAction.RES_TORCH_BATCH, GameAction.RES_TORCH_BATCH_DEC);
            if (torch.active()) {
                handleArmoryShortcut(pressed, torch, null, build_weapon_torch_button, army_warrior_torch_button,
                        null, null);
                return true;
            }
        }
        if (drum_enabled) {
            var drum = checkResourceAction(event, GameAction.RES_DRUM, GameAction.RES_DRUM_DEC,
                    GameAction.RES_DRUM_BATCH, GameAction.RES_DRUM_BATCH_DEC);
            if (drum.active()) {
                handleArmoryShortcut(pressed, drum, null, build_weapon_drum_button, army_warrior_drum_button,
                        null, null);
                return true;
            }
        }
        if (net_enabled) {
            var net = checkResourceAction(event, GameAction.RES_NET, GameAction.RES_NET_DEC,
                    GameAction.RES_NET_BATCH, GameAction.RES_NET_BATCH_DEC);
            if (net.active()) {
                handleArmoryShortcut(pressed, net, null, build_weapon_net_button, army_warrior_net_button,
                        null, null);
                return true;
            }
        }
        return false;
    }

    /** The Champion spinner of a selected Lodge (Buffed); returns whether the event was its. */
    private boolean championShortcut(@NonNull InputEvent event, boolean pressed) {
        if (!current_lodge)
            return false;
        var champion = checkResourceAction(event, GameAction.TRAIN_CHAMPION, GameAction.TRAIN_CHAMPION_DEC,
                GameAction.TRAIN_CHAMPION_BATCH, GameAction.TRAIN_CHAMPION_BATCH_DEC);
        if (!champion.active())
            return false;
        if (pressed)
            lodge_champion_button.shortcutPressed(champion.decrement(), champion.batch());
        else
            lodge_champion_button.shortcutReleased(champion.decrement(), champion.batch());
        return true;
    }

    /** The Champions a player has alive, sheltered or in training, out of the Lodge's limit. */
    private static final class ChampionCounter extends SupplyCounter {
        private final @NonNull Player player;

        ChampionCounter(@NonNull Building lodge, @NonNull Player player) {
            super(lodge, Unit.class);
            this.player = player;
        }

        @Override
        public int getNumSupplies() {
            return player.getChampionCount();
        }

        @Override
        public int getMaxSupplies() {
            Lodge lodge = Lodge.of(getBuilding());
            return lodge != null ? lodge.getStats().max_champions() : 0;
        }
    }

    @Override
    public boolean canHoverBehind() {
        return true;
    }

    public static final int SUBMENU_NONE = 0;
    public static final int SUBMENU_HARVEST = 1;
    public static final int SUBMENU_BUILD = 2;
    public static final int SUBMENU_ARMY = 3;
    public static final int SUBMENU_TRANSPORT = 4;

    public int getSubmenu() {
        if (current_submenu == harvest_group)
            return SUBMENU_HARVEST;
        if (current_submenu == build_group)
            return SUBMENU_BUILD;
        if (current_submenu == army_group)
            return SUBMENU_ARMY;
        if (current_submenu == transport_group)
            return SUBMENU_TRANSPORT;
        return SUBMENU_NONE;
    }

    public void setSubmenu(int submenu) {
        if (submenu == getSubmenu() || (submenu != SUBMENU_NONE && !current_armory && !current_ship))
            return;
        switch (submenu) {
            case SUBMENU_HARVEST -> openSubmenu(harvest_group);
            case SUBMENU_BUILD -> {
                openSubmenu(build_group);
                updateCounters();
            }
            case SUBMENU_ARMY -> openSubmenu(army_group);
            case SUBMENU_TRANSPORT -> openSubmenu(transport_group);
            default -> {
                removeGroups();
                update = true;
            }
        }
    }

    public boolean inHarvestMenu() {
        return current_submenu == harvest_group;
    }

    public boolean inBuildMenu() {
        return current_submenu == build_group;
    }

    public boolean inArmyMenu() {
        return current_submenu == army_group || current_submenu == ship_army_group;
    }

    public boolean inTransportMenu() {
        return current_submenu == transport_group;
    }


    @Override
    public void mouseDragged(@NonNull MouseButton button, int x, int y, int relative_x, int relative_y, int absolute_x,
            int absolute_y) {
        if (getParent() != null)
            getParent().mouseDragged(button, x, y, relative_x, relative_y, absolute_x, absolute_y);
    }

    private void setRallyPoint(@NonNull MouseButton button, int x, int y, int clicks) {
        if (current_building != null && !current_building.isDead()) {
            pushDelegate(new RallyPointDelegate(viewer, camera, current_building));
        }
        removeGroups();
        update = true;
    }

    private void pushDelegate(@NonNull CameraDelegate<?> delegate) {
        var root = viewer.getGUIRoot();
        var current = root.getDelegate();
        if (current instanceof TargetDelegate || current instanceof PlacingDelegate
                || current instanceof RallyPointDelegate) {
            root.removeDelegate(current);
        }
        root.pushDelegate(delegate);
    }

    private void deployChieftainFromShip(@NonNull MouseButton button, int x, int y, int clicks) {
        if (current_ship && current_building instanceof Ship ship) {
            ship.deployChieftain();
        }
    }

    private void cancelSubMenu(@NonNull MouseButton button, int x, int y, int clicks) {
        removeGroups();
        update = true;
    }

    private void openSubmenu(@NonNull Group submenu) {
        if (current_submenu != null)
            current_submenu.remove();
        armory_group.remove();
        ship_group.remove();
        addChild(submenu);
        current_submenu = submenu;
    }

    /**
     * A submenu-opening key works from inside another submenu only when its binding doesn't
     * collide with anything the submenus handle (spinners, back). On collision the submenu
     * meaning wins everywhere, so a key bound to a spinner row never switches submenus.
     */
    private boolean canSwitchSubmenu(@NonNull InputEvent event) {
        if (current_submenu == null) return true;
        for (GameAction action : ARMORY_SUBMENU_ACTIONS) {
            if (event.hasAction(action)) return false;
        }
        return true;
    }

    /**
     * Attempts to close the current armory submenu if one is open.
     * Strips GLOBAL_MENU and UI_CANCEL from the event's action set so
     * they don't trigger the pause menu in InGameDelegate.
     *
     * @return true if a submenu was closed
     */
    public boolean tryCloseSubmenu(@NonNull InputEvent event) {
        if ((current_armory || current_ship) && current_submenu != null) {
            event.consumeAction(GameAction.GLOBAL_MENU);
            event.consumeAction(GameAction.UI_CANCEL);
            removeGroups();
            update = true;
            return true;
        }
        return false;
    }

    private boolean suppliesEmpty(@NonNull SupplyCounter @NonNull... counters) {
        return Arrays.stream(counters).anyMatch(c -> c.getNumSupplies() == 0);
    }
}
