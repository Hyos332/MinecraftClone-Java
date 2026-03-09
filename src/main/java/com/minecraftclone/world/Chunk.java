package com.minecraftclone.world;

import java.util.Arrays;

public final class Chunk {
    public static final int SIZE_X = 16;
    public static final int SIZE_Y = 96;
    public static final int SIZE_Z = 16;

    private final ChunkPos pos;
    private final byte[] blocks = new byte[SIZE_X * SIZE_Y * SIZE_Z];

    private boolean dirty = true;

    public Chunk(ChunkPos pos) {
        this.pos = pos;
        Arrays.fill(blocks, (byte) 0);
    }

    public ChunkPos pos() {
        return pos;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        dirty = true;
    }

    public void clearDirty() {
        dirty = false;
    }

    public byte getLocal(int x, int y, int z) {
        if (!inBounds(x, y, z)) {
            return 0;
        }
        return blocks[indexOf(x, y, z)];
    }

    public void setLocal(int x, int y, int z, byte blockId) {
        if (!inBounds(x, y, z)) {
            return;
        }

        int index = indexOf(x, y, z);
        if (blocks[index] == blockId) {
            return;
        }

        blocks[index] = blockId;
        dirty = true;
    }

    public static boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < SIZE_X
                && y >= 0 && y < SIZE_Y
                && z >= 0 && z < SIZE_Z;
    }

    private static int indexOf(int x, int y, int z) {
        return x + SIZE_X * (z + SIZE_Z * y);
    }
}
