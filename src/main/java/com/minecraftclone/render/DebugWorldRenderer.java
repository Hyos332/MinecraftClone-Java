package com.minecraftclone.render;

import com.minecraftclone.camera.FpsCamera;
import com.minecraftclone.resource.ResourceManager;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_TEST;

public final class DebugWorldRenderer implements AutoCloseable {
    private static final float FOV_DEGREES = 75.0f;
    private static final float NEAR_PLANE = 0.05f;
    private static final float FAR_PLANE = 2000.0f;

    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f view = new Matrix4f();

    private ShaderProgram shader;
    private LineMesh gridMesh;

    private int viewportWidth;
    private int viewportHeight;

    public void init(int width, int height) {
        this.viewportWidth = width;
        this.viewportHeight = height;

        String vertex = ResourceManager.readText("/assets/shaders/debug_world.vert");
        String fragment = ResourceManager.readText("/assets/shaders/debug_world.frag");

        shader = new ShaderProgram(vertex, fragment);
        gridMesh = new LineMesh(DebugGeometryFactory.createGridWithAxes(64, 1.0f));

        glEnable(GL_DEPTH_TEST);
        glLineWidth(1.0f);
        updateProjection();
    }

    public void resize(int width, int height) {
        this.viewportWidth = Math.max(width, 1);
        this.viewportHeight = Math.max(height, 1);
        updateProjection();
    }

    public void render(FpsCamera camera, double timeSeconds) {
        glViewport(0, 0, viewportWidth, viewportHeight);

        float skyPulse = (float) ((Math.sin(timeSeconds * 0.25) + 1.0) * 0.5);
        float r = 0.12f + skyPulse * 0.08f;
        float g = 0.18f + skyPulse * 0.10f;
        float b = 0.24f + skyPulse * 0.12f;

        glClearColor(r, g, b, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        shader.bind();
        shader.setUniform("uProjection", projection);
        shader.setUniform("uView", camera.getViewMatrix(view));
        gridMesh.render();
        shader.unbind();
    }

    @Override
    public void close() {
        if (gridMesh != null) {
            gridMesh.close();
            gridMesh = null;
        }
        if (shader != null) {
            shader.close();
            shader = null;
        }
    }

    private void updateProjection() {
        float aspect = viewportWidth / (float) viewportHeight;
        projection.identity().perspective((float) Math.toRadians(FOV_DEGREES), aspect, NEAR_PLANE, FAR_PLANE);
    }
}
