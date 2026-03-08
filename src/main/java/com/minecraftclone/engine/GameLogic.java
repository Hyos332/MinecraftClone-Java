package com.minecraftclone.engine;

public interface GameLogic {
    void init(EngineContext context);

    void handleFrameInput(double frameDeltaSeconds);

    void fixedUpdate(double fixedDeltaSeconds);

    void render(float alpha);

    void dispose();
}
