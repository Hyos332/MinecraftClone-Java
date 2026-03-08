package com.minecraftclone.input;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LAST;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LAST;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;

import java.util.Arrays;

public final class InputManager {
    private final long windowHandle;

    private final boolean[] keyDown = new boolean[GLFW_KEY_LAST + 1];
    private final boolean[] keyPressed = new boolean[GLFW_KEY_LAST + 1];
    private final boolean[] keyReleased = new boolean[GLFW_KEY_LAST + 1];

    private final boolean[] mouseDown = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];
    private final boolean[] mousePressed = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];
    private final boolean[] mouseReleased = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];

    private boolean cursorCaptured;
    private boolean firstCursorEvent = true;

    private double mouseDeltaX;
    private double mouseDeltaY;
    private double lastCursorX;
    private double lastCursorY;
    private double scrollDeltaY;

    public InputManager(long windowHandle) {
        this.windowHandle = windowHandle;
        registerCallbacks();
    }

    public void beginFrame() {
        Arrays.fill(keyPressed, false);
        Arrays.fill(keyReleased, false);
        Arrays.fill(mousePressed, false);
        Arrays.fill(mouseReleased, false);

        mouseDeltaX = 0.0;
        mouseDeltaY = 0.0;
        scrollDeltaY = 0.0;
    }

    public boolean isKeyDown(int keyCode) {
        return keyCode >= 0 && keyCode < keyDown.length && keyDown[keyCode];
    }

    public boolean wasKeyPressed(int keyCode) {
        return keyCode >= 0 && keyCode < keyPressed.length && keyPressed[keyCode];
    }

    public boolean wasKeyReleased(int keyCode) {
        return keyCode >= 0 && keyCode < keyReleased.length && keyReleased[keyCode];
    }

    public boolean isMouseDown(int button) {
        return button >= 0 && button < mouseDown.length && mouseDown[button];
    }

    public boolean wasMousePressed(int button) {
        return button >= 0 && button < mousePressed.length && mousePressed[button];
    }

    public boolean wasMouseReleased(int button) {
        return button >= 0 && button < mouseReleased.length && mouseReleased[button];
    }

    public double mouseDeltaX() {
        return mouseDeltaX;
    }

    public double mouseDeltaY() {
        return mouseDeltaY;
    }

    public double scrollDeltaY() {
        return scrollDeltaY;
    }

    public boolean isCursorCaptured() {
        return cursorCaptured;
    }

    public void setCursorCaptured(boolean capture) {
        this.cursorCaptured = capture;
        this.firstCursorEvent = true;
        glfwSetInputMode(windowHandle, GLFW_CURSOR, capture ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
    }

    private void registerCallbacks() {
        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (key < 0 || key >= keyDown.length) {
                return;
            }

            if (action == GLFW_PRESS) {
                keyDown[key] = true;
                keyPressed[key] = true;
            } else if (action == GLFW_RELEASE) {
                keyDown[key] = false;
                keyReleased[key] = true;
            }
        });

        glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
            if (button < 0 || button >= mouseDown.length) {
                return;
            }

            if (action == GLFW_PRESS) {
                mouseDown[button] = true;
                mousePressed[button] = true;
            } else if (action == GLFW_RELEASE) {
                mouseDown[button] = false;
                mouseReleased[button] = true;
            }
        });

        glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
            if (!cursorCaptured) {
                lastCursorX = xpos;
                lastCursorY = ypos;
                return;
            }

            if (firstCursorEvent) {
                lastCursorX = xpos;
                lastCursorY = ypos;
                firstCursorEvent = false;
                return;
            }

            mouseDeltaX += xpos - lastCursorX;
            mouseDeltaY += ypos - lastCursorY;
            lastCursorX = xpos;
            lastCursorY = ypos;
        });

        glfwSetScrollCallback(windowHandle, (window, xoffset, yoffset) -> scrollDeltaY += yoffset);
    }
}
