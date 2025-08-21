package dev.cigarette.precomputed;

import net.minecraft.util.math.Vec3i;

public class BlockIn {
    public static final Vec3i[] PLAYER_NEIGHBORS = new Vec3i[]{new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0), new Vec3i(1, -1, 0), new Vec3i(-1, -1, 0), new Vec3i(0, 0, 1), new Vec3i(0, 0, -1), new Vec3i(0, -1, 1), new Vec3i(0, -1, -1), new Vec3i(1, 1, 0), new Vec3i(-1, 1, 0), new Vec3i(0, 1, 1), new Vec3i(0, 1, -1), new Vec3i(0, 2, 0)};
    public static final Vec3i[] BLOCK_NEIGHBORS = new Vec3i[]{new Vec3i(1, 0, 0), new Vec3i(-1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(0, 0, -1), new Vec3i(0, 1, 0), new Vec3i(0, -1, 0)};
}
