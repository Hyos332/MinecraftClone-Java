package com.minecraftclone.player;

public enum GameMode {
    SURVIVAL,
    CREATIVE;

    public GameMode toggle() {
        return this == SURVIVAL ? CREATIVE : SURVIVAL;
    }
}
