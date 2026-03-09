package com.minecraftclone.entity;

import com.minecraftclone.block.BlockType;
import com.minecraftclone.world.VoxelWorld;
import org.joml.Vector3f;

public final class ItemEntity {
    private static final float GRAVITY = 20.0f;
    private static final float GROUND_FRICTION = 0.78f;
    private static final float AIR_FRICTION = 0.99f;
    private static final float BOUNCE_DAMPING = -0.28f;

    private final BlockType blockType;
    private final Vector3f position = new Vector3f();
    private final Vector3f velocity = new Vector3f();

    private int count;
    private float pickupDelaySeconds;
    private float ageSeconds;
    private boolean alive = true;

    public ItemEntity(BlockType blockType, int count, Vector3f position, Vector3f initialVelocity) {
        this.blockType = blockType;
        this.count = Math.max(1, count);
        this.position.set(position);
        this.velocity.set(initialVelocity);
        this.pickupDelaySeconds = 0.35f;
    }

    public BlockType blockType() {
        return blockType;
    }

    public int count() {
        return count;
    }

    public void setCount(int count) {
        this.count = Math.max(0, count);
        if (this.count == 0) {
            alive = false;
        }
    }

    public Vector3f position() {
        return new Vector3f(position);
    }

    public float ageSeconds() {
        return ageSeconds;
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean canBePickedUp() {
        return pickupDelaySeconds <= 0.0f;
    }

    public void kill() {
        alive = false;
    }

    public void update(VoxelWorld world, float dt) {
        if (!alive) {
            return;
        }

        ageSeconds += dt;
        pickupDelaySeconds -= dt;

        velocity.y -= GRAVITY * dt;

        float nextX = position.x + velocity.x * dt;
        float nextY = position.y + velocity.y * dt;
        float nextZ = position.z + velocity.z * dt;

        if (isSolid(world, nextX, position.y, position.z)) {
            velocity.x = 0.0f;
            nextX = position.x;
        }

        if (isSolid(world, position.x, position.y, nextZ)) {
            velocity.z = 0.0f;
            nextZ = position.z;
        }

        boolean onGround = false;
        if (velocity.y <= 0.0f && isSolid(world, nextX, nextY - 0.20f, nextZ)) {
            int floorY = fastFloor(nextY - 0.20f);
            nextY = floorY + 1.20f;
            velocity.y *= BOUNCE_DAMPING;
            if (Math.abs(velocity.y) < 0.45f) {
                velocity.y = 0.0f;
                onGround = true;
            }
        } else if (velocity.y > 0.0f && isSolid(world, nextX, nextY + 0.20f, nextZ)) {
            velocity.y = 0.0f;
            nextY = position.y;
        }

        position.set(nextX, nextY, nextZ);

        float friction = onGround ? GROUND_FRICTION : AIR_FRICTION;
        velocity.x *= friction;
        velocity.z *= friction;

        if (ageSeconds > 180.0f) {
            alive = false;
        }
    }

    private static boolean isSolid(VoxelWorld world, float x, float y, float z) {
        int bx = fastFloor(x);
        int by = fastFloor(y);
        int bz = fastFloor(z);
        return world.isSolidBlock(bx, by, bz);
    }

    private static int fastFloor(float value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }
}
