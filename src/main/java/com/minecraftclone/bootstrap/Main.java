package com.minecraftclone.bootstrap;

import com.minecraftclone.engine.EngineConfig;
import com.minecraftclone.engine.GameEngine;
import com.minecraftclone.game.PhaseOneGame;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        EngineConfig config = new EngineConfig(
                1600,
                900,
                "JavaCraft - Fase 1",
                60,
                true,
                true);

        GameEngine engine = new GameEngine(config, new PhaseOneGame());
        engine.run();
    }
}
