package com.minecraftclone.world;

public record ChunkPos(int x, int z) {
    public static long toKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffff_ffffL);
    }

    public long key() {
        return toKey(x, z);
    }
}
