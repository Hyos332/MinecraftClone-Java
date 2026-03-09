package com.minecraftclone.render.voxel;

import com.minecraftclone.block.BlockType;
import com.minecraftclone.world.Chunk;
import com.minecraftclone.world.VoxelWorld;

public final class ChunkMesher {
    private static final int[] FACE_NX = {0, 0, -1, 1, 0, 0};
    private static final int[] FACE_NY = {0, 0, 0, 0, -1, 1};
    private static final int[] FACE_NZ = {-1, 1, 0, 0, 0, 0};

    private static final float[] FACE_SHADE = {0.83f, 0.83f, 0.72f, 0.72f, 0.58f, 1.0f};

    private static final float[][][] FACE_VERTICES = {
            {{0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0}},
            {{1, 0, 1}, {0, 0, 1}, {0, 1, 1}, {1, 1, 1}},
            {{0, 0, 1}, {0, 0, 0}, {0, 1, 0}, {0, 1, 1}},
            {{1, 0, 0}, {1, 0, 1}, {1, 1, 1}, {1, 1, 0}},
            {{0, 0, 1}, {1, 0, 1}, {1, 0, 0}, {0, 0, 0}},
            {{0, 1, 0}, {1, 1, 0}, {1, 1, 1}, {0, 1, 1}}
    };

    private static final int[] TRIANGLE_INDICES = {0, 1, 2, 0, 2, 3};

    public ChunkMeshData build(Chunk chunk, VoxelWorld world) {
        VertexBufferBuilder builder = new VertexBufferBuilder(32_768);

        int baseWorldX = chunk.pos().x() * Chunk.SIZE_X;
        int baseWorldZ = chunk.pos().z() * Chunk.SIZE_Z;

        for (int y = 0; y < Chunk.SIZE_Y; y++) {
            for (int z = 0; z < Chunk.SIZE_Z; z++) {
                for (int x = 0; x < Chunk.SIZE_X; x++) {
                    byte blockId = chunk.getLocal(x, y, z);
                    if (blockId == BlockType.AIR.id()) {
                        continue;
                    }

                    BlockType block = BlockType.fromId(blockId);

                    int worldX = baseWorldX + x;
                    int worldY = y;
                    int worldZ = baseWorldZ + z;

                    for (int faceIndex = 0; faceIndex < 6; faceIndex++) {
                        int nx = FACE_NX[faceIndex];
                        int ny = FACE_NY[faceIndex];
                        int nz = FACE_NZ[faceIndex];

                        byte neighborId = world.getBlock(worldX + nx, worldY + ny, worldZ + nz);
                        if (!BlockType.isFaceVisible(blockId, neighborId)) {
                            continue;
                        }

                        float shade = FACE_SHADE[faceIndex];
                        float r = block.r() * shade;
                        float g = block.g() * shade;
                        float b = block.b() * shade;

                        for (int triIndex : TRIANGLE_INDICES) {
                            float[] vertex = FACE_VERTICES[faceIndex][triIndex];
                            builder.push(
                                    worldX + vertex[0],
                                    worldY + vertex[1],
                                    worldZ + vertex[2],
                                    r,
                                    g,
                                    b);
                        }
                    }
                }
            }
        }

        return new ChunkMeshData(builder.toArray(), builder.vertexCount());
    }

    private static final class VertexBufferBuilder {
        private float[] data;
        private int size;

        private VertexBufferBuilder(int initialCapacity) {
            this.data = new float[Math.max(initialCapacity, 96)];
        }

        private void push(float x, float y, float z, float r, float g, float b) {
            ensure(size + 6);
            data[size++] = x;
            data[size++] = y;
            data[size++] = z;
            data[size++] = r;
            data[size++] = g;
            data[size++] = b;
        }

        private int vertexCount() {
            return size / 6;
        }

        private float[] toArray() {
            float[] out = new float[size];
            System.arraycopy(data, 0, out, 0, size);
            return out;
        }

        private void ensure(int required) {
            if (required <= data.length) {
                return;
            }
            int next = data.length;
            while (next < required) {
                next *= 2;
            }
            float[] grown = new float[next];
            System.arraycopy(data, 0, grown, 0, size);
            data = grown;
        }
    }
}
