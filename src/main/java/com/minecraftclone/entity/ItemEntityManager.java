package com.minecraftclone.entity;

import com.minecraftclone.block.BlockType;
import com.minecraftclone.inventory.HotbarInventory;
import com.minecraftclone.player.GameMode;
import com.minecraftclone.world.VoxelWorld;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class ItemEntityManager {
    private static final float PICKUP_RADIUS = 1.15f;

    private final List<ItemEntity> entities = new ArrayList<>();

    public List<ItemEntity> entities() {
        return entities;
    }

    public void spawnDrop(BlockType blockType, int count, float worldX, float worldY, float worldZ) {
        if (blockType == null || blockType == BlockType.AIR || count <= 0) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            float rx = (random.nextFloat() - 0.5f) * 0.22f;
            float ry = random.nextFloat() * 0.20f;
            float rz = (random.nextFloat() - 0.5f) * 0.22f;

            Vector3f position = new Vector3f(worldX + 0.5f + rx, worldY + 0.45f + ry, worldZ + 0.5f + rz);

            Vector3f velocity = new Vector3f(
                    (random.nextFloat() - 0.5f) * 2.2f,
                    2.6f + random.nextFloat() * 1.8f,
                    (random.nextFloat() - 0.5f) * 2.2f);

            entities.add(new ItemEntity(blockType, 1, position, velocity));
        }
    }

    public void update(VoxelWorld world, HotbarInventory inventory, Vector3fc playerPos, GameMode mode, float dt) {
        float pickupRadiusSq = PICKUP_RADIUS * PICKUP_RADIUS;

        Iterator<ItemEntity> iterator = entities.iterator();
        while (iterator.hasNext()) {
            ItemEntity entity = iterator.next();
            entity.update(world, dt);

            if (!entity.isAlive()) {
                iterator.remove();
                continue;
            }

            Vector3f pos = entity.position();
            float dx = pos.x - playerPos.x();
            float dy = pos.y - (playerPos.y() - 1.1f);
            float dz = pos.z - playerPos.z();
            float distSq = dx * dx + dy * dy + dz * dz;

            if (distSq > pickupRadiusSq || !entity.canBePickedUp()) {
                continue;
            }

            if (mode == GameMode.CREATIVE) {
                entity.kill();
                iterator.remove();
                continue;
            }

            int remaining = inventory.add(entity.blockType(), entity.count());
            if (remaining <= 0) {
                entity.kill();
                iterator.remove();
            } else {
                entity.setCount(remaining);
            }
        }
    }
}
