package com.minecraftclone.world.raycast;

public record RaycastHit(
        int blockX,
        int blockY,
        int blockZ,
        int normalX,
        int normalY,
        int normalZ,
        float distance) {
}
