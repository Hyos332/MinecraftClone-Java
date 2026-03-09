package com.minecraftclone.save;

import java.util.Map;

public record WorldLoadResult(
        long seed,
        PlayerSaveData player,
        Map<Long, Map<Integer, Byte>> chunkModifications) {
}
