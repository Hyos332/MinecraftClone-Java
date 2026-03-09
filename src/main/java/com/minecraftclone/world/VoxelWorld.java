package com.minecraftclone.world;

import com.minecraftclone.block.BlockType;
import com.minecraftclone.world.gen.TerrainGenerator;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.joml.Vector3fc;

public final class VoxelWorld {
    private final long seed;
    private final TerrainGenerator generator;
    private final Map<Long, Chunk> chunks = new HashMap<>();
    private final Map<Long, Map<Integer, Byte>> chunkModifications = new HashMap<>();

    public VoxelWorld(long seed) {
        this.seed = seed;
        this.generator = new TerrainGenerator(seed);
    }

    public long seed() {
        return seed;
    }

    public Collection<Chunk> chunks() {
        return chunks.values();
    }

    public int chunkCount() {
        return chunks.size();
    }

    public Chunk getChunk(int chunkX, int chunkZ) {
        return chunks.get(ChunkPos.toKey(chunkX, chunkZ));
    }

    public boolean isSolidBlock(int worldX, int worldY, int worldZ) {
        return BlockType.fromId(getBlock(worldX, worldY, worldZ)).solid();
    }

    public byte getBlock(int worldX, int worldY, int worldZ) {
        if (worldY < 0 || worldY >= Chunk.SIZE_Y) {
            return BlockType.AIR.id();
        }

        int chunkX = floorDiv(worldX, Chunk.SIZE_X);
        int chunkZ = floorDiv(worldZ, Chunk.SIZE_Z);
        Chunk chunk = chunks.get(ChunkPos.toKey(chunkX, chunkZ));
        if (chunk == null) {
            return BlockType.AIR.id();
        }

        int localX = floorMod(worldX, Chunk.SIZE_X);
        int localZ = floorMod(worldZ, Chunk.SIZE_Z);
        return chunk.getLocal(localX, worldY, localZ);
    }

    public void setBlock(int worldX, int worldY, int worldZ, byte blockId) {
        if (worldY < 0 || worldY >= Chunk.SIZE_Y) {
            return;
        }

        int chunkX = floorDiv(worldX, Chunk.SIZE_X);
        int chunkZ = floorDiv(worldZ, Chunk.SIZE_Z);
        Chunk chunk = getOrCreateChunk(chunkX, chunkZ);

        int localX = floorMod(worldX, Chunk.SIZE_X);
        int localZ = floorMod(worldZ, Chunk.SIZE_Z);
        byte previous = chunk.getLocal(localX, worldY, localZ);
        if (previous == blockId) {
            return;
        }
        chunk.setLocal(localX, worldY, localZ, blockId);
        recordModification(chunkX, chunkZ, localX, worldY, localZ, blockId);

        if (localX == 0) {
            markChunkDirty(chunkX - 1, chunkZ);
        }
        if (localX == Chunk.SIZE_X - 1) {
            markChunkDirty(chunkX + 1, chunkZ);
        }
        if (localZ == 0) {
            markChunkDirty(chunkX, chunkZ - 1);
        }
        if (localZ == Chunk.SIZE_Z - 1) {
            markChunkDirty(chunkX, chunkZ + 1);
        }
    }

