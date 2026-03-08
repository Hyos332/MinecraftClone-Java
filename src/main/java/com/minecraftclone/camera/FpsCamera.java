package com.minecraftclone.camera;

import com.minecraftclone.input.InputManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;

public final class FpsCamera {
    private static final float MAX_PITCH = 89.0f;

    private final Vector3f position = new Vector3f();
    private final Vector3f front = new Vector3f(0.0f, 0.0f, -1.0f);
    private final Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f);
    private final Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
    private final Vector3f worldUp = new Vector3f(0.0f, 1.0f, 0.0f);

    private float yaw = -90.0f;
    private float pitch = 0.0f;

    private float mouseSensitivity = 0.12f;
    private float moveSpeed = 6.0f;
    private float sprintMultiplier = 1.8f;

    public FpsCamera(float x, float y, float z) {
        position.set(x, y, z);
        recalculateBasis();
    }

    public void applyMouseLook(double mouseDx, double mouseDy) {
        yaw += (float) mouseDx * mouseSensitivity;
        pitch -= (float) mouseDy * mouseSensitivity;

        if (pitch > MAX_PITCH) {
            pitch = MAX_PITCH;
        }
        if (pitch < -MAX_PITCH) {
            pitch = -MAX_PITCH;
        }

        recalculateBasis();
    }

    public void updateMovement(InputManager input, float dt) {
        float speed = moveSpeed;
        if (input.isKeyDown(GLFW_KEY_LEFT_CONTROL)) {
            speed *= sprintMultiplier;
        }

        Vector3f frameVelocity = new Vector3f();

        if (input.isKeyDown(GLFW_KEY_W)) {
            frameVelocity.add(front.x, 0.0f, front.z);
        }
        if (input.isKeyDown(GLFW_KEY_S)) {
            frameVelocity.sub(front.x, 0.0f, front.z);
        }
        if (input.isKeyDown(GLFW_KEY_A)) {
            frameVelocity.sub(right.x, 0.0f, right.z);
        }
        if (input.isKeyDown(GLFW_KEY_D)) {
            frameVelocity.add(right.x, 0.0f, right.z);
        }
        if (input.isKeyDown(GLFW_KEY_SPACE)) {
            frameVelocity.y += 1.0f;
        }
        if (input.isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
            frameVelocity.y -= 1.0f;
        }

        if (frameVelocity.lengthSquared() > 0.0f) {
            frameVelocity.normalize(speed * dt);
            position.add(frameVelocity);
        }
    }

    public Matrix4f getViewMatrix(Matrix4f destination) {
        return destination.identity().lookAt(
                position,
                new Vector3f(position).add(front),
                up);
    }

    public Vector3f position() {
        return new Vector3f(position);
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    private void recalculateBasis() {
        front.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front.y = (float) Math.sin(Math.toRadians(pitch));
        front.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front.normalize();

        front.cross(worldUp, right).normalize();
        right.cross(front, up).normalize();
    }
}
