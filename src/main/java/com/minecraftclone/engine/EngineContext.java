package com.minecraftclone.engine;

import com.minecraftclone.input.InputManager;

public record EngineContext(
        Window window,
        InputManager input,
        EngineConfig config) {
}
