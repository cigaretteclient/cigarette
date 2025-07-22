package io.github.waqfs.precomputed;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

public class PyramidQuadrant {
    public static final Vec3i[][] NORTH_EAST_QUADRANT = new Vec3i[][]{new Vec3i[]{new Vec3i(0, 1, 0), new Vec3i(1, 0, 0), new Vec3i(0, 0, -1),}, new Vec3i[]{new Vec3i(0, 2, 0), new Vec3i(2, 0, 0), new Vec3i(0, 0, -2), new Vec3i(1, 0, -1), new Vec3i(0, 1, -1), new Vec3i(1, 1, 0),}, new Vec3i[]{new Vec3i(0, 3, 0), new Vec3i(3, 0, 0), new Vec3i(0, 0, -3), new Vec3i(1, 2, 0), new Vec3i(1, 0, -2), new Vec3i(0, 1, -2), new Vec3i(2, 1, 0), new Vec3i(0, 2, -1), new Vec3i(2, 0, -1), new Vec3i(1, 1, -1),}, new Vec3i[]{new Vec3i(0, 4, 0), new Vec3i(4, 0, 0), new Vec3i(0, 0, -4), new Vec3i(3, 1, 0), new Vec3i(0, 3, -1), new Vec3i(1, 0, -3), new Vec3i(1, 3, 0), new Vec3i(0, 1, -3), new Vec3i(3, 0, -1), new Vec3i(2, 2, 0), new Vec3i(0, 2, -2), new Vec3i(2, 0, -2),}};
    public static final Vec3i[][] SOUTH_EAST_QUADRANT = new Vec3i[][]{new Vec3i[]{new Vec3i(0, 1, 0), new Vec3i(1, 0, 0), new Vec3i(0, 0, 1),}, new Vec3i[]{new Vec3i(0, 2, 0), new Vec3i(2, 0, 0), new Vec3i(0, 0, 2), new Vec3i(1, 0, 1), new Vec3i(0, 1, 1), new Vec3i(1, 1, 0),}, new Vec3i[]{new Vec3i(0, 3, 0), new Vec3i(3, 0, 0), new Vec3i(0, 0, 3), new Vec3i(1, 2, 0), new Vec3i(1, 0, 2), new Vec3i(0, 1, 2), new Vec3i(2, 1, 0), new Vec3i(0, 2, 1), new Vec3i(2, 0, 1), new Vec3i(1, 1, 1),}, new Vec3i[]{new Vec3i(0, 4, 0), new Vec3i(4, 0, 0), new Vec3i(0, 0, 4), new Vec3i(3, 1, 0), new Vec3i(0, 3, 1), new Vec3i(1, 0, 3), new Vec3i(1, 3, 0), new Vec3i(0, 1, 3), new Vec3i(3, 0, 1), new Vec3i(2, 2, 0), new Vec3i(0, 2, 2), new Vec3i(2, 0, 2),}};
    public static final Vec3i[][] NORTH_WEST_QUADRANT = new Vec3i[][]{new Vec3i[]{new Vec3i(0, 1, 0), new Vec3i(-1, 0, 0), new Vec3i(0, 0, -1),}, new Vec3i[]{new Vec3i(0, 2, 0), new Vec3i(-2, 0, 0), new Vec3i(0, 0, -2), new Vec3i(-1, 0, -1), new Vec3i(0, 1, -1), new Vec3i(-1, 1, 0),}, new Vec3i[]{new Vec3i(0, 3, 0), new Vec3i(-3, 0, 0), new Vec3i(0, 0, -3), new Vec3i(-1, 2, 0), new Vec3i(-1, 0, -2), new Vec3i(0, 1, -2), new Vec3i(-2, 1, 0), new Vec3i(0, 2, -1), new Vec3i(-2, 0, -1), new Vec3i(-1, 1, -1),}, new Vec3i[]{new Vec3i(0, 4, 0), new Vec3i(-4, 0, 0), new Vec3i(0, 0, -4), new Vec3i(-3, 1, 0), new Vec3i(0, 3, -1), new Vec3i(-1, 0, -3), new Vec3i(-1, 3, 0), new Vec3i(0, 1, -3), new Vec3i(-3, 0, -1), new Vec3i(-2, 2, 0), new Vec3i(0, 2, -2), new Vec3i(-2, 0, -2),}};
    public static final Vec3i[][] SOUTH_WEST_QUADRANT = new Vec3i[][]{new Vec3i[]{new Vec3i(0, 1, 0), new Vec3i(-1, 0, 0), new Vec3i(0, 0, 1),}, new Vec3i[]{new Vec3i(0, 2, 0), new Vec3i(-2, 0, 0), new Vec3i(0, 0, 2), new Vec3i(-1, 0, 1), new Vec3i(0, 1, 1), new Vec3i(-1, 1, 0),}, new Vec3i[]{new Vec3i(0, 3, 0), new Vec3i(-3, 0, 0), new Vec3i(0, 0, 3), new Vec3i(-1, 2, 0), new Vec3i(-1, 0, 2), new Vec3i(0, 1, 2), new Vec3i(-2, 1, 0), new Vec3i(0, 2, 1), new Vec3i(-2, 0, 1), new Vec3i(-1, 1, 1),}, new Vec3i[]{new Vec3i(0, 4, 0), new Vec3i(-4, 0, 0), new Vec3i(0, 0, 4), new Vec3i(-3, 1, 0), new Vec3i(0, 3, 1), new Vec3i(-1, 0, 3), new Vec3i(-1, 3, 0), new Vec3i(0, 1, 3), new Vec3i(-3, 0, 1), new Vec3i(-2, 2, 0), new Vec3i(0, 2, 2), new Vec3i(-2, 0, 2),}};

    public static final int MAX_LAYER = SOUTH_WEST_QUADRANT.length;

    public static Vec3i[][] getLeftQuadrant(Direction direction) {
        return switch (direction) {
            case DOWN, UP -> throw new IllegalArgumentException("Direction must lie in the X Z domain");
            case NORTH -> NORTH_WEST_QUADRANT;
            case SOUTH -> SOUTH_EAST_QUADRANT;
            case WEST -> SOUTH_WEST_QUADRANT;
            case EAST -> NORTH_EAST_QUADRANT;
        };
    }

    public static Vec3i[][] getRightQuadrant(Direction direction) {
        return switch (direction) {
            case DOWN, UP -> throw new IllegalArgumentException("Direction must lie in the X Z domain");
            case NORTH -> NORTH_EAST_QUADRANT;
            case SOUTH -> SOUTH_WEST_QUADRANT;
            case WEST -> NORTH_WEST_QUADRANT;
            case EAST -> SOUTH_EAST_QUADRANT;
        };
    }
}
