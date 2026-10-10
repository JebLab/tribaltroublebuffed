package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * The grid cells of a Palisade line (Buffed): a staircase from one cell to another in which every cell touches the
 * next one side by side. Units step diagonally between cells, so cells that touched only at a corner would let them
 * slip through.
 */
public final class WallLine {
    /** The most segments one order lays. */
    public static final int MAX_SEGMENTS = 20;

    public record Cell(int x, int y) {
    }

    /** The cells from ({@code x1}, {@code y1}) towards ({@code x2}, {@code y2}), at most {@code max} of them. */
    public static @NonNull List<@NonNull Cell> cells(int x1, int y1, int x2, int y2, int max) {
        List<Cell> cells = new ArrayList<>();
        int dx = Math.abs(x2 - x1);
        int dy = -Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int error = dx + dy;
        int x = x1;
        int y = y1;
        while (cells.size() < max) {
            cells.add(new Cell(x, y));
            if (x == x2 && y == y2)
                break;
            int e2 = 2 * error;
            if (e2 - dy > dx - e2) {
                error += dy;
                x += sx;
            } else {
                error += dx;
                y += sy;
            }
        }
        return cells;
    }

    private WallLine() {
    }
}
