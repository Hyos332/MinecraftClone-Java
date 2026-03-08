package com.minecraftclone.game;

import com.minecraftclone.camera.FpsCamera;
import com.minecraftclone.engine.EngineContext;
import com.minecraftclone.engine.GameLogic;
import com.minecraftclone.input.InputManager;
import com.minecraftclone.render.DebugWorldRenderer;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;

public final class PhaseOneGame implements GameLogic {
    private EngineContext context;
    private InputManager input;

    private FpsCamera camera;
    private DebugWorldRenderer renderer;

    private double elapsedTimeSeconds;

    @Override
    public void init(EngineContext context) {
        this.context = context;
        this.input = context.input();

        this.camera = new FpsCamera(0.0f, 3.0f, 8.0f);
        this.renderer = new DebugWorldRenderer();
        this.renderer.init(context.window().width(), context.window().height());
    }

    @Override
    public void handleFrameInput(double frameDeltaSeconds) {
        elapsedTimeSeconds += frameDeltaSeconds;

        if (input.wasKeyPressed(GLFW_KEY_ESCAPE)) {
            context.window().requestClose();
        }

        if (input.wasKeyPressed(GLFW_KEY_F1)) {
            input.setCursorCaptured(!input.isCursorCaptured());
        }

        if (input.wasKeyPressed(GLFW_KEY_R)) {
            camera = new FpsCamera(0.0f, 3.0f, 8.0f);
        }

        if (input.isCursorCaptured()) {
            camera.applyMouseLook(input.mouseDeltaX(), input.mouseDeltaY());
        }
    }

    @Override
    public void fixedUpdate(double fixedDeltaSeconds) {
        camera.updateMovement(input, (float) fixedDeltaSeconds);
    }

    @Override
    public void render(float alpha) {
        if (context.window().consumeResizeFlag()) {
            renderer.resize(context.window().width(), context.window().height());
        }

        renderer.render(camera, elapsedTimeSeconds);
    }

    @Override
    public void dispose() {
        if (renderer != null) {
            renderer.close();
        }
    }
}
