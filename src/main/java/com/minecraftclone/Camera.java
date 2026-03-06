package com.minecraftclone;

import java.awt.event.KeyEvent;

public final class Camera {
    private static final double MOVE_SPEED = 6.0;
    private static final double VERTICAL_SPEED = 5.0;
    private static final double ROTATION_SPEED = 1.8;
    private static final double MAX_PITCH = Math.toRadians(80.0);

    private double x;
    private double y;
    private double z;
    private double yaw;
    private double pitch;

    public Camera(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = 0.0;
        this.pitch = 0.0;
    }

    public void update(Input input, World world, double dt) {
        if (input.isKeyDown(KeyEvent.VK_LEFT)) {
            yaw -= ROTATION_SPEED * dt;
        }
        if (input.isKeyDown(KeyEvent.VK_RIGHT)) {
            yaw += ROTATION_SPEED * dt;
        }
        if (input.isKeyDown(KeyEvent.VK_UP)) {
            pitch += ROTATION_SPEED * dt;
        }
        if (input.isKeyDown(KeyEvent.VK_DOWN)) {
            pitch -= ROTATION_SPEED * dt;
        }
        pitch = clamp(pitch, -MAX_PITCH, MAX_PITCH);

        double moveForward = 0.0;
        double moveSide = 0.0;
        double moveVertical = 0.0;

        if (input.isKeyDown(KeyEvent.VK_W)) {
            moveForward += 1.0;
        }
        if (input.isKeyDown(KeyEvent.VK_S)) {
            moveForward -= 1.0;
        }
        if (input.isKeyDown(KeyEvent.VK_D)) {
            moveSide += 1.0;
        }
        if (input.isKeyDown(KeyEvent.VK_A)) {
            moveSide -= 1.0;
        }
        if (input.isKeyDown(KeyEvent.VK_SPACE)) {
            moveVertical += 1.0;
        }
        if (input.isKeyDown(KeyEvent.VK_SHIFT)) {
            moveVertical -= 1.0;
        }

        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);

        double dx = (sinYaw * moveForward + cosYaw * moveSide);
        double dz = (cosYaw * moveForward - sinYaw * moveSide);

        double horizontalLen = Math.sqrt(dx * dx + dz * dz);
        if (horizontalLen > 1.0) {
            dx /= horizontalLen;
            dz /= horizontalLen;
        }

        x += dx * MOVE_SPEED * dt;
        z += dz * MOVE_SPEED * dt;
        y += moveVertical * VERTICAL_SPEED * dt;

        x = clamp(x, 0.2, world.getSizeX() - 0.2);
        y = clamp(y, 0.2, world.getSizeY() - 0.2);
        z = clamp(z, 0.2, world.getSizeZ() - 0.2);
    }

    public Vec3 getPosition() {
        return new Vec3(x, y, z);
    }

    public Vec3 getForwardVector() {
        double cosPitch = Math.cos(pitch);
        return new Vec3(
                Math.sin(yaw) * cosPitch,
                Math.sin(pitch),
                Math.cos(yaw) * cosPitch).normalized();
    }

    public Vec3 worldToCamera(Vec3 worldPoint) {
        double dx = worldPoint.x() - x;
        double dy = worldPoint.y() - y;
        double dz = worldPoint.z() - z;

        double cosYaw = Math.cos(-yaw);
        double sinYaw = Math.sin(-yaw);

        double x1 = dx * cosYaw - dz * sinYaw;
        double z1 = dx * sinYaw + dz * cosYaw;

        double cosPitch = Math.cos(-pitch);
        double sinPitch = Math.sin(-pitch);

        double y2 = dy * cosPitch - z1 * sinPitch;
        double z2 = dy * sinPitch + z1 * cosPitch;

        return new Vec3(x1, y2, z2);
    }

    public double yaw() {
        return yaw;
    }

    public double pitch() {
        return pitch;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
