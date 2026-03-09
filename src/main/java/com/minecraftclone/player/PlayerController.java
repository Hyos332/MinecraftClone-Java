package com.minecraftclone.player;

import com.minecraftclone.block.BlockType;
import com.minecraftclone.camera.FpsCamera;
import com.minecraftclone.input.InputManager;
import com.minecraftclone.physics.Aabb;
import com.minecraftclone.world.VoxelWorld;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;

public final class PlayerController {
    private static final float PLAYER_HALF_WIDTH = 0.30f;
    private static final float PLAYER_HEIGHT = 1.80f;
    private static final float EYE_HEIGHT = 1.62f;

    private static final float WALK_SPEED = 4.7f;
    private static final float SPRINT_MULTIPLIER = 1.55f;
    private static final float GROUND_ACCEL = 26.0f;
    private static final float AIR_ACCEL = 8.0f;

    private static final float GRAVITY = 28.0f;
    private static final float JUMP_SPEED = 8.8f;
    private static final float TERMINAL_VELOCITY = 42.0f;

    private static final float EPSILON = 1.0e-5f;

    private final FpsCamera camera;
    private final Vector3f velocity = new Vector3f();

    private GameMode mode = GameMode.SURVIVAL;
    private boolean onGround;

    public PlayerController(FpsCamera camera) {
        this.camera = camera;
    }

    public void update(InputManager input, VoxelWorld world, float dt) {
        if (mode == GameMode.CREATIVE) {
            camera.updateMovement(input, dt);
            velocity.zero();
            onGround = false;
            return;
        }

        Vector3f front = camera.front();
        Vector3f forwardFlat = new Vector3f(front.x, 0.0f, front.z);
        if (forwardFlat.lengthSquared() > 1.0e-6f) {
            forwardFlat.normalize();
        }

        Vector3f rightFlat = new Vector3f(forwardFlat.z, 0.0f, -forwardFlat.x);

        Vector3f wishDirection = new Vector3f();
        if (input.isKeyDown(GLFW_KEY_W)) {
            wishDirection.add(forwardFlat);
        }
        if (input.isKeyDown(GLFW_KEY_S)) {
            wishDirection.sub(forwardFlat);
        }
        if (input.isKeyDown(GLFW_KEY_D)) {
            wishDirection.add(rightFlat);
        }
        if (input.isKeyDown(GLFW_KEY_A)) {
            wishDirection.sub(rightFlat);
        }

        float baseSpeed = WALK_SPEED;
        if (input.isKeyDown(GLFW_KEY_LEFT_CONTROL)) {
            baseSpeed *= SPRINT_MULTIPLIER;
        }

        float targetX = 0.0f;
        float targetZ = 0.0f;
        if (wishDirection.lengthSquared() > 0.0f) {
            wishDirection.normalize(baseSpeed);
            targetX = wishDirection.x;
            targetZ = wishDirection.z;
        }

        float accel = onGround ? GROUND_ACCEL : AIR_ACCEL;
        velocity.x = approach(velocity.x, targetX, accel * dt);
        velocity.z = approach(velocity.z, targetZ, accel * dt);

        if (onGround && input.wasKeyPressed(GLFW_KEY_SPACE)) {
            velocity.y = JUMP_SPEED;
            onGround = false;
        }

        velocity.y -= GRAVITY * dt;
        if (velocity.y < -TERMINAL_VELOCITY) {
            velocity.y = -TERMINAL_VELOCITY;
        }

        Aabb box = buildAabbFromCamera();

        float dx = velocity.x * dt;
        float dz = velocity.z * dt;
        float dy = velocity.y * dt;

        float resolvedX = resolveAxisX(box, world, dx);
        box.minX += resolvedX;
        box.maxX += resolvedX;
        if (Math.abs(resolvedX - dx) > EPSILON) {
            velocity.x = 0.0f;
        }

        float resolvedZ = resolveAxisZ(box, world, dz);
        box.minZ += resolvedZ;
        box.maxZ += resolvedZ;
        if (Math.abs(resolvedZ - dz) > EPSILON) {
            velocity.z = 0.0f;
        }

        float resolvedY = resolveAxisY(box, world, dy);
        box.minY += resolvedY;
        box.maxY += resolvedY;

        onGround = dy < 0.0f && Math.abs(resolvedY - dy) > EPSILON;
        if (Math.abs(resolvedY - dy) > EPSILON) {
            velocity.y = 0.0f;
        }

        camera.setPosition(
                (box.minX + box.maxX) * 0.5f,
                box.minY + EYE_HEIGHT,
                (box.minZ + box.maxZ) * 0.5f);

        if (camera.position().y < -20.0f) {
            resetVertical();
        }
    }

    public boolean isOnGround() {
        return onGround;
    }

    public Vector3f velocity() {
        return new Vector3f(velocity);
    }

    public GameMode mode() {
        return mode;
    }

    public void setMode(GameMode mode) {
        this.mode = mode;
        if (mode == GameMode.CREATIVE) {
            velocity.zero();
            onGround = false;
        }
    }

    public void toggleMode() {
        setMode(mode.toggle());
    }

    public boolean intersectsBlockAabb(int blockX, int blockY, int blockZ) {
        Aabb player = buildAabbFromCamera();
        return player.intersects(
                blockX,
                blockY,
                blockZ,
                blockX + 1.0f,
                blockY + 1.0f,
                blockZ + 1.0f);
    }

    private void resetVertical() {
        Vector3f p = camera.position();
        camera.setPosition(p.x, ChunkSafeHeight.SAFE_Y, p.z);
        velocity.y = 0.0f;
        onGround = false;
    }