    public void updateStreaming(Vector3fc playerPosition, int loadRadius, int unloadRadius) {
        int playerChunkX = floorDiv(fastFloor(playerPosition.x()), Chunk.SIZE_X);
        int playerChunkZ = floorDiv(fastFloor(playerPosition.z()), Chunk.SIZE_Z);

        Set<Long> keep = new HashSet<>();

        for (int dz = -loadRadius; dz <= loadRadius; dz++) {
            for (int dx = -loadRadius; dx <= loadRadius; dx++) {
                int cx = playerChunkX + dx;
                int cz = playerChunkZ + dz;
                long key = ChunkPos.toKey(cx, cz);
                keep.add(key);

                if (!chunks.containsKey(key)) {
                    Chunk chunk = new Chunk(new ChunkPos(cx, cz));
                    generator.generateChunk(chunk);
                    applyChunkModifications(chunk);
                    chunks.put(key, chunk);
                }
            }
        }

        int unloadRadiusSq = unloadRadius * unloadRadius;
        Iterator<Map.Entry<Long, Chunk>> iterator = chunks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Chunk> entry = iterator.next();
            Chunk chunk = entry.getValue();

            int dx = chunk.pos().x() - playerChunkX;
            int dz = chunk.pos().z() - playerChunkZ;
            int distSq = dx * dx + dz * dz;

            if (distSq > unloadRadiusSq && !keep.contains(entry.getKey())) {
                iterator.remove();
            }
        }
    }

    private Chunk getOrCreateChunk(int chunkX, int chunkZ) {
        long key = ChunkPos.toKey(chunkX, chunkZ);
        Chunk existing = chunks.get(key);
        if (existing != null) {
            return existing;
        }

        Chunk chunk = new Chunk(new ChunkPos(chunkX, chunkZ));
        generator.generateChunk(chunk);
        applyChunkModifications(chunk);
        chunks.put(key, chunk);
        return chunk;
    }

    public Map<Long, Map<Integer, Byte>> snapshotModifications() {
        Map<Long, Map<Integer, Byte>> copy = new HashMap<>();
        for (Map.Entry<Long, Map<Integer, Byte>> entry : chunkModifications.entrySet()) {
            copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return copy;
    }

    public void loadModifications(Map<Long, Map<Integer, Byte>> loaded) {
        chunkModifications.clear();
        if (loaded != null) {
            for (Map.Entry<Long, Map<Integer, Byte>> entry : loaded.entrySet()) {
                chunkModifications.put(entry.getKey(), new HashMap<>(entry.getValue()));
            }
        }

        for (Chunk chunk : chunks.values()) {
            applyChunkModifications(chunk);
            chunk.markDirty();
        }
    }

    public Map<Long, Map<Integer, Byte>> modificationsView() {
        return Collections.unmodifiableMap(chunkModifications);
    }

    private void markChunkDirty(int chunkX, int chunkZ) {
        Chunk chunk = chunks.get(ChunkPos.toKey(chunkX, chunkZ));
        if (chunk != null) {
            chunk.markDirty();
        }
    }

    private void recordModification(int chunkX, int chunkZ, int localX, int localY, int localZ, byte blockId) {
        long key = ChunkPos.toKey(chunkX, chunkZ);
        Map<Integer, Byte> perChunk = chunkModifications.computeIfAbsent(key, ignored -> new HashMap<>());
        perChunk.put(localIndex(localX, localY, localZ), blockId);
    }

    private void applyChunkModifications(Chunk chunk) {
        Map<Integer, Byte> perChunk = chunkModifications.get(chunk.pos().key());
        if (perChunk == null || perChunk.isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, Byte> entry : perChunk.entrySet()) {
            int index = entry.getKey();
            int localX = index % Chunk.SIZE_X;
            int rem = index / Chunk.SIZE_X;
            int localZ = rem % Chunk.SIZE_Z;
            int localY = rem / Chunk.SIZE_Z;

            if (localY < 0 || localY >= Chunk.SIZE_Y) {
                continue;
            }
            chunk.setLocal(localX, localY, localZ, entry.getValue());
        }
    }

    private static int localIndex(int x, int y, int z) {
        return x + Chunk.SIZE_X * (z + Chunk.SIZE_Z * y);
    }

    private static int fastFloor(float value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private static int floorDiv(int value, int divisor) {
        int q = value / divisor;
        int r = value % divisor;
        if (r != 0 && ((value ^ divisor) < 0)) {
            q--;
        }
        return q;
    }

    private static int floorMod(int value, int divisor) {
        int mod = value % divisor;
        return mod < 0 ? mod + divisor : mod;
    }
}
