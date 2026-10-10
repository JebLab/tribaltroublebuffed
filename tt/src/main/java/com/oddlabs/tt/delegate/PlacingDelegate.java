package com.oddlabs.tt.delegate;

import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.camera.GameCamera;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.input.GameAction;
import com.oddlabs.tt.input.InputEvent;
import com.oddlabs.tt.input.InputPhase;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.BuildingTemplate;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.WallLine;
import com.oddlabs.tt.pathfinder.UnitGrid;
import com.oddlabs.tt.render.BuildingGhostRenderer;
import com.oddlabs.tt.render.LandscapeLocation;
import com.oddlabs.tt.render.LandscapeRenderer;
import com.oddlabs.tt.render.MatrixStack;
import com.oddlabs.tt.render.RenderQueues;
import com.oddlabs.tt.viewer.WorldViewer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.logging.Logger;

public final class PlacingDelegate extends ControllableCameraDelegate {
    private static final Logger logger = Logger.getLogger(PlacingDelegate.class.getName());
    private static final LandscapeLocation landscape_hit = new LandscapeLocation();

    private final BuildingGhostRenderer ghost = new BuildingGhostRenderer();
    private final int building_index;
    // Buffed's Palisade is dragged: the cell where the left button went down, while it is held.
    private WallLine.@Nullable Cell drag_start;

    public PlacingDelegate(@NonNull WorldViewer viewer, @NonNull CameraState old_camera, int building_index) {
        super(viewer, new GameCamera(viewer, old_camera));
        this.building_index = building_index;
    }

    @Override
    public int getPlacingBuildingIndex() {
        return building_index;
    }

    private @NonNull BuildingTemplate getTemplate() {
        return getViewer().getLocalPlayer().getRace().getBuildingTemplate(building_index);
    }

    /** The cell under the cursor, or null off the map. */
    private WallLine.@Nullable Cell pickCell() {
        if (!getViewer().getPicker().pickLocation(getCamera().getState(), landscape_hit)) {
            logger.info("placeObject: Pick failed (off map?)");
            return null;
        }
        return new WallLine.Cell(UnitGrid.toGridCoordinate(landscape_hit.x), UnitGrid.toGridCoordinate(
                landscape_hit.y));
    }

    public void placeObject() {
        WallLine.Cell cell = pickCell();
        if (cell == null)
            return;
        if (Race.isWall(building_index)) {
            placeWall(cell, cell);
            return;
        }
        int placing_grid_x = cell.x();
        int placing_grid_y = cell.y();
        if (getTemplate().isPlacingLegal(getViewer().getWorld().getUnitGrid(), placing_grid_x, placing_grid_y)) {
            var peons = getViewer().getSelection().getCurrentSelection().filter(Abilities.BUILD);
            if (peons.length > 0) {
                logger.info("placeObject: Placing building at " + placing_grid_x + "," + placing_grid_y);
                getViewer().getPeerHub().getPlayerInterface().placeBuilding(peons, building_index, placing_grid_x,
                        placing_grid_y);
            } else {
                logger.info("placeObject: No peons selected");
            }
            logger.info("placeObject: Popping delegate");
            pop();
        } else {
            logger.info("placeObject: Placement illegal");
        }
    }

    /** Lays the Palisade segments from one cell to another, or a Gate at the first, if any cell is free. */
    private void placeWall(WallLine.@NonNull Cell start, WallLine.@NonNull Cell end) {
        UnitGrid unit_grid = getViewer().getWorld().getUnitGrid();
        boolean any_legal = wallCells(start, end).stream().anyMatch(
                c -> getTemplate().isPlacingLegal(unit_grid, c.x(), c.y()));
        if (!any_legal) {
            logger.info("placeWall: Placement illegal");
            return;
        }
        var peons = getViewer().getSelection().getCurrentSelection().filter(Abilities.BUILD);
        if (peons.length > 0) {
            logger.info("placeWall: Placing walls from " + start + " to " + end);
            getViewer().getPeerHub().getPlayerInterface().placePalisade(peons, building_index, start.x(), start.y(),
                    end.x(), end.y());
        } else {
            logger.info("placeWall: No peons selected");
        }
        pop();
    }

    private @NonNull List<WallLine.@NonNull Cell> wallCells(WallLine.@NonNull Cell start, WallLine.@NonNull Cell end) {
        int max = building_index == Race.BUILDING_GATE ? 1 : WallLine.MAX_SEGMENTS;
        return WallLine.cells(start.x(), start.y(), end.x(), end.y(), max);
    }

    @Override
    public void handleInput(@NonNull InputEvent event) {
        if (event.consumeAction(GameAction.UI_ACTIVATE)) {
            if (event.getPhase() == InputPhase.RELEASED) {
                placeObject();
            }
            event.consume();
            return;
        }

        if (event.getPhase() == InputPhase.PRESSED || event.getPhase() == InputPhase.REPEAT) {
            if (event.consumeAction(GameAction.UI_CANCEL)) {
                pop();
                event.consume();
                return;
            }
        }

        super.handleInput(event);
    }

    @Override
    public void mousePressed(@NonNull MouseButton button, int x, int y) {
        switch (button) {
            case LEFT -> {
                if (building_index == Race.BUILDING_PALISADE)
                    drag_start = pickCell();
                else
                    placeObject();
            }
            case RIGHT -> pop();
            default -> super.mousePressed(button, x, y);
        }
    }

    @Override
    public void mouseReleased(@NonNull MouseButton button, int x, int y) {
        if (button == MouseButton.LEFT && drag_start != null) {
            WallLine.Cell start = drag_start;
            drag_start = null;
            WallLine.Cell end = pickCell();
            placeWall(start, end != null ? end : start);
            return;
        }
        super.mouseReleased(button, x, y);
    }

    @Override
    public void render3D(@NonNull LandscapeRenderer renderer, @NonNull RenderQueues queues, @NonNull CameraState state,
            @NonNull MatrixStack modelViewStack, @NonNull MatrixStack projectionStack) {
        if (!getViewer().getPicker().pickLocation(getCamera().getState(), landscape_hit)) return;
        int grid_x = UnitGrid.toGridCoordinate(landscape_hit.x);
        int grid_y = UnitGrid.toGridCoordinate(landscape_hit.y);
        if (drag_start != null) {
            ghost.renderLine(getViewer().getWorld(), getTemplate(), wallCells(drag_start,
                    new WallLine.Cell(grid_x, grid_y)), renderer, queues, modelViewStack, projectionStack);
            return;
        }
        ghost.render(getViewer().getWorld(), getTemplate(), grid_x, grid_y, renderer, queues, modelViewStack,
                projectionStack);
    }
}
