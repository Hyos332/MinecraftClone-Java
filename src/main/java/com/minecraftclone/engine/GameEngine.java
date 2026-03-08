package com.minecraftclone.engine;

import com.minecraftclone.input.InputManager;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;

public final class GameEngine {
    private final EngineConfig config;
    private final GameLogic gameLogic;
    private final Time time = new Time();

    private Window window;
    private InputManager input;
    private boolean running;

    public GameEngine(EngineConfig config, GameLogic gameLogic) {
        this.config = config;
        this.gameLogic = gameLogic;
    }

    public void run() {
        try {
            init();
            loop();
        } finally {
            dispose();
        }
    }

    private void init() {
        window = new Window(config);
        window.init();

        GL.createCapabilities();

        input = new InputManager(window.handle());
        input.setCursorCaptured(config.captureMouseOnStart());

        gameLogic.init(new EngineContext(window, input, config));
    }

    private void loop() {
        running = true;

        double fixedDelta = 1.0 / config.targetUps();
        double accumulator = 0.0;
        double lastTime = time.nowSeconds();

        double fpsTimer = lastTime;
        int frameCounter = 0;

        while (running && !window.shouldClose()) {
            double now = time.nowSeconds();
            double frameDelta = Math.min(now - lastTime, 0.25);
            lastTime = now;

            input.beginFrame();
            window.pollEvents();

            if (window.shouldClose()) {
                break;
            }

            gameLogic.handleFrameInput(frameDelta);

            accumulator += frameDelta;
            while (accumulator >= fixedDelta) {
                gameLogic.fixedUpdate(fixedDelta);
                accumulator -= fixedDelta;
            }

            if (!window.isMinimized()) {
                float alpha = (float) (accumulator / fixedDelta);
                gameLogic.render(alpha);
                window.swapBuffers();

                frameCounter++;
                if (now - fpsTimer >= 1.0) {
                    glfwSetWindowTitle(window.handle(), config.title() + " | FPS: " + frameCounter);
                    frameCounter = 0;
                    fpsTimer = now;
                }
            } else {
                sleepQuietly(50L);
            }
        }
    }

    private void dispose() {
        gameLogic.dispose();
        if (window != null) {
            window.close();
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }
}