    private Aabb buildAabbFromCamera() {
        Vector3f p = camera.position();
        float minX = p.x - PLAYER_HALF_WIDTH;
        float minY = p.y - EYE_HEIGHT;
        float minZ = p.z - PLAYER_HALF_WIDTH;
        return new Aabb(minX, minY, minZ,
                minX + PLAYER_HALF_WIDTH * 2.0f,
                minY + PLAYER_HEIGHT,
                minZ + PLAYER_HALF_WIDTH * 2.0f);
    }

    private float resolveAxisX(Aabb box, VoxelWorld world, float delta) {
        if (Math.abs(delta) <= EPSILON) {
            return 0.0f;
        }

        float resolved = delta;
        int minY = fastFloor(box.minY + EPSILON);
        int maxY = fastFloor(box.maxY - EPSILON);
        int minZ = fastFloor(box.minZ + EPSILON);
        int maxZ = fastFloor(box.maxZ - EPSILON);

        if (delta > 0.0f) {
            int maxX = fastFloor(box.maxX + delta - EPSILON);
            for (int bx = fastFloor(box.maxX - EPSILON) + 1; bx <= maxX; bx++) {
                for (int by = minY; by <= maxY; by++) {
                    for (int bz = minZ; bz <= maxZ; bz++) {
                        if (!isSolid(world, bx, by, bz)) {
                            continue;
                        }
                        resolved = Math.min(resolved, bx - box.maxX);
                        return resolved;
                    }
                }
            }
        } else {
            int minX = fastFloor(box.minX + delta + EPSILON);
            for (int bx = fastFloor(box.minX + EPSILON) - 1; bx >= minX; bx--) {
                for (int by = minY; by <= maxY; by++) {
                    for (int bz = minZ; bz <= maxZ; bz++) {
                        if (!isSolid(world, bx, by, bz)) {
                            continue;
                        }
                        resolved = Math.max(resolved, (bx + 1.0f) - box.minX);
                        return resolved;
                    }
                }
            }
        }

        return resolved;
    }

    private float resolveAxisY(Aabb box, VoxelWorld world, float delta) {
        if (Math.abs(delta) <= EPSILON) {
            return 0.0f;
        }

        float resolved = delta;
        int minX = fastFloor(box.minX + EPSILON);
        int maxX = fastFloor(box.maxX - EPSILON);
        int minZ = fastFloor(box.minZ + EPSILON);
        int maxZ = fastFloor(box.maxZ - EPSILON);

        if (delta > 0.0f) {
            int maxY = fastFloor(box.maxY + delta - EPSILON);
            for (int by = fastFloor(box.maxY - EPSILON) + 1; by <= maxY; by++) {
                for (int bx = minX; bx <= maxX; bx++) {
                    for (int bz = minZ; bz <= maxZ; bz++) {
                        if (!isSolid(world, bx, by, bz)) {
                            continue;
                        }
                        resolved = Math.min(resolved, by - box.maxY);
                        return resolved;
                    }
                }
            }
        } else {
            int minY = fastFloor(box.minY + delta + EPSILON);
            for (int by = fastFloor(box.minY + EPSILON) - 1; by >= minY; by--) {
                for (int bx = minX; bx <= maxX; bx++) {
                    for (int bz = minZ; bz <= maxZ; bz++) {
                        if (!isSolid(world, bx, by, bz)) {
                            continue;
                        }
                        resolved = Math.max(resolved, (by + 1.0f) - box.minY);
                        return resolved;
                    }
                }
            }
        }

        return resolved;
    }

    private float resolveAxisZ(Aabb box, VoxelWorld world, float delta) {
        if (Math.abs(delta) <= EPSILON) {
            return 0.0f;
        }

        float resolved = delta;
        int minX = fastFloor(box.minX + EPSILON);
        int maxX = fastFloor(box.maxX - EPSILON);
        int minY = fastFloor(box.minY + EPSILON);
        int maxY = fastFloor(box.maxY - EPSILON);

        if (delta > 0.0f) {
            int maxZ = fastFloor(box.maxZ + delta - EPSILON);
            for (int bz = fastFloor(box.maxZ - EPSILON) + 1; bz <= maxZ; bz++) {
                for (int by = minY; by <= maxY; by++) {
                    for (int bx = minX; bx <= maxX; bx++) {
                        if (!isSolid(world, bx, by, bz)) {
                            continue;
                        }
                        resolved = Math.min(resolved, bz - box.maxZ);
                        return resolved;
                    }
                }
            }
        } else {
            int minZ = fastFloor(box.minZ + delta + EPSILON);
            for (int bz = fastFloor(box.minZ + EPSILON) - 1; bz >= minZ; bz--) {
                for (int by = minY; by <= maxY; by++) {
                    for (int bx = minX; bx <= maxX; bx++) {
                        if (!isSolid(world, bx, by, bz)) {
                            continue;
                        }
                        resolved = Math.max(resolved, (bz + 1.0f) - box.minZ);
                        return resolved;
                    }
                }
            }
        }

        return resolved;
    }

    private static boolean isSolid(VoxelWorld world, int x, int y, int z) {
        if (y < 0) {
            return true;
        }
        return BlockType.fromId(world.getBlock(x, y, z)).solid();
    }

    private static float approach(float current, float target, float delta) {
        if (current < target) {
            return Math.min(current + delta, target);
        }
        return Math.max(current - delta, target);
    }

    private static int fastFloor(float value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private static final class ChunkSafeHeight {
        private static final float SAFE_Y = 80.0f;
    }
}
