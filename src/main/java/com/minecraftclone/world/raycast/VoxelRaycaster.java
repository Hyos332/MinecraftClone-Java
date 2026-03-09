package com.minecraftclone.world.raycast;

import com.minecraftclone.block.BlockType;
import com.minecraftclone.world.VoxelWorld;
import org.joml.Vector3fc;

public final class VoxelRaycaster {
    public RaycastHit raycast(VoxelWorld world, Vector3fc origin, Vector3fc direction, float maxDistance) {
        float dirLengthSq = direction.x() * direction.x() + direction.y() * direction.y() + direction.z() * direction.z();
        if (dirLengthSq < 1.0e-7f) {
            return null;
        }

        float invLen = (float) (1.0 / Math.sqrt(dirLengthSq));
        float dx = direction.x() * invLen;
        float dy = direction.y() * invLen;
        float dz = direction.z() * invLen;

        int x = fastFloor(origin.x());
        int y = fastFloor(origin.y());
        int z = fastFloor(origin.z());

        int stepX = sign(dx);
        int stepY = sign(dy);
        int stepZ = sign(dz);

        float tDeltaX = stepX == 0 ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dx);
        float tDeltaY = stepY == 0 ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dy);
        float tDeltaZ = stepZ == 0 ? Float.POSITIVE_INFINITY : Math.abs(1.0f / dz);

        float tMaxX = stepX == 0
                ? Float.POSITIVE_INFINITY
                : ((stepX > 0 ? (x + 1.0f) - origin.x() : origin.x() - x) * tDeltaX);
        float tMaxY = stepY == 0
                ? Float.POSITIVE_INFINITY
                : ((stepY > 0 ? (y + 1.0f) - origin.y() : origin.y() - y) * tDeltaY);
        float tMaxZ = stepZ == 0
                ? Float.POSITIVE_INFINITY
                : ((stepZ > 0 ? (z + 1.0f) - origin.z() : origin.z() - z) * tDeltaZ);

        int normalX = 0;
        int normalY = 0;
        int normalZ = 0;
        float t = 0.0f;

        while (t <= maxDistance) {
            byte block = world.getBlock(x, y, z);
            if (block != BlockType.AIR.id()) {
                return new RaycastHit(x, y, z, normalX, normalY, normalZ, t);
            }

            if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
                x += stepX;
                t = tMaxX;
                tMaxX += tDeltaX;
                normalX = -stepX;
                normalY = 0;
                normalZ = 0;
            } else if (tMaxY <= tMaxX && tMaxY <= tMaxZ) {
                y += stepY;
                t = tMaxY;
                tMaxY += tDeltaY;
                normalX = 0;
                normalY = -stepY;
                normalZ = 0;
            } else {
                z += stepZ;
                t = tMaxZ;
                tMaxZ += tDeltaZ;
                normalX = 0;
                normalY = 0;
                normalZ = -stepZ;
            }
        }

        return null;
    }

    private static int fastFloor(float value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private static int sign(float value) {
        if (value > 0.0f) {
            return 1;
        }
        if (value < 0.0f) {
            return -1;
        }
        return 0;
    }
}
