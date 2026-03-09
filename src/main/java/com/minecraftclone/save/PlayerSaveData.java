package com.minecraftclone.save;

import com.minecraftclone.player.GameMode;

public record PlayerSaveData(
        float x,
        float y,
        float z,
        float yaw,
        float pitch,
        GameMode mode,
        int selectedBlockIndex,
        byte[] hotbarBlockIds,
        int[] hotbarCounts) {
}
