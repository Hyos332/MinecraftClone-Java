package com.minecraftclone.world.gen;

import com.minecraftclone.block.BlockType;
import com.minecraftclone.world.Chunk;
import com.minecraftclone.world.ChunkPos;

public final class TerrainGenerator {
    private final long seed;

    public TerrainGenerator(long seed) {
        this.seed = seed;
    }

    public void generateChunk(Chunk chunk) {
        ChunkPos pos = chunk.pos();
        int baseWorldX = pos.x() * Chunk.SIZE_X;
        int baseWorldZ = pos.z() * Chunk.SIZE_Z;

        for (int lx = 0; lx < Chunk.SIZE_X; lx++) {
            int worldX = baseWorldX + lx;
            for (int lz = 0; lz < Chunk.SIZE_Z; lz++) {
                int worldZ = baseWorldZ + lz;

                float humidity = fractal2D(worldX * 0.0022f, worldZ * 0.0022f, 3, 0.58f);
                float temperature = fractal2D((worldX + 7_331) * 0.0024f, (worldZ - 2_941) * 0.0024f, 3, 0.62f);

                float continental = fractal2D(worldX * 0.00135f, worldZ * 0.00135f, 5, 0.5f);
                float detail = fractal2D(worldX * 0.0088f, worldZ * 0.0088f, 4, 0.55f);

                float mountainFactor = Math.max(0.0f, continental - 0.12f) * 16.0f;
                float terrainHeight = 30.0f + continental * 28.0f + detail * 8.0f + mountainFactor;

                int surfaceY = clamp(Math.round(terrainHeight), 4, Chunk.SIZE_Y - 3);

                BlockType surfaceBlock = temperature > 0.58f && humidity < 0.2f ? BlockType.SAND : BlockType.GRASS;

                for (int y = 0; y <= surfaceY; y++) {
                    BlockType block;
                    if (y == 0) {
                        block = BlockType.STONE;
                    } else if (y == surfaceY) {
                        block = surfaceBlock;
                    } else if (y >= surfaceY - 3) {
                        block = BlockType.DIRT;
                    } else {
                        block = BlockType.STONE;
                    }

                    float caveNoise = fractal3D(worldX * 0.052f, y * 0.052f, worldZ * 0.052f, 3, 0.5f);
                    boolean cave = y > 8 && y < surfaceY - 3 && caveNoise > 0.56f;
                    if (cave) {
                        continue;
                    }

                    if (block == BlockType.STONE) {
                        float coal = fractal3D(worldX * 0.11f, y * 0.11f, worldZ * 0.11f, 2, 0.5f);
                        if (coal > 0.74f && y < 68) {
                            block = BlockType.COAL_ORE;
                        }

                        float iron = fractal3D((worldX + 1_731) * 0.12f, y * 0.12f, (worldZ - 911) * 0.12f, 2, 0.5f);
                        if (iron > 0.80f && y < 54) {
                            block = BlockType.IRON_ORE;
                        }
                    }

                    chunk.setLocal(lx, y, lz, block.id());
                }

                if (surfaceBlock == BlockType.GRASS && surfaceY > 24) {
                    float treeChance = fractal2D((worldX + 8_111) * 0.023f, (worldZ - 3_121) * 0.023f, 2, 0.5f);
                    if (treeChance > 0.72f) {
                        placeTree(chunk, lx, surfaceY + 1, lz, worldX, worldZ);
                    }
                }
            }
        }
    }

