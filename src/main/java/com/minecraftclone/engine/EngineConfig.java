package com.minecraftclone.engine;

public record EngineConfig(
        int width,
        int height,
        String title,
        int targetUps,
        boolean vSync,
        boolean captureMouseOnStart) {
}
