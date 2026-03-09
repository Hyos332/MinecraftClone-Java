package com.minecraftclone.physics;

public final class Aabb {
    public float minX;
    public float minY;
    public float minZ;

    public float maxX;
    public float maxY;
    public float maxZ;

    public Aabb(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public boolean intersects(float otherMinX, float otherMinY, float otherMinZ,
                              float otherMaxX, float otherMaxY, float otherMaxZ) {
        return minX < otherMaxX && maxX > otherMinX
                && minY < otherMaxY && maxY > otherMinY
                && minZ < otherMaxZ && maxZ > otherMinZ;
    }
}
