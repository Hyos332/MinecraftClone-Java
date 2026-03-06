package com.minecraftclone;

public final class World {
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final byte[][][] blocks;
    private final long seed;

    public World(int sizeX, int sizeY, int sizeZ, long seed) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.seed = seed;
        this.blocks = new byte[sizeX][sizeY][sizeZ];
        generateTerrain();
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public int getSizeZ() {
        return sizeZ;
    }

    public boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < sizeX
                && y >= 0 && y < sizeY
                && z >= 0 && z < sizeZ;
    }

    public byte getBlock(int x, int y, int z) {
        if (!inBounds(x, y, z)) {
            return BlockType.AIR;
        }
        return blocks[x][y][z];
    }

    public void setBlock(int x, int y, int z, byte blockType) {
        if (!inBounds(x, y, z)) {
            return;
        }
        blocks[x][y][z] = blockType;
    }

    public boolean isSolid(int x, int y, int z) {
        return getBlock(x, y, z) != BlockType.AIR;
    }

    public RayHit raycast(Vec3 origin, Vec3 direction, double maxDistance, double step) {
        Vec3 dir = direction.normalized();
        Vec3 previousPoint = origin;

        for (double distance = 0.0; distance <= maxDistance; distance += step) {
            Vec3 point = origin.add(dir.scale(distance));

            int bx = floor(point.x());
            int by = floor(point.y());
            int bz = floor(point.z());

            if (isSolid(bx, by, bz)) {
                int prevX = floor(previousPoint.x());
                int prevY = floor(previousPoint.y());
                int prevZ = floor(previousPoint.z());

                int nx = clamp(prevX - bx, -1, 1);
                int ny = clamp(prevY - by, -1, 1);
                int nz = clamp(prevZ - bz, -1, 1);

                if (nx == 0 && ny == 0 && nz == 0) {
                    if (Math.abs(dir.x()) > Math.abs(dir.y()) && Math.abs(dir.x()) > Math.abs(dir.z())) {
                        nx = dir.x() > 0 ? -1 : 1;
                    } else if (Math.abs(dir.y()) > Math.abs(dir.z())) {
                        ny = dir.y() > 0 ? -1 : 1;
                    } else {
                        nz = dir.z() > 0 ? -1 : 1;
                    }
                }

                return new RayHit(bx, by, bz, nx, ny, nz);
            }

            previousPoint = point;
        }

        return null;
    }

    private void generateTerrain() {
        for (int x = 0; x < sizeX; x++) {
            for (int z = 0; z < sizeZ; z++) {
                double elevation = octaveNoise(x * 0.12, z * 0.12)
                        + 0.5 * octaveNoise(x * 0.26, z * 0.26)
                        + 0.25 * octaveNoise(x * 0.52, z * 0.52);
                elevation /= 1.75;

                int height = (int) Math.round((elevation + 1.0) * 0.5 * (sizeY - 5)) + 2;
                height = clamp(height, 1, sizeY - 2);

                for (int y = 0; y <= height; y++) {
                    if (y == height) {
                        blocks[x][y][z] = BlockType.GRASS;
                    } else if (y >= height - 2) {
                        blocks[x][y][z] = BlockType.DIRT;
                    } else {
                        blocks[x][y][z] = BlockType.STONE;
                    }
                }
            }
        }
    }

    private double octaveNoise(double x, double z) {
        int x0 = floor(x);
        int z0 = floor(z);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        double tx = x - x0;
        double tz = z - z0;

        double n00 = baseNoise(x0, z0);
        double n10 = baseNoise(x1, z0);
        double n01 = baseNoise(x0, z1);
        double n11 = baseNoise(x1, z1);

        double sx = smoothStep(tx);
        double sz = smoothStep(tz);

        double ix0 = lerp(n00, n10, sx);
        double ix1 = lerp(n01, n11, sx);
        return lerp(ix0, ix1, sz);
    }

    private double baseNoise(int x, int z) {
        long n = x * 341_873_128_712L + z * 132_897_987_541L + seed * 17_171L;
        n = (n << 13) ^ n;
        long nn = n * (n * n * 15_731L + 789_221L) + 1_376_312_589L;
        return 1.0 - ((nn & 0x7fff_ffffL) / 1_073_741_824.0);
    }

    private static double smoothStep(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public record RayHit(int blockX, int blockY, int blockZ, int normalX, int normalY, int normalZ) {
    }
}
