package com.minecraftclone.block;

import java.util.HashMap;
import java.util.Map;

public enum BlockType {
    AIR(0, "Air", false, true, 0.0f, 0.0f, 0.0f),
    GRASS(1, "Grass", true, false, 0.42f, 0.72f, 0.32f),
    DIRT(2, "Dirt", true, false, 0.52f, 0.36f, 0.23f),
    STONE(3, "Stone", true, false, 0.56f, 0.58f, 0.62f),
    SAND(4, "Sand", true, false, 0.82f, 0.76f, 0.50f),
    WOOD(5, "Wood", true, false, 0.56f, 0.41f, 0.23f),
    LEAVES(6, "Leaves", true, true, 0.31f, 0.56f, 0.24f),
    COAL_ORE(7, "Coal Ore", true, false, 0.33f, 0.33f, 0.35f),
    IRON_ORE(8, "Iron Ore", true, false, 0.59f, 0.50f, 0.41f),
    WATER(9, "Water", false, true, 0.17f, 0.35f, 0.73f);

    private static final Map<Byte, BlockType> BY_ID = new HashMap<>();

    static {
        for (BlockType type : values()) {
            BY_ID.put(type.id, type);
        }
    }

    private final byte id;
    private final String displayName;
    private final boolean solid;
    private final boolean transparent;
    private final float r;
    private final float g;
    private final float b;

    BlockType(int id, String displayName, boolean solid, boolean transparent, float r, float g, float b) {
        this.id = (byte) id;
        this.displayName = displayName;
        this.solid = solid;
        this.transparent = transparent;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public byte id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public boolean solid() {
        return solid;
    }

    public boolean transparent() {
        return transparent;
    }

    public float r() {
        return r;
    }

    public float g() {
        return g;
    }

    public float b() {
        return b;
    }

    public static BlockType fromId(byte id) {
        return BY_ID.getOrDefault(id, AIR);
    }

    public static boolean isFaceVisible(byte sourceBlockId, byte neighborBlockId) {
        BlockType source = fromId(sourceBlockId);
        BlockType neighbor = fromId(neighborBlockId);

        if (neighbor == AIR) {
            return true;
        }

        if (source.transparent() && neighbor.transparent()) {
            return source != neighbor;
        }

        return neighbor.transparent();
    }
}
