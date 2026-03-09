package com.minecraftclone.engine;

import java.nio.IntBuffer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class Window implements AutoCloseable {
    private final EngineConfig config;

    private long handle;
    private int width;
    private int height;
    private boolean resized;

    private GLFWErrorCallback errorCallback;

    public Window(EngineConfig config) {
        this.config = config;
        this.width = config.width();
        this.height = config.height();
    }

    public void init() {
        errorCallback = GLFWErrorCallback.createPrint(System.err);
        errorCallback.set();

        if (!glfwInit()) {
            throw new IllegalStateException("No se pudo inicializar GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        handle = glfwCreateWindow(width, height, config.title(), NULL, NULL);
        if (handle == NULL) {
            throw new IllegalStateException("No se pudo crear la ventana GLFW");
        }

        glfwSetFramebufferSizeCallback(handle, (window, framebufferWidth, framebufferHeight) -> {
            if (framebufferWidth > 0 && framebufferHeight > 0) {
                width = framebufferWidth;
                height = framebufferHeight;
                resized = true;
            }
        });

        if (System.getenv("WAYLAND_DISPLAY") == null) {
            centerWindow();
        }

        glfwMakeContextCurrent(handle);
        glfwSwapInterval(config.vSync() ? 1 : 0);
        glfwShowWindow(handle);
    }

    public long handle() {
        return handle;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }

    public void requestClose() {
        glfwSetWindowShouldClose(handle, true);
    }

    public void pollEvents() {
        glfwPollEvents();
    }

    public void swapBuffers() {
        glfwSwapBuffers(handle);
    }

    public boolean consumeResizeFlag() {
        boolean result = resized;
        resized = false;
        return result;
    }

    public boolean isMinimized() {
        return width <= 0 || height <= 0;
    }

    private void centerWindow() {
        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidMode == null) {
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetWindowSize(handle, pWidth, pHeight);

            int windowWidth = pWidth.get(0);
            int windowHeight = pHeight.get(0);
            int x = (vidMode.width() - windowWidth) / 2;
            int y = (vidMode.height() - windowHeight) / 2;

            glfwSetWindowPos(handle, x, y);
        }
    }

    @Override
    public void close() {
        if (handle != NULL) {
            glfwDestroyWindow(handle);
            handle = NULL;
        }

        glfwTerminate();

        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
            if (callback == errorCallback) {
                errorCallback = null;
            }
        }

        if (errorCallback != null) {
            errorCallback.free();
            errorCallback = null;
        }
    }
}