    private void placeTree(Chunk chunk, int lx, int baseY, int lz, int worldX, int worldZ) {
        int height = 4 + Math.abs(hash(worldX, worldZ, 12_341) % 3);

        for (int i = 0; i < height; i++) {
            int y = baseY + i;
            if (y >= Chunk.SIZE_Y) {
                break;
            }
            chunk.setLocal(lx, y, lz, BlockType.WOOD.id());
        }

        int leafBase = baseY + height - 2;
        for (int y = leafBase; y <= leafBase + 3; y++) {
            if (y < 0 || y >= Chunk.SIZE_Y) {
                continue;
            }
            int radius = y >= leafBase + 2 ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int tx = lx + dx;
                    int tz = lz + dz;
                    if (!Chunk.inBounds(tx, y, tz)) {
                        continue;
                    }
                    if (chunk.getLocal(tx, y, tz) != BlockType.AIR.id()) {
                        continue;
                    }
                    if (Math.abs(dx) + Math.abs(dz) > radius + 1) {
                        continue;
                    }
                    chunk.setLocal(tx, y, tz, BlockType.LEAVES.id());
                }
            }
        }
    }

    private float fractal2D(float x, float z, int octaves, float persistence) {
        float amplitude = 1.0f;
        float frequency = 1.0f;
        float sum = 0.0f;
        float max = 0.0f;

        for (int i = 0; i < octaves; i++) {
            sum += amplitude * valueNoise2D(x * frequency, z * frequency);
            max += amplitude;
            amplitude *= persistence;
            frequency *= 2.0f;
        }

        return sum / max;
    }

    private float fractal3D(float x, float y, float z, int octaves, float persistence) {
        float amplitude = 1.0f;
        float frequency = 1.0f;
        float sum = 0.0f;
        float max = 0.0f;

        for (int i = 0; i < octaves; i++) {
            sum += amplitude * valueNoise3D(x * frequency, y * frequency, z * frequency);
            max += amplitude;
            amplitude *= persistence;
            frequency *= 2.0f;
        }

        return sum / max;
    }

    private float valueNoise2D(float x, float z) {
        int x0 = fastFloor(x);
        int z0 = fastFloor(z);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        float tx = x - x0;
        float tz = z - z0;

        float n00 = hashToUnit(hash(x0, z0, seed));
        float n10 = hashToUnit(hash(x1, z0, seed));
        float n01 = hashToUnit(hash(x0, z1, seed));
        float n11 = hashToUnit(hash(x1, z1, seed));

        float sx = smooth(tx);
        float sz = smooth(tz);

        float ix0 = lerp(n00, n10, sx);
        float ix1 = lerp(n01, n11, sx);
        return lerp(ix0, ix1, sz);
    }

    private float valueNoise3D(float x, float y, float z) {
        int x0 = fastFloor(x);
        int y0 = fastFloor(y);
        int z0 = fastFloor(z);
        int x1 = x0 + 1;
        int y1 = y0 + 1;
        int z1 = z0 + 1;

        float tx = x - x0;
        float ty = y - y0;
        float tz = z - z0;

        float n000 = hashToUnit(hash(x0, y0, z0, seed));
        float n100 = hashToUnit(hash(x1, y0, z0, seed));
        float n010 = hashToUnit(hash(x0, y1, z0, seed));
        float n110 = hashToUnit(hash(x1, y1, z0, seed));
        float n001 = hashToUnit(hash(x0, y0, z1, seed));
        float n101 = hashToUnit(hash(x1, y0, z1, seed));
        float n011 = hashToUnit(hash(x0, y1, z1, seed));
        float n111 = hashToUnit(hash(x1, y1, z1, seed));

        float sx = smooth(tx);
        float sy = smooth(ty);
        float sz = smooth(tz);

        float nx00 = lerp(n000, n100, sx);
        float nx10 = lerp(n010, n110, sx);
        float nx01 = lerp(n001, n101, sx);
        float nx11 = lerp(n011, n111, sx);

        float nxy0 = lerp(nx00, nx10, sy);
        float nxy1 = lerp(nx01, nx11, sy);
        return lerp(nxy0, nxy1, sz);
    }

    private static float smooth(float t) {
        return t * t * (3.0f - 2.0f * t);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static int fastFloor(float value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float hashToUnit(int hash) {
        int unsigned = hash & 0x7fff_ffff;
        return unsigned / (float) 0x7fff_ffff;
    }

    private static int hash(int x, int z, long seed) {
        long n = x * 341_873_128_712L + z * 132_897_987_541L + seed * 14_921L;
        n ^= (n >>> 13);
        n *= 0x9E3779B97F4A7C15L;
        n ^= (n >>> 17);
        return (int) n;
    }

    private static int hash(int x, int y, int z, long seed) {
        long n = x * 73_856_093L + y * 19_349_663L + z * 83_492_791L + seed * 29_417L;
        n ^= (n >>> 11);
        n *= 0x9E3779B97F4A7C15L;
        n ^= (n >>> 19);
        return (int) n;
    }

    private static int hash(int x, int z, int salt) {
        int n = x * 374_761_393 + z * 668_265_263 + salt * 982_451_653;
        n ^= (n >>> 13);
        n *= 1_274_126_177;
        n ^= (n >>> 16);
        return n;
    }
}
